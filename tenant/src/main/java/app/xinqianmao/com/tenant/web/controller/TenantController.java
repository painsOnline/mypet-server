/**
 * File: TenantController.java
 * Author: system
 * Date: 2026-05-21
 */
package app.xinqianmao.com.tenant.web.controller;

import app.xinqianmao.com.common.result.Result;
import app.xinqianmao.com.tenant.common.entity.DatabaseInstance;
import app.xinqianmao.com.tenant.common.pojo.TenantListResponse;
import app.xinqianmao.com.tenant.common.pojo.TenantSaveRequest;
import app.xinqianmao.com.tenant.service.TenantService;
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
 * Tenant management controller.
 * CRUD operations for tenants and database instances.
 */
@Tag(name = "租户管理", description = "租户和数据库实例管理")
@RestController
@RequestMapping("/tenant")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @Operation(summary = "获取所有租户列表")
    @GetMapping("/tenants")
    public Result<List<TenantListResponse>> listAll() {
        return Result.ok(tenantService.listAll());
    }

    @Operation(summary = "获取单个租户详情")
    @GetMapping("/tenants/{id}")
    public Result<TenantListResponse> getById(@PathVariable String id) {
        return Result.ok(tenantService.getById(id));
    }

    @Operation(summary = "创建租户", description = "创建新租户并初始化数据库")
    @PostMapping("/tenants")
    public Result<TenantListResponse> create(@Valid @RequestBody TenantSaveRequest request) {
        return Result.ok(tenantService.create(request));
    }

    @Operation(summary = "更新租户", description = "更新租户基本信息，不支持修改code和数据库实例")
    @PutMapping("/tenants/{id}")
    public Result<TenantListResponse> update(@PathVariable String id,
                                              @Valid @RequestBody TenantSaveRequest request) {
        return Result.ok(tenantService.update(id, request));
    }

    @Operation(summary = "删除租户", description = "删除租户及其数据库，需确保租户下无订单和商品")
    @DeleteMapping("/tenants/{id}")
    public Result<Void> delete(@PathVariable String id) {
        tenantService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "获取数据库实例列表")
    @GetMapping("/database-instances")
    public Result<List<DatabaseInstance>> listDatabaseInstances() {
        return Result.ok(tenantService.getDatabaseInstances());
    }
}
