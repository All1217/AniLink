<template>
  <div class="player">
    <div ref="container" class="player__core" />
    <!-- 拖动松手后的缓冲提示：自己画，避免被 Artplayer 的 seek 提示覆盖 -->
    <div v-if="seekTipBusy" class="player__tip">{{ seekTipText }}</div>
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
import { createHlsPlayer, preconnect } from '@/composables/useHls'
import { throttleProgressSeek } from '@/composables/useSeekThrottle'
import { usePlayProgress } from '@/composables/usePlayProgress'
import { useSeekFeedback } from '@/composables/useSeekFeedback'
import { createDanmu } from '@/api/danmu'
import { DateStringType, type CountMessageDTO, type CountMessageVO, type Danmu, type Video } from '@/types'
import { formatDate } from '@/utils/format'

const props = defineProps<{ video: Video; danmuList: Danmu[]; uid: number }>()
const emit = defineEmits<{ (e: 'played'): void }>()

const userStore = useUserStore()
const container = ref<HTMLDivElement | null>(null)
const emitter = ref<HTMLDivElement | null>(null)
const watchCount = ref(0)
const danmuCount = ref(0)
const instance = ref<Artplayer | null>(null)

/** 播放地址：优先 HLS 分片（6s 一段、按需加载），还没转码的稿件回退原 MP4 */
const playUrl = computed(() => props.video.hlsUrl || props.video.videoUrl || '')
/** m3u8 交给 hls.js（Safari 走原生 HLS），出错时自动回退 MP4 */
const hls = createHlsPlayer(props.video.videoUrl)
/** 观看进度上报（进入播放即上报 + 每 3 秒一次） */
const progress = usePlayProgress(() => instance.value, () => props.video)

/** 拖动进度条节流的解绑函数（Artplayer 实例创建后注册） */
let detachSeek: (() => void) | null = null
/** 松手后的「缓冲中」提示 */
const seekTip = useSeekFeedback()
/** 模板里要顶层 ref 才会自动解包，这里单独取出来 */
const seekTipBusy = seekTip.busy
const seekTipText = seekTip.text

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
  // danmu.content 是 varchar(100)，超长会触发数据库严格模式报错
  if (text.length > 100) {
    ElMessage.warning('弹幕最多 100 个字符')
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
 * 2) 立即上报一次观看进度 + 每 3 秒继续上报
 */
function onPlayStart() {
  emit('played')
  if (!userStore.isLogin) return
  progress.report()
  progress.start()
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
  () => playUrl.value,
  (url) => {
    if (!instance.value || !url) return
    // 换源前先销毁旧实例，避免 MSE 与新的 src 并存
    hls.destroy()
    instance.value.switch = url
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
  preconnect(playUrl.value)
  instance.value = new Artplayer({
    container: container.value,
    url: playUrl.value,
    type: props.video.hlsUrl ? 'm3u8' : undefined,
    customType: hls.customType,
    poster: props.video.coverUrl,
    autoplay: false,
    autoSize: true,
    fullscreen: true,
    fullscreenWeb: true,
    autoOrientation: true,
    // 默认 preload 是 metadata（点播放才拉流），这里提前缓冲以缩短首帧时间
    moreVideoAttr: { controls: false, preload: 'auto', playsInline: true },
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
  instance.value.on('pause', progress.stop)
  seekTip.attach(instance.value)
  // 拖动只在松手时 seek；拖动中预热落点分片，松手后立刻给出缓冲提示
  detachSeek = throttleProgressSeek(instance.value, {
    onPreview: (time) => hls.prefetch(time),
    onCommit: seekTip.show,
  })

  if (userStore.isLogin) connectWs()
})

onBeforeUnmount(() => {
  progress.stop()
  closeWs()
  detachSeek?.()
  detachSeek = null
  // Artplayer 不会调用 customType 的返回值，必须自己销毁 hls 实例
  hls.destroy()
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

  &__tip {
    position: absolute;
    top: 12px;
    left: 12px;
    z-index: 5;
    padding: 5px 10px;
    border-radius: var(--radius);
    background: rgb(0 0 0 / 60%);
    color: #fff;
    font-size: t.$fs-xs;
    pointer-events: none;
  }

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
