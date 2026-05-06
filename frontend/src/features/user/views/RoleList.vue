<script setup>
import { computed, onMounted, ref } from 'vue'
import { useUserStore } from '@/core/stores/user'
import { getRoles, getRole, deleteRole, createRole, updateRole, getPermissions, assignPermissions } from '@/features/user/api'
import LoadingSkeleton from '@/shared/components/ui/LoadingSkeleton.vue'
import { useConfirm } from '@/shared/composables/useConfirm'
import { useToast } from '@/shared/composables/useToast'
import RoleTable from '@/features/user/components/RoleTable.vue'
import RoleEditDialog from '@/features/user/components/RoleEditDialog.vue'
import PermissionAssignDialog from '@/features/user/components/PermissionAssignDialog.vue'

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

const showDialog = ref(false)
const showPermissionDialog = ref(false)
const editingRole = ref(null)

const selectedRole = ref(null)
const selectedPermissions = ref([])

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
  if (!canManageRoles.value || !role?.roleCode) {
    return false
  }
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

const assignablePermissions = computed(() => {
  if (isSuperAdmin.value) {
    return allPermissions.value
  }
  return allPermissions.value.filter((permission) => !isProtectedPermissionCode(permission.permCode))
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getRoles()
    roles.value = res || []
  } catch (error) {
    console.error('Failed to load roles:', error)
  } finally {
    loading.value = false
  }
}

const loadPermissions = async () => {
  try {
    const res = await getPermissions()
    allPermissions.value = res || []
  } catch (error) {
    console.error('Failed to load permissions:', error)
  }
}

const handleCreate = () => {
  if (!canManageRoles.value) {
    toast.error('当前账号没有创建角色的权限')
    return
  }
  editingRole.value = null
  showDialog.value = true
}

const handleEdit = (role) => {
  if (!canEditRole(role)) {
    toast.error(`当前角色无权修改 ${role?.roleName || role?.roleCode || '该角色'}`)
    return
  }
  editingRole.value = role
  showDialog.value = true
}

const handleSave = async (formData) => {
  if (!formData.roleCode || !formData.roleName) {
    toast.error('请填写必填项：角色代码、角色名称')
    return
  }

  if (editingRole.value && !canEditRole(editingRole.value)) {
    toast.error(`当前角色无权修改 ${editingRole.value.roleName || editingRole.value.roleCode || '该角色'}`)
    return
  }

  try {
    if (editingRole.value) {
      await updateRole(editingRole.value.id, formData)
      toast.success('角色更新成功')
    } else {
      await createRole(formData)
      toast.success('角色创建成功')
    }
    showDialog.value = false
    await loadData()
  } catch (error) {
    console.error('Save error:', error)
  }
}

const handleDelete = async (role) => {
  if (!canDeleteRole(role)) {
    toast.error(`当前角色无权删除 ${role?.roleName || role?.roleCode || '该角色'}`)
    return
  }

  const confirmed = await confirm({
    title: '删除角色',
    message: `确定要删除角色"${role.roleName}"吗？此操作不可恢复。`,
    type: 'danger'
  })

  if (!confirmed) return

  try {
    await deleteRole(role.id)
    toast.success('角色删除成功')
    await loadData()
  } catch (error) {
    console.error('Delete error:', error)
  }
}

const handleAssignPermissions = async (role) => {
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
    selectedPermissions.value = (detail?.permissions || []).map(permission => permission.id)
    showPermissionDialog.value = true
  } catch (error) {
    console.error('Load role detail error:', error)
    toast.error('角色详情加载失败，请稍后重试')
  }
}

const handleSavePermissions = async () => {
  if (!selectedRole.value) return

  if (!canAssignPermissionsToRole(selectedRole.value)) {
    toast.error(`当前角色无权分配 ${selectedRole.value.roleName || selectedRole.value.roleCode || '该角色'} 的权限`)
    return
  }

  try {
    await assignPermissions(selectedRole.value.id, selectedPermissions.value)
    toast.success('权限分配成功')
    showPermissionDialog.value = false
    await loadData()
  } catch (error) {
    console.error('Assign permissions error:', error)
  }
}

onMounted(() => {
  loadData()
  loadPermissions()
})
</script>

<template>
  <div class="space-y-8 relative z-10">
    <div class="relative mb-12">
      <div class="absolute -left-12 -top-12 size-40 bg-emerald-200 rounded-full blur-[80px] opacity-30"></div>
      <div class="flex flex-col md:flex-row md:items-end justify-between gap-6 relative z-10">
        <div>
          <h1 class="text-5xl font-extrabold tracking-tight text-slate-900 leading-[1.1]">
              神经元 <span class="text-emerald-600">访问控制</span>
          </h1>
          <p class="text-lg text-slate-500 mt-4 max-w-2xl font-medium leading-relaxed">
              Neural Access Control. 制定数字化生命周期的权限拓扑。
          </p>
        </div>
        <button
          v-if="canManageRoles"
          @click="handleCreate"
          class="px-8 py-4 bg-emerald-500 hover:bg-emerald-600 text-white rounded-2xl shadow-xl shadow-emerald-200 font-bold transition-all flex items-center justify-center hover:scale-105 active:scale-95"
        >
          <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M12 4v16m8-8H4"></path></svg>
          <span>配置新角色拓扑</span>
        </button>
      </div>
    </div>

    <LoadingSkeleton v-if="loading && roles.length === 0" type="table" :rows="4" />

    <RoleTable 
      v-else
      :roles="roleRows"
      :loading="loading"
      @edit="handleEdit"
      @delete="handleDelete"
      @assign-permissions="handleAssignPermissions"
    />

    <RoleEditDialog
      v-model:visible="showDialog"
      :editingRole="editingRole"
      @save="handleSave"
    />

    <PermissionAssignDialog
      v-model:visible="showPermissionDialog"
      v-model:selectedPermissions="selectedPermissions"
      :role="selectedRole"
      :allPermissions="assignablePermissions"
      @save="handleSavePermissions"
    />
  </div>
</template>
