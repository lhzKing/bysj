<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Plus, RefreshCw } from 'lucide-vue-next'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import PageHeader from '@/shared/components/ui/PageHeader.vue'
import { useConfirm } from '@/shared/composables/useConfirm'
import { usePrompt } from '@/shared/composables/usePrompt'
import { useToast } from '@/shared/composables/useToast'
import {
  batchDeleteUsers,
  createUser,
  deleteUser,
  getRoles,
  getSelectableTraceNodes,
  getUserTraceNodes,
  getUsers,
  replaceUserTraceNodes,
  resetUserPassword,
  updateUser,
  updateUserStatus
} from '@/features/user/api'
import UserEditDialog from '../components/UserEditDialog.vue'
import UserNodeBindingDialog from '../components/UserNodeBindingDialog.vue'
import UserSearchFilter from '../components/UserSearchFilter.vue'
import UserTable from '../components/UserTable.vue'

const PAGE_SIZE = 10

const users = ref([])
const roles = ref([])
const loading = ref(false)
const saving = ref(false)
const total = ref(0)
const hasMore = ref(false)
const selectedIds = ref([])

const query = reactive({
  username: '',
  roleId: '',
  status: '',
  page: 1,
  size: PAGE_SIZE
})

const showDialog = ref(false)
const editingUser = ref(null)
const formData = reactive({
  username: '',
  password: '',
  roleId: '',
  status: 1
})

// Trace-node binding dialog state
const showBindingDialog = ref(false)
const bindingUser = ref(null)
const bindingLoading = ref(false)
const bindingSaving = ref(false)
const allTraceNodes = ref([])
const currentBindings = ref([])

const { confirm } = useConfirm()
const { prompt } = usePrompt()
const toast = useToast()

function resetFormData() {
  Object.assign(formData, {
    username: '',
    password: '',
    roleId: '',
    status: 1
  })
}

async function loadData() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.username.trim()) params.username = query.username.trim()
    if (query.roleId !== '' && query.roleId !== null) params.roleId = query.roleId
    if (query.status !== '' && query.status !== null) params.status = query.status
    const res = await getUsers(params)
    users.value = Array.isArray(res?.list) ? res.list : Array.isArray(res) ? res : []
    total.value = typeof res?.total === 'number' ? res.total : users.value.length
    hasMore.value = users.value.length >= query.size && query.page * query.size < total.value
  } catch (error) {
    users.value = []
    total.value = 0
    hasMore.value = false
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  try {
    const res = await getRoles()
    roles.value = Array.isArray(res) ? res : []
  } catch (error) {
    /* request.js already toasted */
  }
}

function handleSearch() {
  query.page = 1
  loadData()
}

function handleReset() {
  query.username = ''
  query.roleId = ''
  query.status = ''
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
  editingUser.value = null
  resetFormData()
  showDialog.value = true
}

function handleEdit(user) {
  editingUser.value = user
  Object.assign(formData, {
    username: user.username || '',
    password: '',
    roleId: user.roleId ?? user.role_id ?? '',
    status: typeof user.status === 'number' ? user.status : 1
  })
  showDialog.value = true
}

async function handleSave() {
  if (!formData.username?.trim()) {
    toast.error('请输入用户名')
    return
  }
  if (formData.username.trim().length < 3 || formData.username.trim().length > 50) {
    toast.error('用户名长度必须在 3-50 个字符之间')
    return
  }
  if (!editingUser.value && !formData.password) {
    toast.error('创建用户时必须输入密码')
    return
  }
  if (formData.password) {
    if (formData.password.length < 6 || formData.password.length > 100) {
      toast.error('密码长度必须在 6-100 个字符之间')
      return
    }
    if (!/[a-zA-Z]/.test(formData.password) || !/\d/.test(formData.password)) {
      toast.error('密码必须包含字母和数字')
      return
    }
  }
  if (formData.roleId === '' || formData.roleId === null || formData.roleId === undefined) {
    toast.error('请选择角色')
    return
  }

  saving.value = true
  try {
    if (editingUser.value) {
      const payload = {
        username: formData.username.trim(),
        roleId: formData.roleId,
        status: formData.status
      }
      if (formData.password) payload.password = formData.password
      await updateUser(editingUser.value.id, payload)
      toast.success('用户已更新')
    } else {
      await createUser({
        username: formData.username.trim(),
        password: formData.password,
        roleId: formData.roleId,
        status: formData.status
      })
      toast.success('用户已创建')
    }
    showDialog.value = false
    resetFormData()
    editingUser.value = null
    loadData()
  } catch (error) {
    /* request.js already toasted (e.g. 403 越权 / 409 用户名重复) */
  } finally {
    saving.value = false
  }
}

async function handleDelete(user) {
  const ok = await confirm({
    title: '删除用户',
    message: `确定要删除用户「${user.username}」吗？此操作不可恢复。superadmin 账号与高优先级账号会被后端拒绝（403）。`,
    confirmText: '删除',
    cancelText: '取消',
    type: 'danger'
  })
  if (!ok) return

  try {
    await deleteUser(user.id)
    toast.success('用户已删除')
    selectedIds.value = selectedIds.value.filter((id) => id !== user.id)
    if (users.value.length === 1 && query.page > 1) {
      query.page -= 1
    }
    loadData()
  } catch (error) {
    /* request.js 拦截器已 toast */
  }
}

