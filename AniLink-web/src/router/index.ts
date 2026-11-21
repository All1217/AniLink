import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

/** 无需登录即可访问的路径前缀 */
const PUBLIC_PREFIX = ['/home', '/video', '/search']

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/home' },
  { path: '/home', name: 'home', component: () => import('@/views/home/HomeView.vue') },
  { path: '/search', name: 'search', component: () => import('@/views/search/SearchView.vue') },
  { path: '/video/:vid', name: 'video', component: () => import('@/views/video/VideoView.vue') },
  { path: '/user/:uid', name: 'user', component: () => import('@/views/user/UserView.vue') },
  { path: '/message', name: 'message', component: () => import('@/views/message/MessageView.vue') },
  { path: '/upload', name: 'upload', component: () => import('@/views/upload/UploadView.vue') },
  { path: '/history', name: 'history', component: () => import('@/views/history/HistoryView.vue') },
  { path: '/account', name: 'account', component: () => import('@/views/account/AccountView.vue') },
  { path: '/:pathMatch(.*)*', redirect: '/home' },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ left: 0, top: 0 }),
})

router.beforeEach((to) => {
  const store = useUserStore()
  if (store.isLogin) return true

  const isPublic = PUBLIC_PREFIX.some((prefix) => to.path.startsWith(prefix))
  if (isPublic) return true

  // 未登录访问需要登录的页面：回首页并带上来路，同时打开登录弹窗
  store.openLoginDialog()
  return { path: '/home', query: { redirect: to.fullPath } }
})

export default router
