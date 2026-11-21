<template>
  <div class="chart">
    <h3 class="chart__title">{{ title }}</h3>
    <div ref="el" v-loading="loading" class="chart__canvas" />
    <p v-if="!loading && isEmpty" class="chart__empty">所选区间没有数据</p>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { echarts, type EChartsInstance } from '@/utils/echarts'

interface SeriesItem {
  name: string
  data: number[]
  color?: string
}

const props = withDefaults(
  defineProps<{ title: string; labels: string[]; series: SeriesItem[]; loading?: boolean }>(),
  { loading: false },
)

const el = ref<HTMLDivElement | null>(null)
let chart: EChartsInstance | null = null
let observer: ResizeObserver | null = null

const isEmpty = computed(() => props.series.every((item) => item.data.length === 0))

/** 组装折线图配置：面积渐变 + 细网格，贴近 B 站数据中心的观感 */
function render() {
  if (!chart) return
  chart.setOption(
    {
      grid: { left: 8, right: 16, top: 32, bottom: 8, containLabel: true },
      tooltip: { trigger: 'axis' },
      legend: { top: 0, right: 0, itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 12, color: '#9499a0' } },
      xAxis: {
        type: 'category',
        data: props.labels,
        boundaryGap: false,
        axisLine: { lineStyle: { color: '#e3e5e7' } },
        axisLabel: { color: '#9499a0', fontSize: 11 },
        axisTick: { show: false },
      },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: '#f1f2f3' } },
        axisLabel: { color: '#9499a0', fontSize: 11 },
      },
      series: props.series.map((item) => ({
        name: item.name,
        type: 'line',
        smooth: true,
        showSymbol: false,
        data: item.data,
        itemStyle: { color: item.color ?? '#fb7299' },
        areaStyle: { opacity: 0.12 },
      })),
    },
    true,
  )
}

onMounted(() => {
  if (!el.value) return
  chart = echarts.init(el.value)
  render()
  observer = new ResizeObserver(() => chart?.resize())
  observer.observe(el.value)
})

watch(() => [props.labels, props.series], render, { deep: true })

onBeforeUnmount(() => {
  observer?.disconnect()
  chart?.dispose()
  chart = null
})
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.chart {
  padding: 16px;
  background: #fff;
  border-radius: var(--radius-lg);

  &__title {
    margin-bottom: 8px;
    font-size: t.$fs-sm;
    color: var(--text-1);
  }

  &__canvas {
    height: 260px;
  }

  &__empty {
    margin-top: -140px;
    text-align: center;
    font-size: t.$fs-xs;
    color: var(--text-3);
  }
}
</style>
