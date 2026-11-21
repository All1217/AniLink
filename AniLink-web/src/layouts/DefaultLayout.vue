<template>
  <div class="layout">
    <AppHeader />
    <main class="layout__main">
      <slot />
    </main>
    <footer class="layout__footer">
      <div class="layout__footer-inner">
        <span>AniLink · 仿 B 站视频社区（个人学习项目）</span>
        <span class="layout__footer-links">
          <a href="https://github.com/All1217/AniLink-Java" target="_blank" rel="noreferrer">后端仓库</a>
          <a href="https://element-plus.org/zh-CN/component/overview" target="_blank" rel="noreferrer">Element Plus</a>
        </span>
      </div>
    </footer>
    <LoginDialog />
  </div>
</template>

<script setup lang="ts">
import AppHeader from '@/components/base/AppHeader.vue'
import LoginDialog from '@/components/base/LoginDialog.vue'
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--bg-page);

  &__main {
    flex: 1;
    padding-bottom: 40px;
  }

  &__footer {
    border-top: 1px solid var(--line);
    background: var(--bg-card);
    color: var(--text-3);
    font-size: t.$fs-xs;
  }

  &__footer-inner {
    width: t.$page-width;
    margin: 0 auto;
    padding: 16px 0;
    display: flex;
    justify-content: space-between;
  }

  &__footer-links {
    display: flex;
    gap: 16px;

    a:hover {
      color: var(--link);
    }
  }
}
</style>
