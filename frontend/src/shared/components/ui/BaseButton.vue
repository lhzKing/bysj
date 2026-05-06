<script setup>
import PrimeButton from '@/shared/components/prime/PrimeButton.vue'

defineOptions({
  inheritAttrs: false
})

defineProps({
  variant: {
    type: String,
    default: 'primary'
  },
  size: {
    type: String,
    default: 'md'
  },
  block: Boolean,
  loading: Boolean,
  disabled: Boolean,
  class: [String, Array, Object]
})
</script>

<template>
  <PrimeButton
    v-bind="$attrs"
    :variant="variant"
    :size="size"
    :block="block"
    :loading="loading"
    :disabled="disabled"
    :class="$props.class"
  >
    <slot />
  </PrimeButton>
</template>
