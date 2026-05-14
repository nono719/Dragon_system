<template>
  <div class="login-page">
    <!-- 装饰性背景 -->
    <div class="login-bg">
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
      <div class="blob blob-3"></div>
      <svg class="grid-pattern" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <pattern id="grid" width="40" height="40" patternUnits="userSpaceOnUse">
            <path d="M 40 0 L 0 0 0 40" fill="none" stroke="rgba(255,255,255,0.06)" stroke-width="1"/>
          </pattern>
        </defs>
        <rect width="100%" height="100%" fill="url(#grid)" />
      </svg>
    </div>

    <div class="login-content fade-up">
      <!-- 左：品牌/特性 -->
      <div class="brand-panel">
        <div class="brand-logo">
          <el-icon :size="40"><Connection /></el-icon>
        </div>
        <h1 class="brand-title">学术成果<br />区块链平台</h1>
        <p class="brand-subtitle">
          基于以太坊智能合约的成果确权、核验与共享系统
        </p>
        <div class="features">
          <div class="feature">
            <div class="f-icon" style="background: rgba(91,108,255,0.18)"><el-icon><Lock /></el-icon></div>
            <div>
              <div class="f-title">链上存证</div>
              <div class="f-desc">SHA-256 哈希 + 智能合约不可篡改记录</div>
            </div>
          </div>
          <div class="feature">
            <div class="f-icon" style="background: rgba(236,72,153,0.18)"><el-icon><Trophy /></el-icon></div>
            <div>
              <div class="f-title">NFT 凭证</div>
              <div class="f-desc">ERC721 凭证可直接在 MetaMask 钱包查看</div>
            </div>
          </div>
          <div class="feature">
            <div class="f-icon" style="background: rgba(139,92,246,0.18)"><el-icon><Share /></el-icon></div>
            <div>
              <div class="f-title">授权共享 & 激励</div>
              <div class="f-desc">细粒度权限 + ACP 积分自动奖励</div>
            </div>
          </div>
        </div>
        <div class="brand-footer">
          成都信息工程大学 · 区块链工程专业 · 毕业设计
        </div>
      </div>

      <!-- 右：登录卡片 -->
      <div class="login-card">
        <div class="login-card-inner">
          <div class="card-header">
            <div class="card-icon">
              <el-icon :size="32"><Wallet /></el-icon>
              <span class="pulse-ring"></span>
            </div>
            <h2>钱包登录</h2>
            <p class="muted">使用 MetaMask 钱包地址作为身份标识，无需密码</p>
          </div>

          <el-alert
            v-if="!hasMetamask"
            type="warning"
            title="未检测到 MetaMask"
            :closable="false"
            show-icon
          >
            <template #default>
              <p style="margin: 0 0 6px">请在 <b>Chrome / Edge</b> 中：</p>
              <ol style="margin: 0 0 0 18px; padding: 0">
                <li>安装 <a href="https://metamask.io/download" target="_blank">MetaMask 扩展</a></li>
                <li>切换到 <b>Sepolia</b> 测试网（chainId 11155111），或本地 <b>Hardhat</b>（chainId 31337）</li>
                <li>刷新本页面（F5）</li>
              </ol>
              <p class="muted" style="margin: 6px 0 0; font-size: 12px">
                当前浏览器：{{ uaShort }}
              </p>
            </template>
          </el-alert>

          <div class="connect-section" v-else>
            <el-form :model="form" label-position="top">
              <el-form-item label="昵称（首次登录可选）">
                <el-input v-model="form.username" placeholder="留空则自动生成，例如 user_a1b2c3" size="large" />
              </el-form-item>
            </el-form>
            <el-button type="primary" size="large" :loading="loading" @click="connect" class="connect-btn">
              <el-icon><Connection /></el-icon>
              连接钱包并登录
            </el-button>
            <div class="network-hint">
              <el-icon><Promotion /></el-icon>
              <span>支持网络：Sepolia 测试网 · Hardhat 本地链</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { connectWallet } from '../utils/wallet'
import { userApi } from '../api'
import { useUserStore } from '../store/user'

const router = useRouter()
const userStore = useUserStore()

const form = ref({ username: '' })
const loading = ref(false)
const hasMetamask = ref(true)
const uaShort = ref('')

onMounted(() => {
  hasMetamask.value = typeof window !== 'undefined' && !!window.ethereum
  uaShort.value = typeof navigator !== 'undefined' ? navigator.userAgent.slice(0, 100) : ''
})

