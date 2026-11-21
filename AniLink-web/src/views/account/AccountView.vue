<template>
  <div class="account">
    <h1 class="account__title">账号设置</h1>

    <section class="card">
      <h2 class="card__title">头像</h2>
      <div class="avatar">
        <el-avatar :size="72" :src="avatarUrl || undefined">{{ userStore.nickname?.[0] ?? 'U' }}</el-avatar>
        <div class="avatar__ops">
          <input ref="fileInput" type="file" accept="image/*" hidden @change="onPickFile" />
          <el-button size="small" :loading="uploading" @click="fileInput?.click()">上传新头像</el-button>
          <span class="avatar__tip">支持 jpg / png，上传后立即生效</span>
        </div>
      </div>
    </section>

    <section class="card">
      <h2 class="card__title">基本资料</h2>
      <el-form label-width="72px" class="form">
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" maxlength="15" show-word-limit />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio :value="0">男</el-radio>
            <el-radio :value="1">女</el-radio>
            <el-radio :value="2">保密</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="生日">
          <el-date-picker v-model="form.birthDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="学校">
          <el-input v-model="form.school" maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="个性签名">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="saveProfile">保存资料</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="card">
      <h2 class="card__title">空间公告</h2>
      <el-input v-model="notice" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="展示在个人空间顶部" />
      <div class="card__foot">
        <el-button size="small" :loading="savingNotice" @click="saveNotice">保存公告</el-button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { fetchUserStats, fetchUserVo, updateAvatar, updateNotice, updateUserInfo } from '@/api/user'
import { uploadAvatar } from '@/api/upload'

const userStore = useUserStore()

const fileInput = ref<HTMLInputElement | null>(null)
const uploading = ref(false)
const saving = ref(false)
const savingNotice = ref(false)
const notice = ref('')
const avatarUrl = ref('')

const form = reactive({
  nickname: '',
  gender: 2,
  birthDate: '',
  school: '',
  description: '',
})

/** 加载当前用户资料与公告 */
async function load() {
  if (!userStore.uid) return
  const [vo, stats] = await Promise.all([
    fetchUserVo({ uid: userStore.uid }).catch(() => null),
    fetchUserStats({ uid: userStore.uid }).catch(() => null),
  ])
  if (vo) {
    form.nickname = vo.nickname ?? ''
    form.gender = vo.gender ?? 2
    form.birthDate = (vo.birthDate ?? '').slice(0, 10) || '1980-01-01'
    form.school = vo.school ?? ''
    form.description = vo.description ?? ''
  }
  avatarUrl.value = userStore.avatar
  notice.value = stats?.notice ?? ''
}

/** 上传头像：先上传取 URL，再写回用户资料 */
async function onPickFile(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  uploading.value = true
  try {
    const url = await uploadAvatar(file)
    await updateAvatar({ uid: userStore.uid, avatar: url })
    avatarUrl.value = url
    await userStore.loadUserInfo()
    ElMessage.success('头像已更新')
  } finally {
    uploading.value = false
    if (fileInput.value) fileInput.value.value = ''
  }
}

async function saveProfile() {
  saving.value = true
  try {
    await updateUserInfo({
      uid: userStore.uid,
      nickname: form.nickname,
      gender: form.gender,
      birthDate: form.birthDate,
      school: form.school,
      description: form.description,
    })
    await userStore.loadUserInfo()
    ElMessage.success('资料已保存')
  } finally {
    saving.value = false
  }
}

async function saveNotice() {
  savingNotice.value = true
  try {
    await updateNotice({ uid: userStore.uid, notice: notice.value })
    ElMessage.success('公告已保存')
  } finally {
    savingNotice.value = false
  }
}

onMounted(load)
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.account {
  width: 720px;
  margin: 0 auto;
  padding-top: 16px;

  &__title {
    margin-bottom: 12px;
    font-size: t.$fs-lg;
    color: var(--text-1);
  }
}

.card {
  padding: 16px 20px;
  margin-bottom: 16px;
  background: #fff;
  border-radius: var(--radius-lg);

  &__title {
    margin-bottom: 12px;
    font-size: t.$fs;
    color: var(--text-1);
  }

  &__foot {
    margin-top: 12px;
    text-align: right;
  }
}

.avatar {
  display: flex;
  align-items: center;
  gap: 16px;

  &__ops {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  &__tip {
    font-size: t.$fs-xs;
    color: var(--text-3);
  }
}

.form {
  max-width: 480px;
}
</style>
