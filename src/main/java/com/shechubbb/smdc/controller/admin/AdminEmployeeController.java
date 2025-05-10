package com.shechubbb.smdc.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shechubbb.smdc.common.result.Result;
import com.shechubbb.smdc.common.util.JwtUtil;
import com.shechubbb.smdc.common.util.UserContext;
import com.shechubbb.smdc.entity.Employee;
import com.shechubbb.smdc.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 员工管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/employee")
public class AdminEmployeeController {

    @Autowired
    private EmployeeService employeeService;
    
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 员工登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> loginMap) {
        log.info("管理员登录");
        
        String username = loginMap.get("username");
        String password = loginMap.get("password");
        
        if (username == null || password == null) {
            return Result.error("用户名或密码不能为空");
        }
        
        Employee employee = employeeService.login(username, password);
        if (employee != null) {
            // 登录成功，生成JWT令牌
            String token = jwtUtil.createToken(employee.getId(), employee.getUsername(), JwtUtil.USER_TYPE_ADMIN);
            
            // 不返回密码
            employee.setPassword(null);
            
            // 返回员工信息和令牌
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("employee", employee);
            resultMap.put("token", token);
            
            log.info("管理员[{}]登录成功，已生成JWT令牌", employee.getId());
            return Result.success(resultMap);
        } else {
            return Result.error("用户名或密码错误");
        }
    }

    /**
     * 获取当前管理员信息
     */
    @GetMapping("/info")
    public Result<Employee> info() {
        // 从UserContext获取当前管理员
        Employee employee = UserContext.getCurrentAdmin();
        if (employee == null) {
            return Result.error("未登录或登录已过期");
        }
        
        // 不返回密码
        employee.setPassword(null);
        return Result.success(employee);
    }

    /**
     * 获取员工信息
     */
    @GetMapping("/info/{id}")
    public Result<Employee> getById(@PathVariable Long id) {
        // 检查权限
        if (!UserContext.isAdmin()) {
            return Result.error("无权限");
        }
        
        Employee employee = employeeService.getById(id);
        if (employee == null) {
            return Result.error("员工不存在");
        }
        
        // 不返回密码
        employee.setPassword(null);
        return Result.success(employee);
    }

    /**
     * 员工列表分页查询
     */
    @GetMapping("/page")
    public Result<Page<Employee>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name) {
        // 检查权限
        if (!UserContext.isAdmin()) {
            return Result.error("无权限");
        }
        
        Page<Employee> pageInfo = employeeService.page(page, pageSize, name);
        return Result.success(pageInfo);
    }

    /**
     * 添加员工
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody Employee employee) {
        // 检查权限
        if (!UserContext.isAdmin()) {
            return Result.error("无权限");
        }
        
        employeeService.add(employee);
        return Result.success();
    }

    /**
     * 更新员工
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody Employee employee) {
        // 检查权限
        if (!UserContext.isAdmin()) {
            return Result.error("无权限");
        }
        
        // 如果是当前登录的管理员，需要特殊处理
        Employee currentAdmin = UserContext.getCurrentAdmin();
        if (currentAdmin != null && currentAdmin.getId().equals(employee.getId())) {
            // 不允许修改自己的角色
            Employee existingEmployee = employeeService.getById(employee.getId());
            if (existingEmployee != null) {
                employee.setRole(existingEmployee.getRole());
            }
        }
        
        employeeService.update(employee);
        return Result.success();
    }

    /**
     * 删除员工
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Map<String, Long> map) {
        // 检查权限
        if (!UserContext.isAdmin()) {
            return Result.error("无权限");
        }
        
        Long id = map.get("id");
        
        // 不能删除自己
        Employee currentAdmin = UserContext.getCurrentAdmin();
        if (currentAdmin != null && currentAdmin.getId().equals(id)) {
            return Result.error("不能删除当前登录的账号");
        }
        
        employeeService.delete(id);
        return Result.success();
    }

    /**
     * 修改密码
     */
    @PostMapping("/updatePassword")
    public Result<Void> updatePassword(@RequestBody Map<String, Object> map) {
        // 检查是否登录
        Employee currentAdmin = UserContext.getCurrentAdmin();
        if (currentAdmin == null) {
            return Result.error("未登录或登录已过期");
        }
        
        // 只能修改自己的密码
        Long id = Long.valueOf(map.get("id").toString());
        if (!currentAdmin.getId().equals(id)) {
            return Result.error("只能修改自己的密码");
        }
        
        String oldPassword = map.get("oldPassword").toString();
        String newPassword = map.get("newPassword").toString();
        
        employeeService.updatePassword(id, oldPassword, newPassword);
        return Result.success();
    }
} 