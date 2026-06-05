/**
 * File: MigrationController.java
 * Author: system
 * Date: 2026-05-21
 */
package app.xinqianmao.com.tenant.web.controller;

import app.xinqianmao.com.common.result.Result;
import app.xinqianmao.com.tenant.service.MigrationRunnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "数据库迁移管理", description = "查看和执行数据库迁移脚本")
@RestController
@RequestMapping("/tenant/migration")
@RequiredArgsConstructor
public class MigrationController {

    private final MigrationRunnerService migrationRunnerService;

    @Operation(summary = "查看迁移状态")
    @GetMapping("/status")
    public Result<List<Map<String, Object>>> status() {
        return Result.ok(migrationRunnerService.listMigrations());
    }

    @Operation(summary = "执行所有待处理迁移")
    @PostMapping("/run")
    public Result<List<Map<String, Object>>> runAll() {
        return Result.ok(migrationRunnerService.runAllPending());
    }

    @Operation(summary = "执行指定迁移")
    @PostMapping("/run/{name:.+}")
    public Result<Map<String, Object>> runOne(@PathVariable String name) {
        return Result.ok(migrationRunnerService.runMigration(name));
    }

    @Operation(summary = "异步执行所有待处理迁移")
    @PostMapping("/run/async")
    public Result<String> runAsync() {
        migrationRunnerService.runAllAsync();
        return Result.ok("迁移已在后台开始执行，请通过 /status 查看进度");
    }
}
