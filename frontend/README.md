# 工业企业配件供应链溯源系统前端

本目录是工业企业配件供应链溯源系统的 Vue 3 单页应用，负责登录认证、仪表盘、溯源管理、扫码中心、用户/角色权限管理和配件管理等前端功能。

## 技术栈

- Vue 3 + Vite
- Vue Router + Pinia
- Axios 请求封装，统一挂载 `Authorization: Bearer <token>`
- PrimeVue / Tailwind CSS / 自定义共享组件
- ECharts / vue-echarts 数据可视化
- `vue-qrcode-reader` 与浏览器摄像头能力用于扫码场景
- Vitest + Vue Test Utils + jsdom

## 目录说明

```text
frontend/
├── src/
│   ├── core/                 # API 请求、认证存储、路由、全局 store
│   ├── features/             # dashboard / trace / user / part 业务模块
│   ├── shared/               # 通用组件、布局、常量、主题、工具和组合式函数
│   ├── test/                 # Vitest 全局 setup
│   ├── views/                # 非主业务模块页面，如摄像头测试
│   ├── App.vue
│   └── main.js
├── certs/                    # 本地 HTTPS 证书，按需生成
├── public/
├── .env.example              # 环境变量模板，不能写真实第三方 Key
├── vite.config.js            # Vite、HTTPS、代理、测试配置
└── package.json
```

## 前置条件

1. Node.js 18+。
2. 已安装 npm。
3. 后端服务已启动，默认监听 `http://localhost:8080`。
4. 如启用 HTTPS 开发服务器，需要准备 `frontend/certs/localhost.key` 和 `frontend/certs/localhost.crt`。

## 环境变量

从模板创建本地配置：

```powershell
cd frontend
Copy-Item .env.example .env.local
```

常用变量：

| 变量 | 默认值 | 说明 |
|---|---|---|
| `VITE_DEV_HOST` | `0.0.0.0` | Vite 开发服务器监听地址 |
| `VITE_DEV_PORT` | `5173` | Vite 开发服务器端口 |
| `VITE_DEV_HTTPS` | `true` | 是否启用本地 HTTPS；设为 `false` 可改用 HTTP |
| `VITE_DEV_CERT_KEY` | `certs/localhost.key` | HTTPS 私钥路径，基于 `frontend/` |
| `VITE_DEV_CERT_CERT` | `certs/localhost.crt` | HTTPS 证书路径，基于 `frontend/` |
| `VITE_API_PROXY_TARGET` | `http://localhost:8080` | `/api` 代理目标后端 |
| `VITE_AMAP_KEY` | `<replace-with-amap-web-js-api-key>` | 高德 Web JS API Key，仅写入本机 `.env.local` |

注意：

- `.env.example` 必须只保留占位符，不写真实地图 Key。
- 本地 `.env.local` / `.env.development` 不应提交到版本库。
- 生产环境请通过部署平台注入变量，不要把第三方 Key 写入 README 或示例文件。

## 安装依赖

```powershell
cd frontend
npm install
```

## HTTPS 证书

`vite.config.js` 默认启用 HTTPS，并读取：

- `frontend/certs/localhost.key`
- `frontend/certs/localhost.crt`

Windows 开发环境可使用脚本生成包含本机局域网 IP 的自签名证书：

```powershell
cd frontend
powershell -ExecutionPolicy Bypass -File .\generate-cert.ps1
```

如果暂时不需要 HTTPS，可在本机 `.env.local` 中设置：

```env
VITE_DEV_HTTPS=false
```

扫码/摄像头相关能力在移动端浏览器通常需要 HTTPS 或可信本地域名；手机联调时建议使用生成的 HTTPS 证书并访问局域网地址。

## 启动开发服务器

```powershell
cd frontend
npm run dev
```

默认访问：

- HTTPS：`https://localhost:5173/`
- HTTP（当 `VITE_DEV_HTTPS=false`）：`http://localhost:5173/`

开发服务器会将前端请求中的 `/api` 代理到 `VITE_API_PROXY_TARGET`。例如：

```text
前端请求：/api/auth/login
代理目标：http://localhost:8080/api/auth/login
```

因此前端代码中不需要写完整后端地址。

## npm scripts

| 命令 | 用途 |
|---|---|
| `npm run dev` | 启动 Vite 开发服务器 |
| `npm run test` | 以监听模式运行 Vitest |
| `npm run test:run` | 一次性运行全部前端测试 |
| `npm run build` | 生产构建到 `dist/` |
| `npm run preview` | 本地预览生产构建结果 |

## 测试

运行全部前端测试：

```powershell
cd frontend
npm run test:run
```

运行单个测试文件示例：

```powershell
cd frontend
npm run test -- --run src/core/api/__tests__/request.test.js
```

当前测试环境：

- `environment: jsdom`
- `setupFiles: ./src/test/setup.js`
- 路径别名：`@` 指向 `frontend/src`

## 构建与预览

生产构建：

```powershell
cd frontend
npm run build
```

本地预览构建产物：

```powershell
cd frontend
npm run preview
```

构建产物位于 `frontend/dist/`。`dist/` 是生成目录，不应作为源码维护。

## 登录、权限与路由

前端路由入口位于 `src/core/router/index.js`：

- `/login`：登录/自助注册页面。
- `/`：仪表盘。
- `/traces`：溯源管理。
- `/scan`：扫码中心。
- `/traces/:code`：溯源详情。
- `/users`：用户管理。
- `/roles`：角色管理。
- `/parts`：配件管理。
- `/camera-test`：摄像头测试页。

鉴权与权限控制要点：

- Token 和用户缓存统一由 `src/core/auth/authStorage.js` 管理。
- API 401 处理通过 `setUnauthorizedHandler(...)` 注入到请求层，不在请求模块中直接依赖 router。
- 菜单和路由权限使用 `src/shared/constants` 中的权限常量。
- 后端仍是权限最终裁决方；前端权限仅用于导航和交互展示。

## 常见问题

### 启动时报证书文件不存在

确认已生成证书，或临时关闭 HTTPS：

```env
VITE_DEV_HTTPS=false
```

### 前端能打开但 API 请求失败

1. 确认后端服务已启动。
2. 确认 `VITE_API_PROXY_TARGET` 指向正确后端地址。
3. 确认请求路径以 `/api` 开头。
4. 修改 `.env.local` 后重启 `npm run dev`。

### 地图不显示

1. 确认本地环境变量 `VITE_AMAP_KEY` 已配置为有效 Web JS API Key。
2. 不要把真实 Key 写入 README 或 `.env.example`。
3. 检查浏览器控制台是否有 Key、域名白名单或网络错误。

### 手机扫码/摄像头不可用

1. 优先使用 HTTPS 地址访问开发服务器。
2. 手机与开发机需在同一局域网。
3. 首次访问自签名证书时，需要在浏览器中手动信任或继续访问。
4. 可先访问 `/camera-test` 验证浏览器摄像头能力。

## 文档与更多说明

- 根目录 `README.md`：系统整体启动、后端依赖、账号和安全配置。
- `backend/README.md`：后端配置、Profile、数据库、Redis、签名密钥说明。
- `api-doc.md`：接口契约。
- `postman-guide.md`：Postman 调试说明。
- `frontend/TESTING_GUIDE.md`：手工测试清单和历史迁移说明。
