import Vue from 'vue'
import VueRouter from 'vue-router'
import { getToken } from '@/utils/auth'

Vue.use(VueRouter)

// 路由规则
const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/home',
    component: () => import('@/views/Home.vue'),
    redirect: '/welcome',
    children: [
      { path: '/welcome', component: () => import('@/views/Welcome.vue') },
      // 系统管理
      { path: '/system/shop-info', component: () => import('@/views/system/ShopInfo.vue') },
      { path: '/system/employee', component: () => import('@/views/system/Employee.vue') },
      // 菜品管理
      { path: '/dish/list', component: () => import('@/views/dish/index.vue') },
      { path: '/category/list', component: () => import('@/views/category/index.vue') },
      // 订单管理
      { path: '/order/list', component: () => import('@/views/order/index.vue') },
      { path: '/order/statistics', component: () => import('@/views/statistics/index.vue') },
      // 桌台管理
      { path: '/table/list', component: () => import('@/views/table/index.vue') }
    ]
  }
]

const router = new VueRouter({
  routes
})

// 导航守卫
router.beforeEach((to, from, next) => {
  if (to.path === '/login') return next()
  
  // 使用getToken获取token
  const token = getToken()
  console.log('路由守卫检查token:', token)
  
  if (!token) {
    console.log('无token，跳转到登录页')
    return next('/login')
  }
  
  console.log('有token，允许访问:', to.path)
  next()
})

export default router 