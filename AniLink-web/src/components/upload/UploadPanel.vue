<template>
  <div class="upload-panel">
    <!-- ① 选文件 -->
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
      <el-progress v-if="uploading" :percentage="percent" :stroke-width="10" class="zone__progress" />
    </section>

    <!-- ② 填表单 -->
    <UploadForm
      v-else-if="step === 'form'"
      :video-url="videoUrl"
      :duration="duration"
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
import { uploadVideo } from '@/api/upload'

const router = useRouter()

const step = ref<'select' | 'form' | 'done'>('select')
const dragging = ref(false)
const uploading = ref(false)
const percent = ref(0)
const fileName = ref('')
const videoUrl = ref('')
const duration = ref(0)
const inputRef = ref<HTMLInputElement | null>(null)

/** 读取视频时长（提交时后端需要秒数） */
function readDuration(file: File): Promise<number> {
  return new Promise((resolve) => {
    const url = URL.createObjectURL(file)
    const video = document.createElement('video')
    video.preload = 'metadata'
    video.onloadedmetadata = () => {
      const value = video.duration
      URL.revokeObjectURL(url)
      resolve(Number.isFinite(value) ? Math.round(value) : 0)
    }
    video.onerror = () => {
      URL.revokeObjectURL(url)
      resolve(0)
    }
    video.src = url
  })
}

function onPick(event: Event) {
  void startUpload((event.target as HTMLInputElement).files)
}

function onDrop(event: DragEvent) {
  dragging.value = false
  void startUpload(event.dataTransfer?.files ?? null)
}

/** 上传视频：拿到 URL 后进入表单 */
async function startUpload(files: FileList | null) {
  const file = files?.[0]
  if (!file) return
  fileName.value = file.name
  duration.value = await readDuration(file)
  uploading.value = true
  percent.value = 0
  try {
    videoUrl.value = await uploadVideo(file, (value) => (percent.value = value))
    step.value = 'form'
  } catch {
    step.value = 'select'
  } finally {
    uploading.value = false
  }
}

function reset() {
  step.value = 'select'
  videoUrl.value = ''
  fileName.value = ''
  duration.value = 0
  percent.value = 0
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

  &__progress {
    margin-top: 20px;
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
