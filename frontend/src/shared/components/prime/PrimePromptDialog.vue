<script setup>
import { computed } from 'vue'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import PrimeButton from '@/shared/components/prime/PrimeButton.vue'
import { usePrompt } from '@/shared/composables/usePrompt'

const {
  visible,
  title,
  message,
  inputValue,
  inputType,
  placeholder,
  error,
  confirmText,
  cancelText,
  confirm,
  cancel
} = usePrompt()

const isPassword = computed(() => inputType.value === 'password')
</script>

<template>
  <Dialog
    v-model:visible="visible"
    modal
    :header="title"
    :dismissableMask="true"
    :breakpoints="{ '960px': '32rem', '640px': 'calc(100vw - 2rem)' }"
    @hide="cancel"
  >
    <div class="space-y-4">
      <p v-if="message" class="text-sm text-slate-600">{{ message }}</p>

      <Password
        v-if="isPassword"
        v-model="inputValue"
        :feedback="false"
        toggleMask
        fluid
        :invalid="Boolean(error)"
        :placeholder="placeholder"
        @keyup.enter="confirm"
      />
      <InputText
        v-else
        v-model="inputValue"
        fluid
        :invalid="Boolean(error)"
        :placeholder="placeholder"
        @keyup.enter="confirm"
      />

      <p v-if="error" class="text-xs text-red-600">{{ error }}</p>
    </div>

    <template #footer>
      <PrimeButton data-test="prompt-cancel" variant="outline" @click="cancel">
        {{ cancelText }}
      </PrimeButton>
      <PrimeButton data-test="prompt-confirm" @click="confirm">
        {{ confirmText }}
      </PrimeButton>
    </template>
  </Dialog>
</template>
