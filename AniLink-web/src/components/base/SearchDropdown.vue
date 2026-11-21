<template>
  <div class="dropdown">
    <!-- 搜索历史 -->
    <section v-if="history.length > 0" class="dropdown__section">
      <header class="dropdown__head">
        <span>搜索历史</span>
        <span class="dropdown__link" @click="emit('clear-history')">清空</span>
      </header>
      <div class="dropdown__tags">
        <span v-for="item in history" :key="item.content" class="dropdown__tag" @click="emit('use', item.content)">
          {{ item.content }}
        </span>
      </div>
    </section>

    <!-- 当前热搜：两列等高，长标题在列内省略 -->
    <section v-if="hot.length > 0" class="dropdown__section">
      <header class="dropdown__head"><span>当前热搜</span></header>
      <div class="hot">
        <a
          v-for="(item, index) in hot"
          :key="item.keyword || item.show_name"
          class="hot__item"
          :href="hotSearchUrl(item)"
          target="_blank"
          rel="noopener noreferrer"
          :title="item.show_name"
          @click="emit('open-hot', item)"
        >
          <i class="hot__order" :class="{ 'is-top': index < 3 }">{{ index + 1 }}</i>
          <span class="hot__text">{{ item.show_name }}</span>
        </a>
      </div>
    </section>

    <p v-if="history.length === 0 && hot.length === 0" class="dropdown__empty">输入关键词开始搜索</p>
  </div>
</template>

<script setup lang="ts">
import type { HotSearch } from '@/api/stats'
import type { SearchHistoryItem } from '@/stores/search'
import { hotSearchUrl } from '@/utils/bilibili'

defineProps<{ history: SearchHistoryItem[]; hot: HotSearch[] }>()
const emit = defineEmits<{
  (e: 'use', keyword: string): void
  (e: 'clear-history'): void
  (e: 'open-hot', item: HotSearch): void
}>()
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;
@use '@/styles/mixins' as m;

.dropdown {
  position: absolute;
  top: 40px;
  left: 0;
  z-index: 60;
  /* 比输入框宽，双列热搜才有空间；窄屏时收回到视口内 */
  width: 560px;
  max-width: calc(100vw - 48px);
  padding: 12px 14px 14px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);

  &__section + &__section {
    margin-top: 12px;
  }

  &__head {
    display: flex;
    justify-content: space-between;
    margin-bottom: 8px;
    font-size: t.$fs-xs;
    color: var(--text-3);
  }

  &__link {
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

  &__empty {
    font-size: t.$fs-xs;
    color: var(--text-3);
  }
}

.hot {
  display: grid;
  /* minmax(0,1fr) 才能让长标题在列内省略，而不是把列撑破 */
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 2px 20px;

  &__item {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
    padding: 5px 0;
    color: var(--text-1);
    cursor: pointer;

    &:hover .hot__text {
      color: var(--brand);
    }
  }

  &__order {
    flex: 0 0 16px;
    width: 16px;
    font-size: t.$fs-xs;
    color: var(--text-3);
    text-align: center;

    &.is-top {
      color: #f56c6c;
      font-weight: 700;
    }
  }

  &__text {
    flex: 1;
    min-width: 0;
    font-size: t.$fs-sm;

    @include m.ellipsis-1;
  }
}
</style>
