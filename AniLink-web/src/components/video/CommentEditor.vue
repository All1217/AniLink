<template>
  <div class="editor">
    <el-avatar :size="40" :src="userStore.avatar || undefined">
      {{ userStore.nickname?.[0] ?? '游' }}
    </el-avatar>
    <div class="editor__main">
      <el-input
        v-model="content"
        type="textarea"
        :rows="3"
        maxlength="300"
        show-word-limit
        :placeholder="placeholder"
        resize="none"
        @focus="ensureLogin"
      />
      <div class="editor__foot">
        <span class="editor__tip">{{ tip }}</span>
        <el-button size="small" type="primary" :loading="busy" :disabled="!content.trim()" @click="submit">
          发布
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { createComment } from '@/api/comment'

const props = withDefaults(
  defineProps<{
    vid: number
    rootId?: number
    parentId?: number
    toUserId?: number
    placeholder?: string
    tip?: string
  }>(),
  { rootId: 0, parentId: 0, toUserId: 0, placeholder: '发一条友善的评论', tip: '登录后可以评论' },
)
const emit = defineEmits<{ (e: 'submitted'): void }>()

const userStore = useUserStore()
const content = ref('')
const busy = ref(false)

function ensureLogin() {
  if (!userStore.isLogin) userStore.openLoginDialog()
}

/** 发表评论/回复：成功后清空输入并通知外部刷新 */
async function submit() {
  if (!userStore.isLogin) {
    userStore.openLoginDialog()
    return
  }
  if (!content.value.trim()) return
  busy.value = true
  try {
    await createComment({
      vid: props.vid,
      uid: userStore.uid,
      content: content.value.trim(),
      rootId: props.rootId,
      parentId: props.parentId,
      toUserId: props.toUserId,
    })
    content.value = ''
    ElMessage.success('发布成功')
    emit('submitted')
  } finally {
    busy.value = false
  }
}
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.editor {
  display: flex;
  gap: 12px;

  &__main {
    flex: 1;
  }

  &__foot {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 12px;
    margin-top: 8px;
  }

  &__tip {
    font-size: t.$fs-xs;
    color: var(--text-3);
  }
}
</style>
