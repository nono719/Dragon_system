<template>
  <div class="page-card">
    <div class="page-title">我被授权的成果</div>
    <p class="muted">下方列表显示你的钱包地址 <span class="mono">{{ userStore.wallet }}</span> 被授权访问的所有成果。</p>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="权限说明"
      style="margin-bottom: 12px"
    >
      <ul style="margin: 4px 0 0 18px; padding: 0">
        <li><b>READ</b>（只读浏览）：可查看成果元数据、链上记录、NFT 凭证，<b>不可下载</b>原文件</li>
        <li><b>DOWNLOAD</b>（允许下载）：READ 全部权限 + 下载文件原文</li>
      </ul>
    </el-alert>

    <div class="flex-row mb-12">
      <el-switch v-model="onlyActive" active-text="只看有效授权" inline-prompt @change="reload" />
      <el-button @click="reload" :icon="Refresh">刷新</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="achievementId" label="成果ID" width="80" />
      <el-table-column prop="grantorAddress" label="授权方" class-name="mono" min-width="280" />
      <el-table-column label="权限" width="120">
        <template #default="{ row }">
          <el-tag :type="row.permissionType === 'DOWNLOAD' ? 'success' : 'info'" effect="plain">
            <el-icon style="vertical-align: middle; margin-right: 2px">
              <component :is="row.permissionType === 'DOWNLOAD' ? 'Download' : 'View'" />
            </el-icon>
            {{ row.permissionType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="startTime" label="开始时间" width="170" />
      <el-table-column prop="endTime" label="结束时间" width="170">
        <template #default="{ row }">{{ row.endTime || '永久' }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="$router.push('/achievements/' + row.achievementId)">查看详情</el-button>
          <el-tooltip
            v-if="row.permissionType !== 'DOWNLOAD'"
            content="当前授权权限为 READ（只读），不可下载文件"
            placement="top"
          >
            <span>
              <el-button link disabled type="info">
                <el-icon><Lock /></el-icon>仅可浏览
              </el-button>
            </span>
          </el-tooltip>
          <el-button
            v-else
            link
            type="primary"
            :loading="checking[row.authorizationId]"
            :disabled="row.status !== 'ACTIVE'"
            @click="checkAndDownload(row)"
          >
            <el-icon><Download /></el-icon>检查并下载
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { Refresh, Download, View, Lock } from '@element-plus/icons-vue'
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
  // 前端预先拦截一次，避免到后端被 403
  if (row.permissionType !== 'DOWNLOAD') {
    ElMessage.warning('当前授权仅为 READ 权限，不可下载文件')
    return
  }

  checking[row.authorizationId] = true
  try {
    const ok = await authorizationApi.check(row.achievementId, userStore.wallet)
    if (!ok.hasAccess) {
      ElMessage.error('链上权限校验未通过')
      return
    }
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
