<script setup>
import { onBeforeUnmount, onMounted } from 'vue'
import Toast from 'primevue/toast'
import { useToast as usePrimeToast } from 'primevue/usetoast'
import { onToastMessage } from '@/shared/composables/useToast'

const toast = usePrimeToast()
let stopListening = null

onMounted(() => {
  stopListening = onToastMessage((message) => {
    toast.add(message)
  })
})

onBeforeUnmount(() => {
  stopListening?.()
})
</script>

<template>
  <Toast group="app-toast" position="top-center" />
</template>
