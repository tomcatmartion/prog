const app = getApp();
const request = require('../../utils/request');
const auth = require('../../utils/auth');

Page({
  data: {
    loginStatus: false,
    userInfo: null,
    hasUserInfo: false,
    canIUseGetUserProfile: false,
    loginType: 'wx', // 登录类型，wx:微信登录，account:账号登录
    username: '', // 用户名
    password: '', // 密码
    wxLoginCode: '' // 微信登录code
  },
  
  onLoad() {
    try {
      console.log('登录页面加载');
    
      // 判断是否支持getUserProfile
      if (wx.getUserProfile) {
        this.setData({
          canIUseGetUserProfile: true
        });
      }
      
      // 自动填充测试账号
      this.setData({
        username: 'test',
        password: '123456'
      });
      
      // 检查是否已登录
      this.checkLoginStatus();
    } catch (error) {
      console.error('登录页面加载错误:', error);
    }
  },
  
  // 检查登录状态
  checkLoginStatus() {
    try {
      let userId = null;
      try {
        userId = wx.getStorageSync('userId');
      } catch (error) {
        console.warn('获取userId失败，可能在游客模式:', error);
      }
      
      const userInfo = app.globalData.userInfo;
      
      if (userId && userInfo) {
        this.setData({
          loginStatus: true,
          userInfo: userInfo,
          hasUserInfo: true
        });
        
        // 已登录，跳转到首页
        this.navigateToIndex();
      }
    } catch (error) {
      console.error('检查登录状态失败:', error);
    }
  },
  
  // 跳转到首页(新方法)
  navigateToIndex() {
    console.log('尝试跳转到首页');
    
    wx.switchTab({
      url: '/pages/index/index',
      success: () => {
        console.log('跳转成功');
      },
      fail: (error) => {
        console.error('switchTab跳转失败', error);
        // 如果switchTab失败，尝试redirectTo
        wx.redirectTo({
          url: '/pages/index/index',
          fail: (err) => {
            console.error('redirectTo也失败', err);
            // 最后尝试reLaunch
            wx.reLaunch({
              url: '/pages/index/index'
            });
          }
        });
      }
    });
  },
  
  // 切换登录方式
  switchTab(e) {
    const type = e.currentTarget.dataset.type;
    this.setData({
      loginType: type
    });
  },
  
  // 用户名输入
  onUsernameInput(e) {
    this.setData({
      username: e.detail.value
    });
  },
  
  // 密码输入
  onPasswordInput(e) {
    this.setData({
      password: e.detail.value
    });
  },
  
  // 账号登录
  accountLogin(isAutoLogin = false) {
    const { username, password } = this.data;
    
    // 输入验证
    if (!username) {
      wx.showToast({
        title: '请输入用户名',
        icon: 'none'
      });
      return;
    }
    
    if (!password) {
      wx.showToast({
        title: '请输入密码',
        icon: 'none'
      });
      return;
    }
    
    wx.showLoading({
      title: '登录中...',
      mask: true
    });
    
    // 发送登录请求
    request.post('/mini/user/account/login', {
      username: username,
      password: password
    }).then(res => {
      wx.hideLoading();
      
      if (res.code === 1) {
        // 登录成功，保存用户信息
        app.globalData.userInfo = res.data;
        app.globalData.isLoggedIn = true;
        
        // 存储用户ID到本地
        try {
          wx.setStorageSync('userId', res.data.id);
          console.log('成功存储用户ID:', res.data.id);
        } catch (error) {
          console.warn('存储用户ID失败:', error);
        }
        
        this.setData({
          loginStatus: true,
          userInfo: res.data,
          hasUserInfo: true
        });
        
        // 登录成功后显示提示
        wx.showToast({
          title: '登录成功',
          icon: 'success',
          duration: 1000
        });
        
        // 跳转到首页
        setTimeout(() => {
          this.navigateToIndex();
        }, 1000);
      } else {
        // 登录失败，显示错误信息
        wx.showToast({
          title: res.msg || '登录失败',
          icon: 'none',
          duration: 2000
        });
      }
    }).catch(err => {
      console.error('账号登录请求失败:', err);
      wx.hideLoading();
      
      // 显示错误信息，优先使用后台返回的错误消息
      let errorMsg = '网络异常，请稍后再试';
      
      // 检查是否有后台返回的错误信息
      if (err && err.msg) {
        errorMsg = err.msg;
      }
      
      wx.showToast({
        title: errorMsg,
        icon: 'none',
        duration: 2000
      });
    });
  },
  
  // 获取用户信息（新接口）
  getUserProfile() {
    wx.getUserProfile({
      desc: '用于完善会员资料',
      success: (res) => {
        // 将用户信息暂存，后续登录成功后再提交到后端
        const userProfile = res.userInfo;
        
        // 执行登录
        this.doLogin(userProfile);
      },
      fail: (err) => {
        wx.showToast({
          title: '您已取消授权',
          icon: 'none'
        });
      }
    });
  },
  
  // 执行登录操作
  doLogin(userProfile) {
    // 显示加载中
    wx.showLoading({
      title: '登录中...',
      mask: true
    });
    
    // 获取登录凭证
    wx.login({
      success: (loginRes) => {
        if (loginRes.code) {
          // 准备登录数据
          const loginData = {
            code: loginRes.code,
            userInfo: userProfile
          };
          
          // 使用code和用户信息调用后端登录接口
          request.post('/mini/user/wx/login', loginData).then(res => {
            wx.hideLoading();
            
            if (res.code === 1) {
              // 登录成功，保存用户信息
              app.globalData.userInfo = res.data;
              app.globalData.isLoggedIn = true;
              
              // 存储用户ID到本地
              try {
                wx.setStorageSync('userId', res.data.id);
              } catch (error) {
                console.warn('存储用户ID失败:', error);
              }
              
              this.setData({
                loginStatus: true,
                userInfo: res.data,
                hasUserInfo: true
              });
              
              // 显示登录成功提示
              wx.showToast({
                title: '登录成功',
                icon: 'success',
                duration: 1500
              });
              
              // 跳转到首页
              setTimeout(() => {
                this.navigateToIndex();
              }, 1500);
            } else {
              // 登录失败，显示错误信息
              wx.showToast({
                title: res.msg || '微信登录失败',
                icon: 'none',
                duration: 2000
              });
            }
          }).catch(err => {
            console.error('微信登录请求失败:', err);
            wx.hideLoading();
            
            // 显示错误信息，优先使用后台返回的错误消息
            let errorMsg = '网络异常，请稍后再试';
            
            // 检查是否有后台返回的错误信息
            if (err && err.msg) {
              errorMsg = err.msg;
            }
            
            wx.showToast({
              title: errorMsg,
              icon: 'none',
              duration: 2000
            });
          });
        } else {
          wx.hideLoading();
          wx.showToast({
            title: '获取登录凭证失败',
            icon: 'none',
            duration: 2000
          });
        }
      },
      fail: (err) => {
        console.error('微信登录失败:', err);
        wx.hideLoading();
        wx.showToast({
          title: '微信登录失败',
          icon: 'none',
          duration: 2000
        });
      }
    });
  }
}) 