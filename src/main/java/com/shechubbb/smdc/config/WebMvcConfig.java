package com.shechubbb.smdc.config;

import com.shechubbb.smdc.interceptor.JwtAuthenticationInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${smdc.upload-path}")
    private String uploadPath;
    
    @Autowired
    private JwtAuthenticationInterceptor jwtAuthenticationInterceptor;

    /**
     * 跨域配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 设置静态资源映射
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置上传文件访问路径
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }
    
    /**
     * 添加拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册JWT认证拦截器
        registry.addInterceptor(jwtAuthenticationInterceptor)
                .addPathPatterns("/**")  // 拦截所有请求
                .excludePathPatterns("/error")  // 排除错误页面
                .excludePathPatterns("/upload/**"); // 排除上传文件访问路径
    }
} 