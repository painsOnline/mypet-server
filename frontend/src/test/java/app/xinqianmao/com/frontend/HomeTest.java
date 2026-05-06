/**
 * File: HomeTest.java
 * Author: system
 * Date: 2026-05-04
 *
 * Home page test: banner and hot products.
 */
package app.xinqianmao.com.frontend;

import org.junit.jupiter.api.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HomeTest extends BaseFrontendTest {

    @BeforeAll
    static void setUp() {
        initDatabases();
    }

    @Test
    @Order(1)
    @DisplayName("Get banner with default distribution site")
    void getBanner() throws Exception {
        mockMvc.perform(memberGetNoAuth("/home/banner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.result").isArray());
    }

    @Test
    @Order(2)
    @DisplayName("Get banner with distributionSite=2")
    void getBannerType2() throws Exception {
        mockMvc.perform(memberGetNoAuth("/home/banner?distributionSite=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    @Order(3)
    @DisplayName("Get hot products with default pagination")
    void getHotProducts() throws Exception {
        mockMvc.perform(memberGetNoAuth("/home/hot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.result").exists());
    }

    @Test
    @Order(4)
    @DisplayName("Get hot products with page and pageSize")
    void getHotProductsWithPagination() throws Exception {
        mockMvc.perform(memberGetNoAuth("/home/hot?page=1&pageSize=3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.result.page").value(1))
                .andExpect(jsonPath("$.result.pageSize").value(3));
    }

    @Test
    @Order(5)
    @DisplayName("Home endpoints require tenant header")
    void missingTenantHeader() throws Exception {
        mockMvc.perform(memberGetNoAuth("/home/banner"))
                .andExpect(status().isBadRequest());
    }
}
