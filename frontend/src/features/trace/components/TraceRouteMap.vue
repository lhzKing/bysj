<template>
  <div class="trace-route-map">
    <div class="trace-route-map__header">
      <div class="trace-route-map__title">
        <MapPin class="trace-route-map__title-icon" />
        <span>流转地理</span>
      </div>
      <p class="trace-route-map__subtitle">展示产品从生产到当前位置的完整供应链路径</p>
    </div>

    <div v-if="loading" class="trace-route-map__placeholder">
      <div class="trace-route-map__spinner" />
      <p>正在加载地图…</p>
    </div>

    <div v-else-if="!AMAP_KEY" class="trace-route-map__placeholder">
      <Map class="trace-route-map__placeholder-icon" />
      <p class="trace-route-map__placeholder-title">轨迹地图暂不可用</p>
      <p class="trace-route-map__placeholder-subtitle">未配置高德地图密钥，仅支持查看列表流转记录。</p>
    </div>

    <div v-else-if="error" class="trace-route-map__placeholder">
      <AlertCircle class="trace-route-map__placeholder-icon trace-route-map__placeholder-icon--error" />
      <p class="trace-route-map__placeholder-title">{{ error }}</p>
      <button type="button" class="trace-route-map__retry" @click="initMap">重试加载</button>
    </div>

    <div v-show="!loading && !error && AMAP_KEY" ref="mapContainer" class="trace-route-map__canvas"></div>

    <div v-if="!error && AMAP_KEY" class="trace-route-map__legend">
      <span class="trace-route-map__legend-item">
        <span class="trace-route-map__dot trace-route-map__dot--start" />
        起点
      </span>
      <span class="trace-route-map__legend-item">
        <span class="trace-route-map__dot trace-route-map__dot--mid" />
        途经
      </span>
      <span class="trace-route-map__legend-item">
        <span class="trace-route-map__dot trace-route-map__dot--end" />
        当前
      </span>
      <span class="trace-route-map__legend-item">
        <span class="trace-route-map__line" />
        流转路线
      </span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { MapPin, Map, AlertCircle } from 'lucide-vue-next'
import { getCoordsByLocation } from '@/shared/data/regions/coordinates'
import { createTraceRouteInfoWindowContent } from './traceRouteMapInfoWindow'

const AMAP_KEY = (import.meta.env.VITE_AMAP_KEY || '').trim()

const props = defineProps({
  history: {
    type: Array,
    required: true
  }
})

const mapContainer = ref(null)
const loading = ref(false)
const error = ref('')
let map = null
let AMap = null

// 从历史记录中提取有效的坐标点
const extractRoutePoints = () => {
  if (!props.history || props.history.length === 0) return []
  
  const sortedHistory = [...props.history].sort((a, b) => 
    new Date(a.eventTime) - new Date(b.eventTime)
  )
  
  const points = []
  const seenCoords = new Set()
  
  for (const log of sortedHistory) {
    const coords = getCoordsByLocation(log.province, log.city)
    if (coords) {
      const key = coords.join(',')
      if (!seenCoords.has(key)) {
        seenCoords.add(key)
        points.push({
          coords,
          name: log.toNode || log.currentNode || `${log.city || ''} ${log.province || ''}`,
          time: log.eventTime,
          action: log.actionType
        })
      }
    }
  }
  
  return points
}

