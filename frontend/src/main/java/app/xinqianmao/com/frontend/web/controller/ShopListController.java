/**
 * File: ShopListController.java
 * Author: system
 * Date: 2026-05-21
 *
 * Public shop listing and validation endpoints for mini-program shop selection.
 * These endpoints query config DB directly — no auth or tenant header required.
 */
package app.xinqianmao.com.frontend.web.controller;

import app.xinqianmao.com.common.annotation.NoAuth;
import app.xinqianmao.com.common.dao.TenantDataSourceManager;
import app.xinqianmao.com.common.result.Result;
import app.xinqianmao.com.common.utils.ImageUrlUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "店铺选择", description = "小程序端店铺列表和校验（公开接口）")
@RestController
@RequestMapping("/frontend/shop")
@RequiredArgsConstructor
public class ShopListController {

    private final TenantDataSourceManager tenantDataSourceManager;
    private final ImageUrlUtil imageUrlUtil;

    private DataSource getConfigDs() {
        return tenantDataSourceManager.getConfigDataSource();
    }

    @NoAuth
    @Operation(summary = "获取所有开业店铺列表")
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        String sql = "SELECT code, name FROM c_tenant WHERE is_disable = 0 AND is_bussiness_open = 1 ORDER BY create_time";
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection c = getConfigDs().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("code", rs.getString("code"));
                item.put("name", rs.getString("name"));
                item.put("logo", "");
                result.add(item);
            }
        } catch (Exception e) {
            log.error("Failed to list shops", e);
            return Result.error("500", "获取店铺列表失败");
        }
        return Result.ok(result);
    }

    @NoAuth
    @Operation(summary = "校验店铺Code是否有效")
    @GetMapping("/validate")
    public Result<Map<String, Object>> validate(@RequestParam String code) {
        String sql = "SELECT code, name, is_disable, is_bussiness_open FROM c_tenant WHERE code = ?";
        try (Connection c = getConfigDs().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("code", rs.getString("code"));
                    item.put("name", rs.getString("name"));
                    item.put("isDisable", rs.getInt("is_disable"));
                    item.put("isBussinessOpen", rs.getInt("is_bussiness_open"));
                    return Result.ok(item);
                }
            }
        } catch (Exception e) {
            log.error("Failed to validate shop code: {}", code, e);
            return Result.error("500", "校验店铺失败");
        }
        return Result.error("404", "店铺不存在");
    }
}
