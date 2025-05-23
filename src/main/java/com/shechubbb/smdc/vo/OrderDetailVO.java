package com.shechubbb.smdc.vo;

import com.shechubbb.smdc.entity.OrderDetail;
import lombok.Data;

/**
 * 订单详情视图对象
 */
@Data
public class OrderDetailVO extends OrderDetail {
    // 继承自OrderDetail，自动包含了dishName、dishImage和specificationName字段
}