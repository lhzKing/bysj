# 工业企业配件供应链溯源系统前端

本目录是工业企业配件供应链溯源系统的 Vue 3 单页应用，2026-05 完成 [Linear 设计系统](../DESIGN.md) 全量重构（F01-F23 共 23 项任务，详见 [前端重构执行任务表_20260507.md](../前端重构执行任务表_20260507.md)）。负责登录、仪表盘、扫码工位、生产赋码工作台、仓库/物流任务工作台、追溯查询、配件/用户/角色后台管理等模块。

## 设计系统与视觉契约

- **风格基线**：[`../DESIGN.md`](../DESIGN.md) — Linear 设计系统（lavender `#5e6ad2` 单 accent + Inter / JetBrains Mono + 4/6/8/12/16 圆角阶梯 + 4/8/12/16/24/32/48 间距阶梯）
- **视觉契约**：[`preview/`](preview/) 目录下 5 张 Linear-light HTML 预览，是所有 view 的 1:1 视觉对照基线
  - [`preview/linear-login.html`](preview/linear-login.html) — 登录页
  - [`preview/linear-dashboard.html`](preview/linear-dashboard.html) — 仪表盘
  - [`preview/linear-scan.html`](preview/linear-scan.html) — 扫码工位（含 dark surface 取景区）
  - [`preview/linear-traces.html`](preview/linear-traces.html) — 追溯列表
  - [`preview/linear-trace-detail.html`](preview/linear-trace-detail.html) — 追溯详情
- **历史探索**：`preview/scan-hub-{cal-editorial,cal-shell,cal-workspace,industrial,saas,glass}.html` 为 Linear 收敛**前**的 6 张设计探索 alternative，与 5 张 linear-* 并列存在仅作答辩演化叙事素材使用，不再作为新组件开发的视觉契约

## 应用骨架（断点策略）

- **桌面 ≥1024px**：240px 固定侧栏 (`AppSidebarNav.vue`) + 48px 顶栏 (`AppTopbar.vue`) + 内容区
- **平板 768–1023px**：侧栏自动折叠为顶部汉堡，点击后从左滑出抽屉 (`MobileSidebarDrawer.vue`)
- **手机 <768px**：顶栏简化为汉堡 + 当前页标题 + 头像；扫码工位 dark 取景区全屏覆盖

切换由 `MainLayout.vue` 通过 `window.matchMedia('(max-width: 1023.98px)')` JS 判断 `isCompact` 状态驱动 `v-if` 切换 sidebar 与 drawer。详细断点 + 各页响应式策略见 [前端重构执行任务表_20260507.md F23 段](../前端重构执行任务表_20260507.md)。

## 技术栈

- Vue 3 (`<script setup>` Composition API) + Vite 5
- Vue Router 4 + Pinia 2
- Axios 请求封装（`core/api/request.js`：自动注入 `Authorization: Bearer <token>` + camelCase↔snake_case 双向转换 + 401/403/500 全局错误 toast）
- Tailwind CSS（utility）+ scoped CSS（@media 断点）+ 共享原子组件（`shared/components/ui/`）
- PrimeVue 4（仅保留 Toast / ConfirmDialog 等无状态原语；F08 已删除全部 PrimeVue 包装层 `shared/components/prime/*`）
- Lucide Vue Next（图标）+ ECharts / vue-echarts（仪表盘可视化）+ 高德地图 JS API（地图组件）
- `vue-qrcode-reader` + 浏览器摄像头能力（扫码场景）
- Vitest + Vue Test Utils + jsdom（单元测试，含 viewport 响应式快照）

## 目录结构（feature-based）

