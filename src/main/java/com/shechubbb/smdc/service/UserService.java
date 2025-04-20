package com.shechubbb.smdc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shechubbb.smdc.entity.User;

import java.util.Map;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {

    /**
     * 微信登录
     * @param code 登录凭证
     * @return 用户信息
     */
    User wxLogin(String code);
    
    /**
     * 微信登录（带用户信息）
     * @param code 登录凭证
     * @param userInfo 用户信息
     * @return 用户信息
     */
    User wxLogin(String code, Map<String, Object> userInfo);

    /**
     * 根据openId查询用户
     * @param openId 微信openId
     * @return 用户信息
     */
    User getByOpenId(String openId);

    /**
     * 更新用户信息
     * @param user 用户信息
     */
    void updateUser(User user);

    /**
     * 刷新登录状态
     * @param code 微信登录code
     * @param userId 用户ID
     * @return 登录用户
     */
    User refreshLogin(String code, String userId);
    
    /**
     * 账号登录
     * @param username 用户名
     * @param password 密码
     * @return 登录用户
     */
    User accountLogin(String username, String password);
} 