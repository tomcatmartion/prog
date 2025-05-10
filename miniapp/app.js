// app.js
const config = require('./config/config');
const auth = require('./utils/auth');

App({
  onLaunch() {
    // 获取系统信息
    this.getSystemInfo()
    
    // 检查登录状态（优先执行）
    this.checkLoginStatus()
    
    // 展示本地存储能力
    try {
      const logs = wx.getStorageSync('logs') || []
      logs.unshift(Date.now())
      wx.setStorageSync('logs', logs)
    } catch (error) {
      console.error('日志记录失败:', error)
    }
    
    // 监听网络状态变化
    this.listenNetworkStatus()
  },

  // 监听网络状态变化
  listenNetworkStatus() {
    wx.onNetworkStatusChange((result) => {
      console.log('网络状态变化：', result)
      if (result.isConnected) {
        // 网络恢复后，重新检查登录状态
        this.checkLoginStatus()
      }
    })
  },

  // 检查登录状态
  checkLoginStatus() {
    // 设置状态检查标记
    this.globalData.loginStatusChecking = true;
    
    // 获取JWT令牌
    const token = auth.getToken();
    
    // 如果没有令牌，则标记为未登录
    if (!token) {
      this.globalData.isLoggedIn = false;
      this.globalData.loginStatusChecking = false;
      return;
    }
    
    // 有令牌，尝试从后端获取用户信息
    const header = {
      'content-type': 'application/json',
      'Authorization': `Bearer ${token}`
    };
    
    wx.request({
      url: this.globalData.baseUrl + '/mini/user/info',
      method: 'GET',
      header: header,
      success: (result) => {
        if (result.data.code === 1) {
          // 登录状态有效，保存用户信息
          this.globalData.userInfo = result.data.data;
          this.globalData.isLoggedIn = true;
          
          // 触发登录成功回调
          if (this.userInfoReadyCallback) {
            this.userInfoReadyCallback(result.data.data);
          }
        } else {
          // 登录状态无效，清除登录信息
          auth.clearToken();
          wx.removeStorageSync('userId'); // 兼容旧版，可以逐步移除
          this.globalData.userInfo = null;
          this.globalData.isLoggedIn = false;
        }
      },
      fail: (err) => {
        // 请求失败，保持离线状态
        console.error('检查登录状态失败', err);
        this.globalData.isLoggedIn = false;
      },
      complete: () => {
        // 无论结果如何，标记检查完成
        this.globalData.loginStatusChecking = false;
      }
    });
  },
  
  // 跳转到登录页方法
  redirectToLogin() {
    // 判断当前是否在登录页面
    const pages = getCurrentPages();
    const isAtLoginPage = pages.length > 0 && pages[pages.length-1].route === 'pages/login/login';
    
    // 如果不是在登录页面，则跳转到登录页面
    if (!isAtLoginPage) {
      wx.reLaunch({
        url: '/pages/login/login'
      });
    }
  },

  // 获取系统信息
  getSystemInfo() {
    wx.getSystemInfo({
      success: e => {
        this.globalData.StatusBar = e.statusBarHeight;
        this.globalData.screenHeight = e.screenHeight;
        this.globalData.screenWidth = e.screenWidth;
        this.globalData.windowHeight = e.windowHeight;
        this.globalData.windowWidth = e.windowWidth;
      }
    });
  },

  globalData: {
    userInfo: null,
    isLoggedIn: false, // 登录状态
    baseUrl: config.apiBaseUrl, // 使用配置的API地址
    alternateBaseUrl: config.alternateApiBaseUrl, // 备用API地址
    StatusBar: 0,
    screenHeight: 0,
    screenWidth: 0,
    windowHeight: 0,
    windowWidth: 0,
    tableId: null, // 当前桌位ID
    cartList: [], // 购物车数据
    loginStatusChecking: false, // 登录状态检查中标记
  }
}) 