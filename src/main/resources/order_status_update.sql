-- 订单状态更新脚本
-- 更新原状态为2（待接单）或3（待上菜）的订单为新状态2（已支付）
UPDATE `order` SET `status` = 2 WHERE `status` IN (2, 3);

-- 更新原状态为5（已取消）的订单为新状态4（已取消）
UPDATE `order` SET `status` = 4 WHERE `status` = 5;

-- 更新原状态为4（已完成）的订单为新状态3（已完成）
UPDATE `order` SET `status` = 3 WHERE `status` = 4;

-- 不需要更新状态为1（待付款）的订单，因为状态值相同 