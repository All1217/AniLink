<template>
  <div class="form">
    <UploadQueue
      :file-name="fileName"
      :percent="percent"
      :status="status"
      @retry="startUpload"
      @reset="emit('reset')"
    />

    <el-form label-width="72px">
      <el-form-item label="封面">
        <div class="cover">
          <div class="cover__preview" :style="coverUrl ? { backgroundImage: `url(${coverUrl})` } : undefined">
            <span v-if="!coverUrl">16:10 封面</span>
          </div>
          <div class="cover__ops">
            <input ref="coverRef" type="file" accept="image/*" hidden @change="onCover" />
            <el-button size="small" :loading="coverUploading" @click="coverRef?.click()">上传封面</el-button>
            <span class="cover__tip">不上传则使用系统默认封面</span>
          </div>
        </div>
      </el-form-item>

      <el-form-item label="标题" required>
        <el-input v-model="title" maxlength="80" show-word-limit placeholder="请输入稿件标题" />
      </el-form-item>

      <el-form-item label="类型">
        <el-radio-group v-model="type">
          <el-radio :value="1">自制</el-radio>
          <el-radio :value="2">转载</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="分区">
        <el-select v-model="mcId" placeholder="选择分区" class="form__select">
          <el-option v-for="item in partitions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>

      <el-form-item label="标签">
        <el-input-tag v-model="tags" :max="10" placeholder="回车创建标签，最多 10 个" />
      </el-form-item>

      <el-form-item label="简介">
        <el-input v-model="descr" type="textarea" :rows="4" maxlength="2000" show-word-limit />
      </el-form-item>

      <el-form-item label="可见范围">
        <el-radio-group v-model="visible">
          <el-radio :value="0">公开</el-radio>
          <el-radio :value="1">仅自己可见</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="submit">提交投稿</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { partitions } from '@/config/site'
import { submitVideo, uploadCover, uploadVideo } from '@/api/upload'
import { readVideoDuration } from '@/utils/video'
import UploadQueue from './UploadQueue.vue'

const props = defineProps<{ file: File; fileName: string }>()
const emit = defineEmits<{ (e: 'reset'): void; (e: 'done'): void }>()

const userStore = useUserStore()
/** 视频上传状态：进入信息页即开始上传，用户边填信息边等 */
const status = ref<'uploading' | 'done' | 'error'>('uploading')
const percent = ref(0)
const videoUrl = ref('')
/** 时长与上传并行解析，提交时再取，避免拖慢上传启动 */
const durationTask = readVideoDuration(props.file)
const title = ref('')
/** type：1 自制 / 2 转载（与 video.type 列注释一致） */
const type = ref(1)
const mcId = ref<number>()
const tags = ref<string[]>([])
const descr = ref('')
/** visible：0 公开 / 1 仅自己可见（与 video.visible 列注释一致） */
const visible = ref(0)
const coverUrl = ref('')
const coverUploading = ref(false)
const submitting = ref(false)
const coverRef = ref<HTMLInputElement | null>(null)
/** 当前上传的取消句柄：重新上传或离开本页时中断旧请求，防止进度串台 */
let uploadCtrl: AbortController | null = null

/** 上传视频：本页只负责进度展示，上传失败可原地重试（不必重新选文件） */
async function startUpload() {
  uploadCtrl?.abort()
  const ctrl = new AbortController()
  uploadCtrl = ctrl
  status.value = 'uploading'
  percent.value = 0
  videoUrl.value = ''
  try {
    // 旧版逻辑：请求体发完但服务端还在存储时不留 100%，避免进度条先满、状态还没完成
    videoUrl.value = await uploadVideo(
      props.file,
      (value) => (percent.value = value >= 100 ? 90 : value),
      ctrl.signal,
    )
    percent.value = 100
    status.value = 'done'
  } catch {
    // 主动中断（换了文件/离开页面）不算失败
    if (!ctrl.signal.aborted) status.value = 'error'
  }
}

onMounted(() => void startUpload())
onBeforeUnmount(() => uploadCtrl?.abort())

async function onCover(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  coverUploading.value = true
  try {
    coverUrl.value = await uploadCover(file)
  } finally {
    coverUploading.value = false
  }
}

/**
 * 提交投稿：字段与后端 Video 实体一致。
 * status 固定 1（已过审）：后端没有审核后台，而列表查询是 visible=0 AND status=1，
 * 若传 0（审核中）稿件将永远不会出现在首页；库中既有 15 条数据也全是 status=1。
 */
async function submit() {
  if (!title.value.trim() || !mcId.value) {
    ElMessage.warning('请完善标题与分区')
    return
  }
  if (status.value !== 'done' || !videoUrl.value) {
    ElMessage.warning('视频还在上传中，请稍候')
    return
  }
  submitting.value = true
  try {
    await submitVideo({
      vid: 0,
      uid: userStore.uid,
      title: title.value.trim(),
      type: type.value,
      auth: 0,
      duration: await durationTask,
      mcId: mcId.value,
      scId: 1,
      status: 1,
      visible: visible.value,
      tags: tags.value.join(' '),
      descr: descr.value,
      coverUrl: coverUrl.value,
      videoUrl: videoUrl.value,
    })
    emit('done')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.form {
  &__select {
    width: 220px;
  }
}

.cover {
  display: flex;
  gap: 16px;

  &__preview {
    width: 160px;
    height: 100px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: var(--radius);
    background: var(--line-soft, #f1f2f3) center/cover no-repeat;
    font-size: t.$fs-xs;
    color: var(--text-3);
  }

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
</style>
