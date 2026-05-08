<template>
  <div class="page-card">
    <div class="page-title">成果核验</div>
    <p class="muted">上传成果文件，系统重新计算 SHA-256 并查询区块链，给出真实性核验结果。无需登录即可使用。</p>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="文件核验" name="file">
        <el-upload
          ref="uploadRef"
          :auto-upload="false"
          :limit="1"
          :on-change="onFileSelect"
          :on-remove="() => { selectedFile = null }"
          drag
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">将待核验文件拖到此处，或<em>点击选择</em></div>
        </el-upload>
        <el-button type="primary" :disabled="!selectedFile" :loading="loading" @click="verifyByFile" class="mt-12">
          开始核验
        </el-button>
      </el-tab-pane>

      <el-tab-pane label="哈希值核验" name="hash">
        <el-form>
          <el-form-item label="文件 SHA-256（0x 前缀）">
            <el-input v-model="hashInput" placeholder="0x...." class="mono" />
          </el-form-item>
        </el-form>
        <el-button type="primary" :disabled="!hashInput" :loading="loading" @click="verifyByHash">
          按哈希查询
        </el-button>
      </el-tab-pane>
    </el-tabs>

    <div v-if="result" class="mt-24">
      <el-result
        :icon="result.matched ? 'success' : 'warning'"
        :title="result.matched ? '核验成功' : '核验未通过'"
        :sub-title="result.message"
      >
        <template #extra>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="提交哈希" class="mono">{{ result.fileHash }}</el-descriptions-item>
            <template v-if="result.matched">
              <el-descriptions-item label="链上 recordId">{{ result.chainRecordId }}</el-descriptions-item>
              <el-descriptions-item label="所有者地址" class="mono">{{ result.ownerAddress }}</el-descriptions-item>
              <el-descriptions-item label="首次存证时间">{{ result.recordTime }}</el-descriptions-item>
              <el-descriptions-item label="成果名称" v-if="result.achievementName">{{ result.achievementName }}</el-descriptions-item>
            </template>
          </el-descriptions>
          <div class="mt-12" v-if="result.matched && result.achievementId">
            <el-button @click="$router.push('/achievements/' + result.achievementId)">查看成果详情</el-button>
          </div>
        </template>
      </el-result>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { verifyApi } from '../api'

const activeTab = ref('file')
const selectedFile = ref(null)
const hashInput = ref('')
const loading = ref(false)
const result = ref(null)

function onFileSelect(uploadFile) {
  selectedFile.value = uploadFile.raw
  result.value = null
}

async function verifyByFile() {
  loading.value = true
  try {
    result.value = await verifyApi.verifyFile(selectedFile.value)
  } catch (e) { /* error already toasted */ }
  finally { loading.value = false }
}

async function verifyByHash() {
  loading.value = true
  try {
    let h = hashInput.value.trim().toLowerCase()
    if (!h.startsWith('0x')) h = '0x' + h
    result.value = await verifyApi.verifyByHash(h)
  } catch (e) { /* error already toasted */ }
  finally { loading.value = false }
}
</script>
