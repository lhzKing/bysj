# 文件恢复指南

本目录包含所有被整理出来的过程性文件。如需将项目恢复到整理前的状态，请参考本指南。

**整理日期**: 2026-05-30

---

## 完整恢复步骤

### 方法一：使用脚本一键恢复（推荐）

```bash
cd archive

# 将所有文件夹移回根目录
for dir in docs tools 文章 论文 论文-图表 runtime-state test-screenshots; do
  [ -d "$dir" ] && mv "$dir" ../
done

# 将所有文件移回根目录
mv documents/*.md ../ 2>/dev/null
mv temp-files/* ../ 2>/dev/null

cd ..
```

### 方法二：手动逐步恢复

```bash
cd archive

# 移动主要目录
mv docs ../
mv tools ../
mv 文章 ../
mv 论文 ../
mv 论文-图表 ../

# 移动其他目录（如存在）
mv test-screenshots ../测试截图_20260511 2>/dev/null
mv runtime-state ../

# 移动 .md 文件
mv documents/*.md ../

# 移动临时文件
mv temp-files/*.json ../
mv temp-files/*.html ../
mv temp-files/* ../ 2>/dev/null

cd ..
```

---

## 文件映射清单

| 原位置（整理前） | 现位置（archive 中） | 类型 |
|--------|--------|------|
| /docs | /archive/docs | 目录 |
| /tools | /archive/tools | 目录 |
| /文章 | /archive/文章 | 目录 |
| /论文 | /archive/论文 | 目录 |
| /论文-图表 | /archive/论文-图表 | 目录 |
| /测试截图_20260511 | /archive/test-screenshots | 目录 |
| /runtime_state | /archive/runtime-state | 目录 |
| 根目录各 .md 文件 | /archive/documents/ | 21+ 文件 |
| 临时报告和文件 | /archive/temp-files/ | 文件 |

---

## 被永久删除的文件（不可恢复）

以下文件已直接删除，**不在此 archive 中**：

- **.log 文件** - 所有 Java 崩溃日志
  - 根目录的 `*.log`
  - `backend/` 下的 `*.log`
  - `hs_err_pid*.log`
  - `replay_pid*.log`
- **约 19 个崩溃日志文件**

**说明**：这些日志文件是调试文件，整理时被直接删除，不进行恢复。如需日志，请重新运行系统生成。

---

## 保留在根目录的核心文件（未动）

以下文件夹和文件保留在项目根目录，**不在 archive 中**：

- `backend/` - 完整后端源代码
- `frontend/` - 完整前端源代码
- `deploy/` - 部署配置
- `postman/` - API 测试集
- `scripts/` - 脚本工具
- `README.md` - 项目主文档
- `DESIGN.md` - 系统设计
- `api-doc.md` - API 文档
- `CAMERA_SCAN_GUIDE.md` - 扫码指南
- `CLAUDE.md` - Claude 配置
- `.gitignore` - Git 配置
- `.git/` - Git 仓库

---

## 恢复操作的注意事项

1. **完整性检查**：恢复后的文件结构会与整理前完全相同
2. **无法恢复的内容**：删除的 .log 文件无法恢复，这是预期行为
3. **新 AI 会话恢复**：建议在新的 AI 会话中读取本文件，然后执行恢复命令
4. **Git 追踪**：恢复后可以执行 `git add -A && git commit` 以重新追踪这些文件

---

## 恢复后的验证

恢复完成后，请验证以下内容：

- [ ] `/docs` 目录完整恢复（包含 analysis/, design/, plan/, progress/ 等子目录）
- [ ] `/tools` 目录完整恢复
- [ ] `/文章`、`/论文`、`/论文-图表` 目录恢复
- [ ] 根目录显示原有的全部 .md 文件（约 21+ 个）
- [ ] `/archive` 目录仍然存在（可选择保留或删除）
- [ ] `git status` 显示这些文件为新增或修改

---

## 快速恢复命令（复制粘贴）

```bash
# 一键恢复（在项目根目录执行）
cd archive && \
for dir in docs tools 文章 论文 论文-图表 runtime-state test-screenshots; do [ -d "$dir" ] && mv "$dir" ..; done && \
mv documents/*.md ../ 2>/dev/null && \
mv temp-files/* ../ 2>/dev/null && \
cd ..
```

---

**有任何问题或需要帮助，请告知！**
