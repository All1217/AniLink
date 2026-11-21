<template>
  <section class="window">
    <template v-if="current">
      <header class="window__head">
        <RouterLink :to="`/user/${peer.id}`" class="window__name">{{ peer.name || `用户${peer.id}` }}</RouterLink>
        <span class="window__count">共 {{ total }} 条消息</span>
      </header>

      <div ref="scroller" v-loading="loading" class="window__body">
        <button v-if="messages.length < total" class="window__older" @click="emit('load-more')">加载更早的消息</button>

        <div
          v-for="message in messages"
          :key="message.id"
          :class="['bubble', message.senderId === me ? 'is-mine' : 'is-peer']"
        >
          <p class="bubble__text">{{ message.content }}</p>
          <span class="bubble__time">{{ message.time?.slice(11, 16) }}</span>
        </div>
      </div>

      <footer class="window__send">
        <el-input
          v-model="draft"
          type="textarea"
          :rows="2"
          maxlength="500"
          resize="none"
          placeholder="按 Enter 发送，Shift + Enter 换行"
          @keydown.enter.exact.prevent="submit"
        />
        <el-button type="primary" :disabled="!draft.trim()" @click="submit">发送</el-button>
      </footer>
    </template>

    <el-empty v-else description="选择一个会话开始聊天" :image-size="90" />
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import type { ChatMessage, ChatVo } from '@/types'

const props = defineProps<{
  current: ChatVo | null
  messages: ChatMessage[]
  me: number
  total: number
  loading: boolean
}>()
const emit = defineEmits<{ (e: 'send', text: string): void; (e: 'load-more'): void }>()

const draft = ref('')
const scroller = ref<HTMLElement | null>(null)

const peer = computed(() => {
  const chat = props.current
  if (!chat) return { id: 0, name: '' }
  const isMine = chat.senderId === props.me
  return { id: isMine ? chat.targetId : chat.senderId, name: isMine ? chat.targetName : chat.senderName }
})

function submit() {
  const text = draft.value.trim()
  if (!text) return
  emit('send', text)
  draft.value = ''
}

/** 新消息或切换会话后滚到底部 */
function scrollToBottom() {
  nextTick(() => {
    if (scroller.value) scroller.value.scrollTop = scroller.value.scrollHeight
  })
}

watch(() => [props.messages.length, props.current?.id], scrollToBottom, { immediate: true })
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.window {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: #fff;
  border-radius: var(--radius-lg);
  overflow: hidden;

  &__head {
    display: flex;
    align-items: baseline;
    gap: 12px;
    padding: 12px 16px;
    border-bottom: 1px solid var(--line);
  }

  &__name {
    font-size: t.$fs-sm;
    font-weight: 600;
    color: var(--text-1);

    &:hover {
      color: var(--brand);
    }
  }

  &__count {
    font-size: t.$fs-xs;
    color: var(--text-3);
  }

  &__body {
    flex: 1;
    min-height: 380px;
    max-height: 480px;
    overflow-y: auto;
    padding: 16px;
    background: #fafbfc;
  }

  &__older {
    display: block;
    margin: 0 auto 12px;
    border: none;
    background: transparent;
    color: var(--link);
    font-size: t.$fs-xs;
  }

  &__send {
    display: flex;
    align-items: flex-end;
    gap: 12px;
    padding: 12px 16px;
    border-top: 1px solid var(--line);
  }
}

.bubble {
  display: flex;
  flex-direction: column;
  max-width: 68%;
  margin-bottom: 12px;

  &__text {
    padding: 8px 12px;
    border-radius: 8px;
    font-size: t.$fs-sm;
    line-height: 20px;
    word-break: break-word;
    white-space: pre-wrap;
  }

  &__time {
    margin-top: 2px;
    font-size: 10px;
    color: var(--text-3);
  }

  &.is-mine {
    align-items: flex-end;
    margin-left: auto;

    .bubble__text {
      background: var(--brand);
      color: #fff;
    }
  }

  &.is-peer .bubble__text {
    background: #fff;
    color: var(--text-1);
    border: 1px solid var(--line);
  }
}
</style>
