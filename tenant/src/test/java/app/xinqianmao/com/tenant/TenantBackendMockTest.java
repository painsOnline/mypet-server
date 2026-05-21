/**
 * File: TenantBackendMockTest.java
 * Author: system
 * Date: 2026-05-21
 *
 * Integration tests for tenant management backend services and controllers.
 * Uses real PostgreSQL with mock data (test tenant, test admin).
 */
package app.xinqianmao.com.tenant;

import app.xinqianmao.com.common.service.CaptchaService;
import app.xinqianmao.com.tenant.common.pojo.*;
import app.xinqianmao.com.tenant.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
    classes = app.xinqianmao.com.tenant.web.TenantApplication.class,
    properties = {
        "spring.profiles.active=dev",
        "mypet.db.host=127.0.0.1",
        "mypet.db.port=1800",
        "mypet.db.user=postgres",
        "mypet.db.password=mypg123abc"
    }
)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TenantBackendMockTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantLoginService loginService;
    @Autowired private TenantService tenantService;
    @Autowired private TenantAdminService adminService;
    @Autowired private CaptchaService captchaService;

    private static String authToken;
    private static String testTenantId;
    private static String testAdminId;
    private static final String TEST_TENANT_CODE = "testmock";
    private static final String SUPER_ACCOUNT = "super";
    private static final String SUPER_PASSWORD = "mysuper123abc+-";

    // ============================================================
    // 1. Login & Auth Tests
    // ============================================================

    @Test @Order(1)
    @DisplayName("Login with valid super admin credentials via service")
    void loginValid() {
        TenantLoginRequest req = new TenantLoginRequest();
        req.setAccount(SUPER_ACCOUNT);
        req.setPassword(SUPER_PASSWORD);
        String token = loginService.login(req);
        assertNotNull(token, "Token must not be null");
        assertFalse(token.isBlank(), "Token must not be blank");
        authToken = token;
    }

    @Test @Order(2)
    @DisplayName("Login with wrong password returns 401")
    void loginWrongPassword() {
        TenantLoginRequest req = new TenantLoginRequest();
        req.setAccount(SUPER_ACCOUNT);
        req.setPassword("wrongpassword");
        try {
            loginService.login(req);
            fail("Should throw BizException");
        } catch (app.xinqianmao.com.common.exception.BizException e) {
            assertEquals("401", e.getCode());
        }
    }

    @Test @Order(3)
    @DisplayName("Login with non-existent account returns 401")
    void loginWrongAccount() {
        TenantLoginRequest req = new TenantLoginRequest();
        req.setAccount("nonexistent_user_xyz");
        req.setPassword("anything");
        try {
            loginService.login(req);
            fail("Should throw BizException");
        } catch (app.xinqianmao.com.common.exception.BizException e) {
            assertEquals("401", e.getCode());
        }
    }

    @Test @Order(4)
    @DisplayName("Get admin info for existing account")
    void getAdminInfo() {
        var admin = loginService.getAdminInfo(SUPER_ACCOUNT);
        assertNotNull(admin, "Admin must exist");
        assertEquals(SUPER_ACCOUNT, admin.getAccount());
        assertNull(admin.getPassword(), "Password must be masked");
    }

    @Test @Order(5)
    @DisplayName("Get admin info for unknown account throws 404")
    void getAdminInfoNotFound() {
        try {
            loginService.getAdminInfo("no_such_admin");
            fail("Should throw BizException");
        } catch (app.xinqianmao.com.common.exception.BizException e) {
            assertEquals("404", e.getCode());
        }
    }

    @Test @Order(6)
    @DisplayName("Login via POST /tenant/login returns token")
    void loginEndpoint() throws Exception {
        TenantLoginRequest req = new TenantLoginRequest();
        req.setAccount(SUPER_ACCOUNT);
        req.setPassword(SUPER_PASSWORD);

        String resp = mockMvc.perform(post("/tenant/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.result.token").exists())
                .andReturn().getResponse().getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> result = objectMapper.readValue(resp, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get("result");
        authToken = (String) data.get("token");
        assertNotNull(authToken);
    }

    @Test @Order(7)
    @DisplayName("GET /tenant/captcha returns image and token")
    void captchaEndpoint() throws Exception {
        mockMvc.perform(get("/tenant/captcha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.result.token").exists())
                .andExpect(jsonPath("$.result.image").exists());
    }

    // ============================================================
    // 2. Password Change Tests
    // ============================================================

    @Test @Order(8)
    @DisplayName("Change password: wrong old password fails")
    void changePasswordWrongOld() {
        try {
            loginService.changePassword(SUPER_ACCOUNT, "wrong_old", "newpass123");
            fail("Should throw BizException");
        } catch (app.xinqianmao.com.common.exception.BizException e) {
            assertEquals("400", e.getCode());
        }
    }

    @Test @Order(9)
    @DisplayName("Change password: valid change then revert back")
    void changePasswordRoundTrip() {
        String tempPassword = "tempPwd123+-";
        // Change super → temp
        loginService.changePassword(SUPER_ACCOUNT, SUPER_PASSWORD, tempPassword);
        // Verify can login with new password
        TenantLoginRequest req = new TenantLoginRequest();
        req.setAccount(SUPER_ACCOUNT);
        req.setPassword(tempPassword);
        String token = loginService.login(req);
        assertNotNull(token);

        // Change back temp → super
        loginService.changePassword(SUPER_ACCOUNT, tempPassword, SUPER_PASSWORD);
        // Verify original password works again
        req.setPassword(SUPER_PASSWORD);
        token = loginService.login(req);
        assertNotNull(token);
    }

    @Test @Order(10)
    @DisplayName("PUT /tenant/password via endpoint")
    void changePasswordEndpoint() throws Exception {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword(SUPER_PASSWORD);
        req.setNewPassword(SUPER_PASSWORD); // same password is fine

        mockMvc.perform(put("/tenant/password")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    // ============================================================
    // 3. Tenant CRUD Tests
    // ============================================================

    @Test @Order(11)
    @DisplayName("List all tenants includes xlong")
    void listTenants() {
        List<TenantListResponse> list = tenantService.listAll();
        assertNotNull(list);
        assertTrue(list.size() >= 1, "Should have at least xlong tenant");
        boolean hasXlong = list.stream().anyMatch(t -> "xlong".equals(t.getCode()));
        assertTrue(hasXlong, "Must contain xlong tenant");
    }

    @Test @Order(12)
    @DisplayName("Get tenant by ID")
    void getTenantById() {
        List<TenantListResponse> list = tenantService.listAll();
        assertFalse(list.isEmpty());
        String firstId = list.get(0).getId();
        TenantListResponse tenant = tenantService.getById(firstId);
        assertNotNull(tenant);
        assertEquals(firstId, tenant.getId());
    }

    @Test @Order(13)
    @DisplayName("Get tenant by non-existent ID throws 404")
    void getTenantNotFound() {
        try {
            tenantService.getById("00000000-0000-0000-0000-000000000000");
            fail("Should throw BizException");
        } catch (app.xinqianmao.com.common.exception.BizException e) {
            assertEquals("404", e.getCode());
        }
    }

    @Test @Order(14)
    @DisplayName("Create tenant with invalid code format")
    void createTenantInvalidCode() {
        TenantSaveRequest req = new TenantSaveRequest();
        req.setCode("invalid code with spaces!");
        req.setName("Test");
        req.setDatabaseInstanceId("00000000-0000-0000-0000-000000000001");
        try {
            tenantService.create(req);
            fail("Should throw BizException");
        } catch (app.xinqianmao.com.common.exception.BizException e) {
            assertEquals("400", e.getCode());
        }
    }

    @Test @Order(15)
    @DisplayName("Create tenant with duplicate code xlong")
    void createTenantDuplicateCode() {
        TenantSaveRequest req = new TenantSaveRequest();
        req.setCode("xlong"); // already exists
        req.setName("Duplicate");
        req.setDatabaseInstanceId("00000000-0000-0000-0000-000000000001");
        try {
            tenantService.create(req);
            fail("Should throw BizException");
        } catch (app.xinqianmao.com.common.exception.BizException e) {
            assertEquals("400", e.getCode());
        }
    }

    @Test @Order(16)
    @DisplayName("Create new test tenant successfully")
    void createTenant() {
        TenantSaveRequest req = new TenantSaveRequest();
        req.setCode(TEST_TENANT_CODE);
        req.setName("Mock Test Tenant");
        req.setDatabaseInstanceId("00000000-0000-0000-0000-000000000001");
        req.setIsDisable(0);
        req.setFreeShippingAmount(new java.math.BigDecimal("25.00"));

        TenantListResponse created = tenantService.create(req);
        assertNotNull(created);
        assertEquals(TEST_TENANT_CODE, created.getCode());
        assertEquals("Mock Test Tenant", created.getName());
        testTenantId = created.getId();
        assertNotNull(testTenantId);
    }

    @Test @Order(17)
    @DisplayName("Verify new tenant DB has all required tables cloned from empty template")
    void verifyNewTenantDbSchema() throws Exception {
        assertNotNull(testTenantId, "testTenantId must be set from createTenant");
        String dbName = "mypet_" + TEST_TENANT_CODE;

        // Connect directly to the new tenant database
        String url = "jdbc:postgresql://127.0.0.1:1800/" + dbName;
        try (java.sql.Connection c = java.sql.DriverManager.getConnection(url, "postgres", "mypg123abc");
             java.sql.Statement stmt = c.createStatement()) {

            // Get all table names in public schema
            java.sql.ResultSet rs = stmt.executeQuery(
                "SELECT table_name FROM information_schema.tables WHERE table_schema='public' ORDER BY table_name");
            java.util.List<String> tables = new java.util.ArrayList<>();
            while (rs.next()) tables.add(rs.getString(1));
            System.out.println("Tables in " + dbName + ": " + tables);

            // Core business tables that must exist
            String[] requiredTables = {
                "t_product_category", "t_product_type", "t_product_specs", "t_product_specs_value",
                "t_product_type_spec_rel", "t_product_brand", "t_product", "t_product_properties",
                "t_product_sku", "t_inventory_log", "t_order", "t_order_products", "t_order_product_skus",
                "t_order_product_properties", "t_order_receiver", "t_member", "t_receiver",
                "t_cart", "t_shop", "t_admin", "t_hot_products",
                "c_admin_login_error_log", "c_admin_login_lock"
            };
            for (String table : requiredTables) {
                assertTrue(tables.contains(table), "Missing table: " + table);
            }

            // t_shop table exists but may be empty (shop config set by admin later)
            rs = stmt.executeQuery("SELECT COUNT(*) FROM t_shop");
            assertTrue(rs.next(), "t_shop must exist and be queryable");

            // Verify t_admin has seed data (default admin for shop management)
            rs = stmt.executeQuery("SELECT COUNT(*) FROM t_admin");
            assertTrue(rs.next());
            int adminCount = rs.getInt(1);
            assertTrue(adminCount > 0, "t_admin must have default admin from empty template");

            // Verify key table columns
            // t_product_sku must have specs JSONB column
            rs = stmt.executeQuery("SELECT data_type FROM information_schema.columns " +
                "WHERE table_name='t_product_sku' AND column_name='specs'");
            assertTrue(rs.next());
            assertEquals("jsonb", rs.getString(1), "t_product_sku.specs must be jsonb");

            // t_order must have is_delete column
            rs = stmt.executeQuery("SELECT data_type FROM information_schema.columns " +
                "WHERE table_name='t_order' AND column_name='is_delete'");
            assertTrue(rs.next(), "t_order must have is_delete column");

            // c_admin_login_error_log must NOT have tenant_code
            rs = stmt.executeQuery("SELECT column_name FROM information_schema.columns " +
                "WHERE table_name='c_admin_login_error_log' AND column_name='tenant_code'");
            assertFalse(rs.next(), "c_admin_login_error_log must NOT have tenant_code per database.md");

            // c_admin_login_error_log must have modify_time column
            rs = stmt.executeQuery("SELECT column_name FROM information_schema.columns " +
                "WHERE table_name='c_admin_login_error_log' AND column_name='modify_time'");
            assertTrue(rs.next(), "c_admin_login_error_log must have modify_time column");
        }
    }

    @Test @Order(18)
    @DisplayName("Update tenant name and status")
    void updateTenant() {
        assertNotNull(testTenantId, "testTenantId must be set from createTenant");
        TenantSaveRequest req = new TenantSaveRequest();
        req.setCode(TEST_TENANT_CODE);
        req.setName("Mock Test Tenant Updated");
        req.setDatabaseInstanceId("00000000-0000-0000-0000-000000000001");
        req.setIsDisable(1); // disable
        req.setFreeShippingAmount(new java.math.BigDecimal("30.00"));

        TenantListResponse updated = tenantService.update(testTenantId, req);
        assertEquals("Mock Test Tenant Updated", updated.getName());
        assertEquals(1, updated.getIsDisable());
    }

    @Test @Order(19)
    @DisplayName("List database instances")
    void listDatabaseInstances() {
        var list = tenantService.getDatabaseInstances();
        assertNotNull(list);
        assertTrue(list.size() >= 1, "Should have at least 1 DB instance");
    }

    @Test @Order(20)
    @DisplayName("GET /tenant/tenants via endpoint")
    void listTenantsEndpoint() throws Exception {
        mockMvc.perform(get("/tenant/tenants")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.result").isArray());
    }

    @Test @Order(27)
    @DisplayName("GET /tenant/database-instances via endpoint")
    void databaseInstancesEndpoint() throws Exception {
        mockMvc.perform(get("/tenant/database-instances")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.result").isArray());
    }

    // ============================================================
    // 4. Admin Management Tests
    // ============================================================

    @Test @Order(21)
    @DisplayName("List admin users includes super")
    void listAdmins() {
        var list = adminService.listAll();
        assertNotNull(list);
        assertTrue(list.size() >= 1, "Should have at least super admin");
        boolean hasSuper = list.stream().anyMatch(a -> SUPER_ACCOUNT.equals(a.getAccount()));
        assertTrue(hasSuper, "Must contain super admin");
        // All passwords must be masked
        for (var admin : list) {
            assertNull(admin.getPassword(), "Password must be masked for " + admin.getAccount());
        }
    }

    @Test @Order(22)
    @DisplayName("Create new admin via service")
    void createAdmin() {
        TenantAdminSaveRequest req = new TenantAdminSaveRequest();
        req.setAccount("testadmin");
        req.setPassword("test123abc+-");

        var created = adminService.create(req);
        assertNotNull(created);
        assertEquals("testadmin", created.getAccount());
        testAdminId = created.getId();
        assertNotNull(testAdminId);
    }

    @Test @Order(23)
    @DisplayName("Create duplicate admin fails")
    void createDuplicateAdmin() {
        TenantAdminSaveRequest req = new TenantAdminSaveRequest();
        req.setAccount("testadmin"); // already created
        req.setPassword("test123abc+-");
        try {
            adminService.create(req);
            fail("Should throw BizException");
        } catch (app.xinqianmao.com.common.exception.BizException e) {
            assertEquals("400", e.getCode());
        }
    }

    @Test @Order(24)
    @DisplayName("Delete admin succeeds when multiple admins exist")
    void deleteAdmin() {
        assertNotNull(testAdminId, "testAdminId must be set from createAdmin");
        adminService.delete(testAdminId);
        // Verify deleted
        var list = adminService.listAll();
        boolean hasTestAdmin = list.stream().anyMatch(a -> "testadmin".equals(a.getAccount()));
        assertFalse(hasTestAdmin, "Test admin must be deleted");
    }

    @Test @Order(25)
    @DisplayName("Delete last admin fails")
    void deleteLastAdmin() {
        var list = adminService.listAll();
        if (list.size() <= 1) {
            // Try to delete the last admin
            String lastId = list.get(0).getId();
            try {
                adminService.delete(lastId);
                fail("Should throw BizException when deleting last admin");
            } catch (app.xinqianmao.com.common.exception.BizException e) {
                assertEquals("400", e.getCode());
            }
        }
    }

    @Test @Order(26)
    @DisplayName("GET /tenant/admins via endpoint")
    void listAdminsEndpoint() throws Exception {
        mockMvc.perform(get("/tenant/admins")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.result").isArray());
    }

    // ============================================================
    // 5. Auth Guard Tests (no token = 401)
    // ============================================================

    @Test @Order(31)
    @DisplayName("Protected endpoint without token returns 401")
    void noAuthReturns401() throws Exception {
        String content = mockMvc.perform(get("/tenant/tenants"))
                .andReturn().getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = objectMapper.readValue(content, Map.class);
        assertEquals("401", resp.get("code"), "Must reject unauthenticated request");
    }

    @Test @Order(32)
    @DisplayName("Protected endpoint with invalid token returns 401")
    void invalidTokenReturns401() throws Exception {
        String content = mockMvc.perform(get("/tenant/tenants")
                .header("Authorization", "Bearer invalid_token_here"))
                .andReturn().getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = objectMapper.readValue(content, Map.class);
        assertEquals("401", resp.get("code"), "Must reject invalid token");
    }

    // ============================================================
    // 6. Cleanup — delete test tenant
    // ============================================================

    @Test @Order(99)
    @DisplayName("Delete test tenant (cleanup)")
    void deleteTenant() {
        assertNotNull(testTenantId, "testTenantId must be set from createTenant");
        tenantService.delete(testTenantId);
        // Verify deleted
        try {
            tenantService.getById(testTenantId);
            fail("Tenant should be deleted");
        } catch (app.xinqianmao.com.common.exception.BizException e) {
            assertEquals("404", e.getCode());
        }
    }
}
