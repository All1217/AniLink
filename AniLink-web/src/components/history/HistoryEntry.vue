<template>
  <article class="entry">
    <RouterLink :to="`/video/${item.vid}`" class="entry__cover">
      <img :src="coverThumb(item)" :alt="item.title" loading="lazy" decoding="async" @error="onImgError" />
      <span class="entry__duration">
        {{ formatDuration(item.duration) }}/{{ formatDuration(item.videoDuration) }}
      </span>
      <i class="entry__progress" :style="{ width: progress }" />
    </RouterLink>

    <div class="entry__info">
      <RouterLink :to="`/video/${item.vid}`" class="entry__title">{{ item.title }}</RouterLink>
      <RouterLink :to="`/user/${item.authorUid}`" class="entry__up">{{ item.nickname }}</RouterLink>
      <span class="entry__time">观看于 {{ item.viewTime }}</span>
    </div>

    <button class="entry__delete" title="删除该记录" @click="emit('delete', item.vid)">删除</button>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import type { HistoryVo } from '@/types'
import { formatDuration } from '@/utils/format'
import { COVER_PLACEHOLDER, coverThumb } from '@/utils/media'

const props = defineProps<{ item: HistoryVo }>()
const emit = defineEmits<{ (e: 'delete', vid: number): void }>()

/** 坏图直接换成占位图，避免整行裂图 */
function onImgError(event: Event) {
  const img = event.target as HTMLImageElement
  img.removeAttribute('srcset')
  img.src = COVER_PLACEHOLDER
}

/** 观看进度条宽度（百分比，封顶 100%） */
const progress = computed(() => {
  if (!props.item.videoDuration) return '0%'
  return `${Math.min(100, Math.round((props.item.duration / props.item.videoDuration) * 100))}%`
})
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;
@use '@/styles/mixins' as m;

.entry {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: #fff;
  border-radius: var(--radius);

  & + & {
    margin-top: 8px;
  }

  &__cover {
    position: relative;
    flex: 0 0 160px;
    height: 90px;
    border-radius: var(--radius);
    overflow: hidden;
    background: var(--line-soft, #f1f2f3);

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      opacity: 0.92;
    }
  }

  &__duration {
    position: absolute;
    right: 4px;
    bottom: 4px;
    padding: 0 3px;
    border-radius: 2px;
    background: rgba(0, 0, 0, 0.5);
    color: #fff;
    font-size: 10px;
  }

  &__progress {
    position: absolute;
    left: 0;
    bottom: 0;
    height: 2px;
    background: var(--brand);
  }

  &__info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  &__title {
    font-size: t.$fs;
    color: var(--text-1);

    @include m.ellipsis-1;

    &:hover {
      color: var(--link);
    }
  }

  &__up,
  &__time {
    font-size: t.$fs-xs;
    color: var(--text-3);
  }

  &__delete {
    align-self: flex-start;
    border: none;
    background: transparent;
    color: var(--text-3);
    font-size: t.$fs-xs;

    &:hover {
      color: var(--danger, #f56c6c);
    }
  }
}
</style>
