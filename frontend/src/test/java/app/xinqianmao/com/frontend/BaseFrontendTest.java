/**
 * File: BaseFrontendTest.java
 * Author: system
 * Date: 2026-05-04
 */
package app.xinqianmao.com.frontend;

import app.xinqianmao.com.common.DbInitializer;
import app.xinqianmao.com.common.auth.JwtUtil;
import app.xinqianmao.com.common.auth.TenantContext;
import app.xinqianmao.com.common.utils.PasswordUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Base test class for frontend module tests.
 * Provides common utilities: MockMvc, JWT helpers, JSON helpers, DB init.
 */
@SpringBootTest(
    classes = app.xinqianmao.com.frontend.web.FrontendApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
public abstract class BaseFrontendTest {

    protected static final String TENANT_CODE = "xlong";
    protected static final String TEST_MEMBER_PHONE = "13800000001";
    protected static String testMemberId;  // set after first login
    protected static String testMemberToken; // set after first login

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JwtUtil jwtUtil;

    @Autowired
    protected PasswordUtil passwordUtil;

    protected static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerModule(new JavaTimeModule());

    @BeforeAll
    static void initDatabases() {
        DbInitializer.ensureDatabases();
    }

    @BeforeEach
    void setTenantContext() {
        TenantContext.set(TENANT_CODE);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    /** Generate a member JWT token */
    protected String memberToken(String userId) {
        return jwtUtil.generateToken(userId, TENANT_CODE, false);
    }

    /** Helper to create a HashMap from alternating key-value pairs */
    @SuppressWarnings("unchecked")
    protected static Map<String, Object> mapOf(Object... kv) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((String) kv[i], kv[i + 1]);
        }
        return map;
    }

    // ---- JSON helpers ----

    protected String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected <T> T fromJson(String json, TypeReference<T> typeRef) {
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Extract result field from unified response {code, msg, result} */
    protected String extractResult(MvcResult mvcResult) throws UnsupportedEncodingException {
        String body = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        try {
            var node = objectMapper.readTree(body);
            if (node.has("result") && !node.get("result").isNull()) {
                return objectMapper.writeValueAsString(node.get("result"));
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    protected String extractCode(MvcResult mvcResult) throws UnsupportedEncodingException {
        String body = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        try {
            return objectMapper.readTree(body).get("code").asText();
        } catch (Exception e) {
            return null;
        }
    }

    // ---- Request builders ----

    /** Build a GET request with tenant + auth headers for a specific user */
    protected MockHttpServletRequestBuilder memberGet(String url, String token) {
        return get(url)
                .header("Tenant", TENANT_CODE)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON);
    }

    /** Build a POST request with tenant + auth headers */
    protected MockHttpServletRequestBuilder memberPost(String url, Object body, String token) {
        return post(url)
                .header("Tenant", TENANT_CODE)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(body));
    }

    /** Build a PUT request with tenant + auth headers */
    protected MockHttpServletRequestBuilder memberPut(String url, Object body, String token) {
        return put(url)
                .header("Tenant", TENANT_CODE)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(body));
    }

    /** Build a DELETE request with tenant + auth headers */
    protected MockHttpServletRequestBuilder memberDelete(String url, String token) {
        return delete(url)
                .header("Tenant", TENANT_CODE)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON);
    }

    /** Build a POST request with tenant header only (no auth — for login) */
    protected MockHttpServletRequestBuilder memberPostNoAuth(String url, Object body) {
        return post(url)
                .header("Tenant", TENANT_CODE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(body));
    }

    /** Build a GET request with tenant header only (no auth — for public endpoints) */
    protected MockHttpServletRequestBuilder memberGetNoAuth(String url) {
        return get(url)
                .header("Tenant", TENANT_CODE)
                .contentType(MediaType.APPLICATION_JSON);
    }
}
