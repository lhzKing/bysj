# T07 Transform Contract Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让前端内部统一使用 camelCase，并由 `transform.js` + `request.js` 统一处理与后端 snake_case 的请求/响应转换，清理页面层分散的双格式兼容逻辑。

**Architecture:** 先用 Vitest 锁定通用键名转换、请求/响应契约转换、以及溯源赋码组件的 camelCase 调用链，再实现 `transform.js` 的深层转换能力，并在 `request.js` 拦截器接入统一序列化/反序列化。最后批量清理 feature API、页面与表单中的 snake_case 内部状态和 `snake || camel` 兼容写法，只保留边界层自动转换。

**Tech Stack:** Vue 3、Axios、Vitest、jsdom、Vue Test Utils、Vite

---

## File Map

**Create:**
- `frontend/src/shared/utils/__tests__/transform.test.js` - 锁定通用 key 转换、深层对象转换与旧实体 helper 的回归测试
- `frontend/src/core/api/__tests__/request.test.js` - 锁定请求参数 snake_case 序列化与响应 camelCase 反序列化行为
- `frontend/src/features/trace/components/__tests__/CreateTraceDialog.contract.test.js` - 锁定赋码弹窗对 camelCase parts 数据和 camelCase payload 的调用链
- `frontend/src/features/__tests__/api-contracts.test.js` - 锁定 feature API 对页面层暴露 camelCase 契约

**Modify:**
- `frontend/src/shared/utils/transform.js` - 升级为唯一契约转换层
- `frontend/src/core/api/request.js` - 统一接入请求/响应转换
- `frontend/src/features/trace/components/CreateTraceDialog.vue` - 改为消费 camelCase parts 数据并提交 camelCase payload
- `frontend/src/features/trace/components/ScanFlowDialog.vue` - 改为使用 camelCase 表单状态和请求 payload
- `frontend/src/features/trace/components/InboundForm.vue` - 改为消费 camelCase `fromNode / toNode / eventTime`
- `frontend/src/features/trace/components/OutboundForm.vue` - 改为消费 camelCase `fromNode / toNode / eventTime`
- `frontend/src/features/trace/components/TransferForm.vue` - 改为消费 camelCase `fromNode / toNode / eventTime`
- `frontend/src/features/trace/views/TraceDetail.vue` - 移除局部手写转换，直接消费 request 层 camelCase 数据
- `frontend/src/features/trace/views/TraceList.vue` - 改为提交 camelCase payload、消费 camelCase 返回值
- `frontend/src/features/user/api/users.js` - 注释与调用语义改为 camelCase
- `frontend/src/features/user/api/roles.js` - 注释与调用语义改为 camelCase
- `frontend/src/features/part/api/parts.js` - 注释与调用语义改为 camelCase
- `frontend/src/features/trace/api/trace.js` - 注释与调用语义改为 camelCase
- `frontend/src/features/dashboard/api/dashboard.js` - 注释与调用语义改为 camelCase
- `frontend/src/features/user/views/UserList.vue` - 改为 camelCase 查询对象、表单对象与显示字段
- `frontend/src/features/user/views/RoleList.vue` - 改为 camelCase 表单对象与显示字段
- `frontend/src/features/part/views/PartList.vue` - 改为 camelCase 表单对象与显示字段
- `frontend/src/features/dashboard/views/Dashboard.vue` - 改为 camelCase KPI/拓扑数据字段
- `项目整改执行任务表.md` - 更新 T07 计划检查点、验证结果与状态

**Verify:**
- `frontend/src/shared/utils/__tests__/transform.test.js`
- `frontend/src/core/api/__tests__/request.test.js`
- `frontend/src/features/trace/components/__tests__/CreateTraceDialog.contract.test.js`
- `frontend/src/features/__tests__/api-contracts.test.js`

**Workspace note:** 当前工作区未检测到 `.git`，本计划中的“Commit”统一替换为“更新任务表并保存检查点”。

---

### Task 1: 用 TDD 锁定 `transform.js` 的通用契约转换

**Files:**
- Create: `frontend/src/shared/utils/__tests__/transform.test.js`
- Modify: `frontend/src/shared/utils/transform.js`
- Test: `frontend/src/shared/utils/__tests__/transform.test.js`

- [ ] **Step 1: 先写失败测试，锁定 key 转换、深层转换和旧 helper 回归**

