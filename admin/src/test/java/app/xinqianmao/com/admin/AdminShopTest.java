/**
 * File: AdminShopTest.java
 * Author: system
 * Date: 2026-05-04
 *
 * Shop settings and hot products management test.
 */
package app.xinqianmao.com.admin;

import app.xinqianmao.com.admin.common.entity.*;
import app.xinqianmao.com.admin.common.pojo.HotSortRequest;
import app.xinqianmao.com.admin.dao.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminShopTest extends BaseAdminTest {

    @Autowired private AdminMapper adminMapper;
    @Autowired private ShopMapper shopMapper;
    @Autowired private HotProductMapper hotProductMapper;
    @Autowired private ProductMapper productMapper;
    @Autowired private ProductTypeMapper typeMapper;
    @Autowired private ProductCategoryMapper categoryMapper;

    private static String testProductId;

    @BeforeAll
    static void setUp() {
        initDatabases();
    }

    @BeforeEach
    void ensureData() {
        var existing = adminMapper.selectList(
                new LambdaQueryWrapper<Admin>().eq(Admin::getAccount, ADMIN_ACCOUNT));
        if (existing.isEmpty()) {
            Admin admin = new Admin();
            admin.setAccount(ADMIN_ACCOUNT);
            admin.setPassword(passwordUtil.encode(ADMIN_PASSWORD));
            admin.setCreateTime(LocalDateTime.now());
            adminMapper.insert(admin);
        }
        if (testProductId == null) {
            var prods = productMapper.selectList(new LambdaQueryWrapper<>());
            if (!prods.isEmpty()) {
                testProductId = prods.get(0).getId();
            } else {
                String tId = java.util.UUID.randomUUID().toString();
                ProductType t = new ProductType(); t.setId(tId); t.setName("shop测试类型"); t.setSort(0);
                t.setCreateTime(LocalDateTime.now()); typeMapper.insert(t);
                String cId = java.util.UUID.randomUUID().toString();
                ProductCategory cat = new ProductCategory(); cat.setId(cId); cat.setName("shop测试分类"); cat.setSort(0);
                cat.setCreateTime(LocalDateTime.now()); categoryMapper.insert(cat);
                testProductId = java.util.UUID.randomUUID().toString();
                app.xinqianmao.com.admin.common.entity.Product prod = new app.xinqianmao.com.admin.common.entity.Product();
                prod.setId(testProductId); prod.setName("shop测试商品"); prod.setProductType(tId);
                prod.setProductCategory(cId); prod.setPrice(new BigDecimal("10.00")); prod.setOldPrice(new BigDecimal("15.00"));
                prod.setPicture(""); prod.setMainPictures(List.of()); prod.setDetail(""); prod.setSort(0);
                prod.setCreateTime(LocalDateTime.now()); productMapper.insert(prod);
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("Get shop config")
    void getShopConfig() throws Exception {
        mockMvc.perform(adminGet("/admin/shop"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    @Order(2)
    @DisplayName("Update shop config")
    void updateShopConfig() throws Exception {
        var body = mapOf(
                "name", "宠物用品社区店（测试）",
                "logo", "https://example.com/logo.png",
                "freeShippingAmount", BigDecimal.valueOf(30.00),
                "banners", List.of("https://example.com/banner1.png", "https://example.com/banner2.png"));
        mockMvc.perform(adminPut("/admin/shop", body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("200"));
        List<Shop> shops = shopMapper.selectList(new LambdaQueryWrapper<>());
        Assertions.assertFalse(shops.isEmpty());
        Assertions.assertEquals("宠物用品社区店（测试）", shops.get(0).getName());
    }

    @Test
    @Order(3)
    @DisplayName("Get hot products list")
    void getHotProducts() throws Exception {
        mockMvc.perform(adminGet("/admin/hot"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    @Order(4)
    @DisplayName("Manage hot products: add, update sort, remove")
    void manageHotProducts() throws Exception {
        // Toggle product into hot list
        mockMvc.perform(adminPut("/admin/product/" + testProductId + "/hot", null))
                .andExpect(status().isOk());
        var hotList = hotProductMapper.selectList(
                new LambdaQueryWrapper<HotProduct>().eq(HotProduct::getProductId, testProductId));
        Assertions.assertFalse(hotList.isEmpty());

        // Update hot product sort order
        HotSortRequest sortReq = new HotSortRequest();
        HotSortRequest.SortItem item = new HotSortRequest.SortItem();
        item.setProductId(testProductId);
        item.setSort(99);
        sortReq.setItems(List.of(item));
        mockMvc.perform(adminPut("/admin/hot/sort", sortReq))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("200"));

        // Toggle off
        mockMvc.perform(adminPut("/admin/product/" + testProductId + "/hot", null))
                .andExpect(status().isOk());
    }
}
