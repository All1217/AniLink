import { del, get, post, put } from './client'
import type { Chat, ChatMessage, ChatVo, PageRes } from '@/types'

export interface ChatQueryVo {
  current?: number
  size?: number
  uid?: number
  senderId?: number
  targetId?: number
  id?: number
}

/** 会话列表分页 */
export const fetchChatPage = (query: ChatQueryVo) => get<PageRes<ChatVo>>('/main/chat/getChatVoPage', query)

/** 删除会话 */
export const removeChat = (query: ChatQueryVo) => del('/main/chat/deleteChat', query)

/** 会话消息分页 */
export const fetchMessagePage = (query: ChatQueryVo) =>
  get<PageRes<ChatMessage>>('/main/chat/getChatMessagePage', query)

/** 新建会话方（首次私信某人时） */
export const createChat = (payload: Partial<Chat>) => post<ChatVo>('/main/chat/insertOneChat', payload)

/** 落库一条消息（WS 负责实时推送，HTTP 负责持久化） */
export const insertMessage = (payload: Partial<ChatMessage>) => post('/main/chat/insertOneChatMessage', payload)

/** 更新会话最近打开时间 */
export const touchChat = (query: ChatQueryVo) => put('/main/chat/update', query)

/** 批量已读 */
export const markChatRead = (query: ChatQueryVo) => put('/main/chat/update/unread', query)

/** 私信未读数（导航栏红点） */
export const fetchWhisperUnread = (query: ChatQueryVo) => get<number>('/main/chat/getWhisperUnread', query)
