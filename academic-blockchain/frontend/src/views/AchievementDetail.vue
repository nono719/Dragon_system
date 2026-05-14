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
      <div v-else>
        <div class="nft-badge">
          <div class="nft-badge-left">
            <el-icon :size="40"><Trophy /></el-icon>
            <div>
              <div class="nft-badge-title">{{ nft?.contractName || 'Academic Achievement NFT' }} #{{ detail.record.chainRecordId }}</div>
              <div class="nft-badge-desc">
                ERC721 凭证 · symbol: {{ nft?.contractSymbol || 'AAN' }}
                <span v-if="nft?.owner"> · 当前持有者：<span class="mono">{{ shortAddr(nft.owner) }}</span></span>
              </div>
            </div>
          </div>
          <div class="nft-badge-actions">
            <el-button :loading="loadingNft" size="small" plain @click="reloadNft">
              <el-icon><Refresh /></el-icon>刷新链上数据
            </el-button>
          </div>
        </div>
        <el-descriptions :column="1" border class="mt-12">
          <el-descriptions-item label="NFT tokenId / recordId">{{ detail.record.chainRecordId }}</el-descriptions-item>
          <el-descriptions-item label="文件哈希" class="mono">{{ detail.record.fileHash }}</el-descriptions-item>
          <el-descriptions-item label="元数据哈希" class="mono">{{ detail.record.metadataHash }}</el-descriptions-item>
          <el-descriptions-item label="链上交易哈希" class="mono">
            <a :href="etherscanTx(detail.record.txHash)" target="_blank">{{ detail.record.txHash }} ↗</a>
          </el-descriptions-item>
          <el-descriptions-item label="所在区块">{{ detail.record.blockNumber }}</el-descriptions-item>
          <el-descriptions-item label="存证时间">{{ detail.record.recordTime }}</el-descriptions-item>
        </el-descriptions>

        <el-divider>NFT 链上数据（直接从合约读取）</el-divider>
        <el-skeleton v-if="loadingNft && !nft" :rows="3" animated />
        <el-alert v-else-if="nftError" type="warning" :title="'读取链上 NFT 失败：' + nftError" :closable="false" show-icon />
        <div v-else-if="nft">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="合约名称">{{ nft.contractName }} ({{ nft.contractSymbol }})</el-descriptions-item>
            <el-descriptions-item label="当前持有者 (ownerOf)" class="mono">
              <a :href="etherscanAddr(nft.owner)" target="_blank">{{ nft.owner }} ↗</a>
            </el-descriptions-item>
            <template v-if="nft.metadata">
              <el-descriptions-item label="NFT 名称">{{ nft.metadata.name }}</el-descriptions-item>
              <el-descriptions-item label="描述">{{ nft.metadata.description }}</el-descriptions-item>
              <el-descriptions-item label="链上属性 (attributes)">
                <el-table :data="nft.metadata.attributes || []" size="small" stripe>
                  <el-table-column prop="trait_type" label="trait_type" width="140" />
                  <el-table-column prop="value" label="value" class-name="mono" />
                </el-table>
              </el-descriptions-item>
            </template>
            <el-descriptions-item label="原始 tokenURI">
              <details>
                <summary class="muted" style="cursor: pointer">点击展开（{{ nft.tokenURI.length }} 字符）</summary>
                <pre class="uri-block">{{ nft.tokenURI }}</pre>
              </details>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>

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
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, Refresh } from '@element-plus/icons-vue'
import { achievementApi, fileApi, authorizationApi } from '../api'
import { shortAddr } from '../utils/wallet'
import { fetchNftData } from '../utils/contracts'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'

const route = useRoute()
const userStore = useUserStore()
const detail = ref(null)
const auths = ref([])
const loading = ref(false)
const nft = ref(null)
const loadingNft = ref(false)
const nftError = ref('')

async function reloadNft() {
  if (!detail.value?.record?.chainRecordId) return
  loadingNft.value = true
  nftError.value = ''
  try {
    nft.value = await fetchNftData(detail.value.record.chainRecordId)
  } catch (e) {
    nftError.value = e.shortMessage || e.message || String(e)
  } finally { loadingNft.value = false }
}

onMounted(async () => {
  loading.value = true
  try {
    const id = route.params.id
    detail.value = await achievementApi.detail(id)
    auths.value = await authorizationApi.byAchievement(id)
    if (detail.value?.record?.chainRecordId) await reloadNft()
  } finally { loading.value = false }
})

function etherscanTx(h) { return `https://sepolia.etherscan.io/tx/${h}` }
function etherscanAddr(a) { return `https://sepolia.etherscan.io/address/${a}` }

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

<style scoped>
.nft-badge {
  background: linear-gradient(135deg, #7e57c2 0%, #5e35b1 100%);
  border-radius: 12px;
  color: #fff;
  padding: 20px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.nft-badge-left { display: flex; gap: 16px; align-items: center; }
.nft-badge-title { font-size: 18px; font-weight: 600; }
.nft-badge-desc { opacity: 0.85; font-size: 13px; margin-top: 4px; }
.nft-badge-actions :deep(.el-button) {
  background: rgba(255,255,255,0.15);
  color: #fff;
  border-color: rgba(255,255,255,0.3);
}
.uri-block {
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 8px 12px;
  margin-top: 8px;
  font-family: 'SFMono-Regular', Menlo, Consolas, monospace;
  font-size: 12px;
  word-break: break-all;
  white-space: pre-wrap;
  max-height: 200px;
  overflow: auto;
}
</style>
