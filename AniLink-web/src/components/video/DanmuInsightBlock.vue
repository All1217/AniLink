<template>
  <div class="ib">
    <span class="ib__label">视频标签热度（点标签筛弹幕）</span>
    <TagRankBars :tags="tags" :active-tag="activeTag" @pick="emit('pick-tag', $event)" />
    <SimilarUsers :users="users" :common-tags="commonTags" @filter="emit('pick-user', $event)" />
    <p v-if="showEmpty" class="ib__empty">暂无相近用户</p>
  </div>
</template>

<script setup lang="ts">
import TagRankBars from './TagRankBars.vue'
import SimilarUsers from './SimilarUsers.vue'
import type { TagRank } from '@/api/danmu'
import type { User } from '@/types'

defineProps<{ tags: TagRank[]; activeTag: string; users: User[]; commonTags: string[]; showEmpty: boolean }>()
const emit = defineEmits<{ (e: 'pick-tag', tagName: string): void; (e: 'pick-user', user: User): void }>()
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.ib {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid var(--line-soft, #f1f2f3);

  &__label {
    display: block;
    margin-bottom: 6px;
    font-size: 11px;
    color: var(--text-3);
  }

  &__empty {
    margin-top: 6px;
    font-size: t.$fs-xs;
    color: var(--text-3);
  }
}
</style>
