package org.mypetstore.mypetstore.config;

import org.mypetstore.mypetstore.interceptors.AuthInterceptor;
import org.mypetstore.mypetstore.interceptors.LogInterceptor;
import org.mypetstore.mypetstore.interceptors.LoginInterceptor;
import org.mypetstore.mypetstore.interceptors.RequestHeaderInterceptor;
import org.mypetstore.mypetstore.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private LoginInterceptor loginInterceptor;
    @Autowired
    private LogInterceptor logInterceptor;
    @Autowired
    private RequestHeaderInterceptor requestHeaderInterceptor;
    @Autowired
    private AuthInterceptor authInterceptor;
    /**
     * 配置拦截器，在用户发送请求前判断是否登录
     * @param registry 注册拦截器
     */
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor(redisTemplate))
            .addPathPatterns("/api/**")  // 拦截需要认证的路径
            .excludePathPatterns("/api/login/**", "/api/public/**"); // 登录和公共接口放行
    }
    /*
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //添加拦截器，检查请求来源
        registry.addInterceptor(requestHeaderInterceptor);
        //日志拦截器
        registry.addInterceptor(logInterceptor);
        //登录接口和注册接口不拦截
        registry.addInterceptor(loginInterceptor).excludePathPatterns(
                "/account/login",
                "/account/register",
                "/account/send-code",
                "/account/avatar/**",
                "/error",
                "/account/reset-password/**",
                "/account/rePwd",
                "/favicon.ico",
                "/account/forgot-password",
                "/loginForm",
                "/rePassword/**",
                "/registerForm");
    }


    *//**
     * 重定向所有访问资源定向到static
     * @param registry 注册拦截器
     *//*
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/**") // 替换为适当的路径
            .addResourceLocations("classpath:/static/"); // 确保静态资源的路径正确
    }*/
}
