<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Plus, RefreshCw } from 'lucide-vue-next'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import PageHeader from '@/shared/components/ui/PageHeader.vue'
import { useUserStore } from '@/core/stores/user'
import { useConfirm } from '@/shared/composables/useConfirm'
import { useToast } from '@/shared/composables/useToast'
import {
  assignPermissions,
  createRole,
  deleteRole,
  getPermissions,
  getRole,
  getRoles,
  updateRole
} from '@/features/user/api'
import RoleEditDialog from '../components/RoleEditDialog.vue'
import RoleSearchFilter from '../components/RoleSearchFilter.vue'
import RoleTable from '../components/RoleTable.vue'
import PermissionAssignDialog from '../components/PermissionAssignDialog.vue'

const ROLE_PRIORITY = {
  SUPER_ADMIN: 3,
  ADMIN: 2,
  PRODUCER: 1,
  WAREHOUSE: 1,
  LOGISTICS: 1,
  USER: 1
}

const SYSTEM_ROLE_CODES = new Set(['SUPER_ADMIN', 'ADMIN', 'PRODUCER', 'WAREHOUSE', 'LOGISTICS', 'USER'])
const PROTECTED_PERMISSION_PREFIXES = ['user:', 'role:']

const roles = ref([])
const allPermissions = ref([])
const loading = ref(false)
const saving = ref(false)

const showDialog = ref(false)
const showPermissionDialog = ref(false)
const editingRole = ref(null)

const selectedRole = ref(null)
const selectedPermissions = ref([])

const filter = reactive({ keyword: '', scope: '' })

const userStore = useUserStore()
const { confirm } = useConfirm()
const toast = useToast()

const currentPermissions = computed(() => {
  if (Array.isArray(userStore.permissions)) {
    return userStore.permissions
  }
  return userStore.user?.permissions || []
})

const currentRoleCode = computed(() => userStore.user?.roleCode || userStore.user?.role || '')
const canManageRoles = computed(() => currentPermissions.value.includes('role:manage'))
const isSuperAdmin = computed(() => currentRoleCode.value === 'SUPER_ADMIN')

const getRolePriority = (roleCode) => ROLE_PRIORITY[roleCode] ?? 0
const isSystemRole = (roleCode) => SYSTEM_ROLE_CODES.has(roleCode)
const isProtectedPermissionCode = (permCode) =>
  typeof permCode === 'string' && PROTECTED_PERMISSION_PREFIXES.some((prefix) => permCode.startsWith(prefix))

const canManageRole = (role) => {
  if (!canManageRoles.value || !role?.roleCode) return false
  return getRolePriority(currentRoleCode.value) > getRolePriority(role.roleCode)
}

const canEditRole = (role) => canManageRole(role)
const canAssignPermissionsToRole = (role) => canManageRole(role)
const canDeleteRole = (role) => canManageRole(role) && !isSystemRole(role?.roleCode)

const roleRows = computed(() =>
  roles.value.map((role) => ({
    ...role,
    __guard: {
      canEdit: canEditRole(role),
      canAssignPermissions: canAssignPermissionsToRole(role),
      canDelete: canDeleteRole(role),
      isProtected: !canManageRole(role) || isSystemRole(role.roleCode)
    }
  }))
)

const filteredRoles = computed(() => {
  const kw = filter.keyword.trim().toLowerCase()
  const scope = filter.scope
  return roleRows.value.filter((role) => {
    if (scope === 'system' && !isSystemRole(role.roleCode)) return false
    if (scope === 'custom' && isSystemRole(role.roleCode)) return false
    if (!kw) return true
    const hay = `${role.roleName || ''} ${role.roleCode || ''} ${role.remark || ''}`.toLowerCase()
    return hay.includes(kw)
  })
})

const assignablePermissions = computed(() => {
  if (isSuperAdmin.value) return allPermissions.value
  return allPermissions.value.filter((permission) => !isProtectedPermissionCode(permission.permCode))
})

async function loadData() {
  loading.value = true
  try {
    const res = await getRoles()
    roles.value = Array.isArray(res) ? res : []
  } catch (error) {
    /* request.js already toasted */
  } finally {
    loading.value = false
  }
}

async function loadPermissions() {
  try {
    const res = await getPermissions()
    allPermissions.value = Array.isArray(res) ? res : []
  } catch (error) {
    /* request.js already toasted */
  }
}

function handleSearch() {
  /* filter is computed-only, nothing to call */
}

function handleReset() {
  filter.keyword = ''
  filter.scope = ''
}

function handleCreate() {
  if (!canManageRoles.value) {
    toast.error('当前账号没有创建角色的权限')
    return
  }
  editingRole.value = null
  showDialog.value = true
}

function handleEdit(role) {
  if (!canEditRole(role)) {
    toast.error(`当前角色无权修改 ${role?.roleName || role?.roleCode || '该角色'}`)
    return
  }
  editingRole.value = role
  showDialog.value = true
}

