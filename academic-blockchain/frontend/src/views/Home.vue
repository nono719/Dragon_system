<template>
  <div class="home">
    <!-- Hero -->
    <section class="hero">
      <div class="hero-bg">
        <div class="hero-blob hero-blob-1"></div>
        <div class="hero-blob hero-blob-2"></div>
      </div>
      <div class="hero-inner">
        <span class="hero-badge fade-up">
          <span class="dot"></span>
          Sepolia · 已部署运行中
        </span>
        <h1 class="hero-title fade-up">
          基于<span class="grad-text">区块链</span>的<br>
          学术成果<span class="grad-text">确权与共享</span>平台
        </h1>
        <p class="hero-desc fade-up">
          利用区块链不可篡改、可追溯的特性，实现学术成果<b>存证</b>、<b>真实性核验</b>与<b>授权共享</b>的全生命周期管理。
          NFT 凭证 + ERC20 积分激励，构建可信学术生态。
        </p>
        <div class="hero-actions fade-up">
          <el-button type="primary" size="large" @click="$router.push('/achievements/new')" v-if="userStore.isLoggedIn">
            <el-icon><Upload /></el-icon>立即存证
          </el-button>
          <el-button type="primary" size="large" @click="$router.push('/login')" v-else>
            <el-icon><Wallet /></el-icon>连接钱包开始使用
          </el-button>
          <el-button size="large" plain class="hero-btn-secondary" @click="$router.push('/verify')">
            <el-icon><Search /></el-icon>核验任意文件
          </el-button>
        </div>
        <div class="hero-stats fade-up" v-if="health">
          <div class="stat">
            <div class="stat-num">{{ health.chainId }}</div>
            <div class="stat-label">Chain ID</div>
          </div>
          <div class="stat">
            <div class="stat-num">3</div>
            <div class="stat-label">智能合约</div>
          </div>
          <div class="stat">
            <div class="stat-num">5</div>
            <div class="stat-label">功能模块</div>
          </div>
          <div class="stat">
            <div class="stat-num">ERC721 + ERC20</div>
            <div class="stat-label">凭证 / 激励</div>
          </div>
        </div>
      </div>
    </section>

    <!-- Features -->
    <section class="features">
      <h2 class="section-title">五大核心能力</h2>
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="6" v-for="(f, i) in featureList" :key="i">
          <div class="feature-card" :style="{ '--card-accent': f.color }">
            <div class="feature-icon-wrap">
              <component :is="f.icon" />
            </div>
            <div class="feature-title">{{ f.title }}</div>
            <div class="feature-desc">{{ f.desc }}</div>
          </div>
        </el-col>
      </el-row>
    </section>

    <!-- 链上节点状态 -->
    <section class="page-card" v-if="health">
      <div class="page-title">链上节点状态</div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="服务状态">
          <el-tag type="success" effect="dark" size="small">
            <span class="pulse-dot"></span>{{ health.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Chain ID">{{ health.chainId }} <span class="muted">({{ health.chainId === 11155111 ? 'Sepolia' : 'Hardhat' }})</span></el-descriptions-item>
        <el-descriptions-item label="RPC 地址" :span="2"><span class="mono small">{{ health.rpcUrl }}</span></el-descriptions-item>
        <el-descriptions-item label="后端代发地址" class="mono">
          <a :href="`https://sepolia.etherscan.io/address/${health.backendAddress}`" target="_blank">{{ health.backendAddress }} ↗</a>
        </el-descriptions-item>
        <el-descriptions-item label="RecordRegistry (ERC721 NFT)" class="mono">
          <a :href="`https://sepolia.etherscan.io/address/${health.recordRegistry}`" target="_blank">{{ health.recordRegistry }} ↗</a>
        </el-descriptions-item>
        <el-descriptions-item label="AccessControlManager" class="mono">
          <a :href="`https://sepolia.etherscan.io/address/${health.accessControlManager}`" target="_blank">{{ health.accessControlManager }} ↗</a>
        </el-descriptions-item>
        <el-descriptions-item label="AcademicPoint (ACP)" class="mono">
          <a :href="`https://sepolia.etherscan.io/address/${health.academicPoint}`" target="_blank">{{ health.academicPoint }} ↗</a>
        </el-descriptions-item>
      </el-descriptions>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref, markRaw } from 'vue'
import { Lock, DocumentChecked, Share, DataLine, Trophy } from '@element-plus/icons-vue'
import { healthApi } from '../api'
import { useUserStore } from '../store/user'

const userStore = useUserStore()
const health = ref(null)

const featureList = [
  { title: '成果存证',   icon: markRaw(Lock),            color: '#5b6cff', desc: '浏览器端 SHA-256 哈希计算，钱包签名调用合约把成果摘要写入区块链' },
  { title: '真实性核验', icon: markRaw(DocumentChecked), color: '#10b981', desc: '重新计算待核验文件哈希，与链上记录比对，给出权威匹配结果' },
  { title: 'NFT 凭证',   icon: markRaw(Trophy),          color: '#8b5cf6', desc: 'ERC721 NFT 凭证 mint 给成果所有者钱包，MetaMask 中可见' },
  { title: '授权共享',   icon: markRaw(Share),           color: '#ec4899', desc: '细粒度权限（READ/DOWNLOAD），支持有效期、链上撤销、积分激励' }
]

onMounted(async () => {
  try { health.value = await healthApi.ping() } catch (e) { /* ignore */ }
})
</script>

<style scoped>
.home { display: flex; flex-direction: column; gap: 32px; }

/* ============ Hero ============ */
.hero {
  position: relative;
  background: linear-gradient(135deg, #0f172a 0%, #1e1b4b 50%, #4c1d95 100%);
  color: #fff;
  border-radius: var(--radius-lg);
  padding: 72px 48px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(76, 29, 149, 0.25);
}
.hero-bg { position: absolute; inset: 0; overflow: hidden; }
.hero-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(70px);
  opacity: 0.5;
}
.hero-blob-1 {
  width: 460px; height: 460px;
  background: #5b6cff;
  top: -120px; left: -100px;
}
.hero-blob-2 {
  width: 380px; height: 380px;
  background: #ec4899;
  bottom: -100px; right: -80px;
}
.hero-inner {
  position: relative;
  max-width: 880px;
  margin: 0 auto;
  text-align: center;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  background: rgba(255,255,255,0.08);
  border: 1px solid rgba(255,255,255,0.16);
  border-radius: 999px;
  font-size: 13px;
  color: rgba(255,255,255,0.9);
  backdrop-filter: blur(10px);
}
.hero-badge .dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: #4ade80;
  box-shadow: 0 0 0 4px rgba(74, 222, 128, 0.25);
  animation: pulse-ring 2s ease-in-out infinite;
}

