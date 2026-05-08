<template>
  <div>
    <div class="page-card">
      <div class="page-title">后台管理 · 总览</div>
      <el-row :gutter="16">
        <el-col :span="6"><StatCard title="用户数" :value="stats.userCount" color="#1f4cf0" /></el-col>
        <el-col :span="6"><StatCard title="成果数" :value="stats.achievementCount" color="#67C23A" /></el-col>
        <el-col :span="6"><StatCard title="链上存证" :value="stats.registeredCount" color="#E6A23C" /></el-col>
        <el-col :span="6"><StatCard title="活跃授权" :value="stats.activeAuthorizationCount" color="#F56C6C" /></el-col>
      </el-row>
      <el-row :gutter="16" class="mt-12">
        <el-col :span="6"><StatCard title="授权总数" :value="stats.authorizationCount" color="#909399" /></el-col>
        <el-col :span="6"><StatCard title="核验总数" :value="stats.verifyCount" color="#1f4cf0" /></el-col>
        <el-col :span="6"><StatCard title="核验匹配" :value="stats.matchedVerifyCount" color="#67C23A" /></el-col>
        <el-col :span="6"><StatCard title="核验未匹配" :value="stats.verifyCount - stats.matchedVerifyCount" color="#F56C6C" /></el-col>
      </el-row>
    </div>

    <el-card class="mt-24">
      <el-tabs v-model="tab">
        <el-tab-pane label="用户列表" name="users">
          <el-table :data="users" v-loading="loadingUsers" stripe>
            <el-table-column prop="userId" label="ID" width="80" />
            <el-table-column prop="username" label="昵称" />
            <el-table-column prop="walletAddress" label="钱包地址" class-name="mono" min-width="280" />
            <el-table-column prop="email" label="邮箱" />
            <el-table-column prop="role" label="角色" width="100">
              <template #default="{ row }">
                <el-tag :type="row.role === 'ADMIN' ? 'danger' : ''">{{ row.role }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="170" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="成果列表" name="achievements">
          <div class="flex-row mb-12">
            <el-input v-model="kw" placeholder="按标题/摘要搜索" clearable style="width: 300px" @keyup.enter="loadAchievements" />
            <el-button type="primary" @click="loadAchievements">搜索</el-button>
          </div>
          <el-table :data="achievements" v-loading="loadingAchievements" stripe>
            <el-table-column prop="achievementId" label="ID" width="80" />
            <el-table-column prop="name" label="成果标题" />
            <el-table-column prop="category" label="类型" width="100" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="statusTag(row.status)">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="170" />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="$router.push('/achievements/' + row.achievementId)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="授权记录" name="authorizations">
          <el-table :data="authorizations" v-loading="loadingAuthorizations" stripe size="small">
            <el-table-column prop="authorizationId" label="ID" width="80" />
            <el-table-column prop="achievementId" label="成果ID" width="80" />
            <el-table-column prop="grantorAddress" label="授权方" class-name="mono" min-width="240" />
            <el-table-column prop="granteeAddress" label="被授权方" class-name="mono" min-width="240" />
            <el-table-column prop="permissionType" label="权限" width="100" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="170" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="核验日志" name="verify">
          <el-table :data="verifyLogs" v-loading="loadingVerify" stripe size="small">
            <el-table-column prop="verifyId" label="ID" width="80" />
            <el-table-column prop="verifyHash" label="文件哈希" class-name="mono" min-width="320" />
            <el-table-column prop="verifyResult" label="结果" width="120">
              <template #default="{ row }">
                <el-tag :type="row.verifyResult === 'MATCHED' ? 'success' : 'info'">{{ row.verifyResult }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="achievementId" label="成果ID" width="80" />
            <el-table-column prop="operatorAddr" label="操作者" class-name="mono" min-width="240" />
            <el-table-column prop="verifyTime" label="时间" width="170" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { adminApi } from '../api'
import StatCard from '../components/StatCard.vue'

const tab = ref('users')
const stats = ref({
  userCount: 0, achievementCount: 0, registeredCount: 0,
  authorizationCount: 0, activeAuthorizationCount: 0,
  verifyCount: 0, matchedVerifyCount: 0
})

const users = ref([]); const loadingUsers = ref(false)
const achievements = ref([]); const loadingAchievements = ref(false); const kw = ref('')
const authorizations = ref([]); const loadingAuthorizations = ref(false)
const verifyLogs = ref([]); const loadingVerify = ref(false)

async function loadStats() { stats.value = await adminApi.stats() }
async function loadUsers() { loadingUsers.value = true; try { users.value = await adminApi.users() } finally { loadingUsers.value = false } }
async function loadAchievements() { loadingAchievements.value = true; try { achievements.value = await adminApi.achievements(kw.value || null) } finally { loadingAchievements.value = false } }
async function loadAuthorizations() { loadingAuthorizations.value = true; try { authorizations.value = await adminApi.authorizations() } finally { loadingAuthorizations.value = false } }
async function loadVerify() { loadingVerify.value = true; try { verifyLogs.value = await adminApi.verifyLogs() } finally { loadingVerify.value = false } }

function statusTag(s) { return { CREATED: 'info', REGISTERED: 'success', SHARED: 'warning' }[s] || 'info' }

watch(tab, (v) => {
  if (v === 'achievements' && achievements.value.length === 0) loadAchievements()
  else if (v === 'authorizations' && authorizations.value.length === 0) loadAuthorizations()
  else if (v === 'verify' && verifyLogs.value.length === 0) loadVerify()
})

onMounted(async () => {
  await loadStats()
  await loadUsers()
})
</script>
