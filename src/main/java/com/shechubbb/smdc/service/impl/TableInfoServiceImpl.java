package com.shechubbb.smdc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shechubbb.smdc.common.exception.BusinessException;
import com.shechubbb.smdc.entity.TableInfo;
import com.shechubbb.smdc.mapper.TableInfoMapper;
import com.shechubbb.smdc.service.TableInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 桌位服务实现类
 */
@Slf4j
@Service
public class TableInfoServiceImpl extends ServiceImpl<TableInfoMapper, TableInfo> implements TableInfoService {

    /**
     * 分页查询桌位
     */
    @Override
    public Page<TableInfo> page(Integer page, Integer pageSize) {
        Page<TableInfo> pageInfo = new Page<>(page, pageSize);
        return page(pageInfo);
    }

    /**
     * 添加桌位
     */
    @Override
    @Transactional
    public void add(TableInfo tableInfo) {
        // 生成随机code
        tableInfo.setCode(generateTableCode());
        tableInfo.setStatus(0); // 默认空闲状态
        tableInfo.setCreateTime(LocalDateTime.now());
        tableInfo.setUpdateTime(LocalDateTime.now());
        
        // 保存数据
        save(tableInfo);
    }

    /**
     * 更新桌位
     */
    @Override
    @Transactional
    public void update(TableInfo tableInfo) {
        tableInfo.setUpdateTime(LocalDateTime.now());
        updateById(tableInfo);
    }

    /**
     * 删除桌位
     */
    @Override
    @Transactional
    public void delete(Long id) {
        removeById(id);
    }

    /**
     * 生成桌位二维码
     */
    @Override
    public String generateQrCode(Long id) {
        TableInfo tableInfo = getById(id);
        if (tableInfo == null) {
            log.error("生成二维码失败：桌位不存在，id={}", id);
            return null;
        }
        
        // 这里简单返回桌位code，实际项目中可能需要生成二维码图片并返回Base64
        return tableInfo.getCode();
    }

    /**
     * 根据ID获取桌位信息
     */
    @Override
    public TableInfo getById(Long id) {
        return super.getById(id);
    }
    
    /**
     * 根据桌位编码获取桌位信息
     */
    @Override
    public TableInfo getByCode(String code) {
        LambdaQueryWrapper<TableInfo> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TableInfo::getCode, code);
        return getOne(wrapper);
    }

    @Override
    public TableInfo getByName(String name) {
        LambdaQueryWrapper<TableInfo> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TableInfo::getName, name);
        return getOne(wrapper);
    }
    
    /**
     * 更新桌位状态
     */
    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        TableInfo tableInfo = getById(id);
        if (tableInfo != null) {
            tableInfo.setStatus(status);
            tableInfo.setUpdateTime(LocalDateTime.now());
            updateById(tableInfo);
        }
    }
    
    /**
     * 生成随机桌位编码
     */
    private String generateTableCode() {
        // 生成UUID并截取部分作为桌位编码
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
} 