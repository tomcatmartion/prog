package com.shechubbb.smdc.controller.mini;

import com.shechubbb.smdc.common.result.Result;
import com.shechubbb.smdc.entity.TableInfo;
import com.shechubbb.smdc.service.TableInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 小程序桌台相关接口
 */
@Slf4j
@RestController
@RequestMapping("/mini/table")
public class MiniTableController {

    @Autowired
    private TableInfoService tableInfoService;

    /**
     * 获取桌台信息
     */
    @GetMapping("/info/{id}")
    public Result<TableInfo> info(@PathVariable String id) {
        log.info("获取桌台信息，id：{}", id);
        
        // 尝试将id转换为Long类型
        Long tableId;
        try {
            tableId = Long.parseLong(id);
        } catch (NumberFormatException e) {
            // 如果id不是数字，则可能是桌台编码
            TableInfo tableInfo = tableInfoService.getByCode(id);
            if (tableInfo != null) {
                return Result.success(tableInfo);
            }
            return Result.error("无效的桌台号");
        }
        
        // 根据ID查询桌台信息
        TableInfo tableInfo = tableInfoService.getById(tableId);
        if (tableInfo == null) {
            return Result.error("桌台不存在");
        }
        
        return Result.success(tableInfo);
    }
    
    /**
     * 根据桌台编码获取桌台信息
     */
    @GetMapping("/code/{code}")
    public Result<TableInfo> getByCode(@PathVariable String code) {
        log.info("根据编码获取桌台信息，code：{}", code);
        
        TableInfo tableInfo = tableInfoService.getByCode(code);
        if (tableInfo == null) {
            return Result.error("无效的桌台编码");
        }
        
        return Result.success(tableInfo);
    }

    @GetMapping("/name/{name}")
    public Result<TableInfo> getByName(@PathVariable String name) {
        log.info("根据编码获取桌台信息，name：{}", name);

        TableInfo tableInfo = tableInfoService.getByName(name);
        if (tableInfo == null) {
            return Result.error("无效的桌台编码");
        }

        return Result.success(tableInfo);
    }
} 