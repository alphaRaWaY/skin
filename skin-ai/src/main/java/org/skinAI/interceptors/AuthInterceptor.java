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

    private final RedisTemplate<String, String> redisTemplate;

    public AuthInterceptor(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(401);
            response.getWriter().write("Unauthorized: Missing or invalid token");
            return false;
        }

        String token = authHeader.substring(7);
        String userId = redisTemplate.opsForValue().get("TOKEN:" + token);
        if (userId == null) {
            response.setStatus(401);
            response.getWriter().write("Unauthorized: Token invalid or expired");
            return false;
        }

        Map<String, Object> claims = JwtUtil.parseToken(token);
        ThreadLocalUtil.set(claims);
        redisTemplate.expire("TOKEN:" + token, 1, TimeUnit.DAYS);
        return true;
    }
}
