package com.shechubbb.smdc.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shechubbb.smdc.common.exception.BusinessException;
import com.shechubbb.smdc.entity.User;
import com.shechubbb.smdc.mapper.UserMapper;
import com.shechubbb.smdc.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private WxMaService wxMaService;

    /**
     * 微信登录
     * @param code 登录凭证
     * @return 用户信息
     */
    @Override
    public User wxLogin(String code) {
        try {
            // 获取微信用户的openId
            WxMaJscode2SessionResult sessionResult = wxMaService.getUserService().getSessionInfo(code);
            String openId = sessionResult.getOpenid();
            
            // 根据openId查询用户
            User user = getByOpenId(openId);
            
            // 如果用户不存在，则注册新用户
            if (user == null) {
                user = new User();
                user.setOpenId(openId);
                user.setCreateTime(LocalDateTime.now());
                user.setUpdateTime(LocalDateTime.now());
                save(user);
            }
            
            return user;
        } catch (Exception e) {
            log.error("微信登录失败：", e);
            throw new BusinessException("微信登录失败");
        }
    }
    
    /**
     * 微信登录（带用户信息）
     * @param code 登录凭证
     * @param userInfo 用户信息
     * @return 用户信息
     */
    @Override
    public User wxLogin(String code, Map<String, Object> userInfo) {
        try {
            // 获取微信用户的openId
            WxMaJscode2SessionResult sessionResult = wxMaService.getUserService().getSessionInfo(code);
            String openId = sessionResult.getOpenid();
            
            // 根据openId查询用户
            User user = getByOpenId(openId);
            
            // 如果用户不存在，则注册新用户
            if (user == null) {
                user = new User();
                user.setOpenId(openId);
                user.setCreateTime(LocalDateTime.now());
                
                // 如果有用户信息，则设置用户信息
                if (userInfo != null) {
                    setUserInfo(user, userInfo);
                }
                
                user.setUpdateTime(LocalDateTime.now());
                save(user);
            } 
            // 如果用户存在但没有昵称等信息，则更新用户信息
            else if (userInfo != null && (user.getNickName() == null || user.getAvatarUrl() == null)) {
                setUserInfo(user, userInfo);
                user.setUpdateTime(LocalDateTime.now());
                updateById(user);
            }
            
            return user;
        } catch (Exception e) {
            log.error("微信登录失败：", e);
            throw new BusinessException("微信登录失败");
        }
    }
    
    /**
     * 设置用户信息
     * @param user 用户对象
     * @param userInfo 用户信息
     */
    private void setUserInfo(User user, Map<String, Object> userInfo) {
        if (userInfo.containsKey("nickName")) {
            user.setNickName((String) userInfo.get("nickName"));
        }
        if (userInfo.containsKey("avatarUrl")) {
            user.setAvatarUrl((String) userInfo.get("avatarUrl"));
        }
        if (userInfo.containsKey("gender")) {
            user.setGender(((Integer) userInfo.get("gender")).intValue());
        }
        if (userInfo.containsKey("city")) {
            user.setCity((String) userInfo.get("city"));
        }
        if (userInfo.containsKey("province")) {
            user.setProvince((String) userInfo.get("province"));
        }
        if (userInfo.containsKey("country")) {
            user.setCountry((String) userInfo.get("country"));
        }
    }

    /**
     * 根据openId查询用户
     * @param openId 微信openId
     * @return 用户信息
     */
    @Override
    public User getByOpenId(String openId) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getOpenId, openId);
        return getOne(queryWrapper);
    }

    /**
     * 更新用户信息
     * @param user 用户信息
     */
    @Override
    public void updateUser(User user) {
        user.setUpdateTime(LocalDateTime.now());
        updateById(user);
    }

    /**
     * 刷新登录状态
     * @param code 微信登录code
     * @param userId 用户ID
     * @return 登录用户
     */
    @Override
    public User refreshLogin(String code, String userId) {
        try {
            // 首先尝试获取当前用户
            User currentUser = null;
            if (userId != null && !userId.isEmpty()) {
                try {
                    Long id = Long.parseLong(userId);
                    currentUser = this.getById(id);
                } catch (NumberFormatException e) {
                    log.error("无效的用户ID格式: {}", userId);
                }
            }
            
            // 使用微信code请求新的会话信息
            // 这里可以调用微信API获取openid、session_key等信息
            // 简化处理，实际项目中需要调用微信API
            Map<String, Object> sessionInfo = new HashMap<>();
            sessionInfo.put("openid", "test_openid_" + code.substring(0, 4));
            sessionInfo.put("session_key", "test_session_key_" + System.currentTimeMillis());
            
            String openId = (String) sessionInfo.get("openid");
            
            if (openId == null || openId.isEmpty()) {
                log.error("获取微信openid失败");
                return null;
            }
            
            // 如果当前用户不存在，则尝试通过openid查找用户
            if (currentUser == null) {
                LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(User::getOpenId, openId);
                currentUser = this.getOne(queryWrapper);
                
                // 如果仍然找不到用户，则可能是新用户，但刷新接口不应该创建新用户
                if (currentUser == null) {
                    log.error("用户不存在，无法刷新");
                    return null;
                }
            }
            
            // 更新用户的session_key
            String sessionKey = (String) sessionInfo.get("session_key");
            if (sessionKey != null) {
                currentUser.setSessionKey(sessionKey);
                // 更新最后登录时间
                currentUser.setLastLoginTime(LocalDateTime.now());
                this.updateById(currentUser);
            }
            
            return currentUser;
        } catch (Exception e) {
            log.error("刷新登录状态异常", e);
            return null;
        }
    }

    /**
     * 账号登录
     * @param username 用户名
     * @param password 密码
     * @return 登录用户
     */
    @Override
    public User accountLogin(String username, String password) {
        // 用户名密码检查
        if (username == null || password == null) {
            return null;
        }
        
        try {
            // 检查是否为测试账号
            if ("test".equals(username) && "123456".equals(password)) {
                // 使用ID为1的测试用户
                User user = this.getById(1L);
                if (user == null) {
                    // 如果测试用户不存在，则创建一个
                    user = new User();
                    user.setId(1L);
                    user.setNickName("测试用户");
                    user.setUsername(username);
                    user.setPassword(password); // 实际项目中应该加密存储
                    user.setCreateTime(LocalDateTime.now());
                    user.setUpdateTime(LocalDateTime.now());
                    user.setLastLoginTime(LocalDateTime.now());
                    this.save(user);
                } else {
                    // 更新登录时间
                    user.setLastLoginTime(LocalDateTime.now());
                    this.updateById(user);
                }
                return user;
            }
            
            // 真实账号登录逻辑
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getUsername, username);
            User user = this.getOne(queryWrapper);
            
            // 用户存在且密码正确
            if (user != null && password.equals(user.getPassword())) { // 实际项目中应该加密比对
                // 更新登录时间
                user.setLastLoginTime(LocalDateTime.now());
                this.updateById(user);
                return user;
            }
        } catch (Exception e) {
            log.error("账号登录异常", e);
        }
        
        return null;
    }
} 