/**
 * File: MemberAddressTest.java
 * Author: system
 * Date: 2026-05-04
 *
 * Address management closed-loop test: list → create → detail → update → delete.
 */
package app.xinqianmao.com.frontend;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.*;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MemberAddressTest extends BaseFrontendTest {

    private static String addressId;

    @BeforeAll
    static void setUp() {
        initDatabases();
    }

    @BeforeEach
    void ensureLogin() throws Exception {
        if (testMemberId == null) {
            var req = mapOf("phoneNumber", TEST_MEMBER_PHONE);
            var result = mockMvc.perform(memberPostNoAuth("/frontend/member/login/wxMin/simple", req))
                    .andExpect(status().isOk()).andReturn();
            var resp = fromJson(extractResult(result), new TypeReference<Map<String, Object>>() {});
            testMemberId = (String) resp.get("id");
            testMemberToken = (String) resp.get("token");
        }
    }

    @Test
    @Order(1)
    @DisplayName("List all addresses (may be empty initially)")
    void listAddresses() throws Exception {
        mockMvc.perform(memberGet("/frontend/member/address", testMemberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.result").isArray());
    }

    @Test
    @Order(2)
    @DisplayName("Create a new address")
    void createAddress() throws Exception {
        var req = mapOf(
                "receiver", "曹某人",
                "contact", "15921769899",
                "provinceCode", "440000",
                "cityCode", "441300",
                "countyCode", "惠阳区",
                "address", "星河丹堤花园F区2栋3023",
                "isDefault", 1);

        var result = mockMvc.perform(memberPost("/frontend/member/address", req, testMemberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.result.id").exists())
                .andExpect(jsonPath("$.result.receiver").value("曹某人"))
                .andExpect(jsonPath("$.result.fullLocation").exists())
                .andReturn();

        var resp = fromJson(extractResult(result), new TypeReference<Map<String, Object>>() {});
        addressId = (String) resp.get("id");
        Assertions.assertNotNull(addressId);
    }

    @Test
    @Order(3)
    @DisplayName("Get address detail")
    void getAddressDetail() throws Exception {
        mockMvc.perform(memberGet("/frontend/member/address/" + addressId, testMemberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(addressId))
                .andExpect(jsonPath("$.result.receiver").value("曹某人"));
    }

    @Test
    @Order(4)
    @DisplayName("Update address")
    void updateAddress() throws Exception {
        var req = mapOf(
                "receiver", "李某人",
                "contact", "13812345678",
                "provinceCode", "110000",
                "cityCode", "110100",
                "countyCode", "朝阳区",
                "address", "朝阳公园路1号",
                "isDefault", 0);

        mockMvc.perform(memberPut("/frontend/member/address/" + addressId, req, testMemberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.receiver").value("李某人"))
                .andExpect(jsonPath("$.result.contact").value("13812345678"));
    }

    @Test
    @Order(5)
    @DisplayName("Delete address")
    void deleteAddress() throws Exception {
        mockMvc.perform(memberDelete("/frontend/member/address/" + addressId, testMemberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.result").value(addressId));

        // Verify deleted - get detail should fail
        mockMvc.perform(memberGet("/frontend/member/address/" + addressId, testMemberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("404"));
    }
}
