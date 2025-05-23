const app = getApp();
const request = require('../../utils/request');
const auth = require('../../utils/auth');

// 使用登录验证混入
Page(auth.pageAuthMixin({
  /**
   * 页面的初始数据
   */
  data: {
    // 店铺信息
    shopInfo: null,
    // 扫码状态
    scanStatus: {
      hasScan: false,
      tableInfo: '',
      tableId: ''
    },
    inputTableId: '', // 输入的桌台号
    loading: true
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad: function (options) {
    try {
      console.log('首页onLoad');
      
      // 强制检查登录状态
      const isLoggedIn = auth.validateLogin(true);
      if (!isLoggedIn) {
        console.log('未登录，跳转到登录页面');
        return;
      }
      
      console.log('进入正常页面加载流程');
      
      // 检查是否有扫描的桌号参数
      if (options.table) {
        let tableId = options.table;
        try {
          this.setData({
            'scanStatus.hasScan': true,
            'scanStatus.tableInfo': '桌号: ' + tableId,
            'scanStatus.tableId': tableId
          });
          // 记录到全局数据
          app.globalData.tableId = tableId;
        } catch (error) {
          console.error('设置桌号数据失败:', error);
        }
      }
      
      // 获取首页数据
      this.fetchShopInfo();
    } catch (error) {
      console.error('页面加载异常:', error);
      // 出错时也要设置loading为false
      this.setData({ loading: false });
      
      // 在出错的情况下也显示默认数据
      this.setDefaultData();
    }
  },
  
  /**
   * 设置默认数据以确保页面能显示
   */
  setDefaultData: function() {
    // 使用基础默认数据
    this.setData({
      loading: false,
      shopInfo: {
        name: '扫码点餐',
        slogan: '欢迎使用扫码点餐系统',
        logo: '/images/default_shop.png',
        description: '请扫描桌号后开始点餐',
        address: '暂无地址信息',
        phone: '暂无联系方式',
        businessHours: '暂无营业时间'
      }
    });
  },
  
  /**
   * 页面显示时检查登录状态
   */
  onShow: function () {
    try {
      console.log('首页onShow');
      
      // 强制检查登录状态
      const isLoggedIn = auth.validateLogin(true);
      if (!isLoggedIn) {
        console.log('未登录，跳转到登录页面');
        return;
      }
      
      // 检查全局的tableId，可能在其他页面扫描后返回
      const globalTableId = app.globalData.tableId;
      if (globalTableId && globalTableId !== this.data.scanStatus.tableId) {
        try {
          this.setData({
            'scanStatus.hasScan': true,
            'scanStatus.tableInfo': '桌号: ' + globalTableId,
            'scanStatus.tableId': globalTableId
          });
        } catch (error) {
          console.error('设置桌号数据失败:', error);
        }
      }
  
      // 如果数据未加载完成，尝试重新加载数据
      if (!this.data.shopInfo && !this.data.loading) {
        this.fetchShopInfo();
      }
    } catch (error) {
      console.error('页面显示异常:', error);
      this.setData({ loading: false });
      // 确保数据显示
      if (!this.data.shopInfo) {
        this.setDefaultData();
      }
    }
  },
  
  /**
   * 打开扫码界面
   */
  scanTable() {
    wx.scanCode({
      onlyFromCamera: true,
      scanType: ['qrCode'],
      success: (res) => {
        try {
          // 检查扫码结果是否有效
          const table = this.validateScanResult(res.result);
          if (table) {
            // 设置桌号信息
            this.setData({
              'scanStatus.hasScan': true,
              'scanStatus.tableInfo': '桌号: ' + table,
              'scanStatus.tableId': table
            });
            
            // 记录到全局数据
            app.globalData.tableId = table;
            
            wx.showToast({
              title: '桌号识别成功',
              icon: 'success'
            });
          } else {
            wx.showToast({
              title: '无效的桌号',
              icon: 'error'
            });
          }
        } catch (error) {
          console.error('扫码结果处理错误', error);
          wx.showToast({
            title: '二维码无效',
            icon: 'error'
          });
        }
      },
      fail: (err) => {
        console.log('扫码失败', err);
        // 用户取消扫码不显示错误提示
        if (err.errMsg !== 'scanCode:fail cancel') {
          wx.showToast({
            title: '扫码失败',
            icon: 'none'
          });
        }
      }
    });
  },
  
  /**
   * 验证扫码结果格式
   */
  validateScanResult: function(result) {
    try {
      // 尝试解析JSON格式
      const data = JSON.parse(result);
      if (data && data.tableNo) {
        return data.tableNo;
      }
      
      // 如果不是JSON格式，可能是直接的桌号字符串
      if (typeof result === 'string' && result.trim()) {
        // 检查是否以"桌号:"开头
        if (result.startsWith('桌号:')) {
          return result.substring(3).trim();
        }
        // 如果只是数字或字母组合也是有效的
        if (/^[A-Za-z0-9-]+$/.test(result)) {
          return result;
        }
      }
      
      return null;
    } catch (error) {
      console.error('解析扫码结果失败', error);
      // 如果解析失败但是是简单字符串，也认为可能是有效的
      if (typeof result === 'string' && /^[A-Za-z0-9-]+$/.test(result.trim())) {
        return result.trim();
      }
      return null;
    }
  },
  
  /**
   * 跳转到点餐页面
   */
  goToOrder() {
    console.log('跳转到点餐页面');
    
    // 如果已扫描桌号，则传入桌号信息
    if (this.data.scanStatus.hasScan) {
      // 将桌号保存到全局数据，以便其他页面使用
      app.globalData.tableId = this.data.scanStatus.tableId;
      console.log('已扫描桌号，记录桌号信息:', this.data.scanStatus.tableId);
    } else {
      console.log('未扫描桌号，使用默认模式点餐');
      // 清除全局桌号信息
      app.globalData.tableId = null;
    }

    // 构建URL并添加参数
    let url = '/pages/category/category';
    if (this.data.scanStatus.hasScan) {
      url += '?tableId=' + this.data.scanStatus.tableId;
    }

    // 使用navigateTo跳转到点餐页面
    console.log('使用navigateTo跳转到点餐页面:', url);
    wx.navigateTo({
      url: url,
      success: function() {
        console.log('跳转到点餐页面成功');
      },
      fail: function(err) {
        console.error('跳转到点餐页面失败:', err);
        
        // 如果失败，尝试使用其他方式跳转
        console.log('尝试使用reLaunch跳转');
        wx.reLaunch({
          url: url,
          success: function() {
            console.log('使用reLaunch跳转成功');
          },
          fail: function(err2) {
            console.error('所有跳转方式都失败:', err2);
            wx.showToast({
              title: '跳转失败，请重试',
              icon: 'none',
              duration: 2000
            });
          }
        });
      }
    });
  },
  
  // 获取店铺信息
  fetchShopInfo: function() {
    this.setData({ loading: true });
    
    console.log('开始请求店铺信息');
    request.post('/mini/shop/info', {}).then(res => {
      console.log('店铺信息响应:', res);
      if (res.code === 1 && res.data) {
        this.setData({
          loading: false,
          shopInfo: res.data
        });
      } else {
        console.log('获取店铺信息返回错误码:', res.code);
        // API返回错误时调用备用接口
        this.fetchShopInfoBackup();
      }
    }).catch(err => {
      console.error('获取店铺信息失败', err);
      // 发生错误时调用备用接口
      this.fetchShopInfoBackup();
    });
    
    // 确保加载状态不会一直显示，无论请求成功与否，5秒后关闭loading
    setTimeout(() => {
      if (this.data.loading) {
        console.log('数据获取超时，强制关闭loading，尝试备用接口');
        this.fetchShopInfoBackup();
      }
    }, 5000);
  },

  // 备用接口获取店铺信息
  fetchShopInfoBackup: function() {
    console.log('调用备用餐厅信息接口');
    request.post('/mini/restaurant/info', {}).then(res => {
      console.log('备用餐厅信息响应:', res);
      if (res.code === 1 && res.data) {
        this.setData({
          loading: false,
          shopInfo: res.data
        });
      } else {
        console.log('备用接口返回错误码:', res.code);
        // 如果备用接口也失败，则显示基本信息
        this.setDefaultData();
      }
    }).catch(err => {
      console.error('获取备用餐厅信息失败', err);
      // 所有接口都失败时显示默认数据
      this.setDefaultData();
    });
  },

  // 图片加载失败处理
  imageError: function(e) {
    const type = e.currentTarget.dataset.type;
    if (type === 'shop') {
      // 店铺logo加载失败
      this.setData({
        'shopInfo.logo': '/images/default_shop.png'
      });
    }
  },
  
  /**
   * 用户点击右上角分享
   */
  onShareAppMessage: function () {
    let title = '扫码点餐小程序';
    
    if (this.data.shopInfo && this.data.shopInfo.name) {
      title = this.data.shopInfo.name;
    }
    
    return {
      title: title,
      path: '/pages/index/index'
    };
  },

  /**
   * 拨打餐厅电话
   */
  callPhone: function() {
    if (!this.data.shopInfo || !this.data.shopInfo.phone || this.data.shopInfo.phone === '暂无联系方式') {
      wx.showToast({
        title: '暂无联系电话',
        icon: 'none'
      });
      return;
    }
    
    wx.makePhoneCall({
      phoneNumber: this.data.shopInfo.phone,
      success: function() {
        console.log('拨打电话成功');
      },
      fail: function(err) {
        console.log('拨打电话失败', err);
        // 用户取消不显示提示
        if (err.errMsg !== 'makePhoneCall:fail cancel') {
          wx.showToast({
            title: '拨打电话失败',
            icon: 'none'
          });
        }
      }
    });
  },

  /**
   * 处理桌台号输入变化
   */
  onTableInputChange: function(e) {
    this.setData({
      inputTableId: e.detail.value
    });
  },
  
  /**
   * 确认手动输入的桌台号
   */
  confirmTableInput: function() {
    const tableId = this.data.inputTableId.trim();
    
    if (!tableId) {
      wx.showToast({
        title: '请输入桌台号',
        icon: 'none'
      });
      return;
    }
    
    // 验证桌台号格式
    if (!/^[A-Za-z0-9-]+$/.test(tableId)) {
      wx.showToast({
        title: '桌台号格式不正确',
        icon: 'none'
      });
      return;
    }
    
    // 设置桌号信息
    this.setData({
      'scanStatus.hasScan': true,
      'scanStatus.tableInfo': '桌号: ' + tableId,
      'scanStatus.tableId': tableId,
      'inputTableId': '' // 清空输入框
    });
    
    // 记录到全局数据
    app.globalData.tableId = tableId;
    
    wx.showToast({
      title: '桌号设置成功',
      icon: 'success'
    });
  }
})) 