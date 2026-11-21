<template>
  <div class="message-view">
    <header class="message-view__head">
      <h1 class="message-view__title">消息中心</h1>
      <div class="message-view__tabs">
        <span
          :class="['message-view__tab', { 'is-active': tab === 'whisper' }]"
          @click="tab = 'whisper'"
        >
          私信
        </span>
        <span :class="['message-view__tab', { 'is-active': tab === 'system' }]" @click="tab = 'system'">
          系统通知
        </span>
      </div>
      <span class="message-view__status">{{ statusText }}</span>
    </header>

    <div v-if="tab === 'whisper'" class="message-view__body">
      <ChatList
        :chats="session.chats.value"
        :current-id="session.current.value?.id ?? null"
        :me="session.me.value"
        :loading="session.loadingChats.value"
        :has-more="session.hasMoreChats.value"
        @select="onSelect"
        @remove="session.remove"
        @more="session.loadChats()"
      />

      <ChatWindow
        :current="session.current.value"
        :messages="session.messages.value"
        :me="session.me.value"
        :total="session.messageTotal.value"
        :loading="session.loadingMessages.value"
        @send="session.send"
        @load-more="session.loadMessages()"
      />
    </div>

    <SystemNotice v-else />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import ChatList from '@/components/chat/ChatList.vue'
import ChatWindow from '@/components/chat/ChatWindow.vue'
import SystemNotice from '@/components/chat/SystemNotice.vue'
import { useUserStore } from '@/stores/user'
import { useChatSession } from '@/composables/useChatSession'
import { useWebSocket } from '@/composables/useWebSocket'
import type { ChatMessageDTO, ChatVo } from '@/types'

const userStore = useUserStore()
const tab = ref<'whisper' | 'system'>('whisper')

/** WS 连接由页面持有；会话逻辑通过 send 回调把消息发出去 */
const socket = useWebSocket<Record<string, string>>('/chat', {
  onOpen: (send) => send({ from: String(userStore.uid), to: String(userStore.uid), type: 'auth' }),
  onMessage: (message) => {
    if (message.type === 'private') session.receive(message as unknown as ChatMessageDTO)
  },
  heartbeat: () => ({ from: String(userStore.uid), to: String(userStore.uid), type: 'heartbeat' }),
})

const session = useChatSession((payload) => socket.send(payload))

const statusText = computed(() => {
  if (tab.value === 'system') return '系统通知暂未开放'
  return socket.status.value === 'open' ? '实时连接已就绪' : '实时连接未建立，消息仍可落库'
})

function onSelect(chat: ChatVo) {
  void session.select(chat)
}

onMounted(() => {
  if (!userStore.isLogin) return
  void session.loadChats(true)
  socket.connect()
})

// 登录后（含刷新恢复）补连一次
watch(
  () => userStore.isLogin,
  (value) => {
    if (!value) return
    void session.loadChats(true)
    socket.connect()
  },
)
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.message-view {
  width: t.$page-width;
  margin: 0 auto;
  padding-top: 16px;

  &__head {
    display: flex;
    align-items: center;
    gap: 20px;
    margin-bottom: 12px;
  }

  &__title {
    font-size: t.$fs-lg;
    color: var(--text-1);
  }

  &__tabs {
    display: flex;
    gap: 12px;
  }

  &__tab {
    padding: 4px 12px;
    border-radius: 12px;
    background: #fff;
    font-size: t.$fs-xs;
    color: var(--text-2);
    cursor: pointer;

    &.is-active {
      background: var(--brand-soft, #fff1f5);
      color: var(--brand);
    }
  }

  &__status {
    margin-left: auto;
    font-size: t.$fs-xs;
    color: var(--text-3);
  }

  &__body {
    display: flex;
    gap: 16px;
    align-items: stretch;
  }
}
</style>