```js
// frontend/src/shared/utils/__tests__/transform.test.js
import { describe, expect, it } from 'vitest'
import {
  toCamelCaseKey,
  toSnakeCaseKey,
  transformArray,
  transformKeysToCamel,
  transformKeysToSnake,
  transformSnapshot,
  transformTraceLog,
  transformUser
} from '@/shared/utils/transform'

describe('transform contract helpers', () => {
  it('converts snake_case keys to camelCase recursively', () => {
    const payload = {
      trace_code: 'TRACE-001',
      nested_data: {
        current_status: 'IN_TRANSIT'
      },
      history: [
        {
          event_time: '2026-04-11T10:00:00',
          from_node: '工厂A'
        }
      ]
    }

    expect(transformKeysToCamel(payload)).toEqual({
      traceCode: 'TRACE-001',
      nestedData: {
        currentStatus: 'IN_TRANSIT'
      },
      history: [
        {
          eventTime: '2026-04-11T10:00:00',
          fromNode: '工厂A'
        }
      ]
    })
  })

  it('converts camelCase keys to snake_case recursively', () => {
    const payload = {
      roleId: 7,
      traceCode: 'TRACE-001',
      nestedData: {
        manufacturerNode: '工厂A'
      }
    }

    expect(transformKeysToSnake(payload)).toEqual({
      role_id: 7,
      trace_code: 'TRACE-001',
      nested_data: {
        manufacturer_node: '工厂A'
      }
    })
  })

  it('keeps special values intact while transforming plain objects', () => {
    const formData = new FormData()
    formData.append('traceCode', 'TRACE-001')
    const now = new Date('2026-04-11T10:00:00')

    const result = transformKeysToSnake({
      formData,
      now,
      list: [{ eventTime: '2026-04-11 10:00:00' }]
    })

    expect(result.form_data).toBe(formData)
    expect(result.now).toBe(now)
    expect(result.list[0]).toEqual({ event_time: '2026-04-11 10:00:00' })
  })

  it('keeps domain helpers returning camelCase data', () => {
    expect(toCamelCaseKey('trace_code')).toBe('traceCode')
    expect(toSnakeCaseKey('manufacturerNode')).toBe('manufacturer_node')

    expect(transformSnapshot({ trace_code: 'TRACE-001', current_status: 'INIT' })).toEqual({
      traceCode: 'TRACE-001',
      currentStatus: 'INIT'
    })

    expect(transformTraceLog({ action_type: 'INBOUND', from_node: '工厂A' })).toEqual({
      actionType: 'INBOUND',
      fromNode: '工厂A'
    })

    expect(transformUser({ role_id: 2, role_name: '管理员', token_version: 3 })).toEqual({
      roleId: 2,
      roleName: '管理员',
      tokenVersion: 3
    })

    expect(transformArray([{ trace_code: 'TRACE-001' }], transformSnapshot)).toEqual([
      { traceCode: 'TRACE-001' }
    ])
  })
})
```

- [ ] **Step 2: 运行测试，确认它先失败**

Run:

```bash
cd frontend && npm run test -- --run src/shared/utils/__tests__/transform.test.js
```

Expected: FAIL，报错缺少 `transformKeysToCamel` / `transformKeysToSnake` / `toCamelCaseKey` 等导出，或旧 helper 仍返回不完整字段。

- [ ] **Step 3: 用最小实现把 `transform.js` 升级为通用契约层**

