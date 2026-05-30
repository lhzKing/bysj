# T25 风险评估

| 风险 | 概率 | 影响 | 说明 | 缓解策略 |
| --- | --- | --- | --- | --- |
| 权限继承行为漂移 | Medium | High | `trace:inbound -> trace:scan -> trace:view` 等传递关系一旦改错，会导致授权结果变化 | 先用测试锁定继承闭包，再抽取继承展开器 |
| `matchAll` / `matchAny` 语义被破坏 | Medium | High | 注解权限校验依赖该逻辑 | 为 `hasPermission(roleId, String[], boolean)` 建立显式单测 |
| API path 匹配规则退化 | Medium | High | `PermissionInterceptor` 无注解时依赖 `hasApiPermission` | 为 method/path + wildcard 场景补测试，抽出独立匹配器 |
| 缓存失效遗漏 | High | High | 角色权限变更后若缓存未清，会出现旧权限残留 | 保持 `clearCache()` 兼容入口，并锁定 `RoleServiceImpl` 调用点 |
| `roleCode -> roleId` 解析兼容性破坏 | Medium | Medium | `LoginInterceptor` 依赖该逻辑写入 request attribute | 为命中 / miss / null 输入补测试 |
| 拆分类时包结构混乱 | Medium | Medium | 易演变为“只是把代码挪到更多文件” | 在 Phase 1 先确定包结构与命名规则 |
| 误扩散到前端或无关服务 | Low | Medium | 违背当前整改约束 | 明确 out-of-scope，进度文档中持续声明“UI 冻结” |
| 测试不足导致回归漏检 | High | High | 登录、鉴权、扫描链路均依赖权限服务 | 以测试先行为前提，再做拆分 |

## 风险热点说明

### 1. 鉴权链路属于高敏区域

T25 所涉及的 `LoginInterceptor`、`PermissionInterceptor`、`AuthController`、`TraceController` 都位于认证/权限主链路，属于高回归敏感区。  
这意味着 T25 不适合一次性“大重写”，而应采用小步重构。

### 2. 缓存与查询混合导致测试不透明

当前 `PermissionService` 的缓存与查询耦合，容易出现：

- 第一次调用正确，第二次调用因缓存逻辑异常而错误
- 修改角色权限后旧缓存未被清空
- API 权限缓存与权限码缓存失效粒度不一致

### 3. API fallback 行为不能被忽略

`PermissionInterceptor` 逻辑是：

1. 优先检查方法级 `@RequirePermission`
2. 再检查类级注解
3. 若均无注解，回退到 API 权限表匹配

因此 T25 不能只关注权限码集合，还必须保住 API 权限匹配路径。

## 测试优先级建议

### P0

- 继承链路
- `matchAll` / `matchAny`
- API method/path 匹配
- 清缓存行为

### P1

- `roleCode -> roleId`
- `PermissionInterceptor` 的注解优先 / fallback 分流

### P2

- 更细的日志断言或内部实现细节测试（能不锁死则不锁死）
