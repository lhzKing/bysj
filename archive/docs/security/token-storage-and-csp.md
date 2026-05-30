# R-P2-04 Token 存储与 CSP 补偿方案

- 日期：2026-05-03
- 范围：前端认证态存储、JWT 有效期、前端 CSP 基线
- 结论：当前版本继续使用 `Authorization: Bearer <jwt>` + `localStorage`，但必须配套短有效期、Redis 黑名单 fail-closed、CSP 与单一存储适配层；`httpOnly Cookie + CSRF` 作为后续大改造方案预留。

## 1. 当前决策

### 保留 localStorage 的原因

1. 后端认证契约已经稳定为 `Authorization` Header，移动端/扫码端和 Postman 调试也沿用该契约。
2. 当前没有独立 refresh token 与 CSRF 防护链路，直接切换 Cookie 会扩大后端、前端、CORS、测试和文档改造面。
3. R-P0-05 已修复已知 InfoWindow XSS 注入点，本轮先落地补偿控制，避免一次性引入会话模型大迁移风险。

### 风险边界

- `localStorage` 中仅允许保存 `token` 与最小化的 `user` 展示/权限缓存，不保存密码、密钥或其他长期敏感数据。
- 一旦出现新的 XSS，攻击脚本仍可能读取 Token；因此 Token 不能再使用 24h/7d 的长默认有效期。
- 第三方地图脚本是 CSP 允许列表中的显式例外，后续新增第三方脚本必须更新本文件并复核 CSP。

## 2. 已落地补偿控制

| 控制项 | 当前实现 |
|---|---|
| 短 Token | `TRACE_JWT_EXPIRATION_HOURS` 默认从 24h 缩短到 2h；`TRACE_JWT_REMEMBER_EXPIRATION_DAYS` 默认从 7d 缩短到 1d。 |
| 立即失效 | 登出、刷新、改密、角色/密码版本变化继续依赖 Redis 黑名单与 `token_version`；Redis 异常 fail-closed 返回 503。 |
| 存储适配层 | 新增 `frontend/src/core/auth/authStorage.js`，所有 token/user localStorage 读写集中在该模块，便于后续迁移到内存 token 或 Cookie。 |
| CSP 基线 | `frontend/index.html` 新增 CSP meta：默认仅允许同源资源，脚本只允许同源与高德地图脚本域，图片/连接只按地图和本地运行需要开白。 |
| 异常缓存处理 | 损坏的 `user` 缓存会被清理，避免启动时 JSON 解析异常导致前端不可用。 |

## 3. CSP 部署建议

`index.html` 中的 meta CSP 是前端构建产物的兜底策略。生产部署时应优先在网关/Nginx/CDN 返回 HTTP Header，并保持与 meta 内容一致：

```nginx
add_header Content-Security-Policy "default-src 'self'; base-uri 'self'; object-src 'none'; form-action 'self'; frame-ancestors 'none'; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://*.amap.com https://*.autonavi.com; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://*.amap.com; img-src 'self' data: blob: https://*.amap.com https://*.autonavi.com; connect-src 'self' https://*.amap.com https://*.autonavi.com; font-src 'self' data: https://fonts.gstatic.com; worker-src 'self' blob:; media-src 'self' blob:" always;
```

说明：

- `style-src 'unsafe-inline'` 用于兼容 Vue 动态样式 + 高德 marker 内容 + 第三方地图样式注入。
- `script-src 'unsafe-inline'` 为高德地图 `watchSize` 内部使用 `javascript:` URL 监听容器尺寸而必需；移除会导致地图瓦片加载失败、只能渲染 marker（手机端尤其明显，桌面偶有重试救回）。
- `script-src 'unsafe-eval'` 为高德地图 JS API 内部 `new Function(...)` 路径所需。
- 高德相关域名统一用 `https://*.amap.com` 通配，覆盖 `webapi.amap.com`（脚本）、`restapi.amap.com`（搜索）、`vdata.amap.com`（瓦片元数据 JSONP）、`tile.amap.com` 等多个子域。
- 如生产前端不需要高德地图，可移除 `https://*.amap.com`、`https://*.autonavi.com`、以及 script-src 的 `'unsafe-inline' 'unsafe-eval'`。
- meta CSP 不支持可靠设置 `frame-ancestors`，生产必须使用 HTTP Header 补齐防嵌套策略。

## 4. 后续迁移路径

若后续要彻底降低 XSS 后 Token 被读取的影响，建议拆分为独立任务：

1. 后端引入短 access token + 轮换 refresh token，refresh token 使用 `httpOnly; Secure; SameSite` Cookie。
2. 前端仅在内存中保存 access token，页面刷新后通过 refresh Cookie 换取新 access token。
3. 所有会修改状态的接口补充 CSRF Token 或 SameSite 策略验证。
4. CORS 从 `Authorization` Header 模型调整为 Cookie 凭证模型，并补齐端到端测试。

## 5. 验证记录

- `frontend/src/core/auth/__tests__/authStorage.test.js` 覆盖集中存储读写、清理与损坏缓存恢复。
- `backend/src/test/java/com/example/trace/security/JwtUtilTest.java` 覆盖 2h/1d Token 有效期。
- `backend/src/test/java/com/example/trace/config/SecurityDefaultsConfigTest.java` 锁定 `application.yml` 中的短 Token 默认值。

