<script setup>
import { computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { GraphChart } from 'echarts/charts'
import { TooltipComponent } from 'echarts/components'
import VChart from 'vue-echarts'

use([CanvasRenderer, GraphChart, TooltipComponent])

const props = defineProps({
  topology: {
    type: Object,
    default: () => ({ nodes: [], links: [] })
  }
})

const topologyChartOption = computed(() => {
  const rawNodes = Array.isArray(props.topology?.nodes) ? props.topology.nodes : []
  const rawLinks = Array.isArray(props.topology?.links) ? props.topology.links : []

  if (rawNodes.length === 0 && rawLinks.length === 0) return {}

  // 1) 收集节点（节点表 + 链路端点并集）
  const nodeNameSet = new Set()
  const nodeSizeMap = new Map()

  rawNodes.forEach((node) => {
    const name = node?.name
    if (!name) return
    nodeNameSet.add(name)
    if (Number.isFinite(Number(node.symbolSize)) && Number(node.symbolSize) > 0) {
      nodeSizeMap.set(name, Number(node.symbolSize))
    }
  })

  // 2) 去重、过滤无效链路（过滤自环，避免视觉噪声）
  const linkMap = new Map()
  rawLinks.forEach((link) => {
    const source = link?.source
    const target = link?.target
    if (!source || !target || source === target) return

    nodeNameSet.add(source)
    nodeNameSet.add(target)

    const key = `${source}->${target}`
    const weight = Number(link.count ?? link.value ?? 1)
    const value = Number.isFinite(weight) && weight > 0 ? weight : 1
    linkMap.set(key, (linkMap.get(key) || 0) + value)
  })

  const links = Array.from(linkMap.entries()).map(([key, value]) => {
    const [source, target] = key.split('->')
    return { source, target, value }
  })

  if (nodeNameSet.size === 0) {
    return {
      title: {
        text: '暂无有效拓扑数据',
        left: 'center',
        top: 'center',
        textStyle: { color: '#94a3b8' }
      }
    }
  }

  // 3) 用度数给节点做一个轻量级尺寸自适应
  const degreeMap = new Map(Array.from(nodeNameSet).map((name) => [name, 0]))
  links.forEach((link) => {
    degreeMap.set(link.source, (degreeMap.get(link.source) || 0) + link.value)
    degreeMap.set(link.target, (degreeMap.get(link.target) || 0) + link.value)
  })

  const nodes = Array.from(nodeNameSet).map((name) => {
    const baseSize = nodeSizeMap.get(name)
    const degree = degreeMap.get(name) || 0
    const adaptiveSize = 16 + Math.min(14, Math.log2(degree + 1) * 4)
    const symbolSize = Number.isFinite(baseSize)
      ? Math.max(14, Math.min(baseSize, 42))
      : adaptiveSize

    return {
      name,
      value: degree,
      symbolSize,
      itemStyle: {
        color: '#6366f1',
        borderColor: '#4338ca',
        borderWidth: 1.5
      }
    }
  })

  return {
    tooltip: {
      trigger: 'item',
      triggerOn: 'mousemove',
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      borderColor: '#e2e8f0',
      textStyle: { color: '#1e293b', fontWeight: 'bold' },
      formatter: (params) => {
        if (params.dataType === 'edge') {
          const source = params.data?.source || ''
          const target = params.data?.target || ''
          const value = params.data?.value ?? 1
          return `${source} → ${target}<br/>流转次数: ${value}`
        }
        const name = params.data?.name || params.name
        const degree = params.data?.value ?? 0
        return `${name}<br/>连接强度: ${degree}`
      }
    },
    series: [
      {
        type: 'graph',
        layout: 'force',
        data: nodes,
        links,
        roam: true,
        draggable: true,
        focusNodeAdjacency: true,
        lineStyle: {
          color: '#94a3b8',
          curveness: 0.2,
          opacity: 0.35,
          width: 1.5
        },
        label: {
          show: false
        },
        emphasis: {
          focus: 'adjacency',
          label: {
            show: true,
            color: '#1e293b',
            fontWeight: 'bold'
          }
        },
        force: {
          repulsion: 260,
          gravity: 0.08,
          edgeLength: [70, 160]
        },
        animationDurationUpdate: 600
      }
    ]
  }
})
</script>

<template>
  <div class="flex flex-col h-[500px]">
     <div class="mb-4">
        <h3 class="text-2xl font-black text-slate-900 tracking-tight">神经网络拓扑</h3>
        <p class="text-sm text-slate-500 font-medium">Neural Supply Chain Network</p>
     </div>
     <div class="flex-1 w-full relative min-h-[400px]">
        <v-chart class="w-full h-full absolute inset-0" :option="topologyChartOption" autoresize />
     </div>
  </div>
</template>