```js
// frontend/src/shared/utils/transform.js
const CAMEL_SEGMENT_PATTERN = /_([a-z0-9])/g
const SNAKE_SEGMENT_PATTERN = /([a-z0-9])([A-Z])/g

function isRuntimeInstance(value, typeName) {
  return typeof globalThis[typeName] !== 'undefined' && value instanceof globalThis[typeName]
}

export function toCamelCaseKey(key) {
  if (typeof key !== 'string') return key
  return key.replace(CAMEL_SEGMENT_PATTERN, (_, char) => char.toUpperCase())
}

export function toSnakeCaseKey(key) {
  if (typeof key !== 'string') return key
  return key
    .replace(SNAKE_SEGMENT_PATTERN, '$1_$2')
    .replace(/[-\\s]+/g, '_')
    .toLowerCase()
}

function isPlainObject(value) {
  return Object.prototype.toString.call(value) === '[object Object]'
}

function shouldSkipTransform(value) {
  return (
    value == null ||
    value instanceof Date ||
    isRuntimeInstance(value, 'FormData') ||
    isRuntimeInstance(value, 'Blob') ||
    isRuntimeInstance(value, 'File')
  )
}

function transformValue(value, keyTransformer) {
  if (Array.isArray(value)) {
    return value.map((item) => transformValue(item, keyTransformer))
  }

  if (shouldSkipTransform(value) || !isPlainObject(value)) {
    return value
  }

  return Object.fromEntries(
    Object.entries(value).map(([key, nestedValue]) => [
      keyTransformer(key),
      transformValue(nestedValue, keyTransformer)
    ])
  )
}

export function transformKeysToCamel(value) {
  return transformValue(value, toCamelCaseKey)
}

export function transformKeysToSnake(value) {
  return transformValue(value, toSnakeCaseKey)
}

export function transformSnapshot(snapshot) {
  return snapshot ? transformKeysToCamel(snapshot) : null
}

export function transformTraceLog(log) {
  return log ? transformKeysToCamel(log) : null
}

export function transformUser(user) {
  return user ? transformKeysToCamel(user) : null
}

export function transformArray(array, transformFn = transformKeysToCamel) {
  if (!Array.isArray(array)) return []
  return array.map((item) => transformFn(item))
}
```

- [ ] **Step 4: 再次运行测试，确认通用转换已经变绿**

Run:

```bash
cd frontend && npm run test -- --run src/shared/utils/__tests__/transform.test.js
```

Expected: PASS，4 个测试全部通过。

- [ ] **Step 5: 记录 Task 1 检查点**

```md
在 `项目整改执行任务表.md` 的更新记录中补一条“T07 Task 1 通用转换检查点”，注明：
- 已新增通用 key 转换和深层对象转换能力
- `transform.js` 已从零散 helper 升级为通用契约层
```

---

### Task 2: 用 TDD 锁定 `request.js` 的请求/响应统一转换

**Files:**
- Create: `frontend/src/core/api/__tests__/request.test.js`
- Modify: `frontend/src/core/api/request.js`
- Test: `frontend/src/core/api/__tests__/request.test.js`

- [ ] **Step 1: 先写失败测试，锁定请求 snake_case 序列化和响应 camelCase 反序列化**

```js
// frontend/src/core/api/__tests__/request.test.js
import { beforeEach, describe, expect, it, vi } from 'vitest'

const pushMock = vi.fn()
const toastErrorMock = vi.fn()

vi.mock('axios', () => ({
  default: {
    create: () => ({
      interceptors: {
        request: { use: vi.fn() },
        response: { use: vi.fn() }
      }
    })
  }
}))

vi.mock('@/core/router', () => ({
  default: {
    push: pushMock,
    currentRoute: {
      value: {
        fullPath: '/users'
      }
    }
  }
}))

vi.mock('@/shared/composables/useToast', () => ({
  useToast: () => ({
    error: toastErrorMock
  })
}))

import { normalizeRequestConfig, unwrapBusinessResponse } from '@/core/api/request'

describe('request contract normalization', () => {
  beforeEach(() => {
    localStorage.clear()
    pushMock.mockReset()
    toastErrorMock.mockReset()
  })

  it('serializes params and body to snake_case while keeping token injection', () => {
    localStorage.setItem('token', 'token-123')

    const normalized = normalizeRequestConfig({
      params: { roleId: 2, pageSize: 20 },
      data: { partCode: 'P-001', manufacturerNode: '工厂A' },
      headers: {}
    })

    expect(normalized.params).toEqual({ role_id: 2, page_size: 20 })
    expect(normalized.data).toEqual({ part_code: 'P-001', manufacturer_node: '工厂A' })
    expect(normalized.headers.Authorization).toBe('Bearer token-123')
  })

  it('unwraps successful business responses to camelCase data', () => {
    const result = unwrapBusinessResponse({
      data: {
        code: 0,
        data: {
          trace_code: 'TRACE-001',
          total_logs: 4
        }
      },
      config: {}
    })

    expect(result).toEqual({
      traceCode: 'TRACE-001',
      totalLogs: 4
    })
  })

  it('throws readable business errors and keeps toast behavior', () => {
    expect(() =>
      unwrapBusinessResponse(
        {
          data: {
            code: 40001,
            message: '参数错误'
          },
          config: {}
        },
        {
          error: toastErrorMock
        }
      )
    ).toThrow('参数错误')

    expect(toastErrorMock).toHaveBeenCalledWith('参数错误')
  })
})
```

