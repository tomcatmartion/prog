// 权限验证工具
const app = getApp();

/**
 * 判断是否为开发环境
 * @returns {boolean} 是否为开发环境
 */
const isDevelopmentEnv = () => {
  return false; // 始终返回false，不再区分环境
};

/**
 * 检查用户是否已登录
 * @returns {boolean} 返回登录状态
 */
const checkAuth = () => {
  try {
    // 不再区分环境，所有环境都要求登录
    const userId = wx.getStorageSync('userId');
    const userInfo = app.globalData.userInfo;
    
    // 如果没有用户ID或用户信息，则表示未登录
    if (!userId || !userInfo) {
      return false;
    }
    
    return true;
  } catch (error) {
    console.warn('检查登录状态失败:', error);
    return false;
  }
};

/**
 * 验证登录状态，未登录则跳转到登录页面
 * @param {boolean} redirectToLogin 是否重定向到登录页
 * @returns {boolean} 返回登录状态
 */
const validateLogin = (redirectToLogin = true) => {
  try {
    const isLoggedIn = checkAuth();
    
    if (!isLoggedIn && redirectToLogin) {
      // 获取当前页面路径
      const pages = getCurrentPages();
      const currentPage = pages.length > 0 ? pages[pages.length - 1].route : '';
      
      // 如果当前不在登录页，则跳转到登录页
      if (currentPage !== 'pages/login/login') {
        console.log('未登录，跳转到登录页');
        try {
          wx.redirectTo({
            url: '/pages/login/login'
          });
        } catch (error) {
          console.error('跳转登录页失败:', error);
          // 如果redirectTo失败，尝试使用reLaunch
          try {
            wx.reLaunch({
              url: '/pages/login/login'
            });
          } catch (innerError) {
            console.error('reLaunch到登录页也失败:', innerError);
          }
        }
      }
    }
    
    return isLoggedIn;
  } catch (error) {
    console.warn('验证登录状态失败:', error);
    return false;
  }
};

/**
 * 页面登录验证混入，用于在页面 onLoad 或 onShow 方法中调用
 * @param {object} pageObj 页面对象
 */
const pageAuthMixin = (pageObj) => {
  // 保存原来的 onLoad 方法
  const originalOnLoad = pageObj.onLoad;
  const originalOnShow = pageObj.onShow;
  
  // 重写 onLoad 方法，添加登录验证
  pageObj.onLoad = function(options) {
    try {
      // 检查登录状态
      const isLoggedIn = validateLogin(true);
      
      if (isLoggedIn) {
        console.log('已登录，正常加载页面');
        // 调用原来的 onLoad 方法
        if (originalOnLoad) {
          originalOnLoad.call(this, options);
        }
      } else {
        console.log('未登录，跳转到登录页面');
        // 未登录不调用原始方法，强制跳转登录页
      }
    } catch (error) {
      console.error('页面登录验证异常:', error);
      // 确保原始方法被调用，即使出错
      if (originalOnLoad) {
        try {
          originalOnLoad.call(this, options);
        } catch (innerError) {
          console.error('调用原始onLoad方法失败:', innerError);
        }
      }
    }
  };
  
  // 重写 onShow 方法，添加登录状态检查
  if (originalOnShow) {
    pageObj.onShow = function() {
      try {
        // 检查登录状态，强制跳转
        const isLoggedIn = validateLogin(true);
        
        if (isLoggedIn) {
          // 调用原来的 onShow 方法
          originalOnShow.call(this);
        } else {
          console.log('onShow检测到未登录，不执行原始onShow');
        }
      } catch (error) {
        console.error('页面onShow验证异常:', error);
        // 确保原始方法被调用，即使出错
        try {
          originalOnShow.call(this);
        } catch (innerError) {
          console.error('调用原始onShow方法失败:', innerError);
        }
      }
    };
  }
  
  return pageObj;
};

module.exports = {
  checkAuth,
  validateLogin,
  pageAuthMixin,
  isDevelopmentEnv
}; 