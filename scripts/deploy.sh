#!/usr/bin/env bash
# 部署到本服务器：用 Docker Compose 构建并启动"数据库 + 应用"。
# 线上链路：访客 → Cloudflare → 服务器上的 Caddy → 应用（127.0.0.1:8081）
# 用法：./scripts/deploy.sh      （改了代码后重新运行即可更新）
set -euo pipefail
cd "$(dirname "$0")/.."
set -a
source ~/.config/llm.env           # 大模型配置
source ~/.config/triage-prod.env   # 生产数据库密码
set +a
sudo -E docker compose -p ticket-triage up -d --build
sudo docker compose -p ticket-triage ps
# 等应用真正启动好（接口返回 200）才算部署完成；Docker 会先接住连接，只看端口通不通是不够的
echo "等待应用启动……"
if curl -sf -o /dev/null --retry 90 --retry-delay 1 --retry-all-errors --max-time 5 http://127.0.0.1:8081/api/access; then
  echo "应用已就绪"
else
  echo "应用没有正常启动，查看日志：sudo docker logs ticket-triage-app-1"; exit 1
fi
