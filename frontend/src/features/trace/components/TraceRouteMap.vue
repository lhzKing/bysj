<template>
  <div class="trace-route-map bg-white rounded-xl overflow-hidden shadow-sm border border-gray-100">
    <div class="map-header p-4 border-b border-gray-50">
      <h3 class="text-lg font-semibold text-gray-900 flex items-center gap-2">
        <MapPin class="w-5 h-5 text-blue-500" />
        流转轨迹地图
      </h3>
      <p class="text-sm text-gray-500 mt-1">
        展示产品从生产到当前位置的完整供应链路径
      </p>
    </div>
    
    <div v-if="loading" class="map-loading w-full h-[400px] flex flex-col items-center justify-center bg-gray-50 text-gray-500">
      <div class="w-10 h-10 border-4 border-gray-200 border-t-blue-500 rounded-full animate-spin mb-3"></div>
      <p>正在加载地图...</p>
    </div>
    
    <div v-else-if="!AMAP_KEY" class="map-fallback w-full h-[400px] flex flex-col items-center justify-center bg-gray-50 text-gray-500 p-6 text-center">
      <Map class="w-12 h-12 text-gray-300 mb-3" />
      <p class="text-gray-600 font-medium mb-1">轨迹地图暂不可用</p>
      <p class="text-sm text-gray-400">系统未配置高德地图密钥，仅支持查看列表流转记录。</p>
    </div>

    <div v-else-if="error" class="map-error w-full h-[400px] flex flex-col items-center justify-center bg-gray-50 text-gray-500 p-6 text-center">
      <AlertCircle class="w-10 h-10 text-red-400 mb-3" />
      <p class="text-gray-600 font-medium mb-1">{{ error }}</p>
      <button @click="initMap" class="mt-3 px-4 py-2 bg-blue-500 hover:bg-blue-600 text-white text-sm rounded-lg transition-colors">重试加载</button>
    </div>
    
    <div v-show="!loading && !error && AMAP_KEY" ref="mapContainer" class="w-full h-[400px]"></div>
    
    <!-- 图例 -->
    <div v-if="!error && AMAP_KEY" class="flex flex-wrap gap-4 p-3 border-t border-gray-50 bg-gray-50/50">
      <div class="flex items-center gap-1.5 text-xs text-gray-600">
        <span class="w-3 h-3 rounded-full bg-green-500 border-2 border-white shadow-sm"></span>
        <span>起点（生产）</span>
      </div>
      <div class="flex items-center gap-1.5 text-xs text-gray-600">
        <span class="w-3 h-3 rounded-full bg-blue-500 border-2 border-white shadow-sm"></span>
        <span>途经节点</span>
      </div>
      <div class="flex items-center gap-1.5 text-xs text-gray-600">
        <span class="w-3 h-3 rounded-full bg-red-500 border-2 border-white shadow-sm"></span>
        <span>当前位置</span>
      </div>
      <div class="flex items-center gap-1.5 text-xs text-gray-600">
        <span class="w-6 h-1 bg-blue-500 rounded"></span>
        <span>流转路线</span>
      </div>
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
    await nextTick()
    
    if (!mapContainer.value) throw new Error('地图容器未就绪')
    
    map = new AMap.Map(mapContainer.value, {
      zoom: 5,
      center: [116.405285, 39.904989],
      mapStyle: 'amap://styles/whitesmoke'
    })
    
    drawRoute()
    loading.value = false
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
      strokeColor: '#3b82f6',
      strokeOpacity: 0.8,
      strokeWeight: 4,
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
          width: 24px; 
          height: 24px; 
          border-radius: 50%; 
          background: ${isStart ? '#10B981' : isEnd ? '#EF4444' : '#3B82F6'}; 
          border: 3px solid white;
          box-shadow: 0 2px 6px rgba(0,0,0,0.3);
          display: flex;
          align-items: center;
          justify-content: center;
          color: white;
          font-size: 12px;
          font-weight: bold;
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
