/**
 * 订单状态常量
 */
const ORDER_STATUS = {
  PENDING_PAYMENT: 1,    // 待付款
  PAID: 2,               // 已支付
  COMPLETED: 3,          // 已完成
  CANCELLED: 4           // 已取消
};

/**
 * 支付状态常量
 */
const PAY_STATUS = {
  UNPAID: 0,             // 未支付
  PAID: 1                // 已支付
};

/**
 * 支付方式常量
 */
const PAY_METHOD = {
  WECHAT: 1,             // 微信支付
  ALIPAY: 2              // 支付宝支付
};

/**
 * 订单状态映射（用于显示）
 */
const ORDER_STATUS_MAP = {
  [ORDER_STATUS.PENDING_PAYMENT]: { text: '待付款', color: '#FF9800' },
  [ORDER_STATUS.PAID]: { text: '已支付', color: '#4CAF50' },
  [ORDER_STATUS.COMPLETED]: { text: '已完成', color: '#2196F3' },
  [ORDER_STATUS.CANCELLED]: { text: '已取消', color: '#9E9E9E' }
};

/**
 * 订单状态标签（用于筛选）
 */
const ORDER_STATUS_TABS = [
  { id: 0, name: '全部' },
  { id: ORDER_STATUS.PENDING_PAYMENT, name: '待付款' },
  { id: ORDER_STATUS.PAID, name: '已支付' },
  { id: ORDER_STATUS.COMPLETED, name: '已完成' },
  { id: ORDER_STATUS.CANCELLED, name: '已取消' }
];

// 导出所有常量
module.exports = {
  ORDER_STATUS,
  PAY_STATUS,
  PAY_METHOD,
  ORDER_STATUS_MAP,
  ORDER_STATUS_TABS
}; 