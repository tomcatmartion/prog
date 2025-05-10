-- 修改order表中的table_id字段为可空
ALTER TABLE `order` MODIFY COLUMN `table_id` bigint(20) DEFAULT NULL COMMENT '桌位id';

-- 确保user_id字段不为空
ALTER TABLE `order` MODIFY COLUMN `user_id` bigint(20) NOT NULL COMMENT '用户id';

-- 确保amount字段不为空
ALTER TABLE `order` MODIFY COLUMN `amount` decimal(10,2) NOT NULL COMMENT '总金额'; 