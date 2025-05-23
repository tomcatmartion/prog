<template>
  <el-container class="home-container">
    <!-- 头部 -->
    <el-header>
      <div class="logo">餐厅管理系统</div>
      <div>
        <el-button type="info" @click="logout">退出</el-button>
      </div>
    </el-header>
    <!-- 主体 -->
    <el-container>
      <!-- 侧边栏 -->
      <el-aside width="200px">
        <el-menu
          :default-active="activePath"
          background-color="#333744"
          text-color="#fff"
          active-text-color="#409EFF"
          unique-opened
          router
        >
          <!-- 首页 -->
          <el-menu-item index="/welcome">
            <i class="el-icon-s-home"></i>
            <span>首页</span>
          </el-menu-item>
          
          <!-- 商品管理 -->
          <el-submenu index="1">
            <template slot="title">
              <i class="el-icon-dish"></i>
              <span>菜品管理</span>
            </template>
            <el-menu-item index="/dish/list">
              <i class="el-icon-menu"></i>
              <span>菜品列表</span>
            </el-menu-item>
            <el-menu-item index="/category/list">
              <i class="el-icon-collection"></i>
              <span>分类管理</span>
            </el-menu-item>
          </el-submenu>
          
          <!-- 订单管理 -->
          <el-submenu index="2">
            <template slot="title">
              <i class="el-icon-s-order"></i>
              <span>订单管理</span>
            </template>
            <el-menu-item index="/order/list">
              <i class="el-icon-tickets"></i>
              <span>订单列表</span>
            </el-menu-item>
            <el-menu-item index="/order/statistics">
              <i class="el-icon-data-analysis"></i>
              <span>销售统计</span>
            </el-menu-item>
          </el-submenu>
          
          <!-- 桌台管理 -->
          <el-submenu index="3">
            <template slot="title">
              <i class="el-icon-s-grid"></i>
              <span>桌台管理</span>
            </template>
            <el-menu-item index="/table/list">
              <i class="el-icon-s-unfold"></i>
              <span>桌台列表</span>
            </el-menu-item>
          </el-submenu>
          
          <!-- 系统管理 -->
          <el-submenu index="4">
            <template slot="title">
              <i class="el-icon-setting"></i>
              <span>系统管理</span>
            </template>
            <el-menu-item index="/system/shop-info">
              <i class="el-icon-shop"></i>
              <span>店铺信息配置</span>
            </el-menu-item>
            <el-menu-item index="/system/employee">
              <i class="el-icon-user"></i>
              <span>员工管理</span>
            </el-menu-item>
          </el-submenu>
        </el-menu>
      </el-aside>
      <!-- 内容区域 -->
      <el-main>
        <router-view></router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
export default {
  name: 'Home',
  data() {
    return {
      activePath: '/welcome'
    }
  },
  created() {
    this.activePath = window.sessionStorage.getItem('activePath') || this.activePath
  },
  methods: {
    logout() {
      this.$store.dispatch('logout')
      this.$router.push('/login')
    },
    saveNavState(activePath) {
      window.sessionStorage.setItem('activePath', activePath)
      this.activePath = activePath
    }
  }
}
</script>

<style lang="scss" scoped>
.home-container {
  height: 100%;
}

.el-header {
  background-color: #373d41;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #fff;
  font-size: 20px;
}

.el-aside {
  background-color: #333744;

  .el-menu {
    border-right: none;
  }
}

.el-main {
  background-color: #eaedf1;
}
</style> 