```text
frontend/
├── src/
│   ├── core/                 # 全局基础设施
│   │   ├── api/              # axios 实例 + auth 模块（login/register/me/logout）
│   │   ├── auth/             # authStorage（token + user 缓存适配层）
│   │   ├── router/           # vue-router 实例 + meta.permissions 守卫
│   │   └── stores/           # Pinia user store（permissions / hasAnyPermission）
│   ├── features/             # 业务模块（feature-based 划分）
│   │   ├── dashboard/        # 仪表盘 → views/Dashboard.vue + components/* + api/
│   │   ├── trace/            # 溯源核心 → ScanHub / TraceList / TraceDetail / TraceAssignmentWorkbench / TraceFlowTaskWorkbench + 6 个 Dialog + api/
│   │   ├── user/             # 用户与角色管理 → UserList / RoleList + components/* + api/
│   │   ├── part/             # 配件管理 → PartList + components/* + api/
│   │   └── __tests__/        # 跨 feature 契约测试 + responsive viewport 快照
│   ├── shared/               # 跨 feature 复用
│   │   ├── components/       # ui/（Base 原子）+ layout/（4 个骨架）+ Login / NotFound / QRScanner
│   │   ├── composables/      # useToast / useConfirm / usePrompt
│   │   ├── constants/        # PERMISSIONS / actionTypes / regions
│   │   ├── theme/            # tokens.js（Linear 设计 token，承载 DESIGN.md 决议）
│   │   └── utils/            # transform.js（camel↔snake）+ logger
│   ├── test/                 # Vitest 全局 setup（matchMedia / ResizeObserver mock）
│   ├── views/                # 非主业务页面（CameraTest）
│   ├── App.vue
│   └── main.js
├── preview/                  # 5 张 linear-* 视觉契约 + 6 张历史探索 + index.html 索引
├── certs/                    # 本地 HTTPS 证书（generate-cert.js / .ps1 生成）
├── public/
├── .env.example              # 环境变量模板，不写真实第三方 Key
├── vite.config.js            # Vite + HTTPS + /api 代理 + 测试配置
└── package.json
```

路径别名 `@` 指向 `frontend/src`。

## 前置条件

1. Node.js 18+
2. npm（建议 9+）
3. 后端服务已启动，默认监听 `http://localhost:8080`（启动方式见根目录 [`README.md`](../README.md)）
4. 如启用 HTTPS 开发服务器（默认开启，摄像头扫码要求），需要 `frontend/certs/localhost.key` 与 `frontend/certs/localhost.crt`

## 环境变量

从模板创建本地配置：

```powershell
cd frontend
Copy-Item .env.example .env.development.local
```

| 变量 | 默认值 | 说明 |
|---|---|---|
| `VITE_DEV_HOST` | `0.0.0.0` | Vite 开发服务器监听地址 |
| `VITE_DEV_PORT` | `5173` | Vite 开发服务器端口 |
| `VITE_DEV_HTTPS` | `true` | 是否启用本地 HTTPS；设为 `false` 可改用 HTTP |
| `VITE_DEV_CERT_KEY` | `certs/localhost.key` | HTTPS 私钥路径，基于 `frontend/` |
| `VITE_DEV_CERT_CERT` | `certs/localhost.crt` | HTTPS 证书路径，基于 `frontend/` |
| `VITE_API_PROXY_TARGET` | `http://localhost:8080` | `/api` 代理目标后端 |
| `VITE_AMAP_KEY` | `<replace-with-amap-web-js-api-key>` | 高德 Web JS API Key（**仅写入本机 `.env.development.local`，绝不入库**） |

完整敏感凭据规范见根 [`README.md` 前端启动段](../README.md)（含 Key 泄露应急吊销流程）。

## 安装依赖

```powershell
cd frontend
npm install
```

## HTTPS 证书

`vite.config.js` 默认启用 HTTPS，并读取 `frontend/certs/localhost.{key,crt}`。

```powershell
cd frontend
node generate-cert.js                                    # Node 跨平台
# 或
powershell -ExecutionPolicy Bypass -File .\generate-cert.ps1   # PowerShell
```

如果暂时不需要 HTTPS（不调试摄像头），可在本机 `.env.development.local` 中设：

```env
VITE_DEV_HTTPS=false
```

扫码/摄像头能力在移动端浏览器需要 HTTPS 或可信本地域名；手机联调建议生成包含本机局域网 IP 的自签证书 + 在手机浏览器手动信任。详细排障见 [`../CAMERA_SCAN_GUIDE.md`](../CAMERA_SCAN_GUIDE.md)。

## 启动开发服务器

```powershell
cd frontend
npm run dev
```

默认访问：

- HTTPS（默认）：`https://localhost:5173/`
- HTTP（`VITE_DEV_HTTPS=false`）：`http://localhost:5173/`

开发服务器会把 `/api` 代理到 `VITE_API_PROXY_TARGET`（默认 `http://localhost:8080`）。前端代码使用相对路径 `/api/...`，无需写完整后端地址，也不需要额外 CORS 配置。

## npm scripts

| 命令 | 用途 |
|---|---|
| `npm run dev` | 启动 Vite 开发服务器 |
| `npm run test` | 以监听模式运行 Vitest（开发时改完即跑） |
| `npm run test:run` | 一次性运行全部前端测试（CI 与 commit 前用） |
| `npm run build` | 生产构建到 `dist/` |
| `npm run preview` | 本地预览生产构建产物 |

