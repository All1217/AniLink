<template>
  <el-dialog v-model="visible" width="400px" :show-close="true" align-center title="登录 / 注册">
    <div class="login">
      <div class="login__tabs">
        <span :class="['login__tab', { 'is-active': mode === 'login' }]" @click="mode = 'login'">登录</span>
        <span :class="['login__tab', { 'is-active': mode === 'register' }]" @click="mode = 'register'">注册</span>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
        <el-form-item v-if="mode === 'register'" label="昵称" prop="nickname">
          <el-input v-model="form.nickname" maxlength="20" placeholder="2-20 个字符" />
        </el-form-item>
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" maxlength="30" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password maxlength="30" placeholder="请输入密码" />
        </el-form-item>
      </el-form>

      <el-button class="login__submit" type="primary" :loading="loading" @click="onSubmit">
        {{ mode === 'login' ? '登录' : '注册并登录' }}
      </el-button>
      <p class="login__tip">试运行环境，仅用于本地联调</p>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { register } from '@/api/auth'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()

type Mode = 'login' | 'register'
const mode = ref<Mode>('login')
const loading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({ username: '', password: '', nickname: '' })

const visible = computed({
  get: () => userStore.loginDialogVisible,
  set: (value: boolean) => (value ? userStore.openLoginDialog() : userStore.closeLoginDialog()),
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度 2-20', trigger: 'blur' },
  ],
}

async function onSubmit() {
  await formRef.value?.validate().catch(() => Promise.reject(new Error('invalid')))
  loading.value = true
  try {
    if (mode.value === 'register') {
      await register({ nickname: form.nickname, password: form.password })
    }
    await userStore.signIn({ username: form.username, password: form.password })
    userStore.closeLoginDialog()
    ElMessage.success(mode.value === 'login' ? '登录成功' : '注册成功')
    const redirect = route.query.redirect
    if (typeof redirect === 'string' && redirect) {
      router.replace(redirect)
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.login {
  &__tabs {
    display: flex;
    gap: 24px;
    margin-bottom: 16px;
    border-bottom: 1px solid var(--line);
  }

  &__tab {
    padding: 8px 0;
    font-size: t.$fs-md;
    color: var(--text-2);
    cursor: pointer;
    border-bottom: 2px solid transparent;

    &.is-active {
      color: var(--brand);
      border-bottom-color: var(--brand);
      font-weight: 600;
    }
  }

  &__submit {
    width: 100%;
    height: 40px;
    margin-top: 4px;
  }

  &__tip {
    margin-top: 12px;
    font-size: t.$fs-xs;
    color: var(--text-3);
    text-align: center;
  }
}
</style>
