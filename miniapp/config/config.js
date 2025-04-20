/**
 * 小程序配置文件
 */

// 生产环境配置
const production = {
  apiBaseUrl: 'https://your-production-api.com', // 替换为实际的生产环境API地址
  alternateApiBaseUrl: '', // 生产环境通常不需要备用地址
  socketUrl: 'wss://your-production-api.com/socket', // WebSocket地址
  version: '1.0.0',
  env: 'production'
};

// 本地开发配置
const development = {
  apiBaseUrl: 'http://localhost:8080', // 本地API地址
  alternateApiBaseUrl: 'http://192.168.31.12:8080', // 备选本地IP地址（当localhost无法访问时使用）
  socketUrl: 'ws://localhost:8080/socket',
  version: '1.0.0-dev',
  env: 'development'
};

// 根据编译环境选择配置
// 注意: 微信小程序中没有NODE_ENV，使用__wxConfig.envVersion来判断
let isDevEnv = false;
try {
  const envVersion = typeof __wxConfig !== 'undefined' ? __wxConfig.envVersion : 'develop';
  isDevEnv = (envVersion === 'develop' || envVersion === 'trial');
} catch (e) {
  // 如果访问__wxConfig失败，默认为开发环境
  console.error('获取环境信息失败:', e);
  isDevEnv = true;
}

// 导出配置
module.exports = isDevEnv ? development : production; 