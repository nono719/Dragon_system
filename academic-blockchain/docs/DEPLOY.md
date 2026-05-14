# 部署与运行指南

> 适用环境：macOS / Windows 11 / Linux。下文以 macOS / Linux 命令为主。

## 一、环境要求

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 1.8 | 后端 Spring Boot 2.7 兼容 JDK 8+ |
| Maven | 3.6+ | 后端构建 |
| Node.js | 18 LTS | 合约 & 前端 |
| MySQL | 8.x | 关系型数据库 |
| Chrome / Edge | 最新版 | 配合 MetaMask |
| MetaMask | 浏览器扩展 | 钱包登录 / 链上交易 |

## 二、初始化数据库

```bash
# 登录 MySQL 并执行 schema
mysql -uroot -p < db/schema.sql
```

如果 MySQL 用户名/密码不是 `root/root`，记得在 `backend/src/main/resources/application.yml` 中修改 `spring.datasource` 配置。

## 三、启动本地区块链

```bash
cd contracts
npm install
npx hardhat node
```

终端会保留运行；它会列出 20 个测试账号和私钥（默认 chainId 31337，端口 8545）。

## 四、部署智能合约

**新开一个终端**：

```bash
cd contracts
npx hardhat run scripts/deploy.js --network localhost
```

输出示例：
```
RecordRegistry deployed at: 0x5FbDB2315678afecb367f032d93F642f64180aa3
AccessControlManager deployed at: 0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512
```

部署脚本会做三件事：
1. 把合约地址写入 `contracts/deployments/localhost.json`
2. 把 ABI + 地址同步到 `frontend/src/abi/`（前端扩展功能用）
3. 在终端打印地址

> 把上面的 `RecordRegistry` 和 `AccessControlManager` 两个地址填入 `backend/src/main/resources/application.yml` 的 `blockchain.contracts.*` 字段。默认值就是 Hardhat 第一次部署的地址，通常无需修改。

## 五、运行合约单元测试

```bash
cd contracts
npx hardhat test
```

应有 14 个用例全部通过（RecordRegistry 5 个 + AccessControlManager 9 个）。

## 六、启动后端

```bash
cd backend
mvn -DskipTests spring-boot:run
```

启动后访问 http://127.0.0.1:8080/api/health 应返回 JSON：

```json
{
  "code": 0,
  "data": {
    "status": "ok",
    "rpcUrl": "http://127.0.0.1:8545",
    "chainId": 31337,
    "backendAddress": "0xf39fd6e51aad88f6f4ce6ab8827279cfffb92266",
    "recordRegistry": "0x5FbDB2315678afecb367f032d93F642f64180aa3",
    "accessControlManager": "0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512"
  }
}
```

## 七、启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 http://localhost:5173 。

## 八、配置 MetaMask

1. 打开 MetaMask 设置 → 网络 → 添加网络：
   - 网络名：Hardhat Local
   - RPC URL：`http://127.0.0.1:8545`
   - 链 ID：`31337`
   - 货币符号：`ETH`
2. 导入测试账户：从 `npx hardhat node` 输出的账号中复制任意一个私钥导入 MetaMask。
3. 切换到刚刚添加的 Hardhat Local 网络。

## 九、跑通主流程（演示）

1. 浏览器打开前端 → 点击 **连接钱包** → 在 MetaMask 弹窗点确认 → 完成登录。
2. 点击 **成果存证** → 填写标题/类型/摘要 → 上传一个测试文件 → 点 **2. 上传并发起链上存证** → 等待 1-3 秒看到 "存证成功"。
3. 点击 **成果核验**（无需登录） → 上传同一个文件 → 应显示 "核验成功"，并展示链上 recordId、所有者地址、存证时间。
4. 点击 **授权共享** → 选择刚才上链的成果 → 输入另一个 MetaMask 账号地址 → 选择权限类型 → 点击 **发起授权**。
5. 切换 MetaMask 到第二个账户 → 重新登录前端 → 点击 **我被授权的成果** → 看到上一步的授权 → 点击 **检查并下载** 即可下载成果文件。
6. 切回授权方账户 → 在 **授权共享** 列表点 **撤销** → 再次切到第二账户尝试下载，应被拒绝。

