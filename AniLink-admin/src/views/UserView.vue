<template>
  <div class="page">
    <div class="page-header">
      <h2>用户管理</h2>
      <span class="muted">角色调整仅超级管理员可操作</span>
    </div>

    <div class="table-card">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="UID / 用户名 / 昵称" clearable style="width: 220px" @keyup.enter="search" />
        <el-select v-model="query.status" placeholder="账号状态" clearable style="width: 140px">
          <el-option label="正常" :value="0" />
          <el-option label="封禁" :value="1" />
          <el-option label="注销" :value="2" />
        </el-select>
        <el-select v-model="query.role" placeholder="角色" clearable style="width: 140px">
          <el-option label="普通用户" :value="0" />
          <el-option label="管理员" :value="1" />
          <el-option label="超级管理员" :value="2" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="uid" label="UID" width="150" />
        <el-table-column prop="username" label="用户名" width="160" show-overflow-tooltip />
        <el-table-column prop="nickname" label="昵称" width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : row.status === 1 ? 'danger' : 'info'">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="160">
          <template #default="{ row }">
            <el-select
              v-if="auth.isSuper && row.uid !== auth.profile?.uid"
              :model-value="row.role"
              size="small"
              @change="(value: number) => changeRole(row, value)"
            >
              <el-option label="普通用户" :value="0" />
              <el-option label="管理员" :value="1" />
              <el-option label="超级管理员" :value="2" />
            </el-select>
            <span v-else>{{ roleLabel(row.role) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="密码" width="120">
          <template #default="{ row }">
            <span>******</span>
            <el-button
              link
              type="primary"
              :disabled="!canReset(row) || resettingUid === row.uid"
              :loading="resettingUid === row.uid"
              title="重置密码"
              @click="resetPassword(row)"
            >
              <el-icon><View /></el-icon>
            </el-button>
          </template>
        </el-table-column>
        <el-table-column prop="createDate" label="注册时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.uid !== auth.profile?.uid"
              link
              :type="row.status === 1 ? 'success' : 'danger'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '解封' : '封禁' }}
            </el-button>
            <span v-else class="muted">当前账号</span>
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

    <el-dialog
      v-model="tempPasswordVisible"
      title="临时密码"
      width="480px"
      @closed="clearTempPassword"
    >
      <p class="muted">
        用户 {{ tempUsername }} 的密码已重置。请立即复制并转交用户，关闭弹窗后不再显示。
      </p>
      <el-input v-model="tempPassword" readonly>
        <template #append>
          <el-button @click="copyTempPassword">复制</el-button>
        </template>
      </el-input>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { View } from '@element-plus/icons-vue'
import { adminApi } from '@/api/admin'
import type { AdminUser } from '@/api/types'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const query = reactive({ current: 1, size: 10, status: undefined as number | undefined, role: undefined as number | undefined })
const keyword = ref('')
const rows = ref<AdminUser[]>([])
const total = ref(0)
const loading = ref(false)
const resettingUid = ref<number | null>(null)
const tempPasswordVisible = ref(false)
const tempPassword = ref('')
const tempUsername = ref('')

async function load() {
  loading.value = true
  try {
    const result = await adminApi.users({
      current: query.current,
      size: query.size,
      status: query.status,
      role: query.role,
      keyword: keyword.value.trim() || undefined,
    })
    if (result.code !== 200 || !result.data) {
      ElMessage.error(result.message || '加载失败')
      return
    }
    rows.value = result.data.records
    total.value = result.data.total
  } catch {
    ElMessage.error('加载用户失败')
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
  query.role = undefined
  keyword.value = ''
  search()
}

async function toggleStatus(row: AdminUser) {
  const target = row.status === 1 ? 0 : 1
  const action = target === 1 ? '封禁' : '解封'
  try {
    await ElMessageBox.confirm(`确定${action}用户「${row.nickname}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  const result = await adminApi.userStatus(row.uid, target)
  if (result.code !== 200) {
    ElMessage.error(result.message || '操作失败')
    return
  }
  ElMessage.success(`${action}成功`)
  load()
}

async function changeRole(row: AdminUser, role: number) {
  if (role === row.role) return
  try {
    await ElMessageBox.confirm(
      `确定把用户「${row.nickname}」的角色改为 ${roleLabel(role)} 吗？`,
      '提示',
      { type: 'warning' },
    )
  } catch {
    return
  }
  const result = await adminApi.userRole(row.uid, role)
  if (result.code !== 200) {
    ElMessage.error(result.message || '操作失败')
    return
  }
  ElMessage.success('角色已更新')
  load()
}

/** 普通管理员只能重置普通用户；超级管理员还能重置管理员。 */
function canReset(row: AdminUser) {
  return row.uid !== auth.profile?.uid && (row.role === 0 || auth.isSuper)
}

async function resetPassword(row: AdminUser) {
  if (!canReset(row) || resettingUid.value !== null) return
  try {
    await ElMessageBox.confirm(
      `确定重置用户「${row.nickname}」的密码吗？` +
        '重置后原密码立即失效，该用户所有 refresh 会话会被注销，临时密码只显示一次。',
      '重置密码',
      { type: 'warning', confirmButtonText: '重置' },
    )
  } catch {
    return
  }

  resettingUid.value = row.uid
  try {
    const result = await adminApi.resetUserPassword(row.uid)
    if (result.code !== 200 || !result.data) {
      ElMessage.error(result.message || '重置密码失败')
      return
    }
    tempPassword.value = result.data.tempPassword
    tempUsername.value = result.data.username
    tempPasswordVisible.value = true
  } catch {
    ElMessage.error('重置密码失败')
  } finally {
    resettingUid.value = null
  }
}

function clearTempPassword() {
  tempPassword.value = ''
  tempUsername.value = ''
}

async function copyTempPassword() {
  try {
    await navigator.clipboard.writeText(tempPassword.value)
    ElMessage.success('临时密码已复制')
  } catch {
    ElMessage.warning('复制失败，请手动选择密码')
  }
}

function statusLabel(status: number) {
  return status === 0 ? '正常' : status === 1 ? '封禁' : '注销'
}

function roleLabel(role: number) {
  return role === 2 ? '超级管理员' : role === 1 ? '管理员' : '普通用户'
}

onMounted(load)
</script>
