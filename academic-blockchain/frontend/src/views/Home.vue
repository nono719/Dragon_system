<template>
  <div class="home">
    <div class="hero">
      <h1>基于区块链的学术成果确权与共享平台</h1>
      <p class="muted">
        利用区块链不可篡改、可追溯的特性，实现学术成果存证、真实性核验与授权共享的全生命周期管理。
      </p>
      <div class="hero-actions">
        <el-button type="primary" size="large" @click="$router.push('/achievements/new')" v-if="userStore.isLoggedIn">
          <el-icon><Upload /></el-icon>立即存证
        </el-button>
        <el-button type="primary" plain size="large" @click="$router.push('/login')" v-else>
          <el-icon><Wallet /></el-icon>连接钱包开始使用
        </el-button>
        <el-button size="large" @click="$router.push('/verify')">
          <el-icon><Search /></el-icon>立即核验
        </el-button>
      </div>
    </div>

    <el-row :gutter="20" class="features">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="feature-card" shadow="hover">
          <el-icon class="feature-icon" :size="36" color="#409EFF"><Lock /></el-icon>
          <div class="feature-title">成果存证</div>
          <div class="muted">浏览器端 SHA-256 哈希计算，调用智能合约把成果摘要写入区块链</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="feature-card" shadow="hover">
          <el-icon class="feature-icon" :size="36" color="#67C23A"><DocumentChecked /></el-icon>
          <div class="feature-title">真实性核验</div>
          <div class="muted">重新计算待核验文件哈希，与链上记录比对，给出权威结果</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="feature-card" shadow="hover">
          <el-icon class="feature-icon" :size="36" color="#E6A23C"><Share /></el-icon>
          <div class="feature-title">授权共享</div>
          <div class="muted">通过智能合约管理授权关系，支持有效期、撤销及权限校验</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="feature-card" shadow="hover">
          <el-icon class="feature-icon" :size="36" color="#F56C6C"><DataLine /></el-icon>
          <div class="feature-title">链上链下协同</div>
          <div class="muted">摘要上链可信、文件链下高效，兼顾可靠性与性能</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="page-card mt-24" v-if="health">
      <div class="page-title">链上节点状态</div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="服务状态"><el-tag type="success">{{ health.status }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="ChainId">{{ health.chainId }}</el-descriptions-item>
        <el-descriptions-item label="RPC 地址">{{ health.rpcUrl }}</el-descriptions-item>
        <el-descriptions-item label="后端代发地址" class="mono">{{ health.backendAddress }}</el-descriptions-item>
        <el-descriptions-item label="RecordRegistry" class="mono">{{ health.recordRegistry }}</el-descriptions-item>
        <el-descriptions-item label="AccessControlManager" class="mono">{{ health.accessControlManager }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { healthApi } from '../api'
import { useUserStore } from '../store/user'

const userStore = useUserStore()
const health = ref(null)

onMounted(async () => {
  try { health.value = await healthApi.ping() } catch (e) { /* ignore */ }
})
</script>

<style scoped>
.home { display: flex; flex-direction: column; gap: 24px; }
.hero {
  background: linear-gradient(135deg, #1f4cf0 0%, #4d8bff 100%);
  color: #fff;
  border-radius: 12px;
  padding: 60px 40px;
  text-align: center;
}
.hero h1 { margin: 0 0 16px; font-size: 32px; }
.hero p { color: rgba(255, 255, 255, 0.85) !important; max-width: 720px; margin: 0 auto; font-size: 16px; }
.hero-actions { margin-top: 24px; display: flex; justify-content: center; gap: 12px; flex-wrap: wrap; }
.features .el-col { margin-bottom: 16px; }
.feature-card { text-align: center; padding: 16px 0; }
.feature-icon { margin-bottom: 12px; }
.feature-title { font-weight: 600; margin-bottom: 6px; }
</style>
