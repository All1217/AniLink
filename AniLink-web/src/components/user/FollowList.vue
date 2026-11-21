<template>
  <div class="follow">
    <div class="follow__tabs">
      <span :class="['follow__tab', { 'is-active': tab === 'follow' }]" @click="switchTab('follow')">
        关注 {{ followTotal }}
      </span>
      <span :class="['follow__tab', { 'is-active': tab === 'fans' }]" @click="switchTab('fans')">
        粉丝 {{ fansTotal }}
      </span>
    </div>

    <ul v-if="rows.length > 0" class="follow__list">
      <li v-for="user in rows" :key="user.uid" class="row">
        <RouterLink :to="`/user/${user.uid}`">
          <el-avatar :size="44" :src="user.avatar || undefined">{{ user.nickname?.[0] ?? 'U' }}</el-avatar>
        </RouterLink>
        <div class="row__info">
          <RouterLink :to="`/user/${user.uid}`" class="row__name">{{ user.nickname }}</RouterLink>
          <span class="row__bio">{{ user.description || '这个人很神秘' }}</span>
        </div>
        <button
          v-if="!isSelf && user.uid !== userStore.uid"
          class="row__follow"
          :class="{ 'is-following': followed.has(user.uid) }"
          @click="toggle(user.uid)"
        >
          {{ followed.has(user.uid) ? '已关注' : '+ 关注' }}
        </button>
      </li>
    </ul>

    <EmptyState v-else-if="!loading" :text="tab === 'follow' ? '还没有关注任何人' : '还没有粉丝'" />

    <el-pagination
      v-if="total > PAGE"
      :current-page="page"
      :page-size="PAGE"
      :total="total"
      layout="prev, pager, next"
      @current-change="onPageChange"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import EmptyState from '@/components/base/EmptyState.vue'
import { useUserStore } from '@/stores/user'
import { fetchFansList, fetchFollowList } from '@/api/user'
import { followUser, unfollowUser } from '@/api/interaction'
import type { User } from '@/types'

const PAGE = 15

const props = defineProps<{ uid: number; isSelf: boolean }>()
const userStore = useUserStore()

const tab = ref<'follow' | 'fans'>('follow')
const rows = ref<User[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const followed = ref<Set<number>>(new Set())
const followTotal = ref(0)
const fansTotal = ref(0)

/** 关注列表与粉丝列表都按 followerId/followeeId 约定查询（-1 表示不限制） */
async function load() {
  if (!props.uid) return
  loading.value = true
  try {
    const isFollowTab = tab.value === 'follow'
    const query = {
      followerId: isFollowTab ? props.uid : -1,
      followeeId: isFollowTab ? -1 : props.uid,
      current: page.value,
      size: PAGE,
      sortField: 'created_at',
    }
    const result = isFollowTab ? await fetchFollowList(query) : await fetchFansList(query)
    rows.value = result.records ?? []
    total.value = result.total ?? rows.value.length
    if (isFollowTab) followTotal.value = total.value
    else fansTotal.value = total.value
    // 关注列表天然是"已关注"
    if (isFollowTab) followed.value = new Set(rows.value.map((item) => item.uid))
  } finally {
    loading.value = false
  }
}

function switchTab(next: 'follow' | 'fans') {
  if (tab.value === next) return
  tab.value = next
  page.value = 1
  void load()
}

function onPageChange(next: number) {
  page.value = next
  void load()
}

/** 关注/取关：本地先翻状态，失败回滚到操作前的集合 */
async function toggle(uid: number) {
  if (!userStore.isLogin) {
    userStore.openLoginDialog()
    return
  }
  const payload = { followerId: userStore.uid, followeeId: uid }
  const before = followed.value
  const isFollowing = before.has(uid)
  const next = new Set(before)
  if (isFollowing) next.delete(uid)
  else next.add(uid)
  followed.value = next
  try {
    if (isFollowing) await unfollowUser(payload)
    else await followUser(payload)
  } catch {
    followed.value = before
    ElMessage.error('操作失败')
  }
}

watch(() => props.uid, () => {
  page.value = 1
  void load()
})
onMounted(() => {
  followTotal.value = 0
  fansTotal.value = 0
  void load()
})
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;
@use '@/styles/mixins' as m;

.follow {
  &__tabs {
    display: flex;
    gap: 20px;
    margin-bottom: 12px;
    border-bottom: 1px solid var(--line);
  }

  &__tab {
    padding: 8px 0;
    font-size: t.$fs-sm;
    color: var(--text-3);
    cursor: pointer;
    border-bottom: 2px solid transparent;

    &.is-active {
      color: var(--brand);
      border-bottom-color: var(--brand);
    }
  }

  &__list {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
}

.row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #fff;
  border-radius: var(--radius);

  &__info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
  }

  &__name {
    font-size: t.$fs-sm;
    color: var(--text-1);

    &:hover {
      color: var(--brand);
    }
  }

  &__bio {
    font-size: t.$fs-xs;
    color: var(--text-3);

    @include m.ellipsis-1;
  }

  &__follow {
    height: 28px;
    padding: 0 12px;
    border: 1px solid var(--brand);
    border-radius: var(--radius);
    background: var(--brand);
    color: #fff;
    font-size: t.$fs-xs;

    &.is-following {
      background: transparent;
      color: var(--text-2);
      border-color: var(--line);
    }
  }
}
</style>
