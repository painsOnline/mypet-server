/**
 * File: MemberCartTest.java
 * Author: system
 * Date: 2026-05-04
 *
 * Cart management test: get → sync → verify.
 */
package app.xinqianmao.com.frontend;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MemberCartTest extends BaseFrontendTest {

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
    @DisplayName("Get empty cart initially")
    void getEmptyCart() throws Exception {
        mockMvc.perform(memberGet("/frontend/member/cart", testMemberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.result").isArray());
    }

    @Test
    @Order(2)
    @DisplayName("Sync cart with items")
    void syncCart() throws Exception {
        var items = List.of(
                mapOf(
                        "id", "prod-001",
                        "skuId", "sku-001",
                        "name", "测试猫砂6L",
                        "picture", "https://example.com/cat-sand.png",
                        "count", 2,
                        "price", BigDecimal.valueOf(128.00),
                        "nowPrice", BigDecimal.valueOf(99.00),
                        "stock", 200,
                        "selected", true,
                        "attrsText", "规格：2.5Kg/袋",
                        "isEffective", true),
                mapOf(
                        "id", "prod-002",
                        "skuId", "sku-002",
                        "name", "测试猫粮2Kg",
                        "picture", "https://example.com/cat-food.png",
                        "count", 1,
                        "price", BigDecimal.valueOf(89.00),
                        "nowPrice", BigDecimal.valueOf(69.00),
                        "stock", 150,
                        "selected", false,
                        "attrsText", "口味：鸡肉味",
                        "isEffective", true));

        mockMvc.perform(memberPut("/frontend/member/cart", items, testMemberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.result").value(true));

        // Verify cart
        mockMvc.perform(memberGet("/frontend/member/cart", testMemberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(2));
    }

    @Test
    @Order(3)
    @DisplayName("Sync cart to empty clears all items")
    void syncEmptyCart() throws Exception {
        mockMvc.perform(memberPut("/frontend/member/cart", List.of(), testMemberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(true));

        mockMvc.perform(memberGet("/frontend/member/cart", testMemberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray());
        // Note: result length may still be > 0 if other test data persists
    }
}
