<script setup>
import { CheckCircle2, MapPin, Navigation, XCircle, User } from 'lucide-vue-next'
import dayjs from 'dayjs'

const props = defineProps({
  history: {
    type: Array,
    required: true
  }
})

const actionMap = {
  INIT: { label: '生产赋码', color: 'bg-emerald-50 text-emerald-600 border-emerald-200', icon: CheckCircle2 },
  INBOUND: { label: '入库', color: 'bg-indigo-50 text-indigo-600 border-indigo-200', icon: MapPin },
  OUTBOUND: { label: '出库', color: 'bg-indigo-50 text-indigo-600 border-indigo-200', icon: MapPin },
  TRANSFER: { label: '转移', color: 'bg-cyan-50 text-cyan-600 border-cyan-200', icon: Navigation },
  EXCEPTION: { label: '异常', color: 'bg-rose-50 text-rose-600 border-rose-200', icon: XCircle },
  CORRECTION: { label: '修正', color: 'bg-amber-50 text-amber-600 border-amber-200', icon: User }
}

const formatDate = (date) => (date ? dayjs(date).format('YYYY-MM-DD HH:mm:ss') : '-')

const getActionIcon = (actionType) => {
  return actionMap[actionType]?.icon || CheckCircle2
}

const getActionColor = (actionType) => {
  return actionMap[actionType] ? actionMap[actionType].color : 'bg-slate-50 text-slate-600 border-slate-200'
}

const getActionLabel = (actionType) => {
  return actionMap[actionType] ? actionMap[actionType].label : actionType
}
</script>

<template>
  <div class="relative pl-6 md:pl-8 border-l-[3px] border-indigo-100 space-y-10 py-4">
    <div v-for="(log, idx) in history" :key="log.id" class="relative">
      <div
        class="absolute -left-[33px] md:-left-[41px] top-4 w-4 h-4 rounded-full ring-[6px] ring-white"
        :class="idx === 0 ? 'bg-indigo-600 animate-pulse' : 'bg-slate-300'"
      ></div>

      <div class="premium-card rounded-[32px] p-6 hover:shadow-xl transition-all duration-500 border border-transparent hover:border-slate-200 group bg-white/40">
        <div class="flex items-start justify-between mb-6">
          <div class="flex items-center gap-4">
            <div :class="['p-3 rounded-2xl border', getActionColor(log.actionType)]">
              <component :is="getActionIcon(log.actionType)" class="w-6 h-6" />
            </div>
            <div>
              <p class="font-black text-lg text-slate-900 group-hover:text-indigo-600 transition-colors">
                {{ getActionLabel(log.actionType) }}
              </p>
              <p class="text-[10px] font-mono text-slate-400 mt-1 uppercase tracking-widest font-bold">
                Hash: {{ log.currentHash ? log.currentHash.substring(0, 16) : '' }}...
              </p>
            </div>
          </div>
          <div class="text-right hidden sm:block">
            <p class="text-sm font-bold text-slate-500">{{ formatDate(log.eventTime) }}</p>
            <p class="text-xs font-black text-slate-900 mt-1">{{ log.city }} {{ log.province }}</p>
          </div>
        </div>

        <div class="flex flex-col gap-4 bg-slate-50/80 p-5 rounded-[24px] border border-slate-100/50">
          <!-- Operator Row -->
          <div class="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-2">
            <span class="text-[10px] font-black text-slate-400 uppercase tracking-widest shrink-0">Operator</span>
            <span class="font-bold text-slate-900 text-sm sm:text-base break-all sm:text-right">{{ log.operator }}</span>
          </div>
          
          <!-- Route Flow Row -->
          <div v-if="log.fromNode || log.toNode" class="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-2 pt-3 border-t border-slate-100">
            <span class="text-[10px] font-black text-slate-400 uppercase tracking-widest shrink-0">Route Flow</span>
            <div class="flex flex-wrap items-center sm:justify-end gap-2 font-bold text-slate-900 text-sm sm:text-base">
                <span class="whitespace-nowrap">{{ log.fromNode || 'Start' }}</span>
                <span class="text-indigo-400">→</span> 
                <span class="whitespace-nowrap">{{ log.toNode || 'End' }}</span>
            </div>
          </div>

          <!-- Remark Row -->
          <div v-if="log.remark" class="flex flex-col sm:flex-row sm:justify-between sm:items-start gap-2 pt-3 border-t border-slate-100">
            <span class="text-[10px] font-black text-slate-400 uppercase tracking-widest shrink-0">Remark</span>
            <span class="font-medium text-slate-700 text-sm sm:text-right break-words">{{ log.remark }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
