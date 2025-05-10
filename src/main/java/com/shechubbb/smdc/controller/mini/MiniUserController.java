package com.shechubbb.smdc.controller.mini;

import com.shechubbb.smdc.common.result.Result;
import com.shechubbb.smdc.common.util.JwtUtil;
import com.shechubbb.smdc.common.util.UserContext;
import com.shechubbb.smdc.entity.User;
import com.shechubbb.smdc.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 小程序用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/mini/user")
public class MiniUserController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 微信登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, Object> loginMap) {
        log.info("微信小程序用户登录");
        
        try {
            String code = (String) loginMap.get("code");
            Map<String, Object> userInfo = (Map<String, Object>) loginMap.get("userInfo");
            
            if (code == null || code.isEmpty()) {
                return Result.error("微信code不能为空");
            }
            
            // 调用service处理登录逻辑
            User user = userService.wxLogin(code, userInfo);
            if (user != null) {
                // 登录成功，生成JWT令牌
                String token = jwtUtil.createToken(user.getId(), user.getNickName(), JwtUtil.USER_TYPE_USER);
                
                // 返回用户信息和令牌
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("user", user);
                resultMap.put("token", token);
                
                log.info("用户[{}]登录成功，已生成JWT令牌", user.getId());
                return Result.success(resultMap);
            } else {
                return Result.error("登录失败");
            }
        } catch (Exception e) {
            log.error("微信登录异常", e);
            return Result.error("登录异常: " + e.getMessage());
        }
    }
    
    /**
     * 刷新登录状态接口
     * @param refreshMap 刷新参数，包含code和userId
     * @return 刷新结果
     */
    @PostMapping("/refresh")
    public Result<Map<String, Object>> refresh(@RequestBody Map<String, Object> refreshMap) {
        log.info("刷新微信小程序用户登录");
        
        try {
            String code = (String) refreshMap.get("code");
            String userId = (String) refreshMap.get("userId");
            
            if (code == null || code.isEmpty()) {
                return Result.error("微信code不能为空");
            }
            
            // 调用service处理刷新登录逻辑
            User user = userService.refreshLogin(code, userId);
            if (user != null) {
                // 登录成功，生成JWT令牌
                String token = jwtUtil.createToken(user.getId(), user.getNickName(), JwtUtil.USER_TYPE_USER);
                
                // 返回用户信息和令牌
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("user", user);
                resultMap.put("token", token);
                
                log.info("用户[{}]刷新登录成功，已生成新的JWT令牌", user.getId());
                return Result.success(resultMap);
            } else {
                return Result.error("刷新登录失败");
            }
        } catch (Exception e) {
            log.error("刷新微信登录异常", e);
            return Result.error("刷新登录异常: " + e.getMessage());
        }
    }
    
    /**
     * 账号密码登录
     */
    @PostMapping("/account/login")
    public Result<Map<String, Object>> accountLogin(@RequestBody Map<String, String> loginMap) {
        log.info("小程序账号登录");
        
        String username = loginMap.get("username");
        String password = loginMap.get("password");
        
        if (username == null || password == null) {
            return Result.error("用户名或密码不能为空");
        }
        
        User user = userService.accountLogin(username, password);
        if (user != null) {
            // 登录成功，生成JWT令牌
            String token = jwtUtil.createToken(user.getId(), user.getUsername(), JwtUtil.USER_TYPE_USER);
            
            // 返回用户信息和令牌
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("user", user);
            resultMap.put("token", token);
            
            log.info("用户[{}]账号登录成功，已生成JWT令牌", user.getId());
            return Result.success(resultMap);
        } else {
            return Result.error("用户名或密码错误");
        }
    }

    /**
     * 获取当前用户信息
     */
    @PostMapping("/info")
    public Result<User> info() {
        // 从UserContext获取当前用户
        User user = UserContext.getCurrentUser();
        if (user == null) {
            return Result.error("未登录或登录已过期");
        }
        return Result.success(user);
    }
    
    /**
     * 根据ID获取用户信息
     */
    @GetMapping("/info/{id}")
    public Result<User> getById(@PathVariable Long id) {
        // 只有管理员可以查看任意用户信息
        if (!UserContext.isAdmin()) {
            return Result.error("无权限");
        }
        
        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }
    
    /**
     * 更新用户信息
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody User user) {
        // 获取当前登录的用户
        User currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            return Result.error("未登录或登录已过期");
        }
        
        // 只能更新自己的信息，防止越权
        user.setId(currentUser.getId());
        
        userService.updateUser(user);
        return Result.success();
    }
}