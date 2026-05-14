<template>
  <div class="login-wrap">
    <el-card class="login-card">
      <div class="login-header">
        <el-icon :size="48" color="#409EFF"><Wallet /></el-icon>
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
        <el-form :model="form">
          <el-form-item label="昵称（首次登录可选）">
            <el-input v-model="form.username" placeholder="留空则自动生成" />
          </el-form-item>
        </el-form>
        <el-button type="primary" size="large" :loading="loading" @click="connect">
          <el-icon><Connection /></el-icon>连接钱包并登录
        </el-button>
        <div class="muted mt-12">
          连接后会请求 MetaMask 授权，并将钱包地址注册为系统账号。
        </div>
      </div>
    </el-card>
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
.login-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 200px);
}
.login-card { width: 460px; max-width: 100%; padding: 16px; }
.login-header { text-align: center; margin-bottom: 24px; }
.login-header h2 { margin: 12px 0 4px; }
.connect-section .el-button { width: 100%; }
</style>
