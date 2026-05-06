<script setup>
import { Plus } from 'lucide-vue-next'
import Button from 'primevue/button'

const emit = defineEmits(['create'])
</script>

<template>
  <div class="flex items-center justify-between">
    <h1 class="text-2xl font-semibold text-text-primary">角色管理</h1>
    <Button 
      @click="emit('create')"
      class="!bg-text-primary !text-white hover:!bg-black border-none gap-2 px-4 py-2"
    >
      <Plus class="w-4 h-4" />
      <span>新建角色</span>
    </Button>
  </div>
</template>
