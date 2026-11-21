<template>
  <article class="video-card" @click="goDetail">
    <div class="video-card__cover">
      <img :src="cover" :alt="video.title" loading="lazy" @error="onImgError" />
      <span class="video-card__duration">{{ formatDuration(video.duration) }}</span>
      <span class="video-card__play">
        <el-icon><VideoPlay /></el-icon>
        {{ formatCount(video.play) }}
      </span>
    </div>
    <h3 class="video-card__title" :title="video.title">{{ video.title }}</h3>
    <div class="video-card__meta">
      <RouterLink
        v-if="video.uid"
        class="video-card__up"
        :to="`/user/${video.uid}`"
        @click.stop
      >
        {{ video.nickname || '未知 UP 主' }}
      </RouterLink>
      <span class="video-card__count">
        <el-icon><ChatDotSquare /></el-icon>
        {{ formatCount(video.danmu) }}
      </span>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ChatDotSquare, VideoPlay } from '@element-plus/icons-vue'
import type { VideoVo } from '@/types'
import { formatCount, formatDuration } from '@/utils/format'

const props = defineProps<{ video: VideoVo }>()
const router = useRouter()
const broken = ref(false)

/** 封面兜底：后端返回空/坏图时显示渐变占位，避免整行裂图 */
const cover = computed(() => (broken.value || !props.video.coverUrl ? PLACEHOLDER : props.video.coverUrl))

const PLACEHOLDER =
  'data:image/svg+xml;charset=utf-8,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="320" height="200"><defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#e3e5e7"/><stop offset="1" stop-color="#f1f2f3"/></linearGradient></defs><rect width="320" height="200" fill="url(#g)"/><text x="160" y="105" font-size="14" fill="#9499a0" text-anchor="middle">暂无封面</text></svg>',
  )

function onImgError() {
  broken.value = true
}

function goDetail() {
  if (!props.video.vid) return
  router.push(`/video/${props.video.vid}`)
}
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;
@use '@/styles/mixins' as m;

.video-card {
  cursor: pointer;

  &__cover {
    position: relative;
    aspect-ratio: 16 / 10;
    border-radius: var(--radius-lg);
    overflow: hidden;
    background: var(--line-soft, #f1f2f3);

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.2s ease;
    }

    &:hover img {
      transform: scale(1.02);
    }
  }

  &__duration {
    position: absolute;
    right: 6px;
    bottom: 6px;
    padding: 0 4px;
    border-radius: 2px;
    background: rgba(0, 0, 0, 0.5);
    color: #fff;
    font-size: t.$fs-xs;
    line-height: 16px;
  }

  &__play {
    position: absolute;
    left: 6px;
    bottom: 6px;
    display: flex;
    align-items: center;
    gap: 2px;
    color: #fff;
    font-size: t.$fs-xs;
    text-shadow: 0 1px 2px rgba(0, 0, 0, 0.6);
    opacity: 0;
    transition: opacity 0.2s;
  }

  &__cover:hover &__play {
    opacity: 1;
  }

  &__title {
    margin-top: 8px;
    font-size: t.$fs;
    font-weight: 500;
    line-height: 20px;
    color: var(--text-1);

    @include m.ellipsis-2;
  }

  &:hover &__title {
    color: var(--link);
  }

  &__meta {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 4px;
    font-size: t.$fs-xs;
    color: var(--text-3);
  }

  &__up {
    @include m.ellipsis-1;

    &:hover {
      color: var(--brand);
    }
  }

  &__count {
    display: flex;
    align-items: center;
    gap: 2px;
    flex-shrink: 0;
  }
}
</style>
