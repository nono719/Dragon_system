import { BrowserProvider, sha256, hexlify } from 'ethers'
import { ElMessage } from 'element-plus'

let provider = null

export function getProvider() {
  if (typeof window === 'undefined' || !window.ethereum) {
    throw new Error('未检测到 MetaMask，请先安装浏览器钱包插件')
  }
  if (!provider) {
    provider = new BrowserProvider(window.ethereum)
  }
  return provider
}

export async function connectWallet() {
  const p = getProvider()
  const accounts = await p.send('eth_requestAccounts', [])
  if (!accounts || accounts.length === 0) {
    throw new Error('未获得任何钱包账户')
  }
  return accounts[0].toLowerCase()
}

export async function getCurrentAccount() {
  if (typeof window === 'undefined' || !window.ethereum) return null
  const p = getProvider()
  const accounts = await p.listAccounts()
  return accounts.length > 0 ? accounts[0].address.toLowerCase() : null
}

export function bindAccountChange(onChange) {
  if (typeof window === 'undefined' || !window.ethereum) return
  window.ethereum.on('accountsChanged', (accounts) => {
    onChange(accounts && accounts.length > 0 ? accounts[0].toLowerCase() : null)
  })
  window.ethereum.on('chainChanged', () => {
    ElMessage.warning('区块链网络已切换，建议刷新页面')
  })
}

/** 浏览器端流式 SHA-256：与后端 HashUtil.sha256 算法对齐。 */
export async function sha256File(file) {
  const buf = await file.arrayBuffer()
  const digest = await crypto.subtle.digest('SHA-256', buf)
  return '0x' + Array.from(new Uint8Array(digest))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('')
}

export function shortAddr(addr) {
  if (!addr) return ''
  if (addr.length < 12) return addr
  return addr.slice(0, 6) + '...' + addr.slice(-4)
}
