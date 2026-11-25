<template>
  <div class="page">
    <div class="page-header">
      <h2>视频稿件</h2>
      <span class="muted">只管理逻辑状态，不修改 MinIO 与转码</span>
    </div>

    <div class="table-card">
      <div class="toolbar">
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-input v-model="uidInput" placeholder="作者 UID" clearable style="width: 160px" />
        <el-input v-model="keyword" placeholder="标题关键字" clearable style="width: 220px" @keyup.enter="search" />
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="vid" label="VID" width="90" />
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="authorName" label="作者" width="140" show-overflow-tooltip />
        <el-table-column prop="uid" label="作者UID" width="150" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="可见性" width="100">
          <template #default="{ row }">{{ row.visible === 0 ? '公开' : '仅自己' }}</template>
        </el-table-column>
        <el-table-column label="转码" width="100">
          <template #default="{ row }">{{ transcodeLabel(row.transcodeStatus) }}</template>
        </el-table-column>
        <el-table-column prop="uploadDate" label="上传时间" width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 0" link type="success" @click="changeStatus(row, 1)">通过</el-button>
            <el-button v-if="row.status === 0" link type="danger" @click="changeStatus(row, 2)">驳回</el-button>
            <el-button v-if="row.status === 1 || row.status === 2" link type="warning" @click="changeStatus(row, 3)">
              下架
            </el-button>
            <el-button v-if="row.status !== 3" link type="primary" @click="toggleVisible(row)">
              {{ row.visible === 0 ? '设仅自己' : '设公开' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="load"
          @size-change="search"
        />
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="稿件详情" width="680px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="VID">{{ detail.vid }}</el-descriptions-item>
        <el-descriptions-item label="作者">{{ detail.authorName }}（{{ detail.uid }}）</el-descriptions-item>
        <el-descriptions-item label="标题" :span="2">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusLabel(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="可见性">{{ detail.visible === 0 ? '公开' : '仅自己' }}</el-descriptions-item>
        <el-descriptions-item label="MP4 地址" :span="2">{{ detail.videoUrl }}</el-descriptions-item>
        <el-descriptions-item label="HLS 地址" :span="2">{{ detail.hlsUrl || '未生成' }}</el-descriptions-item>
        <el-descriptions-item label="封面地址" :span="2">{{ detail.coverUrl }}</el-descriptions-item>
        <el-descriptions-item label="删除时间">{{ detail.deleteDate || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/admin'
import type { AdminVideo } from '@/api/types'

type TagType = '' | 'success' | 'info' | 'warning' | 'danger'

const statusOptions = [
  { label: '审核中', value: 0 },
  { label: '已过审', value: 1 },
  { label: '未通过', value: 2 },
  { label: '已删除', value: 3 },
]
const query = reactive({ current: 1, size: 10, status: undefined as number | undefined })
const uidInput = ref('')
const keyword = ref('')
const rows = ref<AdminVideo[]>([])
const total = ref(0)
const loading = ref(false)
const detailVisible = ref(false)
const detail = ref<AdminVideo | null>(null)

async function load() {
  loading.value = true
  try {
    const result = await adminApi.videos({
      current: query.current,
      size: query.size,
      status: query.status,
      uid: uidInput.value ? Number(uidInput.value) : undefined,
      keyword: keyword.value.trim() || undefined,
    })
    if (result.code !== 200 || !result.data) {
      ElMessage.error(result.message || '加载失败')
      return
    }
    rows.value = result.data.records
    total.value = result.data.total
  } catch {
    ElMessage.error('加载稿件失败')
  } finally {
    loading.value = false
  }
}

function search() {
  query.current = 1
  load()
}

function reset() {
  query.status = undefined
  uidInput.value = ''
  keyword.value = ''
  search()
}

async function changeStatus(row: AdminVideo, status: number) {
  const action = status === 1 ? '通过' : status === 2 ? '驳回' : '下架'
  try {
    await ElMessageBox.confirm(`确定${action}稿件「${row.title}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  const result = await adminApi.videoStatus(row.vid, status)
  if (result.code !== 200) {
    ElMessage.error(result.message || '操作失败')
    return
  }
  ElMessage.success(`${action}成功`)
  load()
}

async function toggleVisible(row: AdminVideo) {
  const target = row.visible === 0 ? 1 : 0
  const result = await adminApi.videoVisible(row.vid, target)
  if (result.code !== 200) {
    ElMessage.error(result.message || '操作失败')
    return
  }
  ElMessage.success('可见性已更新')
  load()
}

async function openDetail(row: AdminVideo) {
  const result = await adminApi.videoDetail(row.vid)
  if (result.code !== 200 || !result.data) {
    ElMessage.error(result.message || '加载详情失败')
    return
  }
  detail.value = result.data
  detailVisible.value = true
}

function statusLabel(status: number) {
  return statusOptions.find((item) => item.value === status)?.label || '未知'
}

function statusType(status: number): TagType {
  return status === 1 ? 'success' : status === 2 ? 'danger' : status === 3 ? 'info' : 'warning'
}

function transcodeLabel(status: number) {
  return ['待转码', '转码中', '已完成', '失败'][status] || '未知'
}

onMounted(load)
</script>
