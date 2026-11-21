<template>
  <div ref="boxRef" class="search">
    <div class="search__bar" :class="{ 'is-focus': visible }">
      <input
        v-model="keyword"
        class="search__input"
        type="text"
        maxlength="50"
        placeholder="搜索视频、UP 主"
        @focus="onFocus"
        @keydown.enter="onEnter"
      />
      <button class="search__btn" @click="onEnter">
        <el-icon><Search /></el-icon>
      </button>
    </div>

    <div v-if="visible" class="search__panel">
      <section v-if="history.length > 0" class="search__section">
        <header class="search__section-head">
          <span>搜索历史</span>
          <span class="search__clear" @click="searchStore.clear()">清空</span>
        </header>
        <div class="search__tags">
          <span v-for="item in history" :key="item.content" class="search__tag" @click="go(item.content)">
            {{ item.content }}
          </span>
        </div>
      </section>

      <section v-if="hot.length > 0" class="search__section">
        <header class="search__section-head"><span>当前热搜</span></header>
        <div class="search__hot">
          <span v-for="(item, index) in hot" :key="item.keyword" class="search__hot-item" @click="go(item.show_name)">
            <i class="search__hot-order" :class="{ 'is-top': index < 3 }">{{ index + 1 }}</i>
            <span class="search__hot-text">{{ item.show_name }}</span>
          </span>
        </div>
      </section>

      <p v-if="history.length === 0 && hot.length === 0" class="search__empty">输入关键词开始搜索</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { useSearchStore } from '@/stores/search'
import { useUserStore } from '@/stores/user'
import { fetchHotSearch, type HotSearch } from '@/api/stats'

const router = useRouter()
const searchStore = useSearchStore()
const userStore = useUserStore()
const keyword = ref('')
const visible = ref(false)
const hot = ref<HotSearch[]>([])
const boxRef = ref<HTMLElement | null>(null)
const history = computed(() => searchStore.history)

/** 首次聚焦时拉热搜；热搜接口需要登录，未登录静默跳过（失败也不打扰用户） */
async function loadHot() {
  if (!userStore.isLogin || hot.value.length > 0) return
  try {
    hot.value = (await fetchHotSearch())?.slice(0, 10) ?? []
  } catch {
    hot.value = []
  }
}

function onFocus() {
  visible.value = true
  void loadHot()
}

function onEnter() {
  go(keyword.value)
}

function go(value: string) {
  const text = value.trim()
  if (!text) return
  searchStore.add(text)
  keyword.value = text
  visible.value = false
  router.push({ path: '/search', query: { keyword: text } })
}

function onClickOutside(event: MouseEvent) {
  if (boxRef.value && !boxRef.value.contains(event.target as Node)) visible.value = false
}

onMounted(() => document.addEventListener('click', onClickOutside))
onBeforeUnmount(() => document.removeEventListener('click', onClickOutside))

// 登录完成后补拉热搜（未登录时跳过了）
watch(
  () => userStore.isLogin,
  (value) => {
    if (value) void loadHot()
  },
)
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;
@use '@/styles/mixins' as m;

.search {
  position: relative;

  &__bar {
    display: flex;
    align-items: center;
    height: 34px;
    border: 1px solid var(--line);
    border-radius: var(--radius);
    background: #f6f7f8;
    overflow: hidden;

    &.is-focus {
      background: #fff;
      border-color: var(--brand);
    }
  }

  &__input {
    flex: 1;
    height: 100%;
    padding: 0 12px;
    border: none;
    outline: none;
    background: transparent;
    font-size: t.$fs-sm;
    color: var(--text-1);
  }

  &__btn {
    width: 40px;
    height: 100%;
    border: none;
    background: transparent;
    color: var(--text-2);

    &:hover {
      color: var(--brand);
    }
  }

  &__panel {
    position: absolute;
    top: 40px;
    left: 0;
    right: 0;
    z-index: 60;
    padding: 12px 14px 14px;
    background: #fff;
    border: 1px solid var(--line);
    border-radius: var(--radius-lg);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  }

  &__section + &__section {
    margin-top: 12px;
  }

  &__section-head {
    display: flex;
    justify-content: space-between;
    margin-bottom: 8px;
    font-size: t.$fs-xs;
    color: var(--text-3);
  }

  &__clear {
    cursor: pointer;

    &:hover {
      color: var(--brand);
    }
  }

  &__tags {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }

  &__tag {
    max-width: 120px;
    padding: 4px 10px;
    border-radius: 12px;
    background: #f6f7f8;
    font-size: t.$fs-xs;
    color: var(--text-2);
    cursor: pointer;

    @include m.ellipsis-1;

    &:hover {
      background: var(--brand-soft, #fff1f5);
      color: var(--brand);
    }
  }

  &__hot {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 4px 12px;
  }

  &__hot-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 4px 0;
    cursor: pointer;

    &:hover .search__hot-text {
      color: var(--brand);
    }
  }

  &__hot-order {
    width: 16px;
    font-size: t.$fs-xs;
    color: var(--text-3);
    text-align: center;

    &.is-top {
      color: #f56c6c;
      font-weight: 700;
    }
  }

  &__hot-text {
    @include m.ellipsis-1;
  }

  &__empty {
    font-size: t.$fs-xs;
    color: var(--text-3);
  }
}
</style>
