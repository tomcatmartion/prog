import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import './assets/css/global.css'
import axios from 'axios'
import { getToken } from '@/utils/auth'

// DEBUG模式
const DEBUG = true

Vue.config.productionTip = false
Vue.use(ElementUI)

// 配置axios
axios.defaults.baseURL = ''
axios.defaults.withCredentials = true // 允许携带cookie
axios.defaults.timeout = 10000 // 10秒超时

// 添加请求拦截器
axios.interceptors.request.use(config => {
  // 从auth工具函数获取token，确保获取到最新token
  const token = getToken()
  
  if (DEBUG) {
    console.group('全局axios请求')
    console.log('请求URL:', config.url)
    console.log('token状态:', token ? '有token' : '无token')
  }
  
  if (token) {
    // 为请求头添加token，使用JWT格式
    config.headers['Authorization'] = 'Bearer ' + token
    
    if (DEBUG) {
      console.log('添加Authorization头:', 'Bearer ' + token)
    }
  }
  
  if (DEBUG) {
    console.log('完整请求配置:', config)
    console.groupEnd()
  }
  
  return config
}, error => {
  console.error('全局请求拦截器错误:', error)
  return Promise.reject(error)
})

// 添加响应拦截器
axios.interceptors.response.use(
  response => {
    if (DEBUG) {
      console.group('全局axios响应')
      console.log('响应状态:', response.status)
      console.log('响应数据:', response.data)
      console.groupEnd()
    }
    return response
  },
  error => {
    console.group('全局axios响应错误')
    console.error('错误信息:', error)
    
    if (error.response) {
      console.error('错误状态码:', error.response.status)
      console.error('错误数据:', error.response.data)
    }
    
    console.groupEnd()
    
    // 处理401未授权错误
    if (error.response && error.response.status === 401) {
      ElementUI.Message.error('登录状态已过期，请重新登录')
      // 清除token
      store.dispatch('resetToken')
      // 跳转到登录页
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

Vue.prototype.$http = axios

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app') 