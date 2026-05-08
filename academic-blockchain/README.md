# 基于区块链的学术成果确权与共享平台

> 成都信息工程大学 · 区块链工程专业 · 毕业设计  
> 作者：向欢　指导教师：黄源源 副教授

本仓库为论文《基于区块链的学术成果确权与共享平台设计与实现》的完整实现，包含智能合约、后端服务、前端 DApp 三大部分。

## 系统架构

```
┌──────────────────────────────────────────────────────────────┐
│                         客户端层 (Vue 3)                      │
│   Home / 成果存证 / 成果核验 / 授权共享 / 我的成果 / 后台管理   │
└─────────────────┬────────────────────────────────────────────┘
                  │ HTTP / ethers.js
┌─────────────────▼────────────────────────────────────────────┐
│                    控制层 (Spring Boot)                       │
│        请求分发 │ 钱包连接控制 │ 鉴权 │ 异常处理              │
└─────────────────┬────────────────────────────────────────────┘
                  │
┌─────────────────▼────────────────────────────────────────────┐
│                       业务层 (Service)                        │
│  AchievementSvc │ RecordSvc │ VerifySvc │ AuthorizationSvc   │
└─────────┬─────────────────────────────────────────┬──────────┘
          │ MyBatis                                 │ Web3j
┌─────────▼──────────────┐              ┌───────────▼──────────┐
│   数据存储层 (MySQL)    │              │  智能合约 (Solidity) │
│  user / achievement /  │              │  RecordRegistry      │
│  achievement_file /    │              │  AccessControlMgr    │
│  achievement_record /  │              │                      │
│  authorization_record /│              │  部署: Hardhat       │
│  verify_log            │              │  网络: 本地 / Sepolia│
└────────────────────────┘              └──────────────────────┘
```

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 前端 | Vue 3 + Vue Router + Pinia + Element Plus + ethers.js v6 + Axios + Vite |
| 后端 | Spring Boot 2.7 (JDK 8) + MyBatis + MySQL 8 + Web3j 4.10 + Lombok |
| 合约 | Solidity ^0.8.20 + Hardhat + Ethers.js |
| 钱包 | MetaMask |

## 功能模块

| 编号 | 模块 | 主要功能 |
| --- | --- | --- |
| DS_YHGL01 | 用户管理 | 钱包连接、身份认证、用户信息管理 |
| DS_CCZZ01 | 成果存证 | 文件上传、SHA-256 哈希、链上注册 |
| DS_CCHY01 | 成果核验 | 文件再哈希、链上比对、结果展示 |
| DS_GXQX01 | 授权共享 | 授权创建、撤销、权限校验 |
| DS_WDCG01 | 成果管理 | 成果 CRUD、列表、详情、状态 |

## 目录结构

```
academic-blockchain/
├── contracts/              # Hardhat 智能合约项目
│   ├── contracts/          #   .sol 源码
│   ├── scripts/            #   部署脚本
│   ├── test/               #   合约单元测试
│   └── hardhat.config.js
├── backend/                # Spring Boot 后端
│   ├── src/main/java/...   #   控制器、服务、实体、Web3j 封装
│   ├── src/main/resources/ #   application.yml、MyBatis Mapper XML
│   └── pom.xml
├── frontend/               # Vue 3 前端
│   ├── src/
│   │   ├── api/            #   axios 接口封装
│   │   ├── views/          #   页面组件
│   │   ├── router/         #   Vue Router
│   │   ├── store/          #   Pinia 全局状态
│   │   └── utils/wallet.js #   ethers.js + MetaMask
│   └── vite.config.js
├── db/
│   └── schema.sql          # 6 张业务表 DDL
├── uploads/                # 链下文件存储目录（运行时）
└── docs/                   # 部署与使用文档
```

## 快速开始

详细步骤参见 [docs/DEPLOY.md](docs/DEPLOY.md)。三步走：

```bash
# 1. 启动本地链 + 部署合约
cd contracts && npm install && npx hardhat node           # 终端 A
cd contracts && npx hardhat run scripts/deploy.js --network localhost  # 终端 B

# 2. 启动后端（先建好数据库 academic_blockchain，导入 db/schema.sql，
#    并把上一步打印的合约地址写入 backend/src/main/resources/application.yml）
cd backend && mvn spring-boot:run

# 3. 启动前端
cd frontend && npm install && npm run dev
```

打开 http://localhost:5173 ，连接 MetaMask（接入本地 Hardhat 网络 chainId 31337）即可开始体验。

## 主流程

1. **成果存证** —— 选择文件 → 浏览器端 SHA-256 → 调用 `RecordRegistry.registerRecord` → 后端落库
2. **成果核验** —— 上传待验证文件 → 重新计算哈希 → 链上 `getRecordByHash` → 显示首次存证时间和所有者
3. **授权共享** —— 输入被授权地址 / 权限类型 / 截止时间 → `AccessControlManager.grantAccess`
4. **被授权访问** —— 钱包地址在"被授权的成果"页面，访问前 `checkAccess` → 通过后下载文件
5. **撤销授权** —— `revokeAccess` → 链上和数据库同步置为 revoked

## 许可证

MIT
