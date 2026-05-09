<script setup>
import { computed } from 'vue'
import { Search, X } from 'lucide-vue-next'
import KbdShortcut from '@/shared/components/ui/KbdShortcut.vue'

/**
 * RoleSearchFilter —— Linear-light 角色筛选条。
 *
 * 视觉契约：与 UserSearchFilter / PartSearchFilter 同源；32px 高、8px 圆角、1px hairline。
 *  - 关键词输入：模糊匹配 roleName / roleCode / remark；回车提交（@search emit）
 *  - 类型 chip select：全部 / 系统预置（SYSTEM_ROLE_CODES）/ 自定义
 *  - 清空按钮：keyword/scope 任一非默认时显示
 *  - 右侧 count: <strong>N</strong> 个角色 · 总 M
 *
 * 接口：v-model:keyword / v-model:scope（''/'system'/'custom'）+ @search 触发回车 / @reset 清空。
 */
const props = defineProps({
  keyword: { type: String, default: '' },
  scope: { type: String, default: '' },
  total: { type: Number, default: 0 },
  matched: { type: Number, default: 0 }
})

const emit = defineEmits([
  'update:keyword',
  'update:scope',
  'search',
  'reset'
])

const hasFilter = computed(() => props.keyword !== '' || props.scope !== '')

function onKeyword(e) {
  emit('update:keyword', e.target.value)
}
function onScope(e) {
  emit('update:scope', e.target.value)
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
  <section class="role-filter" data-testid="role-filter">
    <div class="role-filter__search" data-testid="role-filter-search-box">
      <Search class="role-filter__search-icon" />
      <input
        :value="keyword"
        class="role-filter__search-input"
        type="text"
        placeholder="搜索角色名称 / 编码 / 描述"
        spellcheck="false"
        autocomplete="off"
        data-testid="role-filter-keyword"
        @input="onKeyword"
        @keydown.enter.prevent="onEnter"
      />
      <KbdShortcut keys="Enter" />
    </div>

    <select
      :value="scope"
      class="role-filter__chip"
      :class="{ 'role-filter__chip--has-val': scope !== '' }"
      data-testid="role-filter-scope"
      @change="onScope"
    >
      <option value="">类型 · 全部</option>
      <option value="system">类型 · 系统预置</option>
      <option value="custom">类型 · 自定义</option>
    </select>

    <button
      v-if="hasFilter"
      type="button"
      class="role-filter__reset"
      data-testid="role-filter-reset"
      @click="onReset"
    >
      <X class="role-filter__reset-icon" />
      清空
    </button>

    <span class="role-filter__count" data-testid="role-filter-count">
      <strong>{{ matched.toLocaleString() }}</strong> 个匹配
      <span v-if="matched !== total" class="role-filter__count-total">/ 共 {{ total.toLocaleString() }}</span>
    </span>
  </section>
</template>

<style scoped>
.role-filter {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.role-filter__search {
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
.role-filter__search:focus-within {
  border-color: var(--primary-focus, #5e69d1);
  box-shadow: 0 0 0 3px rgba(94, 106, 210, 0.15);
}
.role-filter__search-icon {
  width: 13px;
  height: 13px;
  color: var(--ink-tertiary);
  flex-shrink: 0;
}
.role-filter__search-input {
  flex: 1 1 auto;
  border: 0;
  outline: none;
  background: transparent;
  font: inherit;
  font-size: 13px;
  color: var(--ink);
  min-width: 0;
}
.role-filter__search-input::placeholder {
  color: var(--ink-tertiary);
}

.role-filter__chip {
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
.role-filter__chip:hover {
  border-color: var(--ink-subtle);
}
.role-filter__chip--has-val {
  color: var(--ink);
  background: var(--surface-2);
  border-color: var(--hairline-strong);
}

.role-filter__reset {
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
.role-filter__reset:hover {
  background: var(--surface-2);
  color: var(--ink);
}
.role-filter__reset-icon {
  width: 12px;
  height: 12px;
}

.role-filter__count {
  margin-left: auto;
  font-size: 12.5px;
  color: var(--ink-subtle);
}
.role-filter__count strong {
  color: var(--ink);
  font-weight: 600;
}
.role-filter__count-total {
  color: var(--ink-tertiary);
  margin-left: 4px;
}

@media (max-width: 640px) {
  .role-filter__search {
    min-width: 100%;
    flex-basis: 100%;
    max-width: none;
  }
  .role-filter__count {
    width: 100%;
    margin-left: 0;
  }
}
</style>
