<template>
  <div class="fav-detail">
    <header class="fav-detail__head">
      <button class="fav-detail__back" @click="emit('back')">← 返回收藏夹</button>
      <h2 class="fav-detail__title">{{ folder?.title || '收藏夹' }}</h2>
      <span class="fav-detail__count">{{ total }} 个视频</span>
    </header>

    <div v-if="isSelf" class="fav-detail__ops">
      <el-checkbox :model-value="allSelected" @change="toggleAll">全选</el-checkbox>
      <span class="fav-detail__selected">已选 {{ selected.length }}</span>
      <el-button size="small" :disabled="selected.length === 0" @click="openTransfer('uncollect')">取消收藏</el-button>
      <el-button size="small" :disabled="selected.length === 0" @click="openTransfer('move')">移动到…</el-button>
      <el-button size="small" :disabled="selected.length === 0" @click="openTransfer('copy')">复制到…</el-button>
    </div>

    <div v-if="list.length > 0" class="fav-detail__grid">
      <div v-for="item in list" :key="item.vid" class="cell" :class="{ 'is-selected': selected.includes(item.vid) }">
        <el-checkbox
          v-if="isSelf"
          class="cell__check"
          :model-value="selected.includes(item.vid)"
          @change="toggleOne(item.vid)"
        />
        <RouterLink :to="`/video/${item.vid}`" class="cell__link">
          <VideoCard :video="item" />
        </RouterLink>
      </div>
    </div>

    <EmptyState v-else-if="!loading" text="收藏夹还是空的" />

    <button v-if="list.length < total" class="fav-detail__more" :disabled="loading" @click="loadMore">
      {{ loading ? '加载中…' : '加载更多' }}
    </button>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="360px">
      <el-select v-if="mode !== 'uncollect'" v-model="targetFid" placeholder="选择目标收藏夹" class="fav-detail__select">
        <el-option v-for="item in targets" :key="item.fid" :label="item.title" :value="item.fid" />
      </el-select>
      <p v-else class="fav-detail__tip">将从当前收藏夹移除选中的 {{ selected.length }} 个视频</p>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="busy" @click="submitTransfer">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import VideoCard from '@/components/base/VideoCard.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import { batchCopy, batchMove, fetchFavoriteList, fetchFavoriteVideoPage, uncollectVideos } from '@/api/favorite'
import type { Favorite, VideoFavoriteVo } from '@/types'

type TransferMode = 'uncollect' | 'move' | 'copy'

const props = defineProps<{ fid: number; uid: number; isSelf: boolean }>()
const emit = defineEmits<{ (e: 'back'): void }>()

const list = ref<VideoFavoriteVo[]>([])
const folder = ref<Favorite | null>(null)
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const selected = ref<number[]>([])
const dialogVisible = ref(false)
const busy = ref(false)
const mode = ref<TransferMode>('uncollect')
const targetFid = ref<number | undefined>()
const targets = ref<Favorite[]>([])

const allSelected = computed(() => list.value.length > 0 && selected.value.length === list.value.length)
const dialogTitle = computed(
  () => ({ uncollect: '取消收藏', move: '移动视频', copy: '复制视频' })[mode.value],
)

/** 拉取收藏夹内视频（20/页） */
async function load(reset = false) {
  if (loading.value) return
  loading.value = true
  if (reset) {
    list.value = []
    page.value = 1
    selected.value = []
  }
  try {
    const result = await fetchFavoriteVideoPage(props.fid, page.value)
    list.value = [...list.value, ...(result.records ?? [])]
    total.value = result.total ?? list.value.length
    page.value += 1
  } finally {
    loading.value = false
  }
}

/** 并行取收藏夹标题与可移动目标 */
async function loadMeta() {
  const result = await fetchFavoriteList({ uid: props.uid, current: 1, size: 50 }).catch(() => null)
  const folders = result?.records ?? []
  folder.value = folders.find((item) => item.fid === props.fid) ?? null
  targets.value = folders.filter((item) => item.fid !== props.fid)
}

function loadMore() {
  void load()
}

function toggleOne(vid: number) {
  selected.value = selected.value.includes(vid)
    ? selected.value.filter((item) => item !== vid)
    : [...selected.value, vid]
}

function toggleAll(value: boolean | string | number) {
  selected.value = value ? list.value.map((item) => item.vid) : []
}

function openTransfer(next: TransferMode) {
  mode.value = next
  targetFid.value = undefined
  dialogVisible.value = true
}

/** 批量操作：取消收藏 / 移动 / 复制 */
async function submitTransfer() {
  if (mode.value !== 'uncollect' && !targetFid.value) {
    ElMessage.warning('请选择目标收藏夹')
    return
  }
  busy.value = true
  const payload = { vids: [...selected.value], preFid: props.fid, newFid: targetFid.value ?? 0 }
  try {
    if (mode.value === 'uncollect') await uncollectVideos(payload)
    if (mode.value === 'move') await batchMove(payload)
    if (mode.value === 'copy') await batchCopy(payload)
    dialogVisible.value = false
    ElMessage.success('操作成功')
    await load(true)
    await loadMeta()
  } finally {
    busy.value = false
  }
}

watch(() => props.fid, () => {
  void load(true)
  void loadMeta()
})

onMounted(() => {
  void load(true)
  void loadMeta()
})
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.fav-detail {
  &__head {
    display: flex;
    align-items: baseline;
    gap: 12px;
    margin-bottom: 12px;
  }

  &__back {
    border: none;
    background: transparent;
    color: var(--link);
    font-size: t.$fs-xs;
  }

  &__title {
    font-size: t.$fs-md;
    color: var(--text-1);
  }

  &__count {
    font-size: t.$fs-xs;
    color: var(--text-3);
  }

  &__ops {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 8px 12px;
    margin-bottom: 12px;
    background: #fff;
    border-radius: var(--radius);
    font-size: t.$fs-xs;
  }

  &__selected {
    color: var(--text-3);
    margin-right: auto;
  }

  &__grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20px 16px;
  }

  &__more {
    display: block;
    width: 100%;
    margin-top: 20px;
    padding: 10px 0;
    border: 1px solid var(--line);
    border-radius: var(--radius);
    background: #fff;
    color: var(--text-2);
    font-size: t.$fs-sm;
  }

  &__select {
    width: 100%;
  }

  &__tip {
    font-size: t.$fs-sm;
    color: var(--text-2);
  }
}

.cell {
  position: relative;

  &.is-selected {
    outline: 2px solid var(--brand);
    outline-offset: 2px;
    border-radius: var(--radius-lg);
  }

  &__check {
    position: absolute;
    top: 6px;
    left: 6px;
    z-index: 2;
    padding: 0 4px;
    border-radius: 4px;
    background: rgba(255, 255, 255, 0.85);
  }
}
</style>
