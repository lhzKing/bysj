<script setup>
import { Search, X } from 'lucide-vue-next'
import KbdShortcut from '@/shared/components/ui/KbdShortcut.vue'

/**
 * PartSearchFilter —— Linear-light 配件管理筛选条。
 *
 * 视觉契约：与 TraceList 的 .search-box + .filter-chip 同源；32px 高、8px 圆角、1px hairline。
 *  - 关键词输入：搜索 partCode / partName，支持回车提交
 *  - 类型 select：基于 GET /api/parts/types 注入
 *  - 厂商 select：基于 GET /api/parts/manufacturers 注入
 *  - 清空按钮：keyword/partType/manufacturer 任一非空时显示
 *
 * 接口：v-model="keyword" / v-model:partType / v-model:manufacturer + @search 触发回车 / @reset 清空。
 */
defineProps({
  keyword: { type: String, default: '' },
  partType: { type: String, default: '' },
  manufacturer: { type: String, default: '' },
  types: { type: Array, default: () => [] },
  manufacturers: { type: Array, default: () => [] },
  total: { type: Number, default: 0 }
})

const emit = defineEmits([
  'update:keyword',
  'update:partType',
  'update:manufacturer',
  'search',
  'reset'
])

function onKeyword(e) {
  emit('update:keyword', e.target.value)
}
function onPartType(e) {
  emit('update:partType', e.target.value)
  emit('search')
}
function onManufacturer(e) {
  emit('update:manufacturer', e.target.value)
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
  <section class="part-filter" data-testid="part-filter">
    <div class="part-filter__search" data-testid="part-filter-search-box">
      <Search class="part-filter__search-icon" />
      <input
        :value="keyword"
        class="part-filter__search-input"
        type="text"
        placeholder="搜索配件编码 / 名称"
        spellcheck="false"
        autocomplete="off"
        data-testid="part-filter-keyword"
        @input="onKeyword"
        @keydown.enter.prevent="onEnter"
      />
      <KbdShortcut keys="Enter" />
    </div>

    <select
      :value="partType"
      class="part-filter__chip"
      :class="{ 'part-filter__chip--has-val': partType }"
      data-testid="part-filter-type"
      @change="onPartType"
    >
      <option value="">类型 · 全部</option>
      <option v-for="t in types" :key="t" :value="t">类型 · {{ t }}</option>
    </select>

    <select
      :value="manufacturer"
      class="part-filter__chip"
      :class="{ 'part-filter__chip--has-val': manufacturer }"
      data-testid="part-filter-manufacturer"
      @change="onManufacturer"
    >
      <option value="">厂商 · 全部</option>
      <option v-for="m in manufacturers" :key="m" :value="m">厂商 · {{ m }}</option>
    </select>

    <button
      v-if="keyword || partType || manufacturer"
      type="button"
      class="part-filter__reset"
      data-testid="part-filter-reset"
      @click="onReset"
    >
      <X class="part-filter__reset-icon" />
      清空
    </button>

    <span class="part-filter__count" data-testid="part-filter-count">
      <strong>{{ total.toLocaleString() }}</strong> 条匹配
    </span>
  </section>
</template>

<style scoped>
.part-filter {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.part-filter__search {
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
.part-filter__search:focus-within {
  border-color: var(--primary-focus, #5e69d1);
  box-shadow: 0 0 0 3px rgba(94, 106, 210, 0.15);
}
.part-filter__search-icon {
  width: 13px;
  height: 13px;
  color: var(--ink-tertiary);
  flex-shrink: 0;
}
.part-filter__search-input {
  flex: 1 1 auto;
  border: 0;
  outline: none;
  background: transparent;
  font: inherit;
  font-size: 13px;
  color: var(--ink);
  min-width: 0;
}
.part-filter__search-input::placeholder {
  color: var(--ink-tertiary);
}

.part-filter__chip {
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
.part-filter__chip:hover {
  border-color: var(--ink-subtle);
}
.part-filter__chip--has-val {
  color: var(--ink);
  background: var(--surface-2);
  border-color: var(--hairline-strong);
}

.part-filter__reset {
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
.part-filter__reset:hover {
  background: var(--surface-2);
  color: var(--ink);
}
.part-filter__reset-icon {
  width: 12px;
  height: 12px;
}

.part-filter__count {
  margin-left: auto;
  font-size: 12.5px;
  color: var(--ink-subtle);
}
.part-filter__count strong {
  color: var(--ink);
  font-weight: 600;
}

@media (max-width: 640px) {
  .part-filter__search {
    min-width: 100%;
    flex-basis: 100%;
    max-width: none;
  }
  .part-filter__count {
    width: 100%;
    margin-left: 0;
  }
}
</style>
