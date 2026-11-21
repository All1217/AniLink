<template>
  <section class="comments">
    <header class="comments__head">
      <h2 class="comments__title">评论 <em>{{ total }}</em></h2>
      <div class="comments__sort">
        <span
          v-for="item in sorts"
          :key="item.value"
          :class="['comments__sort-item', { 'is-active': sort === item.value }]"
          @click="changeSort(item.value)"
        >
          {{ item.label }}
        </span>
      </div>
    </header>

    <CommentEditor :vid="vid" @submitted="reload" />

    <div v-if="!userStore.isLogin" class="comments__gate">
      <EmptyState text="登录后查看评论">
        <el-button type="primary" size="small" @click="userStore.openLoginDialog()">去登录</el-button>
      </EmptyState>
    </div>

    <div v-else-if="list.length > 0" class="comments__list">
      <CommentItem v-for="item in list" :key="item.id" :comment="item" :vid="vid" @deleted="reload" />
    </div>

    <EmptyState v-else-if="!loading" text="还没有人评论，来抢沙发吧" />

    <button v-if="userStore.isLogin && list.length < total" class="comments__more" :disabled="loading" @click="loadMore">
      {{ loading ? '加载中…' : '加载更多评论' }}
    </button>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import CommentEditor from './CommentEditor.vue'
import CommentItem from './CommentItem.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import { useUserStore } from '@/stores/user'
import { CommentFilter, fetchRootComments } from '@/api/comment'
import type { CommentVo } from '@/types'

const props = defineProps<{ vid: number; count?: number }>()
const userStore = useUserStore()

const sorts = [
  { label: '最新', value: CommentFilter.LATEST },
  { label: '最热', value: CommentFilter.HOTTEST },
]

const list = ref<CommentVo[]>([])
const total = ref(props.count ?? 0)
const sort = ref<CommentFilter>(CommentFilter.LATEST)
const page = ref(1)
const loading = ref(false)

/** 拉一页根评论；reset 表示从头开始。评论接口需要登录，未登录直接跳过 */
async function load(reset = false) {
  if (loading.value || !userStore.isLogin) return
  loading.value = true
  if (reset) {
    list.value = []
    page.value = 1
  }
  try {
    const result = await fetchRootComments({
      vid: props.vid,
      current: page.value,
      size: 20,
      sortField: sort.value === CommentFilter.HOTTEST ? 'love' : 'createTime',
    })
    list.value = [...list.value, ...(result.records ?? [])]
    total.value = result.total ?? list.value.length
    page.value += 1
  } catch {
    // 失败保持已有列表，不打断阅读
  } finally {
    loading.value = false
  }
}

function loadMore() {
  void load()
}

function reload() {
  void load(true)
}

function changeSort(value: CommentFilter) {
  if (sort.value === value) return
  sort.value = value
  reload()
}

watch(() => props.vid, () => load(true))
watch(
  () => userStore.isLogin,
  (value) => {
    if (value) void load(true)
  },
)
onMounted(() => load(true))
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.comments {
  margin-top: 24px;
  padding: 16px 20px;
  background: #fff;
  border-radius: var(--radius-lg);

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
  }

  &__title {
    font-size: t.$fs-md;
    color: var(--text-1);

    em {
      font-size: t.$fs-xs;
      font-style: normal;
      color: var(--text-3);
    }
  }

  &__sort {
    display: flex;
    gap: 12px;
    font-size: t.$fs-sm;
    color: var(--text-3);
  }

  &__sort-item {
    cursor: pointer;

    &.is-active {
      color: var(--brand);
    }
  }

  &__list {
    margin-top: 12px;
  }

  &__gate {
    padding: 8px 0;
  }

  &__more {
    display: block;
    width: 100%;
    margin-top: 16px;
    padding: 10px 0;
    border: 1px solid var(--line);
    border-radius: var(--radius);
    background: #fff;
    color: var(--text-2);
    font-size: t.$fs-sm;

    &:hover {
      border-color: var(--brand);
      color: var(--brand);
    }
  }
}
</style>
