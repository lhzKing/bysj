# R-P2-05 工作区运行产物与大文件归档记录

- 日期：2026-05-03
- 任务：R-P2-05 工作区运行产物与大文件清理策略
- 策略：不删除诊断材料；将根目录、`frontend/`、`backend/` 下的运行产物非删除式归档到 `runtime_state/repo-hygiene/2026-05-03/`。
- 归档清单：`runtime_state/repo-hygiene/2026-05-03/archive-manifest.json`
- 本次归档数量：21 个文件
- 本次归档总量：219.27 MiB

## 归档明细

| 原路径 | 归档路径 | 大小 | 原最后修改时间 |
|---|---|---:|---|
| `backend/backend_runtime.err.log` | `runtime_state/repo-hygiene/2026-05-03/backend/backend_runtime.err.log` | 53 B | 2026-04-10 19:31:39 |
| `backend/backend_runtime.log` | `runtime_state/repo-hygiene/2026-05-03/backend/backend_runtime.log` | 0 B | 2026-04-10 19:31:29 |
| `backend/hs_err_pid31568.log` | `runtime_state/repo-hygiene/2026-05-03/backend/hs_err_pid31568.log` | 14.31 KiB | 2026-04-16 23:19:05 |
| `backend/hs_err_pid4536.log` | `runtime_state/repo-hygiene/2026-05-03/backend/hs_err_pid4536.log` | 14.31 KiB | 2026-04-16 23:19:05 |
| `frontend/runtime_desktop_shell_smoke.png` | `runtime_state/repo-hygiene/2026-05-03/frontend/runtime_desktop_shell_smoke.png` | 115.74 KiB | 2026-04-11 16:09:03 |
| `frontend/runtime_login_smoke.png` | `runtime_state/repo-hygiene/2026-05-03/frontend/runtime_login_smoke.png` | 542.45 KiB | 2026-04-11 16:00:56 |
| `frontend/runtime_mobile_shell_smoke.png` | `runtime_state/repo-hygiene/2026-05-03/frontend/runtime_mobile_shell_smoke.png` | 41.40 KiB | 2026-04-11 16:04:42 |
| `frontend/vite_runtime.err.log` | `runtime_state/repo-hygiene/2026-05-03/frontend/vite_runtime.err.log` | 8.34 KiB | 2026-04-11 22:12:41 |
| `frontend/vite_runtime.log` | `runtime_state/repo-hygiene/2026-05-03/frontend/vite_runtime.log` | 3.71 KiB | 2026-04-11 21:48:59 |
| `frontend/vite_t17.err.log` | `runtime_state/repo-hygiene/2026-05-03/frontend/vite_t17.err.log` | 2.70 KiB | 2026-04-11 16:08:37 |
| `frontend/vite_t17.log` | `runtime_state/repo-hygiene/2026-05-03/frontend/vite_t17.log` | 220 B | 2026-04-11 15:56:00 |
| `hs_err_pid20060.log` | `runtime_state/repo-hygiene/2026-05-03/root/hs_err_pid20060.log` | 66.77 KiB | 2026-04-11 22:05:43 |
| `hs_err_pid22212.log` | `runtime_state/repo-hygiene/2026-05-03/root/hs_err_pid22212.log` | 74.84 KiB | 2026-04-11 22:05:57 |
| `hs_err_pid24408.log` | `runtime_state/repo-hygiene/2026-05-03/root/hs_err_pid24408.log` | 74.59 KiB | 2026-04-11 22:06:05 |
| `hs_err_pid28604.log` | `runtime_state/repo-hygiene/2026-05-03/root/hs_err_pid28604.log` | 74.48 KiB | 2026-04-11 22:06:01 |
| `hs_err_pid34056.log` | `runtime_state/repo-hygiene/2026-05-03/root/hs_err_pid34056.log` | 73.57 KiB | 2026-04-11 22:06:12 |
| `replay_pid22212.log` | `runtime_state/repo-hygiene/2026-05-03/root/replay_pid22212.log` | 1.00 MiB | 2026-04-11 22:05:57 |
| `replay_pid24408.log` | `runtime_state/repo-hygiene/2026-05-03/root/replay_pid24408.log` | 549.47 KiB | 2026-04-11 22:06:05 |
| `replay_pid28604.log` | `runtime_state/repo-hygiene/2026-05-03/root/replay_pid28604.log` | 720.12 KiB | 2026-04-11 22:06:01 |
| `replay_pid34056.log` | `runtime_state/repo-hygiene/2026-05-03/root/replay_pid34056.log` | 658.24 KiB | 2026-04-11 22:06:12 |
| `WizTree_20260414202511.csv` | `runtime_state/repo-hygiene/2026-05-03/root/WizTree_20260414202511.csv` | 215.30 MiB | 2026-04-14 20:25:41 |

## 忽略规则

根目录 `.gitignore` 已补充以下类别：

- `runtime_state/` 本地运行状态目录
- JVM crash/replay 诊断：`hs_err_pid*.log`、`replay_pid*.log`、`backend/hs_err_pid*.log`
- 大型磁盘扫描导出：`WizTree_*.csv`
- 运行日志：`backend/backend_runtime*.log`、`frontend/vite_runtime*.log`、`frontend/vite_t*.log`
- 前端烟测截图与构建/依赖输出：`frontend/runtime_*.png`、`frontend/dist/`、`frontend/node_modules/`

`frontend/.gitignore` 同步补充 `runtime_*.png`、`vite_runtime*.log`、`vite_t*.log`，避免前端目录独立使用时重新混入运行产物。

## 恢复方式

如需查看或恢复某个诊断文件，从归档路径复制回原路径即可；不要将归档目录中的大文件提交到源码仓库。

## 验收检查

- 根目录不再保留 `hs_err_pid*.log`、`replay_pid*.log`、`WizTree_*.csv`。
- `frontend/` 不再保留 `runtime_*.png`、`vite_runtime*.log`、`vite_t*.log`。
- `backend/` 不再保留 `backend_runtime*.log`、`hs_err_pid*.log`。
- 新产物会被 `.gitignore` 默认忽略。
