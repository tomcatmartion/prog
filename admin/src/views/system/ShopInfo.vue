<template>
  <div class="app-container">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span>店铺信息配置</span>
      </div>
      <el-form ref="form" :model="shopInfo" :rules="rules" label-width="100px">
        <!-- 基本信息 -->
        <el-divider content-position="left">基本信息</el-divider>
        <el-form-item label="店铺名称" prop="name">
          <el-input v-model="shopInfo.name" placeholder="请输入店铺名称"></el-input>
        </el-form-item>
        <el-form-item label="店铺标语" prop="slogan">
          <el-input v-model="shopInfo.slogan" placeholder="请输入店铺标语"></el-input>
        </el-form-item>
        <el-form-item label="店铺Logo">
          <el-upload
            class="avatar-uploader"
            action="#"
            :http-request="uploadLogo"
            :show-file-list="false"
            :before-upload="beforeAvatarUpload">
            <img v-if="shopInfo.logo" :src="shopInfo.logo" class="avatar">
            <i v-else class="el-icon-plus avatar-uploader-icon"></i>
          </el-upload>
          <div class="el-upload__tip">建议上传正方形图片，大小不超过2MB</div>
        </el-form-item>
        
        <!-- 联系信息 -->
        <el-divider content-position="left">联系信息</el-divider>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="shopInfo.phone" placeholder="请输入联系电话"></el-input>
        </el-form-item>
        <el-form-item label="营业时间" prop="businessHours">
          <el-input v-model="shopInfo.businessHours" placeholder="例如：10:00-22:00"></el-input>
        </el-form-item>
        <el-form-item label="店铺地址" prop="address">
          <el-input v-model="shopInfo.address" placeholder="请输入店铺地址"></el-input>
        </el-form-item>
        
        <!-- 位置信息 -->
        <el-divider content-position="left">位置信息</el-divider>
        <el-form-item label="经度" prop="longitude">
          <el-input v-model="shopInfo.longitude" placeholder="请输入经度"></el-input>
        </el-form-item>
        <el-form-item label="纬度" prop="latitude">
          <el-input v-model="shopInfo.latitude" placeholder="请输入纬度"></el-input>
        </el-form-item>
        
        <!-- 营业状态 -->
        <el-divider content-position="left">营业状态</el-divider>
        <el-form-item label="营业状态">
          <el-switch
            v-model="shopInfo.status"
            :active-value="1"
            :inactive-value="0"
            active-text="营业中"
            inactive-text="休息中">
          </el-switch>
        </el-form-item>
        
        <!-- 按钮区域 -->
        <el-form-item>
          <el-button type="primary" @click="submitForm">保存配置</el-button>
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
        logo: '',
        address: '',
        phone: '',
        businessHours: '',
        longitude: '',
        latitude: '',
        status: 1
      },
      rules: {
        name: [
          { required: true, message: '请输入店铺名称', trigger: 'blur' },
          { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
        ],
        slogan: [
          { max: 100, message: '长度不能超过 100 个字符', trigger: 'blur' }
        ],
        phone: [
          { required: true, message: '请输入联系电话', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$|^0\d{2,3}-?\d{7,8}$/, message: '请输入正确的电话号码', trigger: 'blur' }
        ],
        address: [
          { required: true, message: '请输入店铺地址', trigger: 'blur' }
        ]
      }
    }
  },
  created() {
    this.fetchShopInfo()
  },
  methods: {
    // 获取店铺信息
    fetchShopInfo() {
      this.$axios.get('/admin/shop/info').then(res => {
        if (res.data.code === 1) {
          this.shopInfo = res.data.data
        } else {
          this.$message.error(res.data.msg || '获取店铺信息失败')
        }
      }).catch(() => {
        this.$message.error('网络异常，请稍后再试')
      })
    },
    // 提交表单
    submitForm() {
      this.$refs.form.validate(valid => {
        if (valid) {
          this.$axios.post('/admin/shop/update', this.shopInfo).then(res => {
            if (res.data.code === 1) {
              this.$message.success('保存成功')
            } else {
              this.$message.error(res.data.msg || '保存失败')
            }
          }).catch(() => {
            this.$message.error('网络异常，请稍后再试')
          })
        } else {
          return false
        }
      })
    },
    // 重置表单
    resetForm() {
      this.fetchShopInfo()
    },
    // 上传Logo前的验证
    beforeAvatarUpload(file) {
      const isJPG = file.type === 'image/jpeg' || file.type === 'image/png'
      const isLt2M = file.size / 1024 / 1024 < 2

      if (!isJPG) {
        this.$message.error('上传头像图片只能是 JPG 或 PNG 格式!')
      }
      if (!isLt2M) {
        this.$message.error('上传头像图片大小不能超过 2MB!')
      }
      return isJPG && isLt2M
    },
    // 上传Logo
    uploadLogo(option) {
      const formData = new FormData()
      formData.append('file', option.file)
      
      this.$axios.post('/admin/upload/image', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      }).then(res => {
        if (res.data.code === 1) {
          this.shopInfo.logo = res.data.data
          this.$message.success('上传成功')
        } else {
          this.$message.error(res.data.msg || '上传失败')
        }
      }).catch(() => {
        this.$message.error('网络异常，请稍后再试')
      })
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