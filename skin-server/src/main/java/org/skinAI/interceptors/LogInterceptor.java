package org.skinAI.interceptors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class LogInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        LocalDateTime now = LocalDateTime.now();
        // 定义格式化器，例如 yyyy-MM-dd HH:mm:ss
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        // 使用 format 方法并传入格式化器
        String formatted = now.format(formatter);
        String host = request.getHeader("host");
        System.out.println(formatted+" by "+host);
        String token = request.getHeader("Authorization");
        if(token == null || token.isEmpty()) {
            System.out.println("未携带token");
        } else {
            System.out.println("登陆凭证:\n" + token);
        }
        String method = "使用 "+request.getMethod()+" 方法请求了\""+request.getRequestURI()+'"';
        System.out.println(method);
        return true;
    }
}
