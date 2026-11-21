<template>
  <section class="coming">
    <div class="coming__stage">{{ stage }}</div>
    <h2 class="coming__title">{{ title }}</h2>
    <p class="coming__desc">{{ desc }}</p>
    <el-button @click="router.push('/home')">回到首页</el-button>
  </section>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

defineProps<{ title: string; desc: string; stage: string }>()
const router = useRouter()
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;
@use '@/styles/mixins' as m;

.coming {
  @include m.flex-center;
  flex-direction: column;
  gap: 12px;
  width: t.$page-width;
  margin: 0 auto;
  padding: 120px 0;
  text-align: center;

  &__stage {
    padding: 2px 10px;
    border-radius: 10px;
    background: var(--brand-soft, #fff1f5);
    color: var(--brand);
    font-size: t.$fs-xs;
  }

  &__title {
    font-size: t.$fs-xl;
    color: var(--text-1);
  }

  &__desc {
    max-width: 520px;
    font-size: t.$fs;
    color: var(--text-3);
    line-height: 22px;
  }
}
</style>
