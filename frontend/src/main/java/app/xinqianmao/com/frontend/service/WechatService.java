/**
 * File: WechatService.java
 * Author: system
 * Date: 2026-05-08
 */
package app.xinqianmao.com.frontend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * WeChat mini-program API service.
 * Handles code-to-session, access-token management, and phone-number retrieval.
 */
@Slf4j
@Service
public class WechatService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${wechat.app-id}")
    private String appId;

    @Value("${wechat.secret}")
    private String secret;

    /** Cached access_token and its expire timestamp (ms) */
    private volatile String cachedAccessToken;
    private volatile long accessTokenExpireAt;

    private static final String JSCODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code";
    private static final String ACCESS_TOKEN_URL =
            "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s";
    private static final String PHONE_NUMBER_URL =
            "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=%s";

    public WechatService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Exchange login code for openid and session_key.
     */
    public WechatSession code2Session(String code) {
        String url = String.format(JSCODE2SESSION_URL, appId, secret, code);
        try {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.GET, null, String.class);
            JsonNode node = objectMapper.readTree(resp.getBody());
            if (node.has("errcode") && node.get("errcode").asInt() != 0) {
                log.error("jscode2session failed: errcode={} errmsg={}",
                        node.get("errcode").asInt(),
                        node.has("errmsg") ? node.get("errmsg").asText() : "");
                return null;
            }
            WechatSession session = new WechatSession();
            session.openid = node.get("openid").asText();
            session.sessionKey = node.has("session_key") ? node.get("session_key").asText() : null;
            return session;
        } catch (Exception e) {
            log.error("jscode2session error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Exchange phone-number auth code for the user's phone number.
     * Requires a server-side access_token (cached internally).
     */
    public String getPhoneNumber(String phoneCode) {
        String accessToken = getAccessToken();
        if (accessToken == null) return null;

        String url = String.format(PHONE_NUMBER_URL, accessToken);
        try {
            Map<String, String> body = Map.of("code", phoneCode);
            ResponseEntity<String> resp = restTemplate.postForEntity(url, body, String.class);
            JsonNode node = objectMapper.readTree(resp.getBody());
            if (node.has("errcode") && node.get("errcode").asInt() != 0) {
                log.error("getPhoneNumber failed: errcode={} errmsg={}",
                        node.get("errcode").asInt(),
                        node.has("errmsg") ? node.get("errmsg").asText() : "");
                return null;
            }
            JsonNode phoneInfo = node.get("phone_info");
            return phoneInfo != null ? phoneInfo.get("purePhoneNumber").asText() : null;
        } catch (Exception e) {
            log.error("getPhoneNumber error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get server-side access_token (cached, auto-refresh).
     */
    synchronized String getAccessToken() {
        if (cachedAccessToken != null && System.currentTimeMillis() < accessTokenExpireAt) {
            return cachedAccessToken;
        }
        String url = String.format(ACCESS_TOKEN_URL, appId, secret);
        try {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.GET, null, String.class);
            JsonNode node = objectMapper.readTree(resp.getBody());
            if (node.has("errcode")) {
                log.error("getAccessToken failed: errcode={}", node.get("errcode").asInt());
                return null;
            }
            cachedAccessToken = node.get("access_token").asText();
            int expiresIn = node.get("expires_in").asInt() - 300; // 5 min safety margin
            accessTokenExpireAt = System.currentTimeMillis() + expiresIn * 1000L;
            log.info("access_token refreshed, expires in {}s", expiresIn);
            return cachedAccessToken;
        } catch (Exception e) {
            log.error("getAccessToken error: {}", e.getMessage());
            return null;
        }
    }

    /** Result from jscode2session */
    public static class WechatSession {
        public String openid;
        public String sessionKey;
    }
}
