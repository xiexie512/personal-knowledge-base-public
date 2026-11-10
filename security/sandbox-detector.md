# 沙箱存储池与恶意检测

> Cordis 插件原型：10GB 存储池 + Seatbelt 沙箱 + 恶意行为识别。

## 核心设计

- **10GB 统一存储池**：所有沙箱会话共享配额，单会话上限 3GB
- **会话生命周期**：创建 → 使用 → 闲置 5 分钟自动销毁回收
- **插件自身入沙**：插件宿主进程也放入 seatbelt 沙箱托管（`isPluginHost` 标记防递归套娃）
- **并发限制**：8GB 机器建议 `maxConcurrentSession: 1`

## Seatbelt（macOS 沙箱）

```lisp
(version 1)
(deny default)
(allow file-write* (subpath ".dsh-sandbox-storage"))  ; 只允许写工作区
(deny file-read* (subpath "/Users"))                   ; 禁止读主机文件
(deny network-outbound)                                ; 禁止出站网络
```

要点：
- 进程启动时加载，**运行中不可动态解除**
- Linux 可切换 bwrap；Windows 需 JobObject + ACL

## 恶意行为检测规则

| 检测项 | 规则 |
|---|---|
| 高危命令 | `rm -rf /`、`mkfs`、`dd if=/dev/zero` 等黑名单 |
| 敏感路径 | `/System` `/Users` `/private/etc` 等 |
| 写入爆破 | 单次写入 > 500MB 告警 |
| fork 炸弹 | 3 秒内 spawn ≥ 10 个进程拦截 |
| GUI 动作 | 仅白名单软件、合法坐标、限长文本 |

## 与 Harness 集成

- 通过 `ctx.provide("sandbox", ...)` 接管沙箱服务
- `create()`：普通沙箱会话
- `createFullAccessSession()`：仅人工触发的高权限会话

## 局限提醒

- 静态规则可被动态拼接绕过 → 需要 AI 审查层
- `sandbox-exec` 拿不到辅助功能权限 → GUI 操控走外部受控代理
