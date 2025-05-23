package com.shechubbb.smdc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shechubbb.smdc.common.constant.OrderStatusConstant;
import com.shechubbb.smdc.common.constant.PayMethodConstant;
import com.shechubbb.smdc.common.constant.PayStatusConstant;
import com.shechubbb.smdc.common.exception.BusinessException;
import com.shechubbb.smdc.config.ImageConfig.ImageUrlConverter;
import com.shechubbb.smdc.entity.Dish;
import com.shechubbb.smdc.entity.Order;
import com.shechubbb.smdc.entity.OrderDetail;
import com.shechubbb.smdc.entity.Specification;
import com.shechubbb.smdc.entity.TableInfo;
import com.shechubbb.smdc.entity.User;
import com.shechubbb.smdc.mapper.DishMapper;
import com.shechubbb.smdc.mapper.OrderMapper;
import com.shechubbb.smdc.service.OrderDetailService;
import com.shechubbb.smdc.service.OrderService;
import com.shechubbb.smdc.service.SpecificationService;
import com.shechubbb.smdc.service.TableInfoService;
import com.shechubbb.smdc.service.UserService;
import com.shechubbb.smdc.vo.OrderDetailVO;
import com.shechubbb.smdc.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单服务实现类
 */
@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private UserService userService;

    @Autowired
    private TableInfoService tableInfoService;
    
    @Autowired
    private DishMapper dishMapper;
    
    @Autowired
    private SpecificationService specificationService;
    
    @Autowired(required = false)
    private ImageUrlConverter imageUrlConverter;

    /**
     * 创建订单
     * @param orderVO 订单信息
     * @return 订单ID
     */
    @Override
    @Transactional
    public Long createOrder(OrderVO orderVO) {
        // 检查必要参数
        if (orderVO.getUserId() == null) {
            throw new BusinessException("用户ID不能为空");
        }
        
        // 检查桌位(如果提供了桌位ID)
        if (orderVO.getTableId() != null && !orderVO.getTableId().isEmpty()) {
            // 尝试根据ID或编码获取桌位信息
            TableInfo tableInfo = null;
            try {
                // 尝试按数字ID查询
                Long tableIdNum = Long.parseLong(orderVO.getTableId());
                tableInfo = tableInfoService.getById(tableIdNum);
            } catch (NumberFormatException e) {
                // 如果不是数字，则可能是桌位编码
                tableInfo = tableInfoService.getByCode(orderVO.getTableId());
                
                // 如果还没找到，尝试按名称查询
                if (tableInfo == null) {
                    tableInfo = tableInfoService.getByName(orderVO.getTableId());
                }
            }
            
            if (tableInfo != null) {
                // 更新桌位状态
                tableInfo.setStatus(1); // 使用中
                tableInfo.setUpdateTime(LocalDateTime.now());
                tableInfoService.updateById(tableInfo);
            }
        }
        
        // 设置订单基本信息
        Order order = new Order();
        BeanUtils.copyProperties(orderVO, order);
        
        // 生成订单号
        String number = generateOrderNumber();
        order.setNumber(number);
        
        // 设置订单状态
        order.setStatus(OrderStatusConstant.PENDING_PAYMENT);
        order.setPayStatus(PayStatusConstant.UNPAID);
        
        // 设置创建时间
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        
        // 保存订单
        save(order);
        
        // 保存订单明细，并同时缓存菜品信息
        List<OrderDetail> orderDetails = orderVO.getOrderDetails().stream().map(item -> {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(item, orderDetail);
            
            // 查询并缓存菜品信息
            Dish dish = dishMapper.selectById(item.getDishId());
            if (dish != null) {
                orderDetail.setDishName(dish.getName());
                orderDetail.setDishImage(dish.getImage());
            }
            
            // 查询并缓存规格信息
            if (item.getSpecificationId() != null) {
                Specification specification = specificationService.getById(item.getSpecificationId());
                if (specification != null) {
                    orderDetail.setSpecificationName(specification.getName());
                }
            }
            
            return orderDetail;
        }).collect(Collectors.toList());
        
        orderDetailService.saveBatch(orderDetails, order.getId());
        
        return order.getId();
    }

    /**
     * 支付订单
     * @param id 订单ID
     * @param payMethod 支付方式
     */
    @Override
    @Transactional
    public void payOrder(Long id, Integer payMethod) {
        // 查询订单
        Order order = getById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        // 检查订单状态
        if (order.getStatus() != OrderStatusConstant.PENDING_PAYMENT) {
            throw new BusinessException("订单状态异常，不能支付");
        }
        
        // 更新订单状态
        order.setStatus(OrderStatusConstant.PAID);
        order.setPayMethod(payMethod);
        order.setPayStatus(PayStatusConstant.PAID);
        order.setUpdateTime(LocalDateTime.now());
        
        updateById(order);
    }

    /**
     * 取消订单
     * @param id 订单ID
     */
    @Override
    @Transactional
    public void cancelOrder(Long id) {
        // 查询订单
        Order order = getById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        // 检查订单状态
        if (order.getStatus() == OrderStatusConstant.COMPLETED || order.getStatus() == OrderStatusConstant.CANCELLED) {
            throw new BusinessException("订单已完成或已取消，不能取消");
        }
        
        // 更新订单状态
        order.setStatus(OrderStatusConstant.CANCELLED);
        order.setUpdateTime(LocalDateTime.now());
        
        updateById(order);
        
        // 更新桌位状态(如果有桌位)
        if (order.getTableId() != null) {
            TableInfo tableInfo = tableInfoService.getById(order.getTableId());
            if (tableInfo != null) {
                tableInfo.setStatus(0); // 空闲
                tableInfo.setUpdateTime(LocalDateTime.now());
                tableInfoService.updateById(tableInfo);
            }
        }
    }

    /**
     * 接单
     * @param id 订单ID
     */
    @Override
    public void acceptOrder(Long id) {
        // 查询订单
        Order order = getById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        // 检查订单状态
        if (order.getStatus() != OrderStatusConstant.PAID) {
            throw new BusinessException("订单状态异常，不能接单");
        }
        
        // 更新订单时间
        order.setUpdateTime(LocalDateTime.now());
        
        updateById(order);
    }

    /**
     * 完成订单
     * @param id 订单ID
     */
    @Override
    @Transactional
    public void completeOrder(Long id) {
        // 查询订单
        Order order = getById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        // 检查订单状态
        if (order.getStatus() != OrderStatusConstant.PAID) {
            throw new BusinessException("订单状态异常，不能完成");
        }
        
        // 更新订单状态
        order.setStatus(OrderStatusConstant.COMPLETED);
        order.setUpdateTime(LocalDateTime.now());
        
        updateById(order);
        
        // 更新桌位状态（如果可以解析为数字ID）
        if (order.getTableId() != null && !order.getTableId().isEmpty()) {
            try {
                Long tableIdNum = Long.parseLong(order.getTableId());
                TableInfo tableInfo = tableInfoService.getById(tableIdNum);
                if (tableInfo != null) {
                    tableInfo.setStatus(0); // 空闲
                    tableInfo.setUpdateTime(LocalDateTime.now());
                    tableInfoService.updateById(tableInfo);
                }
            } catch (NumberFormatException e) {
                // 如果不是数字ID，可能是桌位编码，尝试根据编码查找
                TableInfo tableInfo = tableInfoService.getByCode(order.getTableId());
                if (tableInfo != null) {
                    tableInfo.setStatus(0); // 空闲
                    tableInfo.setUpdateTime(LocalDateTime.now());
                    tableInfoService.updateById(tableInfo);
                }
            }
        }
    }

    /**
     * 根据ID查询订单详情
     * @param id 订单ID
     * @return 订单详情
     */
    @Override
    public OrderVO getOrderDetail(Long id) {
        // 查询订单
        Order order = getById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        // 转换为OrderVO
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order, orderVO);
        
        // 查询用户信息
        User user = userService.getById(order.getUserId());
        if (user != null) {
            orderVO.setUserName(user.getNickName());
        }
        
        // 查询桌位信息
        if (order.getTableId() != null && !order.getTableId().isEmpty()) {
            try {
                // 尝试解析为数字ID
                Long tableIdNum = Long.parseLong(order.getTableId());
                TableInfo tableInfo = tableInfoService.getById(tableIdNum);
                if (tableInfo != null) {
                    orderVO.setTableName(tableInfo.getName());
                    orderVO.setTableCode(tableInfo.getCode());
                }
            } catch (NumberFormatException e) {
                // 如果不是数字ID，可能是桌位编码，尝试根据编码查找
                TableInfo tableInfo = tableInfoService.getByCode(order.getTableId());
                if (tableInfo != null) {
                    orderVO.setTableName(tableInfo.getName());
                    orderVO.setTableCode(tableInfo.getCode());
                } else {
                    // 如果还没找到，尝试按名称查询
                    tableInfo = tableInfoService.getByName(order.getTableId());
                    if (tableInfo != null) {
                        orderVO.setTableName(tableInfo.getName());
                        orderVO.setTableCode(tableInfo.getCode());
                    } else {
                        // 如果都找不到，直接使用tableId作为名称和编码
                        orderVO.setTableName("桌号" + order.getTableId());
                        orderVO.setTableCode(order.getTableId());
                    }
                }
            }
        }
        
        // 查询订单明细
        List<OrderDetailVO> orderDetailVOs = orderDetailService.getByOrderId(id);
        
        // 处理订单明细中的图片URL
        if (imageUrlConverter != null && orderDetailVOs != null && !orderDetailVOs.isEmpty()) {
            imageUrlConverter.processOrderDetailImages(orderDetailVOs);
        }
        
        orderVO.setOrderDetails(orderDetailVOs);
        
        return orderVO;
    }

    /**
     * 用户订单分页查询
     * @param page 页码
     * @param pageSize 每页记录数
     * @param userId 用户ID
     * @param status 订单状态(可选)
     * @return 分页数据
     */
    @Override
    public Page<OrderVO> userPage(int page, int pageSize, Long userId, Integer status) {
        // 创建分页对象
        Page<Order> pageInfo = new Page<>(page, pageSize);
        
        // 构造查询条件
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getUserId, userId);
        // 如果status不为空，则添加状态筛选条件
        queryWrapper.eq(status != null, Order::getStatus, status);
        queryWrapper.orderByDesc(Order::getCreateTime);
        
        // 执行查询
        page(pageInfo, queryWrapper);
        
        // 转换为OrderVO分页对象
        return handleOrderVOPage(pageInfo);
    }

    /**
     * 管理端订单分页查询
     * @param page 页码
     * @param pageSize 每页记录数
     * @param number 订单号
     * @param status 订单状态
     * @return 分页数据
     */
    @Override
    public Page<OrderVO> adminPage(int page, int pageSize, String number, Integer status) {
        // 创建分页对象
        Page<Order> pageInfo = new Page<>(page, pageSize);
        
        // 构造查询条件
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(number), Order::getNumber, number);
        queryWrapper.eq(status != null, Order::getStatus, status);
        queryWrapper.orderByDesc(Order::getCreateTime);
        
        // 执行查询
        page(pageInfo, queryWrapper);
        
        // 转换为OrderVO分页对象
        return handleOrderVOPage(pageInfo);
    }
    
    /**
     * 处理OrderVO分页对象
     * @param pageInfo 订单分页对象
     * @return OrderVO分页对象
     */
    private Page<OrderVO> handleOrderVOPage(Page<Order> pageInfo) {
        // 构造OrderVO分页对象
        Page<OrderVO> orderVOPage = new Page<>();
        BeanUtils.copyProperties(pageInfo, orderVOPage, "records");
        
        // 处理订单记录
        List<OrderVO> orderVOList = pageInfo.getRecords().stream().map(order -> {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(order, orderVO);
            
            // 查询用户信息
            User user = userService.getById(order.getUserId());
            if (user != null) {
                orderVO.setUserName(user.getNickName());
            }
            
            // 查询桌位信息
            if (order.getTableId() != null && !order.getTableId().isEmpty()) {
                try {
                    // 尝试解析为数字ID
                    Long tableIdNum = Long.parseLong(order.getTableId());
                    TableInfo tableInfo = tableInfoService.getById(tableIdNum);
                    if (tableInfo != null) {
                        orderVO.setTableName(tableInfo.getName());
                        orderVO.setTableCode(tableInfo.getCode());
                    }
                } catch (NumberFormatException e) {
                    // 如果不是数字ID，可能是桌位编码，尝试根据编码查找
                    TableInfo tableInfo = tableInfoService.getByCode(order.getTableId());
                    if (tableInfo != null) {
                        orderVO.setTableName(tableInfo.getName());
                        orderVO.setTableCode(tableInfo.getCode());
                    } else {
                        // 如果还没找到，直接使用tableId作为名称
                        orderVO.setTableName(order.getTableId());
                        orderVO.setTableCode(order.getTableId());
                    }
                }
            }
            
            return orderVO;
        }).collect(Collectors.toList());
        
        orderVOPage.setRecords(orderVOList);
        
        return orderVOPage;
    }
    
    /**
     * 生成订单号
     * @return 订单号
     */
    private String generateOrderNumber() {
        // 格式：时间戳 + 6位随机数
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomNum = String.valueOf((int)((Math.random() * 9 + 1) * 100000));
        return timestamp + randomNum;
    }
    
    /**
     * 验证订单归属权，确保用户只能操作自己的订单
     * @param orderId 订单ID
     * @param userId 用户ID
     * @return 是否有权操作此订单
     */
    @Override
    public boolean verifyOrderOwner(Long orderId, Long userId) {
        if (orderId == null || userId == null) {
            return false;
        }
        
        // 查询订单
        Order order = getById(orderId);
        if (order == null) {
            return false;
        }
        
        // 验证用户ID是否匹配
        return order.getUserId().equals(userId);
    }
} 