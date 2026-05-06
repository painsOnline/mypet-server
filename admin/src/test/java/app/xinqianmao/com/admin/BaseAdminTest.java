/**
 * File: BaseAdminTest.java
 * Author: system
 * Date: 2026-05-04
 */
package app.xinqianmao.com.admin;

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
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * Base test class for admin module tests.
 * Provides common utilities: MockMvc, JWT helpers, JSON helpers, DB init.
 */
@SpringBootTest(
    classes = app.xinqianmao.com.admin.web.AdminApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
public abstract class BaseAdminTest {

    protected static final String TENANT_CODE = "xlong";
    protected static final String ADMIN_ACCOUNT = "testadmin";
    protected static final String ADMIN_PASSWORD = "test123456";

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

    /** Generate an admin JWT token for test tenant */
    protected String adminToken() {
        return jwtUtil.generateToken(ADMIN_ACCOUNT, TENANT_CODE, true);
    }

    /** Generate a user JWT token for test tenant */
    protected String userToken(String userId) {
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

    /** Build a GET request with tenant + auth headers */
    protected MockHttpServletRequestBuilder adminGet(String url) {
        return get(url)
                .header("Tenant", TENANT_CODE)
                .header("Authorization", "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON);
    }

    /** Build a GET request with query string parameters */
    protected MockHttpServletRequestBuilder adminGet(String url, Map<String, Object> params) {
        var builder = get(url)
                .header("Tenant", TENANT_CODE)
                .header("Authorization", "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON);
        if (params != null) {
            params.forEach((k, v) -> builder.param(k, v != null ? String.valueOf(v) : ""));
        }
        return builder;
    }

    /** Build a POST request with tenant + auth headers */
    protected MockHttpServletRequestBuilder adminPost(String url, Object body) {
        return post(url)
                .header("Tenant", TENANT_CODE)
                .header("Authorization", "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(body));
    }

    /** Build a PUT request with tenant + auth headers */
    protected MockHttpServletRequestBuilder adminPut(String url, Object body) {
        return put(url)
                .header("Tenant", TENANT_CODE)
                .header("Authorization", "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(body));
    }

    /** Build a DELETE request with tenant + auth headers */
    protected MockHttpServletRequestBuilder adminDelete(String url) {
        return delete(url)
                .header("Tenant", TENANT_CODE)
                .header("Authorization", "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON);
    }
}
