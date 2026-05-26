# 工业零配件供应链溯源系统 —— 云端部署指导（Azure 日本 + 1Panel + Cloudflare）

本指导基于**你已有的 Azure 日本节点 + 1Panel + Cloudflare 部署经验**，与你之前部署 New API 的流程一致。如果服务器是全新机器，按 [附录 A](#附录-a全新服务器才需要做的初始化) 先做一遍初始化。

---

## 0. 部署架构总览

```
                          用户浏览器
                              │ HTTPS 443
                              ▼
                    ┌──────────────────┐
                    │   Cloudflare CDN  │  ← 边缘 HTTPS、DDoS、缓存
                    │   trace.coldhz.codes
                    │   trace-api.coldhz.codes
                    └────────┬─────────┘
                              │  HTTPS（回源用 Cloudflare 源证书，15 年）
                              ▼
                    ┌──────────────────┐
                    │  Azure 日本服务器  │ 20.191.176.25  Ubuntu
                    │  1Panel / OpenResty  ：80/443
                    └──┬───────────────┬─┘
                       │ trace.*       │ trace-api.*
                       │ → 18080       │ → 18081
                       ▼               ▼
              ┌───────────────┐ ┌────────────────┐
              │ trace-frontend │ │ trace-backend  │
              │  nginx(:80)    │ │ spring-boot    │
              │  /srv/dist     │ │ :8080          │
              └───────────────┘ └───┬────────┬───┘
                                    │        │
                                    ▼        ▼
                              ┌───────┐ ┌──────┐
                              │ mysql │ │redis │
                              └───────┘ └──────┘
                          （4 个容器由本目录的 docker-compose 编排）
```

**核心思路**：
- **服务器初始化**（SSH 加固 / ufw / Docker / 1Panel）你已经做完了，**这一步直接跳过**
- **Cloudflare 源证书**已经在 1Panel 里上传过了，**直接复用**
- 本毕设项目用 **docker-compose** 一键拉起 4 个容器（mysql/redis/backend/frontend）
- 用 **1Panel 的网站 → 反向代理**功能加 2 条规则，把 `trace.coldhz.codes` / `trace-api.coldhz.codes` 反代到 compose 暴露的本地端口

---

## 1. 在 Cloudflare 加 DNS 记录

打开 Cloudflare → `coldhz.codes` → DNS → 添加记录：

| 类型 | 名称 | 内容 | 代理状态 |
|---|---|---|---|
| A | `trace` | `20.191.176.25` | 🟧 已代理 |
| A | `trace-api` | `20.191.176.25` | 🟧 已代理 |

等 1–3 分钟生效。本地 `ping trace.coldhz.codes` 返回 Cloudflare 的 IP（不是 Azure 的真实 IP）即可。

---

## 2. 把项目代码上传到服务器

### 2.1 准备目录（与 1Panel 应用风格一致）

放到 `/opt/1panel/apps/bysj/`，和你已经在跑的 `/opt/1panel/apps/new-api/` 并排。这个目录默认 owner 是 `root`，普通用户没写权限，先一次性授权：

```bash
# SSH 到服务器后
sudo mkdir -p /opt/1panel/apps/bysj
sudo chown -R azureuser:azureuser /opt/1panel/apps/bysj
```

> 改 owner 而不是 `chmod 777`：保证 `docker compose` 能在 `mysql-data/`、`redis-data/`、`keys/` 里读写，同时避免世界可写带来的安全风险。

### 2.2 把代码丢进去

在本地 PowerShell：

```powershell
# 推荐：用 git
ssh -i D:\obsidian_note\CS_learning\azure\cloud-japan-east_key.pem -p 9683 azureuser@20.191.176.25
# 上去后：
git clone <你的仓库地址> /opt/1panel/apps/bysj
# 或者你不想用 git
exit
scp -i D:\obsidian_note\CS_learning\azure\cloud-japan-east_key.pem -P 9683 -r `
  d:\bysj\* azureuser@20.191.176.25:/opt/1panel/apps/bysj/
```

下文统一假设代码路径为 `/opt/1panel/apps/bysj/`。

---

## 3. 准备运行所需的密钥和配置

SSH 到服务器，进入项目目录：

```bash
cd /opt/1panel/apps/bysj/deploy
cp .env.example .env
nano .env
```

`.env` 至少改这些（其它字段保持默认即可）：

| 字段 | 值 |
|---|---|
| `TRACE_DB_PASSWORD` / `TRACE_DB_ROOT_PASSWORD` / `TRACE_REDIS_PASSWORD` | 各自一段强密码 |
| `TRACE_JWT_SECRET` | `openssl rand -base64 48` 的输出 |
| `TRACE_CORS_ALLOWED_ORIGINS` | `https://trace.coldhz.codes` |
| `TRACE_PUBLIC_BASE_URL` | `https://trace.coldhz.codes` |
| `VITE_AMAP_KEY` | 你的高德 Web JS Key |

`Ctrl+O` 保存，`Ctrl+X` 退出。

创建数据目录、生成 RSA 签名密钥（项目要求外置，不能用 auto-generate）：

```bash
mkdir -p keys mysql-data redis-data mysql-init

# 项目自带的密钥对（一次性，永久使用）
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out ./keys/private.pem
openssl rsa -in ./keys/private.pem -pubout -out ./keys/public.pem
chmod 600 ./keys/private.pem

# 让 MySQL 首次启动自动建表 —— 用整合版脚本，无需再跑 migrate_v*.sql
cp ../backend/sql/schema_consolidated.sql ./mysql-init/01-init.sql
```

> `schema_consolidated.sql` 是 `init_schema.sql + migrate_v2~v21` 共 22 份脚本的**整合产物**（mysqldump 等价），包含：15 张表的最终结构 + 6 个角色 + 30 个权限 + 角色权限映射 + `superadmin` 账号种子。**只用它一个文件就够了**，不要再叠加 init_schema.sql 或任何 migrate_v*.sql，否则会因为表已存在/键冲突报错。
>
> 详细的"为什么不用 init + migrate 而用 consolidated"见本 README §11「数据库初始化与数据管理」。

---

## 4. 一键启动 4 个容器

```bash
cd /opt/1panel/apps/bysj/deploy
docker compose up -d --build
docker compose ps                  # 四个容器都该 Up，mysql/redis 还会带 (healthy)
docker compose logs -f backend     # 看后端启动有没有 ERROR、prod guard 没拒
```

启动成功后：
- 前端 nginx 监听 `127.0.0.1:18080`
- 后端 spring-boot 监听 `127.0.0.1:18081`
- 都**只绑定回环地址**，外网不能直连，必须经下一步的 OpenResty 反代

本机自测一下：

```bash
curl -I http://127.0.0.1:18080            # 期望 200，返回 index.html
curl http://127.0.0.1:18081/api/traces/public-key   # 期望返回公钥 JSON
```

---

## 5. 在 1Panel 里加两条 OpenResty 反代规则

**这一步流程和你笔记里 New API 部署完全一样**，区别只是改两个域名和两个端口。

### 5.1 前端反代（trace.coldhz.codes → 18080）

1. 1Panel 侧栏 → **网站 → 网站**
2. 顶栏 → **反向代理**
3. 主域名填：`trace.coldhz.codes`
4. 代理地址填：`http://127.0.0.1:18080`
5. 点 **确认**
6. 在列表里点 `trace.coldhz.codes` 右侧的 **配置**
7. → **HTTPS** → **启用 HTTPS**
8. SSL 选项选 **选择已有证书** → 选你之前导入的那个 Cloudflare 源证书（`*.coldhz.codes`）
9. 左下角 **保存**

### 5.2 后端 API 反代（trace-api.coldhz.codes → 18081）

完全重复 5.1，只把：
- 主域名改成 `trace-api.coldhz.codes`
- 代理地址改成 `http://127.0.0.1:18081`

### 5.3 Cloudflare 侧：SSL 模式设为「完全（严格）」

你笔记里第 8 节已经配过一次，全局生效，**这里不用再改**。如果你不确定：
- Cloudflare 侧栏 → SSL/TLS → 确认是 **完全（严格）**
- 边缘证书 → 确认 **始终使用 HTTPS** 已开

---

## 6. 处理"前端用相对路径 /api"的小问题

本项目前端写死 `axios.create({ baseURL: '/api' })`——意思是所有 API 请求都打在**前端自己所在的域名 + /api** 上。

我们现在前端在 `trace.coldhz.codes`、后端在 `trace-api.coldhz.codes`，**不是同一个域名**。

有两种解决方式，**选一种**：

### 方案 A（推荐，零代码改动）：把 /api 也通过前端域名反代过去

在 1Panel 修改 `trace.coldhz.codes` 这条反代规则，**追加**一个 location：

1. 在 1Panel 网站列表 → `trace.coldhz.codes` → **配置** → **反向代理**
2. **新增**一条反向代理：
   - 名称：`api`
   - 代理目录：`/api/`
   - 代理地址：`http://127.0.0.1:18081`
3. 保存

这样浏览器访问 `https://trace.coldhz.codes/api/xxx` 会被 OpenResty 转给后端容器，前端代码不用改、CORS 也不会触发（同源）。

**采用方案 A 的话，第 5.2 步「`trace-api` 反代」可以不做、Cloudflare 那条 DNS 也不用加**（但留着也没坏处，将来想给第三方暴露独立 API 域名直接用）。

### 方案 B：保留独立 API 域名，前端代码改 baseURL

把 `frontend/src/core/api/request.js` 的 `baseURL: '/api'` 改成 `baseURL: 'https://trace-api.coldhz.codes/api'`，重新 `docker compose build frontend`。
然后需要把 `TRACE_CORS_ALLOWED_ORIGINS` 加上 `https://trace.coldhz.codes`（已经在 .env 里了）。

> **答辩演示建议用方案 A**：少一次配置、少一种出错点、同源没有 CORS 问题。

---

## 7. 验证

浏览器开 `https://trace.coldhz.codes`：

- 应直接 HTTPS、显示登录页
- 登录默认账号 `superadmin / superadmin123456`
- F12 → Network → 调用 `/api/auth/login` → 状态 200、返回 token
- 进入"打印标签"页生成二维码，手机扫码应跳到 `https://trace.coldhz.codes/public/traces/xxx`

灌测试数据（可选）：

```bash
TOKEN=$(curl -s -X POST https://trace.coldhz.codes/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"superadmin","password":"superadmin123456"}' \
  | grep -oP '"token":"\K[^"]+')

curl -X POST "https://trace.coldhz.codes/api/admin/generate-sample-data?count=500" \
  -H "Authorization: Bearer $TOKEN"
```

**登录后第一件事：去用户管理改 superadmin 密码！**

---

## 8. 日常运维

```bash
cd /opt/1panel/apps/bysj/deploy

# 看日志
docker compose logs -f backend
docker compose logs -f frontend

# 重启某个服务
docker compose restart backend

# 改了代码（git pull 之后）重建并启动
git -C /opt/1panel/apps/bysj pull
docker compose up -d --build

# 备份数据库（建议放 cron 每天跑）
docker compose exec mysql mysqldump \
  -uroot -p"$(grep TRACE_DB_ROOT_PASSWORD .env | cut -d= -f2)" trace_db \
  > backup-$(date +%F).sql

# 完全停止（数据保留）
docker compose down

# 看资源占用
docker stats
```

---

## 9. 排障速查

| 现象 | 原因 / 排查 |
|---|---|
| `docker compose up` 失败：`backend` 启动即退出 + `JWT secret too short` | `.env` 的 `TRACE_JWT_SECRET` 不到 32 字节，重跑 `openssl rand -base64 48` |
| `backend` 报 `signature key path rejected` | `ProdProfileConfigGuard` 拒绝了路径；必须是 `/app/keys/*.pem` 且 `auto-generate=false` |
| 浏览器打开 `trace.coldhz.codes` → 521/522 | 宿主机 `ufw` 没放行 80/443，或 Azure 安全组没开；按你笔记里 `sudo ufw allow 80/tcp && sudo ufw allow 443/tcp && sudo ufw reload` |
| 浏览器打开 → Cloudflare 525（SSL 握手失败） | 1Panel 那条反代没启用 HTTPS / 没绑定证书；回到第 5 步检查 |
| 前端能开，调 `/api/*` 全 404 | 用了方案 A 但没在 `trace.coldhz.codes` 加 `/api/` 反代规则（第 6 步） |
| 前端能开，调 `/api/*` 报 CORS | 用了方案 B 但 `TRACE_CORS_ALLOWED_ORIGINS` 没包含前端域名；改完 `docker compose restart backend` |
| 手机扫码跳到 `localhost` 或 `127.0.0.1` | `TRACE_PUBLIC_BASE_URL` 没改成 `https://trace.coldhz.codes`；改完重启 backend |
| `docker compose ps` 显示 mysql/redis healthy 但 backend 一直重启 | 看 `docker compose logs backend`；多半是 DB 用户名密码、CORS 必填项、JWT secret 中某一项被 prod guard 拒了 |
| Docker Hub 拉镜像慢 | 在 `/etc/docker/daemon.json` 加 `{"registry-mirrors":["https://docker.m.daocloud.io"]}`，然后 `sudo systemctl restart docker` |
| 改了 `schema_consolidated.sql` 后 `docker compose restart mysql` 没生效 | `mysql-init/` **只在数据目录为空时执行一次**；要么写新的 migrate 脚本进容器手动跑，要么按 §11.6 推倒重来 |
| `mysql-init/` 里同时放了 `init_schema.sql` 和 `migrate_v*.sql` 启动报错 | 这两套与 `schema_consolidated.sql` 是**三选一**关系，云端只用 consolidated 那一个；清空 `mysql-init/` 重放 |

---

## 10. 端口分配速查（避免与你机器现有服务冲突）

| 端口 | 用途 | 暴露范围 |
|---|---|---|
| 22 | 旧 SSH（你已关） | — |
| **9683** | SSH | 公网（Azure 安全组 + ufw 都开） |
| **80 / 443** | OpenResty | 公网 |
| **23562** | 1Panel 面板 | 公网（域名 + 安全入口） |
| **5000** | New API | 仅本机，反代到 `api.coldhz.codes` |
| **18080** | 本项目前端 nginx | 仅 127.0.0.1 |
| **18081** | 本项目后端 spring-boot | 仅 127.0.0.1 |
| 3306 / 6379 / 8080 | mysql / redis / spring 容器内部 | 仅 compose 内部网络 |

如果 18080/18081 与你以后装的别的东西撞了，改 `.env` 里的 `HOST_FRONTEND_PORT` / `HOST_BACKEND_PORT` 就行，1Panel 反代地址同步改。

---

## 11. 数据库初始化与数据管理

> 这一节回答最容易绕住的问题：**云上 Docker 里的 MySQL 和我本地的 MySQL 是什么关系？表是怎么来的？数据会同步吗？**

### 11.1 概念图

```
本地开发                              云端 Docker
─────────                            ─────────
你电脑装的 MySQL                      docker compose 起的 mysql 容器
└── trace_db                          └── trace_db
    ├── sys_user (你日常测试数据)         ├── sys_user (只有 superadmin 1 行)
    ├── trace_snapshot                    ├── trace_snapshot (空表)
    └── ...                               └── ...

  ↑                                    ↑
  你历史上跑过 init + v2~v21            mysql-init/01-init.sql 容器首次启动自动执行
  + 日常测试积累的数据                   = schema_consolidated.sql （只有表 + 种子，无业务数据）
```

**两个数据库完全独立**：本地存的业务数据云上看不到，云上的也不会同步回本地。结构（表定义）一样，因为都源自同一份 SQL。

### 11.2 为什么用 `schema_consolidated.sql` 而不是 `init_schema.sql` + `migrate_v*.sql`？

历史上你的库是这样长出来的：

```
init_schema.sql    （v1 初始）
  ↓ 加字段、加表
migrate_v2_security_enhance.sql
migrate_v3_fine_grained_permissions.sql
...
migrate_v21_part_enabled.sql
```

如果照搬"本地走过的路"——把 init + 22 个 migrate 全部按顺序丢进 `mysql-init/`——理论上能跑通，但有 3 个坑：

1. **顺序依赖严重**：`mysql-init/` 按文件名字典序执行，要保证 `02-migrate_v2.sql` 在 `03-migrate_v3.sql` 之前；写错就崩
2. **MyBatis-Plus 已经把所有约束在 v21 整合到了最终态**，重放 22 次中间态是无用功，启动时间长
3. **某些 migrate 脚本带 `IF EXISTS` / 兼容代码**，依赖中间某个版本的数据状态，云上空库跑会报错

[backend/sql/schema_consolidated.sql](backend/sql/schema_consolidated.sql) 是你 2026-05-23 用 mysqldump 等价方式**整合出的最终态**：
- 15 张表的最终结构（含索引、外键、CHECK、生成列）
- 基础种子：6 个角色 + 30 个权限 + 角色权限映射
- `superadmin` 账号（BCrypt 哈希已写入，密码 `superadmin123456`）
- **不含**任何业务数据（零件、追溯链、扫码记录等都是 0 条）

**结论：云端部署只用这一个文件，省心、不会出错。**

### 11.3 容器首次启动到底发生了什么？

[deploy/docker-compose.yml](deploy/docker-compose.yml) 关键两行：

```yaml
volumes:
  - ./mysql-data:/var/lib/mysql                    # ① 数据持久化目录
  - ./mysql-init:/docker-entrypoint-initdb.d:ro    # ② 启动脚本目录（mysql 镜像内置约定）
```

**第一次** `docker compose up`：
1. 容器启动，MySQL 发现 `/var/lib/mysql` 是空的 → 判定为首次启动
2. 创建 root 账号 + `.env` 指定的 `trace_db` 库 + 普通用户
3. **按字典序扫 `/docker-entrypoint-initdb.d/`**，依次执行 `.sql` 文件
4. 我们里面只有 `01-init.sql`（= `schema_consolidated.sql` 的副本）→ 表+种子全部建好

**第二次起的所有** `docker compose up`：
1. 容器启动，发现 `/var/lib/mysql` 已有数据 → 判定为非首次
2. **完全跳过** `mysql-init/`，直接复用上次的数据

→ 改了 `schema_consolidated.sql` 后**光重启不会生效**，因为根本不会再读那个目录。

### 11.4 业务数据从哪来？三种方式

| 方式 | 适合场景 | 数据来源 |
|---|---|---|
| **A. 不灌**，直接登录用 | 答辩演示新功能，从 0 开始干净 | 仅 superadmin + 角色权限 |
| **B. 调"种主数据 + 生成示例数据"接口** | 演示需要看到列表/图表/任务/聚合都有数据 | 后端代码生成完整 demo 数据 |
| **C. 从本地导出再导入** | 你本地有精心准备的演示链路 | 你本地 MySQL 的真实数据 |

#### B：两步调接口生成（最方便、零外部依赖）

`schema_consolidated.sql` 只灌 RBAC + superadmin。要让所有页面都有数据：

```bash
TOKEN=$(curl -s -X POST https://trace.coldhz.codes/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"superadmin","password":"superadmin123456"}' \
  | python -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")

# 第 1 步：种主数据（demo 用户 / 节点 / SPU / 用户-节点绑定，幂等可反复调）
curl -X POST "https://trace.coldhz.codes/api/admin/seed-master-data" \
  -H "Authorization: Bearer $TOKEN"

# 第 2 步：一次性生成全部业务流水（batch / trace_code / lifecycle / snapshot /
# flow_task / scan / aggregation 共 8 张表）
curl -X POST "https://trace.coldhz.codes/api/admin/generate-sample-data?count=500" \
  -H "Authorization: Bearer $TOKEN"
```

**返回示例**（`count=500` 用时 ~30s，看板/列表/任务/聚合页全有数据）：

```json
{
  "batches": 25, "traceCodes": 500, "lifecycleLogs": 3009, "snapshots": 500,
  "flowTasks": 60, "flowTaskScans": 623, "aggregations": 325, "durationMillis": 35784
}
```

**业务保证**：
- 每条 lifecycle log 的 hash + RSA 签名由后端运行时密钥**真实生成**，前端 `/verify` 接口验签 100% 通过
- demo 用户密码：`producer/producer123456`、`warehouse/warehouse123456`、`logistics/logistics123456`、`user/user123456`（producer2/warehouse2/logistics2/dealer1 与对应主用户共用密码）

**清空重灌**：

```bash
# 清流水（保留节点/SPU/用户/绑定等主数据）
curl -X DELETE "https://trace.coldhz.codes/api/admin/clear-trace-data?confirm=DELETE_TRACE_DATA" \
  -H "Authorization: Bearer $TOKEN"

# 然后重新调 generate-sample-data 即可（无需再调 seed-master-data）
```

`count` 上限由后端 `TRACE_DEMO_DATA_MAX_GENERATE_COUNT` 控制（默认 500）。
prod profile 默认禁用所有 demo 接口；如需启用，设 `TRACE_DEMO_DATA_ENABLED=true`。

> 历史方式 `scripts/seed_extended_data.py`（Python + pymysql 直连）作为离线兜底
> 保留：服务器没起或需要把数据 dump 成 SQL 文件时可用。日常演示用 HTTP API 即可。


#### C：本地数据库整库迁移到云上

本地（PowerShell）导出：

```powershell
# 注意：mysqldump 路径看你装的 MySQL，下面以系统 PATH 已配为例
mysqldump -u root -p `
  --default-character-set=utf8mb4 `
  --single-transaction `
  --routines --triggers --events `
  trace_db > d:\bysj\local-dump.sql
```

传到服务器：

```powershell
scp -i D:\obsidian_note\CS_learning\azure\cloud-japan-east_key.pem -P 9683 `
  d:\bysj\local-dump.sql azureuser@20.191.176.25:/opt/1panel/apps/bysj/
```

服务器上导入到 Docker MySQL：

```bash
cd /opt/1panel/apps/bysj/deploy
# 注意：要先确保数据库已经空了（如果是全新部署，跳过下一行）
# docker compose exec mysql mysql -uroot -p"$ROOT" -e "DROP DATABASE trace_db; CREATE DATABASE trace_db CHARACTER SET utf8mb4;"

docker compose exec -T mysql mysql \
  -uroot -p"$(grep TRACE_DB_ROOT_PASSWORD .env | cut -d= -f2)" \
  trace_db < /opt/1panel/apps/bysj/local-dump.sql
```

导完后云上库 = 本地库的快照。

> ⚠ 如果你本地数据里 `superadmin` 的密码不是默认的，导入后云上登录请用你本地的密码。

### 11.5 数据持久化与丢失风险

| 操作 | 数据保留？ |
|---|---|
| `docker compose restart` | ✅ 在 |
| `docker compose down`（删容器） | ✅ 在（数据在宿主机 `./mysql-data/`，与容器无关） |
| 服务器重启 / 关机再开机 | ✅ 在 |
| 把整个项目目录 `rm -rf` | ❌ **没了** |
| `rm -rf deploy/mysql-data/` | ❌ **没了** |
| 改了 `01-init.sql` 后重启容器 | ⚠ **结构不变**（mysql-init 不会重跑） |

所以 [deploy/.gitignore](deploy/.gitignore) 把 `mysql-data/` 列入忽略——真实数据不能提交到 git。

### 11.6 想重建数据库怎么办？

**仅当未上线、没有真实数据时**可以推倒重来：

```bash
cd /opt/1panel/apps/bysj/deploy
docker compose down
sudo rm -rf mysql-data/                                          # ⚠ 不可逆
cp ../backend/sql/schema_consolidated.sql ./mysql-init/01-init.sql   # 同步最新版
docker compose up -d --build
docker compose logs -f mysql                                     # 应看到 init 脚本被执行的日志
```

**已经上线有真实数据**了想改表结构 → 写新的 migrate 脚本，进容器手动执行：

```bash
docker compose cp ../backend/sql/migrate_vXX_xxx.sql mysql:/tmp/m.sql
docker compose exec mysql mysql -uroot -p"$ROOT" trace_db -e "source /tmp/m.sql"
# 同时把这次改动**回填**到 schema_consolidated.sql，保证下次推倒重建时结构同步
```

### 11.7 备份建议

放 cron 每天凌晨备份：

```bash
# /etc/cron.d/trace-mysql-backup
0 3 * * * azureuser cd /opt/1panel/apps/bysj/deploy && \
  docker compose exec -T mysql mysqldump -uroot \
    -p"$(grep TRACE_DB_ROOT_PASSWORD .env | cut -d= -f2)" \
    trace_db | gzip > /opt/1panel/apps/bysj/backups/trace-$(date +\%F).sql.gz && \
  find /opt/1panel/apps/bysj/backups -name "trace-*.sql.gz" -mtime +14 -delete
```

`mkdir -p /opt/1panel/apps/bysj/backups` 一次即可（注意 owner 是 azureuser）。

---

## 12. HTTPS 链路与证书

> 这一节回答另一个容易绕住的问题：**部署后 HTTPS 是谁给的？我本地 `frontend/certs/localhost.{key,crt}` 和 Cloudflare 那张源证书是什么关系？需要把本地证书也上传服务器吗？**

### 12.1 一句话先讲清

**两张证书各管各的，不能互相代替；部署到服务器后，你不需要给容器配任何证书，HTTPS 由 Cloudflare 自动提供。**

### 12.2 本地开发的链路（一段 HTTPS）

```
你电脑浏览器  ──HTTPS──►  Vite dev server  ──HTTP──►  Spring Boot
              :5173                              :8080
              ▲
              │ 这一段要 HTTPS，因为浏览器 getUserMedia（摄像头）只在 secure context 下允许
              │
              证书：frontend/certs/localhost.{key,crt}
              签的域名：localhost
              CA：generate-cert.js 临时建的，没人信任 → 浏览器弹"不安全"，你手动点继续
```

### 12.3 部署后的链路（三段，不是一段）

```
全世界用户浏览器
   │
   ├─ A ── HTTPS ──►  Cloudflare 边缘节点
   │                       │
   │                       ├─ B ── HTTPS ──►  Azure 服务器 OpenResty :443
   │                                              │
   │                                              ├─ C ── HTTP ──►  容器内 nginx :80
   │                                                                       │
   │                                                                       └──►  容器内 backend
```

| 段 | 证书 | 谁签的 | 谁信任 |
|---|---|---|---|
| **A**：浏览器 → CF 边缘 | Cloudflare 通用 SSL（系统给的，你看不到） | 公共 CA | 全世界浏览器 ✓ |
| **B**：CF → 你服务器 | **Cloudflare 源证书 `*.coldhz.codes`**（你上传到 1Panel 那张） | Cloudflare 自家 CA | 仅 Cloudflare 边缘（普通浏览器不信） |
| **C**：OpenResty → 容器 | **无**，纯 HTTP | — | — |

### 12.4 两张证书能不能互换

**不能**。证书必须绑定**域名**，签发什么域名就只能证明那个域名的身份：

- 本地 `localhost.crt` 签的是 `localhost`，给真域名 `trace.coldhz.codes` 用 → 浏览器 `ERR_CERT_COMMON_NAME_INVALID` 直接拒绝
- Cloudflare 源证书签的是 `*.coldhz.codes`，给 `localhost:5173` 用 → 同样的报错

它们解决的是**不同链路段的 TLS 信任问题**，不可替换。

### 12.5 部署后扫码摄像头还能用吗？

**能用，且你什么都不用做**。

- 用户访问的是 `https://trace.coldhz.codes`
- 浏览器看到的是 Cloudflare 给的**通用 SSL 证书**（A 段），完全信任，绿锁🔒
- secure context 成立 → 摄像头 API 可用 → 扫码功能正常

前提是你笔记里第 8 节那个「Cloudflare → 边缘证书 → 始终使用 HTTPS」打开了（你已经开过）。

### 12.6 为什么容器里那个 nginx 不配 HTTPS

工业界规范：**外层网关做 TLS 终结，内部一律 HTTP**。原因：

- 容器 nginx 只跟同机的 OpenResty 通信（`127.0.0.1:18080`），流量不出本机网卡，没人能窃听
- 给它配 HTTPS 要再准备一张自签证书，徒增复杂度，毫无收益
- Kubernetes / AWS ELB / 阿里云 SLB 全是这个模式

所以 [frontend/nginx.conf](../frontend/nginx.conf) 里只 `listen 80`，没有 `listen 443`。

### 12.7 `frontend/certs/` 要上传服务器吗

**不要**。也不能——它们已经被 [frontend/.gitignore](../frontend/.gitignore) 排除（`certs/*` 那行），自签证书本来就不该入库。

如果你曾经因为 `vite build` 报「未找到 HTTPS 证书」误以为是少了证书，那其实是 `vite.config.js` 的一个 bug——`vite build` 产物是纯静态文件根本不需要证书。修复已经合入：只有 dev server（`command === 'serve'`）才加载证书，build 跳过。

### 12.8 整套 HTTPS 体系的"谁负责什么"速记

| 你想要的效果 | 谁负责 | 你做了什么 |
|---|---|---|
| 用户在浏览器看到绿锁 | Cloudflare 通用 SSL（A 段） | 把域名托管到 CF + 「始终使用 HTTPS」开关 |
| Cloudflare 回源到你服务器走 HTTPS（不被中间人窃听） | Cloudflare 源证书 `*.coldhz.codes`（B 段） | 1Panel 上传源证书 + OpenResty 反代启用 HTTPS + CF 侧 SSL 模式「完全（严格）」 |
| 容器之间通信 | 不需要 TLS | 啥也不做（纯 HTTP，仅本机回环） |
| 本地开发摄像头能用 | 本地 `certs/localhost.{key,crt}`（独立链路） | `node generate-cert.js` 生成自签证书 |

---

## 附录 A：全新服务器才需要做的初始化

> 你现在的 Azure 日本服务器已经做完了下面这些，**不用重复**。仅在你换新机器时参考。

### A.1 SSH 加固（参考你的笔记第 2/3 节）

```bash
sudo apt update
sudo nano /etc/ssh/sshd_config        # 把 Port 22 改成 Port 9683
sudo systemctl restart ssh
sudo ufw allow 9683/tcp
sudo ufw enable
```

Azure 网页控制台同步在「网络 → 入站端口规则」加 9683 / 80 / 443 / 23562。

### A.2 安装 Docker

```bash
curl -fsSL https://get.docker.com | sudo bash
sudo usermod -aG docker $USER         # 重连一次让用户组生效
```

### A.3 安装 1Panel

```bash
curl -sSL https://resource.fit2cloud.com/1panel/package/quick_start.sh -o quick_start.sh
sudo bash quick_start.sh              # 端口你自己定，例如 23562
```

### A.4 域名托管到 Cloudflare、申请 15 年源证书

完全按你笔记的第 5、6、8 节操作。证书上传到 1Panel 后下面所有项目都能复用同一张。

### A.5 在 1Panel 安装 OpenResty

1Panel 侧栏 → 应用商店 → OpenResty → 安装。装完后第 5 步的"网站 → 反向代理"才会出现。

---

## 附录 B：与你旧项目（New API）共存说明

本项目和你已经在跑的 New API 完全互不干扰：

- 端口：本项目用 18080/18081（内网回环），New API 用 5000；不冲突
- 容器名：本项目用 `trace-*` 前缀，New API 用 `new-api`；不冲突
- 数据卷：本项目用 `/opt/1panel/apps/bysj/deploy/{mysql,redis}-data`，New API 用 `/opt/1panel/apps/new-api/data`；不冲突
- 域名：本项目用 `trace.*` / `trace-api.*`，New API 用 `api.*`；不冲突
- 证书：共用同一张 `*.coldhz.codes` Cloudflare 源证书

所以本项目可以随时 `docker compose down` 关掉，不会影响 `api.coldhz.codes` 的 New API。
