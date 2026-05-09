<script setup>
import { computed } from 'vue'
import { Search, X } from 'lucide-vue-next'
import KbdShortcut from '@/shared/components/ui/KbdShortcut.vue'

/**
 * UserSearchFilter —— Linear-light 用户管理筛选条。
 *
 * 视觉契约：与 PartSearchFilter / TraceList 的 .search-box + .filter-chip 同源；32px 高、8px 圆角、1px hairline。
 *  - 关键词输入：模糊搜索 username，支持回车提交
 *  - 角色 chip select：基于 GET /api/roles 注入；空值 = 所有角色
 *  - 状态 chip select：全部 / 启用 (1) / 禁用 (0) 三态
 *  - 清空按钮：username/roleId/status 任一非默认时显示
 *
 * 接口：v-model:username / v-model:role-id / v-model:status + @search 触发回车 / @reset 清空。
 */
const props = defineProps({
  username: { type: String, default: '' },
  roleId: { type: [String, Number], default: '' },
  status: { type: [String, Number], default: '' },
  roles: { type: Array, default: () => [] },
  total: { type: Number, default: 0 }
})

const emit = defineEmits([
  'update:username',
  'update:roleId',
  'update:status',
  'search',
  'reset'
])

const hasFilter = computed(
  () => props.username !== '' || props.roleId !== '' || props.status !== ''
)

function onUsername(e) {
  emit('update:username', e.target.value)
}
function onRole(e) {
  const value = e.target.value
  emit('update:roleId', value === '' ? '' : Number(value))
  emit('search')
}
function onStatus(e) {
  const value = e.target.value
  emit('update:status', value === '' ? '' : Number(value))
  emit('search')
}
function onEnter() {
  emit('search')
}
function onReset() {
  emit('reset')
}
</script>

<template>
  <section class="user-filter" data-testid="user-filter">
    <div class="user-filter__search" data-testid="user-filter-search-box">
      <Search class="user-filter__search-icon" />
      <input
        :value="username"
        class="user-filter__search-input"
        type="text"
        placeholder="搜索用户名"
        spellcheck="false"
        autocomplete="off"
        data-testid="user-filter-username"
        @input="onUsername"
        @keydown.enter.prevent="onEnter"
      />
      <KbdShortcut keys="Enter" />
    </div>

    <select
      :value="roleId"
      class="user-filter__chip"
      :class="{ 'user-filter__chip--has-val': roleId !== '' }"
      data-testid="user-filter-role"
      @change="onRole"
    >
      <option value="">角色 · 全部</option>
      <option v-for="r in roles" :key="r.id" :value="r.id">
        角色 · {{ r.roleName || r.role_name }}
      </option>
    </select>

    <select
      :value="status"
      class="user-filter__chip"
      :class="{ 'user-filter__chip--has-val': status !== '' }"
      data-testid="user-filter-status"
      @change="onStatus"
    >
      <option value="">状态 · 全部</option>
      <option :value="1">状态 · 启用</option>
      <option :value="0">状态 · 禁用</option>
    </select>

    <button
      v-if="hasFilter"
      type="button"
      class="user-filter__reset"
      data-testid="user-filter-reset"
      @click="onReset"
    >
      <X class="user-filter__reset-icon" />
      清空
    </button>

    <span class="user-filter__count" data-testid="user-filter-count">
      <strong>{{ total.toLocaleString() }}</strong> 位匹配
    </span>
  </section>
</template>

<style scoped>
.user-filter {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.user-filter__search {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 32px;
  padding: 0 10px;
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  border-radius: 8px;
  min-width: 280px;
  flex: 1 1 280px;
  max-width: 380px;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.user-filter__search:focus-within {
  border-color: var(--primary-focus, #5e69d1);
  box-shadow: 0 0 0 3px rgba(94, 106, 210, 0.15);
}
.user-filter__search-icon {
  width: 13px;
  height: 13px;
  color: var(--ink-tertiary);
  flex-shrink: 0;
}
.user-filter__search-input {
  flex: 1 1 auto;
  border: 0;
  outline: none;
  background: transparent;
  font: inherit;
  font-size: 13px;
  color: var(--ink);
  min-width: 0;
}
.user-filter__search-input::placeholder {
  color: var(--ink-tertiary);
}

.user-filter__chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 10px;
  border-radius: 8px;
  background: var(--surface-1);
  border: 1px solid var(--hairline);
  font-size: 13px;
  color: var(--ink-muted);
  font-weight: 500;
  cursor: pointer;
  appearance: none;
  font-family: inherit;
}
.user-filter__chip:hover {
  border-color: var(--ink-subtle);
}
.user-filter__chip--has-val {
  color: var(--ink);
  background: var(--surface-2);
  border-color: var(--hairline-strong);
}

.user-filter__reset {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 32px;
  padding: 0 10px;
  border-radius: 8px;
  background: transparent;
  border: 1px solid transparent;
  font-size: 12.5px;
  color: var(--ink-subtle);
  cursor: pointer;
  font-family: inherit;
}
.user-filter__reset:hover {
  background: var(--surface-2);
  color: var(--ink);
}
.user-filter__reset-icon {
  width: 12px;
  height: 12px;
}

.user-filter__count {
  margin-left: auto;
  font-size: 12.5px;
  color: var(--ink-subtle);
}
.user-filter__count strong {
  color: var(--ink);
  font-weight: 600;
}

@media (max-width: 640px) {
  .user-filter__search {
    min-width: 100%;
    flex-basis: 100%;
    max-width: none;
  }
  .user-filter__count {
    width: 100%;
    margin-left: 0;
  }
}
</style>
