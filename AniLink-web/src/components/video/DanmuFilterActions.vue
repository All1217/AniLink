<template>
  <div class="fa">
    <template v-if="isLogin">
      <button v-if="mode === 2" class="fa__btn" @click="emit('code', 0)">优先查看「高质量弹幕贡献者」的弹幕</button>
      <button v-if="mode === 2" class="fa__btn" @click="emit('code', 1)">优先查看「干货贡献者」的弹幕</button>
      <button v-if="mode !== 2" class="fa__btn" @click="emit('reset')">取消筛选（显示全部）</button>
      <button class="fa__btn" :disabled="loadingUsers" @click="emit('load-users')">
        {{ hasUsers ? '刷新相近用户' : '查看与自己最相似的用户的弹幕' }}
      </button>
    </template>
    <button v-else class="fa__login" @click="emit('login')">登录后可按标签 / 用户筛选弹幕</button>
  </div>
</template>

<script setup lang="ts">
defineProps<{ isLogin: boolean; mode: number; hasUsers: boolean; loadingUsers: boolean }>()
const emit = defineEmits<{
  (e: 'code', code: 0 | 1): void
  (e: 'reset'): void
  (e: 'load-users'): void
  (e: 'login'): void
}>()
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.fa {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 8px;

  &__btn {
    padding: 5px 8px;
    border: 1px solid var(--line);
    border-radius: var(--radius);
    background: #fafbfc;
    color: var(--text-2);
    font-size: t.$fs-xs;
    text-align: left;

    &:hover:not(:disabled) {
      border-color: var(--brand);
      color: var(--brand);
    }

    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
  }

  &__login {
    padding: 6px 8px;
    border: 1px dashed var(--line);
    border-radius: var(--radius);
    background: transparent;
    color: var(--text-3);
    font-size: t.$fs-xs;
  }
}
</style>
