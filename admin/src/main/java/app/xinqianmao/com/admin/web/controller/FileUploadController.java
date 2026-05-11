/**
 * File: FileUploadController.java
 * Author: system
 * Date: 2026-05-11
 *
 * File upload controller for admin management backend.
 * Images are stored with tenant isolation: uploads/{tenantCode}/{dateDir}/{uuid}.ext
 */
package app.xinqianmao.com.admin.web.controller;

import app.xinqianmao.com.common.auth.TenantContext;
import app.xinqianmao.com.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Tag(name = "文件上传", description = "图片上传接口")
@RestController
@RequestMapping("/admin/upload")
public class FileUploadController {

    private final Path uploadRoot;

    public FileUploadController(@Value("${mypet.upload.path:uploads}") String uploadPath) {
        this.uploadRoot = Paths.get(uploadPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadRoot);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create upload directory: " + uploadRoot, e);
        }
    }

    @Operation(summary = "上传图片", description = "支持拖拽上传，图片按租户隔离存储，返回访问URL")
    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) return Result.error("400", "文件不能为空");

        String originalName = file.getOriginalFilename();
        if (originalName == null) return Result.error("400", "文件名不能为空");

        String ext = "";
        int dot = originalName.lastIndexOf('.');
        if (dot > 0) ext = originalName.substring(dot).toLowerCase();
        if (!ext.matches("\\.(jpg|jpeg|png|gif|webp|bmp)$"))
            return Result.error("400", "不支持的图片格式: " + ext);

        if (file.getSize() > 10 * 1024 * 1024)
            return Result.error("400", "图片大小不能超过10MB");

        // Tenant isolation: uploads/{tenantCode}/{dateDir}/{uuid}.ext
        String tenantCode = TenantContext.get();
        if (tenantCode == null || tenantCode.isBlank())
            return Result.error("400", "缺少租户信息");

        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path targetDir = uploadRoot.resolve(tenantCode).resolve(dateDir);
        Files.createDirectories(targetDir);

        String newName = UUID.randomUUID().toString().replace("-", "") + ext;
        Path targetFile = targetDir.resolve(newName);
        file.transferTo(targetFile);

        String url = "/uploads/" + tenantCode + "/" + dateDir + "/" + newName;
        log.info("Image uploaded [{}]: {} -> {}", tenantCode, originalName, url);
        return Result.ok(url);
    }
}
