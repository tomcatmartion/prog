// pages/category/category.js
const app = getApp();
const request = require('../../utils/request');
const cartUtil = require('../../utils/cart');
const auth = require('../../utils/auth');

// 使用登录验证混入
Page(auth.pageAuthMixin({
  /**
   * 页面的初始数据
   */
  data: {
    categories: [], // 分类列表
    dishes: [], // 当前分类下的菜品
    currentCategory: null, // 当前选中的分类
    scrollTop: 0, // 右侧菜品列表滚动位置
    tableId: null, // 桌位ID
    hasTableId: false, // 是否有桌位ID
    isLoading: false, // 加载状态
    // 规格选择相关数据
    showSpecDialog: false, // 是否显示规格选择对话框
    currentDish: null, // 当前选择的菜品
    selectedSpecIndex: -1, // 选中的规格索引
    activeCartIndex: -1 // 当前激活的购物车图标索引
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad: function (options) {
    console.log('分类页面加载，接收参数:', options);
    
    // 从options中获取桌号信息，如果存在则优先使用
    if (options && options.tableId) {
      app.globalData.tableId = options.tableId;
      console.log('从URL参数获取桌号:', options.tableId);
    }
    
    this.initTableInfo();
    
    // 加载分类列表
    this.loadCategories();
  },
  
  /**
   * 初始化桌号信息
   */
  initTableInfo() {
    // 从全局数据获取桌位ID
    const globalTableId = app.globalData.tableId;
    
    if (globalTableId) {
      this.setData({
        tableId: globalTableId,
        hasTableId: true
      });
      console.log('桌号信息初始化成功:', globalTableId);
    } else {
      this.setData({
        tableId: null,
        hasTableId: false
      });
      console.log('无桌号信息，使用默认点餐模式');
    }
  },
  
  /**
   * 加载分类列表
   */
  loadCategories() {
    this.setData({
      isLoading: true
    });
    
    request.get('/mini/category/list').then(res => {
      if (res.code === 1 && res.data) {
        // 设置分类数据
        this.setData({
          categories: res.data,
          currentCategory: res.data.length > 0 ? res.data[0].id : null,
          isLoading: false
        });
        
        // 加载第一个分类的菜品
        if (res.data.length > 0) {
          this.loadDishes(res.data[0].id);
        }
      } else {
        this.setData({
          isLoading: false
        });
      }
    }).catch(() => {
      this.setData({
        isLoading: false
      });
    });
  },
  
  /**
   * 根据分类ID加载菜品
   */
  loadDishes(categoryId) {
    if (!categoryId) return;
    
    this.setData({
      isLoading: true
    });
    
    request.get('/mini/dish/list', { categoryId }).then(res => {
      if (res.code === 1) {
        this.setData({
          dishes: res.data || [],
          isLoading: false
        });
      } else {
        this.setData({
          dishes: [],
          isLoading: false
        });
      }
    }).catch(() => {
      this.setData({
        dishes: [],
        isLoading: false
      });
    });
  },
  
  /**
   * 切换分类
   */
  switchCategory(e) {
    const categoryId = e.currentTarget.dataset.id;
    if (categoryId !== this.data.currentCategory) {
      this.setData({
        currentCategory: categoryId,
        scrollTop: 0 // 切换分类时滚动到顶部
      });
      
      // 加载当前分类下的菜品
      this.loadDishes(categoryId);
    }
  },
  
  /**
   * 点击菜品
   */
  onTapDish(e) {
    const dishId = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: '/pages/dish/dish?id=' + dishId
    });
  },
  
  /**
   * 添加到购物车
   */
  onAddToCart(e) {
    const index = e.currentTarget.dataset.index;
    const dish = this.data.dishes[index];
    
    // 设置当前图标为激活状态
    this.setData({
      activeCartIndex: index
    });
    
    // 1.5秒后恢复为非激活状态
    setTimeout(() => {
      this.setData({
        activeCartIndex: -1
      });
    }, 1500);
    
    // 判断是否有多个规格
    if (dish.specifications && dish.specifications.length > 1) {
      // 弹出规格选择对话框
      this.setData({
        showSpecDialog: true,
        currentDish: dish,
        selectedSpecIndex: -1 // 重置选择的规格
      });
    } else if (dish.specifications && dish.specifications.length === 1) {
      // 只有一个规格，直接添加到购物车
      const specDish = {...dish};
      specDish.selectedSpecification = dish.specifications[0];
      specDish.price = dish.specifications[0].price; // 使用规格价格
      
      // 添加到购物车
      cartUtil.addToCart(specDish);
      
      // 更新购物车组件
      this.selectComponent('#cartBar').updateCart();
      
      wx.showToast({
        title: '已加入购物车',
        icon: 'success',
        duration: 1000
      });
    } else {
      // 无规格，直接添加到购物车
      cartUtil.addToCart(dish);
      
      // 更新购物车组件
      this.selectComponent('#cartBar').updateCart();
      
      wx.showToast({
        title: '已加入购物车',
        icon: 'success',
        duration: 1000
      });
    }
  },
  
  /**
   * 选择规格
   */
  selectSpecification(e) {
    const index = e.currentTarget.dataset.index;
    this.setData({
      selectedSpecIndex: index
    });
  },
  
  /**
   * 确认选择规格并添加到购物车
   */
  confirmAddToCart() {
    const { currentDish, selectedSpecIndex } = this.data;
    
    if (selectedSpecIndex === -1) {
      wx.showToast({
        title: '请选择规格',
        icon: 'none',
        duration: 1500
      });
      return;
    }
    
    // 创建一个新对象，包含菜品信息和选中的规格
    const specDish = {...currentDish};
    specDish.selectedSpecification = currentDish.specifications[selectedSpecIndex];
    specDish.price = currentDish.specifications[selectedSpecIndex].price; // 使用规格价格
    
    // 添加到购物车
    cartUtil.addToCart(specDish);
    
    // 更新购物车组件
    this.selectComponent('#cartBar').updateCart();
    
    // 关闭对话框
    this.closeSpecDialog();
    
    wx.showToast({
      title: '已加入购物车',
      icon: 'success',
      duration: 1000
    });
  },
  
  /**
   * 关闭规格选择对话框
   */
  closeSpecDialog() {
    this.setData({
      showSpecDialog: false,
      currentDish: null,
      selectedSpecIndex: -1
    });
  },
  
  /**
   * 更新购物车
   */
  updateCart() {
    // 更新购物车组件
    const cartBar = this.selectComponent('#cartBar');
    if (cartBar) {
      cartBar.updateCart();
    }
  },
  
  /**
   * 页面显示时检查登录状态
   */
  onShow: function () {
    // 检查登录状态
    auth.validateLogin();
    
    // 每次页面显示时更新桌号信息
    this.initTableInfo();
    
    // 更新购物车状态
    this.updateCart();
    
    console.log('点餐页面显示，当前桌号:', this.data.tableId || '无桌号');

    // 添加页面标题
    wx.setNavigationBarTitle({
      title: this.data.hasTableId ? '点餐 (桌号:' + this.data.tableId + ')' : '点餐'
    });
  },
  
  /**
   * 返回首页
   */
  backToHome: function() {
    wx.reLaunch({
      url: '/pages/index/index'
    });
  },
  
  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh: function () {
    // 刷新分类和菜品数据
    this.loadCategories();
    wx.stopPullDownRefresh();
  },
  
  /**
   * 用户点击右上角分享
   */
  onShareAppMessage: function () {
    return {
      title: '菜品分类 - 扫码点餐',
      path: '/pages/category/category'
    };
  }
})) 