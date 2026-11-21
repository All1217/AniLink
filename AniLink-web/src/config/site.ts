/** 首页分区配置：B 站一级分区名，点击暂只做提示（后端无分区筛选接口） */
export interface PartitionItem {
  id: number
  name: string
}

export const partitions: PartitionItem[] = [
  { id: 1, name: '动画' },
  { id: 2, name: '番剧' },
  { id: 3, name: '国创' },
  { id: 4, name: '音乐' },
  { id: 5, name: '舞蹈' },
  { id: 6, name: '游戏' },
  { id: 7, name: '知识' },
  { id: 8, name: '科技数码' },
  { id: 9, name: '运动' },
  { id: 10, name: '汽车' },
  { id: 11, name: '生活' },
  { id: 12, name: '美食' },
  { id: 13, name: '动物圈' },
  { id: 14, name: '时尚美妆' },
  { id: 15, name: '资讯' },
  { id: 16, name: '娱乐' },
  { id: 17, name: '影视' },
  { id: 18, name: '纪录片' },
  { id: 19, name: '电影' },
  { id: 20, name: '电视剧' },
  { id: 21, name: '综艺' },
  { id: 22, name: '鬼畜' },
  { id: 23, name: '搞笑' },
  { id: 24, name: 'VLOG' },
  { id: 25, name: '单机游戏' },
  { id: 26, name: '虚拟UP主' },
  { id: 27, name: '公开课' },
  { id: 28, name: '专栏' },
]

/** 首页轮播：默认 3 条本地占位（不引用外部图片，避免内网地址进代码） */
export interface SlideItem {
  id: number
  title: string
  /** 渐变底色，用于无封面时的占位块 */
  background: string
  /** 可选的跳转地址；留空则点击无动作 */
  href?: string
}

export const slides: SlideItem[] = [
  { id: 1, title: '欢迎来到 AniLink', background: 'linear-gradient(120deg,#fb7299,#ff9db8)' },
  { id: 2, title: '投稿你的第一个视频', background: 'linear-gradient(120deg,#00aeec,#6fd0f5)' },
  { id: 3, title: '本地 MCP 已接入，边聊边查库', background: 'linear-gradient(120deg,#61666d,#9499a0)' },
]

/** 首页列表分页大小 */
export const PAGE_SIZE = 20
