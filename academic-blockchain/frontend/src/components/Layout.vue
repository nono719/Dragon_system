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
  background: linear-gradient(135deg, #0f172a 0%, #1e1b4b 60%, #4c1d95 100%);
  color: #fff;
  height: 64px !important;
  padding: 0;
  box-shadow: 0 4px 24px rgba(15, 23, 42, 0.12);
  position: sticky;
  top: 0;
  z-index: 100;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.header-inner {
  height: 64px;
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  padding: 0 28px;
  gap: 28px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
  font-size: 17px;
  letter-spacing: -0.01em;
  cursor: pointer;
  white-space: nowrap;
  padding: 6px 12px 6px 10px;
  margin-left: -10px;
  border-radius: 10px;
  transition: background .2s ease;
}
.brand:hover { background: rgba(255, 255, 255, 0.06); }
.brand .el-icon {
  background: var(--gradient-primary);
  border-radius: 8px;
  padding: 6px;
  box-shadow: 0 4px 12px rgba(91, 108, 255, 0.4);
}
.header-menu {
  flex: 1;
  border-bottom: none !important;
}
.header-menu :deep(.el-menu-item) {
  border-bottom: none !important;
  height: 64px !important;
  line-height: 64px !important;
  padding: 0 16px !important;
  font-size: 14px !important;
  position: relative;
  transition: color .2s ease;
}
.header-menu :deep(.el-menu-item:hover) {
  background: transparent !important;
  color: #fff !important;
}
.header-menu :deep(.el-menu-item.is-active) {
  background: transparent !important;
}
.header-menu :deep(.el-menu-item.is-active::after) {
  content: '';
  position: absolute;
  bottom: 14px;
  left: 16px;
  right: 16px;
  height: 2px;
  border-radius: 2px;
  background: linear-gradient(90deg, #ffd04b, #ec4899);
}
.header-right { display: flex; align-items: center; gap: 12px; }
.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 7px 14px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 999px;
  cursor: pointer;
  color: #fff;
  font-size: 13px;
  transition: background .2s ease, border-color .2s ease, transform .12s ease;
}
.user-chip:hover {
  background: rgba(255, 255, 255, 0.18);
  border-color: rgba(255, 255, 255, 0.3);
  transform: translateY(-1px);
}
.user-chip .muted { color: rgba(255, 255, 255, 0.65); }
.app-footer {
  text-align: center;
  background: #fff;
  border-top: 1px solid var(--c-border-soft);
  height: 48px !important;
  line-height: 48px;
}
</style>
