<template>
  <div class="actions">
    <button class="actions__btn" :class="{ 'is-on': store.liked }" @click="onLike">
      <el-icon><Pointer /></el-icon>
      <span>{{ formatCount(store.stats?.good) }}</span>
    </button>

    <button class="actions__btn" :class="{ 'is-on': store.coined }" @click="coinVisible = true">
      <el-icon><Coin /></el-icon>
      <span>{{ formatCount(store.stats?.coin) }}</span>
    </button>

    <button class="actions__btn" :class="{ 'is-on': store.collected }" @click="openCollect">
      <el-icon><Star /></el-icon>
      <span>{{ formatCount(store.stats?.collect) }}</span>
    </button>

    <button class="actions__btn" @click="onShare">
      <el-icon><Share /></el-icon>
      <span>分享</span>
    </button>

    <!-- 投币 -->
    <el-dialog v-model="coinVisible" title="给 UP 主投币" width="320px">
      <div class="coin">
        <button
          v-for="item in [1, 2]"
          :key="item"
          :class="['coin__item', { 'is-active': coinCount === item }]"
          @click="coinCount = item"
        >
          <el-icon><Coin /></el-icon>
          {{ item }} 枚
        </button>
      </div>
      <template #footer>
        <el-button @click="coinVisible = false">取消</el-button>
        <el-button type="primary" :loading="busy" @click="onCoin">确定</el-button>
      </template>
    </el-dialog>

    <!-- 收藏 -->
    <el-dialog v-model="collectVisible" title="添加到收藏夹" width="360px">
      <el-empty v-if="folders.length === 0" description="还没有收藏夹，去个人空间新建一个" :image-size="60" />
      <el-checkbox-group v-model="selectedFids" class="folders">
        <el-checkbox v-for="folder in folders" :key="folder.fid" :value="folder.fid">
          {{ folder.title }}（{{ folder.count }}）
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="collectVisible = false">取消</el-button>
        <el-button type="primary" :loading="busy" @click="onCollect">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { Coin, Pointer, Share, Star } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { usePlayerStore } from '@/stores/player'
import { useUserStore } from '@/stores/user'
import { coinVideo, fetchCoinState, fetchCollectState, fetchLikeState, likeVideo } from '@/api/interaction'
import { collectVideo, fetchFavoriteIds, fetchFavoriteList } from '@/api/favorite'
import { Interaction, type Favorite, type Video } from '@/types'
import { formatCount } from '@/utils/format'

const props = defineProps<{ video: Video }>()
const store = usePlayerStore()
const userStore = useUserStore()

const busy = ref(false)
const coinVisible = ref(false)
const coinCount = ref(1)
const collectVisible = ref(false)
const folders = ref<Favorite[]>([])
const selectedFids = ref<number[]>([])

/** 进页面时的既有交互态（未登录不请求，避免 305 噪音） */
async function loadStates() {
  if (!userStore.isLogin || !props.video.vid) return
  const query = { uid: userStore.uid, vid: props.video.vid }
  const [likeState, coinState, collectState] = await Promise.all([
    fetchLikeState(props.video.vid).catch(() => 0),
    fetchCoinState(query).catch(() => null),
    fetchCollectState(query).catch(() => null),
  ])
  store.interaction.love = likeState ?? 0
  store.interaction.coin = coinState?.coin ?? 0
  store.interaction.collect = collectState?.collect ?? 0
}

watch(() => props.video.vid, loadStates)
onMounted(loadStates)

function requireLogin(): boolean {
  if (userStore.isLogin) return true
  userStore.openLoginDialog()
  return false
}

/** 点赞：乐观更新，失败回滚 */
async function onLike() {
  if (!requireLogin()) return
  const next = !store.liked
  store.optimisticLike(next)
  try {
    await likeVideo({ bizId: props.video.vid, bizType: 0, liked: next })
  } catch {
    store.optimisticLike(!next)
  }
}

/** 投币：以后端返回的累计数覆盖本地 */
async function onCoin() {
  if (!requireLogin()) return
  busy.value = true
  try {
    const result = await coinVideo({
      uid: userStore.uid,
      vid: props.video.vid,
      actionType: coinCount.value === 1 ? Interaction.COIN_ONE : Interaction.COIN_TWO,
    })
    store.optimisticCoin(result?.coin ?? coinCount.value)
    coinVisible.value = false
    ElMessage.success('投币成功')
  } finally {
    busy.value = false
  }
}

/** 打开收藏弹窗：拉收藏夹 + 已收藏关系 */
async function openCollect() {
  if (!requireLogin()) return
  collectVisible.value = true
  const [list, ids] = await Promise.all([
    fetchFavoriteList({ uid: userStore.uid, current: 1, size: 50 }).catch(() => null),
    fetchFavoriteIds(userStore.uid, props.video.vid).catch(() => [] as number[]),
  ])
  folders.value = list?.records ?? []
  selectedFids.value = ids ?? []
}

/** 提交收藏：fids 为空即全部取消 */
async function onCollect() {
  busy.value = true
  try {
    await collectVideo({ uid: userStore.uid, vid: props.video.vid, fids: [...selectedFids.value] })
    store.setCollected(selectedFids.value.length > 0)
    collectVisible.value = false
    ElMessage.success(selectedFids.value.length > 0 ? '已收藏' : '已取消收藏')
  } finally {
    busy.value = false
  }
}

async function onShare() {
  const url = `${location.origin}/#/video/${props.video.vid}`
  try {
    await navigator.clipboard.writeText(url)
    ElMessage.success('链接已复制')
  } catch {
    ElMessage.info(url)
  }
}
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.actions {
  display: flex;
  align-items: center;
  gap: 8px;

  &__btn {
    display: flex;
    align-items: center;
    gap: 4px;
    height: 32px;
    padding: 0 12px;
    border: none;
    border-radius: var(--radius);
    background: transparent;
    color: var(--text-2);
    font-size: t.$fs-sm;

    &:hover {
      background: var(--bg-hover);
    }

    &.is-on {
      color: var(--brand);
    }
  }
}

.coin {
  display: flex;
  gap: 12px;

  &__item {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    height: 48px;
    border: 1px solid var(--line);
    border-radius: var(--radius);
    background: #fff;
    color: var(--text-2);

    &.is-active {
      border-color: var(--brand);
      color: var(--brand);
      background: var(--brand-soft, #fff1f5);
    }
  }
}

.folders {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 260px;
  overflow-y: auto;
}
</style>
