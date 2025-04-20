/**
 * 登录检查混入
 * 用于页面需要登录才能访问的情况
 */
const app = getApp();

// 检查用户是否已登录
const checkAuth = () => {
  try {
    // 开发环境检查
    try {
      const envVersion = __wxConfig.envVersion;
      if (envVersion === 'develop' || envVersion === 'trial') {
        console.log('开发环境，跳过登录检查');
        return true;
      }
    } catch (error) {
      console.error('获取环境信息失败:', error);
    }
    
    const userId = wx.getStorageSync('userId');
    const userInfo = app.globalData.userInfo;
    
    // 如果没有用户ID或用户信息，则表示未登录
    if (!userId || !userInfo) {
      return false;
    }
    
    return true;
  } catch (error) {
    console.error('检查登录状态失败:', error);
    return false;
  }
};

// 重定向到登录页面
const redirectToLogin = () => {
  try {
    wx.redirectTo({
      url: '/pages/login/login'
    });
  } catch (error) {
    console.error('跳转登录页面失败:', error);
    // 尝试使用重启方式跳转
    wx.reLaunch({
      url: '/pages/login/login'
    });
  }
};

/**
 * 登录检查混入
 * 使用方法：
 * 1. 导入：const loginCheck = require('../../utils/mixins/loginCheck');
 * 2. 混入：Page(loginCheck.createPageMixin({...}));
 */
module.exports = {
  // 创建页面混入
  createPageMixin(pageConfig) {
    // 保存原始的onLoad和onShow方法
    const originalOnLoad = pageConfig.onLoad;
    const originalOnShow = pageConfig.onShow;
    
    // 扩展onLoad方法
    pageConfig.onLoad = function(options) {
      // 检查登录状态
      if (!checkAuth()) {
        console.log('未登录，跳转到登录页面');
        redirectToLogin();
        return;
      }
      
      // 调用原始的onLoad方法
      if (originalOnLoad) {
        originalOnLoad.call(this, options);
      }
    };
    
    // 扩展onShow方法
    pageConfig.onShow = function() {
      // 检查登录状态
      if (!checkAuth()) {
        console.log('未登录，跳转到登录页面');
        redirectToLogin();
        return;
      }
      
      // 调用原始的onShow方法
      if (originalOnShow) {
        originalOnShow.call(this);
      }
    };
    
    return pageConfig;
  },
  
  // 导出内部方法供其他模块使用
  checkAuth,
  redirectToLogin
}; 