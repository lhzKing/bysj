<script setup>
import { ref, onMounted, reactive } from 'vue'
import { getUsers, deleteUser, batchDeleteUsers, updateUserStatus, resetUserPassword, createUser, updateUser } from '@/features/user/api'
import { getRoles } from '@/features/user/api'
import BaseButton from '@/shared/components/ui/BaseButton.vue'
import { useConfirm } from '@/shared/composables/useConfirm'
import { usePrompt } from '@/shared/composables/usePrompt'
import { useToast } from '@/shared/composables/useToast'
import { Plus } from 'lucide-vue-next'

import UserSearchFilter from '../components/UserSearchFilter.vue'
import UserTable from '../components/UserTable.vue'
import UserEditDialog from '../components/UserEditDialog.vue'

const users = ref([])
const roles = ref([])
const loading = ref(false)
const query = reactive({
  username: '',
  roleId: '',
  status: '',
  page: 1,
  size: 10
})
const total = ref(0)

const showDialog = ref(false)
const editingUser = ref(null)

const { confirm } = useConfirm()
const { prompt } = usePrompt()
const toast = useToast()

const loadData = async () => {
  loading.value = true
  try {
    const res = await getUsers(query)
    users.value = Array.isArray(res?.list) ? res.list : Array.isArray(res) ? res : []
    total.value = typeof res?.total === 'number' ? res.total : users.value.length
  } catch (error) {
    console.error('Failed to load users:', error)
    users.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const loadRoles = async () => {
  const res = await getRoles()
  roles.value = Array.isArray(res) ? res : []
}

const handleSearch = () => {
  query.page = 1
  loadData()
}

const handleDelete = async (id) => {
  const confirmed = await confirm({
    title: '删除用户',
    message: '确定要删除该用户吗？此操作不可恢复。'
  })
  if (!confirmed) return
  try {
    await deleteUser(id)
    toast.success('删除成功')
    loadData()
  } catch (error) {
    toast.error('删除失败')
  }
}

const handleStatusChange = async (user) => {
  const newStatus = user.status === 1 ? 0 : 1
  await updateUserStatus(user.id, newStatus)
  user.status = newStatus
}

const handleCreate = () => {
  editingUser.value = null
  showDialog.value = true
}

const handleEdit = (user) => {
  editingUser.value = user
  showDialog.value = true
}

const handleSave = async (formData) => {
  try {
    if (editingUser.value) {
      const updateData = {
        username: formData.username,
        roleId: formData.roleId,
        status: formData.status
      }
      if (formData.password) {
        updateData.password = formData.password
      }
      await updateUser(editingUser.value.id, updateData)
      toast.success('用户更新成功')
    } else {
      await createUser(formData)
      toast.success('用户创建成功')
    }
    showDialog.value = false
    loadData()
  } catch (error) {
    console.error('Save error:', error)
  }
}

const handleResetPassword = async (id) => {
  const newPass = await prompt({
    title: '重置密码',
    message: '密码要求：',
    type: 'password',
    placeholder: '密码必须包含字母和数字，长度6-100个字符',
    validator: (value) => {
      if (!value) return '密码不能为空'
      if (value.length < 6 || value.length > 100) return '密码长度必须在6-100个字符之间'
      if (!/[a-zA-Z]/.test(value) || !/\d/.test(value)) return '密码必须包含字母和数字'
      return true
    }
  })
  if (newPass) {
    try {
      await resetUserPassword(id, newPass)
      toast.success('密码重置成功')
    } catch (error) {
      toast.error('密码重置失败')
    }
  }
}

const handlePageChange = (newPage) => {
  query.page = newPage
  loadData()
}

onMounted(() => {
  loadData()
  loadRoles()
})
</script>

<template>
  <div class="space-y-8 relative z-10">
    <div class="relative mb-12">
      <div class="absolute -left-12 -top-12 size-40 bg-indigo-200 rounded-full blur-[80px] opacity-30"></div>
      <div class="flex flex-col md:flex-row md:items-end justify-between gap-6 relative z-10">
        <div>
          <h1 class="text-5xl font-extrabold tracking-tight text-slate-900 leading-[1.1]">
              数字生态 <span class="text-indigo-600">操作员</span>
          </h1>
          <p class="text-lg text-slate-500 mt-4 max-w-2xl font-medium leading-relaxed">
              Neural Operators. 维护供应链操作者的数字身份与权限网络。
          </p>
        </div>
        <button @click="handleCreate" class="px-8 py-4 bg-indigo-600 hover:bg-indigo-700 text-white rounded-2xl shadow-xl shadow-indigo-200 font-bold transition-all flex items-center justify-center hover:scale-105 active:scale-95">
          <Plus class="w-5 h-5 mr-2" />
          <span>配置新操作员</span>
        </button>
      </div>
    </div>

    <UserSearchFilter 
      v-model:query="query" 
      :roles="roles"
      @search="handleSearch"
    />

    <UserTable 
      :users="users"
      :loading="loading"
      :total="total"
      :query="query"
      @edit="handleEdit"
      @resetPassword="handleResetPassword"
      @statusChange="handleStatusChange"
      @delete="handleDelete"
      @pageChange="handlePageChange"
    />

    <UserEditDialog 
      v-model:visible="showDialog"
      :editingUser="editingUser"
      :roles="roles"
      @save="handleSave"
    />
  </div>
</template>
