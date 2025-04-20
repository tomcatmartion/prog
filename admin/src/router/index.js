{
  path: '/system',
  component: Layout,
  redirect: '/system/user',
  name: 'System',
  meta: { title: '系统管理', icon: 'el-icon-setting' },
  children: [
    {
      path: 'shop-info',
      name: 'ShopInfo',
      component: () => import('@/views/system/ShopInfo'),
      meta: { title: '店铺信息配置', icon: 'el-icon-shop' }
    }
  ]
} 