- [ ] **Step 2: 运行测试，确认它先失败**

Run:

```bash
cd frontend && npm run test -- --run src/core/api/__tests__/request.test.js
```

Expected: FAIL，报错缺少 `normalizeRequestConfig` / `unwrapBusinessResponse` 导出，或返回值仍保持 snake_case。

- [ ] **Step 3: 重构 `request.js`，把转换逻辑集中到统一入口**

```js
// frontend/src/core/api/request.js
import axios from 'axios'
import router from '@/core/router'
import { useToast } from '@/shared/composables/useToast'
import { transformKeysToCamel, transformKeysToSnake } from '@/shared/utils/transform'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

const toast = useToast()

export function normalizeRequestConfig(config) {
  const nextConfig = {
    ...config,
    headers: {
      ...(config.headers || {})
    }
  }

  const token = localStorage.getItem('token')
  if (token) {
    nextConfig.headers.Authorization = `Bearer ${token}`
  }

  if (config.params) {
    nextConfig.params = transformKeysToSnake(config.params)
  }

  if (config.data) {
    nextConfig.data = transformKeysToSnake(config.data)
  }

  return nextConfig
}

export function unwrapBusinessResponse(response, notifier = toast) {
  const res = response.data ?? {}

  if (res.code !== 0) {
    const error = new Error(res.message || '请求失败')
    error.code = res.code
    error.response = res

    if (!response.config?.hideErrorToast) {
      notifier.error(res.message || '请求失败')
    }

    throw error
  }

  return transformKeysToCamel(res.data)
}

request.interceptors.request.use(
  (config) => normalizeRequestConfig(config),
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => unwrapBusinessResponse(response),
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      const serverMessage = data?.message

      if (status === 401) {
        const isLoginRequest = error.config?.url?.includes('/auth/login')
        const isPasswordError = data?.code === 11002
        const isUserNotFound = data?.code === 11001

        if (isLoginRequest || isPasswordError || isUserNotFound) {
          error.message = serverMessage || '用户名或密码错误'
          toast.error(error.message)
        } else {
          localStorage.removeItem('token')
          localStorage.removeItem('user')
          toast.error(serverMessage || '登录已过期，请重新登录')
          router.push({
            path: '/login',
            query: { redirect: router.currentRoute.value.fullPath }
          }).catch(() => {})
          error.message = serverMessage || '登录已过期，请重新登录'
        }
      } else if (status === 403) {
        error.message = serverMessage || '没有权限访问该资源'
        toast.error(error.message)
      } else if (status === 404) {
        error.message = serverMessage || '请求的资源不存在'
        toast.error(error.message)
      } else if (status === 500) {
        error.message = serverMessage || '服务器内部错误'
        toast.error(error.message)
      } else {
        error.message = serverMessage || `请求失败 (${status})`
        toast.error(error.message)
      }
    } else if (error.request) {
      error.message = '网络连接失败，请检查网络'
      toast.error(error.message)
    } else {
      error.message = error.message || '请求配置错误'
      toast.error(error.message)
    }

    return Promise.reject(error)
  }
)

export default request
```

- [ ] **Step 4: 运行测试，确认 request 层契约已经变绿**

Run:

```bash
cd frontend && npm run test -- --run src/core/api/__tests__/request.test.js
```

Expected: PASS，3 个测试全部通过。

- [ ] **Step 5: 记录 Task 2 检查点**

```md
在 `项目整改执行任务表.md` 的更新记录中补一条“T07 Task 2 request 收口检查点”，注明：
- 请求 `params/data` 已统一序列化为 snake_case
- 成功响应已统一转换为 camelCase
- 页面层不再需要再手写响应字段转换
```

---

### Task 3: 用回归测试锁定溯源创建/流转链路的 camelCase 调用方式

**Files:**
- Create: `frontend/src/features/trace/components/__tests__/CreateTraceDialog.contract.test.js`
- Modify: `frontend/src/features/trace/components/CreateTraceDialog.vue`
- Modify: `frontend/src/features/trace/components/ScanFlowDialog.vue`
- Modify: `frontend/src/features/trace/components/InboundForm.vue`
- Modify: `frontend/src/features/trace/components/OutboundForm.vue`
- Modify: `frontend/src/features/trace/components/TransferForm.vue`
- Modify: `frontend/src/features/trace/views/TraceDetail.vue`
- Modify: `frontend/src/features/trace/views/TraceList.vue`
- Test: `frontend/src/features/trace/components/__tests__/CreateTraceDialog.contract.test.js`

