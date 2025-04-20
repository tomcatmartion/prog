package com.shechubbb.smdc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shechubbb.smdc.entity.ShopInfo;
import com.shechubbb.smdc.mapper.ShopInfoMapper;
import com.shechubbb.smdc.service.ShopInfoService;
import org.springframework.stereotype.Service;

/**
 * 店铺信息Service实现类
 */
@Service
public class ShopInfoServiceImpl extends ServiceImpl<ShopInfoMapper, ShopInfo> implements ShopInfoService {
    
    @Override
    public ShopInfo getShopInfo() {
        LambdaQueryWrapper<ShopInfo> queryWrapper = new LambdaQueryWrapper<>();
        // 默认获取ID为1的店铺信息（单店模式）
        queryWrapper.eq(ShopInfo::getId, 1L);
        return getOne(queryWrapper);
    }
    
    @Override
    public boolean updateShopInfo(ShopInfo shopInfo) {
        // 确保更新的是ID为1的店铺信息
        shopInfo.setId(1L);
        return updateById(shopInfo);
    }
} 