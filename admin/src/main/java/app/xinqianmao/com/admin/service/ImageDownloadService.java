/**
 * File: ImageDownloadService.java
 * Author: system
 * Date: 2026-05-12
 */
package app.xinqianmao.com.admin.service;

import app.xinqianmao.com.admin.common.entity.Product;
import app.xinqianmao.com.admin.common.entity.ProductSku;
import app.xinqianmao.com.admin.dao.ProductMapper;
import app.xinqianmao.com.admin.dao.ProductSkuMapper;
import app.xinqianmao.com.common.auth.TenantContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ImageDownloadService {

    private final ProductMapper productMapper;
    private final ProductSkuMapper skuMapper;
    private final Path uploadRoot;

    private static final Pattern IMG_SRC_PATTERN = Pattern.compile(
            "src\\s*=\\s*['\"]?(https?://[^\\s>'\"]+)['\"]?", Pattern.CASE_INSENSITIVE);

    private static final Pattern TEMP_URL_PATTERN = Pattern.compile(
            "/uploads/([^/]+)/temp/products/(\\d{4}/\\d{2})/([^/]+)$");

    private static final List<String> IMAGE_EXTS = List.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".svg", ".ico", ".tiff", ".tif");

    public ImageDownloadService(ProductMapper productMapper, ProductSkuMapper skuMapper,
            @Value("${mypet.upload.path:F:/MyWorkspace/project/mypet/uploads}") String uploadPath) {
        this.productMapper = productMapper;
        this.skuMapper = skuMapper;
        this.uploadRoot = Paths.get(uploadPath).toAbsolutePath().normalize();
        try { Files.createDirectories(uploadRoot); }
        catch (IOException e) { throw new RuntimeException("Cannot create upload directory", e); }
    }

    // ---- Public API ----

    /** Download external images in HTML detail, save to product/detail path. */
    public String downloadImagesInHtml(String html, String productId) {
        if (html == null || html.isBlank()) return html;
        Matcher m = IMG_SRC_PATTERN.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String full = m.group(0), url = m.group(1);
            if (!isLocalUrl(url)) {
                String local = downloadImage(url, "product/detail", productId);
                if (local != null) {
                    m.appendReplacement(sb, Matcher.quoteReplacement(full.replace(url, local)));
                    continue;
                }
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(full));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** Download a single external image, save to product/sku path. */
    public String downloadSingleImage(String url, String productId) {
        if (url == null || url.isBlank() || isLocalUrl(url)) return url;
        String local = downloadImage(url, "product/sku", productId);
        return local != null ? local : url;
    }

    /** Download external image URLs, save to product/main path. */
    public List<String> downloadImageList(List<String> urls, String productId) {
        if (urls == null || urls.isEmpty()) return urls;
        List<String> res = new ArrayList<>();
        for (String url : urls) {
            if (!isLocalUrl(url)) {
                String l = downloadImage(url, "product/main", productId);
                res.add(l != null ? l : url);
            } else res.add(url);
        }
        return res;
    }

    // ---- Backward-compatible overloads (no productId → temp/products) ----

    String downloadImagesInHtml(String html) {
        return downloadImagesInHtml(html, null);
    }

    public String downloadSingleImage(String url) {
        if (url == null || url.isBlank() || isLocalUrl(url)) return url;
        String local = downloadImage(url, "temp/products", null);
        return local != null ? local : url;
    }

    List<String> downloadImageList(List<String> urls) {
        if (urls == null || urls.isEmpty()) return urls;
        List<String> res = new ArrayList<>();
        for (String url : urls) {
            if (!isLocalUrl(url)) {
                String l = downloadImage(url, "temp/products", null);
                res.add(l != null ? l : url);
            } else res.add(url);
        }
        return res;
    }

    // ---- Temp-to-final relocation for product images ----

    /**
     * After a product is created (ID generated), move temp images to the
     * product's permanent directory and update all URLs.
     */
    public void relocateProductImages(String productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) return;
        boolean changed = false;

        // Main pictures
        List<String> pics = product.getMainPictures();
        if (pics != null) {
            List<String> relocated = new ArrayList<>();
            for (String url : pics) relocated.add(relocateIfTemp(url, "main", productId));
            if (!relocated.equals(pics)) { product.setMainPictures(relocated); changed = true; }
        }
        // Picture (first main)
        String pic = product.getPicture();
        if (pic != null) {
            String newPic = relocateIfTemp(pic, "main", productId);
            if (!newPic.equals(pic)) { product.setPicture(newPic); changed = true; }
        }
        // Detail HTML
        String detail = product.getDetail();
        if (detail != null) {
            String newDetail = relocateTempInHtml(detail, productId);
            if (!newDetail.equals(detail)) { product.setDetail(newDetail); changed = true; }
        }
        syncPictureFromMainPictures(product);
        if (changed) productMapper.updateById(product);

        // SKU pictures
        List<ProductSku> skus = skuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, productId)
                        .eq(ProductSku::getIsDelete, 0));
        for (ProductSku sku : skus) {
            String skuPic = sku.getPicture();
            if (skuPic != null) {
                String newSkuPic = relocateIfTemp(skuPic, "sku", productId);
                if (!newSkuPic.equals(skuPic)) {
                    sku.setPicture(newSkuPic);
                    skuMapper.updateById(sku);
                }
            }
        }
        log.info("Product {} image relocation complete", productId);
    }

    private String relocateIfTemp(String url, String subType, String productId) {
        if (url == null || !url.contains("/temp/products/")) return url;
        Matcher m = TEMP_URL_PATTERN.matcher(url);
        if (!m.find()) return url;
        String tenant = m.group(1), dateDir = m.group(2), filename = m.group(3);
        // Move file
        Path src = uploadRoot.resolve(tenant).resolve("temp/products").resolve(dateDir).resolve(filename);
        String newSubPath = "products/" + productId + "/" + subType + "/" + dateDir;
        Path dst = uploadRoot.resolve(tenant).resolve(newSubPath).resolve(filename);
        try {
            Files.createDirectories(dst.getParent());
            Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.warn("Failed to move temp image {} -> {}: {}", src, dst, e.getMessage());
            return url;
        }
        return "/uploads/" + tenant + "/" + newSubPath + "/" + filename;
    }

    /**
     * After relocation, ensure picture field matches mainPictures[0].
     */
    private void syncPictureFromMainPictures(Product product) {
        List<String> pics = product.getMainPictures();
        if (pics != null && !pics.isEmpty()) {
            product.setPicture(pics.get(0));
        }
    }

    private String relocateTempInHtml(String html, String productId) {
        if (html == null) return html;
        Matcher m = TEMP_URL_PATTERN.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String full = m.group(0);
            String newUrl = relocateIfTemp(full, "detail", productId);
            m.appendReplacement(sb, Matcher.quoteReplacement(newUrl));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    // ---- Async ----

    @Async
    public void downloadAndUpdateAsync(String productId, String tenantCode) {
        TenantContext.set(tenantCode);
        try {
            var product = productMapper.selectById(productId);
            if (product == null) return;
            String oldDetail = product.getDetail();
            String newDetail = downloadImagesInHtml(oldDetail, productId);
            List<String> newPics = downloadImageList(product.getMainPictures(), productId);
            boolean changed = !java.util.Objects.equals(newDetail, oldDetail)
                    || !java.util.Objects.equals(newPics, product.getMainPictures());
            if (changed) {
                product.setDetail(newDetail);
                product.setMainPictures(newPics);
                if (newPics != null && !newPics.isEmpty()) product.setPicture(newPics.get(0));
                productMapper.updateById(product);
                log.info("Async image download complete for product {}", productId);
            }
        } catch (Exception e) {
            log.error("Async image download failed: {}", e.getMessage());
        } finally {
            TenantContext.clear();
        }
    }

    // ---- Internal ----

    private String downloadImage(String url, String type, String productId) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                conn.setRequestProperty("Referer", referer(url));
                conn.setInstanceFollowRedirects(true);

                String ct = conn.getContentType();
                String ext = getExt(url, ct);
                byte[] data = readBytes(conn);
                if (!isValid(data)) {
                    log.warn("Download INVALID {} (attempt {}/3)", url, attempt);
                    if (attempt < 3) { sleep(1000); continue; }
                    return null;
                }

                String tc = TenantContext.get();
                if (tc == null || tc.isBlank()) tc = "xlong";
                String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
                String subPath = buildSubPath(type, productId, dateDir);
                Path dir = uploadRoot.resolve(tc).resolve(subPath);
                Files.createDirectories(dir);
                String name = UUID.randomUUID().toString().replace("-", "") + ext;
                Files.write(dir.resolve(name), data);
                return "/uploads/" + tc + "/" + subPath + "/" + name;
            } catch (Exception e) {
                log.warn("Download FAILED {} (attempt {}/3): {}", url, attempt, e.getMessage());
                if (attempt < 3) { sleep(1000); }
            } finally {
                if (conn != null) conn.disconnect();
            }
        }
        return null;
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    private String buildSubPath(String type, String productId, String dateDir) {
        if (type == null || type.isBlank()) return dateDir;
        switch (type) {
            case "product/main": case "product/slider": case "product/sku": case "product/detail":
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

    private boolean isLocalUrl(String url) {
        if (url == null) return true;
        return !url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("//");
    }

    private String getExt(String url, String ct) {
        if (ct != null) {
            ct = ct.toLowerCase();
            if (ct.contains("webp")) return ".webp";
            if (ct.contains("jpeg")||ct.contains("jpg")) return ".jpg";
            if (ct.contains("png")) return ".png";
            if (ct.contains("gif")) return ".gif";
            if (ct.contains("bmp")) return ".bmp";
            if (ct.contains("svg")) return ".svg";
        }
        String l = url.toLowerCase(); int q = l.indexOf('?'); if (q > 0) l = l.substring(0, q);
        for (String e : IMAGE_EXTS) if (l.endsWith(e)) return e;
        if (ct != null && ct.startsWith("image/")) return ".webp";
        return ".webp";
    }

    private String referer(String url) {
        try { URI u = URI.create(url); return u.getScheme()+"://"+u.getHost(); }
        catch (Exception e) { return "https://www.google.com"; }
    }

    private byte[] readBytes(HttpURLConnection c) throws IOException {
        try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] b = new byte[8192]; int n;
            while ((n = in.read(b)) != -1) out.write(b, 0, n);
            return out.toByteArray();
        }
    }

    private boolean isValid(byte[] d) {
        if (d == null || d.length < 100) return false;
        byte[] head = Arrays.copyOfRange(d, 0, 12);
        String s = new String(head, java.nio.charset.StandardCharsets.ISO_8859_1);
        if (s.startsWith("RIFF") && d.length > 20 && new String(Arrays.copyOfRange(d, 8, 12)).startsWith("WEBP")) return true;
        try { BufferedImage img = ImageIO.read(new ByteArrayInputStream(d)); return img != null && img.getWidth() > 0; }
        catch (Exception e) { return false; }
    }
}