// 加载高德地图 JS API
const loadAMapScript = () => {
  return new Promise((resolve, reject) => {
    if (window.AMap) {
      resolve(window.AMap)
      return
    }

    if (!AMAP_KEY) {
      reject(new Error('未配置高德地图 Key'))
      return
    }

    const existingScript = document.querySelector('script[data-amap-loader="trace-route-map"]')
    if (existingScript) {
      existingScript.addEventListener('load', () => {
        if (window.AMap) resolve(window.AMap)
        else reject(new Error('高德地图加载失败'))
      }, { once: true })
      existingScript.addEventListener('error', () => {
        reject(new Error('高德地图脚本加载失败'))
      }, { once: true })
      return
    }
    
    const script = document.createElement('script')
    script.dataset.amapLoader = 'trace-route-map'
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${AMAP_KEY}&plugin=AMap.Driving`
    script.async = true
    
    script.onload = () => {
      if (window.AMap) resolve(window.AMap)
      else reject(new Error('高德地图加载失败'))
    }
    
    script.onerror = () => {
      reject(new Error('高德地图脚本加载失败'))
    }
    
    document.head.appendChild(script)
  })
}

const initMap = async () => {
  if (!AMAP_KEY) return // 如果没有key，直接不初始化，展示缺省图

  loading.value = true
  error.value = ''

  try {
    AMap = await loadAMapScript()

    // 关键：在 new AMap.Map 之前必须把 canvas 容器变回可见。
    // v-show 在 loading=true 时给 canvas 设 display:none，宽高=0，
    // 此时 AMap 内部测量出来的视口就是 0×0，瓦片永远不请求 —— 桌面上偶尔
    // 因为后续 resize 事件被救回，手机端不会触发那个事件，结果只剩 marker。
    loading.value = false
    await nextTick()

    if (!mapContainer.value) throw new Error('地图容器未就绪')

    map = new AMap.Map(mapContainer.value, {
      zoom: 5,
      center: [116.405285, 39.904989],
      mapStyle: 'amap://styles/whitesmoke'
    })

    // 再兜底一次：若容器在 init 时仍未拿到最终尺寸（如父级 flex 布局延迟计算），
    // 在下一帧主动触发 resize，让瓦片层按真实尺寸重绘。
    requestAnimationFrame(() => {
      if (map && typeof map.resize === 'function') map.resize()
    })

    drawRoute()
  } catch (err) {
    console.error('地图初始化失败:', err)
    error.value = err.message || '地图加载失败'
    loading.value = false
  }
}

const drawRoute = () => {
  if (!map || !AMap) return
  
  const points = extractRoutePoints()
  if (points.length === 0) {
    error.value = '无有效的位置数据，无法绘制轨迹'
    return
  }
  
  map.clearMap()
  
  if (points.length >= 2) {
    const path = points.map(p => new AMap.LngLat(p.coords[0], p.coords[1]))
    
    const polyline = new AMap.Polyline({
      path,
      isOutline: true,
      outlineColor: '#ffffff',
      borderWeight: 2,
      strokeColor: '#5e6ad2',
      strokeOpacity: 0.85,
      strokeWeight: 3,
      strokeStyle: 'solid',
      lineJoin: 'round',
      lineCap: 'round'
    })
    map.add(polyline)
  }
  
  points.forEach((point, index) => {
    const isStart = index === 0
    const isEnd = index === points.length - 1
    
    const marker = new AMap.Marker({
      position: new AMap.LngLat(point.coords[0], point.coords[1]),
      offset: new AMap.Pixel(-12, -12),
      content: `
        <div style="
          width: 22px;
          height: 22px;
          border-radius: 9999px;
          background: ${isStart ? '#27a644' : isEnd ? '#e5484d' : '#5e6ad2'};
          border: 2px solid #ffffff;
          box-shadow: 0 2px 6px rgba(15,23,42,0.2);
          display: flex;
          align-items: center;
          justify-content: center;
          color: #ffffff;
          font-family: 'JetBrains Mono', ui-monospace, monospace;
          font-size: 11px;
          font-weight: 600;
        ">${index + 1}</div>
      `
    })
    
    const infoWindow = new AMap.InfoWindow({
      content: createTraceRouteInfoWindowContent({
        name: point.name,
        time: point.time,
        isStart,
        isEnd
      }),
      offset: new AMap.Pixel(0, -30)
    })
    
    marker.on('click', () => {
      infoWindow.open(map, marker.getPosition())
    })
    map.add(marker)
  })
  
  map.setFitView()
}

watch(() => props.history, () => {
  if (map) drawRoute()
}, { deep: true })

onMounted(() => {
  initMap()
})

onUnmounted(() => {
  if (map) {
    map.destroy()
    map = null
  }
})
</script>

<style scoped>
.trace-route-map {
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.trace-route-map__header {
  padding: 14px 16px;
  border-bottom: 1px solid var(--hairline);
}
.trace-route-map__title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
}
.trace-route-map__title-icon {
  width: 14px;
  height: 14px;
  color: var(--primary);
}
.trace-route-map__subtitle {
  margin: 4px 0 0 0;
  font-size: 12px;
  color: var(--ink-subtle);
}

.trace-route-map__placeholder {
  width: 100%;
  height: 320px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: var(--surface-2);
  color: var(--ink-subtle);
  padding: 24px;
  text-align: center;
}
.trace-route-map__placeholder-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--ink);
  margin: 0;
}
.trace-route-map__placeholder-subtitle {
  font-size: 12px;
  color: var(--ink-subtle);
  margin: 0;
}
.trace-route-map__placeholder-icon {
  width: 36px;
  height: 36px;
  color: var(--ink-tertiary);
}
.trace-route-map__placeholder-icon--error {
  color: var(--error);
}
.trace-route-map__spinner {
  width: 28px;
  height: 28px;
  border: 2px solid var(--hairline);
  border-top-color: var(--primary);
  border-radius: 9999px;
  animation: trace-route-map-spin 0.8s linear infinite;
}
@keyframes trace-route-map-spin {
  to { transform: rotate(360deg); }
}
.trace-route-map__retry {
  margin-top: 4px;
  height: 30px;
  padding: 0 14px;
  border-radius: 6px;
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  color: var(--ink);
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  transition: border-color 0.15s;
}
.trace-route-map__retry:hover {
  border-color: var(--ink-subtle);
}

.trace-route-map__canvas {
  width: 100%;
  height: 360px;
}

.trace-route-map__legend {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 10px 16px;
  border-top: 1px solid var(--hairline);
  background: var(--surface-2);
  font-size: 11.5px;
  color: var(--ink-muted);
}
.trace-route-map__legend-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}
.trace-route-map__dot {
  width: 8px;
  height: 8px;
  border-radius: 9999px;
  border: 1.5px solid var(--surface-1);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.12);
}
.trace-route-map__dot--start { background: var(--success); }
.trace-route-map__dot--mid { background: var(--primary); }
.trace-route-map__dot--end { background: var(--error); }
.trace-route-map__line {
  width: 18px;
  height: 2px;
  background: var(--primary);
  border-radius: 9999px;
}

@media (max-width: 640px) {
  .trace-route-map__canvas { height: 280px; }
}
</style>
