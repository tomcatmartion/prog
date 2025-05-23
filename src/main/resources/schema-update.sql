-- 修改订单表中桌台ID列的数据类型，从bigint(20)改为varchar(32)
ALTER TABLE `order` MODIFY COLUMN `table_id` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '桌位id或桌台号';

-- 更新订单状态注释
ALTER TABLE `order` MODIFY COLUMN `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '订单状态 1:待付款 2:待接单 3:待上菜 4:已完成 5:已取消'; 