## 🌐 Sepolia 已部署合约（直接使用）

如果你想跳过本地部署，直接连到已经部署在 Sepolia 测试网上的合约：

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@11/libexec/openjdk.jdk/Contents/Home \
  MYSQL_PASSWORD='your_db_password' \
  SPRING_PROFILES_ACTIVE=sepolia \
  SEPOLIA_RPC_URL='https://eth-sepolia.g.alchemy.com/v2/<your-key>' \
  BLOCKCHAIN_PRIVATE_KEY='0x<your-test-private-key>' \
  mvn -DskipTests spring-boot:run
```

应用会自动加载 `application-sepolia.yml`，连到下列合约：

- RecordRegistry: [`0xF5618f7e5D8A8113971AF6FDED72D424474A51d3`](https://sepolia.etherscan.io/address/0xF5618f7e5D8A8113971AF6FDED72D424474A51d3)
- AcademicPoint:  [`0xda5473df66dbEFac9540d210C305B7499348e69A`](https://sepolia.etherscan.io/address/0xda5473df66dbEFac9540d210C305B7499348e69A)
- AccessControlManager: [`0x5cB7BF1295244E9003f60546e9578D7976C31f77`](https://sepolia.etherscan.io/address/0x5cB7BF1295244E9003f60546e9578D7976C31f77)

> **注意**：`BLOCKCHAIN_PRIVATE_KEY` 是 *后端代发交易* 的账户私钥，**必须使用专门的测试账户**，并且这个账户需要有 Sepolia 测试 ETH（去 [Google Cloud Faucet](https://cloud.google.com/application/web3/faucet/ethereum/sepolia) 领）。

## 十、自行部署到 Sepolia 测试网（可选）

1. 在 [Alchemy](https://www.alchemy.com/) / [Infura](https://www.infura.io/) 申请 Sepolia 节点 RPC URL。
2. 在 [Sepolia 水龙头](https://sepoliafaucet.com/) 领取测试 ETH。
3. 复制 `contracts/.env.example` 为 `contracts/.env` 并填入：
   ```
   SEPOLIA_RPC_URL=https://eth-sepolia.g.alchemy.com/v2/<your-key>
   PRIVATE_KEY=0x<your-test-private-key>
   ```
4. 部署：
   ```bash
   cd contracts
   npx hardhat run scripts/deploy.js --network sepolia
   ```
5. 把输出的合约地址、对应 RPC URL、chainId（Sepolia 是 11155111）填入 `backend/src/main/resources/application.yml`。
6. 后端 `blockchain.private-key` 改为 Sepolia 测试账户私钥；前端 MetaMask 切到 Sepolia 网络即可。

## 十一、构建产物

```bash
# 后端打包
cd backend && mvn clean package -DskipTests
# 产物：backend/target/academic-blockchain-backend.jar

# 前端构建
cd frontend && npm run build
# 产物：frontend/dist/
```

启动 jar：
```bash
java -jar backend/target/academic-blockchain-backend.jar
```

## 常见问题

**Q: 前端连接钱包后不断弹窗？**  
A: 一般是 MetaMask 网络没切到 Hardhat Local（chainId 31337）。

**Q: 后端启动报 "Connection refused" 或 RPC error？**  
A: 检查 `npx hardhat node` 是否还在运行；后端配置 `blockchain.rpc-url` 是否指向正确端口。

**Q: 上传后存证失败 "Record already exists"？**  
A: 同一个文件哈希只能上链一次。换一个文件试试。

**Q: 授权后被授权方下载提示 403 未授权？**  
A: 确认链上链下两边的钱包地址是同一个（小写），且授权未过期；可在后台管理 → 授权记录中确认状态为 ACTIVE。
