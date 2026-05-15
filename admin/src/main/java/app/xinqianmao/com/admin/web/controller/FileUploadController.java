/**
 * File: FileUploadController.java
 * Author: system
 * Date: 2026-05-11
 *
 * Image upload with categorized storage:
 *   type=product/main|slider|sku|detail  -> products/{id}/{main|slider|sku|detail}/{yyyy/MM}/{uuid}.ext
 *   type=banner                          -> banners/{yyyy/MM}/{uuid}.ext
 *   type=category                        -> categories/{yyyy/MM}/{uuid}.ext
 *   type=brand                           -> brands/{yyyy/MM}/{uuid}.ext
 *   type=logo                            -> logos/{yyyy/MM}/{uuid}.ext
 *   type=temp/products|banners|categories|brands|logos -> temp/{subtype}/{yyyy/MM}/{uuid}.ext
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
@Tag(name = "文件上传", description = "图片上传接口，支持分类存储")
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

    @Operation(summary = "上传图片", description = "type参数指定分类，productId指定商品ID（商品图片必填）")
    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file,
                                      @RequestParam(defaultValue = "") String type,
                                      @RequestParam(required = false) String productId) throws IOException {
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

        String tenantCode = TenantContext.get();
        if (tenantCode == null || tenantCode.isBlank())
            return Result.error("400", "缺少租户信息");

        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String subPath = buildSubPath(type, productId, dateDir);
        Path targetDir = uploadRoot.resolve(tenantCode).resolve(subPath);
        Files.createDirectories(targetDir);

        String newName = UUID.randomUUID().toString().replace("-", "") + ext;
        file.transferTo(targetDir.resolve(newName));

        String url = "/uploads/" + tenantCode + "/" + subPath + "/" + newName;
        log.info("Image uploaded [{}] type={}: {} -> {}", tenantCode, type, originalName, url);
        return Result.ok(url);
    }

    /** Build the relative path (without tenant prefix) for both disk and URL. */
    static String buildSubPath(String type, String productId, String dateDir) {
        if (type == null || type.isBlank()) return dateDir;

        switch (type) {
            case "product/main":
            case "product/slider":
            case "product/sku":
            case "product/detail":
                if (productId == null || productId.isBlank())
                    throw new RuntimeException("商品图片上传需要productId参数");
                return "products/" + productId + "/" + type.substring(8) + "/" + dateDir;
            case "banner":   return "banners/" + dateDir;
            case "category": return "categories/" + dateDir;
            case "brand":    return "brands/" + dateDir;
            case "logo":     return "logos/" + dateDir;
            case "temp/products":  return "temp/products/" + dateDir;
            case "temp/banners":   return "temp/banners/" + dateDir;
            case "temp/categories": return "temp/categories/" + dateDir;
            case "temp/brands":    return "temp/brands/" + dateDir;
            case "temp/logos":     return "temp/logos/" + dateDir;
            default: return dateDir;
        }
    }
}
