<template>
  <router-view />
</template>

<script setup>
import { onMounted } from 'vue'
import { bindAccountChange, getCurrentAccount } from './utils/wallet'
import { useUserStore } from './store/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()

onMounted(async () => {
  bindAccountChange((acc) => {
    if (userStore.isLoggedIn && acc && acc.toLowerCase() !== userStore.wallet) {
      ElMessage.warning('钱包账户已切换，请重新登录')
      userStore.logout()
    }
  })
  // 如果本地有 token 但浏览器钱包账户已断开，做一次轻校验
  if (userStore.isLoggedIn) {
    try {
      const acc = await getCurrentAccount()
      if (acc && acc.toLowerCase() !== userStore.wallet) {
        userStore.logout()
      }
    } catch (e) { /* ignore */ }
  }
})
</script>
