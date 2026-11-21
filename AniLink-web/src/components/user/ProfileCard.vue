<template>
  <section class="profile">
    <div class="profile__banner" :style="bannerStyle" />

    <div class="profile__body">
      <el-avatar :size="76" :src="user.avatar || undefined" class="profile__avatar">
        {{ user.nickname?.[0] ?? 'U' }}
      </el-avatar>

      <div class="profile__info">
        <h1 class="profile__name">{{ user.nickname }}</h1>
        <p class="profile__bio">{{ user.description || '这个人很神秘，什么都没有写' }}</p>
      </div>

      <div class="profile__actions">
        <el-button v-if="isSelf" @click="emit('edit')">编辑资料</el-button>
        <button
          v-else
          class="profile__follow"
          :class="{ 'is-following': isFollow }"
          :disabled="busy"
          @click="emit('toggle-follow')"
        >
          {{ isFollow ? '已关注' : '+ 关注' }}
        </button>
      </div>
    </div>

    <ul class="profile__stats">
      <li><b>{{ formatCount(stats?.followCount) }}</b><span>关注</span></li>
      <li><b>{{ formatCount(stats?.fansCount) }}</b><span>粉丝</span></li>
      <li><b>{{ formatCount(stats?.likeCount) }}</b><span>获赞</span></li>
      <li><b>{{ formatCount(stats?.playCount) }}</b><span>播放</span></li>
    </ul>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { User, UserStats } from '@/types'
import { formatCount } from '@/utils/format'

const props = defineProps<{
  user: User
  stats: UserStats | null
  isSelf: boolean
  isFollow: boolean
  busy: boolean
}>()
const emit = defineEmits<{ (e: 'edit'): void; (e: 'toggle-follow'): void }>()

/** 有主页背景图就用，否则用品牌渐变兜底 */
const bannerStyle = computed(() =>
  props.user.background
    ? { backgroundImage: `url(${props.user.background})` }
    : { background: 'linear-gradient(120deg,#fb7299,#fc8bab 55%,#ffd3e0)' },
)
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.profile {
  background: #fff;
  border-radius: var(--radius-lg);
  overflow: hidden;

  &__banner {
    height: 120px;
    background-size: cover;
    background-position: center;
  }

  &__body {
    position: relative;
    display: flex;
    gap: 16px;
    padding: 0 24px 12px;
    margin-top: -28px;
  }

  &__avatar {
    border: 3px solid #fff;
    flex-shrink: 0;
  }

  &__info {
    flex: 1;
    min-width: 0;
    padding-top: 32px;
  }

  &__name {
    font-size: 20px;
    color: var(--text-1);
  }

  &__bio {
    margin-top: 4px;
    font-size: t.$fs-xs;
    color: var(--text-3);
    max-width: 520px;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  &__actions {
    padding-top: 34px;
  }

  &__follow {
    height: 32px;
    padding: 0 18px;
    border: 1px solid var(--brand);
    border-radius: var(--radius);
    background: var(--brand);
    color: #fff;
    font-size: t.$fs-sm;

    &.is-following {
      background: transparent;
      color: var(--text-2);
      border-color: var(--line);
    }
  }

  &__stats {
    display: flex;
    gap: 32px;
    padding: 4px 24px 16px;

    li {
      display: flex;
      align-items: baseline;
      gap: 4px;
      font-size: t.$fs-xs;
      color: var(--text-3);
    }

    b {
      font-size: t.$fs-md;
      color: var(--text-1);
    }
  }
}
</style>
