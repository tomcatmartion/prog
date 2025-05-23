package com.shechubbb.smdc.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shechubbb.smdc.entity.TableInfo;

/**
 * 桌台信息服务接口
 */
public interface TableInfoService extends IService<TableInfo> {

    /**
     * 分页查询桌位
     */
    Page<TableInfo> page(Integer page, Integer pageSize);

    /**
     * 添加桌位
     */
    void add(TableInfo tableInfo);

    /**
     * 更新桌位
     */
    void update(TableInfo tableInfo);

    /**
     * 删除桌位
     */
    void delete(Long id);

    /**
     * 生成桌位二维码
     */
    String generateQrCode(Long id);
    
    /**
     * 根据ID获取桌位信息
     */
    TableInfo getById(Long id);
    
    /**
     * 根据桌位编码获取桌位信息
     */
    TableInfo getByCode(String code);

    /**
     * 根据桌位名称获取桌位信息
     */
    TableInfo getByName(String name);
    
    /**
     * 更新桌位状态
     */
    void updateStatus(Long id, Integer status);
} 