/**
 * 前端 ethers.js 合约客户端
 *
 * 设计：所有需要 msg.sender = 用户钱包的链上操作（registerRecord / grantAccess / revokeAccess）
 *       都由前端直接发起，让用户的 MetaMask 弹窗签名。这样：
 *         - 用户在 MetaMask "NFTs" 标签可直接看到自己的 Academic Achievement NFT
 *         - 积分（ACP）真实归属于用户钱包，可在 MetaMask "代币" 中导入查看
 *         - 后端无需私钥，只接收 txHash 等待确认后写入链下数据库镜像
 *
 * 注意：合约地址从后端 /api/health 接口读取，所以前端不需要硬编码网络 / chainId。
 */
import { BrowserProvider, Contract, JsonRpcProvider } from 'ethers'
import { healthApi } from '../api'
import { useUserStore } from '../store/user'
import RecordRegistryArtifact from '../abi/RecordRegistry.json'
import AccessControlManagerArtifact from '../abi/AccessControlManager.json'
import AcademicPointArtifact from '../abi/AcademicPoint.json'

let cachedAddrs = null
let cachedChainConfig = null

export async function loadChainConfig(force = false) {
  if (cachedChainConfig && !force) return cachedChainConfig
  const h = await healthApi.ping()
  cachedAddrs = {
    RecordRegistry: h.recordRegistry,
    AcademicPoint: h.academicPoint,
    AccessControlManager: h.accessControlManager
  }
  cachedChainConfig = {
    chainId: Number(h.chainId),
    rpcUrl: h.rpcUrl,
    addresses: cachedAddrs
  }
  return cachedChainConfig
}

function getProvider() {
  if (typeof window === 'undefined' || !window.ethereum) {
    throw new Error('未检测到 MetaMask')
  }
  return new BrowserProvider(window.ethereum)
}

async function getSigner() {
  const provider = getProvider()
  // 确保已请求过账户授权
  await provider.send('eth_requestAccounts', [])
  return await provider.getSigner()
}

/**
 * 校验 MetaMask 当前激活的账户 与 登录态钱包地址 一致。
 * 不一致就抛错 —— 防止"登录 A、签名 B"导致链上 NFT / 授权归属错乱。
 */
export async function ensureWalletMatches() {
  const userStore = useUserStore()
  const loggedWallet = (userStore.wallet || '').toLowerCase()
  if (!loggedWallet) throw new Error('未登录，无法发起链上交易')

  const provider = getProvider()
  const accounts = await provider.send('eth_requestAccounts', [])
  const active = accounts && accounts[0] ? accounts[0].toLowerCase() : ''
  if (!active) throw new Error('MetaMask 未连接账户')
  if (active !== loggedWallet) {
    throw new Error(
      `MetaMask 当前激活的账户 (${shorten(active)}) 与登录账户 (${shorten(loggedWallet)}) 不一致。\n` +
      `请在 MetaMask 中切换到 ${shorten(loggedWallet)} 后重试；\n` +
      `或者退出登录，用 ${shorten(active)} 重新登录。`
    )
  }
}
function shorten(a) { return a ? a.slice(0, 6) + '...' + a.slice(-4) : '' }

/** 校验 MetaMask 当前网络与后端配置的 chainId 一致，不一致则提示用户切换。 */
export async function ensureCorrectNetwork() {
  const cfg = await loadChainConfig()
  const provider = getProvider()
  const network = await provider.getNetwork()
  if (Number(network.chainId) !== cfg.chainId) {
    const hex = '0x' + cfg.chainId.toString(16)
    try {
      // 尝试请求 MetaMask 切换网络
      await provider.send('wallet_switchEthereumChain', [{ chainId: hex }])
    } catch (e) {
      throw new Error(
        `当前 MetaMask 网络 chainId=${network.chainId}，需要切换到 ${cfg.chainId}（${cfg.chainId === 11155111 ? 'Sepolia' : 'Hardhat'}）`
      )
    }
  }
}

export async function getRecordRegistry() {
  const cfg = await loadChainConfig()
  const signer = await getSigner()
  return new Contract(cfg.addresses.RecordRegistry, RecordRegistryArtifact.abi, signer)
}

export async function getAccessControlManager() {
  const cfg = await loadChainConfig()
  const signer = await getSigner()
  return new Contract(cfg.addresses.AccessControlManager, AccessControlManagerArtifact.abi, signer)
}

