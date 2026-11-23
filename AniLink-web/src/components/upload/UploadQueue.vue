<template>
  <div class="queue">
    <div class="queue__icon">
      <el-icon :size="20"><VideoCamera /></el-icon>
    </div>
    <div class="queue__main">
      <div class="queue__row">
        <span class="queue__name" :title="fileName">{{ fileName }}</span>
        <span :class="['queue__status', `is-${status}`]">
          <el-icon v-if="status === 'done'"><CircleCheckFilled /></el-icon>
          {{ statusText }}
        </span>
        <button v-if="status === 'error'" class="queue__op" @click="emit('retry')">重试</button>
        <button class="queue__op" @click="emit('reset')">重新选择</button>
      </div>
      <el-progress
        :percentage="barPercent"
        :stroke-width="4"
        :show-text="false"
        :status="status === 'error' ? 'exception' : undefined"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { CircleCheckFilled, VideoCamera } from '@element-plus/icons-vue'

const props = defineProps<{
  fileName: string
  percent: number
  status: 'uploading' | 'done' | 'error'
}>()
const emit = defineEmits<{ (e: 'retry'): void; (e: 'reset'): void }>()

/** 上传完成后固定满格，避免服务端处理尾段时进度条回退 */
const barPercent = computed(() => (props.status === 'done' ? 100 : props.percent))

const statusText = computed(() => {
  if (props.status === 'done') return '上传完成'
  if (props.status === 'error') return '上传失败'
  return `上传中 ${props.percent}%`
})
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.queue {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  padding: 12px 14px;
  border: 1px solid t.$line;
  border-radius: t.$radius-lg;
  background: t.$bg-hover;

  &__icon {
    display: flex;
    align-items: center;
    justify-content: center;
    flex: 0 0 34px;
    height: 34px;
    border-radius: t.$radius;
    color: t.$brand;
    background: t.$brand-soft;
  }

  &__main {
    flex: 1;
    min-width: 0;
  }

  &__row {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 6px;
  }

  &__name {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: t.$fs-sm;
    color: t.$text-1;
  }

  &__status {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-size: t.$fs-xs;
    color: t.$text-3;

    &.is-done {
      color: t.$success;
    }

    &.is-error {
      color: t.$danger;
    }
  }

  &__op {
    border: none;
    background: transparent;
    padding: 0;
    font-size: t.$fs-xs;
    color: t.$link;
  }
}
</style>