async function handleBatchDelete() {
  if (!selectedIds.value.length) return
  const ok = await confirm({
    title: `批量删除 ${selectedIds.value.length} 位用户`,
    message: 'superadmin 与权限不足的账号会被后端自动跳过；返回值为实际删除数量。',
    confirmText: '批量删除',
    cancelText: '取消',
    type: 'danger'
  })
  if (!ok) return

  try {
    const removed = await batchDeleteUsers([...selectedIds.value])
    const count = typeof removed === 'number' ? removed : selectedIds.value.length
    toast.success(`已删除 ${count} 位用户`)
    selectedIds.value = []
    if (query.page > 1 && users.value.length === 0) {
      query.page -= 1
    }
    loadData()
  } catch (error) {
    /* request.js already toasted */
  }
}

async function handleEnable(user) {
  try {
    await updateUserStatus(user.id, 1)
    toast.success(`用户「${user.username}」已启用`)
    loadData()
  } catch (error) {
    /* request.js already toasted */
  }
}

async function handleDisable(user) {
  const ok = await confirm({
    title: '禁用用户',
    message: `禁用后「${user.username}」无法登录系统，已颁发的 Token 将在下次请求时失效。superadmin 账号不能被禁用。`,
    confirmText: '禁用',
    cancelText: '取消',
    type: 'warning'
  })
  if (!ok) return

  try {
    await updateUserStatus(user.id, 0)
    toast.success(`用户「${user.username}」已禁用`)
    loadData()
  } catch (error) {
    /* request.js already toasted */
  }
}

async function handleResetPassword(user) {
  const newPass = await prompt({
    title: '重置密码',
    message: `为「${user.username}」生成新密码（提交后立即生效，老密码失效）`,
    type: 'password',
    placeholder: '6-100 字符，必须包含字母和数字',
    validator: (value) => {
      if (!value) return '密码不能为空'
      if (value.length < 6 || value.length > 100) return '密码长度必须在 6-100 个字符之间'
      if (!/[a-zA-Z]/.test(value) || !/\d/.test(value)) return '密码必须包含字母和数字'
      return true
    }
  })
  if (!newPass) return

  try {
    await resetUserPassword(user.id, newPass)
    toast.success(`「${user.username}」密码已重置`)
  } catch (error) {
    /* request.js already toasted */
  }
}

async function handleManageNodes(user) {
  bindingUser.value = user
  showBindingDialog.value = true
  bindingLoading.value = true
  currentBindings.value = []
  try {
    // Fetch in parallel: full selectable node pool + this user's current bindings.
    const [pool, mine] = await Promise.all([
      allTraceNodes.value.length ? Promise.resolve(allTraceNodes.value) : getSelectableTraceNodes(),
      getUserTraceNodes(user.id)
    ])
    allTraceNodes.value = Array.isArray(pool) ? pool : []
    currentBindings.value = Array.isArray(mine) ? mine : []
  } catch (error) {
    /* request.js already toasted; dialog stays open showing empty state */
  } finally {
    bindingLoading.value = false
  }
}

async function handleSaveBinding(payload) {
  if (!bindingUser.value) return
  bindingSaving.value = true
  try {
    await replaceUserTraceNodes(bindingUser.value.id, payload)
    toast.success(`「${bindingUser.value.username}」节点绑定已更新（${payload.nodeIds.length} 个节点）`)
    showBindingDialog.value = false
    bindingUser.value = null
    currentBindings.value = []
  } catch (error) {
    /* request.js already toasted (403 越权、400 节点 id 非法、500 服务端等) */
  } finally {
    bindingSaving.value = false
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
  loadRoles()
})
</script>

<template>
  <div class="user-list">
    <PageHeader
      title="用户管理"
      :subtitle="loading
        ? '加载中…'
        : `${total.toLocaleString()} 位操作员 · 第 ${query.page} 页 · 列表按角色优先级自动过滤；superadmin 账号不可删除/禁用`"
    >
      <template #actions>
        <BaseButton
          variant="secondary"
          size="sm"
          data-testid="user-list-refresh"
          :loading="loading"
          @click="loadData"
        >
          <template #icon><RefreshCw class="user-list__btn-icon" /></template>
          刷新
        </BaseButton>
        <BaseButton
          variant="primary"
          size="sm"
          data-testid="user-list-create"
          @click="handleCreate"
        >
          <template #icon><Plus class="user-list__btn-icon" /></template>
          新建用户
        </BaseButton>
      </template>
    </PageHeader>

    <UserSearchFilter
      v-model:username="query.username"
      v-model:role-id="query.roleId"
      v-model:status="query.status"
      :roles="roles"
      :total="total"
      @search="handleSearch"
      @reset="handleReset"
    />

    <UserTable
      :users="users"
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
      @reset-password="handleResetPassword"
      @manage-nodes="handleManageNodes"
      @page-change="handlePageChange"
      @create="handleCreate"
      @update:selected="handleSelectionChange"
      @batch-delete="handleBatchDelete"
      @clear-selection="handleClearSelection"
    />

    <UserEditDialog
      v-model:visible="showDialog"
      :editing-user="editingUser"
      :form-data="formData"
      :roles="roles"
      :saving="saving"
      @save="handleSave"
    />

    <UserNodeBindingDialog
      v-model:visible="showBindingDialog"
      :user="bindingUser"
      :all-nodes="allTraceNodes"
      :current-bindings="currentBindings"
      :loading="bindingLoading"
      :saving="bindingSaving"
      @save="handleSaveBinding"
    />
  </div>
</template>

<style scoped>
.user-list {
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px 24px 48px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.user-list__btn-icon {
  width: 13px;
  height: 13px;
}

@media (max-width: 640px) {
  .user-list {
    padding: 16px 12px 32px;
  }
}
</style>
