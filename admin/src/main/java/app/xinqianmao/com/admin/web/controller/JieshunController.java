/**
 * File: JieshunController.java
 * Author: system
 * Date: 2026-05-12
 *
 * Proxy controller for importing product data from the 街顺 (waisongbang.com) system.
 * Proxies search and detail requests server-side.
 */
package app.xinqianmao.com.admin.web.controller;

import app.xinqianmao.com.common.result.Result;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Slf4j
@Tag(name = "街顺导入", description = "从街顺系统导入商品数据")
@RestController
@RequestMapping("/admin/jieshun")
@RequiredArgsConstructor
public class JieshunController {

    private final ObjectMapper objectMapper;

    private static final String BASE_URL = "https://product-service.waisongbang.com/api/storeProduct";

    @Operation(summary = "搜索街顺商品", description = "根据条形码搜索街顺商品列表")
    @PostMapping("/search")
    public Result<Map<String, Object>> search(@RequestBody Map<String, String> body) {
        String accessToken = body.get("accessToken");
        String storeId = body.get("storeId");
        String vendorId = body.get("vendorId");
        String kw = body.get("kw");

        if (accessToken == null || accessToken.isBlank()) return Result.error("400", "access_token不能为空");
        if (kw == null || kw.isBlank()) return Result.error("400", "条形码不能为空");

        try {
            String ts = String.valueOf(System.currentTimeMillis() / 1000);
            String url = BASE_URL + "/list?page=1&pageSize=20"
                    + "&access_token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8)
                    + "&kw=" + URLEncoder.encode(kw, StandardCharsets.UTF_8)
                    + "&store_id=" + (storeId != null ? storeId : "1289567")
                    + "&vendor_id=" + (vendorId != null ? vendorId : "5406")
                    + "&_=" + ts;

            log.info("Jieshun search: kw={}, storeId={}, vendorId={}", kw, storeId, vendorId);

            JsonNode result = doGet(url);
            return parseJieshunResponse(result);

        } catch (Exception e) {
            log.error("Jieshun search failed: {}", e.getMessage(), e);
            return Result.error("500", "请求街顺接口异常：" + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    @Operation(summary = "获取街顺商品详情", description = "根据商品ID获取街顺商品详情及SKU列表")
    @PostMapping("/detail")
    public Result<Map<String, Object>> detail(@RequestBody Map<String, String> body) {
        String accessToken = body.get("accessToken");
        String productId = body.get("productId");

        if (accessToken == null || accessToken.isBlank()) return Result.error("400", "access_token不能为空");
        if (productId == null || productId.isBlank()) return Result.error("400", "商品ID不能为空");

        try {
            String ts = String.valueOf(System.currentTimeMillis() / 1000);
            String url = BASE_URL + "/" + productId
                    + "?access_token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8)
                    + "&_=" + ts;

            log.info("Jieshun detail: productId={}", productId);

            JsonNode result = doGet(url);
            return parseJieshunResponse(result);

        } catch (Exception e) {
            log.error("Jieshun detail failed: {}", e.getMessage(), e);
            return Result.error("500", "请求街顺接口异常：" + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    private JsonNode doGet(String url) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36")
                .header("Accept", "application/json, text/plain, */*")
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        int httpStatus = response.statusCode();
        String respBody = response.body();
        log.info("Jieshun response: HTTP {}, bodyLen={}", httpStatus, respBody != null ? respBody.length() : 0);

        if (httpStatus != 200) {
            String preview = respBody != null && respBody.length() > 500 ? respBody.substring(0, 500) : respBody;
            throw new RuntimeException("街顺接口返回HTTP " + httpStatus + "，响应：" + preview);
        }

        JsonNode node = objectMapper.readTree(respBody);
        return node;
    }

    private Result<Map<String, Object>> parseJieshunResponse(JsonNode result) {
        if (!result.path("ok").asBoolean()) {
            String reason = result.path("reason").asText();
            String errorCode = result.path("error_code").asText();
            String detail = (reason != null && !reason.isBlank()) ? reason : "街顺接口返回失败";
            if (errorCode != null && !errorCode.isBlank()) detail += " (error_code=" + errorCode + ")";
            log.warn("Jieshun ok=false: {}", detail);
            return Result.error("500", detail);
        }

        JsonNode obj = result.path("obj");
        Map<String, Object> data = objectMapper.convertValue(obj, Map.class);
        return Result.ok(data);
    }
}
