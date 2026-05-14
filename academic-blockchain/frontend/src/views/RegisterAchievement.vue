<template>
  <div class="page-card">
    <div class="page-title">成果存证</div>
    <p class="muted">
      填写成果信息 → 上传文件（哈希落库）→ <b>MetaMask 签名上链</b> → 后端记录链上结果。
      整个上链过程由你的钱包亲自签名，NFT 凭证将归你的钱包账户所有。
    </p>

    <el-steps :active="active" finish-status="success" align-center class="mt-12 mb-12">
      <el-step title="填写信息" />
      <el-step title="上传文件" />
      <el-step title="钱包签名上链" />
      <el-step title="完成" />
    </el-steps>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" size="default">
      <el-form-item label="成果标题" prop="name">
        <el-input v-model="form.name" placeholder="例如：基于深度学习的文本分类研究" />
      </el-form-item>
      <el-form-item label="成果类型" prop="category">
        <el-select v-model="form.category" placeholder="请选择" style="width: 100%">
          <el-option label="论文" value="PAPER" />
          <el-option label="实验数据" value="DATA" />
          <el-option label="技术报告" value="REPORT" />
          <el-option label="软件源码" value="CODE" />
          <el-option label="其他" value="OTHER" />
        </el-select>
      </el-form-item>
      <el-form-item label="成果摘要">
        <el-input v-model="form.summary" type="textarea" :rows="4" placeholder="简要描述成果内容、贡献、关键词等" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :disabled="achievement" @click="createAchievement" :loading="creating">
          1. 创建成果记录
        </el-button>
        <el-tag class="ml-12" v-if="achievement" type="success">
          已创建，achievementId = {{ achievement.achievementId }}
        </el-tag>
      </el-form-item>
    </el-form>

    <el-divider>2. 上传文件并发起链上存证</el-divider>

    <el-upload
      ref="uploadRef"
      :auto-upload="false"
      :limit="1"
      :on-change="onFileSelect"
      :on-remove="() => { selectedFile = null; localHash = '' }"
      drag
      :disabled="!achievement"
    >
      <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
      <div class="el-upload__text">将文件拖到此处，或<em>点击选择</em></div>
      <template #tip>
        <div class="el-upload__tip muted">支持 PDF / Word / Excel / 数据文件等任意类型，最大 100MB</div>
      </template>
    </el-upload>

    <div v-if="localHash" class="mt-12">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="本地 SHA-256">
          <span class="mono">{{ localHash }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </div>

    <div class="mt-12 flex-row">
      <el-button type="primary" :loading="uploading" :disabled="!achievement || !selectedFile" @click="uploadAndRegister">
        2. 上传并发起钱包签名上链
      </el-button>
      <span class="muted" v-if="uploading">{{ uploadStage }}</span>
    </div>

    <el-alert v-if="uploading && uploadStage.includes('签名')" type="info" :closable="false" show-icon class="mt-12">
      <template #default>
        请在 MetaMask 弹窗中确认交易（gas 大约 0.005 ETH）。<br>
        签名后请耐心等待 Sepolia 出块（约 12-15 秒）。
      </template>
    </el-alert>

    <el-result
      v-if="record"
      icon="success"
      title="存证成功"
      sub-title="ERC721 NFT 凭证已铸造到你的钱包；成果哈希、所有者地址、时间戳已写入区块链"
      class="mt-24"
    >
      <template #extra>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="NFT tokenId / 链上 recordId">
            #{{ record.chainRecordId }}
          </el-descriptions-item>
          <el-descriptions-item label="文件哈希" class="mono">{{ record.fileHash }}</el-descriptions-item>
          <el-descriptions-item label="元数据哈希" class="mono">{{ record.metadataHash }}</el-descriptions-item>
          <el-descriptions-item label="链上交易哈希" class="mono">
            <a :href="etherscanTx(record.txHash)" target="_blank">{{ record.txHash }} ↗</a>
          </el-descriptions-item>
          <el-descriptions-item label="所在区块">{{ record.blockNumber }}</el-descriptions-item>
          <el-descriptions-item label="存证时间">{{ record.recordTime }}</el-descriptions-item>
        </el-descriptions>
        <div class="mt-12">
          <el-button @click="$router.push('/achievements/' + record.achievementId)">查看成果详情</el-button>
          <el-button @click="$router.push('/grant')">前往授权共享</el-button>
          <el-button @click="hint">在 MetaMask 查看 NFT 凭证 ↗</el-button>
        </div>
      </template>
    </el-result>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { keccak256, toUtf8Bytes } from 'ethers'
import { sha256File } from '../utils/wallet'
import { achievementApi, fileApi, recordApi } from '../api'
import { chainRegisterRecord, loadChainConfig } from '../utils/contracts'

const formRef = ref(null)
const uploadRef = ref(null)
const form = ref({ name: '', category: 'PAPER', summary: '' })
const rules = {
  name: [{ required: true, message: '请输入成果标题', trigger: 'blur' }]
}

const achievement = ref(null)
const creating = ref(false)
const selectedFile = ref(null)
const localHash = ref('')
const uploading = ref(false)
const uploadStage = ref('')
const record = ref(null)

const active = computed(() => {
  if (record.value) return 4
  if (selectedFile.value && uploading.value) return 3
  if (achievement.value) return 2
  return 1
})

async function createAchievement() {
  await formRef.value.validate()
  creating.value = true
  try {
    achievement.value = await achievementApi.create({ ...form.value })
    ElMessage.success('成果创建成功，请上传文件')
  } finally { creating.value = false }
}

async function onFileSelect(uploadFile) {
  selectedFile.value = uploadFile.raw
  localHash.value = ''
  ElMessage.info('计算文件本地哈希...')
  try {
    localHash.value = await sha256File(uploadFile.raw)
  } catch (e) {
    ElMessage.error('哈希计算失败：' + e.message)
  }
}

async function uploadAndRegister() {
  if (!achievement.value || !selectedFile.value) return
  uploading.value = true
  try {
    // 1. 上传文件 → 后端落库
    uploadStage.value = '正在上传文件...'
    const fileMeta = await fileApi.upload(achievement.value.achievementId, selectedFile.value, () => {})
    if (localHash.value && fileMeta.fileHash !== localHash.value) {
      ElMessage.warning('本地哈希与服务端哈希不一致')
    }

    // 2. 取后端用的 metadataInput 字符串（确保 keccak256 输入一致）
    uploadStage.value = '准备元数据哈希...'
    const meta = await recordApi.metadataInput(achievement.value.achievementId)
    const metadataHash = keccak256(toUtf8Bytes(meta.metadataInput))

    // 3. 钱包签名上链
    uploadStage.value = '请在 MetaMask 弹窗中签名上链...'
    const { txHash, blockNumber, chainRecordId } =
      await chainRegisterRecord(fileMeta.fileHash, metadataHash)

    // 4. 通知后端落库镜像
    uploadStage.value = '同步链下镜像...'
    record.value = await recordApi.confirmRegister({
      achievementId: achievement.value.achievementId,
      fileId: fileMeta.fileId,
      chainRecordId: Number(chainRecordId),
      txHash,
      blockNumber,
      metadataHash
    })
    ElMessage.success('存证成功，NFT 已发送到你的钱包')
  } catch (e) {
    const msg = parseError(e)
    ElMessage.error('存证失败：' + msg)
  } finally {
    uploading.value = false
    uploadStage.value = ''
  }
}

function parseError(e) {
  if (e?.code === 'ACTION_REJECTED') return '用户在 MetaMask 中拒绝了交易'
  if (e?.shortMessage) return e.shortMessage
  return e?.message || '未知错误'
}

function etherscanTx(h) {
  return `https://sepolia.etherscan.io/tx/${h}`
}

async function hint() {
  const cfg = await loadChainConfig()
  ElMessageBox.alert(
    `打开 MetaMask 扩展 → "NFTs" 标签 → 你应该看到一枚名为 "Academic Achievement NFT" 的凭证（合约 ${cfg.addresses.RecordRegistry}）。
若没看到，点 "导入 NFT" 输入合约地址 + tokenId（=链上 recordId）。`,
    'MetaMask 查看 NFT 凭证',
    { confirmButtonText: '知道了' }
  )
}
</script>

<style scoped>
.ml-12 { margin-left: 12px; }
</style>
