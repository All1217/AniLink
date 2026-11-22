<template>
  <el-dialog v-model="visible" width="400px" :show-close="true" align-center title="登录 / 注册">
    <div class="login">
      <div class="login__tabs">
        <span :class="['login__tab', { 'is-active': mode === 'login' }]" @click="switchMode('login')">登录</span>
        <span :class="['login__tab', { 'is-active': mode === 'register' }]" @click="switchMode('register')">注册</span>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
        <el-form-item :label="mode === 'login' ? '用户名' : '用户名（登录用）'" prop="username">
          <el-input
            v-model="form.username"
            maxlength="20"
            :placeholder="mode === 'login' ? '用户名 / 旧数字账号' : '2-20 位，中英文、数字、下划线'"
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            maxlength="72"
            :placeholder="mode === 'login' ? '请输入密码' : '至少 8 位，可用任意字符'"
          />
        </el-form-item>
        <el-form-item v-if="mode === 'register'" label="昵称（可选）" prop="nickname">
          <el-input v-model="form.nickname" maxlength="20" placeholder="展示用，可重复；不填默认同用户名" />
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

/** 用户名：2-20 位，中英文/数字/下划线，不能纯数字（纯数字留给历史账号） */
function validateUsername(_rule: unknown, value: string, callback: (error?: Error) => void) {
  const text = (value ?? '').trim()
  if (!text) return callback(new Error('请输入用户名'))
  if (text.length < 2 || text.length > 20) return callback(new Error('用户名需为 2-20 个字符'))
  if (!/^[\p{Script=Han}A-Za-z0-9_]+$/u.test(text)) return callback(new Error('只能包含中英文、数字和下划线'))
  if (/^\d+$/.test(text) && mode.value === 'register') return callback(new Error('用户名不能是纯数字'))
  callback()
}

/** 密码：至少 8 位、最多 72 字节（bcrypt 上限），不做组合规则 */
function validatePassword(_rule: unknown, value: string, callback: (error?: Error) => void) {
  const text = value ?? ''
  if (!text) return callback(new Error('请输入密码'))
  if (text.length < 8) return callback(new Error('密码至少 8 位'))
  if (new TextEncoder().encode(text).length > 72) return callback(new Error('密码过长（最多 72 字节）'))
  callback()
}

const rules: FormRules = {
  username: [{ validator: validateUsername, trigger: 'blur' }],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  nickname: [
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (mode.value !== 'register' || !value) return callback()
        if (value.trim().length < 2 || value.length > 20) return callback(new Error('昵称需为 2-20 个字符'))
        callback()
      },
      trigger: 'blur',
    },
  ],
}

function switchMode(next: Mode) {
  mode.value = next
  formRef.value?.clearValidate()
}

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    if (mode.value === 'register') {
      // 注册接口直接返回 access-token，不再二次调用登录
      const result = await register({
        username: form.username.trim(),
        password: form.password,
        nickname: form.nickname.trim() || undefined,
      })
      await userStore.applySession(result.accessToken)
      ElMessage.success('注册成功，已自动登录')
    } else {
      await userStore.signIn({ username: form.username.trim(), password: form.password })
      ElMessage.success('登录成功')
    }
    userStore.closeLoginDialog()
    const redirect = route.query.redirect
    if (typeof redirect === 'string' && redirect) {
      router.replace(redirect)
    }
  } catch {
    // 失败文案由请求拦截器统一提示，这里只结束 loading
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
