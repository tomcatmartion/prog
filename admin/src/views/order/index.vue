<template>
  <div class="app-container">
    <!-- 搜索和过滤 -->
    <div class="filter-container">
      <el-input
        v-model="listQuery.orderNo"
        placeholder="订单号"
        style="width: 200px;"
        class="filter-item"
        clearable
        @keyup.enter.native="handleFilter"
      />
      <el-select
        v-model="listQuery.status"
        placeholder="订单状态"
        clearable
        class="filter-item"
        style="width: 130px"
      >
        <el-option
          v-for="item in statusOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="yyyy-MM-dd"
        class="filter-item"
        @change="handleDateChange"
      />
      <el-button
        class="filter-item"
        type="primary"
        icon="el-icon-search"
        @click="handleFilter"
      >
        搜索
      </el-button>
    </div>


    <!-- 表格 -->
    <el-table
      v-loading="listLoading"
      :data="list"
      element-loading-text="正在加载..."
      border
      fit
      highlight-current-row
      style="width: 100%"
    >
      <el-table-column label="订单号" prop="number" align="center" width="180" />
      <el-table-column label="桌号" prop="tableCode" align="center" width="100" />
      <el-table-column label="金额" align="center" width="100">
        <template slot-scope="{row}">
          <span>¥{{ row.amount }}</span>
        </template>
      </el-table-column>
      <el-table-column label="订单状态" align="center" width="120">
        <template slot-scope="{row}">
          <el-tag :type="getStatusType(row.status)">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="支付方式" align="center" width="120">
        <template slot-scope="{row}">
          <span>{{ row.payMethod === 1 ? '微信支付' : '餐后支付' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="下单时间" align="center" width="180">
        <template slot-scope="{row}">
          <span>{{ row.createTime }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="260" class-name="small-padding fixed-width">
        <template slot-scope="{row}">
          <el-button 
            type="primary" 
            size="mini" 
            @click="handleView(row)"
          >
            查看
          </el-button>
          <el-button 
            v-if="row.status < 3" 
            type="danger" 
            size="mini" 
            @click="handleCancel(row)"
          >
            取消
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="listQuery.page"
      :limit.sync="listQuery.limit"
      @pagination="getList"
    />

    <!-- 取消订单对话框 -->
    <el-dialog
      title="取消订单"
      :visible.sync="cancelDialogVisible"
      width="30%"
    >
      <el-form ref="cancelForm" :model="cancelForm" :rules="cancelRules">
        <el-form-item label="取消原因" prop="reason">
          <el-input
            v-model="cancelForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入取消原因"
          />
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="cancelDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmCancel">确认</el-button>
      </span>
    </el-dialog>

    <!-- 订单详情对话框 -->
    <el-dialog
      title="订单详情"
      :visible.sync="detailDialogVisible"
      width="50%"
    >
      <div v-loading="detailLoading" class="order-detail">
        <div v-if="orderDetail" class="detail-content">
          <!-- 订单基本信息 -->
          <div class="detail-header">
            <div class="detail-item">
              <span class="label">订单号：</span>
              <span class="value">{{ orderDetail.number }}</span>
            </div>
            <div class="detail-item">
              <span class="label">下单时间：</span>
              <span class="value">{{ orderDetail.createTime }}</span>
            </div>
            <div class="detail-item">
              <span class="label">用户：</span>
              <span class="value">{{ orderDetail.userName }}</span>
            </div>
            <div class="detail-item">
              <span class="label">桌号：</span>
              <span class="value">{{ orderDetail.tableName || orderDetail.tableId }}</span>
            </div>
            <div class="detail-item" v-if="orderDetail.tableCode">
              <span class="label">桌位编码：</span>
              <span class="value">{{ orderDetail.tableCode }}</span>
            </div>
            <div class="detail-item">
              <span class="label">订单状态：</span>
              <el-tag :type="getStatusType(orderDetail.status)">
                {{ getStatusText(orderDetail.status) }}
              </el-tag>
            </div>
            <div class="detail-item">
              <span class="label">支付状态：</span>
              <el-tag :type="orderDetail.payStatus === 1 ? 'success' : 'info'">
                {{ orderDetail.payStatus === 1 ? '已支付' : '未支付' }}
              </el-tag>
            </div>
            <div class="detail-item">
              <span class="label">支付方式：</span>
              <span class="value">{{ orderDetail.payMethod === 1 ? '微信支付' : '餐后支付' }}</span>
            </div>
            <div class="detail-item" v-if="orderDetail.remark">
              <span class="label">备注：</span>
              <span class="value">{{ orderDetail.remark }}</span>
            </div>
          </div>

          <!-- 订单菜品列表 -->
          <div class="detail-dishes">
            <div class="section-title">订单菜品</div>
            <el-table :data="orderDetail.orderDetails" border style="width: 100%">
              <el-table-column label="菜品图片" width="100" align="center">
                <template slot-scope="{row}">
                  <img :src="row.dishImage" alt="菜品图片" class="dish-img">
                </template>
              </el-table-column>
              <el-table-column label="菜品名称" prop="dishName" />
              <el-table-column label="规格" prop="specificationName" width="100" />
              <el-table-column label="数量" width="80" align="center" prop="number" />
              <el-table-column label="金额" width="100" align="center">
                <template slot-scope="{row}">
                  <span>¥{{ row.amount }}</span>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 订单总计 -->
          <div class="detail-total">
            <div class="total-item">
              <span class="label">商品总数：</span>
              <span class="value">{{ getTotalQuantity() }} 件</span>
            </div>
            <div class="total-item">
              <span class="label">订单总计：</span>
              <span class="value price">¥{{ orderDetail.amount }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { 
  getOrderList, 
  getOrderDetail, 
  cancelOrder,
  getOrderStatistics 
} from '@/api/order'
import Pagination from '@/components/Pagination'

export default {
  name: 'OrderList',
  components: { Pagination },
  data() {
    return {
      list: [],
      total: 0,
      listLoading: true,
      listQuery: {
        page: 1,
        limit: 10,
        orderNo: undefined,
        status: undefined,
        startDate: undefined,
        endDate: undefined
      },
      statusOptions: [
        { label: '待支付', value: 1 },
        { label: '已支付', value: 2 },
        { label: '已完成', value: 3 },
        { label: '已取消', value: 4 }
      ],
      dateRange: [],
      statistics: {
        todayTotal: 0,
        pendingCount: 0,
        todayAmount: 0,
        monthAmount: 0
      },
      cancelDialogVisible: false,
      cancelForm: {
        id: undefined,
        reason: ''
      },
      cancelRules: {
        reason: [
          { required: true, message: '请输入取消原因', trigger: 'blur' },
          { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
        ]
      },
      detailDialogVisible: false,
      detailLoading: false,
      orderDetail: null
    }
  },
  created() {
    this.getList()
  },
  methods: {
    // 获取订单列表
    getList() {
      this.listLoading = true
      getOrderList(this.listQuery).then(response => {
        this.list = response.data.records
        this.total = response.data.total
        this.listLoading = false
      }).catch(() => {
        this.listLoading = false
      })
    },
    
    // 获取订单统计数据
    getStatistics() {
      getOrderStatistics().then(response => {
        this.statistics = response.data
      })
    },
    
    // 处理日期变化
    handleDateChange(val) {
      if (val) {
        this.listQuery.startDate = val[0]
        this.listQuery.endDate = val[1]
      } else {
        this.listQuery.startDate = undefined
        this.listQuery.endDate = undefined
      }
    },
    
    // 搜索
    handleFilter() {
      this.listQuery.page = 1
      this.getList()
    },
    
    // 获取状态类型
    getStatusType(status) {
      const statusMap = {
        1: 'warning',
        2: 'primary',
        3: 'success',
        4: 'info'
      }
      return statusMap[status]
    },
    
    // 获取状态文本
    getStatusText(status) {
      const statusMap = {
        1: '待支付',
        2: '已支付',
        3: '已完成',
        4: '已取消'
      }
      return statusMap[status]
    },
    
    // 查看订单详情
    handleView(row) {
      this.detailDialogVisible = true
      this.detailLoading = true
      getOrderDetail(row.id).then(response => {
        this.orderDetail = response.data
        this.detailLoading = false
      }).catch(() => {
        this.detailLoading = false
      })
    },
    
    // 取消订单
    handleCancel(row) {
      this.cancelForm.id = Number(row.id)
      this.cancelForm.reason = ''
      this.cancelDialogVisible = true
    },
    
    // 确认取消订单
    confirmCancel() {
      this.$refs.cancelForm.validate(valid => {
        if (valid) {
          try {
            cancelOrder(this.cancelForm.id, this.cancelForm.reason)
              .then(response => {
                this.$message({
                  type: 'success',
                  message: '订单已取消!'
                })
                this.cancelDialogVisible = false
                this.getList()
              })
              .catch(error => {
                let errorMsg = '取消订单失败，请重试'
                if (error.response && error.response.data && error.response.data.msg) {
                  errorMsg = error.response.data.msg
                }
                this.$message({
                  type: 'error',
                  message: errorMsg
                })
              })
          } catch (err) {
            console.error('取消订单出错:', err)
            this.$message({
              type: 'error',
              message: '取消订单失败，请重试'
            })
          }
        }
      })
    },
    
    // 计算订单总数量
    getTotalQuantity() {
      if (!this.orderDetail || !this.orderDetail.orderDetails) {
        return 0
      }
      return this.orderDetail.orderDetails.reduce((total, item) => {
        return total + item.number
      }, 0)
    }
  }
}
</script>

<style lang="scss" scoped>
.filter-container {
  padding-bottom: 10px;
  .filter-item {
    margin-bottom: 10px;
    margin-right: 10px;
  }
}

.dashboard-container {
  margin: 30px;
}

.dashboard-item {
  background-color: #fff;
  border-radius: 4px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.card-item {
  color: #666;
  text-align: center;
  
  .card-title {
    font-size: 14px;
    margin-bottom: 10px;
  }
  
  .card-num {
    font-size: 24px;
    font-weight: bold;
    color: #333;
  }
  
  .card-today {
    font-size: 18px;
    font-weight: bold;
    color: #409EFF;
  }
  
  .card-pending {
    color: #E6A23C;
  }
  
  .card-amount {
    color: #67C23A;
  }
}

// 订单详情样式
.order-detail {
  .detail-content {
    overflow: hidden;
  }
  
  .detail-header {
    margin-bottom: 20px;
    padding-bottom: 20px;
    border-bottom: 1px solid #eee;
  }
  
  .detail-item {
    margin-bottom: 10px;
    display: flex;
    
    &:last-child {
      margin-bottom: 0;
    }
    
    .label {
      width: 100px;
      color: #606266;
    }
    
    .value {
      flex: 1;
      color: #333;
    }
  }
  
  .section-title {
    font-size: 16px;
    font-weight: bold;
    margin-bottom: 15px;
    padding-left: 10px;
    border-left: 3px solid #409EFF;
  }
  
  .detail-dishes {
    margin-bottom: 20px;
  }
  
  .dish-img {
    width: 60px;
    height: 60px;
    border-radius: 4px;
    object-fit: cover;
  }
  
  .detail-total {
    padding: 15px;
    background-color: #f9f9f9;
    border-radius: 4px;
    
    .total-item {
      display: flex;
      justify-content: flex-end;
      margin-bottom: 10px;
      
      &:last-child {
        margin-bottom: 0;
      }
      
      .label {
        margin-right: 10px;
      }
      
      .price {
        color: #f56c6c;
        font-weight: bold;
      }
    }
  }
}
</style> 