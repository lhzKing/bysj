<script setup>
import { MapPin, Clock, User } from 'lucide-vue-next'
import dayjs from 'dayjs'

const props = defineProps({
  snapshot: {
    type: Object,
    required: true
  }
})

const formatDate = (date) => (date ? dayjs(date).format('YYYY-MM-DD HH:mm:ss') : '-')
</script>

<template>
  <div class="grid grid-cols-1 md:grid-cols-3 gap-6 premium-card rounded-[40px] p-8 border-none shadow-sm">
    <div class="flex items-start gap-4">
      <div class="p-3 bg-indigo-50 rounded-2xl text-indigo-600 shadow-sm shadow-indigo-100">
        <MapPin class="w-6 h-6" />
      </div>
      <div>
        <p class="text-[10px] font-black text-slate-400 uppercase tracking-widest">当前位置</p>
        <p class="mt-1 font-black text-slate-900 text-lg">{{ snapshot.city || '-' }} {{ snapshot.province }}</p>
        <p class="text-xs font-bold text-indigo-600 mt-0.5">{{ snapshot.currentNode || '未知节点' }}</p>
      </div>
    </div>

    <div class="flex items-start gap-4">
      <div class="p-3 bg-emerald-50 rounded-2xl text-emerald-600 shadow-sm shadow-emerald-100">
        <Clock class="w-6 h-6" />
      </div>
      <div>
        <p class="text-[10px] font-black text-slate-400 uppercase tracking-widest">最后更新</p>
        <p class="mt-1 font-black text-slate-900 text-lg">{{ formatDate(snapshot.lastEventTime) }}</p>
      </div>
    </div>

    <div class="flex items-start gap-4">
      <div class="p-3 bg-slate-50 rounded-2xl text-slate-600 shadow-sm shadow-slate-100">
        <User class="w-6 h-6" />
      </div>
      <div>
        <p class="text-[10px] font-black text-slate-400 uppercase tracking-widest">持有者</p>
        <p class="mt-1 font-black text-slate-900 text-lg">{{ snapshot.currentOwner || '-' }}</p>
      </div>
    </div>
  </div>
</template>
