<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Plus, RefreshCw } from 'lucide-vue-next'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import PageHeader from '@/shared/components/ui/PageHeader.vue'
import { useConfirm } from '@/shared/composables/useConfirm'
import { useToast } from '@/shared/composables/useToast'
import {
  batchDeleteParts,
  createPart,
  deletePart,
  disablePart,
  enablePart,
  getManufacturers,
  getPartTypes,
  getParts,
  updatePart
} from '@/features/part/api'
import PartEditDialog from '../components/PartEditDialog.vue'
import PartSearchFilter from '../components/PartSearchFilter.vue'
import PartTable from '../components/PartTable.vue'

const PAGE_SIZE = 10

const parts = ref([])
const types = ref([])
const manufacturers = ref([])
const loading = ref(false)
const saving = ref(false)
const total = ref(0)
const hasMore = ref(false)
const selectedIds = ref([])

const query = reactive({
  keyword: '',
  partType: '',
  manufacturer: '',
  enabled: '',
  page: 1,
  size: PAGE_SIZE
})

const showDialog = ref(false)
const editingPart = ref(null)
const formData = reactive({
  partCode: '',
  partName: '',
  partType: '',
  manufacturer: '',
  model: '',
  unit: '',
  remark: ''
})

const { confirm } = useConfirm()
const toast = useToast()

function resetFormData() {
  Object.assign(formData, {
    partCode: '',
    partName: '',
    partType: '',
    manufacturer: '',
    model: '',
    unit: '',
    remark: ''
  })
}

async function loadData() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.keyword.trim()) params.keyword = query.keyword.trim()
    if (query.partType) params.partType = query.partType
    if (query.manufacturer) params.manufacturer = query.manufacturer
    if (query.enabled === 'true') params.enabled = true
    else if (query.enabled === 'false') params.enabled = false
    const res = await getParts(params)
    parts.value = res?.list || []
    total.value = res?.total || 0
    hasMore.value = parts.value.length >= query.size && query.page * query.size < total.value
  } catch (error) {
    parts.value = []
    total.value = 0
    hasMore.value = false
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  try {
    const [t, m] = await Promise.all([getPartTypes(), getManufacturers()])
    types.value = Array.isArray(t) ? t : []
    manufacturers.value = Array.isArray(m) ? m : []
  } catch (error) {
    /* request.js already toasted; keep options empty */
  }
}

function handleSearch() {
  query.page = 1
  loadData()
}

function handleReset() {
  query.keyword = ''
  query.partType = ''
  query.manufacturer = ''
  query.enabled = ''
  query.page = 1
  loadData()
}

function handlePageChange(delta) {
  const next = query.page + delta
  if (next < 1) return
  query.page = next
  loadData()
}

function handleCreate() {
  editingPart.value = null
  resetFormData()
  showDialog.value = true
}

function handleEdit(part) {
  editingPart.value = part
  Object.assign(formData, {
    partCode: part.partCode || '',
    partName: part.partName || '',
    partType: part.partType || '',
    manufacturer: part.manufacturer || '',
    model: part.model || '',
    unit: part.unit || '',
    remark: part.remark || ''
  })
  showDialog.value = true
}

async function handleSave() {
  if (!formData.partCode?.trim() || !formData.partName?.trim() || !formData.partType?.trim()) {
    toast.error('请填写必填项：配件编码、名称、类型')
    return
  }

  saving.value = true
  try {
    const payload = {
      partCode: formData.partCode.trim(),
      partName: formData.partName.trim(),
      partType: formData.partType.trim(),
      manufacturer: formData.manufacturer?.trim() || undefined,
      model: formData.model?.trim() || undefined,
      unit: formData.unit?.trim() || undefined,
      remark: formData.remark?.trim() || undefined
    }
    if (editingPart.value) {
      await updatePart(editingPart.value.id, payload)
      toast.success('配件已更新')
    } else {
      await createPart(payload)
      toast.success('配件已创建')
    }
    showDialog.value = false
    resetFormData()
    editingPart.value = null
    loadData()
    loadOptions()
  } catch (error) {
    /* request.js already toasted; keep dialog open for user to fix input */
  } finally {
    saving.value = false
  }
}

