/**
 * File: GoodsTest.java
 * Author: system
 * Date: 2026-05-04
 *
 * Product/frontend/goods detail test.
 */
package app.xinqianmao.com.frontend;

import org.junit.jupiter.api.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GoodsTest extends BaseFrontendTest {

    @BeforeAll
    static void setUp() {
        initDatabases();
    }

    @Test
    @Order(1)
    @DisplayName("Get product detail by id")
    void getProductDetail() throws Exception {
        // Use a known non-existent ID first to test 404
        mockMvc.perform(memberGetNoAuth("/frontend/goods?id=non-existent-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("404"));
    }

    @Test
    @Order(2)
    @DisplayName("Missing id parameter returns 400")
    void missingId() throws Exception {
        mockMvc.perform(memberGetNoAuth("/frontend/goods"))
                .andExpect(status().isBadRequest());
    }
}
