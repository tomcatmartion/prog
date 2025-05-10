package com.shechubbb.smdc.interceptor;

import com.shechubbb.smdc.common.util.JwtUtil;
import com.shechubbb.smdc.common.util.UserContext;
import com.shechubbb.smdc.entity.Employee;
import com.shechubbb.smdc.entity.User;
import com.shechubbb.smdc.service.EmployeeService;
import com.shechubbb.smdc.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT认证拦截器
 * 用于校验用户是否已登录，并将用户信息存入UserContext
 */
@Slf4j
@Component
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private EmployeeService employeeService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 从请求头获取Authorization
        String token = request.getHeader("Authorization");
        
        // 检查token前缀（Bearer）
        if (token != null && token.startsWith("Bearer ")) {
            // 去除前缀
            token = token.substring(7);
            
            // 验证token有效性
            if (jwtUtil.validateToken(token)) {
                // 获取用户ID和类型
                Long userId = jwtUtil.getUserIdFromToken(token);
                String userType = jwtUtil.getUserTypeFromToken(token);
                
                if (userId != null) {
                    // 根据用户类型加载不同类型的用户
                    if (JwtUtil.USER_TYPE_ADMIN.equals(userType)) {
                        // 管理员
                        Employee employee = employeeService.getById(userId);
                        if (employee != null) {
                            // 设置管理员到上下文
                            UserContext.setCurrentAdmin(employee);
                            log.debug("管理员 {} 已通过JWT身份验证", userId);
                            return true;
                        }
                    } else {
                        // 普通用户
                        User user = userService.getById(userId);
                        if (user != null) {
                            // 将用户信息设置到上下文中
                            UserContext.setCurrentUser(user);
                            log.debug("用户 {} 已通过JWT身份验证", userId);
                            return true;
                        }
                    }
                }
            }
        }
        
        // API访问路径检查
        String requestURI = request.getRequestURI();
        
        // 允许登录相关接口、静态资源等不需要认证
        if (isPublicAPI(requestURI)) {
            return true;
        }
        
        // 请求未通过认证，返回401状态码
        log.warn("未通过JWT身份验证的请求: {}", requestURI);
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
            requestURI.contains("/mini/user/account/login") ||
            requestURI.contains("/admin/employee/login")) {
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