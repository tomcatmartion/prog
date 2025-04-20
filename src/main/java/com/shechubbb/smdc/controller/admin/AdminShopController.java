package com.shechubbb.smdc.controller.admin;

import com.shechubbb.smdc.common.result.Result;
import com.shechubbb.smdc.entity.ShopInfo;
import com.shechubbb.smdc.service.ShopInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 管理后台店铺信息控制器
 */
@RestController
@RequestMapping("/admin/shop")
@Slf4j
public class AdminShopController {
    
    @Autowired
    private ShopInfoService shopInfoService;
    
    /**
     * 获取店铺信息
     * @return 店铺信息
     */
    @GetMapping("/info")
    public Result<ShopInfo> getShopInfo() {
        log.info("管理后台获取店铺信息");
        ShopInfo shopInfo = shopInfoService.getShopInfo();
        if (shopInfo != null) {
            return Result.success(shopInfo);
        }
        
        // 如果没有店铺信息，则创建一个初始信息
        ShopInfo newShopInfo = new ShopInfo();
        newShopInfo.setId(1L);
        newShopInfo.setName("智能点餐系统");
        newShopInfo.setSlogan("便捷、高效、美味");
        newShopInfo.setStatus(1);
        shopInfoService.save(newShopInfo);
        
        return Result.success(newShopInfo);
    }
    
    /**
     * 更新店铺信息
     * @param shopInfo 店铺信息
     * @return 更新结果
     */
    @PostMapping("/update")
    public Result<String> updateShopInfo(@RequestBody ShopInfo shopInfo) {
        log.info("更新店铺信息:{}", shopInfo);
        boolean result = shopInfoService.updateShopInfo(shopInfo);
        if (result) {
            return Result.success("更新店铺信息成功");
        }
        return Result.error("更新店铺信息失败");
    }
} 