import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'

// Element Plus 按需引入（组件由 unplugin-vue-components 自动注册）；
// 函数式组件的样式需要手动引入。
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/loading/style/css'

import '@/styles/index.scss'
import App from './App.vue'
import router from './router'
import { onUnauthorized } from '@/api'
import { useUserStore } from '@/stores/user'

const app = createApp(App)
const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

app.use(pinia)
app.use(router)

// token 失效时统一回到首页并唤起登录
onUnauthorized(() => {
  router.replace({ path: '/home' })
  useUserStore().openLoginDialog()
})

// 有持久化 token 时恢复用户信息（失败不阻塞首屏）
const store = useUserStore()
if (store.token) {
  store.loadUserInfo().catch(() => store.reset())
}

app.mount('#app')
