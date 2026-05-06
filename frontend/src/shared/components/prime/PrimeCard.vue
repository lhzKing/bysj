<script setup>
import { computed } from 'vue'
import Card from 'primevue/card'
import { clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

const props = defineProps({
  title: String,
  subtitle: String,
  noPadding: Boolean,
  variant: {
    type: String,
    default: 'default'
  },
  contentClass: [String, Array, Object],
  class: [String, Array, Object]
})

const rootClass = computed(() =>
  twMerge(
    clsx(
      'prime-base-card overflow-hidden border border-slate-200 shadow-sm',
      props.variant === 'muted' ? 'bg-slate-50' : 'bg-white',
      props.class
    )
  )
)

const contentClassName = computed(() =>
  twMerge(
    clsx(!props.noPadding && 'p-6', props.contentClass)
  )
)
</script>

<template>
  <Card
    :class="rootClass"
    :pt="{
      body: { class: 'p-0' },
      caption: { class: 'px-6 pt-5' },
      content: { class: contentClassName },
      footer: { class: 'px-6 pb-5 pt-0' }
    }"
  >
    <template v-if="$slots.header" #header>
      <slot name="header" />
    </template>

    <template v-if="$slots.title || title" #title>
      <slot name="title">{{ title }}</slot>
    </template>

    <template v-if="$slots.subtitle || subtitle" #subtitle>
      <slot name="subtitle">{{ subtitle }}</slot>
    </template>

    <template #content>
      <slot />
    </template>

    <template v-if="$slots.footer" #footer>
      <slot name="footer" />
    </template>
  </Card>
</template>