- [ ] **Step 1: 先写失败测试，证明组件必须消费 camelCase parts 数据并提交 camelCase payload**

```js
// frontend/src/features/trace/components/__tests__/CreateTraceDialog.contract.test.js
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import CreateTraceDialog from '@/features/trace/components/CreateTraceDialog.vue'

const createTraceMock = vi.fn()
const getPartsMock = vi.fn()

vi.mock('@/features/trace/api', () => ({
  createTrace: (...args) => createTraceMock(...args)
}))

vi.mock('@/features/part/api', () => ({
  getParts: (...args) => getPartsMock(...args)
}))

describe('CreateTraceDialog contract', () => {
  beforeEach(() => {
    createTraceMock.mockReset()
    getPartsMock.mockReset()
    getPartsMock.mockResolvedValue({
      list: [{ id: 1, partCode: 'P-001', partName: '轴承' }]
    })
  })

  it('renders camelCase part data and submits camelCase payload', async () => {
    createTraceMock.mockResolvedValue({ traceCodes: ['TRACE-001'] })

    const wrapper = mount(CreateTraceDialog, {
      props: { modelValue: true },
      global: {
        stubs: {
          teleport: true
        }
      }
    })

    await Promise.resolve()
    await nextTick()

    expect(wrapper.text()).toContain('P-001 - 轴承')

    await wrapper.find('select').setValue('P-001')
    await wrapper.find('input[type="number"]').setValue('2')

    const textInputs = wrapper.findAll('input[type="text"]')
    await textInputs[0].setValue('工厂A')
    await textInputs[1].setValue('北京')
    await textInputs[2].setValue('北京市')

    await wrapper.findAll('button')[1].trigger('click')
    await Promise.resolve()

    expect(createTraceMock).toHaveBeenCalledWith({
      partCode: 'P-001',
      quantity: 2,
      manufacturerNode: '工厂A',
      province: '北京',
      city: '北京市'
    })
    expect(wrapper.emitted('success')[0][0]).toEqual(['TRACE-001'])
  })
})
```

- [ ] **Step 2: 运行测试，确认它先失败**

Run:

```bash
cd frontend && npm run test -- --run src/features/trace/components/__tests__/CreateTraceDialog.contract.test.js
```

Expected: FAIL，当前组件仍读取 `part_code / part_name`，并提交 `part_code / manufacturer_node`。

- [ ] **Step 3: 实现 trace 相关组件与页面的 camelCase 收口**

```js
// frontend/src/features/trace/components/CreateTraceDialog.vue
const formData = ref({
  spuId: '',
  partCode: '',
  quantity: 1,
  manufacturerNode: '',
  province: '',
  city: ''
})

const loadParts = async () => {
  const res = await getParts({ page: 1, size: 100 })
  parts.value = res.list || []
}

const handleSubmit = async () => {
  const payload = {
    quantity: Number(formData.value.quantity),
    manufacturerNode: formData.value.manufacturerNode.trim(),
    province: formData.value.province.trim(),
    city: formData.value.city.trim()
  }

  if (formData.value.partCode?.trim()) {
    payload.partCode = formData.value.partCode.trim()
  } else if (formData.value.spuId) {
    payload.spuId = Number(formData.value.spuId)
  }

  const res = await createTrace(payload)
  emit('success', res.traceCodes || [])
}
```

```vue
<!-- frontend/src/features/trace/components/CreateTraceDialog.vue -->
<option v-for="p in parts" :key="p.id" :value="p.partCode">
  {{ p.partCode }} - {{ p.partName }}
</option>
```

```js
// frontend/src/features/trace/components/ScanFlowDialog.vue
const formData = reactive({
  fromNode: '',
  toNode: '',
  province: '',
  city: '',
  eventTime: getCurrentDateTime(),
  remark: ''
})

const resetForm = () => {
  formData.fromNode = ''
  formData.toNode = ''
  formData.province = ''
  formData.city = ''
  formData.eventTime = getCurrentDateTime()
  formData.remark = ''
}

const handleSubmit = async () => {
  const apiData = {
    actionType: actionTypeApiMap[props.actionType],
    fromNode: formData.fromNode,
    toNode: formData.toNode,
    province: formData.province,
    city: formData.city,
    eventTime: formatToBackend(formData.eventTime),
    correctionOf: null
  }

  await createEvent(props.traceCode, apiData)
}
```

