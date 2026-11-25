<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="brand">AniLink Admin</div>
      <el-menu :default-active="route.path" router class="menu">
        <el-menu-item index="/videos">视频稿件</el-menu-item>
        <el-menu-item index="/users">用户管理</el-menu-item>
        <el-menu-item index="/storage">对象存储</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="muted">管理后台</span>
        <div class="user">
          <span>{{ auth.profile?.nickname || auth.profile?.username }}</span>
          <el-tag size="small" :type="auth.isSuper ? 'danger' : 'warning'">
            {{ auth.isSuper ? '超级管理员' : '管理员' }}
          </el-tag>
          <el-button link type="primary" @click="logout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

async function logout() {
  try {
    await ElMessageBox.confirm('确定退出管理端吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  await auth.signOut()
  router.replace('/login')
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
}

.aside {
  background: #1f2d3d;
  color: #fff;
}

.brand {
  height: 56px;
  line-height: 56px;
  padding: 0 20px;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.menu {
  border-right: none;
  background: transparent;
}

.menu :deep(.el-menu-item) {
  color: #c0c4cc;
}

.menu :deep(.el-menu-item.is-active) {
  color: #fff;
  background: #409eff;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid var(--admin-line);
}

.user {
  display: flex;
  gap: 10px;
  align-items: center;
}

.main {
  padding: 0;
  background: var(--admin-bg);
}
</style>
