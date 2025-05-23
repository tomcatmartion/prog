// pages/order/confirm/confirm.js
const app = getApp();
const request = require('../../../utils/request');
const cartUtil = require('../../../utils/cart');
const util = require('../../../utils/util');
const auth = require('../../../utils/auth');

Page(auth.pageAuthMixin({
  /**
   * 页面的初始数据
   */
  data: {
    tableId: null,
    tableName: '',
    tableCode: '',
    cartList: [],
    selectedItems: [],
    totalAmount: 0,
    remark: '',
    payMethods: [
      { id: 1, name: '微信支付', icon: '/images/wxpay.png', selected: true }
    ],
    selectedPayMethod: 1,
    isSubmitting: false
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad: function (options) {
    const tableId = options.tableId || app.globalData.tableId;
    let selectedItems = [];
    
    // 检查是否传入了选中项
    if (options.selected) {
      try {
        selectedItems = JSON.parse(options.selected);
      } catch (e) {
        console.error('解析选中项失败', e);
      }
    }
    
    this.setData({
      tableId,
      selectedItems
    });
    
    // 获取桌位信息
    if (tableId) {
      this.getTableInfo(tableId);
    }
    
    // 更新购物车数据
    this.updateCartData();
  },
  
  /**
   * 获取桌位信息
   */
  getTableInfo(tableId) {
    request.get('/mini/table/name/' + tableId).then(res => {
      if (res.code === 1 && res.data) {
        this.setData({
          tableName: res.data.name,
          tableCode: res.data.code
        });
      }
    });
  },
  
  /**
   * 更新购物车数据
   */
  updateCartData() {
    const cartList = cartUtil.getCartList();
    const { selectedItems } = this.data;
    
    // 如果没有选中项，则默认全选
    const itemsToShow = selectedItems.length > 0 
      ? selectedItems.map(index => cartList[index]).filter(Boolean)
      : cartList;
    
    // 计算总金额
    const totalAmount = itemsToShow.reduce((total, item) => {
      return total + (item.price * item.quantity);
    }, 0);
    
    this.setData({
      cartList: itemsToShow,
      totalAmount
    });
  },
  
  /**
   * 选择支付方式
   */
  selectPayMethod(e) {
    const id = e.currentTarget.dataset.id;
    
    // 更新选中状态
    const payMethods = this.data.payMethods.map(item => {
      return {
        ...item,
        selected: item.id === id
      };
    });
    
    this.setData({
      payMethods,
      selectedPayMethod: id
    });
  },
  
  /**
   * 输入备注
   */
  onRemarkInput(e) {
    this.setData({
      remark: e.detail.value
    });
  },
  
  /**
   * 提交订单
   */
  submitOrder() {
    // 检查是否有商品
    if (this.data.cartList.length === 0) {
      wx.showToast({
        title: '购物车为空',
        icon: 'none'
      });
      return;
    }
    
    // 检查是否已登录
    if (!auth.checkAuth()) {
      wx.showToast({
        title: '请先登录',
        icon: 'none'
      });
      return;
    }
    
    // 防止重复提交
    if (this.data.isSubmitting) {
      return;
    }
    
    this.setData({
      isSubmitting: true
    });
    
    // 获取用户ID
    const userId = wx.getStorageSync('userId');
    if (!userId) {
      wx.showModal({
        title: '提示',
        content: '登录信息已失效，请重新登录',
        showCancel: false,
        success: () => {
          wx.redirectTo({
            url: '/pages/login/login'
          });
        }
      });
      return;
    }
    
    // 构建订单数据
    const orderData = {
      userId: userId,
      amount: this.data.totalAmount,
      orderDetails: this.data.cartList.map(item => ({
        dishId: item.id,
        specificationId: item.specificationId,
        number: item.quantity,
        amount: item.price * item.quantity
      })),
      payMethod: this.data.selectedPayMethod,
      remark: this.data.remark
    };
    
    // 如果有桌位ID，则添加到请求数据中
    if (this.data.tableId) {
      orderData.tableId = this.data.tableId;
    }
    
    // 提交订单
    request.post('/mini/order/create', orderData).then(res => {
      this.setData({
        isSubmitting: false
      });
      
      if (res.code === 1 && res.data) {
        // 订单创建成功
        const orderId = res.data;  // 直接使用 res.data 作为 orderId，不需要 res.data.id
        
        // 清空购物车
        cartUtil.clearCart();
        
        // 微信支付
        this.wxPay(orderId);
      } else {
        wx.showModal({
          title: '提示',
          content: res.msg || '创建订单失败',
          showCancel: false
        });
      }
    }).catch(err => {
      this.setData({
        isSubmitting: false
      });
      
      wx.showModal({
        title: '提示',
        content: '创建订单失败，请稍后再试',
        showCancel: false
      });
    });
  },
  
  /**
   * 发起微信支付
   */
  wxPay(orderId) {
    // 获取支付参数
    request.post('/mini/order/pay', { 
      id: orderId,
      payMethod: this.data.selectedPayMethod || 1 // 使用选中的支付方式，默认为微信支付
    }).then(res => {
      if (res.code === 1) {
        // 已经支付成功的情况（服务器可能直接更新了订单状态）
        if (!res.data || Object.keys(res.data).length === 0) {
          // 支付成功，跳转到订单详情
          wx.showToast({
            title: '支付成功',
            icon: 'success',
            duration: 2000,
            complete: () => {
              wx.navigateTo({
                url: '/pages/order/detail/detail?id=' + orderId
              });
            }
          });
          return;
        }
        
        // 调起微信支付
        wx.requestPayment({
          timeStamp: res.data.timeStamp,
          nonceStr: res.data.nonceStr,
          package: res.data.package,
          signType: res.data.signType,
          paySign: res.data.paySign,
          success: () => {
            // 支付成功，跳转到订单详情
            wx.showToast({
              title: '支付成功',
              icon: 'success',
              duration: 2000,
              complete: () => {
                wx.navigateTo({
                  url: '/pages/order/detail/detail?id=' + orderId
                });
              }
            });
          },
          fail: (err) => {
            console.error('微信支付失败:', err);
            wx.showModal({
              title: '支付失败',
              content: '您已取消支付',
              confirmText: '查看订单',
              success: (result) => {
                if (result.confirm) {
                  wx.navigateTo({
                    url: '/pages/order/detail/detail?id=' + orderId
                  });
                }
              }
            });
          }
        });
      } else {
        console.error('支付接口返回错误:', res);
        wx.showModal({
          title: '提示',
          content: res.msg || '发起支付失败',
          confirmText: '查看订单',
          success: (result) => {
            if (result.confirm) {
              wx.navigateTo({
                url: '/pages/order/detail/detail?id=' + orderId
              });
            }
          }
        });
      }
    }).catch(err => {
      console.error('支付接口请求异常:', err);
      wx.showModal({
        title: '提示',
        content: '网络异常，请稍后再试',
        confirmText: '查看订单',
        success: (result) => {
          if (result.confirm) {
            wx.navigateTo({
              url: '/pages/order/detail/detail?id=' + orderId
            });
          }
        }
      });
    });
  }
}))