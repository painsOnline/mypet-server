/**
 * File: ImageDownloadService.java
 * Author: system
 * Date: 2026-05-12
 */
package app.xinqianmao.com.admin.service;

import app.xinqianmao.com.admin.dao.ProductMapper;
import app.xinqianmao.com.common.auth.TenantContext;
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ImageDownloadService {

    private final ProductMapper productMapper;
    private final Path uploadRoot;

    private static final Pattern IMG_SRC_PATTERN = Pattern.compile(
            "src\\s*=\\s*['\"]?(https?://[^\\s>'\"]+)['\"]?", Pattern.CASE_INSENSITIVE);

    private static final List<String> IMAGE_EXTS = List.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".svg", ".ico", ".tiff", ".tif");

    private static final String[] USER_AGENTS = {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
    };

    private static final String[] ACCEPT = {
        "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8",
        "image/webp,image/*,*/*;q=0.8",
    };

    public ImageDownloadService(ProductMapper productMapper,
            @Value("${mypet.upload.path:F:/MyWorkspace/project/mypet/uploads}") String uploadPath) {
        this.productMapper = productMapper;
        this.uploadRoot = Paths.get(uploadPath).toAbsolutePath().normalize();
        try { Files.createDirectories(uploadRoot); }
        catch (IOException e) { throw new RuntimeException("Cannot create upload directory", e); }
    }

    /** Async: download external images in background and update DB */
    @Async
    public void downloadAndUpdateAsync(String productId, String tenantCode) {
        TenantContext.set(tenantCode);
        try {
            var product = productMapper.selectById(productId);
            if (product == null) return;
            String oldDetail = product.getDetail();
            String newDetail = downloadImagesInHtml(oldDetail);
            List<String> newPics = downloadImageList(product.getMainPictures());
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

    String downloadImagesInHtml(String html) {
        if (html == null || html.isBlank()) { log.info("downloadImagesInHtml: empty input"); return html; }
        Matcher m = IMG_SRC_PATTERN.matcher(html);
        StringBuffer sb = new StringBuffer();
        int found = 0, replaced = 0;
        while (m.find()) {
            found++;
            String full = m.group(0), url = m.group(1);
            log.info("Found external img: {}", url.substring(0, Math.min(80, url.length())));
            if (!isLocalUrl(url)) {
                String local = downloadImage(url);
                if (local != null) {
                    m.appendReplacement(sb, Matcher.quoteReplacement(full.replace(url, local)));
                    replaced++;
                    continue;
                }
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(full));
        }
        m.appendTail(sb);
        log.info("downloadImagesInHtml: found={} replaced={}", found, replaced);
        return sb.toString();
    }

    List<String> downloadImageList(List<String> urls) {
        if (urls == null || urls.isEmpty()) { log.info("downloadImageList: empty"); return urls; }
        log.info("downloadImageList: {} urls to check", urls.size());
        List<String> res = new ArrayList<>();
        for (String url : urls) {
            if (!isLocalUrl(url)) { String l = downloadImage(url); res.add(l != null ? l : url); }
            else res.add(url);
        }
        return res;
    }

    private String downloadImage(String url) {
        log.info("Downloading: {}", url);
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setRequestProperty("Referer", referer(url));
            conn.setInstanceFollowRedirects(true);

            String ct = conn.getContentType();
            log.info("Content-Type: {} for {}", ct, url.substring(0, Math.min(60, url.length())));
            String ext = getExt(url, ct);
            log.info("Ext decided: {}", ext);
            byte[] data = readBytes(conn);
            log.info("Read {} bytes", data != null ? data.length : 0);
            if (!isValid(data)) { log.info("Invalid image data"); return null; }

            String tc = TenantContext.get();
            if (tc == null || tc.isBlank()) tc = "xlong";
            String dd = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            Path dir = uploadRoot.resolve(tc).resolve(dd);
            Files.createDirectories(dir);
            String name = UUID.randomUUID().toString().replace("-", "") + ext;
            Files.write(dir.resolve(name), data);
            String lp = "/uploads/" + tc + "/" + dd + "/" + name;
            log.info("Downloaded {} -> {}", url, lp);
            return lp;
        } catch (Exception e) {
            log.info("Download FAILED {}: {} {}", url, e.getClass().getSimpleName(), e.getMessage());
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private boolean isLocalUrl(String url) {
        if (url == null) return true;
        if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("//")) return false;
        return true;
    }

    private String getExt(String url, String ct) {
        // Content-Type takes priority (handles servers returning different format than URL suggests)
        if (ct != null) {
            ct = ct.toLowerCase();
            if (ct.contains("webp")) return ".webp";
            if (ct.contains("jpeg")||ct.contains("jpg")) return ".jpg";
            if (ct.contains("png")) return ".png";
            if (ct.contains("gif")) return ".gif";
            if (ct.contains("bmp")) return ".bmp";
            if (ct.contains("svg")) return ".svg";
        }
        // Fallback: URL extension
        String l = url.toLowerCase(); int q = l.indexOf('?'); if (q > 0) l = l.substring(0, q);
        for (String e : IMAGE_EXTS) if (l.endsWith(e)) return e;
        if (ct != null && ct.startsWith("image/")) return ".webp";
        log.info("getExt: unknown, defaulting to .webp");
        return ".webp";
    }

    private String referer(String url) {
        try { URI u = URI.create(url); return u.getScheme()+"://"+u.getHost(); }
        catch (Exception e) { return "https://www.google.com"; }
    }

    private String randomUA() { return USER_AGENTS[ThreadLocalRandom.current().nextInt(USER_AGENTS.length)]; }
    private String randomAccept() { return ACCEPT[ThreadLocalRandom.current().nextInt(ACCEPT.length)]; }
    private void randomDelay(int min, int max) {
        try { Thread.sleep(ThreadLocalRandom.current().nextInt(min, max+1)); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
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
        // Skip ImageIO for WebP (not supported natively by Java)
        byte[] head = Arrays.copyOfRange(d, 0, 12);
        String s = new String(head, java.nio.charset.StandardCharsets.ISO_8859_1);
        if (s.startsWith("RIFF") && d.length > 20 && new String(Arrays.copyOfRange(d, 8, 12)).startsWith("WEBP")) return true;
        try { BufferedImage img = ImageIO.read(new ByteArrayInputStream(d)); return img != null && img.getWidth() > 0; }
        catch (Exception e) { return false; }
    }
}
