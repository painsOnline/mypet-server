/**
 * File: AdminLoginTest.java
 * Author: system
 * Date: 2026-05-04
 */
package app.xinqianmao.com.admin;

import app.xinqianmao.com.admin.common.entity.Admin;
import app.xinqianmao.com.admin.common.pojo.AdminLoginRequest;
import app.xinqianmao.com.admin.common.pojo.ChangePasswordRequest;
import app.xinqianmao.com.admin.dao.AdminMapper;
import app.xinqianmao.com.common.utils.PasswordUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Admin login closed-loop test.
 * Covers: login success, wrong password, password change, re-login with new password.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminLoginTest extends BaseAdminTest {

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private PasswordUtil passwordUtil;

    private static String adminToken;

    @BeforeAll
    static void setUp() {
        initDatabases();
    }

    @BeforeEach
    void ensureAdminExists() {
        // Insert a test admin with known password hash (SHA-256+salt)
        var existing = adminMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Admin>()
                        .eq(Admin::getAccount, ADMIN_ACCOUNT));
        if (existing.isEmpty()) {
            Admin admin = new Admin();
            admin.setAccount(ADMIN_ACCOUNT);
            admin.setPassword(passwordUtil.encode(ADMIN_PASSWORD));
            admin.setCreateTime(java.time.LocalDateTime.now());
            adminMapper.insert(admin);
        }
    }

    @Test
    @Order(1)
    @DisplayName("Login with correct credentials should succeed")
    void loginSuccess() throws Exception {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setAccount(ADMIN_ACCOUNT);
        req.setPassword(ADMIN_PASSWORD);

        MvcResult result = mockMvc.perform(
                post("/admin/login")
                        .header("Tenant", TENANT_CODE)
                        .contentType("application/json")
                        .content(toJson(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.result.token").exists())
                .andExpect(jsonPath("$.result.account").value(ADMIN_ACCOUNT))
                .andReturn();

        // Extract token for later use
        String resultJson = extractResult(result);
        var resp = fromJson(resultJson,
                app.xinqianmao.com.admin.common.pojo.AdminLoginResponse.class);
        adminToken = resp.getToken();
    }

    @Test
    @Order(2)
    @DisplayName("Login with wrong password should return 401")
    void loginWrongPassword() throws Exception {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setAccount(ADMIN_ACCOUNT);
        req.setPassword("wrongpassword");

        mockMvc.perform(
                post("/admin/login")
                        .header("Tenant", TENANT_CODE)
                        .contentType("application/json")
                        .content(toJson(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("401"));
    }

    @Test
    @Order(3)
    @DisplayName("Login with non-existent account should return 401")
    void loginNonExistent() throws Exception {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setAccount("nonexistent");
        req.setPassword("whatever");

        mockMvc.perform(
                post("/admin/login")
                        .header("Tenant", TENANT_CODE)
                        .contentType("application/json")
                        .content(toJson(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("401"));
    }

    @Test
    @Order(4)
    @DisplayName("Change password and re-login with new password")
    void changePasswordAndReLogin() throws Exception {
        // Generate fresh token for current admin
        String currentToken = adminToken();

        // Change password
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword(ADMIN_PASSWORD);
        req.setNewPassword("newpass456");

        mockMvc.perform(
                put("/admin/password")
                        .header("Tenant", TENANT_CODE)
                        .header("Authorization", "Bearer " + currentToken)
                        .contentType("application/json")
                        .content(toJson(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        // Login with new password should succeed
        AdminLoginRequest loginReq = new AdminLoginRequest();
        loginReq.setAccount(ADMIN_ACCOUNT);
        loginReq.setPassword("newpass456");

        mockMvc.perform(
                post("/admin/login")
                        .header("Tenant", TENANT_CODE)
                        .contentType("application/json")
                        .content(toJson(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.result.token").exists());

        // Login with old password should fail
        loginReq.setPassword(ADMIN_PASSWORD);
        mockMvc.perform(
                post("/admin/login")
                        .header("Tenant", TENANT_CODE)
                        .contentType("application/json")
                        .content(toJson(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("401"));

        // Restore old password
        ChangePasswordRequest restoreReq = new ChangePasswordRequest();
        restoreReq.setOldPassword("newpass456");
        restoreReq.setNewPassword(ADMIN_PASSWORD);

        mockMvc.perform(
                put("/admin/password")
                        .header("Tenant", TENANT_CODE)
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType("application/json")
                        .content(toJson(restoreReq)))
                .andExpect(status().isOk());
    }

    @Test
    @Order(5)
    @DisplayName("Missing Tenant header should return 400")
    void missingTenantHeader() throws Exception {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setAccount(ADMIN_ACCOUNT);
        req.setPassword(ADMIN_PASSWORD);

        mockMvc.perform(
                post("/admin/login")
                        .contentType("application/json")
                        .content(toJson(req)))
                .andExpect(status().isBadRequest());
    }
}
