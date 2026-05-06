<script setup>
import PrimeCard from '@/shared/components/prime/PrimeCard.vue'

defineOptions({
  inheritAttrs: false
})

defineProps({
  class: [String, Array, Object],
  noPadding: Boolean
})
</script>

<template>
  <PrimeCard v-bind="$attrs" :class="$props.class" :noPadding="noPadding">
    <template v-if="$slots.header" #header>
      <slot name="header" />
    </template>
    <template v-if="$slots.title" #title>
      <slot name="title" />
    </template>
    <template v-if="$slots.subtitle" #subtitle>
      <slot name="subtitle" />
    </template>

    <slot />

    <template v-if="$slots.footer" #footer>
      <slot name="footer" />
    </template>
  </PrimeCard>
</template>
