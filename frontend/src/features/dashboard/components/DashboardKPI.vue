<script setup>
import { computed } from 'vue'

const props = defineProps({
  kpiData: {
    type: Object,
    required: true
  }
})
</script>

<template>
  <div class="grid grid-cols-12 gap-8">
      <!-- 大尺寸核心指标 (Hero Stat) -->
      <div class="col-span-12 lg:col-span-8 premium-card hover-lift rounded-[48px] p-12 relative overflow-hidden group">
          <div class="relative z-10">
              <div class="flex justify-between items-center">
                  <span class="px-4 py-1.5 bg-indigo-600 text-white text-[10px] font-black rounded-full uppercase tracking-[0.15em]">Neural Flow Live</span>
                  <div class="flex -space-x-2">
                      <div class="size-8 rounded-full border-2 border-white bg-slate-200 shadow-sm"></div>
                      <div class="size-8 rounded-full border-2 border-white bg-slate-300 shadow-sm"></div>
                      <div class="size-8 rounded-full border-2 border-white bg-indigo-400 shadow-sm"></div>
                  </div>
              </div>
              <h3 class="text-3xl font-black text-slate-900 mt-10 tracking-tight">全网流通溯源码</h3>
              <div class="mt-8 flex items-baseline flex-wrap">
                  <span class="text-7xl md:text-[100px] font-black leading-none stat-value">
                    {{ kpiData.totalTraces >= 1000 ? (kpiData.totalTraces / 1000).toFixed(1) : kpiData.totalTraces }}<span class="text-4xl ml-2 font-bold opacity-50" v-if="kpiData.totalTraces >= 1000">K</span>
                  </span>
                  <div class="ml-0 mt-4 md:mt-0 md:ml-10">
                      <div class="text-emerald-500 font-black text-2xl">↑ 今日新增 {{ kpiData.todayNew }}</div>
                      <div class="text-slate-400 font-bold text-xs uppercase tracking-widest">Efficiency</div>
                  </div>
              </div>
          </div>
          <!-- 抽象几何修饰 -->
          <div class="absolute -right-20 -bottom-20 size-[300px] md:size-[400px] bg-gradient-to-br from-indigo-100 to-emerald-50 rounded-full opacity-30 blur-3xl group-hover:scale-110 transition-transform"></div>
      </div>

      <!-- 右侧垂直卡片组 -->
      <div class="col-span-12 lg:col-span-4 space-y-8">
          <div class="premium-card hover-lift rounded-[40px] p-10 flex flex-col justify-between h-[260px] md:h-[280px]">
              <div>
                  <p class="text-[10px] font-black text-slate-400 uppercase tracking-[0.2em]">Total Logs</p>
                  <h4 class="text-5xl font-black mt-4 text-slate-900">{{ kpiData.totalLogs.toLocaleString() }}</h4>
              </div>
              <div class="flex items-center justify-between">
                <div class="flex items-center text-xs font-bold text-indigo-600">
                    <span>活跃溯源日志</span>
                </div>
                <div class="text-right">
                  <p class="text-[10px] font-black text-rose-400 uppercase tracking-[0.2em]">Exceptions</p>
                  <h4 class="text-3xl font-black mt-1 text-rose-500">{{ kpiData.exceptionCount }}</h4>
                </div>
              </div>
          </div>
          <div class="premium-card rounded-[40px] p-10 bg-slate-900 text-white border-none shadow-indigo-200">
              <div class="size-12 rounded-2xl bg-indigo-600 flex items-center justify-center mb-6">
                  <svg class="size-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path></svg>
              </div>
              <h4 class="text-xl font-black">区块链校验已就绪</h4>
              <p class="text-slate-400 text-xs mt-3 leading-relaxed font-medium">所有的溯源码均已通过去中心化哈希校验，确保物理实物与数字信息的唯一对应。</p>
          </div>
      </div>
  </div>
</template>
