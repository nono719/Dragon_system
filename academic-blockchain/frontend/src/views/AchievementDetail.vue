<template>
  <div v-loading="loading">
    <div class="page-card" v-if="detail">
      <div class="flex-row mb-12">
        <el-button @click="$router.back()" :icon="ArrowLeft">返回</el-button>
        <span class="page-title" style="margin: 0">{{ detail.achievement.name }}</span>
        <el-tag :type="statusTag(detail.achievement.status)">{{ statusLabel(detail.achievement.status) }}</el-tag>
      </div>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="成果编号">{{ detail.achievement.achievementId }}</el-descriptions-item>
        <el-descriptions-item label="成果类型">{{ detail.achievement.category || '未指定' }}</el-descriptions-item>
        <el-descriptions-item label="所有者">
          {{ detail.owner?.username }} <span class="muted mono">({{ shortAddr(detail.owner?.walletAddress) }})</span>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.achievement.createTime }}</el-descriptions-item>
        <el-descriptions-item label="成果摘要" :span="2">{{ detail.achievement.summary || '—' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider>链上存证</el-divider>
      <el-empty v-if="!detail.record" description="尚未上链存证" />
      <el-descriptions v-else :column="1" border>
        <el-descriptions-item label="链上 recordId">{{ detail.record.chainRecordId }}</el-descriptions-item>
        <el-descriptions-item label="文件哈希" class="mono">{{ detail.record.fileHash }}</el-descriptions-item>
        <el-descriptions-item label="元数据哈希" class="mono">{{ detail.record.metadataHash }}</el-descriptions-item>
        <el-descriptions-item label="链上交易哈希" class="mono">{{ detail.record.txHash }}</el-descriptions-item>
        <el-descriptions-item label="所在区块">{{ detail.record.blockNumber }}</el-descriptions-item>
        <el-descriptions-item label="存证时间">{{ detail.record.recordTime }}</el-descriptions-item>
      </el-descriptions>

      <el-divider>成果文件</el-divider>
      <el-table :data="detail.files" v-if="detail.files?.length">
        <el-table-column prop="fileName" label="文件名" />
        <el-table-column prop="fileType" label="类型" width="100" />
        <el-table-column label="大小" width="120">
          <template #default="{ row }">{{ humanSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="fileHash" label="文件哈希" min-width="320" class-name="mono" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="download(row)">下载</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无文件" />

      <el-divider v-if="auths.length">授权关系（{{ auths.length }}）</el-divider>
      <el-table v-if="auths.length" :data="auths" size="small">
        <el-table-column prop="granteeAddress" label="被授权地址" class-name="mono" min-width="280" />
        <el-table-column prop="permissionType" label="权限" width="100" />
        <el-table-column prop="startTime" label="开始时间" width="170" />
        <el-table-column prop="endTime" label="结束时间" width="170">
          <template #default="{ row }">{{ row.endTime || '永久' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="txHash" label="授权交易哈希" class-name="mono" min-width="320" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { achievementApi, fileApi, authorizationApi } from '../api'
import { shortAddr } from '../utils/wallet'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'

const route = useRoute()
const userStore = useUserStore()
const detail = ref(null)
const auths = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const id = route.params.id
    detail.value = await achievementApi.detail(id)
    auths.value = await authorizationApi.byAchievement(id)
  } finally { loading.value = false }
})

function statusLabel(s) { return { CREATED: '未存证', REGISTERED: '已存证', SHARED: '已共享' }[s] || s }
function statusTag(s)   { return { CREATED: 'info', REGISTERED: 'success', SHARED: 'warning' }[s] || 'info' }

function humanSize(b) {
  if (b == null) return '-'
  if (b < 1024) return b + ' B'
  if (b < 1024 * 1024) return (b / 1024).toFixed(1) + ' KB'
  return (b / 1024 / 1024).toFixed(2) + ' MB'
}

async function download(row) {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  // 通过带 token 的 fetch 下载
  const resp = await fetch(fileApi.downloadUrl(row.fileId), {
    headers: { Authorization: `Bearer ${userStore.token}` }
  })
  if (!resp.ok) {
    let msg = '下载失败'
    try { const j = await resp.json(); msg = j.message || msg } catch (e) { /* ignore */ }
    ElMessage.error(msg)
    return
  }
  const blob = await resp.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = row.fileName
  a.click()
  URL.revokeObjectURL(url)
}
</script>
