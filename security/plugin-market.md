# 插件市场安全预检

> 下载插件时先扫描，而不是等跑起来再拦。

## 流程

```
浏览插件市场 → 点击安装 → 下载源码 → 静态安全扫描
  → SAFE：直接安装
  → WARN：强制沙箱隔离 + 禁止自动 full-access
  → BLOCK：拒绝安装，源码删除
```

## 扫描规则

### 源码特征（正则匹配）

| 特征 | 级别 |
|---|---|
| `child_process` / `spawn` / `exec` | WARN |
| `fs.writeFile` / `fs.rm` | WARN |
| 网络请求（fetch/axios/net） | WARN |
| `danger-full-access` | BLOCK |
| `rm -rf /` | BLOCK |
| `osascript` / AppleScript | BLOCK |
| 读取密钥环境变量 | WARN |
| 目录穿越 `../` | WARN |

### 依赖黑名单

- `node-pty`、`rootkit-js` 等危险包直接 BLOCK

## 安全分级

| 等级 | 处理 |
|---|---|
| SAFE | 正常安装，默认沙箱运行 |
| WARN | **强制**塞进配额沙箱，禁止自动申请完全权限 |
| BLOCK | 下载扫描拒绝，不落入运行时 |

## 分级算法（参考）

- 每条 BLOCK 特征 +80 分，WARN 特征 +20 分
- score ≥ 80 → BLOCK；≥ 20 → WARN；否则 SAFE

## 重要局限

> **静态扫描能绕过**：加密代码、动态拼接字符串、远程加载的恶意代码扫不出来。
> 所以就算标 SAFE，运行时仍保留行为检测 + DLP 持续监控。市场预检是第一道关卡，不是替代沙箱。

## 进阶方向

- 插件签名校验（只有私钥签名的插件才能安装）
- 插件权限清单可视化（安装前展示会拥有哪些能力）
- 声明能力 vs 实际行为对比（声明读文件却调用 osascript → 阻断）
