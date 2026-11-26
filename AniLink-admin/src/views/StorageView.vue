<template>
  <div class="page">
    <div class="page-header">
      <h2>对象存储</h2>
      <span class="muted">删除前会检查业务引用，被引用的对象不能删除</span>
    </div>

    <div class="table-card">
      <div class="toolbar">
        <el-select v-model="bucket" :disabled="busy" style="width: 160px" @change="reload">
          <el-option v-for="item in buckets" :key="item" :label="item" :value="item" />
        </el-select>
        <el-input
          v-model="prefix"
          :disabled="busy"
          placeholder="前缀，如 hls/"
          clearable
          style="width: 240px"
          @keyup.enter="reload"
        />
        <el-button type="primary" :disabled="busy" @click="reload">加载</el-button>
        <el-button :disabled="!hasMore || busy" @click="loadMore">加载更多</el-button>
        <el-button type="danger" plain :loading="cleaning" :disabled="busy" @click="cleanup">
          一键清除冗余存储
        </el-button>
        <el-button type="warning" plain :loading="backfilling" :disabled="busy" @click="backfillCoverThumbs">
          重建封面缩略图
        </el-button>
      </div>

      <el-table v-loading="busy" element-loading-text="正在处理，请稍候..." :data="objects" border>
        <el-table-column prop="key" label="对象 Key" min-width="360" show-overflow-tooltip />
        <el-table-column label="大小" width="120">
          <template #default="{ row }">{{ formatSize(row.size) }}</template>
        </el-table-column>
        <el-table-column prop="lastModified" label="更新时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="busy" @click="preview(row)">预览</el-button>
            <el-button link type="danger" :disabled="busy" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <p v-if="!busy && objects.length === 0" class="muted">该前缀下没有对象</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
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
const cleaning = ref(false)
const backfilling = ref(false)
const busy = computed(() => loading.value || cleaning.value || backfilling.value)

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
  if (cleaning.value) return
  nextCursor.value = null
  hasMore.value = true
  load(true)
}

function loadMore() {
  if (cleaning.value) return
  load(false)
}

async function preview(row: AdminObject) {
  if (busy.value) return
  const result = await adminApi.presign(row.bucket, row.key)
  if (result.code !== 200 || !result.data) {
    ElMessage.error(result.message || '生成预览地址失败')
    return
  }
  window.open(result.data, '_blank', 'noopener')
}

async function remove(row: AdminObject) {
  if (busy.value) return
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

async function cleanup() {
  if (busy.value) return
  const scope = prefix.value.trim() ? `前缀「${prefix.value.trim()}」` : '整个桶'
  try {
    await ElMessageBox.confirm(
      `将扫描 ${bucket.value} 桶${scope} 下的全部对象，删除没有任何业务引用的对象；` +
        '被引用的对象会自动跳过。删除不可恢复，是否继续？',
      '一键清除冗余存储',
      { type: 'warning', confirmButtonText: '开始清理', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  cleaning.value = true
  try {
    const result = await adminApi.cleanup(bucket.value, prefix.value.trim() || undefined)
    if (result.code !== 200 || !result.data) {
      ElMessage.error(result.message || '清理失败')
      return
    }
    const { scanned, deleted, skipped, failed, failedKeys } = result.data
    const summary =
      `扫描 ${scanned} 个，删除 ${deleted} 个，跳过 ${skipped} 个被引用对象` +
      (failed > 0 ? `，失败 ${failed} 个` : '')
    if (failed > 0) {
      ElMessage.warning(summary)
    } else {
      ElMessage.success(summary)
    }
    if (failedKeys && failedKeys.length > 0) {
      await ElMessageBox.alert(failedKeys.join('\n'), '部分对象删除失败', { type: 'warning' })
    }
    await load(true)
  } catch {
    ElMessage.error('清理失败')
  } finally {
    cleaning.value = false
  }
}

/** 为缺少缩略图的历史封面补生成 480w WebP，重复执行会跳过已处理的稿件 */
async function backfillCoverThumbs() {
  if (busy.value) return
  try {
    await ElMessageBox.confirm(
      '将为缺少缩略图的历史封面生成 480w WebP 并写回，已处理的稿件会自动跳过。是否继续？',
      '重建封面缩略图',
      { type: 'info', confirmButtonText: '开始', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  backfilling.value = true
  try {
    const result = await adminApi.backfillCoverThumbs()
    if (result.code !== 200 || !result.data) {
      ElMessage.error(result.message || '回填失败')
      return
    }
    const { scanned, updated, skipped, failed } = result.data
    const summary =
      `扫描 ${scanned} 条，生成 ${updated} 个缩略图，跳过 ${skipped} 条` +
      (failed > 0 ? `，失败 ${failed} 条` : '')
    if (failed > 0) {
      ElMessage.warning(summary)
    } else {
      ElMessage.success(summary)
    }
    if (bucket.value === 'video-cover') await load(true)
  } catch (error) {
    // 把 axios 的真实原因带出来，避免只看到一句“回填失败”
    const err = error as { message?: string; response?: { status?: number; data?: { message?: string } } }
    const detail = err.response?.data?.message || err.message || '未知错误'
    ElMessage.error(`回填失败：${detail}${err.response?.status ? `（HTTP ${err.response.status}）` : ''}`)
  } finally {
    backfilling.value = false
  }
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
