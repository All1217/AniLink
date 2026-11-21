<template>
  <div class="search-view">
    <header class="search-view__head">
      <h1 class="search-view__title">
        搜索结果
        <em v-if="keyword">“{{ keyword }}”</em>
      </h1>
      <span class="search-view__count">共 {{ total }} 个视频</span>
    </header>

    <section v-if="videos.length > 0" class="grid">
      <VideoCard v-for="video in videos" :key="video.vid" :video="video" />
    </section>

    <div v-if="loading" class="grid grid--skeleton">
      <div v-for="i in 10" :key="i" class="skeleton" />
    </div>

    <EmptyState v-if="!loading && videos.length === 0" text="没有找到相关视频，换个关键词试试" />
    <div ref="sentinel" class="sentinel" />
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import VideoCard from '@/components/base/VideoCard.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import { PAGE_SIZE } from '@/config/site'
import { fetchVideoPage } from '@/api/video'
import type { VideoVo } from '@/types'

const route = useRoute()
const keyword = ref('')
const videos = ref<VideoVo[]>([])
const total = ref(0)
const loading = ref(false)
const exhausted = ref(false)
const current = ref(1)
const sentinel = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null

/** 按关键词分页搜索；关键词变化时从第一页重来 */
async function loadMore(reset = false) {
  if (reset) {
    videos.value = []
    current.value = 1
    exhausted.value = false
    total.value = 0
  }
  if (loading.value || exhausted.value) return
  loading.value = true
  try {
    const page = await fetchVideoPage({ title: keyword.value || undefined, current: current.value, size: PAGE_SIZE })
    const records = page.records ?? []
    videos.value.push(...records)
    total.value = page.total ?? 0
    current.value += 1
    if (records.length === 0 || videos.value.length >= total.value) exhausted.value = true
  } catch {
    exhausted.value = true
  } finally {
    loading.value = false
  }
}

watch(
  () => route.query.keyword,
  (value) => {
    keyword.value = typeof value === 'string' ? value : ''
    void loadMore(true)
  },
  { immediate: true },
)

onMounted(() => {
  observer = new IntersectionObserver(
    (entries) => {
      if (entries[0]?.isIntersecting) void loadMore()
    },
    { rootMargin: '200px' },
  )
  if (sentinel.value) observer.observe(sentinel.value)
})

onBeforeUnmount(() => observer?.disconnect())
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.search-view {
  width: t.$page-width;
  margin: 0 auto;
  padding-top: t.$space-lg;

  &__head {
    display: flex;
    align-items: baseline;
    gap: 12px;
    margin-bottom: 16px;
  }

  &__title {
    font-size: t.$fs-lg;
    color: var(--text-1);

    em {
      color: var(--brand);
      font-weight: 600;
    }
  }

  &__count {
    font-size: t.$fs-xs;
    color: var(--text-3);
  }
}

.grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 20px 16px;
}

.skeleton {
  height: 180px;
  border-radius: var(--radius-lg);
  background: #e9eaec;
  animation: pulse 1.2s infinite ease-in-out;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.55;
  }
}

.sentinel {
  height: 1px;
}
</style>
