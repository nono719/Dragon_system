#!/usr/bin/env bash
# 开发模式一键启动脚本（macOS / Linux）
# 在 4 个终端面板里分别跑：hardhat node、deploy、backend、frontend
set -e

cd "$(dirname "$0")"
ROOT=$(pwd)

echo "========================================"
echo "一键启动学术成果区块链平台 (开发模式)"
echo "项目根目录：$ROOT"
echo "========================================"
echo ""
echo "提示：这是开发引导脚本，请按提示在不同终端窗口执行命令。"
echo ""
cat <<'EOF'
[终端 1] 启动本地链：
    cd contracts && npm install && npx hardhat node

[终端 2] 部署合约（保留终端 1 运行）：
    cd contracts && npx hardhat run scripts/deploy.js --network localhost

[终端 3] 启动后端（确保 MySQL 已建库 + 导入 db/schema.sql）：
    cd backend && mvn -DskipTests spring-boot:run

[终端 4] 启动前端：
    cd frontend && npm install && npm run dev

打开 http://localhost:5173 → 连接 MetaMask（chainId 31337）→ 开始体验
EOF