.hero-title {
  font-size: 48px;
  font-weight: 800;
  margin: 24px 0 20px;
  line-height: 1.15;
  letter-spacing: -0.02em;
}
.grad-text {
  background: linear-gradient(90deg, #ffd04b, #ec4899, #5b6cff);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.hero-desc {
  font-size: 17px;
  color: rgba(255,255,255,0.75) !important;
  line-height: 1.7;
  margin: 0 auto 32px;
  max-width: 720px;
}
.hero-desc b { color: rgba(255,255,255,0.95); }

.hero-actions {
  display: flex;
  justify-content: center;
  gap: 14px;
  flex-wrap: wrap;
}
.hero-btn-secondary {
  background: rgba(255,255,255,0.08) !important;
  color: #fff !important;
  border: 1px solid rgba(255,255,255,0.18) !important;
  backdrop-filter: blur(10px);
}
.hero-btn-secondary:hover {
  background: rgba(255,255,255,0.18) !important;
  border-color: rgba(255,255,255,0.32) !important;
}

.hero-stats {
  margin-top: 48px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  padding-top: 32px;
  border-top: 1px solid rgba(255,255,255,0.1);
}
.stat-num {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  font-family: 'SFMono-Regular', Menlo, Consolas, monospace;
}
.stat-label {
  font-size: 12px;
  color: rgba(255,255,255,0.55);
  margin-top: 4px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
@media (max-width: 640px) {
  .hero { padding: 48px 24px; }
  .hero-title { font-size: 32px; }
  .hero-stats { grid-template-columns: repeat(2, 1fr); gap: 16px; }
}

/* ============ Features ============ */
.section-title {
  margin: 0 0 18px;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.01em;
}
.features .el-col { margin-bottom: 16px; }
.feature-card {
  background: var(--c-card);
  border-radius: var(--radius-md);
  padding: 28px 24px;
  border: 1px solid var(--c-border-soft);
  height: 100%;
  box-sizing: border-box;
  transition: transform .25s ease, box-shadow .25s ease, border-color .25s ease;
  position: relative;
  overflow: hidden;
}
.feature-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 3px;
  background: var(--card-accent);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform .3s ease;
}
.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
  border-color: transparent;
}
.feature-card:hover::before { transform: scaleX(1); }
.feature-icon-wrap {
  width: 56px; height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: color-mix(in srgb, var(--card-accent) 12%, transparent);
  color: var(--card-accent);
  margin-bottom: 18px;
  font-size: 28px;
  transition: transform .3s ease;
}
.feature-card:hover .feature-icon-wrap { transform: scale(1.08) rotate(-4deg); }
.feature-icon-wrap :deep(svg) { width: 28px; height: 28px; }
.feature-title {
  font-weight: 600;
  font-size: 17px;
  margin-bottom: 8px;
  letter-spacing: -0.01em;
}
.feature-desc {
  color: var(--c-text-soft);
  font-size: 13px;
  line-height: 1.7;
}

/* ============ 链上节点状态 ============ */
.pulse-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #fff;
  margin-right: 6px;
  vertical-align: middle;
  animation: pulse-ring 2s ease-in-out infinite;
}
.small { font-size: 13px; }
</style>
