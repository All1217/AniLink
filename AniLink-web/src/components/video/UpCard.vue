<template>
  <section class="up">
    <RouterLink :to="`/user/${author.uid}`" class="up__avatar">
      <el-avatar :size="48" :src="author.avatar || undefined">{{ author.nickname?.[0] ?? 'U' }}</el-avatar>
    </RouterLink>

    <div class="up__info">
      <RouterLink :to="`/user/${author.uid}`" class="up__name">{{ author.nickname }}</RouterLink>
      <span class="up__fans">{{ formatCount(fansCount) }} 粉丝</span>
    </div>

    <button class="up__follow" :class="{ 'is-following': isFollow }" :disabled="busy" @click="emit('toggle')">
      {{ isFollow ? '已关注' : '+ 关注' }}
    </button>

    <p v-if="author.description" class="up__desc">{{ author.description }}</p>
  </section>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
import type { User } from '@/types'
import { formatCount } from '@/utils/format'

defineProps<{ author: User; fansCount: number; isFollow: boolean; busy: boolean }>()
const emit = defineEmits<{ (e: 'toggle'): void }>()
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.up {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  padding: 16px;
  background: #fff;
  border-radius: var(--radius-lg);

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__name {
    display: block;
    font-size: t.$fs-sm;
    font-weight: 600;
    color: var(--text-1);

    &:hover {
      color: var(--brand);
    }
  }

  &__fans {
    font-size: t.$fs-xs;
    color: var(--text-3);
  }

  &__follow {
    height: 30px;
    padding: 0 14px;
    border: 1px solid var(--brand);
    border-radius: var(--radius);
    background: var(--brand);
    color: #fff;
    font-size: t.$fs-xs;

    &.is-following {
      background: transparent;
      color: var(--text-2);
      border-color: var(--line);
    }
  }

  &__desc {
    flex: 0 0 100%;
    font-size: t.$fs-xs;
    line-height: 18px;
    color: var(--text-3);
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}
</style>
