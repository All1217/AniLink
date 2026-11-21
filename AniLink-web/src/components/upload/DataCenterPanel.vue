<template>
  <div class="data-center">
    <header class="data-center__head">
      <div class="data-center__ranges">
        <span
          v-for="item in ranges"
          :key="item.days"
          :class="['data-center__range', { 'is-active': range === item.days }]"
          @click="changeRange(item.days)"
        >
          {{ item.label }}
        </span>
      </div>
      <span class="data-center__hint">统计口径：稿件播放与点赞的按天累计</span>
    </header>

    <div class="data-center__cards">
      <div class="stat">
        <span class="stat__label">区间总播放</span>
        <b class="stat__value">{{ formatCount(playTotal) }}</b>
      </div>
      <div class="stat">
        <span class="stat__label">区间总点赞</span>
        <b class="stat__value">{{ formatCount(likeTotal) }}</b>
      </div>
      <div class="stat">
        <span class="stat__label">粉丝播放</span>
        <b class="stat__value">{{ formatCount(playFansTotal) }}</b>
      </div>
    </div>

    <TrendChart title="播放量趋势" :labels="labels" :series="playSeries" :loading="loading" />
    <TrendChart title="点赞趋势" :labels="labels" :series="likeSeries" :loading="loading" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import TrendChart from './TrendChart.vue'
import { useUserStore } from '@/stores/user'
import { fetchLikeTotal, fetchPlayTotal, type DataResVo } from '@/api/stats'
import { formatCount } from '@/utils/format'

const ranges = [
  { label: '近 4 天', days: 4 },
  { label: '近 7 天', days: 7 },
  { label: '近 30 天', days: 30 },
  { label: '近 90 天', days: 90 },
]

const userStore = useUserStore()
const range = ref(4)
const loading = ref(false)
const play = ref<DataResVo | null>(null)
const like = ref<DataResVo | null>(null)

const ymd = (d: Date) =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`

/** 生成区间日期标签（与后端返回序列等长对齐由 echarts 自行截断） */
const labels = computed(() => {
  const list: string[] = []
  for (let i = range.value - 1; i >= 0; i -= 1) list.push(ymd(new Date(Date.now() - i * 86_400_000)))
  return list
})

const playSeries = computed(() => [
  { name: '总播放量', data: play.value?.mainRes ?? [], color: '#fb7299' },
  { name: '粉丝播放量', data: play.value?.subRes ?? [], color: '#00aeec' },
])

const likeSeries = computed(() => [
  { name: '总点赞', data: like.value?.mainRes ?? [], color: '#fb7299' },
  { name: '粉丝点赞', data: like.value?.subRes ?? [], color: '#00aeec' },
])

const sum = (list?: number[]) => (list ?? []).reduce((acc, item) => acc + (item ?? 0), 0)
const playTotal = computed(() => sum(play.value?.mainRes))
const playFansTotal = computed(() => sum(play.value?.subRes))
const likeTotal = computed(() => sum(like.value?.mainRes))

/** 并行拉播放与点赞两条趋势 */
async function load() {
  if (!userStore.uid) return
  loading.value = true
  const end = ymd(new Date())
  const begin = labels.value[0] ?? end
  try {
    const [playData, likeData] = await Promise.all([
      fetchPlayTotal(userStore.uid, begin, end).catch(() => null),
      fetchLikeTotal(userStore.uid, begin, end).catch(() => null),
    ])
    play.value = playData
    like.value = likeData
  } finally {
    loading.value = false
  }
}

function changeRange(days: number) {
  if (range.value === days) return
  range.value = days
  void load()
}

onMounted(load)
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.data-center {
  display: flex;
  flex-direction: column;
  gap: 16px;

  &__head {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 12px 16px;
    background: #fff;
    border-radius: var(--radius-lg);
  }

  &__ranges {
    display: flex;
    gap: 8px;
  }

  &__range {
    padding: 4px 12px;
    border-radius: 12px;
    background: var(--bg-hover);
    font-size: t.$fs-xs;
    color: var(--text-2);
    cursor: pointer;

    &.is-active {
      background: var(--brand-soft, #fff1f5);
      color: var(--brand);
    }
  }

  &__hint {
    margin-left: auto;
    font-size: t.$fs-xs;
    color: var(--text-3);
  }

  &__cards {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;
  }
}

.stat {
  padding: 16px;
  background: #fff;
  border-radius: var(--radius-lg);

  &__label {
    display: block;
    font-size: t.$fs-xs;
    color: var(--text-3);
  }

  &__value {
    display: block;
    margin-top: 4px;
    font-size: 24px;
    color: var(--text-1);
  }
}
</style>
