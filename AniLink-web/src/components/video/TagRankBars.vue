<template>
  <div v-if="tags.length > 0" class="bars">
    <button
      v-for="tag in tags"
      :key="tag.name"
      :class="['bars__row', { 'is-active': activeTag === tag.name }]"
      @click="emit('pick', tag.name)"
    >
      <span class="bars__name">{{ tag.name }}</span>
      <span class="bars__track"><i :style="{ width: `${percent(tag.value)}%` }" /></span>
      <span class="bars__value">{{ tag.value }}</span>
    </button>
  </div>
  <p v-else class="bars__empty">暂无标签数据</p>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { TagRank } from '@/api/danmu'

const props = defineProps<{ tags: TagRank[]; activeTag: string }>()
const emit = defineEmits<{ (e: 'pick', tagName: string): void }>()

const maxValue = computed(() => Math.max(...props.tags.map((item) => item.value), 1))

function percent(value: number): number {
  return Math.round((value / maxValue.value) * 100)
}
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.bars {
  display: flex;
  flex-direction: column;
  gap: 6px;

  &__row {
    display: grid;
    grid-template-columns: 88px 1fr 40px;
    align-items: center;
    gap: 8px;
    padding: 0;
    border: none;
    background: transparent;
    text-align: left;
    font-size: t.$fs-xs;
    color: var(--text-2);

    &.is-active {
      color: var(--brand);
    }
  }

  &__name {
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  &__track {
    height: 8px;
    border-radius: 4px;
    background: var(--line-soft, #f1f2f3);
    overflow: hidden;

    i {
      display: block;
      height: 100%;
      border-radius: 4px;
      background: linear-gradient(90deg, #fb7299, #fc8bab);
    }
  }

  &__value {
    text-align: right;
  }

  &__empty {
    font-size: t.$fs-xs;
    color: var(--text-3);
  }
}
</style>
