// 获取全局app实例
const getAppInstance = () => {
  return getApp();
};

// 导入配置
const config = require('../config/config');

// 网络请求工具类

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
                // 更新存储的用户信息
                app.globalData.userInfo = res.data.data;
                app.globalData.isLoggedIn = true;
                wx.setStorageSync('userId', res.data.data.id);
                
                // 处理队列中的请求
                waitQueue.forEach(task => {
                  task.resolve();
                });
                
                resolve();
              } else {
                // 刷新失败，清除登录信息
                wx.removeStorageSync('userId');
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
    
    // 获取用户ID
    const userId = wx.getStorageSync('userId');
    
    // 设置请求头
    const header = {
      'content-type': 'application/json'
    };
    
    // 如果有用户ID，则添加到请求头中
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
                  } else if (res.data.code === -1) { // 未登录或token过期
                    // 清除登录信息
                    wx.removeStorageSync('userId');
                    app.globalData.userInfo = null;
                    app.globalData.isLoggedIn = false;
                    
                    // 跳转到登录页面
                    wx.redirectTo({
                      url: '/pages/login/login'
                    });
                    
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
                
                console.error(`【错误】请求失败：${method} ${fullUrl}`, err);
                
                // 如果是localhost失败并且有备用URL，尝试使用备用URL重新请求
                if (retry && fullUrl.includes('localhost') && app.globalData.alternateBaseUrl) {
                  console.log('尝试使用本地IP地址重新请求...');
                  // 标记使用备用URL
                  wx.setStorageSync('useAlternateBaseUrl', true);
                  // 构造新的URL
                  const newUrl = fullUrl.replace(app.globalData.baseUrl, app.globalData.alternateBaseUrl);
                  // 重新发送请求
                  wx.request({
                    url: newUrl,
                    method: method,
                    data: data,
                    header: header,
                    success: (res) => {
                      try {
                        if (showLoading) {
                          wx.hideLoading();
                        }
                        
                        console.log(`【响应】(重试) ${method} ${newUrl}`, res);
                        
                        // 统一处理返回结果
                        if (res.statusCode === 200) {
                          // 业务层面的成功与失败处理
                          if (res.data.code === 1) {
                            resolve(res.data);
                          } else if (res.data.code === -1) { // 未登录或token过期
                            // 清除登录信息
                            wx.removeStorageSync('userId');
                            app.globalData.userInfo = null;
                            app.globalData.isLoggedIn = false;
                            
                            // 跳转到登录页面
                            wx.redirectTo({
                              url: '/pages/login/login'
                            });
                            
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
                        } else {
                          // HTTP异常
                          wx.showToast({
                            title: `请求错误: ${res.statusCode}`,
                            icon: 'none',
                            duration: 2000
                          });
                          console.error(`【错误】请求失败(重试)：status=${res.statusCode}`, res);
                          reject({ code: res.statusCode, msg: '网络异常' });
                        }
                      } catch (error) {
                        console.error('请求响应处理异常(重试):', error);
                        reject(error);
                      }
                    },
                    fail: (retryErr) => {
                      console.error(`【错误】重试请求也失败：${method} ${newUrl}`, retryErr);
                      
                      // 本地调试环境特殊处理
                      console.error('请确保：');
                      console.error('1. 后端服务已启动并监听在8080端口');
                      console.error('2. 微信开发者工具中勾选了"不校验合法域名"选项');
                      
                      wx.showToast({
                        title: retryErr.errMsg || '网络异常，请稍后再试',
                        icon: 'none',
                        duration: 2000
                      });
                      reject(retryErr);
                    }
                  });
                  return; // 重要：防止执行下面的代码
                }
                
                // 如果没有重试或者重试失败，显示常规错误提示
                wx.showToast({
                  title: err.errMsg || '网络异常，请稍后再试',
                  icon: 'none',
                  duration: 2000
                });
                reject(err);
              } catch (error) {
                console.error('请求失败处理异常:', error);
                reject(error);
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
    console.error('请求初始化异常:', error);
    if (showLoading) {
      wx.hideLoading();
    }
    return Promise.reject(error);
  }
};

// 封装GET请求
const get = (url, data = {}, showLoading = true) => {
  return request(url, 'GET', data, showLoading);
};

// 封装POST请求
const post = (url, data = {}, showLoading = true) => {
  return request(url, 'POST', data, showLoading);
};

// 封装PUT请求
const put = (url, data = {}, showLoading = true) => {
  return request(url, 'PUT', data, showLoading);
};

// 封装DELETE请求
const deleteRequest = (url, data = {}, showLoading = true) => {
  return request(url, 'DELETE', data, showLoading);
};

// 导出方法
module.exports = {
  get,
  post,
  put,
  delete: deleteRequest,
  refreshLogin
}; 