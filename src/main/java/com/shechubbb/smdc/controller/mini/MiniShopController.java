package com.shechubbb.smdc.controller.mini;

import com.shechubbb.smdc.common.result.Result;
import com.shechubbb.smdc.entity.ShopInfo;
import com.shechubbb.smdc.service.ShopInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 小程序端店铺信息控制器
 */
@RestController
@RequestMapping("/mini/shop")
@Slf4j
public class MiniShopController {
    
    @Autowired
    private ShopInfoService shopInfoService;
    
    /**
     * 获取店铺信息
     * @return 店铺信息
     */
    @PostMapping("/info")
    public Result<ShopInfo> getShopInfo() {
        log.info("获取店铺信息");
        ShopInfo shopInfo = shopInfoService.getShopInfo();
        if (shopInfo != null) {
            return Result.success(shopInfo);
        }
        return Result.error("获取店铺信息失败");
    }
} 