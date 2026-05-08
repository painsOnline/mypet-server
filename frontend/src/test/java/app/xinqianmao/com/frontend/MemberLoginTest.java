/**
 * File: MemberLoginTest.java
 * Author: system
 * Date: 2026-05-04
 *
 * Member login closed-loop test: simple login → account login → verify token.
 */
package app.xinqianmao.com.frontend;

import org.junit.jupiter.api.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MemberLoginTest extends BaseFrontendTest {

    @BeforeAll
    static void setUp() {
        initDatabases();
    }

    @Test
    @Order(1)
    @DisplayName("Quick login by phone should create member and return token")
    void simpleLogin() throws Exception {
        SimpleLoginRequest req = new SimpleLoginRequest();
        req.setPhoneNumber(TEST_MEMBER_PHONE);

        var result = mockMvc.perform(memberPostNoAuth("/frontend/member/login/wxMin/simple", req))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.result.token").exists())
                .andExpect(jsonPath("$.result.mobile").value(TEST_MEMBER_PHONE))
                .andReturn();

        String resultJson = extractResult(result);
        var resp = fromJson(resultJson, app.xinqianmao.com.frontend.common.pojo.MemberLoginResponse.class);
        testMemberId = resp.getId();
        testMemberToken = resp.getToken();
        Assertions.assertNotNull(testMemberId);
        Assertions.assertNotNull(testMemberToken);
    }

    @Test
    @Order(2)
    @DisplayName("Login by same phone should find existing member")
    void loginAgainShouldFindExisting() throws Exception {
        SimpleLoginRequest req = new SimpleLoginRequest();
        req.setPhoneNumber(TEST_MEMBER_PHONE);

        mockMvc.perform(memberPostNoAuth("/frontend/member/login/wxMin/simple", req))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.mobile").value(TEST_MEMBER_PHONE));
    }

    @Test
    @Order(3)
    @DisplayName("Account login creates or finds member")
    void accountLogin() throws Exception {
        AccountLoginRequest req = new AccountLoginRequest();
        req.setAccount("13900001111");
        req.setPassword("test");

        mockMvc.perform(memberPostNoAuth("/frontend/member/login", req))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.result.token").exists())
                .andExpect(jsonPath("$.result.account").value("13900001111"));
    }

    @Test
    @Order(4)
    @DisplayName("Auth-required endpoint without token should return 401")
    void missingAuth() throws Exception {
        mockMvc.perform(
                get("/frontend/member/cart")
                        .header("Tenant", TENANT_CODE))
                .andExpect(status().isUnauthorized());
    }

    // ---- DTO classes for JSON serialization ----

    // These match the actual POJO field names used in the controller

    static class SimpleLoginRequest {
        private String phoneNumber;
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    }

    static class AccountLoginRequest {
        private String account;
        private String password;
        public String getAccount() { return account; }
        public void setAccount(String account) { this.account = account; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
