/**
 * File: AdminProductTypeTest.java
 * Author: system
 * Date: 2026-05-04
 *
 * Product type management closed-loop test: create → update → add specs → list → delete.
 */
package app.xinqianmao.com.admin;

import app.xinqianmao.com.admin.common.entity.*;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;

import org.junit.jupiter.api.Assertions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminProductTypeTest extends BaseAdminTest {

    @Autowired private AdminMapper adminMapper;
    @Autowired private ProductTypeMapper typeMapper;
    @Autowired private ProductSpecsMapper specsMapper;

    private static String typeId;
    private static String specId;

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

    @Test
    @Order(1)
    @DisplayName("List all product types")
    void listTypes() throws Exception {
        mockMvc.perform(adminGet("/admin/product/type"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.result").isArray());
    }

    @Test
    @Order(2)
    @DisplayName("Create product type")
    void createType() throws Exception {
        var body = mapOf("name", "猫粮类型", "sort", 1);
        var result = mockMvc.perform(adminPost("/admin/product/type", body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("200")).andReturn();
        // POST /admin/product/type returns Result<ProductType> — parse object, extract id
        typeId = (String) fromJson(extractResult(result), new TypeReference<Map<String, Object>>() {}).get("id");
        Assertions.assertNotNull(typeId);
        Assertions.assertEquals("猫粮类型", typeMapper.selectById(typeId).getName());
    }

    @Test
    @Order(3)
    @DisplayName("Update product type")
    void updateType() throws Exception {
        mockMvc.perform(adminPut("/admin/product/type/" + typeId, mapOf("name", "猫粮类型升级版", "sort", 2)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("200"));
        Assertions.assertEquals("猫粮类型升级版", typeMapper.selectById(typeId).getName());
    }

    @Test
    @Order(4)
    @DisplayName("Add specs to product type")
    void addSpecs() throws Exception {
        var body = mapOf(
                "name", "口味", "type", 1, "inputType", 3,
                "inputOptions", List.of("鸡肉味", "鱼肉味", "牛肉味"), "sort", 1);
        mockMvc.perform(adminPost("/admin/product/type/" + typeId + "/specs", body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("200"));
        // POST /admin/product/type/{typeId}/specs returns Result<Void> — query DB for created spec
        List<ProductSpecs> specs = specsMapper.selectList(
                new LambdaQueryWrapper<ProductSpecs>().eq(ProductSpecs::getProductType, typeId));
        Assertions.assertEquals(1, specs.size());
        specId = specs.get(0).getId();
        Assertions.assertNotNull(specId);
    }

    @Test
    @Order(5)
    @DisplayName("Delete specs")
    void deleteSpecs() throws Exception {
        mockMvc.perform(adminDelete("/admin/product/type/" + typeId + "/specs/" + specId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("200"));
        Assertions.assertNull(specsMapper.selectById(specId));
    }

    @Test
    @Order(6)
    @DisplayName("Delete empty product type")
    void deleteType() throws Exception {
        mockMvc.perform(adminDelete("/admin/product/type/" + typeId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("200"));
        Assertions.assertNull(typeMapper.selectById(typeId));
    }
}
