package com.shechubbb.smdc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shechubbb.smdc.entity.ShopInfo;

/**
 * 店铺信息Service接口
 */
public interface ShopInfoService extends IService<ShopInfo> {
    
    /**
     * 获取店铺信息
     * @return 店铺信息
     */
    ShopInfo getShopInfo();
    
    /**
     * 更新店铺信息
     * @param shopInfo 店铺信息
     * @return 是否成功
     */
    boolean updateShopInfo(ShopInfo shopInfo);
} 