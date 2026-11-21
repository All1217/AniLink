<template>
  <div class="folders">
    <header class="folders__head">
      <span class="folders__count">共 {{ folders.length }} 个收藏夹</span>
      <el-button v-if="isSelf" size="small" type="primary" @click="openCreate">新建收藏夹</el-button>
    </header>

    <div v-if="folders.length > 0" class="folders__grid">
      <article v-for="folder in folders" :key="folder.fid" class="folder" @click="emit('open', folder)">
        <div class="folder__cover" :style="folder.cover ? { backgroundImage: `url(${folder.cover})` } : undefined">
          <span v-if="!folder.cover" class="folder__placeholder">收藏夹</span>
          <span class="folder__count">{{ folder.count }} 个视频</span>
        </div>
        <h3 class="folder__title" :title="folder.title">{{ folder.title }}</h3>
        <div class="folder__foot">
          <span class="folder__vis">{{ folder.visible ? '公开' : '私密' }}</span>
          <button v-if="isSelf" class="folder__edit" @click.stop="openEdit(folder)">编辑</button>
        </div>
      </article>
    </div>

    <EmptyState v-else-if="!loading" :text="isSelf ? '还没有收藏夹，新建一个吧' : '该用户还没有公开收藏夹'" />

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑收藏夹' : '新建收藏夹'" width="380px">
      <el-form label-width="64px">
        <el-form-item label="名称">
          <el-input v-model="form.title" maxlength="20" show-word-limit placeholder="最多 20 字" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="公开">
          <el-switch v-model="form.visible" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import EmptyState from '@/components/base/EmptyState.vue'
import { createFavorite, fetchFavoriteList, updateFavorite } from '@/api/favorite'
import type { Favorite } from '@/types'

const props = defineProps<{ uid: number; isSelf: boolean }>()
const emit = defineEmits<{ (e: 'open', folder: Favorite): void }>()

const folders = ref<Favorite[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const editing = ref<Favorite | null>(null)
const form = reactive({ title: '', description: '', visible: 1 })

async function load() {
  loading.value = true
  try {
    const result = await fetchFavoriteList({ uid: props.uid, current: 1, size: 50 })
    folders.value = result.records ?? []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  form.title = ''
  form.description = ''
  form.visible = 1
  dialogVisible.value = true
}

function openEdit(folder: Favorite) {
  editing.value = folder
  form.title = folder.title
  form.description = folder.description ?? ''
  form.visible = folder.visible ?? 1
  dialogVisible.value = true
}

/** 新建或更新收藏夹 */
async function save() {
  if (!form.title.trim()) {
    ElMessage.warning('请填写收藏夹名称')
    return
  }
  saving.value = true
  try {
    if (editing.value) {
      await updateFavorite({
        fid: editing.value.fid,
        uid: props.uid,
        title: form.title.trim(),
        description: form.description,
        visible: form.visible,
      })
    } else {
      await createFavorite({
        fid: 0,
        uid: props.uid,
        type: 2,
        visible: form.visible,
        cover: '',
        title: form.title.trim(),
        description: form.description,
        count: 0,
        isDelete: 0,
      })
    }
    dialogVisible.value = false
    ElMessage.success('已保存')
    await load()
  } finally {
    saving.value = false
  }
}

watch(() => props.uid, load)
onMounted(load)
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;
@use '@/styles/mixins' as m;

.folders {
  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
  }

  &__count {
    font-size: t.$fs-xs;
    color: var(--text-3);
  }

  &__grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
  }
}

.folder {
  cursor: pointer;
  background: #fff;
  border-radius: var(--radius-lg);
  overflow: hidden;

  &__cover {
    position: relative;
    height: 108px;
    background: linear-gradient(120deg, #f1f2f3, #e3e5e7);
    background-size: cover;
    background-position: center;
  }

  &__placeholder {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--text-3);
    font-size: t.$fs-sm;
  }

  &__count {
    position: absolute;
    right: 6px;
    bottom: 6px;
    padding: 0 5px;
    border-radius: 2px;
    background: rgba(0, 0, 0, 0.45);
    color: #fff;
    font-size: 10px;
  }

  &__title {
    margin: 8px 10px 4px;
    font-size: t.$fs-sm;
    color: var(--text-1);

    @include m.ellipsis-1;
  }

  &__foot {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 10px 10px;
    font-size: t.$fs-xs;
    color: var(--text-3);
  }

  &__edit {
    border: none;
    background: transparent;
    color: var(--link);
    font-size: t.$fs-xs;
  }
}
</style>
