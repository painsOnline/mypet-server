/**
 * File: AuthInterceptor.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.common.web;

import app.xinqianmao.com.common.annotation.NoAuth;
import app.xinqianmao.com.common.auth.JwtUtil;
import app.xinqianmao.com.common.auth.UserAuthInfo;
import app.xinqianmao.com.common.auth.UserContext;
import app.xinqianmao.com.common.constant.GlobalConstants;
import app.xinqianmao.com.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that validates JWT tokens from Authorization header.
 * Skips validation for methods annotated with @NoAuth.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // Skip non-handler requests
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }

        // Skip if @NoAuth annotation is present
        if (method.hasMethodAnnotation(NoAuth.class)) {
            return true;
        }

        // Extract token from Authorization header
        String authHeader = request.getHeader(GlobalConstants.AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(GlobalConstants.BEARER_PREFIX)) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(401);
            response.getWriter().write(objectMapper.writeValueAsString(
                    Result.error("401", "Missing or invalid Authorization header")));
            return false;
        }

        String token = authHeader.substring(GlobalConstants.BEARER_PREFIX.length());
        try {
            UserAuthInfo authInfo = jwtUtil.getUserAuthInfo(token);
            UserContext.set(authInfo);
            return true;
        } catch (Exception e) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(401);
            response.getWriter().write(objectMapper.writeValueAsString(
                    Result.error("401", e.getMessage())));
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
