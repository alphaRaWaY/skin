package org.skinAI.interceptors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.skinAI.utils.JwtUtil;
import org.skinAI.utils.ThreadLocalUtil;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private RedisTemplate<String, String> redisTemplate;

    public AuthInterceptor(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 1. 获取请求头中的 Authorization
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(401);
            response.getWriter().write("Unauthorized: Missing or invalid token");
            return false;
        }

        String token = authHeader.substring(7);
        // 2. 从Redis校验token是否存在
//        System.out.println("请求的token为"+token);
        String userId = redisTemplate.opsForValue().get("TOKEN:" + token);
        if (userId == null) {
            response.setStatus(401);
//            System.out.println("token不存在");
            response.getWriter().write("Unauthorized: Token invalid or expired");
            return false;
        }

        // 3. 可以把userId放入请求属性中供后续使用
//        request.setAttribute("userId", userId);
        // 解析 Token 获取用户信息
        Map<String, Object> claims = JwtUtil.parseToken(token);
        ThreadLocalUtil.set(claims);
        // 4. （可选）刷新token过期时间，延长登录状态
        redisTemplate.expire("TOKEN:" + token, 1, TimeUnit.DAYS);

        return true; // 放行请求
    }
}
