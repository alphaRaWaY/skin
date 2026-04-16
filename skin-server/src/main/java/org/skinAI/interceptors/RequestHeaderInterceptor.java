package org.skinAI.interceptors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RequestHeaderInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response, Object handler) {
        String source = request.getHeader("source-client");
        if ("miniapp".equals(source)) {
            // 标识是小程序请求，可以做日志、限流等
        }
        return true;
    }
}
