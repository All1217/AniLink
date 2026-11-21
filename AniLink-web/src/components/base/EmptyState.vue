<template>
  <div class="empty">
    <el-icon class="empty__icon" :size="40"><Box /></el-icon>
    <p class="empty__text">{{ text }}</p>
    <slot />
  </div>
</template>

<script setup lang="ts">
import { Box } from '@element-plus/icons-vue'

withDefaults(defineProps<{ text?: string }>(), { text: '这里什么都没有' })
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;
@use '@/styles/mixins' as m;

.empty {
  @include m.flex-center;
  flex-direction: column;
  gap: 8px;
  padding: 64px 0;
  color: var(--text-3);

  &__text {
    font-size: t.$fs;
  }
}
</style>
