<template>
  <div class="player">
    <div ref="container" class="player__core" />
    <div class="player__bar">
      <span class="player__online">
        <b>{{ watchCount }}</b> 人正在看
      </span>
      <span class="player__divider">，</span>
      <span class="player__dm">已装填 {{ danmuCount }} 条弹幕</span>
      <div ref="emitter" class="player__emitter anilink-dm-root" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import Artplayer from 'artplayer'
import artplayerPluginDanmuku, { type Danmu as PluginDanmu, type Mode } from 'artplayer-plugin-danmuku'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useWebSocket } from '@/composables/useWebSocket'
import { updateProgress } from '@/api/history'
import { createDanmu } from '@/api/danmu'
import { recordPlay } from '@/api/interaction'
import { DateStringType, Interaction, type CountMessageDTO, type CountMessageVO, type Danmu, type Video } from '@/types'
import { formatDate } from '@/utils/format'

const props = defineProps<{ video: Video; danmuList: Danmu[]; uid: number }>()
const emit = defineEmits<{ (e: 'played'): void }>()

const userStore = useUserStore()
const container = ref<HTMLDivElement | null>(null)
const emitter = ref<HTMLDivElement | null>(null)
const watchCount = ref(0)
const danmuCount = ref(0)
const instance = ref<Artplayer | null>(null)

let progressTimer: number | undefined
let playRecorded = false

const danmuku = computed<PluginDanmu[]>(() =>
  props.danmuList.map((item) => ({
    text: item.content,
    time: item.timePoint,
    color: item.color || '#ffffff',
    mode: (item.mode ?? 1) as Mode,
  })),
)

/** 发送弹幕：未登录/空内容拦截，通过后落库 */
function onEmitDanmu(danmu: PluginDanmu) {
  if (!userStore.isLogin) {
    ElMessage.warning('登录后可发送弹幕')
    return false
  }
  const text = danmu.text?.trim() ?? ''
  if (!text) {
    ElMessage.warning('请输入弹幕内容')
    return false
  }
  void createDanmu({
    uid: userStore.uid,
    vid: props.video.vid,
    content: text,
    fontsize: 14,
    mode: danmu.mode ?? 1,
    color: danmu.color ?? '#ffffff',
    timePoint: danmu.time ?? 0,
    status: 1,
    createDate: formatDate(Date.now(), DateStringType.ALL),
  })
    .then(() => ElMessage.success('发送成功'))
    .catch(() => undefined)
  return true
}

/**
 * 开始播放：
 * 1) 通知父组件播放量 +1
 * 2) 登录用户记录一次播放（公开接口，只对登录用户计数）
 * 3) 立即上报一次进度 + 每 3 秒继续上报
 *
 * 注意：观看历史不要另外调 `history/add`——`bh_video` 上有唯一索引 (uid, vid)，
 * 而 `record/add` 本身就是「有则更新、无则插入」的 upsert，重复插入会撞唯一键。
 */
function onPlayStart() {
  emit('played')
  if (!userStore.isLogin) return

  if (!playRecorded) {
    playRecorded = true
    void recordPlay({
      uid: props.uid,
      vid: props.video.vid,
      actionType: Interaction.PLAY,
      actionTime: formatDate(Date.now(), DateStringType.ALL),
    }).catch(() => undefined)
  }

  reportProgress()
  startProgressLoop()
}

/** 上报当前位置：后端把它写成「上次观看到哪个节点」（bh_video.duration） */
function reportProgress() {
  const current = instance.value?.currentTime ?? 0
  if (!props.video.vid) return
  void updateProgress({
    vid: props.video.vid,
    duration: props.video.duration,
    moment: Math.floor(current),
    commitTime: formatDate(Date.now(), DateStringType.ALL),
  }).catch(() => undefined)
}

/** 每 3 秒上报一次播放进度 */
function startProgressLoop() {
  if (progressTimer) return
  progressTimer = window.setInterval(() => {
    const current = instance.value?.currentTime ?? 0
    if (!current || !props.video.vid) return
    void updateProgress({
      vid: props.video.vid,
      duration: props.video.duration,
      moment: Math.floor(current),
      commitTime: formatDate(Date.now(), DateStringType.ALL),
    }).catch(() => undefined)
  }, 3000)
}

function stopProgressLoop() {
  if (progressTimer) window.clearInterval(progressTimer)
  progressTimer = undefined
}

// 在线人数：登录后加入房间，服务端按心跳统计
const { connect: connectWs, close: closeWs } = useWebSocket<CountMessageVO>('/video', {
  onOpen: (send) => send({ vid: props.video.vid, type: 'join' } satisfies CountMessageDTO),
  onMessage: (message) => {
    if (message.type === 'cnt') watchCount.value = message.count
  },
  heartbeat: () => ({ vid: props.video.vid, type: 'heartbeat' }) satisfies CountMessageDTO,
})

watch(
  () => props.video.videoUrl,
  (url) => {
    if (url && instance.value) instance.value.switch = url
  },
  { immediate: true },
)

watch(danmuku, (list) => {
  const plugin = (instance.value?.plugins as Record<string, { load: () => void }> | undefined)?.artplayerPluginDanmuku
  if (!plugin) return
  plugin.load()
  danmuCount.value = list.length
})

onMounted(async () => {
  await nextTick()
  if (!container.value) return
  instance.value = new Artplayer({
    container: container.value,
    url: props.video.videoUrl || '',
    autoSize: true,
    fullscreen: true,
    fullscreenWeb: true,
    autoOrientation: true,
    plugins: [
      artplayerPluginDanmuku({
        danmuku: danmuku.value,
        mount: emitter.value ?? '.anilink-dm-root',
        theme: 'light',
        beforeEmit: onEmitDanmu,
      }),
    ],
  })
  danmuCount.value = danmuku.value.length
  instance.value.on('play', onPlayStart)
  instance.value.on('pause', stopProgressLoop)

  if (userStore.isLogin) connectWs()
})

onBeforeUnmount(() => {
  stopProgressLoop()
  closeWs()
  instance.value?.destroy(false)
  instance.value = null
})
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.player {
  position: relative;
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: #000;

  &__core {
    height: 476px;
  }

  &__bar {
    display: flex;
    align-items: center;
    height: 44px;
    padding: 0 12px;
    background: #fff;
    border-top: 1px solid var(--line);
    font-size: t.$fs-sm;
    color: var(--text-2);
  }

  &__online b {
    color: var(--text-1);
    font-weight: 600;
  }

  &__divider {
    margin-right: 4px;
  }

  &__dm {
    flex-shrink: 0;
  }

  &__emitter {
    flex: 1;
    margin-left: 16px;
    height: 34px;
  }
}
</style>
