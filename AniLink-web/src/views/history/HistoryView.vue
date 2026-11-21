<template>
  <div class="history">
    <header class="history__head">
      <h1 class="history__title">观看历史</h1>
      <div class="history__filters">
        <span
          v-for="item in ranges"
          :key="item.label"
          :class="['history__filter', { 'is-active': active === item.label }]"
          @click="changeRange(item.label)"
        >
          {{ item.label }}
        </span>
      </div>
      <el-button size="small" :disabled="rows.length === 0" @click="onClear">清空历史</el-button>
    </header>

    <template v-if="rows.length > 0">
      <section v-for="group in groups" :key="group.label" class="group">
        <h2 class="group__label">{{ group.label }}</h2>
        <HistoryEntry v-for="item in group.items" :key="item.id" :item="item" @delete="onDelete" />
      </section>
    </template>

    <EmptyState v-else-if="!loading" text="还没有观看记录" />

    <button v-if="rows.length < total" class="history__more" :disabled="loading" @click="loadMore">
      {{ loading ? '加载中…' : '加载更多' }}
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import HistoryEntry from '@/components/history/HistoryEntry.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import { useUserStore } from '@/stores/user'
import { clearHistory, deleteHistory, fetchHistoryPage, type HistoryQueryVo } from '@/api/history'
import type { HistoryVo } from '@/types'

const PAGE = 20
const ranges = [
  { label: '全部', start: null, end: null },
  { label: '10 分钟以下', start: 0, end: 600 },
  { label: '10-30 分钟', start: 600, end: 1800 },
  { label: '30-60 分钟', start: 1800, end: 3600 },
  { label: '60 分钟以上', start: 3600, end: null },
] as const

const userStore = useUserStore()
const rows = ref<HistoryVo[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const active = ref<string>('全部')

/** 按日期分组：今天 / 昨天 / 具体日期（本地时区） */
const groups = computed(() => {
  const localKey = (d: Date) =>
    `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  const today = localKey(new Date())
  const yesterday = localKey(new Date(Date.now() - 86_400_000))
  const buckets = new Map<string, HistoryVo[]>()
  for (const item of rows.value) {
    const day = (item.viewTime ?? '').slice(0, 10)
    const label = day === today ? '今天' : day === yesterday ? '昨天' : day
    const list = buckets.get(label) ?? []
    list.push(item)
    buckets.set(label, list)
  }
  return [...buckets.entries()].map(([label, items]) => ({ label, items }))
})

/** 分页加载；reset 用于切换时长筛选 */
async function load(reset = false) {
  if (!userStore.uid || loading.value) return
  loading.value = true
  if (reset) {
    rows.value = []
    page.value = 1
  }
  const range = ranges.find((item) => item.label === active.value) ?? ranges[0]
  const query: HistoryQueryVo = {
    uid: userStore.uid,
    current: page.value,
    size: PAGE,
    startDuration: range.start,
    endDuration: range.end,
  }
  try {
    const result = await fetchHistoryPage(query)
    rows.value = [...rows.value, ...(result.records ?? [])]
    total.value = result.total ?? rows.value.length
    page.value += 1
  } finally {
    loading.value = false
  }
}

function loadMore() {
  void load()
}

function changeRange(label: string) {
  if (active.value === label) return
  active.value = label
  void load(true)
}

async function onDelete(vid: number) {
  if (!userStore.uid) return
  await deleteHistory({ uid: userStore.uid, vids: [vid] }).catch(() => undefined)
  rows.value = rows.value.filter((item) => item.vid !== vid)
  total.value = Math.max(0, total.value - 1)
}

async function onClear() {
  const confirmed = await ElMessageBox.confirm('清空全部观看历史？', '提示', { type: 'warning' })
    .then(() => true)
    .catch(() => false)
  if (!confirmed || !userStore.uid) return
  await clearHistory({ uid: userStore.uid }).catch(() => undefined)
  ElMessage.success('已清空')
  rows.value = []
  total.value = 0
}

onMounted(() => load(true))
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.history {
  width: t.$page-width;
  margin: 0 auto;
  padding-top: 16px;

  &__head {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 12px;
  }

  &__title {
    font-size: t.$fs-lg;
    color: var(--text-1);
  }

  &__filters {
    display: flex;
    gap: 8px;
    margin-right: auto;
  }

  &__filter {
    padding: 4px 10px;
    border-radius: 12px;
    background: #fff;
    font-size: t.$fs-xs;
    color: var(--text-2);
    cursor: pointer;

    &.is-active {
      background: var(--brand-soft, #fff1f5);
      color: var(--brand);
    }
  }

  &__more {
    display: block;
    width: 100%;
    margin-top: 16px;
    padding: 10px 0;
    border: 1px solid var(--line);
    border-radius: var(--radius);
    background: #fff;
    color: var(--text-2);
    font-size: t.$fs-sm;
  }
}

.group {
  margin-bottom: 16px;

  &__label {
    margin: 12px 0 8px;
    font-size: t.$fs-sm;
    color: var(--text-2);
  }
}
</style>