```vue
<!-- frontend/src/features/trace/components/InboundForm.vue -->
<input v-model="formData.fromNode" />
<input v-model="formData.toNode" />
<input v-model="formData.eventTime" type="datetime-local" />
```

```vue
<!-- frontend/src/features/trace/components/OutboundForm.vue -->
<input v-model="formData.fromNode" />
<input v-model="formData.toNode" />
<input v-model="formData.eventTime" type="datetime-local" />
```

```vue
<!-- frontend/src/features/trace/components/TransferForm.vue -->
<input v-model="formData.fromNode" />
<input v-model="formData.toNode" />
<input v-model="formData.eventTime" type="datetime-local" />
```

```js
// frontend/src/features/trace/views/TraceDetail.vue
const loadDetail = async () => {
  const data = await getTraceDetail(traceCode)
  snapshot.value = data.snapshot
  history.value = (data.history || []).sort(
    (a, b) => new Date(b.eventTime) - new Date(a.eventTime)
  )
}

const verifyChain = async () => {
  const res = await verifyTraceChain(traceCode)
  verification.value = {
    valid: res.valid,
    totalLogs: res.totalLogs,
    hashVerifiedCount: res.hashVerifiedCount,
    signatureVerifiedCount: res.signatureVerifiedCount,
    errors: res.errors || []
  }
}
```

```js
// frontend/src/features/trace/views/TraceList.vue
const res = await createTrace({
  spuId: Number(createForm.value.spuId),
  quantity: Number(createForm.value.quantity) || 1,
  manufacturerNode: createForm.value.manufacturerNode.trim(),
  province: createForm.value.province.trim(),
  city: createForm.value.city.trim()
})

const newCode = res.traceCodes?.[0]
```

- [ ] **Step 4: 重新运行回归测试，确认 trace 创建链路已只依赖 camelCase**

Run:

```bash
cd frontend && npm run test -- --run src/features/trace/components/__tests__/CreateTraceDialog.contract.test.js
```

Expected: PASS，测试可证明组件消费 `partCode / partName`，提交 `partCode / manufacturerNode`。

- [ ] **Step 5: 记录 Task 3 检查点**

```md
在 `项目整改执行任务表.md` 的更新记录中补一条“T07 Task 3 trace 链路收口检查点”，注明：
- 赋码与扫码流转内部状态已切到 camelCase
- TraceDetail/TraceList 已直接消费 request 层 camelCase 返回值
```

---

### Task 4: 锁定 feature API 对页面层暴露 camelCase 契约，并清理用户/角色/配件/看板页面

**Files:**
- Create: `frontend/src/features/__tests__/api-contracts.test.js`
- Modify: `frontend/src/features/user/api/users.js`
- Modify: `frontend/src/features/user/api/roles.js`
- Modify: `frontend/src/features/part/api/parts.js`
- Modify: `frontend/src/features/trace/api/trace.js`
- Modify: `frontend/src/features/dashboard/api/dashboard.js`
- Modify: `frontend/src/features/user/views/UserList.vue`
- Modify: `frontend/src/features/user/views/RoleList.vue`
- Modify: `frontend/src/features/part/views/PartList.vue`
- Modify: `frontend/src/features/dashboard/views/Dashboard.vue`
- Test: `frontend/src/features/__tests__/api-contracts.test.js`

- [ ] **Step 1: 先写失败测试，锁定 feature API 只向页面层暴露 camelCase**

