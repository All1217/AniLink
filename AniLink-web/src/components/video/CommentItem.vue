<template>
  <article class="item">
    <el-avatar :size="36" :src="comment.avatar || undefined">{{ comment.nickname?.[0] ?? 'U' }}</el-avatar>
    <div class="item__body">
      <header class="item__head">
        <span class="item__name">{{ comment.nickname || `用户${comment.uid}` }}</span>
        <span class="item__time">{{ fromNow(comment.createTime) }}</span>
        <span v-if="comment.isTop" class="item__top">置顶</span>
      </header>

      <p class="item__content">{{ comment.content }}</p>

      <div class="item__actions">
        <span class="item__action"><el-icon><Pointer /></el-icon>{{ comment.love }}</span>
        <span class="item__action" @click="toggleReply">回复</span>
        <span v-if="isMine" class="item__action" @click="onDelete">删除</span>
      </div>

      <CommentEditor
        v-if="replying"
        :vid="vid"
        :root-id="rootId"
        :parent-id="comment.id"
        :to-user-id="comment.uid"
        :placeholder="`回复 @${comment.nickname || '对方'}`"
        @submitted="onReplied"
      />

      <div v-if="comment.replyCount > 0" class="item__replies">
        <button v-if="!expanded" class="item__expand" @click="expand">
          共 {{ comment.replyCount }} 条回复，点击查看
        </button>

        <template v-else>
          <CommentReply
            v-for="reply in replies"
            :key="reply.id"
            :reply="reply"
            :can-delete="reply.uid === userStore.uid"
            @delete="onDeleteReply"
          />
          <button v-if="replies.length < comment.replyCount" class="item__expand" @click="expand">
            查看更多回复
          </button>
        </template>
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { Pointer } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import CommentEditor from './CommentEditor.vue'
import CommentReply from './CommentReply.vue'
import { deleteComment, fetchReplies } from '@/api/comment'
import { useUserStore } from '@/stores/user'
import type { CommentQuery, CommentVo } from '@/types'
import { fromNow } from '@/utils/format'

const props = defineProps<{ comment: CommentVo; vid: number; rootId?: number }>()
const emit = defineEmits<{ (e: 'deleted'): void }>()

const userStore = useUserStore()
const expanded = ref(false)
const replying = ref(false)
const replies = ref<CommentVo[]>([])
const page = ref(1)

const isMine = computed(() => userStore.isLogin && userStore.uid === props.comment.uid)
const rootId = computed(() => props.rootId ?? props.comment.id)

function toggleReply() {
  if (!userStore.isLogin) {
    userStore.openLoginDialog()
    return
  }
  replying.value = !replying.value
}

/** 分页拉取回复（一次 10 条，到底隐藏按钮） */
async function expand() {
  // 已经拉齐了就不用再请求，避免出现"查看更多回复"点了没反应
  if (replies.value.length >= (props.comment.replyCount ?? 0)) {
    expanded.value = true
    return
  }
  const query: CommentQuery = {
    vid: props.vid,
    rootId: rootId.value,
    current: page.value,
    size: 10,
  }
  const result = await fetchReplies(query).catch(() => null)
  if (!result) return
  replies.value = [...replies.value, ...(result.records ?? [])]
  page.value += 1
  expanded.value = true
}

/**
 * 自己回复成功后：
 * 回复总数变了，必须从第 1 页重新拉，不能沿用旧页码（旧页码会取到空页，
 * 于是出现"共 2 条回复只显示 1 条 + 查看更多回复点不动"的现象）。
 */
function onReplied() {
  replying.value = false
  replies.value = []
  page.value = 1
  const target = props.comment
  target.replyCount = (target.replyCount ?? 0) + 1
  void expand()
}

/** 删除自己的回复：本地移除并同步计数，不整页刷新 */
async function onDeleteReply(reply: CommentVo) {
  const confirmed = await ElMessageBox.confirm('删除这条回复？', '提示', { type: 'warning' })
    .then(() => true)
    .catch(() => false)
  if (!confirmed) return
  const ok = await deleteComment({ id: reply.id, vid: props.vid } as CommentQuery)
    .then(() => true)
    .catch(() => false)
  if (!ok) return
  ElMessage.success('已删除')
  replies.value = replies.value.filter((item) => item.id !== reply.id)
  const target = props.comment
  target.replyCount = Math.max(0, (target.replyCount ?? 0) - 1)
}

async function onDelete() {
  const confirmed = await ElMessageBox.confirm('删除这条评论？', '提示', { type: 'warning' })
    .then(() => true)
    .catch(() => false)
  if (!confirmed) return
  // 后端只允许删除自己的评论，失败时不要误报成功
  const ok = await deleteComment({ id: props.comment.id, vid: props.vid } as CommentQuery)
    .then(() => true)
    .catch(() => false)
  if (!ok) return
  ElMessage.success('已删除')
  emit('deleted')
}
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.item {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid var(--line-soft, #f1f2f3);

  &__body {
    flex: 1;
    min-width: 0;
  }

  &__head {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: t.$fs-xs;
  }

  &__name {
    color: var(--text-2);
    font-weight: 500;
  }

  &__time {
    color: var(--text-3);
  }

  &__top {
    padding: 0 4px;
    border-radius: 2px;
    background: var(--brand-soft, #fff1f5);
    color: var(--brand);
  }

  &__content {
    margin: 6px 0 8px;
    font-size: t.$fs;
    line-height: 22px;
    color: var(--text-1);
    word-break: break-word;
  }

  &__actions {
    display: flex;
    gap: 16px;
    margin-bottom: 8px;
    font-size: t.$fs-xs;
    color: var(--text-3);
  }

  &__action {
    display: flex;
    align-items: center;
    gap: 2px;
    cursor: pointer;

    &:hover {
      color: var(--brand);
    }
  }

  &__expand {
    margin: 4px 0;
    border: none;
    background: transparent;
    color: var(--link);
    font-size: t.$fs-xs;
  }

  &__replies {
    margin-top: 6px;
    padding-left: 8px;
    border-left: 2px solid var(--line-soft, #f1f2f3);
  }
}
</style>
