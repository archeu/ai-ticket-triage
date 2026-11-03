#!/usr/bin/env bash
# 一键启动：读取大模型配置（~/.config/llm.env）和数据库配置（~/.config/demo-db.env），然后启动应用（按 Ctrl+C 停止）
# 用法：./scripts/run.sh    启动后另开一个窗口运行 python3 eval/run_eval.py 跑评测
cd "$(dirname "$0")/.."
set -a; source ~/.config/llm.env; source ~/.config/demo-db.env; set +a
./mvnw -q spring-boot:run -Dspring-boot.run.profiles=dev   # dev：只监听本机、放宽限流、不需要邀请码