```js
// frontend/src/features/__tests__/api-contracts.test.js
import { beforeEach, describe, expect, it, vi } from 'vitest'
import request from '@/core/api/request'
import { getUsers, createUser } from '@/features/user/api/users'
import { createRole } from '@/features/user/api/roles'
import { createPart } from '@/features/part/api/parts'
import { createTrace } from '@/features/trace/api/trace'
import { getTopology } from '@/features/dashboard/api/dashboard'

vi.mock('@/core/api/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn()
  }
}))

describe('feature api contracts', () => {
  beforeEach(() => {
    request.get.mockReset()
    request.post.mockReset()
    request.put.mockReset()
    request.patch.mockReset()
    request.delete.mockReset()
  })

  it('keeps user and role inputs in camelCase before request serialization', async () => {
    request.get.mockResolvedValue({ list: [], total: 0 })
    request.post.mockResolvedValue({})

    await getUsers({ username: 'alice', roleId: 2, page: 1, size: 10 })
    await createUser({ username: 'alice', password: 'abc123', roleId: 2, status: 1 })
    await createRole({ roleCode: 'manager', roleName: '管理员' })

    expect(request.get).toHaveBeenCalledWith('/users', {
      params: { username: 'alice', roleId: 2, page: 1, size: 10 }
    })
    expect(request.post).toHaveBeenNthCalledWith(1, '/users', {
      username: 'alice',
      password: 'abc123',
      roleId: 2,
      status: 1
    })
    expect(request.post).toHaveBeenNthCalledWith(2, '/roles', {
      roleCode: 'manager',
      roleName: '管理员'
    })
  })

  it('keeps part, trace and dashboard inputs in camelCase before request serialization', async () => {
    request.post.mockResolvedValue({})
    request.get.mockResolvedValue({})

    await createPart({ partCode: 'P-001', partName: '轴承', partType: '机械件' })
    await createTrace({ partCode: 'P-001', manufacturerNode: '工厂A', quantity: 3 })
    await getTopology('TRACE-001', '30d')

    expect(request.post).toHaveBeenNthCalledWith(1, '/parts', {
      partCode: 'P-001',
      partName: '轴承',
      partType: '机械件'
    })
    expect(request.post).toHaveBeenNthCalledWith(2, '/traces', {
      partCode: 'P-001',
      manufacturerNode: '工厂A',
      quantity: 3
    })
    expect(request.get).toHaveBeenCalledWith('/dashboard/topology', {
      params: { traceCode: 'TRACE-001', range: '30d' }
    })
  })
})
```

- [ ] **Step 2: 运行测试，确认它先失败**

Run:

```bash
cd frontend && npm run test -- --run src/features/__tests__/api-contracts.test.js
```

Expected: FAIL，当前 feature API 仍然要求/注释 snake_case，`getTopology` 仍传 `trace_code`，部分 create/update 仍用 `role_id / part_code`。

- [ ] **Step 3: 修改 feature API 与页面内部状态，只保留 camelCase**

```js
// frontend/src/features/user/api/users.js
export function getUsers(params) {
  return request.get('/users', { params })
}

export function createUser(data) {
  return request.post('/users', data)
}

export function updateUser(id, data) {
  return request.put(`/users/${id}`, data)
}
```

```js
// frontend/src/features/user/api/roles.js
export function createRole(data) {
  return request.post('/roles', data)
}

export function assignPermissions(id, permissionIds) {
  return request.put(`/roles/${id}/permissions`, { permissionIds })
}
```

```js
// frontend/src/features/part/api/parts.js
export function createPart(data) {
  return request.post('/parts', data)
}

export function updatePart(id, data) {
  return request.put(`/parts/${id}`, data)
}
```

```js
// frontend/src/features/trace/api/trace.js
export function createTrace(data) {
  return request.post('/traces', data)
}

export function createEvent(traceCode, data) {
  return request.post(`/traces/${traceCode}/events`, data)
}
```

```js
// frontend/src/features/dashboard/api/dashboard.js
export function getTopology(traceCode, range = '30d') {
  return request.get('/dashboard/topology', { params: { traceCode, range } })
}
```

```js
// frontend/src/features/user/views/UserList.vue
const query = reactive({
  username: '',
  roleId: '',
  status: '',
  page: 1,
  size: 10
})

const formData = reactive({
  username: '',
  password: '',
  roleId: '',
  status: 1
})

users.value = res.list || []
total.value = res.total || 0

Object.assign(formData, {
  username: user.username,
  password: '',
  roleId: user.roleId,
  status: user.status
})
```

```js
// frontend/src/features/user/views/RoleList.vue
const formData = reactive({
  roleCode: '',
  roleName: '',
  remark: ''
})

Object.assign(formData, {
  roleCode: role.roleCode,
  roleName: role.roleName,
  remark: role.remark || ''
})
```

```js
// frontend/src/features/part/views/PartList.vue
const formData = reactive({
  partCode: '',
  partName: '',
  partType: '',
  manufacturer: '',
  model: ''
})

Object.assign(formData, {
  partCode: part.partCode,
  partName: part.partName,
  partType: part.partType,
  manufacturer: part.manufacturer,
  model: part.model
})
```

