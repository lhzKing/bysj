<script setup>
import { computed } from 'vue'
import { Boxes, CheckCircle2, MapPin, Navigation, PackageOpen, XCircle, User } from 'lucide-vue-next'
import dayjs from 'dayjs'

const props = defineProps({
  history: {
    type: Array,
    required: true
  },
  view: {
    type: String,
    default: 'effective'
  }
})

const actionMap = {
  INIT: { label: '生产赋码', color: 'bg-emerald-50 text-emerald-600 border-emerald-200', icon: CheckCircle2 },
  INBOUND: { label: '入库', color: 'bg-indigo-50 text-indigo-600 border-indigo-200', icon: MapPin },
  OUTBOUND: { label: '出库', color: 'bg-indigo-50 text-indigo-600 border-indigo-200', icon: MapPin },
  TRANSFER: { label: '转移', color: 'bg-cyan-50 text-cyan-600 border-cyan-200', icon: Navigation },
  PRINT_CODE: { label: '打印标签', color: 'bg-emerald-50 text-emerald-600 border-emerald-200', icon: CheckCircle2 },
  REPRINT_CODE: { label: '重打标签', color: 'bg-amber-50 text-amber-600 border-amber-200', icon: CheckCircle2 },
  VOID_CODE: { label: '作废标签', color: 'bg-rose-50 text-rose-600 border-rose-200', icon: XCircle },
  ACTIVATE_CODE: { label: '扫码激活', color: 'bg-emerald-50 text-emerald-600 border-emerald-200', icon: CheckCircle2 },
  PACK: { label: '装箱', color: 'bg-cyan-50 text-cyan-600 border-cyan-200', icon: Boxes },
  UNPACK: { label: '拆箱', color: 'bg-amber-50 text-amber-600 border-amber-200', icon: PackageOpen },
  PALLETIZE: { label: '托盘绑定', color: 'bg-cyan-50 text-cyan-600 border-cyan-200', icon: Boxes },
  UNPALLETIZE: { label: '托盘解绑', color: 'bg-amber-50 text-amber-600 border-amber-200', icon: PackageOpen },
  EXCEPTION: { label: '异常', color: 'bg-rose-50 text-rose-600 border-rose-200', icon: XCircle },
  CORRECTION: { label: '修正', color: 'bg-amber-50 text-amber-600 border-amber-200', icon: User }
}

const formatDate = (date) => (date ? dayjs(date).format('YYYY-MM-DD HH:mm:ss') : '-')

const correctedLogIds = computed(() => new Set(
  props.history
    .map((log) => log.correctionOf)
    .filter((id) => id !== null && id !== undefined)
    .map((id) => String(id))
))

const isAuditView = computed(() => props.view === 'audit')

const isCorrectedOriginal = (log) => isAuditView.value && correctedLogIds.value.has(String(log.id))

const isCorrectionRecord = (log) => log.correctionOf !== null && log.correctionOf !== undefined

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

      <div
        class="premium-card rounded-[32px] p-6 hover:shadow-xl transition-all duration-500 border group"
        :class="isCorrectedOriginal(log) ? 'bg-amber-50/60 border-amber-200 opacity-90' : 'bg-white/40 border-transparent hover:border-slate-200'"
        :data-corrected-original="isCorrectedOriginal(log) ? 'true' : undefined"
        :data-correction-record="isCorrectionRecord(log) ? 'true' : undefined"
      >
        <div class="flex items-start justify-between mb-6">
          <div class="flex items-center gap-4">
            <div :class="['p-3 rounded-2xl border', getActionColor(log.actionType)]">
              <component :is="getActionIcon(log.actionType)" class="w-6 h-6" />
            </div>
            <div>
              <div class="flex flex-wrap items-center gap-2">
                <p class="font-black text-lg text-slate-900 group-hover:text-indigo-600 transition-colors">
                  {{ getActionLabel(log.actionType) }}
                </p>
                <span
                  v-if="isCorrectedOriginal(log)"
                  class="px-2.5 py-1 rounded-full text-[10px] font-black bg-amber-100 text-amber-700 border border-amber-200"
                >
                  已被纠错覆盖
                </span>
                <span
                  v-if="isCorrectionRecord(log)"
                  class="px-2.5 py-1 rounded-full text-[10px] font-black bg-indigo-50 text-indigo-600 border border-indigo-100"
                >
                  修正 #{{ log.correctionOf }}
                </span>
              </div>
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
          <!-- Audit Correction Row -->
          <div v-if="isAuditView && (isCorrectedOriginal(log) || isCorrectionRecord(log))" class="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-2 rounded-2xl bg-white/70 px-4 py-3 border border-amber-100">
            <span class="text-[10px] font-black text-amber-500 uppercase tracking-widest shrink-0">Audit Marker</span>
            <span v-if="isCorrectedOriginal(log)" class="font-bold text-amber-700 text-sm sm:text-right">原始日志 #{{ log.id }} 已被后续 CORRECTION 覆盖，业务有效视图默认隐藏</span>
            <span v-else class="font-bold text-indigo-700 text-sm sm:text-right">本记录修正原始日志 #{{ log.correctionOf }}</span>
          </div>

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

          <!-- Correction Link Row -->
          <div v-if="isAuditView && isCorrectionRecord(log)" class="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-2 pt-3 border-t border-slate-100">
            <span class="text-[10px] font-black text-slate-400 uppercase tracking-widest shrink-0">Correction Of</span>
            <span class="font-mono font-black text-indigo-600 text-sm sm:text-right">#{{ log.correctionOf }}</span>
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
