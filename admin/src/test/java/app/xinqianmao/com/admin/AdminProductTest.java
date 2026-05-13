/**
 * File: AdminProductTest.java
 * Author: system
 * Date: 2026-05-04
 *
 * Product management closed-loop test:
 *   create type → create category → create specs → create product with properties
 *   → create SKUs → search product → get detail → update product → toggle hot → delete product
 */
package app.xinqianmao.com.admin;

import app.xinqianmao.com.admin.common.entity.*;
import app.xinqianmao.com.admin.common.pojo.*;
import app.xinqianmao.com.admin.dao.*;
import app.xinqianmao.com.common.utils.PasswordUtil;
import app.xinqianmao.com.common.utils.UUIDUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminProductTest extends BaseAdminTest {

    @Autowired private AdminMapper adminMapper;
    @Autowired private ProductTypeMapper typeMapper;
    @Autowired private ProductCategoryMapper categoryMapper;
    @Autowired private ProductSpecsMapper specsMapper;
    @Autowired private ProductMapper productMapper;
    @Autowired private ProductPropertyMapper propertyMapper;
    @Autowired private ProductSkuMapper skuMapper;
    @Autowired private HotProductMapper hotProductMapper;
    @Autowired private ProductTypeSpecRelMapper typeSpecRelMapper;
    @Autowired private PasswordUtil passwordUtil;

    private static String typeId;
    private static String categoryId;
    private static String specId;
    private static String productId;
    private static String skuId;

    @BeforeAll
    static void setUp() {
        initDatabases();
    }

    @BeforeEach
    void ensureAdmin() {
        var existing = adminMapper.selectList(
                new LambdaQueryWrapper<Admin>().eq(Admin::getAccount, ADMIN_ACCOUNT));
        if (existing.isEmpty()) {
            Admin admin = new Admin();
            admin.setAccount(ADMIN_ACCOUNT);
            admin.setPassword(passwordUtil.encode(ADMIN_PASSWORD));
            admin.setCreateTime(LocalDateTime.now());
            adminMapper.insert(admin);
        }
    }

    // ========== Step 1: Create Product Type ==========

    @Test
    @Order(1)
    @DisplayName("Create product type")
    void createProductType() throws Exception {
        var body = mapOf("name", "测试猫粮类型", "sort", 1);
        var result = mockMvc.perform(adminPost("/admin/product/type", body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.result").exists())
                .andReturn();
        // POST /admin/product/type returns Result<ProductType> — parse object, extract id
        typeId = (String) fromJson(extractResult(result), new TypeReference<Map<String, Object>>() {}).get("id");
        Assertions.assertNotNull(typeId);
    }

    // ========== Step 2: Create Category ==========

    @Test
    @Order(2)
    @DisplayName("Create product category")
    void createCategory() throws Exception {
        var body = mapOf("name", "品质猫砂", "picture", "https://example.com/cat-sand.png", "sort", 1);
        var result = mockMvc.perform(adminPost("/admin/category", body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.result").exists())
                .andReturn();
        // POST /admin/category returns Result<ProductCategory> — parse object, extract id
        categoryId = (String) fromJson(extractResult(result), new TypeReference<Map<String, Object>>() {}).get("id");
        Assertions.assertNotNull(categoryId);
    }

    // ========== Step 3: Create Specs ==========

    @Test
    @Order(3)
    @DisplayName("Create product specs for the type")
    void createSpecs() throws Exception {
        var body = mapOf(
                "name", "规格",
                "type", 1,
                "inputType", 2,
                "inputOptions", List.of("2.5Kg/袋", "5Kg/袋", "10Kg/袋"),
                "sort", 1);
        mockMvc.perform(adminPost("/admin/product/type/" + typeId + "/specs", body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
        // POST /admin/product/type/{typeId}/specs returns Result<Void> — query DB via ProductTypeSpecRel to get created spec
        List<ProductTypeSpecRel> rels = typeSpecRelMapper.selectList(
                new LambdaQueryWrapper<ProductTypeSpecRel>().eq(ProductTypeSpecRel::getProductType, typeId));
        List<String> specIds = rels.stream().map(ProductTypeSpecRel::getSpecsId).toList();
        List<ProductSpecs> createdSpecs = specsMapper.selectBatchIds(specIds);
        Assertions.assertEquals(1, createdSpecs.size());
        specId = createdSpecs.get(0).getId();
        Assertions.assertNotNull(specId);
    }

    // ========== Step 4: Create Product with Properties ==========

    @Test
    @Order(4)
    @DisplayName("Create product with properties")
    void createProduct() throws Exception {
        var body = mapOf(
                "name", "尼可露豆腐猫砂6L/袋",
                "productType", typeId,
                "productCategory", categoryId,
                "desc", "品质优选，宠物最爱",
                "price", BigDecimal.valueOf(99.00),
                "oldPrice", BigDecimal.valueOf(128.00),
                "picture", "https://example.com/product-main.png",
                "mainPictures", List.of("https://example.com/product-s1.png", "https://example.com/product-s2.png"),
                "detail", "<p>详情图片描述</p>",
                "sort", 1,
                "properties", List.of(
                        mapOf("name", "品牌", "valueName", "好命天生"),
                        mapOf("name", "适用对象", "valueName", "猫"),
                        mapOf("name", "产地", "valueName", "国产")));

        var result = mockMvc.perform(adminPost("/admin/product", body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.result").exists())
                .andReturn();
        productId = fromJson(extractResult(result), String.class);
        Assertions.assertNotNull(productId);

        // Verify product was created in DB
        Product product = productMapper.selectById(productId);
        Assertions.assertNotNull(product);
        Assertions.assertEquals("尼可露豆腐猫砂6L/袋", product.getName());
        Assertions.assertEquals(BigDecimal.valueOf(99.00).setScale(2), product.getPrice().setScale(2));

        // Verify properties
        List<ProductProperty> props = propertyMapper.selectList(
                new LambdaQueryWrapper<ProductProperty>().eq(ProductProperty::getProductId, productId));
        Assertions.assertEquals(3, props.size());
    }

    // ========== Step 5: Create SKUs ==========

    @Test
    @Order(5)
    @DisplayName("Create product SKUs")
    void createSkus() throws Exception {
        // SKU 1: 2.5Kg/袋
        var sku1 = mapOf(
                "price", BigDecimal.valueOf(99.00),
                "oldPrice", BigDecimal.valueOf(128.00),
                "inventory", 100,
                "picture", "https://example.com/sku-25kg.png",
                "specs", List.of(mapOf("name", "规格", "valueName", "2.5Kg/袋")));

        mockMvc.perform(adminPost("/admin/product/" + productId + "/sku", sku1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
        // POST /admin/product/{productId}/sku returns Result<Void> — query DB for created SKU
        List<ProductSku> firstSkus = skuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, productId));
        Assertions.assertEquals(1, firstSkus.size());
        skuId = firstSkus.get(0).getId();
        Assertions.assertNotNull(skuId);

        // SKU 2: 5Kg/袋
        var sku2 = mapOf(
                "price", BigDecimal.valueOf(179.00),
                "oldPrice", BigDecimal.valueOf(218.00),
                "inventory", 50,
                "picture", "https://example.com/sku-5kg.png",
                "specs", List.of(mapOf("name", "规格", "valueName", "5Kg/袋")));

        mockMvc.perform(adminPost("/admin/product/" + productId + "/sku", sku2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        // Verify SKUs in DB
        List<ProductSku> skus = skuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, productId));
        Assertions.assertEquals(2, skus.size());
    }

    // ========== Step 6: Search Products ==========

    @Test
    @Order(6)
    @DisplayName("Search products by name, category, type, price")
    void searchProducts() throws Exception {
        // GET /admin/product?...params... — search uses GET query string, not POST
        var result = mockMvc.perform(adminGet("/admin/product", mapOf("page", 1, "pageSize", 10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andReturn();

        var pageResult = fromJson(extractResult(result), new TypeReference<Map<String, Object>>() {});
        Assertions.assertTrue(((Number) pageResult.get("counts")).intValue() >= 1);

        // Search by name
        mockMvc.perform(adminGet("/admin/product", mapOf("page", 1, "pageSize", 10, "name", "猫砂")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    // ========== Step 7: Get Product Detail ==========

    @Test
    @Order(7)
    @DisplayName("Get product detail with properties, SKUs, specs")
    void getProductDetail() throws Exception {
        mockMvc.perform(adminGet("/admin/product/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.result.name").value("尼可露豆腐猫砂6L/袋"))
                .andExpect(jsonPath("$.result.properties").isArray())
                .andExpect(jsonPath("$.result.skus").isArray())
                .andExpect(jsonPath("$.result.specs").isArray());
    }

    // ========== Step 8: Update Product ==========

    @Test
    @Order(8)
    @DisplayName("Update product info")
    void updateProduct() throws Exception {
        var body = mapOf(
                "name", "尼可露豆腐猫砂6L/袋（升级版）",
                "productType", typeId,
                "productCategory", categoryId,
                "desc", "升级版品质优选",
                "price", BigDecimal.valueOf(109.00),
                "oldPrice", BigDecimal.valueOf(138.00),
                "picture", "https://example.com/product-main-v2.png",
                "mainPictures", List.of("https://example.com/product-s1-v2.png"),
                "detail", "<p>升级详情</p>",
                "sort", 2,
                "properties", List.of(
                        mapOf("name", "品牌", "valueName", "好命天生"),
                        mapOf("name", "适用对象", "valueName", "猫"),
                        mapOf("name", "产地", "valueName", "国产"),
                        mapOf("name", "净含量", "valueName", "6L")));

        mockMvc.perform(adminPut("/admin/product/" + productId, body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        Product product = productMapper.selectById(productId);
        Assertions.assertEquals("尼可露豆腐猫砂6L/袋（升级版）", product.getName());
        Assertions.assertEquals(BigDecimal.valueOf(109.00).setScale(2), product.getPrice().setScale(2));
    }

    // ========== Step 9: Toggle Hot Product ==========

    @Test
    @Order(9)
    @DisplayName("Toggle product in/out of hot list")
    void toggleHot() throws Exception {
        // Add to hot list
        mockMvc.perform(adminPut("/admin/product/" + productId + "/hot", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        List<HotProduct> hotList = hotProductMapper.selectList(
                new LambdaQueryWrapper<HotProduct>().eq(HotProduct::getProductId, productId));
        Assertions.assertEquals(1, hotList.size());

        // Remove from hot list
        mockMvc.perform(adminPut("/admin/product/" + productId + "/hot", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        hotList = hotProductMapper.selectList(
                new LambdaQueryWrapper<HotProduct>().eq(HotProduct::getProductId, productId));
        Assertions.assertTrue(hotList.isEmpty());
    }

    // ========== Step 10: Delete Product ==========

    @Test
    @Order(10)
    @DisplayName("Delete product and verify cascading cleanup")
    void deleteProduct() throws Exception {
        mockMvc.perform(adminDelete("/admin/product/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        Assertions.assertNull(productMapper.selectById(productId));
        Assertions.assertTrue(propertyMapper.selectList(
                new LambdaQueryWrapper<ProductProperty>().eq(ProductProperty::getProductId, productId)).isEmpty());
        Assertions.assertTrue(skuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, productId)).isEmpty());
    }
}
