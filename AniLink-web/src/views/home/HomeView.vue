<template>
  <div class="home">
    <!-- 轮播 -->
    <section class="banner">
      <div class="banner__viewport">
        <div class="banner__track" :style="{ transform: `translateX(${-index * 100}%)` }">
          <div
            v-for="slide in slides"
            :key="slide.id"
            class="banner__slide"
            :style="{ background: slide.background }"
            @click="openSlide(slide.href)"
          >
            <span class="banner__title">{{ slide.title }}</span>
          </div>
        </div>
      </div>
      <button class="banner__arrow banner__arrow--prev" @click="move(-1)">
        <el-icon><ArrowLeft /></el-icon>
      </button>
      <button class="banner__arrow banner__arrow--next" @click="move(1)">
        <el-icon><ArrowRight /></el-icon>
      </button>
      <div class="banner__dots">
        <i
          v-for="(slide, i) in slides"
          :key="slide.id"
          :class="['banner__dot', { 'is-active': i === index }]"
          @click="index = i"
        />
      </div>
    </section>

    <!-- 分区入口 -->
    <nav class="channels">
      <span v-for="item in partitions" :key="item.id" class="channels__item" @click="notReady">
        {{ item.name }}
      </span>
    </nav>

    <!-- 推荐流 -->
    <section class="grid">
      <VideoCard v-for="video in videos" :key="video.vid" :video="video" />
    </section>

    <div v-if="loading" class="grid grid--skeleton">
      <div v-for="i in 10" :key="i" class="skeleton" />
    </div>

    <EmptyState v-if="!loading && videos.length === 0" text="还没有视频，去投稿第一个吧" />
    <p v-if="exhausted && videos.length > 0" class="end">已经到底啦 ~</p>

    <div ref="sentinel" class="sentinel" />
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import VideoCard from '@/components/base/VideoCard.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import { PAGE_SIZE, partitions, slides } from '@/config/site'
import { fetchVideoPage } from '@/api/video'
import type { VideoVo } from '@/types'

const videos = ref<VideoVo[]>([])
const loading = ref(false)
const exhausted = ref(false)
const current = ref(1)
const sentinel = ref<HTMLElement | null>(null)
const index = ref(0)
let observer: IntersectionObserver | null = null
let timer: number | undefined

/** 分页拉取首页推荐流，到底后停止 */
async function loadMore() {
  if (loading.value || exhausted.value) return
  loading.value = true
  try {
    const page = await fetchVideoPage({ current: current.value, size: PAGE_SIZE })
    const records = page.records ?? []
    videos.value.push(...records)
    current.value += 1
    if (records.length === 0 || videos.value.length >= (page.total ?? 0)) exhausted.value = true
  } catch {
    exhausted.value = true
  } finally {
    loading.value = false
  }
}

function move(step: number) {
  index.value = (index.value + step + slides.length) % slides.length
}

function openSlide(href?: string) {
  if (href) window.open(href, '_blank', 'noreferrer')
}

function notReady() {
  ElMessage.info('该分区暂未开放，敬请期待')
}

onMounted(() => {
  void loadMore()
  observer = new IntersectionObserver(
    (entries) => {
      if (entries[0]?.isIntersecting) void loadMore()
    },
    { rootMargin: '200px' },
  )
  if (sentinel.value) observer.observe(sentinel.value)
  timer = window.setInterval(() => move(1), 5000)
})

onBeforeUnmount(() => {
  observer?.disconnect()
  if (timer) window.clearInterval(timer)
})
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;
@use '@/styles/mixins' as m;

.home {
  width: t.$page-width;
  margin: 0 auto;
  padding-top: t.$space-md;
}

.banner {
  position: relative;
  height: 220px;
  border-radius: var(--radius-xl);
  overflow: hidden;

  &__viewport {
    height: 100%;
    overflow: hidden;
  }

  &__track {
    display: flex;
    height: 100%;
    transition: transform 0.35s ease;
  }

  &__slide {
    flex: 0 0 100%;
    height: 100%;
    display: flex;
    align-items: flex-end;
    padding: 24px 32px;
    cursor: pointer;
  }

  &__title {
    color: #fff;
    font-size: 26px;
    font-weight: 600;
    text-shadow: 0 2px 8px rgba(0, 0, 0, 0.25);
  }

  &__arrow {
    position: absolute;
    top: 50%;
    transform: translateY(-50%);
    width: 32px;
    height: 48px;
    border: none;
    border-radius: 6px;
    background: rgba(255, 255, 255, 0.7);
    color: var(--text-1);
    opacity: 0;
    transition: opacity 0.2s;

    &--prev {
      left: 12px;
    }

    &--next {
      right: 12px;
    }
  }

  &:hover &__arrow {
    opacity: 1;
  }

  &__dots {
    position: absolute;
    right: 20px;
    bottom: 16px;
    display: flex;
    gap: 6px;
  }

  &__dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.5);
    cursor: pointer;

    &.is-active {
      background: #fff;
      width: 16px;
      border-radius: 4px;
    }
  }
}

.channels {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 16px 0;

  &__item {
    padding: 5px 14px;
    border-radius: 14px;
    background: #fff;
    font-size: t.$fs-sm;
    color: var(--text-2);
    cursor: pointer;

    &:hover {
      background: var(--brand-soft, #fff1f5);
      color: var(--brand);
    }
  }
}

.grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 20px 16px;
}

.end {
  padding: 24px 0;
  text-align: center;
  font-size: t.$fs-xs;
  color: var(--text-3);
}

.sentinel {
  height: 1px;
}

.skeleton {
  height: 180px;
  border-radius: var(--radius-lg);
  background: linear-gradient(90deg, #e9eaec 25%, #f4f5f6 37%, #e9eaec 63%);
  background-size: 400% 100%;
  animation: shimmer 1.2s infinite;
}

@keyframes shimmer {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}

@media (max-width: 1240px) {
  .grid {
    grid-template-columns: repeat(4, 1fr);
  }
}
</style>
