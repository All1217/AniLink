<template>
  <aside class="chat-list">
    <header class="chat-list__head">
      <span class="chat-list__title">近期消息</span>
    </header>

    <ul v-if="chats.length > 0" class="chat-list__body">
      <li
        v-for="chat in chats"
        :key="chat.id"
        :class="['row', { 'is-active': chat.id === currentId }]"
        @click="emit('select', chat)"
      >
        <el-avatar :size="38" :src="peer(chat).avatar || undefined">
          {{ peer(chat).name?.[0] ?? 'U' }}
        </el-avatar>

        <div class="row__info">
          <span class="row__name">{{ peer(chat).name || `用户${peer(chat).id}` }}</span>
          <span class="row__preview">{{ chat.chatMessage?.content || '暂无消息' }}</span>
        </div>

        <span v-if="chat.unreadCount > 0" class="row__badge">{{ chat.unreadCount > 99 ? '99+' : chat.unreadCount }}</span>
        <button class="row__close" title="删除会话" @click.stop="emit('remove', chat)">×</button>
      </li>
    </ul>

    <p v-else-if="!loading" class="chat-list__empty">还没有会话，去别人的空间发私信吧</p>

    <button v-if="hasMore" class="chat-list__more" :disabled="loading" @click="emit('more')">
      {{ loading ? '加载中…' : '加载更多会话' }}
    </button>
  </aside>
</template>

<script setup lang="ts">
import type { ChatVo } from '@/types'

const props = defineProps<{
  chats: ChatVo[]
  currentId: number | null
  me: number
  loading: boolean
  hasMore: boolean
}>()
const emit = defineEmits<{
  (e: 'select', chat: ChatVo): void
  (e: 'remove', chat: ChatVo): void
  (e: 'more'): void
}>()

/** 会话对方信息：我是发送方取 target，否则取 sender */
function peer(chat: ChatVo) {
  const isMine = chat.senderId === props.me
  return {
    id: isMine ? chat.targetId : chat.senderId,
    name: isMine ? chat.targetName : chat.senderName,
    avatar: isMine ? chat.targetAvatar : chat.senderAvatar,
  }
}
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;
@use '@/styles/mixins' as m;

.chat-list {
  display: flex;
  flex-direction: column;
  width: 260px;
  background: #fff;
  border-radius: var(--radius-lg);
  overflow: hidden;

  &__head {
    padding: 12px 16px;
    border-bottom: 1px solid var(--line);
  }

  &__title {
    font-size: t.$fs-sm;
    color: var(--text-1);
  }

  &__body {
    flex: 1;
    overflow-y: auto;
    max-height: 560px;
  }

  &__empty {
    padding: 40px 16px;
    font-size: t.$fs-xs;
    color: var(--text-3);
    text-align: center;
  }

  &__more {
    padding: 10px 0;
    border: none;
    border-top: 1px solid var(--line);
    background: transparent;
    color: var(--link);
    font-size: t.$fs-xs;
  }
}

.row {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;

  &:hover {
    background: var(--bg-hover);
  }

  &.is-active {
    background: var(--brand-soft, #fff1f5);
  }

  &__info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  &__name {
    font-size: t.$fs-sm;
    color: var(--text-1);

    @include m.ellipsis-1;
  }

  &__preview {
    font-size: t.$fs-xs;
    color: var(--text-3);

    @include m.ellipsis-1;
  }

  &__badge {
    min-width: 16px;
    height: 16px;
    padding: 0 4px;
    border-radius: 8px;
    background: #f56c6c;
    color: #fff;
    font-size: 10px;
    line-height: 16px;
    text-align: center;
  }

  &__close {
    width: 16px;
    height: 16px;
    border: none;
    background: transparent;
    color: var(--text-3);
    font-size: 14px;
    line-height: 1;
    opacity: 0;

    .row:hover & {
      opacity: 1;
    }
  }
}
</style>
