package com.shechubbb.smdc.controller.mini;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shechubbb.smdc.common.constant.PayMethodConstant;
import com.shechubbb.smdc.common.exception.BusinessException;
import com.shechubbb.smdc.common.result.Result;
import com.shechubbb.smdc.common.util.UserContext;
import com.shechubbb.smdc.entity.TableInfo;
import com.shechubbb.smdc.entity.User;
import com.shechubbb.smdc.service.OrderService;
import com.shechubbb.smdc.service.TableInfoService;
import com.shechubbb.smdc.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 小程序订单控制器
 */
@Slf4j
@RestController
@RequestMapping("/mini/order")
public class MiniOrderController {

    @Autowired
    private OrderService orderService;
    
    @Autowired
    private TableInfoService tableInfoService;

    /**
     * 创建订单
     */
    @PostMapping("/create")
    public Result<Long> create(@RequestBody OrderVO orderVO) {
        // 从UserContext获取当前登录用户信息
        User currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            return Result.error("请先登录");
        }
        
        // 设置订单的用户ID
        orderVO.setUserId(currentUser.getId());
        
        Long orderId = orderService.createOrder(orderVO);
        return Result.success(orderId);
    }

    /**
     * 支付订单
     */
    @PostMapping("/pay")
    public Result<Map<String, String>> pay(@RequestBody Map<String, Object> map) {
        log.info("订单支付请求参数: {}", map);
        
        // 从UserContext获取当前登录用户信息
        User currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            return Result.error("请先登录");
        }
        
        if (map == null || !map.containsKey("id")) {
            log.error("支付失败: 订单ID为空");
            return Result.error("订单ID不能为空");
        }
        
        try {
            Long id = Long.valueOf(map.get("id").toString());
            
            // 验证订单归属权
            if (!orderService.verifyOrderOwner(id, currentUser.getId())) {
                log.error("支付失败: 无权操作此订单");
                return Result.error("无权操作此订单");
            }
            
            Integer payMethod = map.containsKey("payMethod") ? 
                Integer.valueOf(map.get("payMethod").toString()) : PayMethodConstant.WECHAT; // 默认微信支付
            
            log.info("开始处理订单支付, 订单ID: {}, 支付方式: {}", id, payMethod);
            orderService.payOrder(id, payMethod);
            
            // 模拟返回微信支付参数
            // 由于模拟环境，直接返回空数据，前端会处理为支付成功
            Map<String, String> payParams = new HashMap<>();
            
            // 注释掉模拟参数，避免前端误认为要调用微信支付API
            // payParams.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
            // payParams.put("nonceStr", UUID.randomUUID().toString().replaceAll("-", ""));
            // payParams.put("package", "prepay_id=wx" + System.currentTimeMillis());
            // payParams.put("signType", "MD5");
            // payParams.put("paySign", UUID.randomUUID().toString().replaceAll("-", ""));
            
            return Result.success(payParams);
        } catch (Exception e) {
            log.error("支付处理异常", e);
            return Result.error("支付处理失败: " + e.getMessage());
        }
    }

    /**
     * 取消订单
     */
    @PostMapping("/cancel")
    public Result<Void> cancel(@RequestBody Map<String, Long> map) {
        // 从UserContext获取当前登录用户信息
        User currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            return Result.error("请先登录");
        }
        
        Long id = map.get("id");
        if (id == null) {
            return Result.error("订单ID不能为空");
        }
        
        // 验证订单归属权
        if (!orderService.verifyOrderOwner(id, currentUser.getId())) {
            log.error("取消订单失败: 无权操作此订单");
            return Result.error("无权操作此订单");
        }
        
        orderService.cancelOrder(id);
        return Result.success();
    }

    /**
     * 订单列表
     */
    @GetMapping("/list")
    public Result<Page<OrderVO>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        // 从UserContext获取当前登录用户信息
        User currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            return Result.error("请先登录");
        }
        
        Long userId = currentUser.getId();
        log.info("获取订单列表 - userId: {}, status: {}, page: {}, pageSize: {}", userId, status, page, pageSize);
        
        Page<OrderVO> pageInfo = orderService.userPage(page, pageSize, userId, status);
        return Result.success(pageInfo);
    }

    /**
     * 订单详情
     */
    @GetMapping("/detail/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        // 从UserContext获取当前登录用户信息
        User currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            return Result.error("请先登录");
        }
        
        // 验证订单归属权
        if (!orderService.verifyOrderOwner(id, currentUser.getId())) {
            log.error("获取订单详情失败: 无权查看此订单");
            return Result.error("无权查看此订单");
        }
        
        OrderVO orderVO = orderService.getOrderDetail(id);
        return Result.success(orderVO);
    }
    
    /**
     * 扫码获取桌位信息
     */
    @GetMapping("/table/{code}")
    public Result<Long> table(@PathVariable String code) {
        // 根据桌位码查询桌位
        TableInfo tableInfo = tableInfoService.getByCode(code);
        if (tableInfo == null) {
            return Result.error("无效的桌位码");
        }
        
        return Result.success(tableInfo.getId());
    }
}