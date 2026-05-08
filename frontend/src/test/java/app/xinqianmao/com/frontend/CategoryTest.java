/**
 * File: CategoryTest.java
 * Author: system
 * Date: 2026-05-04
 *
 * Category browsing test: list categories and browse products by category.
 */
package app.xinqianmao.com.frontend;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.*;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CategoryTest extends BaseFrontendTest {

    private static String existingCategoryId;

    @BeforeAll
    static void setUp() {
        initDatabases();
    }

    @BeforeEach
    void ensureCategoryData() throws Exception {
        // First try to get an existing category
        var result = mockMvc.perform(memberGetNoAuth("/frontend/category/list"))
                .andExpect(status().isOk()).andReturn();

        String resultJson = extractResult(result);
        List<Map<String, Object>> cats = fromJson(resultJson,
                new TypeReference<List<Map<String, Object>>>() {});

        if (!cats.isEmpty()) {
            existingCategoryId = (String) cats.get(0).get("id");
        }
    }

    @Test
    @Order(1)
    @DisplayName("List all categories")
    void listCategories() throws Exception {
        mockMvc.perform(memberGetNoAuth("/frontend/category/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.result").isArray());
    }

    @Test
    @Order(2)
    @DisplayName("Get products by category")
    void productsByCategory() throws Exception {
        if (existingCategoryId == null) return;

        mockMvc.perform(memberGetNoAuth("/frontend/category/product/list?id=" + existingCategoryId + "&page=1&pageSize=6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.result.items").isArray());
    }

    @Test
    @Order(3)
    @DisplayName("Category product list with pagination")
    void productsByCategoryWithPage() throws Exception {
        if (existingCategoryId == null) return;

        mockMvc.perform(memberGetNoAuth("/frontend/category/product/list?id=" + existingCategoryId + "&page=1&pageSize=3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.pageSize").value(3));
    }

    @Test
    @Order(4)
    @DisplayName("Missing category id parameter")
    void missingCategoryId() throws Exception {
        mockMvc.perform(memberGetNoAuth("/frontend/category/product/list?page=1&pageSize=6"))
                .andExpect(status().isBadRequest());
    }
}
