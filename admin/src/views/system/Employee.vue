<template>
  <div>
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/home' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>系统管理</el-breadcrumb-item>
      <el-breadcrumb-item>员工管理</el-breadcrumb-item>
    </el-breadcrumb>

    <el-card>
      <el-row :gutter="20">
        <el-col :span="8">
          <el-input placeholder="请输入员工姓名" v-model="queryInfo.name" clearable>
            <el-button slot="append" icon="el-icon-search" @click="getEmployeeList"></el-button>
          </el-input>
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="dialogVisible = true">添加员工</el-button>
        </el-col>
      </el-row>

      <el-table :data="employeeList" stripe border style="width: 100%" v-loading="loading">
        <el-table-column type="index" label="#" width="50"></el-table-column>
        <el-table-column prop="username" label="账号" width="180"></el-table-column>
        <el-table-column prop="name" label="姓名" width="180"></el-table-column>
        <el-table-column prop="phone" label="手机号"></el-table-column>
        <el-table-column prop="role" label="角色">
          <template slot-scope="scope">
            <el-tag type="success" v-if="scope.row.role === 1">管理员</el-tag>
            <el-tag type="info" v-else>员工</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template slot-scope="scope">
            <el-button type="primary" icon="el-icon-edit" size="mini" @click="editEmployee(scope.row)">编辑</el-button>
            <el-button type="danger" icon="el-icon-delete" size="mini" @click="deleteEmployee(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="queryInfo.page"
        :page-sizes="[5, 10, 15, 20]"
        :page-size="queryInfo.pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total">
      </el-pagination>
    </el-card>

    <!-- 添加/编辑员工对话框 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="40%">
      <el-form :model="employeeForm" :rules="employeeFormRules" ref="employeeFormRef" label-width="80px">
        <el-form-item label="账号" prop="username">
          <el-input v-model="employeeForm.username" :disabled="isEdit"></el-input>
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="employeeForm.name"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="employeeForm.password" type="password"></el-input>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="employeeForm.phone"></el-input>
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="employeeForm.role" placeholder="请选择角色">
            <el-option label="管理员" :value="1"></el-option>
            <el-option label="员工" :value="2"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'Employee',
  data() {
    return {
      // 查询参数
      queryInfo: {
        page: 1,
        pageSize: 10,
        name: ''
      },
      // 员工列表
      employeeList: [],
      // 总条数
      total: 0,
      // 加载状态
      loading: false,
      // 对话框可见性
      dialogVisible: false,
      // 对话框标题
      dialogTitle: '添加员工',
      // 是否是编辑
      isEdit: false,
      // 员工表单
      employeeForm: {
        id: '',
        username: '',
        name: '',
        password: '',
        phone: '',
        role: 2
      },
      // 表单验证规则
      employeeFormRules: {
        username: [
          { required: true, message: '请输入账号', trigger: 'blur' },
          { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
        ],
        name: [
          { required: true, message: '请输入姓名', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
        ],
        phone: [
          { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
        ],
        role: [
          { required: true, message: '请选择角色', trigger: 'change' }
        ]
      }
    }
  },
  created() {
    this.getEmployeeList()
  },
  methods: {
    // 获取员工列表
    async getEmployeeList() {
      this.loading = true
      try {
        const { data: res } = await this.$http.get('/admin/employee/page', {
          params: this.queryInfo
        })
        this.loading = false
        if (res.code !== 1) return this.$message.error('获取员工列表失败')
        this.employeeList = res.data.records
        this.total = res.data.total
      } catch (error) {
        this.loading = false
        this.$message.error('获取员工列表失败')
      }
    },
    // 页码改变
    handleCurrentChange(page) {
      this.queryInfo.page = page
      this.getEmployeeList()
    },
    // 每页条数改变
    handleSizeChange(pageSize) {
      this.queryInfo.pageSize = pageSize
      this.getEmployeeList()
    },
    // 编辑员工
    editEmployee(row) {
      this.isEdit = true
      this.dialogTitle = '编辑员工'
      this.employeeForm = { ...row }
      delete this.employeeForm.password
      this.dialogVisible = true
    },
    // 删除员工
    async deleteEmployee(id) {
      const confirmResult = await this.$confirm('此操作将永久删除该员工, 是否继续?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).catch(err => err)
      
      if (confirmResult !== 'confirm') return
      try {
        const { data: res } = await this.$http.post('/admin/employee/delete', { id })
        if (res.code !== 1) return this.$message.error(res.msg || '删除员工失败')
        this.$message.success('删除员工成功')
        this.getEmployeeList()
      } catch (error) {
        this.$message.error('删除员工失败')
      }
    },
    // 提交表单
    submitForm() {
      this.$refs.employeeFormRef.validate(async valid => {
        if (!valid) return
        try {
          const url = this.isEdit ? '/admin/employee/update' : '/admin/employee/add'
          const { data: res } = await this.$http.post(url, this.employeeForm)
          if (res.code !== 1) return this.$message.error(res.msg || '操作失败')
          this.$message.success('操作成功')
          this.dialogVisible = false
          this.getEmployeeList()
        } catch (error) {
          this.$message.error('操作失败')
        }
      })
    }
  }
}
</script> 