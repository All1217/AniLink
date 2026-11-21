<template>
  <div class="form">
    <header class="form__head">
      <span class="form__file">{{ fileName }}</span>
      <button class="form__reset" @click="emit('reset')">重新选择</button>
    </header>

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
          <el-radio :value="0">自制</el-radio>
          <el-radio :value="1">转载</el-radio>
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
          <el-radio :value="1">公开</el-radio>
          <el-radio :value="0">仅自己可见</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="submit">提交投稿</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { partitions } from '@/config/site'
import { submitVideo, uploadCover } from '@/api/upload'

const props = defineProps<{ videoUrl: string; duration: number; fileName: string }>()
const emit = defineEmits<{ (e: 'reset'): void; (e: 'done'): void }>()

const userStore = useUserStore()
const title = ref('')
const type = ref(0)
const mcId = ref<number>()
const tags = ref<string[]>([])
const descr = ref('')
const visible = ref(1)
const coverUrl = ref('')
const coverUploading = ref(false)
const submitting = ref(false)
const coverRef = ref<HTMLInputElement | null>(null)

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

/** 提交投稿：字段与后端 Video 实体一致（status=1 进入审核） */
async function submit() {
  if (!title.value.trim() || !mcId.value || !props.videoUrl) {
    ElMessage.warning('请完善标题与分区')
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
      duration: props.duration,
      mcId: mcId.value,
      scId: 1,
      status: 1,
      visible: visible.value,
      tags: tags.value.join(' '),
      descr: descr.value,
      coverUrl: coverUrl.value,
      videoUrl: props.videoUrl,
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
  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid var(--line);
  }

  &__file {
    font-size: t.$fs-sm;
    color: var(--text-2);
  }

  &__reset {
    border: none;
    background: transparent;
    color: var(--link);
    font-size: t.$fs-xs;
  }

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