async function handleDelete(part) {
  const ok = await confirm({
    title: '删除配件',
    message: `确定要删除「${part.partName}」(${part.partCode}) 吗？此操作不可恢复。若该配件已参与溯源记录，删除会被后端拒绝（HTTP 409），届时可改用“禁用”软下线。`,
    confirmText: '删除',
    cancelText: '取消',
    type: 'danger'
  })
  if (!ok) return

  try {
    await deletePart(part.id)
    toast.success('配件已删除')
    selectedIds.value = selectedIds.value.filter((id) => id !== part.id)
    if (parts.value.length === 1 && query.page > 1) {
      query.page -= 1
    }
    loadData()
  } catch (error) {
    /* request.js 拦截器已 toast 后端 message：例如 "配件已参与溯源记录，不能删除: ids=[2]" */
  }
}

async function handleBatchDelete() {
  if (!selectedIds.value.length) return
  const ok = await confirm({
    title: `批量删除 ${selectedIds.value.length} 条配件`,
    message: `选中的配件将被永久删除。若任一配件已参与溯源，整批将被后端拒绝（HTTP 409），届时可改用“禁用”软下线。`,
    confirmText: '批量删除',
    cancelText: '取消',
    type: 'danger'
  })
  if (!ok) return

  try {
    await batchDeleteParts([...selectedIds.value])
    toast.success(`已删除 ${selectedIds.value.length} 条配件`)
    selectedIds.value = []
    if (query.page > 1 && parts.value.length === 0) {
      query.page -= 1
    }
    loadData()
  } catch (error) {
    /* request.js 已 toast 后端 message */
  }
}

async function handleEnable(part) {
  try {
    await enablePart(part.id)
    toast.success(`配件「${part.partName}」已启用`)
    loadData()
  } catch (error) {
    /* request.js already toasted */
  }
}

async function handleDisable(part) {
  const ok = await confirm({
    title: '禁用配件',
    message: `禁用后「${part.partName}」(${part.partCode}) 将不能用于新的生产赋码与扫码流程，但历史溯源数据保留不变。可随时启用恢复。`,
    confirmText: '禁用',
    cancelText: '取消',
    type: 'warning'
  })
  if (!ok) return

  try {
    await disablePart(part.id)
    toast.success(`配件「${part.partName}」已禁用`)
    loadData()
  } catch (error) {
    /* request.js already toasted */
  }
}

function handleSelectionChange(ids) {
  selectedIds.value = ids
}

function handleClearSelection() {
  selectedIds.value = []
}

onMounted(() => {
  loadData()
  loadOptions()
})
</script>

<template>
  <div class="part-list">
    <PageHeader
      title="配件管理"
      :subtitle="loading
        ? '加载中…'
        : `${total.toLocaleString()} 个 SPU · 第 ${query.page} 页 · 已参与溯源的配件不可删除，可改用禁用`"
    >
      <template #actions>
        <BaseButton
          variant="secondary"
          size="sm"
          data-testid="part-list-refresh"
          :loading="loading"
          @click="loadData"
        >
          <template #icon><RefreshCw class="part-list__btn-icon" /></template>
          刷新
        </BaseButton>
        <BaseButton
          variant="primary"
          size="sm"
          data-testid="part-list-create"
          @click="handleCreate"
        >
          <template #icon><Plus class="part-list__btn-icon" /></template>
          新建配件
        </BaseButton>
      </template>
    </PageHeader>

    <PartSearchFilter
      v-model:keyword="query.keyword"
      v-model:part-type="query.partType"
      v-model:manufacturer="query.manufacturer"
      v-model:enabled="query.enabled"
      :types="types"
      :manufacturers="manufacturers"
      :total="total"
      @search="handleSearch"
      @reset="handleReset"
    />

    <PartTable
      :parts="parts"
      :loading="loading"
      :total="total"
      :page="query.page"
      :size="query.size"
      :has-more="hasMore"
      :selected="selectedIds"
      @edit="handleEdit"
      @delete="handleDelete"
      @enable="handleEnable"
      @disable="handleDisable"
      @page-change="handlePageChange"
      @create="handleCreate"
      @update:selected="handleSelectionChange"
      @batch-delete="handleBatchDelete"
      @clear-selection="handleClearSelection"
    />

    <PartEditDialog
      v-model:visible="showDialog"
      :editing-part="editingPart"
      :form-data="formData"
      :types="types"
      :manufacturers="manufacturers"
      :saving="saving"
      @save="handleSave"
    />
  </div>
</template>

<style scoped>
.part-list {
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px 24px 48px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.part-list__btn-icon {
  width: 13px;
  height: 13px;
}

@media (max-width: 640px) {
  .part-list {
    padding: 16px 12px 32px;
  }
}
</style>
