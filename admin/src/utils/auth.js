import Cookies from 'js-cookie'

const TokenKey = 'Admin-Token'

export function getToken() {
  // 优先从sessionStorage获取token，因为我们现在主要使用这种方式存储
  const sessionToken = window.sessionStorage.getItem('token')
  if (sessionToken) {
    return sessionToken
  }
  // 后备选项：从cookie获取
  return Cookies.get(TokenKey)
}

export function setToken(token) {
  // 同时存储在sessionStorage和cookie中
  window.sessionStorage.setItem('token', token)
  return Cookies.set(TokenKey, token)
}

export function removeToken() {
  // 同时从sessionStorage和cookie中移除
  window.sessionStorage.removeItem('token')
  return Cookies.remove(TokenKey)
}