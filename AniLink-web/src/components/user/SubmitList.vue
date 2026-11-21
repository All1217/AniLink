<template>
  <div class="submit-list">
    <div v-if="list.length > 0" class="submit-list__grid">
      <VideoCard v-for="item in list" :key="item.vid" :video="item" />
    </div>

    <EmptyState v-else-if="!loading" :text="isSelf ? '还没有投稿，去投稿第一个吧' : '该用户还没有投稿'" />

    <button v-if="list.length < total" class="submit-list__more" :disabled="loading" @click="loadMore">
      {{ loading ? '加载中…' : '加载更多' }}
    </button>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import VideoCard from '@/components/base/VideoCard.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import { fetchUserVideoPage } from '@/api/video'
import type { VideoVo } from '@/types'

const props = defineProps<{ uid: number; isSelf: boolean }>()

const list = ref<VideoVo[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)

/** 投稿列表分页（pageVideo 带 uid） */
async function load(reset = false) {
  if (loading.value) return
  loading.value = true
  if (reset) {
    list.value = []
    page.value = 1
  }
  try {
    const result = await fetchUserVideoPage({ uid: props.uid, current: page.value, size: 20 })
    list.value = [...list.value, ...(result.records ?? [])]
    total.value = result.total ?? list.value.length
    page.value += 1
  } finally {
    loading.value = false
  }
}

function loadMore() {
  void load()
}

watch(
  () => props.uid,
  () => load(true),
)
onMounted(() => load(true))
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.submit-list {
  &__grid {
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    gap: 20px 16px;
  }

  &__more {
    display: block;
    width: 100%;
    margin-top: 20px;
    padding: 10px 0;
    border: 1px solid var(--line);
    border-radius: var(--radius);
    background: #fff;
    color: var(--text-2);
    font-size: t.$fs-sm;

    &:hover {
      border-color: var(--brand);
      color: var(--brand);
    }
  }
}
</style>
