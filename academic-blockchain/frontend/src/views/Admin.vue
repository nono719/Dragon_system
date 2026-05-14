<template>
  <div>
    <div class="page-card">
      <div class="page-title">后台管理 · 总览</div>
      <el-row :gutter="16">
        <el-col :span="6"><StatCard title="用户数" :value="stats.userCount" color="#5b6cff" /></el-col>
        <el-col :span="6"><StatCard title="成果数" :value="stats.achievementCount" color="#10b981" /></el-col>
        <el-col :span="6"><StatCard title="链上存证" :value="stats.registeredCount" color="#f59e0b" /></el-col>
        <el-col :span="6"><StatCard title="活跃授权" :value="stats.activeAuthorizationCount" color="#ec4899" /></el-col>
      </el-row>
      <el-row :gutter="16" class="mt-12">
        <el-col :span="6"><StatCard title="授权总数" :value="stats.authorizationCount" color="#94a3b8" /></el-col>
        <el-col :span="6"><StatCard title="核验总数" :value="stats.verifyCount" color="#5b6cff" /></el-col>
        <el-col :span="6"><StatCard title="核验匹配" :value="stats.matchedVerifyCount" color="#10b981" /></el-col>
        <el-col :span="6"><StatCard title="核验未匹配" :value="stats.verifyCount - stats.matchedVerifyCount" color="#f56c6c" /></el-col>
      </el-row>
    </div>

    <el-card class="mt-24">
      <el-tabs v-model="tab">
        <!-- 用户列表 + 角色变更 -->
        <el-tab-pane label="用户列表" name="users">
          <div class="flex-row mb-12">
            <el-button @click="loadUsers" :icon="Refresh">刷新</el-button>
            <span class="muted">提示：管理员可点击操作列变更他人角色（USER ↔ ADMIN）</span>
          </div>
          <el-table :data="users" v-loading="loadingUsers" stripe>
            <el-table-column prop="userId" label="ID" width="80" />
            <el-table-column prop="username" label="昵称" />
            <el-table-column prop="walletAddress" label="钱包地址" class-name="mono" min-width="280" />
            <el-table-column prop="email" label="邮箱" />
            <el-table-column prop="role" label="角色" width="100">
              <template #default="{ row }">
                <el-tag :type="row.role === 'ADMIN' ? 'danger' : ''" effect="plain">
                  <el-icon style="vertical-align: middle"><Avatar v-if="row.role === 'ADMIN'" /><User v-else /></el-icon>
                  {{ row.role }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="170" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-popconfirm
                  v-if="row.role === 'USER'"
                  :title="`将 ${row.username} 晋升为 ADMIN？`"
                  @confirm="changeRole(row, 'ADMIN')"
                >
                  <template #reference>
                    <el-button link type="warning">
                      <el-icon><Top /></el-icon>晋升管理员
                    </el-button>
                  </template>
                </el-popconfirm>
                <el-popconfirm
                  v-else
                  :title="`将 ${row.username} 降级为 USER？`"
                  @confirm="changeRole(row, 'USER')"
                >
                  <template #reference>
                    <el-button link type="info" :disabled="row.userId === myUserId">
                      <el-icon><Bottom /></el-icon>降为普通用户
                    </el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 成果列表 -->
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

        <!-- 授权记录 -->
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

        <!-- 核验日志 -->
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

        <!-- 操作审计日志（新增） -->
        <el-tab-pane label="操作审计" name="opLogs">
          <div class="flex-row mb-12">
            <el-button @click="loadOpLogs" :icon="Refresh" :loading="loadingOpLogs">刷新</el-button>
            <span class="muted">展示最近 {{ opLogLimit }} 条写操作（POST / PUT / DELETE），由 AOP 自动记录</span>
          </div>
          <el-table :data="opLogs" v-loading="loadingOpLogs" stripe size="small">
            <el-table-column prop="logId" label="ID" width="70" />
            <el-table-column prop="createTime" label="时间" width="170" />
            <el-table-column label="HTTP" width="120">
              <template #default="{ row }">
                <el-tag size="small" :type="methodTag(row.method)" effect="plain">{{ row.method }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="path" label="路径" min-width="220" />
            <el-table-column prop="operation" label="Controller.method" min-width="200" />
            <el-table-column label="操作者" min-width="200">
              <template #default="{ row }">
                <div>{{ row.userId ? `#${row.userId}` : '匿名' }}
                  <el-tag v-if="row.role" size="small" :type="row.role === 'ADMIN' ? 'danger' : ''" effect="plain" style="margin-left: 4px">{{ row.role }}</el-tag>
                </div>
                <div class="muted mono" style="font-size: 12px" v-if="row.walletAddress">{{ shortAddr(row.walletAddress) }}</div>
              </template>
            </el-table-column>
            <el-table-column label="结果" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 'SUCCESS' ? 'success' : 'danger'">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="durationMs" label="耗时(ms)" width="100" />
            <el-table-column prop="requestIp" label="来源 IP" width="130" />
            <el-table-column label="参数 / 错误" min-width="240">
              <template #default="{ row }">
                <details v-if="row.params || row.errorMessage" style="cursor: pointer">
                  <summary class="muted">展开</summary>
                  <pre v-if="row.params" class="log-detail">{{ row.params }}</pre>
                  <pre v-if="row.errorMessage" class="log-detail error">{{ row.errorMessage }}</pre>
                </details>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { Refresh, Top, Bottom, Avatar, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '../api'
import { useUserStore } from '../store/user'
import { shortAddr } from '../utils/wallet'
import StatCard from '../components/StatCard.vue'

const userStore = useUserStore()
const myUserId = userStore.user?.userId

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
const opLogs = ref([]); const loadingOpLogs = ref(false); const opLogLimit = ref(200)

async function loadStats() { stats.value = await adminApi.stats() }
async function loadUsers() { loadingUsers.value = true; try { users.value = await adminApi.users() } finally { loadingUsers.value = false } }
async function loadAchievements() { loadingAchievements.value = true; try { achievements.value = await adminApi.achievements(kw.value || null) } finally { loadingAchievements.value = false } }
async function loadAuthorizations() { loadingAuthorizations.value = true; try { authorizations.value = await adminApi.authorizations() } finally { loadingAuthorizations.value = false } }
async function loadVerify() { loadingVerify.value = true; try { verifyLogs.value = await adminApi.verifyLogs() } finally { loadingVerify.value = false } }
async function loadOpLogs() {
  loadingOpLogs.value = true
  try { opLogs.value = await adminApi.operationLogs(opLogLimit.value) }
  finally { loadingOpLogs.value = false }
}

async function changeRole(row, newRole) {
  try {
    const updated = await adminApi.changeRole(row.userId, newRole)
    Object.assign(row, updated)
    ElMessage.success(`${row.username} 角色已更新为 ${newRole}`)
    loadStats()
  } catch (e) { /* toasted by http interceptor */ }
}

function statusTag(s) { return { CREATED: 'info', REGISTERED: 'success', SHARED: 'warning' }[s] || 'info' }
function methodTag(m) { return { POST: 'success', PUT: 'warning', DELETE: 'danger' }[m] || '' }

watch(tab, (v) => {
  if (v === 'achievements' && achievements.value.length === 0) loadAchievements()
  else if (v === 'authorizations' && authorizations.value.length === 0) loadAuthorizations()
  else if (v === 'verify' && verifyLogs.value.length === 0) loadVerify()
  else if (v === 'opLogs' && opLogs.value.length === 0) loadOpLogs()
})

onMounted(async () => {
  await loadStats()
  await loadUsers()
})
</script>

<style scoped>
.log-detail {
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 8px 12px;
  margin: 6px 0 0;
  font-family: 'SFMono-Regular', Menlo, Consolas, monospace;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 200px;
  overflow: auto;
}
.log-detail.error { color: #f56c6c; border-color: #fbc4c4; background: #fef0f0; }
</style>
