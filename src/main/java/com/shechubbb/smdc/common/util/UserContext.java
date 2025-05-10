package com.shechubbb.smdc.common.util;

import com.shechubbb.smdc.entity.Employee;
import com.shechubbb.smdc.entity.User;

/**
 * 用户上下文，用于在服务器端存储当前用户信息
 * 基于ThreadLocal实现线程安全的用户信息存储
 * 支持普通用户和管理员用户两种身份
 */
public class UserContext {
    // 使用ThreadLocal保证线程安全
    private static final ThreadLocal<User> userHolder = new ThreadLocal<>();
    private static final ThreadLocal<Employee> adminHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> userTypeHolder = new ThreadLocal<>();
    
    // 用户类型常量
    public static final String USER_TYPE_USER = "USER";
    public static final String USER_TYPE_ADMIN = "ADMIN";

    /**
     * 设置当前线程的普通用户信息
     * @param user 用户信息
     */
    public static void setCurrentUser(User user) {
        userHolder.set(user);
        userTypeHolder.set(USER_TYPE_USER);
    }
    
    /**
     * 设置当前线程的管理员用户信息
     * @param employee 管理员信息
     */
    public static void setCurrentAdmin(Employee employee) {
        adminHolder.set(employee);
        userTypeHolder.set(USER_TYPE_ADMIN);
    }

    /**
     * 获取当前线程的用户信息
     * @return 当前用户信息，如果是管理员或未登录则返回null
     */
    public static User getCurrentUser() {
        return USER_TYPE_USER.equals(userTypeHolder.get()) ? userHolder.get() : null;
    }
    
    /**
     * 获取当前线程的管理员信息
     * @return 当前管理员信息，如果是普通用户或未登录则返回null
     */
    public static Employee getCurrentAdmin() {
        return USER_TYPE_ADMIN.equals(userTypeHolder.get()) ? adminHolder.get() : null;
    }
    
    /**
     * 获取当前用户类型
     * @return 用户类型，可能是USER或ADMIN
     */
    public static String getUserType() {
        return userTypeHolder.get();
    }
    
    /**
     * 判断当前是否为管理员
     * @return 是否为管理员
     */
    public static boolean isAdmin() {
        return USER_TYPE_ADMIN.equals(userTypeHolder.get());
    }

    /**
     * 获取当前用户ID（不区分用户类型）
     * @return 用户ID，如果未登录则返回null
     */
    public static Long getCurrentUserId() {
        if (USER_TYPE_USER.equals(userTypeHolder.get())) {
            User user = getCurrentUser();
            return user != null ? user.getId() : null;
        } else if (USER_TYPE_ADMIN.equals(userTypeHolder.get())) {
            Employee admin = getCurrentAdmin();
            return admin != null ? admin.getId() : null;
        }
        return null;
    }

    /**
     * 清除当前线程的用户信息
     * 避免内存泄漏，应在请求结束时调用
     */
    public static void remove() {
        userHolder.remove();
        adminHolder.remove();
        userTypeHolder.remove();
    }
} 