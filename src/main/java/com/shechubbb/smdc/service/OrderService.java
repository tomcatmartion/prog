package com.shechubbb.smdc.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shechubbb.smdc.entity.Order;
import com.shechubbb.smdc.vo.OrderVO;

import java.util.List;

/**
 * 订单服务接口
 */
public interface OrderService extends IService<Order> {

    /**
     * 创建订单
     * @param orderVO 订单信息
     * @return 订单ID
     */
    Long createOrder(OrderVO orderVO);

    /**
     * 支付订单
     * @param id 订单ID
     * @param payMethod 支付方式
     */
    void payOrder(Long id, Integer payMethod);

    /**
     * 取消订单
     * @param id 订单ID
     */
    void cancelOrder(Long id);

    /**
     * 接单
     * @param id 订单ID
     */
    void acceptOrder(Long id);

    /**
     * 完成订单
     * @param id 订单ID
     */
    void completeOrder(Long id);

    /**
     * 根据ID查询订单详情
     * @param id 订单ID
     * @return 订单详情
     */
    OrderVO getOrderDetail(Long id);

    /**
     * 用户订单分页查询
     * @param page 页码
     * @param pageSize 每页记录数
     * @param userId 用户ID
     * @param status 订单状态(可选)
     * @return 分页数据
     */
    Page<OrderVO> userPage(int page, int pageSize, Long userId, Integer status);

    /**
     * 管理端订单分页查询
     * @param page 页码
     * @param pageSize 每页记录数
     * @param number 订单号
     * @param status 订单状态
     * @return 分页数据
     */
    Page<OrderVO> adminPage(int page, int pageSize, String number, Integer status);

    /**
     * 验证订单归属权，确保用户只能操作自己的订单
     * @param orderId 订单ID
     * @param userId 用户ID
     * @return 是否有权操作此订单
     */
    boolean verifyOrderOwner(Long orderId, Long userId);
} 