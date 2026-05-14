<template>
  <div v-if="user">
    <el-card class="incentive-card">
      <div class="flex-row" style="justify-content: space-between; align-items: flex-start">
        <div>
          <div class="incentive-title">学术积分（ACP）</div>
          <div class="muted">每授权一次成果共享自动奖励 1 ACP</div>
        </div>
        <el-icon :size="36" color="#ffd04b"><Coin /></el-icon>
      </div>
      <div class="incentive-balance">
        <span class="balance-value">{{ balance ? balance.balance : '0' }}</span>
        <span class="balance-symbol">ACP</span>
      </div>
      <div class="muted mono" v-if="balance">
        合约：{{ shortAddr(tokenAddr) }} · 全网总量 {{ balance.totalSupply }} ACP
      </div>
    </el-card>

    <div class="page-card mt-24">
      <div class="page-title">个人中心</div>
      <el-form :model="form" label-width="100px" style="max-width: 540px">
        <el-form-item label="钱包地址">
          <el-input :value="user.walletAddress" readonly class="mono" />
        </el-form-item>
        <el-form-item label="角色">
          <el-tag :type="user.role === 'ADMIN' ? 'danger' : ''">{{ user.role }}</el-tag>
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="电子邮件">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保存修改</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { userApi, incentiveApi, healthApi } from '../api'
import { useUserStore } from '../store/user'
import { shortAddr } from '../utils/wallet'

const userStore = useUserStore()
const user = ref(null)
const form = ref({ username: '', phone: '', email: '' })
const saving = ref(false)
const balance = ref(null)
const tokenAddr = ref('')

onMounted(async () => {
  user.value = await userApi.me()
  form.value = { username: user.value.username || '', phone: user.value.phone || '', email: user.value.email || '' }
  try {
    balance.value = await incentiveApi.me()
    const h = await healthApi.ping()
    tokenAddr.value = h.academicPoint
  } catch (e) { /* ignore */ }
})

async function save() {
  saving.value = true
  try {
    const updated = await userApi.updateMe(form.value)
    user.value = updated
    userStore.updateUser(updated)
    ElMessage.success('已保存')
  } finally { saving.value = false }
}
</script>

<style scoped>
.incentive-card {
  background: linear-gradient(135deg, #ffb74d 0%, #ff7043 100%);
  color: #fff;
  border: none;
}
.incentive-title { font-size: 18px; font-weight: 600; }
.incentive-balance { margin-top: 12px; }
.balance-value { font-size: 40px; font-weight: 700; }
.balance-symbol { font-size: 18px; margin-left: 8px; opacity: 0.85; }
.incentive-card .muted { color: rgba(255, 255, 255, 0.85) !important; }
</style>
