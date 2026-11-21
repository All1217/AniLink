<template>
  <div class="upload-view">
    <aside class="nav">
      <h1 class="nav__title">创作中心</h1>
      <button
        v-for="item in navs"
        :key="item.key"
        :class="['nav__item', { 'is-active': active === item.key }]"
        @click="active = item.key"
      >
        <el-icon><component :is="item.icon" /></el-icon>
        <span>{{ item.label }}</span>
      </button>
    </aside>

    <main class="content">
      <UploadPanel v-if="active === 'upload'" />
      <ArticleEmpty v-else-if="active === 'article'" />
      <DataCenterPanel v-else />
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { DataAnalysis, Document, UploadFilled } from '@element-plus/icons-vue'
import UploadPanel from '@/components/upload/UploadPanel.vue'
import ArticleEmpty from '@/components/upload/ArticleEmpty.vue'
import DataCenterPanel from '@/components/upload/DataCenterPanel.vue'

type TabKey = 'upload' | 'article' | 'data'

const navs = [
  { key: 'upload' as TabKey, label: '上传视频', icon: UploadFilled },
  { key: 'article' as TabKey, label: '稿件管理', icon: Document },
  { key: 'data' as TabKey, label: '数据中心', icon: DataAnalysis },
]

const active = ref<TabKey>('upload')
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.upload-view {
  display: flex;
  gap: 20px;
  width: t.$page-width;
  margin: 0 auto;
  padding-top: 16px;
}

.nav {
  flex: 0 0 200px;
  align-self: flex-start;
  padding: 16px 12px;
  background: #fff;
  border-radius: var(--radius-lg);

  &__title {
    margin: 0 4px 12px;
    font-size: t.$fs-md;
    color: var(--text-1);
  }

  &__item {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;
    padding: 9px 12px;
    margin-bottom: 4px;
    border: none;
    border-radius: var(--radius);
    background: transparent;
    font-size: t.$fs-sm;
    color: var(--text-2);
    text-align: left;

    &:hover {
      background: var(--bg-hover);
    }

    &.is-active {
      background: var(--brand-soft, #fff1f5);
      color: var(--brand);
    }
  }
}

.content {
  flex: 1;
  min-width: 0;
}
</style>
