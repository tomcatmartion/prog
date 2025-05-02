module.exports = {
  devServer: {
    port: 8081,
    proxy: {
      '/admin': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true,
        pathRewrite: null
      }
    }
  },
  lintOnSave: false
} 