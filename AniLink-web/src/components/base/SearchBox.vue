<template>
  <div ref="boxRef" class="search">
    <div class="search__bar" :class="{ 'is-focus': visible }">
      <input
        ref="inputRef"
        v-model="keyword"
        class="search__input"
        type="text"
        maxlength="50"
        placeholder="搜索视频、UP 主"
        @focus="onFocus"
        @keydown.enter="onEnter"
      />
      <button v-if="showReset" class="search__reset" title="清空" @mousedown.prevent @click="clearInput">
        <el-icon :size="12"><Close /></el-icon>
      </button>
      <button class="search__btn" @click="onEnter">
        <el-icon><Search /></el-icon>
      </button>
    </div>

    <SearchDropdown
      v-if="visible"
      :history="history"
      :hot="hot"
      @use="go"
      @clear-history="searchStore.clear()"
      @open-hot="onHotClick"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Close, Search } from '@element-plus/icons-vue'
import SearchDropdown from '@/components/base/SearchDropdown.vue'
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
const inputRef = ref<HTMLInputElement | null>(null)
const history = computed(() => searchStore.history)
/** 聚焦且有内容时显示快速清空按钮 */
const showReset = computed(() => visible.value && keyword.value.length > 0)

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

/** 站内搜索：记录历史并跳转搜索结果页 */
function go(value: string) {
  const text = value.trim()
  if (!text) return
  searchStore.add(text)
  keyword.value = text
  visible.value = false
  router.push({ path: '/search', query: { keyword: text } })
}

/** 点击热搜：只记录历史，跳转 B 站由 <a target="_blank"> 完成 */
function onHotClick(item: HotSearch) {
  const word = (item.keyword || item.show_name || '').trim()
  if (word) searchStore.add(word)
  visible.value = false
}

/** 清空输入框但保持聚焦，便于继续输入 */
function clearInput() {
  keyword.value = ''
  inputRef.value?.focus()
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

  &__reset {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    width: 24px;
    height: 24px;
    margin-right: 2px;
    border: none;
    border-radius: 50%;
    background: transparent;
    color: var(--text-3);

    &:hover {
      background: var(--bg-hover);
      color: var(--text-1);
    }
  }

  &__btn {
    flex-shrink: 0;
    width: 40px;
    height: 100%;
    border: none;
    background: transparent;
    color: var(--text-2);

    &:hover {
      color: var(--brand);
    }
  }
}
</style>
