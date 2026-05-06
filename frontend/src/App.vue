<script setup>
import { RouterView } from 'vue-router'
import Toast from '@/shared/components/ui/Toast.vue'
import ConfirmDialog from '@/shared/components/ui/ConfirmDialog.vue'
import PromptDialog from '@/shared/components/ui/PromptDialog.vue'
</script>

<template>
  <Toast />
  <ConfirmDialog />
  <PromptDialog />
  <RouterView />
</template>