## 测试

运行全部前端测试：

```powershell
cd frontend
npm run test:run
```

运行单个测试文件示例：

```powershell
npm run test -- --run src/core/api/__tests__/request.test.js
npm run test -- --run src/features/__tests__/responsive
```

测试基线（截至 F23 完成时）：**50 个测试文件 / 301 个测试用例 / ~12s**，覆盖：

- 共享原子组件（BaseButton/Card/Input/Dialog/Toast/StatusPill/TraceCodeChip/VerifyBadge/PageHeader/EmptyState/LoadingSkeleton/KbdShortcut）
- 4 个 layout 组件 + MainLayout 桌面/移动 viewport 切换
- 12 个 feature view 的 smoke / contract / route / permission / responsive 测试
- 6 个扫码 Dialog 的契约测试
- API 模块字段契约（`api-contracts.test.js` + `api-contracts.wire.test.js`）
- core 层（request 拦截器 / authStorage / transform 工具 / logger）
- **响应式 viewport 测试**：`features/__tests__/responsive/` 4 文件 / 25 tests，覆盖 Dashboard / TraceList / TraceDetail / ScanHub 在 mobile 390 与 desktop 1280 两套 viewport 下的 DOM 结构 + scoped CSS @media 规则 + matchMedia 行为契约

测试环境配置：

- `environment: jsdom`
- `setupFiles: ./src/test/setup.js`（mock `matchMedia` / `ResizeObserver`）
- 路径别名：`@` 指向 `frontend/src`
- Vite `?raw` suffix 用于 viewport 测试中读取 SFC 源码做 @media 规则正则校验

## 构建与预览

生产构建：

```powershell
cd frontend
npm run build
```

本地预览构建产物：

```powershell
npm run preview
```

构建产物位于 `frontend/dist/`（不入库；`.gitignore` 已忽略）。

构建产物 chunk 基线（截至 F23 完成时，与 F19/F20/F21/F22 完全一致）：

| chunk | 体积 | gzip |
|---|---:|---:|
| `vendor-echarts` | 514.68 kB | 174.82 kB |
| `vendor-prime` | **143.21 kB** | **26.50 kB** |
| `vendor-vue` | 109.44 kB | 42.74 kB |
| `vendor-qrcode` | 69.78 kB | 23.50 kB |
| `index` | 65.38 kB | 25.36 kB |
| `TraceDetail` | 40.54 kB | 14.63 kB |
| `TraceAssignmentWorkbench` | 23.62 kB | 8.56 kB |
| `UserList` / `RoleList` / `PartList` | ~22-23 kB | ~7.4-8.1 kB |
| `TraceFlowTaskWorkbench` | 20.67 kB | 7.72 kB |
| `ScanHub` | 19.51 kB | 7.76 kB |
| `TraceList` | 14.69 kB | 5.52 kB |
| `MainLayout` | 11.81 kB | 4.12 kB |

字体走 Google Fonts CDN（Inter + JetBrains Mono），不打包进产物。

## 鉴权与路由

前端路由入口位于 [`src/core/router/index.js`](src/core/router/index.js)：

| 路径 | name | meta.permissions | 说明 |
|---|---|---|---|
| `/login` | login | — | 登录/自助注册 |
| `/` | dashboard | `dashboard:view` | 仪表盘（KPI/趋势/区域分布/异常） |
| `/scan` | scan-hub | `TRACE_SCAN_HUB_ACCESS` 聚合 | 扫码工位（dark surface 取景） |
| `/trace-assignment` | trace-assignment-workbench | `TRACE_ASSIGNMENT_ACCESS` 聚合 | 生产赋码工作台 |
| `/trace-flow-tasks` | trace-flow-task-workbench | `TRACE_FLOW_TASK_ACCESS` 聚合 | 仓库/物流任务工作台 |
| `/traces` | traces | `trace:view` | 追溯查询（分页 + 多筛选） |
| `/traces/:code` | trace-detail | `trace:view` | 详情（5 tabs + 链验签 + audit 视图） |
| `/users` | users | `user:view` | 用户管理 |
| `/roles` | roles | `role:view` | 角色与权限分配 |
| `/parts` | parts | `part:view` | 配件管理 |
| `/camera-test` | camera-test | — | 摄像头能力测试 |
| `/:pathMatch(.*)*` | not-found | — | 404 |

鉴权要点：

