<script setup>
import { computed, useAttrs } from 'vue'
import Button from 'primevue/button'
import { clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

const props = defineProps({
  label: String,
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
  icon: String,
  iconPos: {
    type: String,
    default: 'left'
  },
  class: [String, Array, Object]
})

const attrs = useAttrs()

const sizeMap = {
  sm: 'small',
  md: undefined,
  lg: 'large'
}

const severityMap = {
  primary: undefined,
  secondary: 'secondary',
  outline: 'secondary',
  ghost: 'secondary',
  danger: 'danger'
}

const primeVariant = computed(() => {
  if (props.variant === 'outline') return 'outlined'
  if (props.variant === 'ghost') return 'text'

  return undefined
})

const classes = computed(() =>
  twMerge(
    clsx('prime-base-button', props.block && 'w-full', props.class)
  )
)
</script>

<template>
  <Button
    v-bind="attrs"
    :label="label"
    :icon="icon"
    :iconPos="iconPos"
    :loading="loading"
    :disabled="disabled || loading"
    :size="sizeMap[size]"
    :severity="severityMap[variant]"
    :variant="primeVariant"
    :fluid="block"
    :class="classes"
  >
    <slot />
  </Button>
</template>
