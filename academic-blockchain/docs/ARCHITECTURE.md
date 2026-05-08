# 系统架构与数据流

## 整体分层（论文 §3.1）

```
┌────────────────────────── 客户端层 ──────────────────────────┐
│ Vue 3 + Element Plus + ethers.js + MetaMask                  │
│ Home / Verify / RegisterAchievement / GrantAccess /          │
│ MyAchievements / AchievementDetail / AuthorizedToMe / Admin  │
└────────────────────────────┬─────────────────────────────────┘
                             │ HTTP /api/*  (JWT)
┌────────────────────────────▼─────────────────────────────────┐
│                      控制层 (Spring MVC)                      │
│ AuthInterceptor (JWT) │ GlobalExceptionHandler │ Cors        │
└────────────────────────────┬─────────────────────────────────┘
                             │
┌────────────────────────────▼─────────────────────────────────┐
│                          业务层                               │
│ UserService │ AchievementService │ FileService │             │
│ RecordService │ VerifyService │ AuthorizationService │ Admin │
└─────────┬──────────────────────────────────────┬─────────────┘
          │ MyBatis                              │ Web3j
┌─────────▼──────────────┐         ┌─────────────▼─────────────┐
│  数据存储层 (MySQL)     │         │  智能合约 (Solidity ^0.8) │
│  6 张业务表             │         │  RecordRegistry           │
│  + uploads/ 链下文件    │         │  AccessControlManager     │
└────────────────────────┘         └───────────────────────────┘
```

## 关键数据流

### 1. 成果上链存证

```
用户提交文件
   │
   ▼
浏览器 SHA-256（utils/wallet.js#sha256File）
   │
   ▼
POST /files/upload          (后端再算一次哈希落库 + 存盘)
   │
   ▼
POST /records/register      (后端调用 RecordRegistry.registerRecord)
   │                            └─ Web3j RawTransactionManager 代发
   │                            └─ 等待回执 → 反查 chainRecordId
   ▼
INSERT INTO achievement_record  (链下镜像)
UPDATE achievement.status = 'REGISTERED'
   │
   ▼
返回 (chainRecordId, txHash, blockNumber, recordTime)
```

### 2. 成果核验

```
用户提交文件 (无需登录)
   │
   ▼
后端 SHA-256
   │
   ▼
RecordRegistry.getRecordByHash(fileHash) → recordId
   │
   ▼ (recordId > 0)
RecordRegistry.getRecordInfo(recordId) → (fileHash, metaHash, owner, time)
   │
   ▼
INSERT INTO verify_log
   │
   ▼
返回核验结果（链上权威）
```

### 3. 授权共享

```
所有者 (currentUserId, currentWallet)
   │
   ├── 校验：成果存在 && 自己拥有 && 已上链 && 链上 owner 一致
   │
   ▼
AccessControlManager.grantAccess(recordId, grantee, permission, expireTime)
   │
   ▼
INSERT INTO authorization_record
UPDATE achievement.status = 'SHARED'
```

```
被授权方下载文件
   │
   ▼
GET /files/{fileId}/download (带 JWT)
   │
   ▼
FileService.loadAsResource:
   if 不是 owner:
     SELECT authorization_record where ACTIVE and not expired
     若无 → 403
   │
   ▼
返回文件流
```

### 4. 撤销授权

```
所有者发起 revoke
   │
   ▼
AccessControlManager.revokeAccess(recordId, grantee)
   │
   ▼
UPDATE authorization_record SET status='REVOKED', revoke_tx_hash=...
```

## 数据库表（论文 §3.4 → schema.sql）

| 表名 | 主键 | 关键字段 |
| --- | --- | --- |
| user | user_id | wallet_address (唯一) / role |
| achievement | achievement_id | user_id / status |
| achievement_file | file_id | achievement_id / file_hash / file_path |
| achievement_record | record_id | chain_record_id / file_hash (唯一) / tx_hash |
| authorization_record | authorization_id | achievement_id / grantor_address / grantee_address / status |
| verify_log | verify_id | verify_hash / verify_result |

## 安全设计

1. **链上身份分离** —— 用户钱包私钥保存在 MetaMask；登录通过 `eth_requestAccounts` 获取地址，不传输私钥。
2. **后端代发交易** —— 简化 MVP；生产可改为前端用 ethers.js 直接发送交易再回传 tx 给后端。
3. **JWT 鉴权** —— Authorization: Bearer header；登录时根据钱包地址签发。
4. **权限三层校验** ——
   - 数据库 owner 校验：避免越权改/删他人成果
   - 链上 owner 校验：`AccessControlManager.onlyRecordOwner`
   - 文件下载校验：链下 `authorization_record` 中 ACTIVE + 未过期
5. **哈希双写一致** —— 浏览器和后端各算一次 SHA-256，落库时校验，发现不一致时给用户警告。

## 性能（论文 §5.3 测试结果对照）

| 操作 | 论文实测 | 本实现预期 |
| --- | --- | --- |
| 成果列表查询 | 0.42s | < 0.5s |
| 成果详情 | 0.31s | < 0.5s |
| 10MB 文件哈希 | 0.78s | < 1s（流式 SHA-256） |
| 链上存证 | ~2.4s | 1-3s（Hardhat 本地链区块时间） |
| 核验返回 | ~1.1s | < 1s（仅一次链上 view 调用） |

## 模块映射（论文 §3.3 → 代码）

| 论文模块 | 后端 Service | 后端 Controller | 前端 View |
| --- | --- | --- | --- |
| DS_YHGL01 用户管理 | UserService | UserController | Login.vue / Profile.vue |
| DS_CCZZ01 成果存证 | RecordService + FileService | RecordController + FileController | RegisterAchievement.vue |
| DS_CCHY01 成果核验 | VerifyService | VerifyController | Verify.vue |
| DS_GXQX01 授权共享 | AuthorizationService | AuthorizationController | GrantAccess.vue / AuthorizedToMe.vue |
| DS_WDCG01 成果管理 | AchievementService | AchievementController | MyAchievements.vue / AchievementDetail.vue |