async function connect() {
  loading.value = true
  try {
    const wallet = await connectWallet()
    const data = await userApi.login({ walletAddress: wallet, username: form.value.username || null })
    userStore.setAuth(data.token, data.user)
    ElMessage.success(`欢迎，${data.user.username}`)
    router.push('/')
  } catch (e) {
    ElMessage.error(e.message || '连接失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ============ 整页 ============ */
.login-page {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f172a 0%, #1e1b4b 50%, #4c1d95 100%);
  overflow: hidden;
  padding: 24px;
}
.login-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}
.grid-pattern {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}
.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.55;
}
.blob-1 {
  width: 560px; height: 560px;
  background: #5b6cff;
  top: -150px; left: -150px;
  animation: float-a 14s ease-in-out infinite;
}
.blob-2 {
  width: 520px; height: 520px;
  background: #ec4899;
  bottom: -160px; right: -120px;
  animation: float-b 16s ease-in-out infinite;
}
.blob-3 {
  width: 420px; height: 420px;
  background: #8b5cf6;
  top: 30%; right: -120px;
  animation: float-c 18s ease-in-out infinite;
}
@keyframes float-a { 0%,100% { transform: translate(0,0); } 50% { transform: translate(40px, 30px); } }
@keyframes float-b { 0%,100% { transform: translate(0,0); } 50% { transform: translate(-60px, -40px); } }
@keyframes float-c { 0%,100% { transform: translate(0,0); } 50% { transform: translate(30px, -50px); } }

/* ============ 内容布局 ============ */
.login-content {
  position: relative;
  display: grid;
  grid-template-columns: 1fr 460px;
  gap: 56px;
  width: 100%;
  max-width: 1120px;
  z-index: 1;
}
@media (max-width: 960px) {
  .login-content { grid-template-columns: 1fr; max-width: 480px; }
}

/* ============ 左侧品牌区 ============ */
.brand-panel {
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.brand-logo {
  width: 64px; height: 64px;
  background: rgba(255,255,255,0.12);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 28px;
  border: 1px solid rgba(255,255,255,0.18);
}
.brand-title {
  font-size: 44px;
  font-weight: 800;
  margin: 0 0 16px;
  line-height: 1.18;
  letter-spacing: -0.02em;
  background: linear-gradient(90deg, #fff 0%, #cbd5ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.brand-subtitle {
  font-size: 16px;
  color: rgba(255,255,255,0.75);
  line-height: 1.6;
  margin: 0 0 36px;
  max-width: 420px;
}
.features { display: flex; flex-direction: column; gap: 16px; max-width: 460px; }
.feature {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  background: rgba(255,255,255,0.04);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.08);
  padding: 14px 16px;
  border-radius: 14px;
  transition: background .2s ease, border-color .2s ease;
}
.feature:hover { background: rgba(255,255,255,0.08); border-color: rgba(255,255,255,0.18); }
.f-icon {
  width: 40px; height: 40px;
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  color: #fff;
}
.f-title { font-weight: 600; color: #fff; font-size: 15px; }
.f-desc  { font-size: 13px; color: rgba(255,255,255,0.65); margin-top: 2px; }
.brand-footer { margin-top: 48px; color: rgba(255,255,255,0.4); font-size: 13px; }

/* ============ 右侧登录卡 ============ */
.login-card {
  background: rgba(255,255,255,0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  box-shadow: 0 30px 80px rgba(0,0,0,0.35), 0 8px 24px rgba(91,108,255,0.15);
  border: 1px solid rgba(255,255,255,0.5);
  overflow: hidden;
}
.login-card-inner { padding: 40px 36px; }

.card-header { text-align: center; margin-bottom: 24px; }
.card-icon {
  position: relative;
  width: 72px; height: 72px;
  margin: 0 auto 16px;
  border-radius: 20px;
  background: var(--gradient-primary);
  display: flex; align-items: center; justify-content: center;
  color: #fff;
  box-shadow: 0 12px 28px rgba(91,108,255,0.4);
}
.pulse-ring {
  position: absolute;
  inset: -4px;
  border-radius: 22px;
  border: 2px solid rgba(91,108,255,0.5);
  animation: pulse-ring 2.4s ease-in-out infinite;
}
.card-header h2 {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.01em;
}
.card-header .muted { font-size: 13px; }

.connect-section { margin-top: 8px; }
.connect-btn {
  width: 100%;
  height: 50px !important;
  font-size: 16px !important;
  font-weight: 600 !important;
  border-radius: 14px !important;
  margin-top: 6px;
}
.network-hint {
  margin-top: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 12px;
  color: var(--c-text-soft);
  padding: 10px;
  background: var(--c-border-soft);
  border-radius: 10px;
}
</style>
