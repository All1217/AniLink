<template>
  <section class="dp">
    <header class="dp__head" @click="expand = !expand">
      <span class="dp__arrow" :class="{ 'is-open': expand }">›</span>
      <span class="dp__title">弹幕列表</span>
      <span class="dp__count">{{ displayList.length }}</span>
      <span v-if="modeLabel" class="dp__chip">{{ modeLabel }}</span>
    </header>

    <div v-if="expand" class="dp__body">
      <DanmuFilterActions
        :is-login="userStore.isLogin"
        :mode="mode"
        :has-users="users.length > 0"
        :loading-users="usersLoading"
        @code="filterByCode"
        @reset="resetFilter"
        @load-users="loadUsers"
        @login="userStore.openLoginDialog()"
      />

      <DanmuList :items="displayList" :empty-text="emptyText" />

      <DanmuInsightBlock
        v-if="userStore.isLogin"
        :tags="tags"
        :active-tag="activeTag"
        :users="users"
        :common-tags="commonTags"
        :show-empty="usersSearched && users.length === 0"
        @pick-tag="pickTag"
        @pick-user="pickUser"
      />
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import DanmuList from './DanmuList.vue'
import DanmuFilterActions from './DanmuFilterActions.vue'
import DanmuInsightBlock from './DanmuInsightBlock.vue'
import { useUserStore } from '@/stores/user'
import { fetchSimilarUsers, fetchTagRank, fetchCommonTags, filterDanmuByTags, type TagRank } from '@/api/danmu'
import type { Danmu, RecoQueryVo, User } from '@/types'

/** 弹幕筛选模式：与后端 TagType 对齐（0 高质量 / 1 干货 / 4 通用标签），2 默认、3 指定用户 */
type FilterMode = 2 | 0 | 1 | 3 | 4
const MODE_DEFAULT = 2
const MODE_USER = 3
const MODE_TAG = 4

const props = defineProps<{ vid: number; danmuList: Danmu[] }>()
const userStore = useUserStore()

const expand = ref(true)
const mode = ref<FilterMode>(MODE_DEFAULT)
const serverList = ref<Danmu[]>([])
const tags = ref<TagRank[]>([])
const users = ref<User[]>([])
const commonTags = ref<string[]>([])
const activeTag = ref('')
const activeUser = ref<User | null>(null)
const usersLoading = ref(false)
const usersSearched = ref(false)

/** 当前展示的弹幕：默认模式用播放器同一份数据，筛选模式用接口结果或本地过滤 */
const displayList = computed<Danmu[]>(() => {
  if (mode.value === MODE_DEFAULT) return props.danmuList
  if (mode.value === MODE_USER) return props.danmuList.filter((item) => item.uid === activeUser.value?.uid)
  return serverList.value
})

const modeLabel = computed(() => {
  if (mode.value === 0) return '高质量贡献者优先'
  if (mode.value === 1) return '干货贡献者优先'
  if (mode.value === MODE_USER) return activeUser.value?.nickname ?? '指定用户'
  if (mode.value === MODE_TAG) return activeTag.value
  return ''
})

const emptyText = computed(() => {
  if (mode.value === MODE_DEFAULT) return '这个视频还没有弹幕'
  if (mode.value === MODE_USER) return '该用户还没有在这个视频发过弹幕'
  return '没有符合筛选条件的弹幕'
})

/** 标签热度：接口需要登录 */
async function loadTags() {
  if (!userStore.isLogin) return
  tags.value = (await fetchTagRank(props.vid).catch(() => [])) ?? []
}

/** 相近用户：接口需要登录，按需触发 */
async function loadUsers() {
  if (!userStore.isLogin || usersLoading.value) return
  usersLoading.value = true
  try {
    const query: RecoQueryVo = { uid: userStore.uid, vid: props.vid }
    users.value = (await fetchSimilarUsers(query).catch(() => [])) ?? []
    usersSearched.value = true
  } finally {
    usersLoading.value = false
  }
}

/** 按标签代码筛选（0 高质量 / 1 干货） */
async function filterByCode(code: 0 | 1) {
  const query: RecoQueryVo = { vid: props.vid, tag: code }
  serverList.value = (await filterDanmuByTags(query).catch(() => [])) ?? []
  mode.value = code
  activeTag.value = ''
  activeUser.value = null
}

/** 点标签热度 → 按标签名筛选（tag=4 表示通用标签） */
async function pickTag(tagName: string) {
  if (mode.value === MODE_TAG && activeTag.value === tagName) {
    resetFilter()
    return
  }
  const query: RecoQueryVo = { vid: props.vid, tag: MODE_TAG, tagName }
  serverList.value = (await filterDanmuByTags(query).catch(() => [])) ?? []
  mode.value = MODE_TAG
  activeTag.value = tagName
  activeUser.value = null
}

/** 选相近用户 → 本地只看 TA 的弹幕，同时拉共同标签 */
async function pickUser(user: User) {
  activeUser.value = user
  activeTag.value = ''
  mode.value = MODE_USER
  // 后端契约：vid 传当前用户 uid、uid 传目标用户（沿用旧版调用方式）
  const query: RecoQueryVo = { vid: userStore.uid, uid: user.uid }
  commonTags.value = (await fetchCommonTags(query).catch(() => [])) ?? []
}

function resetFilter() {
  mode.value = MODE_DEFAULT
  serverList.value = []
  activeTag.value = ''
  activeUser.value = null
  commonTags.value = []
}

onMounted(loadTags)

watch(
  () => props.vid,
  () => {
    resetFilter()
    users.value = []
    usersSearched.value = false
    tags.value = []
    void loadTags()
  },
)

// 登录状态变化后补拉标签热度（登录前接口会 305）
watch(
  () => userStore.isLogin,
  (value) => {
    if (value) void loadTags()
  },
)
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.dp {
  padding: 10px 12px 12px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);

  &__head {
    display: flex;
    align-items: center;
    gap: 6px;
    cursor: pointer;
    user-select: none;
  }

  &__arrow {
    display: inline-block;
    font-size: 16px;
    line-height: 1;
    color: var(--text-3);
    transition: transform 0.15s;

    &.is-open {
      transform: rotate(90deg);
    }
  }

  &__title {
    font-size: t.$fs-sm;
    font-weight: 600;
    color: var(--text-1);
  }

  &__count {
    font-size: t.$fs-xs;
    color: var(--text-3);
  }

  &__chip {
    margin-left: auto;
    padding: 1px 8px;
    border-radius: 9px;
    background: var(--brand-soft, #fff1f5);
    color: var(--brand);
    font-size: 11px;
  }

  &__body {
    margin-top: 8px;
  }
}
</style>
