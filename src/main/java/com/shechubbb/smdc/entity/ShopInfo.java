package com.shechubbb.smdc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 店铺信息实体类
 */
@Data
@TableName("shop_info")
public class ShopInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 店铺名称
     */
    private String name;

    /**
     * 店铺标语
     */
    private String slogan;

    /**
     * 店铺logo
     */
    private String logo;

    /**
     * 店铺地址
     */
    private String address;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 营业时间
     */
    private String businessHours;

    /**
     * 经度
     */
    private String longitude;

    /**
     * 纬度
     */
    private String latitude;

    /**
     * 状态，0关闭，1营业中
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
} 