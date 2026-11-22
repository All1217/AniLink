<template>
  <div class="dm-list">
    <div class="dm-list__head">
      <span class="dm-list__time">时间</span>
      <span class="dm-list__text">弹幕内容</span>
      <span class="dm-list__date">发送时间</span>
    </div>

    <ul v-if="items.length > 0" class="dm-list__body">
      <li v-for="(item, index) in items" :key="item.id ?? index" class="dm-list__row">
        <span class="dm-list__time">{{ formatDuration(item.timePoint) }}</span>
        <span class="dm-list__text" :title="item.content">{{ item.content }}</span>
        <span class="dm-list__date">{{ shortDate(item.createDate) }}</span>
      </li>
    </ul>

    <p v-else class="dm-list__empty">{{ emptyText }}</p>
  </div>
</template>

<script setup lang="ts">
import type { Danmu } from '@/types'
import { formatDate, formatDuration } from '@/utils/format'
import { DateStringType } from '@/types'

withDefaults(defineProps<{ items: Danmu[]; emptyText?: string }>(), { emptyText: '暂无弹幕' })

/** 发送时间只保留 月-日 时:分，和旧版一致 */
function shortDate(value: string): string {
  if (!value) return ''
  const date = new Date(value.replace(/-/g, '/'))
  return Number.isNaN(date.getTime()) ? value : formatDate(date, DateStringType.MON_D_H_MIN)
}
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;
@use '@/styles/mixins' as m;

.dm-list {
  &__head,
  &__row {
    display: grid;
    grid-template-columns: 44px minmax(0, 1fr) 72px;
    gap: 8px;
    align-items: center;
  }

  &__head {
    padding: 4px 0;
    border-bottom: 1px solid var(--line-soft, #f1f2f3);
    font-size: 11px;
    color: var(--text-3);
  }

  &__body {
    max-height: 220px;
    overflow-y: auto;
  }

  &__row {
    padding: 5px 0;
    font-size: t.$fs-xs;
    color: var(--text-2);

    &:hover {
      background: var(--bg-hover);
    }
  }

  &__time {
    color: var(--text-3);
    font-variant-numeric: tabular-nums;
  }

  &__text {
    color: var(--text-1);

    @include m.ellipsis-1;
  }

  &__date {
    font-size: 11px;
    color: var(--text-3);
    text-align: right;
  }

  &__empty {
    padding: 12px 0;
    font-size: t.$fs-xs;
    color: var(--text-3);
    text-align: center;
  }
}
</style>
