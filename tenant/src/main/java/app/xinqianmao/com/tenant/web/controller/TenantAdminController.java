/**
 * File: TenantAdminController.java
 * Author: system
 * Date: 2026-05-21
 */
package app.xinqianmao.com.tenant.web.controller;

import app.xinqianmao.com.common.result.Result;
import app.xinqianmao.com.tenant.common.entity.TenantAdmin;
import app.xinqianmao.com.tenant.common.pojo.TenantAdminSaveRequest;
import app.xinqianmao.com.tenant.service.TenantAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Tenant admin account management controller.
 * CRUD operations for tenant management admin accounts.
 */
@Tag(name = "租户管理-管理员", description = "租户管理平台管理员账号管理")
@RestController
@RequestMapping("/tenant")
@RequiredArgsConstructor
public class TenantAdminController {

    private final TenantAdminService tenantAdminService;

    @Operation(summary = "获取所有管理员列表")
    @GetMapping("/admins")
    public Result<List<TenantAdmin>> listAll() {
        return Result.ok(tenantAdminService.listAll());
    }

    @Operation(summary = "创建管理员", description = "创建新的租户管理平台管理员账号")
    @PostMapping("/admins")
    public Result<TenantAdmin> create(@Valid @RequestBody TenantAdminSaveRequest request) {
        return Result.ok(tenantAdminService.create(request));
    }

    @Operation(summary = "删除管理员", description = "删除管理员账号，至少保留一个管理员")
    @DeleteMapping("/admins/{id}")
    public Result<Void> delete(@PathVariable String id) {
        tenantAdminService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "修改管理员密码", description = "管理员修改其他管理员的密码（无需旧密码）")
    @PutMapping("/admins/{id}/password")
    public Result<Void> changePassword(@PathVariable String id, @RequestBody java.util.Map<String, String> body) {
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.isBlank()) {
            return app.xinqianmao.com.common.result.Result.error("400", "新密码不能为空");
        }
        tenantAdminService.changePassword(id, newPassword);
        return Result.ok();
    }
}
