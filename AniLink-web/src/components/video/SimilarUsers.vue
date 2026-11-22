<template>
  <div v-if="users.length > 0" class="users">
    <div v-if="commonTags.length > 0" class="users__tags">
      <span class="users__label">你与 TA 的共同标签</span>
      <span v-for="tag in commonTags" :key="tag" class="users__tag">{{ tag }}</span>
    </div>

    <ul class="users__list">
      <li v-for="user in users" :key="user.uid" class="row">
        <RouterLink :to="`/user/${user.uid}`" target="_blank">
          <el-avatar :size="26" :src="user.avatar || undefined">{{ user.nickname?.[0] ?? 'U' }}</el-avatar>
        </RouterLink>
        <span class="row__name" :title="user.nickname">{{ user.nickname }}</span>
        <button class="row__filter" @click="emit('filter', user)">筛选</button>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
import type { User } from '@/types'

defineProps<{ users: User[]; commonTags: string[] }>()
const emit = defineEmits<{ (e: 'filter', user: User): void }>()
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;
@use '@/styles/mixins' as m;

.users {
  &__tags {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 6px;
    margin-bottom: 6px;
  }

  &__label {
    font-size: t.$fs-xs;
    color: var(--text-3);
  }

  &__tag {
    padding: 1px 8px;
    border-radius: 9px;
    background: var(--brand-soft, #fff1f5);
    color: var(--brand);
    font-size: 11px;
  }
}

.row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;

  &__name {
    flex: 1;
    min-width: 0;
    font-size: t.$fs-xs;
    color: var(--text-2);

    @include m.ellipsis-1;
  }

  &__filter {
    flex-shrink: 0;
    height: 22px;
    padding: 0 8px;
    border: 1px solid var(--line);
    border-radius: 11px;
    background: #fff;
    color: var(--text-2);
    font-size: 11px;

    &:hover {
      border-color: var(--brand);
      color: var(--brand);
    }
  }
}
</style>
