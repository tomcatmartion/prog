package com.shechubbb.smdc.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shechubbb.smdc.common.result.Result;
import com.shechubbb.smdc.service.OrderService;
import com.shechubbb.smdc.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 订单管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/order")
public class AdminOrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 订单分页查询
     */
    @GetMapping("/page")
    public Result<Page<OrderVO>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String number,
            @RequestParam(required = false) Integer status) {
        Page<OrderVO> pageInfo = orderService.adminPage(page, pageSize, number, status);
        return Result.success(pageInfo);
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/detail/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        OrderVO orderVO = orderService.getOrderDetail(id);
        return Result.success(orderVO);
    }

    /**
     * 接单
     */
    @PostMapping("/accept")
    public Result<Void> accept(@RequestBody Map<String, Long> map) {
        Long id = map.get("id");
        orderService.acceptOrder(id);
        return Result.success();
    }

    /**
     * 完成订单
     */
    @PostMapping("/complete")
    public Result<Void> complete(@RequestBody Map<String, Long> map) {
        Long id = map.get("id");
        orderService.completeOrder(id);
        return Result.success();
    }

    /**
     * 取消订单
     */
    @PostMapping("/cancel")
    public Result<Void> cancel(@RequestBody Map<String, Object> map) {
        try {
            // 确保id存在且能转换为Long类型
            if (map.get("id") == null) {
                return Result.error("订单ID不能为空");
            }
            
            Long id = Long.valueOf(map.get("id").toString());
            String reason = map.get("reason") != null ? map.get("reason").toString() : null;
            
            // 记录取消原因
            log.info("管理端取消订单 - id: {}, reason: {}", id, reason);
            
            orderService.cancelOrder(id);
            return Result.success();
        } catch (NumberFormatException e) {
            log.error("取消订单失败：订单ID格式错误", e);
            return Result.error("订单ID格式错误");
        } catch (Exception e) {
            log.error("取消订单失败", e);
            return Result.error("取消订单失败：" + e.getMessage());
        }
    }
}