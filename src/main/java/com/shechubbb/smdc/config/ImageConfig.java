package com.shechubbb.smdc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 图片URL配置类
 */
@Configuration
public class ImageConfig {

    @Value("${smdc.base-url:}")
    private String baseUrl;

    @Value("${server.port:8080}")
    private int serverPort;

    @Bean
    public ImageUrlConverter imageUrlConverter() {
        return new ImageUrlConverter(baseUrl, serverPort);
    }

    /**
     * 图片URL转换器
     */
    public static class ImageUrlConverter {
        private final String baseUrl;
        private final int serverPort;

        public ImageUrlConverter(String baseUrl, int serverPort) {
            this.baseUrl = baseUrl;
            this.serverPort = serverPort;
        }

        /**
         * 将相对URL转换为绝对URL
         *
         * @param relativeUrl 相对URL
         * @return 绝对URL
         */
        public String getFullImageUrl(String relativeUrl) {
            if (relativeUrl == null || relativeUrl.isEmpty()) {
                return relativeUrl;
            }

            // 如果已经是完整的URL，直接返回
            if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
                return relativeUrl;
            }

            // 确保相对URL以/开头
            if (!relativeUrl.startsWith("/")) {
                relativeUrl = "/" + relativeUrl;
            }

            // 使用配置的baseUrl或默认构建
            if (baseUrl != null && !baseUrl.isEmpty()) {
                return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) + relativeUrl : baseUrl + relativeUrl;
            } else {
                // 默认使用localhost
                return "http://localhost:" + serverPort + relativeUrl;
            }
        }
        
        /**
         * 处理订单详情中的菜品图片URL
         * 
         * @param orderDetails 订单详情列表
         */
        public void processOrderDetailImages(List<?> orderDetails) {
            if (orderDetails == null || orderDetails.isEmpty()) {
                return;
            }
            
            // 遍历订单详情列表，处理图片URL
            for (Object detail : orderDetails) {
                try {
                    // 使用反射获取dishImage属性
                    java.lang.reflect.Method getDishImage = detail.getClass().getMethod("getDishImage");
                    java.lang.reflect.Method setDishImage = detail.getClass().getMethod("setDishImage", String.class);
                    
                    String dishImage = (String) getDishImage.invoke(detail);
                    if (dishImage != null && !dishImage.isEmpty()) {
                        // 转换为完整URL
                        setDishImage.invoke(detail, getFullImageUrl(dishImage));
                    }
                } catch (Exception e) {
                    // 忽略反射异常
                }
            }
        }
    }
} 