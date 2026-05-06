/**
 * File: AdminCategoryTest.java
 * Author: system
 * Date: 2026-05-04
 *
 * Category management closed-loop test: list → create → update → delete.
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
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;

import org.junit.jupiter.api.Assertions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminCategoryTest extends BaseAdminTest {

    @Autowired private AdminMapper adminMapper;
    @Autowired private ProductCategoryMapper categoryMapper;

    private static String categoryId;

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
    @DisplayName("List all categories")
    void listCategories() throws Exception {
        mockMvc.perform(adminGet("/admin/category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.result").isArray());
    }

    @Test
    @Order(2)
    @DisplayName("Create category")
    void createCategory() throws Exception {
        var body = mapOf("name", "品牌猫粮", "picture", "https://example.com/cat-food.png", "sort", 2);
        var result = mockMvc.perform(adminPost("/admin/category", body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("200")).andReturn();
        // POST /admin/category returns Result<ProductCategory> — parse object, extract id
        categoryId = (String) fromJson(extractResult(result), new TypeReference<Map<String, Object>>() {}).get("id");
        Assertions.assertNotNull(categoryId);
        Assertions.assertEquals("品牌猫粮", categoryMapper.selectById(categoryId).getName());
    }

    @Test
    @Order(3)
    @DisplayName("Update category")
    void updateCategory() throws Exception {
        var body = mapOf("name", "品牌猫粮（升级）", "picture", "https://example.com/cat-food-v2.png", "sort", 3);
        mockMvc.perform(adminPut("/admin/category/" + categoryId, body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("200"));
        Assertions.assertEquals("品牌猫粮（升级）", categoryMapper.selectById(categoryId).getName());
    }

    @Test
    @Order(4)
    @DisplayName("Delete empty category")
    void deleteCategory() throws Exception {
        mockMvc.perform(adminDelete("/admin/category/" + categoryId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("200"));
        Assertions.assertNull(categoryMapper.selectById(categoryId));
    }
}
