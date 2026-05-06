<script setup>
import { computed } from 'vue'
import Skeleton from 'primevue/skeleton'

const props = defineProps({
  type: {
    type: String,
    default: 'default',
    validator: (value) => ['card', 'table', 'chart', 'kpi', 'detail', 'list', 'default'].includes(value)
  },
  rows: {
    type: Number,
    default: 5
  },
  count: {
    type: Number,
    default: 1
  }
})

const rowIndexes = computed(() => Array.from({ length: props.rows }, (_, index) => index))
const cardIndexes = computed(() => Array.from({ length: props.count }, (_, index) => index))
const chartHeights = ['35%', '58%', '42%', '76%', '54%']
</script>

<template>
  <div class="space-y-4">
    <template v-if="type === 'card'">
      <div
        v-for="cardIndex in cardIndexes"
        :key="cardIndex"
        data-test="skeleton-card"
        class="rounded-xl border border-slate-200 bg-white p-6 shadow-sm"
      >
        <Skeleton width="40%" height="1rem" class="mb-4" />
        <Skeleton width="28%" height="2rem" class="mb-3" />
        <Skeleton width="100%" height="0.75rem" />
      </div>
    </template>

    <div v-else-if="type === 'table'" class="rounded-xl border border-slate-200 bg-white shadow-sm">
      <div class="border-b border-slate-200 p-4">
        <div class="mb-4 flex items-center justify-between">
          <Skeleton width="8rem" height="2rem" />
          <Skeleton width="6rem" height="2.5rem" />
        </div>
      </div>
      <div class="p-4">
        <div
          v-for="row in rowIndexes"
          :key="row"
          data-test="skeleton-row"
          class="flex items-center justify-between border-b border-slate-100 py-4 last:border-b-0"
        >
          <div class="flex-1 space-y-2">
            <Skeleton width="70%" height="1rem" />
            <Skeleton width="44%" height="0.75rem" />
          </div>
          <div class="ml-4 flex gap-2">
            <Skeleton width="4rem" height="2rem" />
            <Skeleton width="4rem" height="2rem" />
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="type === 'chart'" class="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <Skeleton width="25%" height="1.25rem" class="mb-4" />
      <div class="flex h-64 items-end justify-between gap-3 rounded-lg bg-slate-50 p-4">
        <Skeleton
          v-for="(height, index) in chartHeights"
          :key="index"
          width="12%"
          :height="height"
        />
      </div>
    </div>

    <div v-else-if="type === 'kpi'" class="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-4">
      <div
        v-for="cardIndex in 4"
        :key="cardIndex"
        class="rounded-xl border border-slate-200 bg-white p-6 shadow-sm"
      >
        <div class="mb-4 flex items-start justify-between">
          <Skeleton width="6rem" height="1rem" />
          <Skeleton shape="circle" size="2.75rem" />
        </div>
        <Skeleton width="7rem" height="2rem" class="mb-2" />
        <Skeleton width="4rem" height="0.75rem" />
      </div>
    </div>

    <div
      v-else-if="type === 'detail'"
      class="space-y-6 rounded-xl border border-slate-200 bg-white p-6 shadow-sm"
    >
      <div class="border-b border-slate-200 pb-4">
        <Skeleton width="34%" height="2rem" class="mb-2" />
        <Skeleton width="46%" height="0.875rem" />
      </div>
      <div class="grid grid-cols-1 gap-6 md:grid-cols-2">
        <div v-for="fieldIndex in 6" :key="fieldIndex" class="space-y-2">
          <Skeleton width="5rem" height="0.75rem" />
          <Skeleton width="100%" height="1.25rem" />
        </div>
      </div>
      <div class="border-t border-slate-200 pt-4">
        <Skeleton width="7rem" height="1.25rem" class="mb-4" />
        <Skeleton width="100%" height="12rem" />
      </div>
    </div>

    <div v-else-if="type === 'list'" class="space-y-4">
      <div
        v-for="row in rowIndexes"
        :key="row"
        class="rounded-xl border border-slate-200 bg-white p-4 shadow-sm"
      >
        <div class="flex items-center gap-4">
          <Skeleton shape="circle" size="3rem" />
          <div class="flex-1 space-y-2">
            <Skeleton width="72%" height="1rem" />
            <Skeleton width="48%" height="0.75rem" />
          </div>
          <Skeleton width="5rem" height="2rem" />
        </div>
      </div>
    </div>

    <div v-else class="space-y-3">
      <Skeleton width="75%" height="1rem" />
      <Skeleton width="100%" height="1rem" />
      <Skeleton width="84%" height="1rem" />
    </div>
  </div>
</template>
