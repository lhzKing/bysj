<script setup>
import PrimeLoadingSkeleton from '@/shared/components/prime/PrimeLoadingSkeleton.vue'

defineOptions({
  inheritAttrs: false
})

defineProps({
  type: {
    type: String,
    default: 'default'
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
</script>

<template>
  <PrimeLoadingSkeleton v-bind="$attrs" :type="type" :rows="rows" :count="count" />
</template>