- Token + user 缓存统一由 [`src/core/auth/authStorage.js`](src/core/auth/authStorage.js) 管理（localStorage 适配层；后续可平滑迁移到 httpOnly Cookie，详见 [`../docs/security/token-storage-and-csp.md`](../docs/security/token-storage-and-csp.md)）
- `request.js` 的 401 处理通过 `setUnauthorizedHandler(...)` 注入到请求层，避免循环依赖（router 不被 request 模块直接 import）
- 菜单与路由权限使用 [`src/shared/constants/permissions.js`](src/shared/constants/permissions.js) 中的 `PERMISSIONS` 常量与三个聚合 (`TRACE_SCAN_HUB_ACCESS` / `TRACE_ASSIGNMENT_ACCESS` / `TRACE_FLOW_TASK_ACCESS`)
- **后端仍是权限最终裁决方**；前端 RBAC 仅用于侧栏菜单可见性 + 路由跳转拦截 + 按钮 disable，所有 API 拒绝由后端 `@RequirePermission` + `PermissionInterceptor` 兜底返回 `HTTP 403 code=10003`

## 常见问题

### 启动时报证书文件不存在

```powershell
cd frontend
node generate-cert.js
```

或临时关闭 HTTPS（在 `.env.development.local`）：

```env
VITE_DEV_HTTPS=false
```

### 前端能打开但 API 请求失败

1. 确认后端服务已启动 (`mvn spring-boot:run -Dspring-boot.run.profiles=dev` + Redis 6379)
2. 确认 `VITE_API_PROXY_TARGET` 指向正确后端地址
3. 确认请求路径以 `/api` 开头
4. 修改 `.env.development.local` 后重启 `npm run dev`
5. 5 角色账号默认密码均为 `用户名 + 123456`（superadmin/admin/producer/warehouse/logistics/user）

### 地图不显示

1. 确认本地 `.env.development.local` 中 `VITE_AMAP_KEY` 已配置为有效 Web JS API Key
2. 确认高德控制台 Key 的 Referer 白名单包含 `localhost` / `127.0.0.1` / 本机局域网段 + 生产域名
3. 检查浏览器控制台是否有 Key、域名白名单或网络错误

### 手机扫码 / 摄像头不可用

1. 优先使用 HTTPS 地址访问开发服务器（`https://<本机局域网IP>:5173/`）
2. 手机与开发机需在同一局域网
3. 首次访问自签名证书时，需要在浏览器中手动信任或继续访问
4. 可先访问 `/camera-test` 验证浏览器摄像头能力
5. 完整排障流程见 [`../CAMERA_SCAN_GUIDE.md`](../CAMERA_SCAN_GUIDE.md)

### 5 角色权限测试

按角色分别登录后预期菜单：

- **superadmin / admin**：全部菜单可见（dashboard / scan / trace-assignment / trace-flow-tasks / traces / users / roles / parts）
- **producer**：dashboard / trace-assignment / scan / traces / parts
- **warehouse**：dashboard / trace-flow-tasks / scan / traces / parts
- **logistics**：dashboard / trace-flow-tasks / scan / traces（无 parts）
- **user**：dashboard / traces

完整接口对账证据见 [前端重构执行任务表_20260507.md F22 段](../前端重构执行任务表_20260507.md)（curl 烟测覆盖 5 角色 minimal path GET 全 200 + 跨角色 RBAC 全部 403 + producer→warehouse→logistics→superadmin 完整 mutation chain 6 logs hash+RSA 全验签 valid=true）。

## 文档与更多说明

- 根目录 [`README.md`](../README.md)：系统整体启动、后端依赖、账号、安全配置、Token 黑名单原理、RSA 密钥管理
- [`../DESIGN.md`](../DESIGN.md)：Linear 设计系统（颜色 / 字体 / 圆角 / 间距 / 暗面取景区 / 组件原语）
- [`../api-doc.md`](../api-doc.md)：API 接口契约（请求体 / 响应字段 / 错误码 + 第 10 节 Vue 3 + Axios 对接示例）
- [`../backend/README.md`](../backend/README.md)：后端配置、Profile、数据库、Redis、签名密钥说明
- [`../postman-guide.md`](../postman-guide.md)：Postman 调试说明
- [`../CAMERA_SCAN_GUIDE.md`](../CAMERA_SCAN_GUIDE.md)：摄像头扫码（HTTPS + 证书）排障
- [`../前端重构执行任务表_20260507.md`](../前端重构执行任务表_20260507.md)：F01-F24 完整重构记录（设计共识 + 任务明细 + 验证证据 + commit 链路）
- [`TESTING_GUIDE.md`](TESTING_GUIDE.md)：手工测试清单和历史迁移说明
