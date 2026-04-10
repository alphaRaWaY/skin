package org.crythic.interceptors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.crythic.utils.JwtUtil;
import org.crythic.utils.ThreadLocalUtil;

import java.util.Map;

//拦截器
@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        //令牌验证
        try {
            // 获取 Token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                throw new RuntimeException("Token 为空");
            }
            // 解析 Token 获取用户信息
            Map<String, Object> claims = JwtUtil.parseToken(token);
            if (claims == null || !claims.containsKey("id")) {
                throw new RuntimeException("Token 解析失败");
            }
            Integer id = (Integer) claims.get("id");
            String role = (String) claims.get("role");
        // 从 Redis 中获取相同的 Token
            ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
            String redisToken = operations.get(id + "_token");

            if (redisToken == null) {
                // Token失效
                throw new RuntimeException("Token 失效");
            }

            // 检查用户身份
            String requestURI = request.getRequestURI();
            if (role != null) {
                if (requestURI.startsWith("/admin") && !"admin".equals(role)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "您正在访问前台资源");
                    throw new RuntimeException("非法访问：您没有权限访问后台资源");
                } else if (requestURI.startsWith("/customer") && !"customer".equals(role)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "您正在访问后台资源");
                    throw new RuntimeException("非法访问：您没有权限访问前台资源");
                }
            }

            // 将解析出的数据存入 ThreadLocal
            ThreadLocalUtil.set(claims);
        } catch (RuntimeException exception) {
            // 输出异常信息
            System.err.println("登录失败: " + exception.getMessage());
            // 设置响应状态
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"" + exception.getMessage() + "\"}");
            // 不放行
            return false;
        } catch (Exception exception) {
            // 处理其他异常
            System.err.println("服务器错误: " + exception.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return false;
        }
        System.out.println("请求成功"+response.toString());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //清空TreadLocal中的数据
        ThreadLocalUtil.remove();
    }
}
