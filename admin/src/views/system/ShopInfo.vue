<template>
  <div>
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/home' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>系统管理</el-breadcrumb-item>
      <el-breadcrumb-item>店铺信息配置</el-breadcrumb-item>
    </el-breadcrumb>

    <el-card>
      <el-form :model="shopInfo" :rules="shopInfoRules" ref="shopInfoRef" label-width="100px">
        <el-form-item label="店铺名称" prop="name">
          <el-input v-model="shopInfo.name"></el-input>
        </el-form-item>
        <el-form-item label="店铺标语" prop="slogan">
          <el-input v-model="shopInfo.slogan"></el-input>
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="shopInfo.phone"></el-input>
        </el-form-item>
        <el-form-item label="营业时间" prop="businessHours">
          <el-input v-model="shopInfo.businessHours"></el-input>
        </el-form-item>
        <el-form-item label="店铺地址" prop="address">
          <el-input v-model="shopInfo.address"></el-input>
        </el-form-item>
        <el-form-item label="营业状态">
          <el-switch
            v-model="shopInfo.status"
            :active-value="1"
            :inactive-value="0"
            active-text="营业中"
            inactive-text="休息中">
          </el-switch>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveShopInfo">保存</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'ShopInfo',
  data() {
    return {
      shopInfo: {
        id: 1,
        name: '',
        slogan: '',
        phone: '',
        businessHours: '',
        address: '',
        status: 1
      },
      shopInfoRules: {
        name: [
          { required: true, message: '请输入店铺名称', trigger: 'blur' }
        ],
        phone: [
          { required: true, message: '请输入联系电话', trigger: 'blur' }
        ],
        businessHours: [
          { required: true, message: '请输入营业时间', trigger: 'blur' }
        ],
        address: [
          { required: true, message: '请输入店铺地址', trigger: 'blur' }
        ]
      }
    }
  },
  created() {
    this.getShopInfo()
  },
  methods: {
    // 获取店铺信息
    async getShopInfo() {
      try {
        const { data: res } = await this.$http.get('/admin/shop/info')
        if (res.code !== 1) return this.$message.error('获取店铺信息失败')
        this.shopInfo = res.data
      } catch (error) {
        this.$message.error('获取店铺信息失败')
      }
    },
    // 保存店铺信息
    saveShopInfo() {
      this.$refs.shopInfoRef.validate(async valid => {
        if (!valid) return
        try {
          const { data: res } = await this.$http.post('/admin/shop/update', this.shopInfo)
          if (res.code !== 1) return this.$message.error(res.msg)
          this.$message.success('保存成功')
        } catch (error) {
          this.$message.error('保存失败')
        }
      })
    },
    // 重置表单
    resetForm() {
      this.$refs.shopInfoRef.resetFields()
    }
  }
}
</script>

<style scoped>
.avatar-uploader .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}
.avatar-uploader .el-upload:hover {
  border-color: #409EFF;
}
.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 120px;
  height: 120px;
  line-height: 120px;
  text-align: center;
}
.avatar {
  width: 120px;
  height: 120px;
  display: block;
}
</style> 