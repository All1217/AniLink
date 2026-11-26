<template>
  <aside class="related">
    <h2 class="related__title">相关推荐</h2>
    <RouterLink v-for="item in list" :key="item.vid" class="related__item" :to="`/video/${item.vid}`">
      <div class="related__cover">
        <img :src="coverThumb(item)" :alt="item.title" loading="lazy" decoding="async" @error="onImgError" />
        <span class="related__duration">{{ formatDuration(item.duration) }}</span>
      </div>
      <div class="related__info">
        <p class="related__name" :title="item.title">{{ item.title }}</p>
        <span class="related__up">{{ item.nickname }}</span>
        <span class="related__play">{{ formatCount(item.play) }} 播放</span>
      </div>
    </RouterLink>
    <EmptyState v-if="!loading && list.length === 0" text="暂无推荐" />
  </aside>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import EmptyState from '@/components/base/EmptyState.vue'
import { useUserStore } from '@/stores/user'
import { fetchRecommendVideos } from '@/api/video'
import { fetchVideoPage } from '@/api/video'
import type { VideoVo } from '@/types'
import { formatCount, formatDuration } from '@/utils/format'
import { COVER_PLACEHOLDER, coverThumb } from '@/utils/media'

const props = defineProps<{ vid: number }>()
const userStore = useUserStore()
const list = ref<VideoVo[]>([])
const loading = ref(false)

/** 坏图直接换成占位图，避免右侧列表裂图 */
function onImgError(event: Event) {
  const img = event.target as HTMLImageElement
  img.removeAttribute('srcset')
  img.src = COVER_PLACEHOLDER
}

/** 登录用户走实时推荐（按画像），未登录退回分页列表，保证右侧不空 */
async function load() {
  loading.value = true
  try {
    if (userStore.isLogin) {
      const recommend = await fetchRecommendVideos({ vid: props.vid, uid: userStore.uid, count: 10 }).catch(() => [])
      if (recommend && recommend.length > 0) {
        list.value = recommend
        return
      }
    }
    const page = await fetchVideoPage({ current: 1, size: 10 })
    list.value = (page.records ?? []).filter((item) => item.vid !== props.vid)
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

watch(() => props.vid, load)
onMounted(load)
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.related {
  padding: 16px;
  background: #fff;
  border-radius: var(--radius-lg);

  &__title {
    margin-bottom: 12px;
    font-size: t.$fs-md;
    color: var(--text-1);
  }

  &__item {
    display: flex;
    gap: 10px;
    padding: 8px 0;

    & + & {
      border-top: 1px solid var(--line-soft, #f1f2f3);
    }

    &:hover .related__name {
      color: var(--link);
    }
  }

  &__cover {
    position: relative;
    flex: 0 0 112px;
    height: 70px;
    border-radius: var(--radius);
    overflow: hidden;
    background: var(--line-soft, #f1f2f3);

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
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

  &__info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  &__name {
    font-size: t.$fs-xs;
    line-height: 18px;
    color: var(--text-1);
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  &__up,
  &__play {
    font-size: 11px;
    color: var(--text-3);
  }
}
</style>
