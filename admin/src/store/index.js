import Vue from 'vue'
import Vuex from 'vuex'
import { getToken, setToken, removeToken } from '@/utils/auth'
import axios from 'axios'

Vue.use(Vuex)

export default new Vuex.Store({
  state: {
    token: getToken(),
    employee: JSON.parse(window.sessionStorage.getItem('employeeInfo') || '{}')
  },
  getters: {
    token: state => state.token,
    employee: state => state.employee
  },
  mutations: {
    SET_TOKEN: (state, token) => {
      state.token = token
      if (token) {
        setToken(token)
      } else {
        removeToken()
      }
    },
    SET_EMPLOYEE: (state, employee) => {
      state.employee = employee
      window.sessionStorage.setItem('employeeInfo', JSON.stringify(employee))
    },
    CLEAR_INFO: (state) => {
      state.token = ''
      state.employee = {}
      removeToken()
      window.sessionStorage.removeItem('employeeInfo')
    }
  },
  actions: {
    // 登录
    login({ commit }, loginData) {
      return new Promise((resolve, reject) => {
        console.log('准备发送登录请求...')
        
        // 直接使用axios而不是Vue.prototype.$http，避免拦截器干扰
        axios.post('/admin/employee/login', loginData)
          .then(response => {
            console.log('登录API返回原始数据:', response)
            
            const responseData = response.data
            console.log('登录API返回处理后数据:', responseData)
            
            if (responseData.code === 1 && responseData.data) {
              const token = responseData.data.token
              const employee = responseData.data.employee
              
              if (!token) {
                console.error('登录成功但没有返回token')
                reject('登录成功但没有返回token')
                return
              }
              
              console.log('获取到token:', token)
              commit('SET_TOKEN', token)
              
              if (employee) {
                console.log('获取到employee信息:', employee)
                commit('SET_EMPLOYEE', employee)
              } else {
                console.warn('未获取到employee信息')
              }
              
              // 验证token是否被正确保存
              setTimeout(() => {
                const savedToken = getToken()
                console.log('保存后立即获取token:', savedToken)
              }, 100)
              
              resolve(responseData)
            } else {
              const errorMsg = responseData.msg || '登录失败'
              console.error('登录失败:', errorMsg)
              reject(errorMsg)
            }
          })
          .catch(error => {
            console.error('登录请求出错:', error)
            reject(error)
          })
      })
    },
    // 退出登录
    logout({ commit }) {
      commit('CLEAR_INFO')
    },
    // 重置token
    resetToken({ commit }) {
      commit('CLEAR_INFO')
    }
  },
  modules: {
  }
}) 