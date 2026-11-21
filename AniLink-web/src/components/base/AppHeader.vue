<template>
  <header class="header">
    <div class="header__inner">
      <RouterLink to="/home" class="header__logo">
        <span class="header__logo-mark">Ani</span>
        <span class="header__logo-text">Link</span>
      </RouterLink>

      <nav class="header__nav">
        <RouterLink to="/home" class="header__nav-item" :class="{ 'is-active': route.path === '/home' }">首页</RouterLink>
        <el-popover placement="bottom-start" :width="520" trigger="hover" popper-class="partition-popover">
          <template #reference>
            <span class="header__nav-item">分区</span>
          </template>
          <div class="partition">
            <a v-for="item in partitions" :key="item.id" class="partition__item" @click="notReady">{{ item.name }}</a>
          </div>
        </el-popover>
        <span class="header__nav-item" @click="notReady">热门</span>
        <span class="header__nav-item" @click="notReady">直播</span>
      </nav>

      <SearchBox class="header__search" />

      <div class="header__actions">
        <button class="header__upload" @click="goUpload">投稿</button>

        <RouterLink v-if="userStore.isLogin" to="/message" class="header__icon" title="消息">
          <el-icon><Bell /></el-icon>
          <span v-if="unread > 0" class="header__badge">{{ unread > 99 ? '99+' : unread }}</span>
        </RouterLink>

        <el-dropdown v-if="userStore.isLogin" trigger="hover" @command="onCommand">
          <span class="header__avatar">
            <img :src="avatarUrl" alt="头像" />
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="user">我的空间</el-dropdown-item>
              <el-dropdown-item command="history">历史记录</el-dropdown-item>
              <el-dropdown-item command="account">账号设置</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <button v-else class="header__login" @click="userStore.openLoginDialog()">登录</button>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { Bell } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SearchBox from '@/components/base/SearchBox.vue'
import { partitions } from '@/config/site'
import { useUserStore } from '@/stores/user'
import { fetchWhisperUnread } from '@/api/chat'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const unread = ref(0)

const avatarUrl = computed(() => userStore.avatar || 'https://static.hdslb.com/images/akari.jpg')

/** 未读私信数：登录后拉一次，切路由时刷新 */
async function loadUnread() {
  if (!userStore.isLogin || !userStore.uid) {
    unread.value = 0
    return
  }
  try {
    unread.value = (await fetchWhisperUnread({ uid: userStore.uid })) ?? 0
  } catch {
    unread.value = 0
  }
}

onMounted(loadUnread)
watch(() => route.path, loadUnread)

function notReady() {
  ElMessage.info('该分区暂未开放，敬请期待')
}

function goUpload() {
  if (!userStore.isLogin) {
    userStore.openLoginDialog()
    return
  }
  router.push('/upload')
}

async function onCommand(command: string) {
  if (command === 'logout') {
    await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' }).catch(() => 'cancel')
    if (userStore.isLogin) await userStore.signOut()
    router.push('/home')
    return
  }
  if (command === 'user') router.push(`/user/${userStore.uid}`)
  if (command === 'history') router.push('/history')
  if (command === 'account') router.push('/account')
}
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;
@use '@/styles/mixins' as m;

.header {
  position: sticky;
  top: 0;
  z-index: 50;
  height: t.$header-height;
  background: var(--bg-card);
  border-bottom: 1px solid var(--line);

  &__inner {
    width: t.$page-width;
    height: 100%;
    margin: 0 auto;
    display: flex;
    align-items: center;
    gap: t.$space-lg;
  }

  &__logo {
    display: flex;
    align-items: baseline;
    font-size: 22px;
    font-weight: 700;
    letter-spacing: -0.5px;
    color: var(--brand);
  }

  &__logo-text {
    color: var(--text-1);
  }

  &__nav {
    display: flex;
    align-items: center;
    gap: t.$space-md;
    font-size: t.$fs-sm;
    color: var(--text-2);
  }

  &__nav-item {
    cursor: pointer;
    padding: 4px 2px;
    transition: color 0.2s;

    &:hover,
    &.is-active {
      color: var(--brand);
    }
  }

  &__search {
    flex: 1;
    max-width: 420px;
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: t.$space-md;
    margin-left: auto;
  }

  &__upload {
    height: 32px;
    padding: 0 16px;
    border: none;
    border-radius: var(--radius);
    background: var(--brand);
    color: #fff;
    font-size: t.$fs-sm;

    &:hover {
      background: var(--brand-hover);
    }
  }

  &__login {
    height: 32px;
    padding: 0 18px;
    border: 1px solid var(--brand);
    border-radius: var(--radius);
    background: transparent;
    color: var(--brand);
    font-size: t.$fs-sm;

    &:hover {
      background: var(--brand);
      color: #fff;
    }
  }

  &__icon {
    position: relative;
    display: flex;
    color: var(--text-2);

    &:hover {
      color: var(--brand);
    }
  }

  &__badge {
    position: absolute;
    top: -6px;
    right: -8px;
    min-width: 16px;
    height: 16px;
    padding: 0 4px;
    border-radius: 8px;
    background: #f56c6c;
    color: #fff;
    font-size: 10px;
    line-height: 16px;
    text-align: center;
  }

  &__avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    overflow: hidden;
    cursor: pointer;
    border: 1px solid var(--line);

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }
}

.partition {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 4px 12px;

  &__item {
    padding: 6px 8px;
    border-radius: 4px;
    font-size: t.$fs-sm;
    color: var(--text-2);
    cursor: pointer;

    &:hover {
      background: var(--bg-hover);
      color: var(--brand);
    }
  }
}
</style>