export async function getAcademicPoint() {
  const cfg = await loadChainConfig()
  const signer = await getSigner()
  return new Contract(cfg.addresses.AcademicPoint, AcademicPointArtifact.abi, signer)
}

/**
 * 链上 registerRecord(fileHash, metadataHash)：
 *   - MetaMask 弹窗用户签名
 *   - 等 tx mined（Sepolia ~12-15s）
 *   - 从交易事件解析出 recordId
 * 返回 { txHash, blockNumber, chainRecordId }
 */
export async function chainRegisterRecord(fileHashHex, metadataHashHex) {
  await ensureWalletMatches()
  await ensureCorrectNetwork()
  const registry = await getRecordRegistry()
  const tx = await registry.registerRecord(fileHashHex, metadataHashHex)
  const receipt = await tx.wait()
  if (receipt.status !== 1) {
    throw new Error('链上交易 revert')
  }
  // 解析 RecordRegistered(recordId, fileHash, metadataHash, owner, recordTime)
  let chainRecordId = null
  for (const log of receipt.logs) {
    try {
      const parsed = registry.interface.parseLog(log)
      if (parsed && parsed.name === 'RecordRegistered') {
        chainRecordId = parsed.args.recordId
        break
      }
    } catch (e) { /* not our event */ }
  }
  if (chainRecordId == null) {
    // 回退：直接查 getRecordByHash
    chainRecordId = await registry.getRecordByHash(fileHashHex)
  }
  return {
    txHash: receipt.hash,
    blockNumber: receipt.blockNumber,
    chainRecordId: chainRecordId.toString()
  }
}

/** 链上 grantAccess(recordId, grantee, permissionType, expireTime) */
export async function chainGrantAccess(chainRecordId, grantee, permissionType, expireTime) {
  await ensureWalletMatches()
  await ensureCorrectNetwork()
  const acm = await getAccessControlManager()
  const tx = await acm.grantAccess(chainRecordId, grantee, permissionType, expireTime)
  const receipt = await tx.wait()
  if (receipt.status !== 1) throw new Error('授权交易 revert')
  return { txHash: receipt.hash, blockNumber: receipt.blockNumber }
}

/**
 * View-only 读取链上 NFT 数据。
 * 优先用 MetaMask 的 BrowserProvider（不暴露 Alchemy API key 到浏览器）；
 * 没装 MetaMask 时 fallback 到 RPC URL。
 * 返回 { tokenId, owner, tokenURI, metadata } —— metadata 是从 base64 data: URI 解码出来的 JSON
 */
export async function fetchNftData(chainRecordId) {
  const cfg = await loadChainConfig()
  let provider
  if (typeof window !== 'undefined' && window.ethereum) {
    provider = new BrowserProvider(window.ethereum)
  } else {
    provider = new JsonRpcProvider(cfg.rpcUrl)
  }
  const registry = new Contract(cfg.addresses.RecordRegistry, [
    'function ownerOf(uint256) view returns (address)',
    'function tokenURI(uint256) view returns (string)',
    'function name() view returns (string)',
    'function symbol() view returns (string)'
  ], provider)

  const [owner, uri, name, symbol] = await Promise.all([
    registry.ownerOf(chainRecordId),
    registry.tokenURI(chainRecordId),
    registry.name(),
    registry.symbol()
  ])

  let metadata = null
  if (uri && uri.startsWith('data:application/json;base64,')) {
    const b64 = uri.slice('data:application/json;base64,'.length)
    try {
      // atob 在浏览器原生可用
      metadata = JSON.parse(atob(b64))
    } catch (e) { /* metadata 解码失败，保持 null */ }
  }

  return {
    tokenId: chainRecordId.toString(),
    contractName: name,
    contractSymbol: symbol,
    owner,
    tokenURI: uri,
    metadata
  }
}

/** 链上 revokeAccess(recordId, grantee) */
export async function chainRevokeAccess(chainRecordId, grantee) {
  await ensureWalletMatches()
  await ensureCorrectNetwork()
  const acm = await getAccessControlManager()
  const tx = await acm.revokeAccess(chainRecordId, grantee)
  const receipt = await tx.wait()
  if (receipt.status !== 1) throw new Error('撤销交易 revert')
  return { txHash: receipt.hash, blockNumber: receipt.blockNumber }
}
