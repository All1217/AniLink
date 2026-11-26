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

    <!-- 分区筛选 + 换一批 -->
    <nav class="channels">
      <div class="channels__list">
        <span
          v-for="item in partitionOptions"
          :key="item.id"
          :class="['channels__item', { 'is-active': activeMcId === item.id }]"
          @click="selectPartition(item.id)"
        >
          {{ item.name }}
        </span>
      </div>
      <button class="channels__refresh" :disabled="loading" @click="nextBatch">
        <el-icon><Refresh /></el-icon>
        换一批
      </button>
    </nav>

    <!-- 推荐流：每批 20 条，换一批整体替换；首行 5 张按高优先级加载 -->
    <section v-if="videos.length > 0 && !loading" class="grid">
      <VideoCard
        v-for="(video, i) in videos"
        :key="video.vid"
        :video="video"
        :priority="i < 5"
      />
    </section>

    <div v-if="loading" class="grid grid--skeleton">
      <div v-for="i in 10" :key="i" class="skeleton" />
    </div>

    <EmptyState v-if="!loading && videos.length === 0" :text="emptyText" />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ArrowLeft, ArrowRight, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import VideoCard from '@/components/base/VideoCard.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import { ALL_PARTITION, PAGE_SIZE, partitions, slides } from '@/config/site'
import { fetchVideoPage } from '@/api/video'
import type { VideoVo } from '@/types'

const videos = ref<VideoVo[]>([])
const loading = ref(false)
const batch = ref(1)
const activeMcId = ref(ALL_PARTITION.id)
const total = ref(0)
const index = ref(0)
let timer: number | undefined
/** 请求序号：分区切换比接口快时，丢弃过期响应 */
let requestSeq = 0

/** 分区标签：首位固定「全部」 */
const partitionOptions = computed(() => [ALL_PARTITION, ...partitions])

const emptyText = computed(() =>
  activeMcId.value === ALL_PARTITION.id ? '还没有视频，去投稿第一个吧' : '该分区还没有视频，换一个看看吧',
)

/** 加载当前分区第 batch 批，整体替换列表 */
async function loadBatch() {
  const seq = ++requestSeq
  loading.value = true
  try {
    const page = await fetchVideoPage({
      current: batch.value,
      size: PAGE_SIZE,
      mcId: activeMcId.value > 0 ? activeMcId.value : undefined,
    })
    if (seq !== requestSeq) return
    videos.value = page.records ?? []
    total.value = page.total ?? 0
  } catch {
    if (seq !== requestSeq) return
    videos.value = []
    total.value = 0
    ElMessage.error('视频加载失败，请稍后再试')
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

/** 换一批：到最后一页后循环回第 1 批，按钮始终可用 */
function nextBatch() {
  const pages = Math.ceil(total.value / PAGE_SIZE)
  if (pages === 0) {
    ElMessage.info('该分区还没有视频')
    return
  }
  if (batch.value >= pages) {
    batch.value = 1
    ElMessage.info('已经到底，回到第一批')
  } else {
    batch.value += 1
  }
  void loadBatch()
}

/** 切分区：重置批次，从该分区第 1 批重新加载 */
function selectPartition(id: number) {
  if (activeMcId.value === id) return
  activeMcId.value = id
  batch.value = 1
  videos.value = []
  total.value = 0
  void loadBatch()
}

function move(step: number) {
  index.value = (index.value + step + slides.length) % slides.length
}

function openSlide(href?: string) {
  if (href) window.open(href, '_blank', 'noreferrer')
}

onMounted(() => {
  void loadBatch()
  timer = window.setInterval(() => move(1), 5000)
})

onBeforeUnmount(() => {
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
  align-items: flex-start;
  gap: 12px;
  margin: 16px 0;

  &__list {
    flex: 1;
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  &__item {
    padding: 5px 14px;
    border-radius: 14px;
    background: #fff;
    font-size: t.$fs-sm;
    color: var(--text-2);
    cursor: pointer;
    transition: background 0.2s, color 0.2s;

    &:hover {
      background: var(--brand-soft, #fff1f5);
      color: var(--brand);
    }

    &.is-active {
      background: var(--brand);
      color: #fff;
    }
  }

  &__refresh {
    flex: none;
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 5px 14px;
    border: none;
    border-radius: 14px;
    background: #fff;
    font-size: t.$fs-sm;
    color: var(--text-2);
    cursor: pointer;
    transition: background 0.2s, color 0.2s;

    &:hover:not(:disabled) {
      background: var(--brand-soft, #fff1f5);
      color: var(--brand);
    }

    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
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
