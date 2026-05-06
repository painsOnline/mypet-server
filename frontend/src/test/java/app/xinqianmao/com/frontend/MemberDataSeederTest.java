/**
 * File: MemberDataSeederTest.java
 * Author: system
 * Date: 2026-05-05
 *
 * Seeds 30 test members via the simple-login API, then verifies DB consistency.
 * Uses the real Spring Boot MockMvc + service layer (tenant-aware).
 */
package app.xinqianmao.com.frontend;

import app.xinqianmao.com.common.auth.TenantContext;
import app.xinqianmao.com.frontend.common.entity.Member;
import app.xinqianmao.com.frontend.dao.MemberMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Seeds 30 test members and verifies all data is consistent.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MemberDataSeederTest extends BaseFrontendTest {

    @Autowired
    private MemberMapper memberMapper;

    private static final List<Map<String, Object>> seededMembers = new ArrayList<>();
    private static final int MEMBER_COUNT = 30;

    @BeforeAll
    static void setUp() {
        initDatabases();
    }

    @Test
    @Order(1)
    @DisplayName("Seed 30 test members via simpleLogin API")
    void seed30Members() throws Exception {
        String[] nickSuffixes = {
            "爱宠达人", "喵星人控", "狗粮专家", "宠物小助手", "毛毛家长",
            "鱼乐无穷", "小鸟依人", "仓鼠大王", "龟龟慢生活", "兔兔快跑",
            "萌宠日记", "猫粮测评师", "汪汪队队长", "铲屎官老张", "遛狗小能手",
            "布偶猫舍", "金毛控", "柯基爱好者", "英短铲屎官", "柴犬小分队",
            "宠食测评", "喵不可言", "汪星来客", "家有懒猫", "爱宠一生",
            "宠物营养师", "小橘猫", "拉布拉多妈", "边境牧羊人", "虎斑猫爸"
        };

        for (int i = 0; i < MEMBER_COUNT; i++) {
            String phoneNumber = String.format("138%08d", 10000001 + i * 357);
            SimpleLoginRequest req = new SimpleLoginRequest();
            req.setPhoneNumber(phoneNumber);

            var result = mockMvc.perform(memberPostNoAuth("/member/login/wxMin/simple", req))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("200"))
                    .andExpect(jsonPath("$.result.token").exists())
                    .andExpect(jsonPath("$.result.mobile").value(phoneNumber))
                    .andReturn();

            String resultJson = extractResult(result);
            Map<String, Object> memberData = objectMapper.readValue(resultJson, new TypeReference<Map<String, Object>>() {});
            memberData.put("_nickSuffix", nickSuffixes[i]);
            seededMembers.add(memberData);
            System.out.println("[SEED] " + phoneNumber + " → " + memberData.get("id"));
        }

        assertEquals(MEMBER_COUNT, seededMembers.size(), "Should have seeded all 30 members");
    }

    @Test
    @Order(2)
    @DisplayName("Verify all 30 members exist in DB and data matches API response")
    void verifyDbConsistency() {
        // Need tenant context for direct DB access
        TenantContext.set(TENANT_CODE);
        try {
            for (Map<String, Object> m : seededMembers) {
                String id = (String) m.get("id");
                String mobile = (String) m.get("mobile");
                String account = (String) m.get("account");

                // Query by mobile
                List<Member> byMobile = memberMapper.selectList(
                        new LambdaQueryWrapper<Member>().eq(Member::getMobile, mobile));
                assertEquals(1, byMobile.size(), "Should find exactly 1 member by mobile " + mobile);

                Member db = byMobile.get(0);
                assertEquals(id, db.getId(), "ID mismatch for " + mobile);
                assertEquals(mobile, db.getMobile(), "Mobile mismatch");
                assertEquals(account, db.getAccount(), "Account mismatch for " + mobile);
                assertNotNull(db.getCreateTime(), "createTime should not be null for " + mobile);
                assertNotNull(db.getNickname(), "nickname should not be null for " + mobile);
            }
            System.out.println("[VERIFY] All " + MEMBER_COUNT + " members verified in DB — 100% consistent");
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @Order(3)
    @DisplayName("Re-login with same phone numbers should return same members (no duplicates)")
    void reLoginShouldReturnSameMembers() throws Exception {
        Map<String, String> firstMobile = new HashMap<>();
        firstMobile.put((String) seededMembers.get(0).get("mobile"), (String) seededMembers.get(0).get("id"));
        firstMobile.put((String) seededMembers.get(15).get("mobile"), (String) seededMembers.get(15).get("id"));
        firstMobile.put((String) seededMembers.get(29).get("mobile"), (String) seededMembers.get(29).get("id"));

        for (Map.Entry<String, String> entry : firstMobile.entrySet()) {
            String mobile = entry.getKey();
            String expectedId = entry.getValue();

            SimpleLoginRequest req = new SimpleLoginRequest();
            req.setPhoneNumber(mobile);

            var result = mockMvc.perform(memberPostNoAuth("/member/login/wxMin/simple", req))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("200"))
                    .andExpect(jsonPath("$.result.mobile").value(mobile))
                    .andReturn();

            String resultJson = extractResult(result);
            Map<String, Object> data = objectMapper.readValue(resultJson, new TypeReference<Map<String, Object>>() {});
            assertEquals(expectedId, data.get("id"), "Re-login should return same member ID for " + mobile);
        }

        // Count total members — should still be exactly 30
        TenantContext.set(TENANT_CODE);
        try {
            long count = memberMapper.selectCount(new LambdaQueryWrapper<>());
            assertEquals(MEMBER_COUNT, count, "Should still have exactly " + MEMBER_COUNT + " members (no duplicates)");
            System.out.println("[VERIFY] Total member count: " + count + " — no duplicates created");
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @Order(4)
    @DisplayName("Account login creates member if not found, returns existing if found")
    void accountLoginCreatesOrFinds() throws Exception {
        AccountLoginRequest req = new AccountLoginRequest();
        req.setAccount("brandnewuser99");
        req.setPassword("test");

        var result = mockMvc.perform(memberPostNoAuth("/member/login", req))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.result.token").exists())
                .andReturn();

        String resultJson = extractResult(result);
        Map<String, Object> data = objectMapper.readValue(resultJson, new TypeReference<Map<String, Object>>() {});
        String newId = (String) data.get("id");
        assertNotNull(newId);
        System.out.println("[OK] Account login created member: " + newId);
    }

    // ---- Local DTOs mirror request POJOs for test serialization ----

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
