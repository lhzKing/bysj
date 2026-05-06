<script setup>
import { ref, onMounted, reactive } from 'vue'
import { getParts, deletePart, createPart, updatePart, getPartTypes, getManufacturers } from '@/features/part/api'
import { Plus } from 'lucide-vue-next'
import { useConfirm } from '@/shared/composables/useConfirm'
import { useToast } from '@/shared/composables/useToast'
import PartSearchFilter from '../components/PartSearchFilter.vue'
import PartTable from '../components/PartTable.vue'
import PartEditDialog from '../components/PartEditDialog.vue'

const parts = ref([])
const types = ref([])
const manufacturers = ref([])
const loading = ref(false)
const query = reactive({
  keyword: '',
  page: 1,
  size: 10
})
const total = ref(0)
const hasMore = ref(true)

const showDialog = ref(false)
const editingPart = ref(null)
const formData = reactive({
  partCode: '',
  partName: '',
  partType: '',
  manufacturer: '',
  model: ''
})

const { confirm } = useConfirm()
const toast = useToast()

const loadData = async () => {
  loading.value = true
  try {
    const res = await getParts(query)
    parts.value = res.list || []
    total.value = res.total || 0
    hasMore.value = (res.list && res.list.length >= query.size)
  } catch (error) {
    console.error('Failed to load parts:', error)
    hasMore.value = false
  } finally {
    loading.value = false
  }
}

const loadOptions = async () => {
  try {
    types.value = await getPartTypes() || []
    manufacturers.value = await getManufacturers() || []
  } catch (error) {
    console.error('Failed to load options:', error)
  }
}

const handleSearch = () => {
  query.page = 1
  loadData()
}

const handlePageChange = (delta) => {
  query.page += delta
  loadData()
}

const handleCreate = () => {
  editingPart.value = null
  Object.assign(formData, {
    partCode: '',
    partName: '',
    partType: '',
    manufacturer: '',
    model: ''
  })
  showDialog.value = true
}

const handleEdit = (part) => {
  editingPart.value = part
  Object.assign(formData, {
    partCode: part.partCode,
    partName: part.partName,
    partType: part.partType,
    manufacturer: part.manufacturer,
    model: part.model
  })
  showDialog.value = true
}

const handleSave = async () => {
  if (!formData.partCode || !formData.partName || !formData.partType) {
    toast.error('请填写必填项：配件代码、配件名称、配件类型')
    return
  }

  try {
    if (editingPart.value) {
      await updatePart(editingPart.value.id, formData)
      toast.success('配件更新成功')
    } else {
      await createPart(formData)
      toast.success('配件创建成功')
    }
    showDialog.value = false
    loadData()
  } catch (error) {
    console.error('Save error:', error)
  }
}

const handleDelete = async (part) => {
  const confirmed = await confirm({
    title: '删除配件',
    message: `确定要删除配件"${part.partName}"吗？此操作不可恢复。`,
    type: 'danger'
  })
  
  if (!confirmed) return
  
  try {
    await deletePart(part.id)
    toast.success('配件删除成功')
    loadData()
  } catch (error) {
    console.error('Delete error:', error)
  }
}

onMounted(() => {
  loadData()
  loadOptions()
})
</script>

<template>
  <div class="space-y-8 relative z-10">
    <div class="relative mb-12">
      <div class="absolute -left-12 -top-12 size-40 bg-indigo-200 rounded-full blur-[80px] opacity-30"></div>
      <div class="flex flex-col md:flex-row md:items-end justify-between gap-6 relative z-10">
        <div>
          <h1 class="text-5xl font-extrabold tracking-tight text-slate-900 leading-[1.1]">
              智能配件 <span class="text-indigo-600">中枢</span>
          </h1>
          <p class="text-lg text-slate-500 mt-4 max-w-2xl font-medium leading-relaxed">
              Neural Part Inventory. 维护供应链的物理节点元数据与数字档案。
          </p>
        </div>
        <button @click="handleCreate" class="px-8 py-4 bg-indigo-600 hover:bg-indigo-700 text-white rounded-2xl shadow-xl shadow-indigo-200 font-bold transition-all flex items-center justify-center hover:scale-105 active:scale-95">
          <Plus class="w-5 h-5 mr-2" />
          <span>注入新节点</span>
        </button>
      </div>
    </div>

    <PartSearchFilter 
      v-model="query.keyword" 
      @search="handleSearch" 
    />

    <PartTable 
      :parts="parts" 
      :loading="loading" 
      :total="total" 
      :query="query" 
      :hasMore="hasMore"
      @edit="handleEdit"
      @delete="handleDelete"
      @page-change="handlePageChange"
    />

    <PartEditDialog 
      v-model:visible="showDialog"
      :editingPart="editingPart"
      :formData="formData"
      :types="types"
      :manufacturers="manufacturers"
      @save="handleSave"
    />
  </div>
</template>
