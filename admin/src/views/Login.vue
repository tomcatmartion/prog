<template>
  <div class="login-container">
    <div class="login-box">
      <div class="title">餐厅后台管理系统</div>
      <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" class="login-form">
        <el-form-item prop="username">
          <el-input v-model="loginForm.username" prefix-icon="el-icon-user" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="loginForm.password" prefix-icon="el-icon-lock" type="password" placeholder="请输入密码"></el-input>
        </el-form-item>
        <el-form-item class="btn-container">
          <el-button type="primary" @click="login">登录</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Login',
  data() {
    return {
      loginForm: {
        username: 'admin',
        password: '123456'
      },
      loginRules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    resetForm() {
      this.$refs.loginFormRef.resetFields()
    },
    login() {
      this.$refs.loginFormRef.validate(async valid => {
        if (!valid) return
        try {
          console.log('开始登录请求...')
          const result = await this.$store.dispatch('login', this.loginForm)
          console.log('登录成功，返回数据:', result)
          this.$message.success('登录成功')
          this.$router.push('/home')
        } catch (error) {
          console.error('登录失败:', error)
          this.$message.error(typeof error === 'string' ? error : '登录失败')
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.login-container {
  height: 100%;
  background-color: #2b4b6b;
  display: flex;
  justify-content: center;
  align-items: center;

  .login-box {
    width: 450px;
    background-color: #fff;
    border-radius: 3px;
    padding: 20px;

    .title {
      font-size: 22px;
      text-align: center;
      margin-bottom: 20px;
    }

    .btn-container {
      display: flex;
      justify-content: flex-end;
    }
  }
}
</style> 