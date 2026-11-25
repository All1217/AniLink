import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
  },
  {
    path: '/',
    component: () => import('@/layout/AdminLayout.vue'),
    redirect: '/videos',
    children: [
      { path: 'videos', name: 'videos', component: () => import('@/views/VideoView.vue') },
      { path: 'users', name: 'users', component: () => import('@/views/UserView.vue') },
      { path: 'storage', name: 'storage', component: () => import('@/views/StorageView.vue') },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/videos' },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.path === '/login') {
    return true
  }
  if (!auth.token) {
    return '/login'
  }
  if (!auth.profile && !(await auth.loadMe())) {
    return '/login'
  }
  return true
})

export default router
