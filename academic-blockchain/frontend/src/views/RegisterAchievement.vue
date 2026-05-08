<template>
  <div class="page-card">
    <div class="page-title">成果存证</div>
    <p class="muted">填写成果信息 → 上传文件 → 浏览器计算 SHA-256 → 后端调用智能合约写入区块链。</p>

    <el-steps :active="active" finish-status="success" align-center class="mt-12 mb-12">
      <el-step title="填写信息" />
      <el-step title="上传文件" />
      <el-step title="链上存证" />
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

    <el-divider>2. 上传文件并存证</el-divider>

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
        2. 上传并发起链上存证
      </el-button>
      <span class="muted" v-if="uploading">{{ uploadStage }}</span>
    </div>

    <el-result
      v-if="record"
      icon="success"
      title="存证成功"
      sub-title="成果哈希、所有者地址和时间戳已写入区块链，可被永久核验"
      class="mt-24"
    >
      <template #extra>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="链上 recordId">{{ record.chainRecordId }}</el-descriptions-item>
          <el-descriptions-item label="文件哈希" class="mono">{{ record.fileHash }}</el-descriptions-item>
          <el-descriptions-item label="元数据哈希" class="mono">{{ record.metadataHash }}</el-descriptions-item>
          <el-descriptions-item label="链上交易哈希" class="mono">{{ record.txHash }}</el-descriptions-item>
          <el-descriptions-item label="所在区块">{{ record.blockNumber }}</el-descriptions-item>
          <el-descriptions-item label="存证时间">{{ record.recordTime }}</el-descriptions-item>
        </el-descriptions>
        <div class="mt-12">
          <el-button @click="$router.push('/achievements/' + record.achievementId)">查看成果详情</el-button>
          <el-button @click="$router.push('/grant')">前往授权共享</el-button>
        </div>
      </template>
    </el-result>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { sha256File } from '../utils/wallet'
import { achievementApi, fileApi, recordApi } from '../api'

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
  } finally {
    creating.value = false
  }
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
    uploadStage.value = '正在上传文件...'
    const fileMeta = await fileApi.upload(achievement.value.achievementId, selectedFile.value, () => {})
    if (localHash.value && fileMeta.fileHash !== localHash.value) {
      ElMessage.warning('本地哈希与服务端哈希不一致，请检查传输过程')
    }
    uploadStage.value = '正在调用智能合约存证...'
    record.value = await recordApi.register(achievement.value.achievementId, fileMeta.fileId)
    ElMessage.success('存证成功')
  } catch (e) {
    ElMessage.error('存证失败：' + (e.message || ''))
  } finally {
    uploading.value = false
    uploadStage.value = ''
  }
}
</script>

<style scoped>
.ml-12 { margin-left: 12px; }
</style>
