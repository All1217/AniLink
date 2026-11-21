import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

// 按需注册：只打包折线图 + 网格/提示/图例，避免整包 echarts（3 倍体积）
echarts.use([LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

export { echarts }
export type EChartsInstance = echarts.ECharts
