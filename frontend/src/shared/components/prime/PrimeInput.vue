<script setup>
import { computed } from 'vue'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import { clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

const props = defineProps({
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

const emit = defineEmits(['update:modelValue'])

const value = computed({
  get: () => props.modelValue ?? '',
  set: (nextValue) => emit('update:modelValue', nextValue)
})

const isPassword = computed(() => props.type === 'password')

const inputClass = computed(() =>
  twMerge(
    clsx('w-full', props.icon && 'pl-10', props.error && 'p-invalid')
  )
)
</script>

<template>
  <div class="space-y-1.5">
    <label v-if="label" :for="inputId" class="block text-sm font-medium text-slate-700">
      {{ label }}
    </label>

    <div class="relative">
      <div
        v-if="icon"
        class="pointer-events-none absolute inset-y-0 left-3 flex items-center text-slate-400"
      >
        <component :is="icon" :size="18" />
      </div>

      <Password
        v-if="isPassword"
        v-model="value"
        :inputId="inputId"
        :placeholder="placeholder"
        :disabled="disabled"
        :feedback="false"
        toggleMask
        fluid
        :invalid="Boolean(error)"
        :inputClass="inputClass"
      />
      <InputText
        v-else
        v-model="value"
        :id="inputId"
        :placeholder="placeholder"
        :disabled="disabled"
        fluid
        :invalid="Boolean(error)"
        :class="inputClass"
      />
    </div>

    <p v-if="error" class="text-xs text-red-600">{{ error }}</p>
  </div>
</template>
