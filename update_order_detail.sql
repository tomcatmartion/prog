-- 更新订单明细表，添加缓存菜品信息字段
ALTER TABLE order_detail 
ADD COLUMN dish_name VARCHAR(32) COMMENT '菜品名称' AFTER create_time,
ADD COLUMN dish_image VARCHAR(255) COMMENT '菜品图片' AFTER dish_name,
ADD COLUMN specification_name VARCHAR(32) COMMENT '规格名称' AFTER dish_image;

-- 更新现有订单数据
-- 这部分需要手动执行，更新现有数据的菜品名称、图片和规格名称 