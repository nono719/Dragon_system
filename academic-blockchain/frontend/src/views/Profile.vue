<template>
  <div v-if="user" class="portfolio">
    <!-- ============ Hero ============ -->
    <div class="hero-card">
      <div class="hero-bg-blob"></div>
      <div class="hero-content">
        <Identicon :seed="user.walletAddress" :size="96" />
        <div class="hero-info">
          <div class="hero-name">
            {{ user.username }}
            <el-tag :type="user.role === 'ADMIN' ? 'danger' : ''" effect="dark" size="small">{{ user.role }}</el-tag>
          </div>
          <div class="hero-wallet mono">
            <el-icon><Wallet /></el-icon>
            <span>{{ user.walletAddress }}</span>
            <el-tooltip content="复制" placement="top">
              <el-button link size="small" @click="copy(user.walletAddress)"><el-icon><CopyDocument /></el-icon></el-button>
            </el-tooltip>
            <a :href="`https://sepolia.etherscan.io/address/${user.walletAddress}`" target="_blank">
              <el-button link size="small"><el-icon><Link /></el-icon>Etherscan</el-button>
            </a>
          </div>
          <div class="hero-meta">
            <span><el-icon><Calendar /></el-icon>加入于 {{ formatDate(user.createTime) }}</span>
            <span class="dot-sep">·</span>
            <span><el-icon><Connection /></el-icon>Sepolia (chainId 11155111)</span>
          </div>
        </div>
        <div class="hero-stats">
          <div class="qs"><div class="qs-num">{{ stats.achievementCount }}</div><div class="qs-label">成果</div></div>
          <div class="qs"><div class="qs-num">{{ stats.nftCount }}</div><div class="qs-label">NFT</div></div>
          <div class="qs"><div class="qs-num">{{ stats.acp }}</div><div class="qs-label">ACP</div></div>
          <div class="qs"><div class="qs-num">{{ stats.grantedCount }}</div><div class="qs-label">授权</div></div>
        </div>
      </div>
    </div>

    <!-- ============ Tabs ============ -->
    <el-card class="mt-24 tabs-card">
      <el-tabs v-model="tab">
        <!-- ============ Tab: 资产 ============ -->
        <el-tab-pane label="资产" name="assets">
          <template #label>
            <el-icon><Coin /></el-icon> 资产
          </template>
          <el-row :gutter="16">
            <el-col :xs="24" :md="8">
              <div class="asset-card asset-eth">
                <div class="asset-row">
                  <el-icon :size="22"><Money /></el-icon>
                  <span class="asset-name">Sepolia ETH</span>
                </div>
                <div class="asset-value">{{ formatNum(ethBalance, 4) }} <span class="asset-unit">ETH</span></div>
                <a :href="`https://sepolia.etherscan.io/address/${user.walletAddress}`" target="_blank" class="asset-link">查看链上交易 ↗</a>
              </div>
            </el-col>
            <el-col :xs="24" :md="8">
              <div class="asset-card asset-acp">
                <div class="asset-row">
                  <el-icon :size="22"><Trophy /></el-icon>
                  <span class="asset-name">Academic Point</span>
                </div>
                <div class="asset-value">{{ stats.acp }} <span class="asset-unit">ACP</span></div>
                <div class="asset-link">全网总量 {{ acpTotal }} · 合约 {{ shortAddr(acpAddr) }}</div>
              </div>
            </el-col>
            <el-col :xs="24" :md="8">
              <div class="asset-card asset-nft">
                <div class="asset-row">
                  <el-icon :size="22"><Picture /></el-icon>
                  <span class="asset-name">学术成果 NFT</span>
                </div>
                <div class="asset-value">{{ stats.nftCount }} <span class="asset-unit">枚 (AAN)</span></div>
                <div class="asset-link" @click="tab = 'nfts'" style="cursor: pointer">查看全部 NFT ↓</div>
              </div>
            </el-col>
          </el-row>

          <div class="page-title mt-24">链上交易历史</div>
          <el-table :data="onChainTxs" v-loading="loadingTxs" stripe size="small">
            <el-table-column label="类型" width="140">
              <template #default="{ row }">
                <el-tag size="small" :type="txTagType(row.kind)" effect="plain">{{ txKindLabel(row.kind) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="详情" min-width="200">
              <template #default="{ row }">{{ row.summary }}</template>
            </el-table-column>
            <el-table-column prop="blockNumber" label="区块" width="120" />
            <el-table-column prop="time" label="时间" width="170" />
            <el-table-column label="Tx Hash" min-width="240" class-name="mono">
              <template #default="{ row }">
                <a :href="`https://sepolia.etherscan.io/tx/${row.txHash}`" target="_blank">
                  {{ row.txHash.slice(0, 12) }}…{{ row.txHash.slice(-6) }} ↗
                </a>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!onChainTxs.length && !loadingTxs" description="还没有链上交易" />
        </el-tab-pane>

        <!-- ============ Tab: 活动时间线 ============ -->
        <el-tab-pane name="activity">
          <template #label>
            <el-icon><Bell /></el-icon> 活动
          </template>
          <el-timeline v-if="activities.length">
            <el-timeline-item
              v-for="a in activities"
              :key="a.id"
              :timestamp="a.time"
              :color="a.color"
              :icon="a.icon"
              size="large"
              placement="top"
            >
              <div class="activity-item">
                <div class="activity-title">{{ a.title }}</div>
                <div class="activity-desc muted">{{ a.desc }}</div>
                <div v-if="a.txHash" class="mono activity-tx">
                  <a :href="`https://sepolia.etherscan.io/tx/${a.txHash}`" target="_blank">
                    {{ a.txHash.slice(0, 16) }}…{{ a.txHash.slice(-6) }} ↗
                  </a>
                </div>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无活动记录" />
        </el-tab-pane>

        <!-- ============ Tab: 我的 NFT 凭证 ============ -->
        <el-tab-pane name="nfts">
          <template #label>
            <el-icon><Trophy /></el-icon> 我的 NFTs ({{ stats.nftCount }})
          </template>
          <el-empty v-if="!nfts.length && !loadingNfts" description="还没有 NFT 凭证。去成果存证页 mint 一枚吧～" />
          <el-skeleton v-if="loadingNfts && !nfts.length" :rows="3" animated />
          <div class="nft-grid">
            <div v-for="n in nfts" :key="n.tokenId" class="nft-card" @click="$router.push('/achievements/' + n.achievementId)">
              <div class="nft-card-bg"></div>
              <div class="nft-card-top">
                <el-icon :size="22"><Trophy /></el-icon>
                <span class="nft-id">#{{ n.tokenId }}</span>
              </div>
              <div class="nft-card-name">{{ n.achievementName }}</div>
              <div class="nft-card-attrs">
                <div v-for="(a, i) in (n.attributes || []).slice(0, 3)" :key="i" class="nft-attr">
                  <span class="nft-attr-key">{{ a.trait_type }}</span>
                  <span class="nft-attr-val mono">{{ shortenAttrValue(a.value) }}</span>
                </div>
              </div>
              <div class="nft-card-foot">
                <span class="muted">AAN · Sepolia</span>
                <span class="nft-link">查看详情 ↗</span>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- ============ Tab: 资料 ============ -->
        <el-tab-pane name="profile">
          <template #label>
            <el-icon><User /></el-icon> 资料
          </template>
          <el-form :model="form" label-width="100px" style="max-width: 520px">
            <el-form-item label="钱包地址">
              <el-input :value="user.walletAddress" readonly class="mono" />
            </el-form-item>
            <el-form-item label="角色">
              <el-tag :type="user.role === 'ADMIN' ? 'danger' : ''">{{ user.role }}</el-tag>
              <span class="muted ml-8">由管理员通过后台分配，不可自行修改</span>
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="form.username" maxlength="64" show-word-limit />
            </el-form-item>
            <el-form-item label="联系电话">
              <el-input v-model="form.phone" />
            </el-form-item>
            <el-form-item label="电子邮件">
              <el-input v-model="form.email" />
            </el-form-item>
            <el-form-item label="加入时间">
              <span>{{ formatDate(user.createTime) }}</span>
            </el-form-item>
            <el-form-item label="最后更新">
              <span>{{ formatDate(user.updateTime) }}</span>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="save">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, markRaw } from 'vue'
import {
  Wallet, CopyDocument, Link, Calendar, Connection,
  Coin, Money, Trophy, Picture, Bell, User,
  Document, Check, Share, Lock, RefreshLeft
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  userApi, incentiveApi, healthApi, achievementApi,
  authorizationApi, verifyApi, recordApi
} from '../api'
import { useUserStore } from '../store/user'
import { shortAddr } from '../utils/wallet'
import { fetchEthBalance, fetchNftData } from '../utils/contracts'
import Identicon from '../components/Identicon.vue'

const userStore = useUserStore()
const user = ref(null)
const form = ref({ username: '', phone: '', email: '' })
const saving = ref(false)
const tab = ref('assets')

const stats = reactive({
  achievementCount: 0,
  nftCount: 0,
  acp: '0',
  grantedCount: 0
})
const acpTotal = ref('0')
const acpAddr = ref('')
const ethBalance = ref('0')

const nfts = ref([])
const loadingNfts = ref(false)
const onChainTxs = ref([])
const loadingTxs = ref(false)
const activities = ref([])

// =========== 加载 ===========
async function loadAll() {
  user.value = await userApi.me()
  form.value = {
    username: user.value.username || '',
    phone: user.value.phone || '',
    email: user.value.email || ''
  }

  // 资产 + 统计
  await Promise.all([
    (async () => {
      const inc = await incentiveApi.me()
      stats.acp = String(inc.balance || 0)
      acpTotal.value = String(inc.totalSupply || 0)
    })(),
    (async () => {
      const h = await healthApi.ping()
      acpAddr.value = h.academicPoint
    })(),
    (async () => {
      try { ethBalance.value = await fetchEthBalance(user.value.walletAddress) }
      catch (e) { ethBalance.value = '—' }
    })(),
    (async () => {
      const page = await achievementApi.mine({ pageNum: 1, pageSize: 100 })
      stats.achievementCount = page.total || 0
      const myList = page.list || []
      const onChain = myList.filter(a => a.status === 'REGISTERED' || a.status === 'SHARED')
      stats.nftCount = onChain.length
      await loadNfts(onChain)
      await buildTxsAndActivities(myList)
    })(),
    (async () => {
      const g = await authorizationApi.granted()
      stats.grantedCount = g.length
    })()
  ])
}

async function loadNfts(onChainAchievements) {
  loadingNfts.value = true
  try {
    const results = []
    for (const a of onChainAchievements) {
      try {
        const rec = await recordApi.byAchievement(a.achievementId)
        if (!rec || !rec.chainRecordId) continue
        const data = await fetchNftData(rec.chainRecordId)
        results.push({
          tokenId: data.tokenId,
          achievementId: a.achievementId,
          achievementName: a.name,
          attributes: data.metadata?.attributes || []
        })
      } catch (e) { /* 单个失败不阻断 */ }
    }
    nfts.value = results.sort((x, y) => Number(y.tokenId) - Number(x.tokenId))
  } finally { loadingNfts.value = false }
}

async function buildTxsAndActivities(myList) {
  loadingTxs.value = true
  try {
    const items = []
    const acts = []

    for (const a of myList) {
      acts.push({
        id: `ach-${a.achievementId}`,
        time: formatDate(a.createTime),
        rawTime: a.createTime,
        color: '#94a3b8',
        icon: markRaw(Document),
        title: `创建成果："${a.name}"`,
        desc: `类型 ${a.category || '未指定'}；状态 ${statusLabel(a.status)}`
      })
    }

    // 成果上链 tx
    for (const a of myList) {
      if (a.status === 'CREATED') continue
      try {
        const r = await recordApi.byAchievement(a.achievementId)
        if (r && r.txHash) {
          items.push({
            kind: 'register',
            summary: `上链存证 "${a.name}" → NFT #${r.chainRecordId}`,
            blockNumber: r.blockNumber,
            time: formatDate(r.recordTime),
            txHash: r.txHash,
            rawTime: r.recordTime
          })
          acts.push({
            id: `reg-${a.achievementId}`,
            time: formatDate(r.recordTime),
            rawTime: r.recordTime,
            color: '#5b6cff',
            icon: markRaw(Trophy),
            title: `mint NFT #${r.chainRecordId}：${a.name}`,
            desc: `链上存证成功；区块 ${r.blockNumber}`,
            txHash: r.txHash
          })
        }
      } catch (e) { /* ignore */ }
    }

    // 我发出的授权
    const granted = await authorizationApi.granted()
    for (const auth of granted) {
      if (auth.txHash) {
        items.push({
          kind: auth.status === 'REVOKED' ? 'revoke' : 'grant',
          summary: `${auth.status === 'REVOKED' ? '撤销' : '授予'} ${auth.permissionType} 给 ${shortAddr(auth.granteeAddress)} (成果 ${auth.achievementId})`,
          blockNumber: null,
          time: formatDate(auth.createTime),
          txHash: auth.status === 'REVOKED' ? (auth.revokeTxHash || auth.txHash) : auth.txHash,
          rawTime: auth.createTime
        })
        acts.push({
          id: `auth-${auth.authorizationId}-${auth.status}`,
          time: formatDate(auth.updateTime || auth.createTime),
          rawTime: auth.updateTime || auth.createTime,
          color: auth.status === 'REVOKED' ? '#f56c6c' : '#10b981',
          icon: markRaw(auth.status === 'REVOKED' ? RefreshLeft : Share),
          title: `${auth.status === 'REVOKED' ? '撤销授权' : '发起授权'}：成果 #${auth.achievementId}`,
          desc: `${auth.permissionType} 权限 → ${shortAddr(auth.granteeAddress)}`,
          txHash: auth.status === 'REVOKED' ? (auth.revokeTxHash || auth.txHash) : auth.txHash
        })
      }
    }

    // 我的核验日志
    try {
      const logs = await verifyApi.logs(true)
      for (const v of logs) {
        acts.push({
          id: `v-${v.verifyId}`,
          time: formatDate(v.verifyTime),
          rawTime: v.verifyTime,
          color: v.verifyResult === 'MATCHED' ? '#10b981' : '#94a3b8',
          icon: markRaw(Check),
          title: `成果核验 · ${v.verifyResult === 'MATCHED' ? '匹配成功' : '未匹配'}`,
          desc: `哈希 ${v.verifyHash.slice(0, 18)}…${v.verifyHash.slice(-6)}`
        })
      }
    } catch (e) { /* ignore */ }

    // 排序
    onChainTxs.value = items.sort((x, y) => new Date(y.rawTime) - new Date(x.rawTime))
    activities.value = acts.sort((x, y) => new Date(y.rawTime) - new Date(x.rawTime))
  } finally { loadingTxs.value = false }
}

async function save() {
  saving.value = true
  try {
    const updated = await userApi.updateMe(form.value)
    user.value = updated
    userStore.updateUser(updated)
    ElMessage.success('已保存')
  } finally { saving.value = false }
}

function copy(text) {
  navigator.clipboard.writeText(text)
  ElMessage.success('已复制到剪贴板')
}

function formatDate(s) {
  if (!s) return '-'
  return s.replace('T', ' ').slice(0, 19)
}
function formatNum(s, digits = 4) {
  const n = Number(s)
  if (isNaN(n)) return s
  return n.toFixed(digits).replace(/\.?0+$/, '')
}
function statusLabel(s) { return { CREATED: '未存证', REGISTERED: '已存证', SHARED: '已共享' }[s] || s }
function txKindLabel(k) { return { register: '上链存证', grant: '授权', revoke: '撤销授权' }[k] || k }
function txTagType(k)   { return { register: '', grant: 'success', revoke: 'danger' }[k] || '' }
function shortenAttrValue(v) {
  const s = String(v)
  return s.length > 22 ? s.slice(0, 10) + '…' + s.slice(-6) : s
}

onMounted(loadAll)
</script>

<style scoped>
.portfolio { display: flex; flex-direction: column; gap: 24px; }

/* ============ Hero ============ */
.hero-card {
  position: relative;
  background: linear-gradient(135deg, #1e1b4b 0%, #4c1d95 60%, #831843 100%);
  color: #fff;
  border-radius: var(--radius-lg);
  padding: 32px 36px;
  overflow: hidden;
  box-shadow: 0 16px 40px rgba(76, 29, 149, 0.25);
}
.hero-bg-blob {
  position: absolute;
  width: 360px; height: 360px;
  background: radial-gradient(circle, rgba(255,255,255,0.15) 0%, transparent 70%);
  top: -120px; right: -80px;
  pointer-events: none;
}
.hero-content {
  position: relative;
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}
.hero-info { flex: 1; min-width: 280px; }
.hero-name {
  font-size: 28px;
  font-weight: 700;
  display: flex; align-items: center; gap: 10px;
}
.hero-wallet {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  padding: 4px 12px;
  background: rgba(255,255,255,0.12);
  border-radius: 999px;
  font-size: 13px;
}
.hero-wallet :deep(.el-button) { color: #fff; }
.hero-wallet a { color: #fff; }
.hero-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  font-size: 13px;
  color: rgba(255,255,255,0.7);
  flex-wrap: wrap;
}
.hero-meta .dot-sep { opacity: 0.5; }
.hero-meta .el-icon { vertical-align: middle; margin-right: 4px; }

.hero-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(80px, 1fr));
  gap: 12px;
  min-width: 280px;
}
.qs {
  background: rgba(255,255,255,0.08);
  border: 1px solid rgba(255,255,255,0.12);
  border-radius: 12px;
  padding: 12px 8px;
  text-align: center;
  backdrop-filter: blur(8px);
}
.qs-num { font-size: 22px; font-weight: 700; }
.qs-label { font-size: 11px; color: rgba(255,255,255,0.7); margin-top: 2px; text-transform: uppercase; letter-spacing: 0.05em; }

/* ============ Tabs ============ */
.tabs-card :deep(.el-tabs__item) {
  font-size: 14px;
  height: 44px;
  line-height: 44px;
}

/* ============ Asset cards ============ */
.asset-card {
  position: relative;
  border-radius: var(--radius-md);
  padding: 20px;
  color: #fff;
  overflow: hidden;
  min-height: 130px;
}
.asset-eth { background: linear-gradient(135deg, #6366f1 0%, #818cf8 100%); }
.asset-acp { background: linear-gradient(135deg, #f59e0b 0%, #f97316 100%); }
.asset-nft { background: linear-gradient(135deg, #8b5cf6 0%, #ec4899 100%); }
.asset-row { display: flex; align-items: center; gap: 8px; }
.asset-name { font-weight: 600; font-size: 14px; opacity: 0.95; }
.asset-value { font-size: 28px; font-weight: 700; margin: 10px 0 4px; }
.asset-unit  { font-size: 14px; font-weight: 500; opacity: 0.8; }
.asset-link  { font-size: 12px; color: rgba(255,255,255,0.85); }
.asset-link:hover { color: #fff; }
a.asset-link { color: rgba(255,255,255,0.85); text-decoration: none; }
a.asset-link:hover { text-decoration: underline; }

/* ============ NFT grid ============ */
.nft-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}
.nft-card {
  position: relative;
  background: linear-gradient(135deg, #5b6cff 0%, #8b5cf6 50%, #ec4899 100%);
  color: #fff;
  border-radius: var(--radius-md);
  padding: 18px 20px 16px;
  cursor: pointer;
  transition: transform .2s ease, box-shadow .2s ease;
  overflow: hidden;
}
.nft-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 40px rgba(91, 108, 255, 0.35);
}
.nft-card-bg {
  position: absolute;
  width: 180px; height: 180px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255,255,255,0.2) 0%, transparent 70%);
  right: -60px; top: -60px;
  pointer-events: none;
}
.nft-card-top { display: flex; align-items: center; gap: 8px; opacity: 0.9; }
.nft-id { font-family: 'SFMono-Regular', Menlo, Consolas, monospace; font-size: 22px; font-weight: 700; }
.nft-card-name { font-size: 16px; font-weight: 600; margin: 8px 0 12px; }
.nft-card-attrs { display: flex; flex-direction: column; gap: 4px; }
.nft-attr { display: flex; justify-content: space-between; font-size: 12px; }
.nft-attr-key { opacity: 0.7; }
.nft-attr-val { opacity: 0.95; }
.nft-card-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid rgba(255,255,255,0.15);
  font-size: 12px;
}
.nft-link { opacity: 0.95; }
.nft-card .muted { color: rgba(255,255,255,0.7); }

/* ============ Activity timeline ============ */
.activity-item { padding: 4px 0; }
.activity-title { font-weight: 500; }
.activity-desc { font-size: 13px; margin-top: 2px; }
.activity-tx { font-size: 12px; margin-top: 4px; }
.activity-tx a { color: var(--c-primary); }

.page-title {
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 600;
}
</style>
