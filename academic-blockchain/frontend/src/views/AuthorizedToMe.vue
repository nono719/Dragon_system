<template>
  <div class="page-card">
    <div class="page-title">我被授权的成果</div>
    <p class="muted">下方列表显示你的钱包地址 <span class="mono">{{ userStore.wallet }}</span> 被授权访问的所有成果。</p>

    <div class="flex-row mb-12">
      <el-switch v-model="onlyActive" active-text="只看有效授权" inline-prompt @change="reload" />
      <el-button @click="reload" :icon="Refresh">刷新</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="achievementId" label="成果ID" width="80" />
      <el-table-column prop="grantorAddress" label="授权方" class-name="mono" min-width="280" />
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
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="$router.push('/achievements/' + row.achievementId)">查看详情</el-button>
          <el-button link type="primary" :loading="checking[row.authorizationId]" @click="checkAndDownload(row)">检查并下载</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { achievementApi, authorizationApi, fileApi } from '../api'
import { useUserStore } from '../store/user'

const userStore = useUserStore()
const list = ref([])
const onlyActive = ref(false)
const loading = ref(false)
const checking = reactive({})

async function reload() {
  loading.value = true
  try { list.value = await authorizationApi.received(onlyActive.value) }
  finally { loading.value = false }
}

async function checkAndDownload(row) {
  checking[row.authorizationId] = true
  try {
    const ok = await authorizationApi.check(row.achievementId, userStore.wallet)
    if (!ok.hasAccess) {
      ElMessage.error('链上权限校验未通过')
      return
    }
    // 拉取该成果文件，找到第一个进行下载
    const detail = await achievementApi.detail(row.achievementId)
    if (!detail.files || detail.files.length === 0) {
      ElMessage.warning('该成果没有可下载的文件')
      return
    }
    const f = detail.files[0]
    const resp = await fetch(fileApi.downloadUrl(f.fileId), {
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
    a.download = f.fileName
    a.click()
    URL.revokeObjectURL(url)
  } finally { checking[row.authorizationId] = false }
}

onMounted(reload)
</script>
