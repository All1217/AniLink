<template>
  <div class="user-view">
    <div v-if="user" class="user-view__head">
      <ProfileCard
        :user="user"
        :stats="stats"
        :is-self="isSelf"
        :is-follow="isFollow"
        :busy="followBusy"
        @edit="router.push('/account')"
        @toggle-follow="toggleFollow"
      />

      <el-tabs v-model="tab" class="user-view__tabs" @tab-change="onTabChange">
        <el-tab-pane label="投稿" name="submit" />
        <el-tab-pane label="收藏夹" name="fav" />
        <el-tab-pane label="粉丝" name="fans" />
      </el-tabs>
    </div>

    <div v-if="user" class="user-view__body">
      <SubmitList v-if="tab === 'submit'" :uid="user.uid" :is-self="isSelf" />

      <template v-else-if="tab === 'fav'">
        <FavDetail
          v-if="openFolder"
          :fid="openFolder.fid"
          :uid="user.uid"
          :is-self="isSelf"
          @back="openFolder = null"
        />
        <FavFolders v-else :uid="user.uid" :is-self="isSelf" @open="openFolder = $event" />
      </template>

      <FollowList v-else :uid="user.uid" :is-self="isSelf" />
    </div>

    <EmptyState v-else-if="!loading" text="用户不存在" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ProfileCard from '@/components/user/ProfileCard.vue'
import SubmitList from '@/components/user/SubmitList.vue'
import FavFolders from '@/components/user/FavFolders.vue'
import FavDetail from '@/components/user/FavDetail.vue'
import FollowList from '@/components/user/FollowList.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import { useUserStore } from '@/stores/user'
import { fetchUserById, fetchUserInfo, fetchUserStats, fetchUserStatsPublic } from '@/api/user'
import { fetchFollowState, followUser, unfollowUser } from '@/api/interaction'
import type { Favorite, User, UserStats } from '@/types'
import { toNumber } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const user = ref<User | null>(null)
const stats = ref<UserStats | null>(null)
const loading = ref(false)
const tab = ref<'submit' | 'fav' | 'fans'>('submit')
const openFolder = ref<Favorite | null>(null)
const isFollow = ref(false)
const followBusy = ref(false)

const uid = computed(() => toNumber(route.params.uid))
const isSelf = computed(() => userStore.isLogin && userStore.uid === uid.value)

/** 自己用登录态接口，别人用公开接口；关注态仅登录且非自己时查询 */
async function load() {
  const target = uid.value
  if (!target) {
    user.value = null
    return
  }
  loading.value = true
  user.value = null
  stats.value = null
  try {
    const [info, statsData] = isSelf.value
      ? await Promise.all([
          fetchUserInfo({ uid: target }).catch(() => null),
          fetchUserStats({ uid: target }).catch(() => null),
        ])
      : await Promise.all([
          fetchUserById({ uid: target }).catch(() => null),
          fetchUserStatsPublic({ uid: target }).catch(() => null),
        ])
    user.value = info
    stats.value = statsData
    if (!isSelf.value && userStore.isLogin) {
      const follow = await fetchFollowState({ followerId: userStore.uid, followeeId: target }).catch(() => null)
      isFollow.value = Boolean(follow?.id)
    }
  } finally {
    loading.value = false
  }
}

async function toggleFollow() {
  if (!userStore.isLogin) {
    userStore.openLoginDialog()
    return
  }
  if (!user.value) return
  followBusy.value = true
  const payload = { followerId: userStore.uid, followeeId: user.value.uid }
  try {
    if (isFollow.value) {
      await unfollowUser(payload)
      isFollow.value = false
      if (stats.value) stats.value.fansCount = Math.max(0, stats.value.fansCount - 1)
    } else {
      await followUser(payload)
      isFollow.value = true
      if (stats.value) stats.value.fansCount += 1
    }
  } finally {
    followBusy.value = false
  }
}

function onTabChange() {
  openFolder.value = null
}

watch(
  () => route.params.uid,
  () => {
    tab.value = 'submit'
    openFolder.value = null
    void load()
  },
  { immediate: true },
)
watch(() => userStore.isLogin, () => void load())
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.user-view {
  width: t.$page-width;
  margin: 0 auto;
  padding-top: 16px;

  &__head {
    background: #fff;
    border-radius: var(--radius-lg);
    padding-bottom: 4px;
  }

  &__tabs {
    padding: 0 24px;
  }

  &__body {
    margin-top: 16px;
  }
}
</style>
