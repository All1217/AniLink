<template>
  <div class="upload-panel">
    <!-- ① 选文件：选中后立刻进入信息页，上传在那边进行 -->
    <section v-if="step === 'select'">
      <div
        class="zone"
        :class="{ 'is-dragging': dragging }"
        @click="inputRef?.click()"
        @dragover.prevent="dragging = true"
        @dragleave.prevent="dragging = false"
        @drop.prevent="onDrop"
      >
        <el-icon :size="44"><UploadFilled /></el-icon>
        <p class="zone__title">点击或拖拽视频到此处上传</p>
        <p class="zone__tip">支持 mp4 / webm / mov，单个文件建议不超过 1GB</p>
        <input ref="inputRef" type="file" accept="video/*" hidden @change="onPick" />
      </div>
    </section>

    <!-- ② 填表单：上传进度在页面顶部 -->
    <UploadForm
      v-else-if="step === 'form' && file"
      :file="file"
      :file-name="fileName"
      @reset="reset"
      @done="step = 'done'"
    />

    <!-- ③ 完成 -->
    <section v-else class="done">
      <el-result icon="success" title="投稿已提交" sub-title="稿件进入审核流程，通过后会在首页出现" />
      <div class="done__ops">
        <el-button @click="reset">继续投稿</el-button>
        <el-button type="primary" @click="router.push('/home')">回到首页</el-button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { UploadFilled } from '@element-plus/icons-vue'
import UploadForm from './UploadForm.vue'

const router = useRouter()

const step = ref<'select' | 'form' | 'done'>('select')
const dragging = ref(false)
const file = ref<File | null>(null)
const fileName = ref('')
const inputRef = ref<HTMLInputElement | null>(null)

function onPick(event: Event) {
  enterForm((event.target as HTMLInputElement).files)
}

function onDrop(event: DragEvent) {
  dragging.value = false
  enterForm(event.dataTransfer?.files ?? null)
}

/** 选中文件即切到填写页（与旧版 Upload → EditUpload 一致），不在这里等上传 */
function enterForm(files: FileList | null) {
  const picked = files?.[0]
  if (!picked) return
  file.value = picked
  fileName.value = picked.name
  step.value = 'form'
}

function reset() {
  step.value = 'select'
  file.value = null
  fileName.value = ''
  if (inputRef.value) inputRef.value.value = ''
}
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.upload-panel {
  padding: 20px;
  background: #fff;
  border-radius: var(--radius-lg);
}

.zone {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 64px 0;
  border: 2px dashed var(--line);
  border-radius: var(--radius-lg);
  color: var(--text-3);
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;

  &:hover,
  &.is-dragging {
    border-color: var(--brand);
    background: var(--brand-soft, #fff1f5);
  }

  &__title {
    font-size: t.$fs-md;
    color: var(--text-1);
  }

  &__tip {
    font-size: t.$fs-xs;
  }
}

.done {
  &__ops {
    display: flex;
    justify-content: center;
    gap: 12px;
  }
}
</style>
