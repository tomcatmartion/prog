package com.shechubbb.smdc.controller.mini;

import com.shechubbb.smdc.common.result.Result;
import com.shechubbb.smdc.entity.Employee;
import com.shechubbb.smdc.entity.User;
import com.shechubbb.smdc.service.EmployeeService;
import com.shechubbb.smdc.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

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
    private EmployeeService employeeService;

    /**
     * 微信登录
     */
    @PostMapping("/login")
    public Result<User> login(@RequestBody Map<String, Object> loginMap) {
        log.info("微信小程序用户登录: {}", loginMap);
        
        try {
            String code = (String) loginMap.get("code");
            Map<String, Object> userInfo = (Map<String, Object>) loginMap.get("userInfo");
            
            if (code == null || code.isEmpty()) {
                return Result.error("微信code不能为空");
            }
            
            // 调用service处理登录逻辑
            User user = userService.wxLogin(code, userInfo);
            if (user != null) {
                return Result.success(user);
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
    public Result<User> refresh(@RequestBody Map<String, Object> refreshMap) {
        log.info("刷新微信小程序用户登录: {}", refreshMap);
        
        try {
            String code = (String) refreshMap.get("code");
            String userId = (String) refreshMap.get("userId");
            
            if (code == null || code.isEmpty()) {
                return Result.error("微信code不能为空");
            }
            
            // 调用service处理刷新登录逻辑
            User user = userService.refreshLogin(code, userId);
            if (user != null) {
                return Result.success(user);
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
    public Result<User> accountLogin(@RequestBody Map<String, String> loginMap) {
        log.info("小程序账号登录");
        
        String username = loginMap.get("username");
        String password = loginMap.get("password");
        
        if (username == null || password == null) {
            return Result.error("用户名或密码不能为空");
        }
        
        User user = userService.accountLogin(username, password);
        if (user != null) {
            return Result.success(user);
        } else {
            return Result.error("用户名或密码错误");
        }
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/info/{id}")
    public Result<User> info(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }
    
    /**
     * 验证用户登录状态
     */
    @PostMapping("/info")
    public Result<User> getUserInfo(@RequestBody Map<String, Object> params) {
        String userId = (String) params.get("userId");
        log.info("获取用户信息，userId: {}", userId);
        
        if (userId == null || userId.isEmpty()) {
            return Result.error("用户ID不能为空");
        }
        
        try {
            Long id = Long.parseLong(userId);
            User user = userService.getById(id);
            if (user != null) {
                return Result.success(user);
            } else {
                return Result.error("用户不存在");
            }
        } catch (NumberFormatException e) {
            return Result.error("无效的用户ID格式");
        }
    }

    /**
     * 更新用户信息
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody User user) {
        userService.updateUser(user);
        return Result.success();
    }
}