# 云 GPU 微调大模型

> 场景：Mac mini 8GB 无法全参数微调，用云 GPU 跑 LoRA，训练完合并回本地用。

## 为什么推荐云 GPU

| 方案 | 适合吗 | 优点 | 缺点 |
|---|---|---|---|
| 云 GPU（RTX4090） | ✅ 强烈推荐 | 开箱镜像、CUDA 原生、几小时几块钱 | 需要 SSH 操作 |
| Mac 本地 Metal LoRA | 勉强 | 免费 | 8GB 内存压力大、容易 swap |
| 外接 eGPU | ❌ 不推荐 | — | M 芯片无原生 CUDA 支持 |

## 选型（Qwen2.5-3B LoRA）

- **RTX 4090 24G**：约 1.6~2 元/小时，余量足，最适合新手
- QLoRA 4bit 甚至 4GB 显存就能跑
- 几百条数据训练 30~60 分钟，总成本几块钱

## 国内平台

1. **AutoDL**：学生友好，预制 LLaMA-Factory 镜像，按量计费
2. **智星云**：价格便宜，适合小模型炼丹
3. **极智算**：闲时折扣

## LoRA 训练流程

```bash
# 环境
conda create -n lora python=3.11
git clone https://github.com/hiyouga/LLaMA-Factory.git
cd LLaMA-Factory && pip install -e .[mac]

# 训练配置（train_review_lora.yaml）
# model_name_or_path: Qwen/Qwen2.5-3B-Instruct
# finetuning_type: lora
# lora_rank: 8
# max_steps: 300
# learning_rate: 2e-4

llamafactory-cli train train_review_lora.yaml

# 合并 LoRA → 完整 HF 模型
llamafactory-cli export --adapter_name_or_path ./review-lora-ckpt ...

# 转 GGUF 给 llama-server
# convert-hf-to-gguf.py + 量化 Q4_K_M
```

## 数据集要点

- 单轮对话 SFT 格式（system/user/assistant）
- SAFE / WARN / BLOCK 三类样本均衡，建议 200~500 条
- 每条 assistant 输出必须带【SAFE】等标签，训练后格式才稳定
- **训练用 prompt 必须与线上推理 prompt 完全一致**

## 省钱避坑

1. 训练完**立即关机**，关机才停止计费
2. 数据盘选持久化存储，防止关机数据清空
3. 别选 A100/H100，3B 任务浪费
4. 优先 QLoRA 4bit 训练，降低显存压力

## 关键概念区分

- **Full 全参数微调**：3B 也要 >16GB 显存
- **LoRA**：只训练小适配器，显存友好，适合你的场景
- **GGUF 不能直接微调**：微调需要 HF 格式，训练完再转 GGUF