```js
// frontend/src/features/dashboard/views/Dashboard.vue
const kpiData = ref({
  totalTraces: 0,
  todayNew: 0,
  totalLogs: 0,
  exceptionCount: 0
})

const kpiCards = [
  { key: 'totalTraces', label: '总溯源数', icon: Package, color: 'text-blue-500 bg-blue-50' },
  { key: 'todayNew', label: '今日新增', icon: TrendingUp, color: 'text-green-500 bg-green-50' },
  { key: 'totalLogs', label: '溯源日志', icon: Box, color: 'text-orange-500 bg-orange-50' },
  { key: 'exceptionCount', label: '异常事件', icon: Users, color: 'text-purple-500 bg-purple-50' }
]

const mapItems = mapRes.value?.items || []
const trendItems = trendRes.value?.items || []
```

```vue
<!-- 在 UserList.vue / RoleList.vue / PartList.vue 模板中同步替换 -->
{{ user.roleName }}
{{ role.roleCode }}
{{ role.roleName }}
{{ part.partCode }}
{{ part.partName }}
{{ part.partType }}
```

- [ ] **Step 4: 运行 feature API 契约测试并用 grep 复核已移除主要 fallback**

Run:

```bash
cd frontend && npm run test -- --run src/features/__tests__/api-contracts.test.js
```

Expected: PASS，2 个测试全部通过。

Run:

```powershell
Get-ChildItem -Path @(
  'frontend/src/features/user/views/UserList.vue',
  'frontend/src/features/user/views/RoleList.vue',
  'frontend/src/features/part/views/PartList.vue',
  'frontend/src/features/trace/views/TraceDetail.vue'
) | Select-String -Pattern '\.\w+_\w+\s*\|\|'
```

Expected: 无输出，说明这些代表性页面已去掉 `snake || camel` 回退读法。

- [ ] **Step 5: 记录 Task 4 检查点**

```md
在 `项目整改执行任务表.md` 的更新记录中补一条“T07 Task 4 API 与页面契约检查点”，注明：
- feature API 注释与调用语义已切到 camelCase
- UserList / RoleList / PartList / Dashboard 已清理主要 snake_case 内部状态
- 代表性页面已移除 `snake || camel` 回退读法
```

---

### Task 5: 执行整体验证并更新任务表到 T07 完成

**Files:**
- Modify: `项目整改执行任务表.md`
- Test: `frontend/src/shared/utils/__tests__/transform.test.js`
- Test: `frontend/src/core/api/__tests__/request.test.js`
- Test: `frontend/src/features/trace/components/__tests__/CreateTraceDialog.contract.test.js`
- Test: `frontend/src/features/__tests__/api-contracts.test.js`

- [ ] **Step 1: 运行 T07 相关测试集合**

Run:

```bash
cd frontend && npm run test -- --run src/shared/utils/__tests__/transform.test.js src/core/api/__tests__/request.test.js src/features/trace/components/__tests__/CreateTraceDialog.contract.test.js src/features/__tests__/api-contracts.test.js
```

Expected: PASS，所有 T07 新增契约测试全部通过。

- [ ] **Step 2: 运行前端全量测试，确保未引入回归**

Run:

```bash
cd frontend && npm run test:run
```

Expected: PASS，现有测试与 T07 新增测试全部通过。

- [ ] **Step 3: 运行前端构建**

Run:

```bash
cd frontend && npm run build
```

Expected: PASS，构建成功，允许保留既有 chunk warning，但不能新增语法错误或模块解析错误。

- [ ] **Step 4: 更新任务表，将 T07 标记为 DONE**

```md
同步更新 `项目整改执行任务表.md`：
- “当前进行中任务”改为无，或切到下一任务
- “最近一次更新摘要”写明 T07 已完成
- “当前任务状态总览”中将 T07 改为 DONE
- “更新记录”补入 T07 的影响文件、验证方式、遗留问题与下一步建议
- “下一推荐任务”切到 T08
```

- [ ] **Step 5: 保存最终检查点**

```md
在最终更新记录里明确写出：
- `transform.js` 已成为前端唯一契约收口层
- `request.js` 已统一处理 snake_case / camelCase 转换
- 本次未做 UI 改造，仅完成逻辑/契约治理
```
