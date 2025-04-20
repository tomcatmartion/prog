package com.shechubbb.smdc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@TableName("user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 微信用户唯一标识
     */
    private String openId;
    
    /**
     * 会话密钥
     */
    private String sessionKey;

    /**
     * 用户昵称
     */
    private String nickName;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码
     */
    private String password;

    /**
     * 用户头像
     */
    private String avatarUrl;
    
    /**
     * 性别，0-未知，1-男，2-女
     */
    private Integer gender;
    
    /**
     * 所在城市
     */
    private String city;
    
    /**
     * 所在省份
     */
    private String province;
    
    /**
     * 所在国家
     */
    private String country;

    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
} 