<template>
  <div class="page">
    <div class="page-header">
      <h2>对象存储</h2>
      <span class="muted">删除前会检查业务引用，被引用的对象不能删除</span>
    </div>

    <div class="table-card">
      <div class="toolbar">
        <el-select v-model="bucket" style="width: 160px" @change="reload">
          <el-option v-for="item in buckets" :key="item" :label="item" :value="item" />
        </el-select>
        <el-input v-model="prefix" placeholder="前缀，如 hls/" clearable style="width: 240px" @keyup.enter="reload" />
        <el-button type="primary" @click="reload">加载</el-button>
        <el-button :disabled="!hasMore" @click="loadMore">加载更多</el-button>
      </div>

      <el-table v-loading="loading" :data="objects" border>
        <el-table-column prop="key" label="对象 Key" min-width="360" show-overflow-tooltip />
        <el-table-column label="大小" width="120">
          <template #default="{ row }">{{ formatSize(row.size) }}</template>
        </el-table-column>
        <el-table-column prop="lastModified" label="更新时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="preview(row)">预览</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <p v-if="!loading && objects.length === 0" class="muted">该前缀下没有对象</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/admin'
import type { AdminObject, AdminRef } from '@/api/types'

const buckets = ref<string[]>([])
const bucket = ref('video')
const prefix = ref('')
const objects = ref<AdminObject[]>([])
const nextCursor = ref<string | null>(null)
const hasMore = ref(false)
const loading = ref(false)

async function loadBuckets() {
  const result = await adminApi.buckets()
  if (result.code === 200 && result.data) {
    buckets.value = result.data
    if (!buckets.value.includes(bucket.value) && buckets.value.length > 0) {
      bucket.value = buckets.value[0]
    }
  }
}

async function load(reset: boolean) {
  if (!reset && !hasMore.value) return
  loading.value = true
  try {
    const result = await adminApi.objects({
      bucket: bucket.value,
      prefix: prefix.value.trim() || undefined,
      cursor: reset ? undefined : nextCursor.value || undefined,
      size: 50,
    })
    if (result.code !== 200 || !result.data) {
      ElMessage.error(result.message || '加载失败')
      return
    }
    objects.value = reset ? result.data.items : [...objects.value, ...result.data.items]
    nextCursor.value = result.data.nextCursor
    hasMore.value = result.data.hasMore
  } catch {
    ElMessage.error('加载对象失败')
  } finally {
    loading.value = false
  }
}

function reload() {
  nextCursor.value = null
  hasMore.value = true
  load(true)
}

function loadMore() {
  load(false)
}

async function preview(row: AdminObject) {
  const result = await adminApi.presign(row.bucket, row.key)
  if (result.code !== 200 || !result.data) {
    ElMessage.error(result.message || '生成预览地址失败')
    return
  }
  window.open(result.data, '_blank', 'noopener')
}

async function remove(row: AdminObject) {
  try {
    await ElMessageBox.confirm(
      `确定删除对象「${row.key}」吗？此操作不可恢复。`,
      '删除对象',
      { type: 'warning', confirmButtonText: '删除' },
    )
  } catch {
    return
  }
  const result = await adminApi.deleteObject(row.bucket, row.key)
  if (result.code === 409) {
    await ElMessageBox.alert(refText(result.data || []), '对象被业务引用，未删除', {
      type: 'warning',
    })
    return
  }
  if (result.code !== 200) {
    ElMessage.error(result.message || '删除失败')
    return
  }
  ElMessage.success('已删除')
  reload()
}

function refText(refs: AdminRef[]) {
  if (refs.length === 0) return '存在业务引用'
  return refs
    .map((ref) => `${ref.type === 'video' ? '稿件' : '用户'} ${ref.id}（${ref.name}）引用字段 ${ref.field}`)
    .join('\n')
}

function formatSize(size: number) {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

onMounted(async () => {
  await loadBuckets()
  reload()
})
</script>
