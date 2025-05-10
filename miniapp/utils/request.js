// 获取全局app实例
const getAppInstance = () => {
  return getApp();
};

// 导入配置和工具
const config = require('../config/config');
const auth = require('./auth');

// 网络请求工具类

// 防抖：记录最后一次登录跳转的时间戳
let lastLoginRedirectTime = 0;

// 正在执行登录刷新的标记
let isRefreshing = false;
// 等待队列，存储token刷新期间的请求
let waitQueue = [];

/**
 * 刷新登录状态
 * @returns {Promise} 刷新结果
 */
const refreshLogin = () => {
  if (isRefreshing) {
    // 如果已经在刷新中，返回等待的Promise
    return new Promise((resolve, reject) => {
      waitQueue.push({ resolve, reject });
    });
  }

  isRefreshing = true;
  console.log('正在刷新登录状态...');
  
  // 获取最新的app实例
  const app = getAppInstance();

  return new Promise((resolve, reject) => {
    // 调用微信登录获取新code
    wx.login({
      success: (loginRes) => {
        if (loginRes.code) {
          // 发送code到后端获取新token
          wx.request({
            url: app.globalData.baseUrl + '/mini/user/refresh',
            method: 'POST',
            data: {
              code: loginRes.code,
              userId: wx.getStorageSync('userId') || ''
            },
            success: (res) => {
              if (res.data.code === 1 && res.data.data) {
                // 更新存储的用户信息和Token
                app.globalData.userInfo = res.data.data.user;
                app.globalData.isLoggedIn = true;
                auth.setToken(res.data.data.token);
                wx.setStorageSync('userId', res.data.data.user.id); // 兼容旧版，可以逐步移除
                
                // 处理队列中的请求
                waitQueue.forEach(task => {
                  task.resolve();
                });
                
                resolve();
              } else {
                // 刷新失败，清除登录信息
                auth.clearToken();
                wx.removeStorageSync('userId'); // 兼容旧版，可以逐步移除
                app.globalData.userInfo = null;
                app.globalData.isLoggedIn = false;
                
                // 处理队列中的请求（失败）
                waitQueue.forEach(task => {
                  task.reject(new Error('刷新登录状态失败'));
                });
                
                // 跳转到登录页面
                wx.reLaunch({
                  url: '/pages/login/login'
                });
                
                reject(new Error('刷新登录状态失败'));
              }
            },
            fail: (err) => {
              // 处理队列中的请求（失败）
              waitQueue.forEach(task => {
                task.reject(err);
              });
              
              reject(err);
            },
            complete: () => {
              isRefreshing = false;
              waitQueue = [];
            }
          });
        } else {
          // 获取code失败
          waitQueue.forEach(task => {
            task.reject(new Error('获取微信code失败'));
          });
          
          isRefreshing = false;
          waitQueue = [];
          reject(new Error('获取微信code失败'));
        }
      },
      fail: (err) => {
        // 微信登录失败
        waitQueue.forEach(task => {
          task.reject(err);
        });
        
        isRefreshing = false;
        waitQueue = [];
        reject(err);
      }
    });
  });
};

