<script setup>
import PrimeInput from '@/shared/components/prime/PrimeInput.vue'

defineOptions({
  inheritAttrs: false
})

defineProps({
  modelValue: [String, Number],
  label: String,
  placeholder: String,
  type: {
    type: String,
    default: 'text'
  },
  error: String,
  disabled: Boolean,
  icon: [Object, Function],
  inputId: String
})

defineEmits(['update:modelValue'])
</script>

<template>
  <PrimeInput
    v-bind="$attrs"
    :modelValue="modelValue"
    :label="label"
    :placeholder="placeholder"
    :type="type"
    :error="error"
    :disabled="disabled"
    :icon="icon"
    :inputId="inputId"
    @update:modelValue="$emit('update:modelValue', $event)"
  />
</template>
