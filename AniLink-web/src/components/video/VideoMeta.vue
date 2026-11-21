<template>
  <section class="meta">
    <h1 class="meta__title">{{ video.title }}</h1>

    <div class="meta__row">
      <span class="meta__stat"><el-icon><VideoPlay /></el-icon>{{ formatCount(stats?.play) }}</span>
      <span class="meta__stat"><el-icon><ChatDotSquare /></el-icon>{{ formatCount(stats?.danmu) }}</span>
      <span class="meta__date">{{ video.uploadDate }}</span>
      <VideoActions class="meta__actions" :video="video" />
    </div>

    <div class="meta__desc">
      <p :class="['meta__text', { 'is-open': open }]">{{ video.descr || '这个视频还没有简介' }}</p>
      <button v-if="(video.descr?.length ?? 0) > 120" class="meta__toggle" @click="open = !open">
        {{ open ? '收起' : '展开更多' }}
      </button>
      <div v-if="tags.length > 0" class="meta__tags">
        <span v-for="tag in tags" :key="tag" class="meta__tag">{{ tag }}</span>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ChatDotSquare, VideoPlay } from '@element-plus/icons-vue'
import VideoActions from './VideoActions.vue'
import type { Video, VideoStats } from '@/types'
import { formatCount } from '@/utils/format'

const props = defineProps<{ video: Video; stats: VideoStats | null }>()
const open = ref(false)

const tags = computed(() =>
  (props.video.tags ?? '')
    .split(/[\s,，]+/)
    .filter(Boolean)
    .slice(0, 10),
)
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.meta {
  &__title {
    margin: 16px 0 8px;
    font-size: 20px;
    line-height: 28px;
    color: var(--text-1);
  }

  &__row {
    display: flex;
    align-items: center;
    gap: 16px;
    font-size: t.$fs-xs;
    color: var(--text-3);
  }

  &__stat {
    display: flex;
    align-items: center;
    gap: 2px;
  }

  &__actions {
    margin-left: auto;
  }

  &__desc {
    margin-top: 12px;
    padding: 12px 16px;
    background: #fff;
    border-radius: var(--radius-lg);
  }

  &__text {
    font-size: t.$fs-sm;
    line-height: 22px;
    color: var(--text-2);
    white-space: pre-wrap;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;

    &.is-open {
      display: block;
    }
  }

  &__toggle {
    margin-top: 4px;
    border: none;
    background: transparent;
    color: var(--link);
    font-size: t.$fs-xs;
  }

  &__tags {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-top: 10px;
  }

  &__tag {
    padding: 2px 8px;
    border-radius: 10px;
    background: var(--bg-hover);
    color: var(--text-2);
    font-size: t.$fs-xs;
  }
}
</style>
