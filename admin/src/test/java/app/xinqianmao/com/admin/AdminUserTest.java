/**
 * File: AdminUserTest.java
 * Author: system
 * Date: 2026-05-04
 *
 * User management test: search users by phone and time range.
 */
package app.xinqianmao.com.admin;

import app.xinqianmao.com.admin.common.entity.*;
import app.xinqianmao.com.admin.dao.*;
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
class AdminUserTest extends BaseAdminTest {

    @Autowired private AdminMapper adminMapper;
    @Autowired private MemberMapper memberMapper;
    @Autowired private ReceiverMapper receiverMapper;
    @Autowired private ProductTypeMapper typeMapper;
    @Autowired private ProductCategoryMapper categoryMapper;
    @Autowired private ProductMapper productMapper;
    @Autowired private ProductSkuMapper skuMapper;
    @Autowired private OrderMapper orderMapper;
    @Autowired private OrderProductMapper orderProductMapper;
    @Autowired private OrderProductSkuMapper orderProductSkuMapper;

    private static String testMemberId;

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
        if (testMemberId == null) {
            testMemberId = java.util.UUID.randomUUID().toString();
            Member member = new Member();
            member.setId(testMemberId); member.setAccount("13912345678");
            member.setMobile("13912345678"); member.setAvatar("https://example.com/avatar.png");
            member.setNickname("测试用户139");
            member.setCreateTime(LocalDateTime.now()); memberMapper.insert(member);
        }
    }

    @Test
    @Order(1)
    @DisplayName("List users with pagination")
    void listUsers() throws Exception {
        // GET /admin/user?...params... — search uses GET query string, not POST
        var req = mapOf("page", Long.valueOf(1), "pageSize", Long.valueOf(10));
        var result = mockMvc.perform(adminGet("/admin/user", req))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200")).andReturn();
        var pageResult = fromJson(extractResult(result), new TypeReference<Map<String, Object>>() {});
        Assertions.assertTrue(((Number) pageResult.get("counts")).intValue() >= 1);
        Assertions.assertTrue(pageResult.containsKey("items"));
    }

    @Test
    @Order(2)
    @DisplayName("Search users by phone")
    void searchByPhone() throws Exception {
        var req = mapOf("page", Long.valueOf(1), "pageSize", Long.valueOf(10), "phone", "139");
        mockMvc.perform(adminGet("/admin/user", req))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("200"));
    }
}
