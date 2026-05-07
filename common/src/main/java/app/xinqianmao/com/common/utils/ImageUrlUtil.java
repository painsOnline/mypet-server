/**
 * File: ImageUrlUtil.java
 * Author: system
 * Date: 2026-05-07
 *
 * Prefixes relative image paths with the configured base URL.
 * Injected as a Spring bean via @Value.
 */
package app.xinqianmao.com.common.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ImageUrlUtil {

    @Value("${mypet.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Prefix a single path with base URL if it starts with /uploads/ or /static/.
     * Already-absolute URLs (http/https) are returned unchanged.
     */
    public String fullUrl(String path) {
        if (!StringUtils.hasText(path)) return "";
        if (path.startsWith("http://") || path.startsWith("https://")) return path;
        if (path.startsWith("/")) return baseUrl + path;
        return baseUrl + "/" + path;
    }

    /**
     * Prefix all paths in a list.
     */
    public List<String> fullUrls(List<String> paths) {
        if (paths == null) return List.of();
        return paths.stream().map(this::fullUrl).collect(Collectors.toList());
    }

    /**
     * Replace relative image paths in HTML content with full URLs.
     * Handles both src="/uploads/..." and src="uploads/..." patterns.
     */
    public String fullUrlsInHtml(String html) {
        if (!StringUtils.hasText(html)) return "";
        String result = html.replaceAll("src=\"/uploads/", "src=\"" + baseUrl + "/uploads/");
        result = result.replaceAll("src=\"uploads/", "src=\"" + baseUrl + "/uploads/");
        return result;
    }
}
