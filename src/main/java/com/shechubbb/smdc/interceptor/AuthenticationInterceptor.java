package com.shechubbb.smdc.interceptor;

import com.shechubbb.smdc.common.util.UserContext;
import com.shechubbb.smdc.entity.User;
import com.shechubbb.smdc.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 身份认证拦截器
 * 用于校验用户是否已登录，并将用户信息存入UserContext
 */
@Slf4j
@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 从请求头或session中获取用户ID
        String userIdStr = null;
        HttpSession session = request.getSession(false);
        
        // 优先使用SESSION获取用户ID
        if (session != null) {
            userIdStr = (String) session.getAttribute("userId");
        }
        
        // 如果SESSION中没有，兼容从请求头获取（过渡期使用）
        if (userIdStr == null || userIdStr.isEmpty()) {
            userIdStr = request.getHeader("X-User-Id");
        }
        
        if (userIdStr != null && !userIdStr.isEmpty()) {
            try {
                Long userId = Long.valueOf(userIdStr);
                
                // 从数据库获取完整的用户信息
                User user = userService.getById(userId);
                
                if (user != null) {
                    // 将用户信息设置到上下文中
                    UserContext.setCurrentUser(user);
                    
                    // 如果是从请求头获取的，保存到SESSION中
                    if (session == null || session.getAttribute("userId") == null) {
                        request.getSession().setAttribute("userId", userId.toString());
                    }
                    
                    log.debug("用户 {} 已通过身份验证", userId);
                    return true;
                }
            } catch (NumberFormatException e) {
                log.warn("无效的用户ID格式: {}", userIdStr);
            }
        }
        
        // API访问路径检查
        String requestURI = request.getRequestURI();
        
        // 允许登录相关接口、静态资源等不需要认证
        if (isPublicAPI(requestURI)) {
            return true;
        }
        
        // 请求未通过认证，返回401状态码
        log.warn("未通过身份验证的请求: {}", requestURI);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求结束后，清除ThreadLocal中的用户信息，防止内存泄漏
        UserContext.remove();
    }

    /**
     * 判断是否为公开API，不需要身份验证
     * @param requestURI 请求URI
     * @return 是否为公开API
     */
    private boolean isPublicAPI(String requestURI) {
        // 登录相关接口
        if (requestURI.contains("/mini/user/login") || 
            requestURI.contains("/mini/user/refresh") || 
            requestURI.contains("/mini/user/account/login")) {
            return true;
        }
        
        // 静态资源
        if (requestURI.contains(".") && (
            requestURI.endsWith(".js") || 
            requestURI.endsWith(".css") || 
            requestURI.endsWith(".html") || 
            requestURI.endsWith(".png") || 
            requestURI.endsWith(".jpg") || 
            requestURI.endsWith(".jpeg") || 
            requestURI.endsWith(".gif") || 
            requestURI.endsWith(".svg") || 
            requestURI.endsWith(".ico"))) {
            return true;
        }
        
        // 放行swagger相关路径
        if (requestURI.contains("/swagger") || 
            requestURI.contains("/v2/api-docs") || 
            requestURI.contains("/webjars/") || 
            requestURI.startsWith("/doc.html")) {
            return true;
        }
        
        // 其他需要放行的接口
        return false;
    }
} 