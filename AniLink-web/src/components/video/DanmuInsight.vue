<template>
  <section class="insight">
    <header class="insight__head">
      <h2 class="insight__title">弹幕画像</h2>
      <button v-if="activeTag || activeUser" class="insight__reset" @click="reset">取消筛选</button>
    </header>

    <div v-if="!userStore.isLogin" class="insight__gate">
      <span class="insight__empty">登录后可查看弹幕画像（标签热度 / 相近用户 / 标签筛选）</span>
      <button class="insight__login" @click="userStore.openLoginDialog()">去登录</button>
    </div>

    <div v-if="userStore.isLogin" class="insight__block">
      <span class="insight__label">视频标签热度</span>
      <TagRankBars :tags="tags" :active-tag="activeTag" @pick="pickTag" />
    </div>

    <div v-if="userStore.isLogin" class="insight__block">
      <span class="insight__label">和你标签相近的用户</span>
      <div v-if="users.length > 0" class="users">
        <button
          v-for="user in users"
          :key="user.uid"
          :class="['users__item', { 'is-active': activeUser === user.uid }]"
          @click="pickUser(user.uid)"
        >
          <el-avatar :size="28" :src="user.avatar || undefined">{{ user.nickname?.[0] ?? 'U' }}</el-avatar>
          <span class="users__name">{{ user.nickname }}</span>
        </button>
      </div>
      <p v-else class="insight__empty">暂无相近用户</p>
    </div>

    <div v-if="commonTags.length > 0" class="insight__block">
      <span class="insight__label">共同标签</span>
      <div class="chips">
        <span v-for="tag in commonTags" :key="tag" class="chips__item">{{ tag }}</span>
      </div>
    </div>

    <div v-if="filtered.length > 0" class="insight__block">
      <span class="insight__label">筛选出的弹幕 · {{ filtered.length }} 条</span>
      <ul class="dm">
        <li v-for="(item, index) in filtered.slice(0, 12)" :key="index" class="dm__row">
          <span class="dm__time">{{ formatDuration(item.timePoint) }}</span>
          <span class="dm__text">{{ item.content }}</span>
        </li>
      </ul>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import TagRankBars from './TagRankBars.vue'
import { useUserStore } from '@/stores/user'
import { filterDanmuByTags, fetchCommonTags, fetchSimilarUsers, fetchTagRank, type TagRank } from '@/api/danmu'
import type { Danmu, RecoQueryVo, User } from '@/types'
import { formatDuration } from '@/utils/format'

const props = defineProps<{ vid: number; danmuList: Danmu[] }>()
const userStore = useUserStore()

const tags = ref<TagRank[]>([])
const users = ref<User[]>([])
const commonTags = ref<string[]>([])
const filtered = ref<Danmu[]>([])
const activeTag = ref('')
const activeUser = ref(0)

/** 标签热度：接口需要登录，未登录跳过 */
async function loadTags() {
  if (!userStore.isLogin) return
  tags.value = (await fetchTagRank(props.vid).catch(() => [])) ?? []
}

/** 相似用户：需要登录，未登录跳过 */
async function loadUsers() {
  if (!userStore.isLogin) return
  const query: RecoQueryVo = { uid: userStore.uid, vid: props.vid }
  users.value = (await fetchSimilarUsers(query).catch(() => [])) ?? []
}

/** 选标签 → 后端按标签筛弹幕 */
async function pickTag(tagName: string) {
  activeTag.value = tagName
  activeUser.value = 0
  commonTags.value = []
  const query: RecoQueryVo = { vid: props.vid, tag: 4, tagName }
  filtered.value = (await filterDanmuByTags(query).catch(() => [])) ?? []
}

/** 选用户 → 看共同标签 + 只看 TA 的弹幕（本地过滤） */
async function pickUser(uid: number) {
  activeUser.value = uid
  activeTag.value = ''
  // 后端契约：vid 传当前用户 uid、uid 传目标用户（沿用旧调用方式）
  const query: RecoQueryVo = { vid: userStore.uid, uid }
  commonTags.value = (await fetchCommonTags(query).catch(() => [])) ?? []
  filtered.value = props.danmuList.filter((item) => item.uid === uid)
}

function reset() {
  activeTag.value = ''
  activeUser.value = 0
  commonTags.value = []
  filtered.value = []
}

watch(() => props.vid, loadTags)
onMounted(() => {
  void loadTags()
  void loadUsers()
})
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.insight {
  margin-top: 16px;
  padding: 16px 20px;
  background: #fff;
  border-radius: var(--radius-lg);

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__title {
    font-size: t.$fs-md;
    color: var(--text-1);
  }

  &__reset {
    border: none;
    background: transparent;
    color: var(--link);
    font-size: t.$fs-xs;
  }

  &__block + &__block {
    margin-top: 14px;
  }

  &__label {
    display: block;
    margin-bottom: 8px;
    font-size: t.$fs-xs;
    color: var(--text-3);
  }

  &__empty {
    font-size: t.$fs-xs;
    color: var(--text-3);
  }

  &__gate {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 12px;
    padding: 10px 12px;
    border-radius: var(--radius);
    background: var(--bg-hover);
  }

  &__login {
    border: none;
    background: transparent;
    color: var(--brand);
    font-size: t.$fs-xs;
  }
}

.users {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  &__item {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 4px 10px 4px 4px;
    border: 1px solid var(--line);
    border-radius: 16px;
    background: #fff;
    font-size: t.$fs-xs;
    color: var(--text-2);

    &.is-active {
      border-color: var(--brand);
      color: var(--brand);
    }
  }
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;

  &__item {
    padding: 2px 10px;
    border-radius: 10px;
    background: var(--brand-soft, #fff1f5);
    color: var(--brand);
    font-size: t.$fs-xs;
  }
}

.dm {
  display: flex;
  flex-direction: column;
  gap: 4px;

  &__row {
    display: flex;
    gap: 8px;
    font-size: t.$fs-xs;
  }

  &__time {
    width: 40px;
    color: var(--text-3);
  }

  &__text {
    flex: 1;
    color: var(--text-1);
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }
}
</style>
