import { get, post, put } from './client'
import type { Comment, CommentQuery, CommentVo, PageRes } from '@/types'

/** 按最新/最热筛选根评论 */
export enum CommentFilter {
  LATEST = 1,
  HOTTEST = 2,
}

/** 根评论分页 */
export const fetchRootComments = (query: CommentQuery) =>
  get<PageRes<CommentVo>>('/main/comment/pageCommentVo', query)

/** 某条根评论下的回复分页 */
export const fetchReplies = (query: CommentQuery) => get<PageRes<CommentVo>>('/main/comment/pageReply', query)

/** 发表评论（根评论 rootId=0，回复传 rootId/parentId/toUserId） */
export const createComment = (payload: Partial<Comment>) => post<number>('/main/comment/insert', payload)

/** 逻辑删除自己的评论 */
export const deleteComment = (query: CommentQuery) => put('/main/comment/logicDelete', query)
