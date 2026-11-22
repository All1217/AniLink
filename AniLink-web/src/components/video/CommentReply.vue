<template>
  <div class="reply">
    <el-avatar :size="24" :src="reply.avatar || undefined">{{ reply.nickname?.[0] ?? 'U' }}</el-avatar>
    <div class="reply__body">
      <span class="reply__name">{{ reply.nickname }}</span>
      <span class="reply__content">{{ reply.content }}</span>
      <span class="reply__time">{{ fromNow(reply.createTime) }}</span>
      <span v-if="canDelete" class="reply__delete" title="删除这条回复" @click="emit('delete', reply)">删除</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { CommentVo } from '@/types'
import { fromNow } from '@/utils/format'

defineProps<{ reply: CommentVo; canDelete: boolean }>()
const emit = defineEmits<{ (e: 'delete', reply: CommentVo): void }>()
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.reply {
  display: flex;
  gap: 8px;
  padding: 4px 0;
  border-radius: 4px;

  /* 悬浮整行时让「删除」更显眼一点，避免只能靠文字本身辨认 */
  &:hover .reply__delete {
    opacity: 1;
  }

  &__body {
    flex: 1;
    min-width: 0;
    font-size: t.$fs-xs;
    line-height: 20px;
  }

  &__name {
    margin-right: 6px;
    color: var(--text-2);
  }

  &__content {
    color: var(--text-1);
  }

  &__time {
    margin: 0 8px;
    color: var(--text-3);
  }

  &__delete {
    display: inline-block;
    padding: 0 6px;
    border-radius: 9px;
    color: var(--text-3);
    cursor: pointer;
    opacity: 0.75;
    transition: color 0.15s, background-color 0.15s, opacity 0.15s;
    user-select: none;

    &:hover {
      background: var(--brand-soft, #fff1f5);
      color: var(--brand);
      opacity: 1;
    }
  }
}
</style>
