import axios from 'axios'
import { MessageBox, Message } from 'element-ui'
import store from '@/store'
import { getToken } from '@/utils/auth'

// DEBUG模式
const DEBUG = true

// 创建 axios 实例
const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API || '', // url = base url + request url
  timeout: 10000, // 请求超时时间
  withCredentials: true // 跨域请求时发送cookies
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 在发送请求之前做一些处理
    const token = getToken()
    
    if (DEBUG) {
      console.group('请求拦截')
      console.log('请求URL:', config.url)
      console.log('请求方法:', config.method)
      console.log('请求数据:', config.data || config.params)
      console.log('Token状态:', token ? '有token' : '无token')
    }
    
    if (token) {
      // 修改为JWT格式的Authorization头部，添加Bearer前缀
      config.headers['Authorization'] = 'Bearer ' + token
      
      if (DEBUG) {
        console.log('Authorization头:', 'Bearer ' + token)
      }
    }
    
    if (DEBUG) {
      console.log('完整请求配置:', config)
      console.groupEnd()
    }
    
    return config
  },
  error => {
    // 处理请求错误
    console.error('请求拦截器错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  /**
   * 通过判断状态码统一处理响应，根据项目需求可做相应修改
   */
  response => {
    if (DEBUG) {
      console.group('响应拦截')
      console.log('响应状态:', response.status)
      console.log('响应数据:', response.data)
      console.groupEnd()
    }
    
    const res = response.data

    // 如果自定义code不是1，则判断为错误
    if (res.code !== 1) {
      Message({
        message: res.msg || '系统错误',
        type: 'error',
        duration: 5 * 1000
      })

      // 50008: 非法token; 50012: 其他客户端已登录; 50014: Token过期;
      if (res.code === 50008 || res.code === 50012 || res.code === 50014) {
        // 重新登录
        MessageBox.confirm(
          '您已登出，请重新登录',
          '确认登出',
          {
            confirmButtonText: '重新登录',
            cancelButtonText: '取消',
            type: 'warning'
          }
        ).then(() => {
          store.dispatch('resetToken').then(() => {
            location.reload()
          })
        })
      }
      return Promise.reject(new Error(res.msg || '系统错误'))
    } else {
      return res
    }
  },
  error => {
    console.group('响应错误')
    console.error('完整错误对象:', error)
    
    if (error.response) {
      console.error('错误响应状态:', error.response.status)
      console.error('错误响应数据:', error.response.data)
    } else {
      console.error('无响应对象，可能是网络错误')
    }
    
    console.groupEnd()
    
    // 处理401未授权错误，自动跳转到登录页
    if (error.response && error.response.status === 401) {
      // 清除token
      store.dispatch('resetToken')
      // 跳转到登录页
      window.location.href = '/#/login'
      Message({
        message: '登录状态已过期，请重新登录',
        type: 'error',
        duration: 5 * 1000
      })
    } else {
      Message({
        message: error.message,
        type: 'error',
        duration: 5 * 1000
      })
    }
    return Promise.reject(error)
  }
)

export default service 