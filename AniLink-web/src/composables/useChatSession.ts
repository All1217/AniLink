import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { createChat, fetchChatPage, fetchMessagePage, insertMessage, markChatRead, removeChat, touchChat } from '@/api/chat'
import type { ChatMessage, ChatMessageDTO, ChatVo } from '@/types'
import { formatDate } from '@/utils/format'
import { DateStringType } from '@/types'

const PAGE = 20

/**
 * 私信会话状态机：会话列表 / 当前会话消息 / 发送 / 接收 / 已读。
 * WS 连接本身由页面持有，这里只通过 sendViaSocket 把消息发出去。
 */
export function useChatSession(sendViaSocket: (payload: unknown) => void) {
  const userStore = useUserStore()
  const me = computed(() => userStore.uid)

  const chats = ref<ChatVo[]>([])
  const current = ref<ChatVo | null>(null)
  const messages = ref<ChatMessage[]>([])
  const loadingChats = ref(false)
  const loadingMessages = ref(false)
  const chatPage = ref(1)
  const messagePage = ref(1)
  const messageTotal = ref(0)
  const hasMoreChats = ref(true)
  let tempId = -1

  /** 会话对方的信息：我是发送方就取 target，否则取 sender */
  function peerOf(chat: ChatVo) {
    const isMine = chat.senderId === me.value
    return {
      id: isMine ? chat.targetId : chat.senderId,
      name: isMine ? chat.targetName : chat.senderName,
      avatar: isMine ? chat.targetAvatar : chat.senderAvatar,
    }
  }

  /** 会话与「我 + 对方」是否匹配（两个方向都算） */
  function matches(chat: ChatVo, uid: number) {
    return (
      (chat.senderId === me.value && chat.targetId === uid) ||
      (chat.senderId === uid && chat.targetId === me.value)
    )
  }

  /** 加载会话列表（按最近时间倒序由后端给出） */
  async function loadChats(reset = false) {
    if (!me.value || loadingChats.value) return
    if (reset) {
      chats.value = []
      chatPage.value = 1
      hasMoreChats.value = true
    }
    if (!hasMoreChats.value) return
    loadingChats.value = true
    try {
      const result = await fetchChatPage({ uid: me.value, current: chatPage.value, size: PAGE })
      const records = result.records ?? []
      chats.value = [...chats.value, ...records]
      chatPage.value += 1
      hasMoreChats.value = records.length === PAGE
    } finally {
      loadingChats.value = false
    }
  }

  /** 消息按时间升序展示（后端分页顺序不做假设） */
  function sortMessages() {
    messages.value.sort((a, b) => new Date(a.time).getTime() - new Date(b.time).getTime())
  }

  /** 加载当前会话消息，page 递增即加载更早的消息 */
  async function loadMessages(reset = false) {
    if (!current.value || loadingMessages.value) return
    if (reset) {
      messages.value = []
      messagePage.value = 1
      messageTotal.value = 0
    }
    loadingMessages.value = true
    try {
      const result = await fetchMessagePage({
        current: messagePage.value,
        size: PAGE,
        senderId: current.value.senderId,
        targetId: current.value.targetId,
      })
      messages.value = [...messages.value, ...(result.records ?? [])]
      messageTotal.value = result.total ?? messages.value.length
      messagePage.value += 1
      sortMessages()
    } finally {
      loadingMessages.value = false
    }
  }

  /** 选中会话：标记已读 + 更新最近打开时间 + 拉消息 */
  async function select(chat: ChatVo) {
    current.value = chat
    if (chat.unreadCount > 0) {
      chat.unreadCount = 0
      void markChatRead({ uid: me.value, senderId: chat.senderId, targetId: chat.targetId }).catch(() => undefined)
    }
    void touchChat({ id: chat.id }).catch(() => undefined)
    await loadMessages(true)
  }

  /** 发送：先本地上屏，再落库，最后走 WS 推给对方 */
  async function send(content: string) {
    const text = content.trim()
    if (!text || !current.value) return
    const chat = current.value
    const now = formatDate(Date.now(), DateStringType.ALL)
    const local: ChatMessage = {
      id: tempId--,
      senderId: me.value,
      targetId: peerOf(chat).id,
      content: text,
      senderDel: 0,
      targetDel: 0,
      withdraw: 0,
      messageType: 0,
      readStatus: 1,
      time: now,
    }
    messages.value = [...messages.value, local]
    chat.chatMessage = local
    chat.latestTime = now
    sortMessages()

    sendViaSocket({ from: String(me.value), to: String(local.targetId), type: 'private', content: text })
    await insertMessage(local).catch(() => ElMessage.warning('消息已显示，但落库失败'))
  }

  /** 收到 WS 私信：当前会话直接上屏，其它会话加未读 */
  function receive(dto: ChatMessageDTO) {
    const from = Number(dto.from)
    const to = Number(dto.to)
    const content = dto.content ?? ''
    const now = formatDate(Date.now(), DateStringType.ALL)

    if (current.value && from === peerOf(current.value).id) {
      messages.value = [
        ...messages.value,
        {
          id: tempId--,
          senderId: from,
          targetId: to,
          content,
          senderDel: 0,
          targetDel: 0,
          withdraw: 0,
          messageType: 0,
          readStatus: 0,
          time: now,
        },
      ]
      sortMessages()
      void markChatRead({ uid: me.value, senderId: current.value.senderId, targetId: current.value.targetId }).catch(
        () => undefined,
      )
      return
    }

    const target = chats.value.find((chat) => matches(chat, from))
    if (target) {
      target.unreadCount += 1
      target.latestTime = now
      return
    }
    // 新会话：重新拉列表（沿用旧实现思路，避免臆造顺序）
    void loadChats(true)
  }

  /** 删除会话（后端按 senderId+targetId 删除） */
  async function remove(chat: ChatVo) {
    await removeChat({ senderId: chat.senderId, targetId: chat.targetId }).catch(() => undefined)
    chats.value = chats.value.filter((item) => item.id !== chat.id)
    if (current.value?.id === chat.id) {
      current.value = null
      messages.value = []
    }
  }

  /** 新会话：先建 Chat 行，再选中 */
  async function startChat(uid: number) {
    const created = await createChat({
      id: 0,
      senderId: me.value,
      targetId: uid,
      latestTime: formatDate(Date.now(), DateStringType.ALL),
    }).catch(() => null)
    if (!created) return
    const exists = chats.value.find((chat) => chat.id === created.id)
    if (!exists) chats.value = [created, ...chats.value]
    await select(exists ?? created)
  }

  return {
    me,
    chats,
    current,
    messages,
    loadingChats,
    loadingMessages,
    messageTotal,
    hasMoreChats,
    peerOf,
    loadChats,
    loadMessages,
    select,
    send,
    receive,
    remove,
    startChat,
  }
}
