/**
 * File: HotProductService.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.service;

import app.xinqianmao.com.admin.common.entity.HotProduct;
import app.xinqianmao.com.admin.common.entity.Product;
import app.xinqianmao.com.admin.common.pojo.HotProductResponse;
import app.xinqianmao.com.admin.common.pojo.HotSortRequest;
import app.xinqianmao.com.admin.dao.HotProductMapper;
import app.xinqianmao.com.admin.dao.ProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Hot products management.
 */
@Service
@RequiredArgsConstructor
public class HotProductService {

    private final HotProductMapper hotProductMapper;
    private final ProductMapper productMapper;

    /**
     * List hot products with product details.
     */
    public List<HotProductResponse> listAll() {
        List<HotProduct> hotList = hotProductMapper.selectList(
                new LambdaQueryWrapper<HotProduct>().orderByAsc(HotProduct::getSort));

        // Load product info
        List<String> productIds = hotList.stream().map(HotProduct::getProductId).toList();
        if (productIds.isEmpty()) return List.of();

        Map<String, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        return hotList.stream().map(hp -> {
            HotProductResponse r = new HotProductResponse();
            r.setProductId(hp.getProductId());
            r.setSort(hp.getSort());
            Product p = productMap.get(hp.getProductId());
            // Only include enabled products (is_enable = 1)
            if (p != null && p.getIsEnable() != null && p.getIsEnable() == 1) {
                r.setProductName(p.getName());
                r.setPicture(p.getPicture());
                r.setPrice(p.getPrice());
                r.setOldPrice(p.getOldPrice());
                return r;
            }
            return null;
        }).filter(r -> r != null).collect(Collectors.toList());
    }

    /**
     * Reorder hot products.
     */
    @Transactional
    public void updateSort(HotSortRequest req) {
        // Remove existing and re-insert with new sort
        hotProductMapper.delete(new LambdaQueryWrapper<>());
        if (req.getItems() != null) {
            for (HotSortRequest.SortItem item : req.getItems()) {
                HotProduct hp = new HotProduct();
                hp.setProductId(item.getProductId());
                hp.setSort(item.getSort() != null ? item.getSort() : 0);
                hp.setCreateTime(java.time.LocalDateTime.now(app.xinqianmao.com.common.utils.DateTimeUtil.ZONE_BEIJING));
                hotProductMapper.insert(hp);
            }
        }
    }
}
