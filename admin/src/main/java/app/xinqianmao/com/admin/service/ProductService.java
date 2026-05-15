/**
 * File: ProductService.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.service;

import app.xinqianmao.com.admin.common.entity.*;
import app.xinqianmao.com.admin.common.pojo.*;
import app.xinqianmao.com.admin.dao.*;
import app.xinqianmao.com.common.auth.TenantContext;
import app.xinqianmao.com.common.exception.BizException;
import app.xinqianmao.com.common.utils.DateTimeUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Product management: CRUD, search, hot toggle, properties, SKU aggregation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final ProductPropertyMapper propertyMapper;
    private final ProductSkuMapper skuMapper;
    private final ProductSpecsMapper specsMapper;
    private final InventoryLogMapper inventoryLogMapper;
    private final ProductCategoryMapper categoryMapper;
    private final ProductTypeMapper typeMapper;
    private final HotProductMapper hotProductMapper;
    private final OrderProductSkuMapper orderProductSkuMapper;
    private final ProductTypeSpecRelMapper typeSpecRelMapper;
    private final ImageDownloadService imageDownloadService;

    /**
     * Search products with filters and pagination.
     */
    public IPage<ProductListResponse> search(ProductSearchRequest req) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();

        if (req.getName() != null && !req.getName().isBlank()) {
            wrapper.like(Product::getName, req.getName());
        }
        // Category filter: support both single and multi
        if (req.getCategoryIds() != null && !req.getCategoryIds().isEmpty()) {
            wrapper.in(Product::getProductCategory, req.getCategoryIds());
        } else if (req.getCategoryId() != null && !req.getCategoryId().isBlank()) {
            wrapper.eq(Product::getProductCategory, req.getCategoryId());
        }
        // Type filter: support both single and multi
        if (req.getTypeIds() != null && !req.getTypeIds().isEmpty()) {
            wrapper.in(Product::getProductType, req.getTypeIds());
        } else if (req.getTypeId() != null && !req.getTypeId().isBlank()) {
            wrapper.eq(Product::getProductType, req.getTypeId());
        }
        // Brand filter
        if (req.getBrandId() != null && !req.getBrandId().isBlank()) {
            wrapper.eq(Product::getProductBrand, req.getBrandId());
        }
        // Barcode search: match any SKU's barcode
        if (req.getBarcode() != null && !req.getBarcode().isBlank()) {
            wrapper.exists("SELECT 1 FROM t_product_sku s WHERE s.product_id = t_product.id AND s.barcode LIKE CONCAT('%', {0}, '%')",
                    req.getBarcode().replaceAll("[%_]", "\\\\$0"));
        }
        if (req.getIsEnable() != null) {
            wrapper.eq(Product::getIsEnable, req.getIsEnable());
        }
        if (req.getPriceMin() != null) {
            wrapper.ge(Product::getPrice, req.getPriceMin());
        }
        if (req.getPriceMax() != null) {
            wrapper.le(Product::getPrice, req.getPriceMax());
        }
        if (req.getCreateTimeStart() != null) {
            LocalDateTime start = DateTimeUtil.parse(req.getCreateTimeStart());
            if (start != null) wrapper.ge(Product::getCreateTime, start);
        }
        if (req.getCreateTimeEnd() != null) {
            LocalDateTime end = DateTimeUtil.parse(req.getCreateTimeEnd());
            if (end != null) wrapper.le(Product::getCreateTime, end);
        }

        // Sorting
        String sortBy = req.getSortBy() != null ? req.getSortBy() : "createTime";
        boolean asc = "asc".equalsIgnoreCase(req.getSortOrder());
        wrapper.orderBy(true, true, Product::getSort);
        switch (sortBy) {
            case "price" -> wrapper.orderBy(true, asc, Product::getPrice);
            case "salesCount" -> wrapper.last(
                    "ORDER BY (SELECT COALESCE(SUM(ops.inventory),0) FROM t_order_product_skus ops WHERE ops.product_id = t_product.id) DESC");
            default -> wrapper.orderByDesc(Product::getCreateTime);
        }

        Page<Product> page = Page.of(req.getPage(), req.getPageSize());
        IPage<Product> productPage = productMapper.selectPage(page, wrapper);

        // Load hot product IDs
        List<HotProduct> hotList = hotProductMapper.selectList(new LambdaQueryWrapper<>());
        Set<String> hotProductIds = hotList.stream().map(HotProduct::getProductId).collect(Collectors.toSet());

        // Load category and type name maps
        Map<String, String> categoryNames = new HashMap<>();
        Map<String, String> typeNames = new HashMap<>();

        return productPage.convert(p -> {
            ProductListResponse r = new ProductListResponse();
            r.setId(p.getId());
            r.setName(p.getName());
            r.setDesc(p.getDesc());
            r.setPrice(p.getPrice());
            r.setOldPrice(p.getOldPrice());
            r.setPicture(p.getPicture());
            r.setSort(p.getSort());
            r.setIsEnable(p.getIsEnable());
            r.setCreateTime(DateTimeUtil.format(p.getCreateTime()));
            r.setIsHot(hotProductIds.contains(p.getId()));

            // Lazy load category name
            String catName = categoryNames.computeIfAbsent(p.getProductCategory(), catId -> {
                ProductCategory cat = categoryMapper.selectById(catId);
                return cat != null ? cat.getName() : "";
            });
            r.setCategoryName(catName);

            // Lazy load type name
            String tName = typeNames.computeIfAbsent(p.getProductType(), typeId -> {
                ProductType t = typeMapper.selectById(typeId);
                return t != null ? t.getName() : "";
            });
            r.setTypeName(tName);

            return r;
        });
    }

    /**
     * Get product full detail with properties, SKUs, and specs.
     */
    public ProductDetailResponse getDetail(String productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BizException("404", "商品不存在");
        }

        ProductDetailResponse r = new ProductDetailResponse();
        r.setId(product.getId());
        r.setName(product.getName());
        r.setDesc(product.getDesc());
        r.setPrice(product.getPrice());
        r.setOldPrice(product.getOldPrice());
        r.setPicture(product.getPicture());
        r.setMainPictures(product.getMainPictures());
        r.setDetail(product.getDetail());
        r.setProductType(product.getProductType());
        r.setProductCategory(product.getProductCategory());
        r.setProductBrand(product.getProductBrand());
        r.setSort(product.getSort());
        r.setIsEnable(product.getIsEnable());
        r.setCreateTime(DateTimeUtil.format(product.getCreateTime()));
        r.setModifyTime(DateTimeUtil.format(product.getModifyTime()));

        // Properties - look up name from specs via specsId
        List<ProductProperty> props = propertyMapper.selectList(
                new LambdaQueryWrapper<ProductProperty>().eq(ProductProperty::getProductId, productId)
                        .eq(ProductProperty::getIsDelete, 0)
                        .orderByAsc(ProductProperty::getSort));
        // Pre-load specs for name resolution
        Map<String, String> specsNameMap = new HashMap<>();
        if (!props.isEmpty()) {
            List<String> specsIds = props.stream().map(ProductProperty::getSpecsId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            if (!specsIds.isEmpty()) {
                List<ProductSpecs> specsList = specsMapper.selectBatchIds(specsIds);
                specsList.forEach(s -> specsNameMap.put(s.getId(), s.getName()));
            }
        }
        r.setProperties(props.stream().map(prop -> {
            ProductDetailResponse.PropertyItem pi = new ProductDetailResponse.PropertyItem();
            pi.setId(prop.getId());
            pi.setSpecsId(prop.getSpecsId());
            pi.setName(specsNameMap.getOrDefault(prop.getSpecsId(), ""));
            pi.setValueName(prop.getValueName());
            return pi;
        }).collect(Collectors.toList()));

        // SKUs
        List<ProductSku> skus = skuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, productId)
                        .eq(ProductSku::getIsDelete, 0));
        r.setSkus(skus.stream().map(sku -> {
            ProductDetailResponse.SkuItem si = new ProductDetailResponse.SkuItem();
            si.setId(sku.getId());
            si.setPrice(sku.getPrice());
            si.setOldPrice(sku.getOldPrice());
            si.setCostPrice(sku.getCostPrice());
            si.setInventory(sku.getInventory());
            si.setBarcode(sku.getBarcode());
            si.setPicture(sku.getPicture());
            // Parse specs from JSON string
            si.setSpecs(parseSpecsJson(sku.getSpecs()));
            return si;
        }).collect(Collectors.toList()));

        // Specs: get from type-spec relation table + global specs
        List<ProductTypeSpecRel> rels = typeSpecRelMapper.selectList(
                new LambdaQueryWrapper<ProductTypeSpecRel>().eq(ProductTypeSpecRel::getProductType, product.getProductType()));
        List<String> relatedSpecIds = rels.stream().map(ProductTypeSpecRel::getSpecsId).collect(Collectors.toList());
        // Also include global specs (scope=0)
        List<ProductSpecs> globalSpecs = specsMapper.selectList(
                new LambdaQueryWrapper<ProductSpecs>().eq(ProductSpecs::getScope, 0).orderByAsc(ProductSpecs::getSort));
        List<ProductSpecs> specDefs = new ArrayList<>(globalSpecs);
        if (!relatedSpecIds.isEmpty()) {
            List<ProductSpecs> typeSpecs = specsMapper.selectBatchIds(relatedSpecIds);
            specDefs.addAll(typeSpecs);
        }
        r.setSpecs(buildSpecItems(specDefs));

        return r;
    }

    /**
     * Create product with properties.
     */
    @Transactional
    public String create(ProductSaveRequest req) {
        // Generate product ID upfront so images can be saved in categorized paths
        String productId = UUID.randomUUID().toString();

        Product product = new Product();
        product.setId(productId);
        product.setName(req.getName());
        product.setProductType(req.getProductType());
        product.setProductCategory(req.getProductCategory());
        product.setProductBrand(req.getProductBrand());
        product.setDesc(req.getDesc());
        product.setPrice(req.getPrice());
        product.setOldPrice(req.getOldPrice());
        product.setMainPictures(req.getMainPictures() != null ? req.getMainPictures() : List.of());
        product.setPicture(req.getPicture());
        product.setDetail(imageDownloadService.downloadImagesInHtml(
                req.getDetail() != null ? req.getDetail() : "", productId));
        product.setMainPictures(imageDownloadService.downloadImageList(
                req.getMainPictures() != null ? req.getMainPictures() : List.of(), productId));
        if (product.getMainPictures() != null && !product.getMainPictures().isEmpty())
            product.setPicture(product.getMainPictures().get(0));
        else product.setPicture(req.getPicture());
        product.setSort(req.getSort() != null ? req.getSort() : 0);
        product.setIsEnable(1);
        product.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        productMapper.insert(product);

        // Save properties - resolve specsId from name
        Map<String, String> specsNameToId = buildSpecsNameMap(product.getProductType());
        if (req.getProperties() != null) {
            for (ProductSaveRequest.PropertyItem pi : req.getProperties()) {
                ProductProperty prop = new ProductProperty();
                prop.setProductId(product.getId());
                prop.setSpecsId(specsNameToId.get(pi.getName()));
                prop.setValueName(pi.getValueName());
                prop.setSort(0);
                prop.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
                propertyMapper.insert(prop);
            }
        }

        // Save SKUs (at least one required)
        if (req.getSkus() == null || req.getSkus().isEmpty()) {
            throw new BizException("400", "至少需要一个SKU");
        }
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        for (ProductSaveRequest.SkuItem si : req.getSkus()) {
                ProductSku sku = new ProductSku();
                sku.setProductId(product.getId());
                sku.setPrice(si.getPrice());
                sku.setOldPrice(si.getOldPrice());
                sku.setInventory(si.getInventory());
                if (si.getCostPrice() == null) throw new BizException("400", "SKU成本价不能为空");
                sku.setCostPrice(si.getCostPrice());
                sku.setBarcode(si.getBarcode() != null ? si.getBarcode() : "");
                sku.setPicture(imageDownloadService.downloadSingleImage(si.getPicture(), productId));
                try {
                    sku.setSpecs(mapper.writeValueAsString(si.getSpecs()));
                } catch (Exception e) {
                    sku.setSpecs("[]");
                }
                sku.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
                skuMapper.insert(sku);
                writeInventoryLog(sku, null, "in", sku.getInventory(), 0, sku.getInventory());
        }

        // Move temp images to product's permanent directory
        imageDownloadService.relocateProductImages(productId);

        return product.getId();
    }

    private void writeInventoryLog(ProductSku sku, String orderNo, String changeType,
                                    int changeNum, int before, int after) {
        InventoryLog log = new InventoryLog();
        log.setSkuId(sku.getId());
        log.setBarcode(sku.getBarcode());
        log.setOrderNo(orderNo);
        log.setChangeType(changeType);
        log.setChangeNum(changeNum);
        log.setBeforeInventory(before);
        log.setAfterInventory(after);
        log.setOperator("admin");
        log.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        inventoryLogMapper.insert(log);
    }

    /**
     * Update product info and properties.
     */
    @Transactional
    public void update(String productId, ProductSaveRequest req) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BizException("404", "商品不存在");
        }
        product.setName(req.getName());
        product.setProductType(req.getProductType());
        product.setProductCategory(req.getProductCategory());
        product.setProductBrand(req.getProductBrand());
        product.setDesc(req.getDesc());
        product.setPrice(req.getPrice());
        product.setOldPrice(req.getOldPrice());
        product.setMainPictures(req.getMainPictures() != null ? req.getMainPictures() : List.of());
        product.setPicture(req.getPicture());
        product.setDetail(imageDownloadService.downloadImagesInHtml(
                req.getDetail() != null ? req.getDetail() : "", productId));
        product.setMainPictures(imageDownloadService.downloadImageList(
                req.getMainPictures() != null ? req.getMainPictures() : List.of(), productId));
        if (product.getMainPictures() != null && !product.getMainPictures().isEmpty())
            product.setPicture(product.getMainPictures().get(0));
        else product.setPicture(req.getPicture());
        product.setSort(req.getSort() != null ? req.getSort() : 0);
        product.setModifyTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        productMapper.updateById(product);

        // Replace properties
        propertyMapper.delete(new LambdaQueryWrapper<ProductProperty>()
                .eq(ProductProperty::getProductId, productId));
        Map<String, String> specsNameMap = buildSpecsNameMap(product.getProductType());
        if (req.getProperties() != null) {
            for (ProductSaveRequest.PropertyItem pi : req.getProperties()) {
                ProductProperty prop = new ProductProperty();
                prop.setProductId(productId);
                prop.setSpecsId(specsNameMap.get(pi.getName()));
                prop.setValueName(pi.getValueName());
                prop.setSort(0);
                prop.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
                propertyMapper.insert(prop);
            }
        }

        // Replace SKUs (at least one required)
        // Load old SKUs before delete for inventory comparison
        Map<String, ProductSku> oldSkus = new HashMap<>();
        List<ProductSku> existingSkus = skuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, productId)
                        .eq(ProductSku::getIsDelete, 0));
        for (ProductSku s : existingSkus) oldSkus.put(s.getId(), s);

        // Soft-delete old SKUs
        for (ProductSku s : existingSkus) {
            s.setIsDelete(1);
            skuMapper.updateById(s);
        }
        if (req.getSkus() == null || req.getSkus().isEmpty()) {
            throw new BizException("400", "至少需要一个SKU");
        }
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        for (ProductSaveRequest.SkuItem si : req.getSkus()) {
            ProductSku sku = new ProductSku();
            sku.setProductId(productId);
            sku.setPrice(si.getPrice());
            sku.setOldPrice(si.getOldPrice());
            sku.setInventory(si.getInventory());
            if (si.getCostPrice() == null) throw new BizException("400", "SKU成本价不能为空");
            sku.setCostPrice(si.getCostPrice());
            sku.setBarcode(si.getBarcode() != null ? si.getBarcode() : "");
            sku.setPicture(imageDownloadService.downloadSingleImage(si.getPicture(), productId));
            try {
                sku.setSpecs(mapper.writeValueAsString(si.getSpecs()));
            } catch (Exception e) {
                sku.setSpecs("[]");
            }
            sku.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
            skuMapper.insert(sku);

            // Inventory change log (adjust)
            ProductSku oldSku = oldSkus.get(si.getId());
            int oldInv = oldSku != null && oldSku.getInventory() != null ? oldSku.getInventory() : 0;
            int newInv = sku.getInventory() != null ? sku.getInventory() : 0;
            if (oldInv != newInv) {
                writeInventoryLog(sku, null, "adjust", newInv - oldInv, oldInv, newInv);
            }
        }
    }

    /**
     * Delete product.
     */
    @Transactional
    public void delete(String productId) {
        productMapper.deleteById(productId);
        propertyMapper.delete(new LambdaQueryWrapper<ProductProperty>()
                .eq(ProductProperty::getProductId, productId));
        skuMapper.delete(new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getProductId, productId));
        hotProductMapper.delete(new LambdaQueryWrapper<HotProduct>()
                .eq(HotProduct::getProductId, productId));
    }

    /**
     * Toggle product in/out of hot list.
     */
    public void toggleHot(String productId) {
        List<HotProduct> existing = hotProductMapper.selectList(
                new LambdaQueryWrapper<HotProduct>().eq(HotProduct::getProductId, productId));
        if (existing.isEmpty()) {
            HotProduct hp = new HotProduct();
            hp.setProductId(productId);
            hp.setSort(0);
            hp.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
            hotProductMapper.insert(hp);
        } else {
            hotProductMapper.delete(new LambdaQueryWrapper<HotProduct>().eq(HotProduct::getProductId, productId));
        }
    }

    /**
     * Toggle product enable/disable (上架/下架).
     */
    public void toggleEnable(String productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) throw new BizException("404", "商品不存在");
        // 1→2 (上架→下架), 2→1 (下架→上架)
        product.setIsEnable(product.getIsEnable() != null && product.getIsEnable() == 1 ? 2 : 1);
        product.setModifyTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        productMapper.updateById(product);
    }

    private Map<String, String> buildSpecsNameMap(String productType) {
        Map<String, String> map = new HashMap<>();
        // Global specs (scope=0)
        List<ProductSpecs> globalSpecs = specsMapper.selectList(
                new LambdaQueryWrapper<ProductSpecs>().eq(ProductSpecs::getScope, 0));
        globalSpecs.forEach(s -> map.put(s.getName(), s.getId()));
        // Type-linked specs via rel table
        List<ProductTypeSpecRel> rels = typeSpecRelMapper.selectList(
                new LambdaQueryWrapper<ProductTypeSpecRel>().eq(ProductTypeSpecRel::getProductType, productType));
        if (!rels.isEmpty()) {
            List<String> ids = rels.stream().map(ProductTypeSpecRel::getSpecsId).collect(Collectors.toList());
            List<ProductSpecs> typeSpecs = specsMapper.selectBatchIds(ids);
            typeSpecs.forEach(s -> map.put(s.getName(), s.getId()));
        }
        return map;
    }

    // --- Helper methods ---

    @SuppressWarnings("unchecked")
    private List<ProductDetailResponse.SpecValue> parseSpecsJson(String specsJson) {
        if (specsJson == null || specsJson.isBlank()) return List.of();
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, String>> list = mapper.readValue(specsJson, List.class);
            return list.stream().map(m -> {
                ProductDetailResponse.SpecValue sv = new ProductDetailResponse.SpecValue();
                sv.setName(m.get("name"));
                sv.setValueName(m.get("valueName"));
                return sv;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<ProductDetailResponse.SpecItem> buildSpecItems(List<ProductSpecs> specDefs) {
        // Group specs by name to build spec items
        Map<String, List<String>> specValuesMap = new LinkedHashMap<>();
        Map<String, Integer> specTypeMap = new LinkedHashMap<>();
        for (ProductSpecs spec : specDefs) {
            if (spec.getType() != null && spec.getType() == 1) { // SKU type only
                specTypeMap.put(spec.getName(), spec.getType());
                specValuesMap.computeIfAbsent(spec.getName(), k -> new ArrayList<>())
                        .addAll(spec.getInputOptions() != null ? spec.getInputOptions() : List.of());
            }
        }
        return specValuesMap.entrySet().stream().map(entry -> {
            ProductDetailResponse.SpecItem si = new ProductDetailResponse.SpecItem();
            si.setName(entry.getKey());
            si.setValues(entry.getValue().stream().map(v -> {
                ProductDetailResponse.SpecValue sv = new ProductDetailResponse.SpecValue();
                sv.setName(v);
                sv.setValueName(v);
                return sv;
            }).collect(Collectors.toList()));
            return si;
        }).collect(Collectors.toList());
    }
}
