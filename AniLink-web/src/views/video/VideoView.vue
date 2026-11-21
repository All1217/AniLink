<template>
  <div v-if="video" class="video-view">
    <div class="video-view__main">
      <BiliPlayer :video="video" :danmu-list="danmuList" :uid="userStore.uid" @played="onPlayed" />
      <VideoMeta :video="video" :stats="stats" />
      <DanmuInsight :vid="video.vid" :danmu-list="danmuList" />
      <CommentSection :vid="video.vid" :count="stats?.comment" />
    </div>

    <aside class="video-view__side">
      <UpCard
        v-if="author"
        :author="author"
        :fans-count="authorStats?.fansCount ?? 0"
        :is-follow="isFollow"
        :busy="followBusy"
        @toggle="toggleFollow"
      />
      <RelatedPanel :vid="video.vid" />
    </aside>
  </div>

  <div v-else-if="!loading" class="video-view__empty">
    <EmptyState text="视频不存在或已下架">
      <el-button @click="router.push('/home')">回到首页</el-button>
    </EmptyState>
  </div>

  <div v-else class="video-view__loading">
    <div class="video-view__skeleton" />
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BiliPlayer from '@/components/video/BiliPlayer.vue'
import VideoMeta from '@/components/video/VideoMeta.vue'
import DanmuInsight from '@/components/video/DanmuInsight.vue'
import CommentSection from '@/components/video/CommentSection.vue'
import RelatedPanel from '@/components/video/RelatedPanel.vue'
import UpCard from '@/components/video/UpCard.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import { useUserStore } from '@/stores/user'
import { usePlayerStore } from '@/stores/player'
import { fetchUserById, fetchUserStatsPublic } from '@/api/user'
import { fetchVideo, fetchVideoStats } from '@/api/video'
import { fetchDanmuList } from '@/api/danmu'
import { fetchFollowState, followUser, unfollowUser } from '@/api/interaction'
import type { Danmu, User, UserStats, Video, VideoStats } from '@/types'
import { toNumber } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const store = usePlayerStore()

const video = ref<Video | null>(null)
const stats = ref<VideoStats | null>(null)
const author = ref<User | null>(null)
const authorStats = ref<UserStats | null>(null)
const danmuList = ref<Danmu[]>([])
const loading = ref(false)
const isFollow = ref(false)
const followBusy = ref(false)

/** 加载页面所需数据；视频本体失败则展示空状态 */
async function load() {
  const vid = toNumber(route.params.vid)
  if (!vid) {
    video.value = null
    return
  }
  loading.value = true
  video.value = null
  store.init(vid)
  try {
    const [detail, statsData, danmu] = await Promise.all([
      fetchVideo(vid),
      fetchVideoStats(vid).catch(() => null),
      fetchDanmuList(vid).catch(() => [] as Danmu[]),
    ])
    if (!detail) return
    video.value = detail
    stats.value = statsData
    store.setStats(statsData)
    danmuList.value = danmu ?? []
    await loadAuthor(detail.uid)
  } finally {
    loading.value = false
  }
}

/** 作者信息与关注状态（关注态需要登录） */
async function loadAuthor(uid: number) {
  const [user, userStats] = await Promise.all([
    fetchUserById({ uid }).catch(() => null),
    fetchUserStatsPublic({ uid }).catch(() => null),
  ])
  author.value = user
  authorStats.value = userStats
  if (!userStore.isLogin || !uid) return
  const follow = await fetchFollowState({ followerId: userStore.uid, followeeId: uid }).catch(() => null)
  isFollow.value = Boolean(follow?.id)
}

async function toggleFollow() {
  if (!userStore.isLogin) {
    userStore.openLoginDialog()
    return
  }
  if (!author.value) return
  followBusy.value = true
  try {
    const payload = { followerId: userStore.uid, followeeId: author.value.uid }
    if (isFollow.value) {
      await unfollowUser(payload)
      isFollow.value = false
      if (authorStats.value) authorStats.value.fansCount = Math.max(0, authorStats.value.fansCount - 1)
    } else {
      await followUser(payload)
      isFollow.value = true
      if (authorStats.value) authorStats.value.fansCount += 1
    }
  } finally {
    followBusy.value = false
  }
}

// 首帧播放后本地 +1，与后端异步累计保持一致
function onPlayed() {
  if (!stats.value) return
  stats.value.play += 1
  if (store.stats) store.stats.play += 1
}

watch(() => route.params.vid, load, { immediate: true })
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.video-view {
  display: flex;
  gap: 20px;
  width: t.$page-width;
  margin: 0 auto;
  padding-top: 16px;

  &__main {
    flex: 1;
    min-width: 0;
  }

  &__side {
    flex: 0 0 320px;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  &__loading {
    width: t.$page-width;
    margin: 16px auto;
  }

  &__skeleton {
    height: 476px;
    border-radius: var(--radius-lg);
    background: linear-gradient(90deg, #e9eaec 25%, #f4f5f6 37%, #e9eaec 63%);
    background-size: 400% 100%;
    animation: shimmer 1.2s infinite;
  }

  &__empty {
    padding-top: 80px;
  }
}

@keyframes shimmer {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}
</style>