async function handleSave(formData) {
  if (!formData?.roleName) {
    toast.error('请填写角色名称')
    return
  }
  if (!editingRole.value && !formData.roleCode) {
    toast.error('请填写角色编码')
    return
  }
  if (editingRole.value && !canEditRole(editingRole.value)) {
    toast.error(`当前角色无权修改 ${editingRole.value.roleName || editingRole.value.roleCode || '该角色'}`)
    return
  }

  saving.value = true
  try {
    if (editingRole.value) {
      await updateRole(editingRole.value.id, {
        roleName: formData.roleName,
        remark: formData.remark || ''
      })
      toast.success('角色已更新')
    } else {
      await createRole({
        roleCode: formData.roleCode,
        roleName: formData.roleName,
        remark: formData.remark || ''
      })
      toast.success('角色已创建')
    }
    showDialog.value = false
    editingRole.value = null
    await loadData()
  } catch (error) {
    /* request.js already toasted (e.g. 403 越权 / 409 编码重复) */
  } finally {
    saving.value = false
  }
}

async function handleDelete(role) {
  if (!canDeleteRole(role)) {
    toast.error(`当前角色无权删除 ${role?.roleName || role?.roleCode || '该角色'}`)
    return
  }

  const ok = await confirm({
    title: '删除角色',
    message: `确定要删除角色「${role.roleName}」吗？此操作不可恢复，已绑定该角色的用户也会被后端拒绝（409）。`,
    confirmText: '删除',
    cancelText: '取消',
    type: 'danger'
  })
  if (!ok) return

  try {
    await deleteRole(role.id)
    toast.success('角色已删除')
    await loadData()
  } catch (error) {
    /* request.js already toasted */
  }
}

async function handleAssignPermissions(role) {
  if (!canAssignPermissionsToRole(role)) {
    toast.error(`当前角色无权分配 ${role?.roleName || role?.roleCode || '该角色'} 的权限`)
    return
  }

  try {
    const detail = await getRole(role.id)
    if (!isSuperAdmin.value && (detail?.permissions || []).some((permission) => isProtectedPermissionCode(permission.permCode))) {
      toast.error('仅 SUPER_ADMIN 可管理包含用户/角色管理权限的角色')
      return
    }

    selectedRole.value = { ...role, ...detail }
    selectedPermissions.value = (detail?.permissions || []).map((permission) => permission.id)
    showPermissionDialog.value = true
  } catch (error) {
    toast.error('角色详情加载失败，请稍后重试')
  }
}

async function handleSavePermissions() {
  if (!selectedRole.value) return
  if (!canAssignPermissionsToRole(selectedRole.value)) {
    toast.error(`当前角色无权分配 ${selectedRole.value.roleName || selectedRole.value.roleCode || '该角色'} 的权限`)
    return
  }

  saving.value = true
  try {
    await assignPermissions(selectedRole.value.id, selectedPermissions.value)
    toast.success('权限已分配')
    showPermissionDialog.value = false
    await loadData()
  } catch (error) {
    /* request.js already toasted */
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadData()
  loadPermissions()
})
</script>

<template>
  <div class="role-list">
    <PageHeader
      title="角色管理"
      :subtitle="loading
        ? '加载中…'
        : `${roles.length.toLocaleString()} 个角色 · 系统预置 6 个不可删除；可根据业务自定义新角色并分配权限`"
    >
      <template #actions>
        <BaseButton
          variant="secondary"
          size="sm"
          data-testid="role-list-refresh"
          :loading="loading"
          @click="loadData"
        >
          <template #icon><RefreshCw class="role-list__btn-icon" /></template>
          刷新
        </BaseButton>
        <BaseButton
          v-if="canManageRoles"
          variant="primary"
          size="sm"
          data-testid="role-list-create"
          @click="handleCreate"
        >
          <template #icon><Plus class="role-list__btn-icon" /></template>
          新建角色
        </BaseButton>
      </template>
    </PageHeader>

    <RoleSearchFilter
      v-model:keyword="filter.keyword"
      v-model:scope="filter.scope"
      :total="roleRows.length"
      :matched="filteredRoles.length"
      @search="handleSearch"
      @reset="handleReset"
    />

    <RoleTable
      :roles="filteredRoles"
      :loading="loading"
      :total="roleRows.length"
      :matched="filteredRoles.length"
      @edit="handleEdit"
      @delete="handleDelete"
      @assign-permissions="handleAssignPermissions"
      @create="handleCreate"
    />

    <RoleEditDialog
      v-model:visible="showDialog"
      :editing-role="editingRole"
      :saving="saving"
      @save="handleSave"
    />

    <PermissionAssignDialog
      v-model:visible="showPermissionDialog"
      v-model:selected-permissions="selectedPermissions"
      :role="selectedRole"
      :all-permissions="assignablePermissions"
      :saving="saving"
      @save="handleSavePermissions"
    />
  </div>
</template>

<style scoped>
.role-list {
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px 24px 48px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.role-list__btn-icon {
  width: 13px;
  height: 13px;
}

@media (max-width: 640px) {
  .role-list {
    padding: 16px 12px 32px;
  }
}
</style>
