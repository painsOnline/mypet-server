/**
 * File: TenantInterceptor.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.common.web;

import app.xinqianmao.com.common.auth.TenantContext;
import app.xinqianmao.com.common.constant.GlobalConstants;
import app.xinqianmao.com.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that reads the Tenant header and sets TenantContext.
 * Runs before AuthInterceptor so DataSource routing is established early.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String tenantCode = request.getHeader(GlobalConstants.TENANT_HEADER);

        if (tenantCode == null || tenantCode.isBlank()) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(400);
            response.getWriter().write(objectMapper.writeValueAsString(
                    Result.error("400", "Missing required header: " + GlobalConstants.TENANT_HEADER)));
            return false;
        }

        // Save previous tenant so it can be restored after request (important for tests
        // that set TenantContext before MockMvc and make direct DB calls afterward)
        String previousTenant = TenantContext.get();
        request.setAttribute("_prevTenant", previousTenant);
        TenantContext.set(tenantCode);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // Restore previous tenant value if one existed before this request
        Object prev = request.getAttribute("_prevTenant");
        if (prev instanceof String s && !s.isBlank()) {
            TenantContext.set(s);
        } else {
            TenantContext.clear();
        }
    }
}
