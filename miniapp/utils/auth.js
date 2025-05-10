// 权限验证工具

// 用于防止循环跳转的时间戳记录
let lastRedirectTimestamp = 0;

/**
 * 安全获取App实例
 * @returns {Object|null} App实例或null
 */
const getAppInstance = () => {
  try {
    const app = getApp();
    return app;
  } catch (error) {
    console.warn('获取App实例失败:', error);
    return null;
  }
};

/**
 * 判断是否为开发环境
 * @returns {boolean} 是否为开发环境
 */
const isDevelopmentEnv = () => {
  return false; // 始终返回false，不再区分环境
};

/**
 * 获取存储的Token
 * @returns {string|null} JWT令牌
 */
const getToken = () => {
  try {
    return wx.getStorageSync('token');
  } catch (error) {
    console.error('获取token失败:', error);
    return null;
  }
};

/**
 * 设置Token到存储
 * @param {string} token JWT令牌
 */
const setToken = (token) => {
  try {
    wx.setStorageSync('token', token);
  } catch (error) {
    console.error('存储token失败:', error);
  }
};

/**
 * 清除Token
 */
const clearToken = () => {
  try {
    wx.removeStorageSync('token');
  } catch (error) {
    console.error('清除token失败:', error);
  }
};

/**
 * 检查用户是否已登录
 * @returns {boolean} 返回登录状态
 */
const checkAuth = () => {
  try {
    // 安全获取app实例
    const app = getAppInstance();
    
    // 检查app是否可用
    if (!app || !app.globalData) {
      console.warn('app实例或globalData不可用');
      return false;
    }
    
    // 不再区分环境，所有环境都要求登录
    const token = getToken();
    const userInfo = app.globalData.userInfo;
    
    // 如果没有令牌或用户信息，则表示未登录
    if (!token || !userInfo) {
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
        // 防止循环跳转 - 检查上次跳转时间，如果在3秒内则不再跳转
        const now = Date.now();
        if (now - lastRedirectTimestamp < 3000) {
          console.log('检测到频繁跳转，短时间内忽略重定向到登录页');
          return false;
        }
        
        // 更新最后跳转时间
        lastRedirectTimestamp = now;
        
        // 检查全局数据是否正在加载中
        const app = getAppInstance();
        if (app && app.globalData && app.globalData.loginStatusChecking) {
          console.log('登录状态检查中，暂不跳转');
          return false;
        }
        
        console.log('未登录，跳转到登录页');
        
        try {
          // 使用直接跳转而不是延迟跳转，避免状态不一致
          wx.reLaunch({
            url: '/pages/login/login',
            success: () => {
              console.log('已成功跳转到登录页');
            },
            fail: (error) => {
              console.error('reLaunch到登录页失败:', error);
              // 尝试使用redirectTo
              wx.redirectTo({
                url: '/pages/login/login'
              });
            }
          });
        } catch (error) {
          console.error('跳转登录页失败:', error);
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
        // 获取当前页面信息
        const pages = getCurrentPages();
        const currentPage = pages.length > 0 ? pages[pages.length - 1].route : '';
        
        // 检查登录状态，首页特殊处理
        const isLoggedIn = checkAuth();
        const app = getAppInstance();
        
        // 如果app还没准备好，不要阻止页面显示
        if (!app || app.globalData.loginStatusChecking) {
          console.log('App还未准备好或正在检查登录状态，先执行原始onShow');
          if (originalOnShow) {
            originalOnShow.call(this);
          }
          return;
        }
        
        // 特殊处理首页 - 不阻止首页显示，但会在必要时跳转到登录页
        if (currentPage === 'pages/index/index') {
          // 首页始终执行原始onShow，保证页面可以正常显示
          originalOnShow.call(this);
          
          // 如果未登录，并且非短时间内重复跳转，则延迟跳转登录页
          if (!isLoggedIn) {
            const now = Date.now();
            if (now - lastRedirectTimestamp > 5000) {
              console.log('首页检测到未登录状态，延迟跳转到登录页');
              setTimeout(() => {
                // 再次检查登录状态，防止期间已登录
                if (!checkAuth()) {
                  validateLogin(true);
                }
              }, 2000);
            }
          }
        } else {
          // 其他页面正常处理
          if (isLoggedIn) {
            // 已登录，调用原始onShow
            originalOnShow.call(this);
          } else {
            console.log(`${currentPage} onShow检测到未登录，不执行原始onShow`);
            // 未登录状态由validateLogin处理
            validateLogin(true);
          }
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
  isDevelopmentEnv,
  getToken,
  setToken,
  clearToken,
  getAppInstance
}; 