// 统一的请求方法
const request = (url, method, data, showLoading = true) => {
  try {
    // 获取最新的app实例
    const app = getAppInstance();
    
    // 检查是否需要使用备用URL（如果之前localhost连接失败过）
    let useAlternateUrl = false;
    try {
      useAlternateUrl = wx.getStorageSync('useAlternateBaseUrl');
    } catch (error) {
      console.warn('获取备用URL标记失败', error);
    }
    
    // 完整URL - 优先使用app.globalData中的baseUrl，如果获取不到则使用配置文件中的
    let baseUrl, alternateBaseUrl;
    
    if (app && app.globalData) {
      baseUrl = app.globalData.baseUrl || config.apiBaseUrl;
      alternateBaseUrl = app.globalData.alternateBaseUrl || config.alternateApiBaseUrl;
    } else {
      baseUrl = config.apiBaseUrl;
      alternateBaseUrl = config.alternateApiBaseUrl;
    }
    
    // 构建完整URL
    let fullUrl;
    if (url.indexOf('http') === 0) {
      // 如果url已经是完整的http(s)地址，则直接使用
      fullUrl = url;
    } else {
      // 否则添加baseUrl前缀
      if (useAlternateUrl && alternateBaseUrl) {
        fullUrl = alternateBaseUrl + url;
      } else {
        fullUrl = baseUrl + url;
      }
    }
    
    // 打印请求信息，用于调试
    console.log(`【请求】${method} ${fullUrl}`, data);
    
    // 检查baseUrl是否为本地地址
    if (fullUrl.includes('localhost') || fullUrl.includes('127.0.0.1')) {
      console.log('【警告】小程序中访问localhost可能会失败，请确保在开发者工具中勾选了"不校验合法域名"选项');
    }
    
    // 显示加载中
    if (showLoading) {
      wx.showLoading({
        title: '加载中...',
        mask: true
      });
    }
    
    // 获取JWT令牌
    const token = auth.getToken();
    
    // 设置请求头
    const header = {
      'content-type': 'application/json'
    };
    
    // 如果有令牌，则添加到请求头中
    if (token) {
      header['Authorization'] = `Bearer ${token}`;
    }
    
    // 添加用户ID到请求头，兼容旧版，可以在后续更新中移除
    const userId = wx.getStorageSync('userId');
    if (userId) {
      header['X-User-Id'] = userId;
    }
    
    // 返回Promise
    return new Promise((resolve, reject) => {
      // 发送请求的函数，增加retry参数用于尝试备用URL
      const sendRequest = (retry = true) => {
        try {
          wx.request({
            url: fullUrl,
            method: method,
            data: data,
            header: header,
            success: (res) => {
              try {
                if (showLoading) {
                  wx.hideLoading();
                }
                
                console.log(`【响应】${method} ${fullUrl}`, res);
                
                // 统一处理返回结果
                if (res.statusCode === 200) {
                  // 业务层面的成功与失败处理
                  if (res.data.code === 1) {
                    resolve(res.data);
                  } else if (res.statusCode === 401 || res.data.code === -1) { // 未登录或token过期
                    console.warn('认证失败，清除登录信息');
                    // 清除登录信息
                    auth.clearToken();
                    wx.removeStorageSync('userId'); // 兼容旧版，可以逐步移除
                    app.globalData.userInfo = null;
                    app.globalData.isLoggedIn = false;
                    
                    // 防抖：检查上次跳转时间，避免频繁跳转
                    const now = Date.now();
                    if (now - lastLoginRedirectTime < 3000) {
                      console.log('短时间内已有跳转到登录页的请求，忽略此次跳转');
                      return reject(res.data);
                    }
                    
                    // 更新最后跳转时间
                    lastLoginRedirectTime = now;
                    
                    // 跳转到登录页面 - 增加延迟
                    setTimeout(() => {
                      const pages = getCurrentPages();
                      const currentPage = pages.length > 0 ? pages[pages.length - 1].route : '';
                      
                      // 如果当前不在登录页，才进行跳转
                      if (currentPage !== 'pages/login/login') {
                        console.log('认证失败，跳转到登录页');
                        wx.reLaunch({
                          url: '/pages/login/login',
                          fail: (error) => {
                            console.error('reLaunch到登录页失败:', error);
                            // 备用方案
                            wx.redirectTo({
                              url: '/pages/login/login'
                            });
                          }
                        });
                      }
                    }, 500);
                    
                    reject(res.data);
                  } else {
                    // 其他业务错误
                    wx.showToast({
                      title: res.data.msg || '请求失败',
                      icon: 'none',
                      duration: 2000
                    });
                    // 将完整的错误信息传递给调用者，便于显示错误信息
                    reject(res.data);
                  }
                } else if (res.statusCode === 401) {
                  // 未授权，清除登录信息并跳转到登录页
                  console.warn('401未授权，清除登录信息');
                  auth.clearToken();
                  wx.removeStorageSync('userId'); // 兼容旧版，可以逐步移除
                  app.globalData.userInfo = null;
                  app.globalData.isLoggedIn = false;
                  
                  // 与上面类似的防抖处理
                  const now = Date.now();
                  if (now - lastLoginRedirectTime < 3000) {
                    console.log('短时间内已有跳转到登录页的请求，忽略此次跳转');
                    return reject({ code: 401, msg: '未登录或登录已过期' });
                  }
                  
                  // 更新最后跳转时间
                  lastLoginRedirectTime = now;
                  
                  setTimeout(() => {
                    const pages = getCurrentPages();
                    const currentPage = pages.length > 0 ? pages[pages.length - 1].route : '';
                    
                    // 如果当前不在登录页，才进行跳转
                    if (currentPage !== 'pages/login/login') {
                      console.log('401未授权，跳转到登录页');
                      wx.reLaunch({
                        url: '/pages/login/login',
                        fail: (error) => {
                          console.error('reLaunch到登录页失败:', error);
                          wx.redirectTo({
                            url: '/pages/login/login'
                          });
                        }
                      });
                    }
                  }, 500);
                  
                  reject({ code: 401, msg: '未登录或登录已过期' });
                } else {
                  // HTTP异常
                  wx.showToast({
                    title: `请求错误: ${res.statusCode}`,
                    icon: 'none',
                    duration: 2000
                  });
                  console.error(`【错误】请求失败：status=${res.statusCode}`, res);
                  reject({ code: res.statusCode, msg: '网络异常' });
                }
              } catch (error) {
                console.error('请求响应处理异常:', error);
                if (showLoading) {
                  wx.hideLoading();
                }
                reject(error);
              }
            },
            fail: (err) => {
              try {
                if (showLoading) {
                  wx.hideLoading();
                }
                
                console.error(`【错误】请求失败`, err);
                
                // 当使用localhost或IP地址失败时，提示开发者
                const errMsg = err.errMsg || '';
                if ((errMsg.includes('fail') || errMsg.includes('error')) && 
                    (fullUrl.includes('localhost') || fullUrl.includes('127.0.0.1'))) {
                  console.warn('访问本地服务器失败，请确保开发者工具中勾选了"不校验合法域名"选项，或者使用远程服务器地址');
                  
                  // 如果有备用URL，尝试使用备用URL
                  if (retry && alternateBaseUrl && !useAlternateUrl) {
                    console.log('尝试使用备用服务器地址...');
                    try {
                      wx.setStorageSync('useAlternateBaseUrl', true);
                    } catch (storageErr) {
                      console.warn('存储备用URL标记失败', storageErr);
                    }
                    
                    // 使用备用URL重试
                    if (url.indexOf('http') !== 0) {
                      fullUrl = alternateBaseUrl + url;
                      console.log(`【重试】使用备用地址：${fullUrl}`);
                      sendRequest(false); // 不再重试，防止循环
                      return; // 退出当前失败回调
                    }
                  }
                }
                
                reject(err);
              } catch (error) {
                console.error('请求失败处理异常:', error);
                if (showLoading) {
                  wx.hideLoading();
                }
                reject(err);
              }
            }
          });
        } catch (error) {
          console.error('发送请求异常:', error);
          if (showLoading) {
            wx.hideLoading();
          }
          reject(error);
        }
      };
      
      // 发送请求
      sendRequest();
    });
  } catch (error) {
    console.error('请求方法异常:', error);
    if (showLoading) {
      wx.hideLoading();
    }
    return Promise.reject(error);
  }
};

// GET请求
const get = (url, data = {}, showLoading = true) => {
  return request(url, 'GET', data, showLoading);
};

// POST请求
const post = (url, data = {}, showLoading = true) => {
  return request(url, 'POST', data, showLoading);
};

// PUT请求
const put = (url, data = {}, showLoading = true) => {
  return request(url, 'PUT', data, showLoading);
};

// DELETE请求
const deleteRequest = (url, data = {}, showLoading = true) => {
  return request(url, 'DELETE', data, showLoading);
};

module.exports = {
  refreshLogin,
  request,
  get,
  post,
  put,
  delete: deleteRequest
}; 