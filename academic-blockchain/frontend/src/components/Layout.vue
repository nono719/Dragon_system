<template>
  <div class="app-shell">
    <el-header class="app-header">
      <div class="header-inner">
        <div class="brand" @click="router.push('/')">
          <el-icon><Connection /></el-icon>
          <span>学术成果区块链平台</span>
        </div>
        <el-menu
          :default-active="activeMenu"
          mode="horizontal"
          router
          background-color="transparent"
          text-color="#fff"
          active-text-color="#ffd04b"
          class="header-menu"
        >
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/verify">成果核验</el-menu-item>
          <el-menu-item index="/achievements/new" v-if="userStore.isLoggedIn">成果存证</el-menu-item>
          <el-menu-item index="/achievements/mine" v-if="userStore.isLoggedIn">我的成果</el-menu-item>
          <el-menu-item index="/grant" v-if="userStore.isLoggedIn">授权共享</el-menu-item>
          <el-menu-item index="/authorized" v-if="userStore.isLoggedIn">我被授权的成果</el-menu-item>
          <el-menu-item index="/admin" v-if="userStore.isAdmin">后台管理</el-menu-item>
        </el-menu>
        <div class="header-right">
          <template v-if="userStore.isLoggedIn">
            <el-dropdown>
              <span class="user-chip">
                <el-icon><User /></el-icon>
                <span>{{ userStore.user?.username }}</span>
                <span class="muted mono">({{ shortAddr(userStore.wallet) }})</span>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="router.push('/profile')">个人中心</el-dropdown-item>
                  <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <el-button v-else type="primary" plain @click="router.push('/login')">连接钱包</el-button>
        </div>
      </div>
    </el-header>
    <main class="app-main">
      <router-view v-slot="{ Component }">
        <transition name="el-fade-in" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
    <el-footer class="app-footer">
      <span class="muted">© 成都信息工程大学 · 区块链工程专业 · 毕业设计</span>
    </el-footer>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../store/user'
import { shortAddr } from '../utils/wallet'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

function logout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.app-header {
  background: linear-gradient(135deg, #1f4cf0 0%, #4d8bff 100%);
  color: #fff;
  height: 60px !important;
  padding: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
.header-inner {
  height: 60px;
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  padding: 0 24px;
  gap: 24px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 18px;
  cursor: pointer;
  white-space: nowrap;
}
.header-menu {
  flex: 1;
  border-bottom: none !important;
}
.header-menu :deep(.el-menu-item) {
  border-bottom: none !important;
}
.header-right { display: flex; align-items: center; gap: 12px; }
.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  cursor: pointer;
  color: #fff;
}
.user-chip .muted { color: rgba(255, 255, 255, 0.7); }
.app-footer {
  text-align: center;
  background: #fff;
  border-top: 1px solid #ebeef5;
  height: 48px !important;
  line-height: 48px;
}
</style>
