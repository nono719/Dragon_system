<template>
  <div class="page-card">
    <div class="page-title">授权共享</div>
    <p class="muted">
      向指定钱包地址授予成果访问权限。<b>MetaMask 弹窗签名</b> 后，授权关系同时写入链上和数据库。
    </p>

    <el-form :model="form" label-width="120px" :rules="rules" ref="formRef">
      <el-form-item label="选择成果" prop="achievementId">
        <el-select v-model="form.achievementId" placeholder="只能选择已上链存证的成果" filterable style="width: 100%">
          <el-option
            v-for="a in registeredList"
            :key="a.achievementId"
            :label="`#${a.achievementId} ${a.name}`"
            :value="a.achievementId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="被授权地址" prop="granteeAddress">
        <el-input v-model="form.granteeAddress" placeholder="0x..." class="mono" />
      </el-form-item>
      <el-form-item label="权限类型" prop="permissionType">
        <el-radio-group v-model="form.permissionType">
          <el-radio-button label="READ">只读浏览</el-radio-button>
          <el-radio-button label="DOWNLOAD">允许下载</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="有效期">
        <el-date-picker
          v-model="form.expireDate"
          type="datetime"
          placeholder="留空则永久有效"
          format="YYYY-MM-DD HH:mm"
          value-format="x"
          style="width: 240px"
        />
        <span class="muted ml-8">链上以 unix 秒级时间戳记录</span>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="submit">
          钱包签名授权
        </el-button>
        <span class="muted ml-8" v-if="submitting">{{ submitStage }}</span>
      </el-form-item>
    </el-form>

    <el-divider>已发出的授权</el-divider>
    <el-table :data="granted" v-loading="loadingGranted" size="small" stripe>
      <el-table-column prop="achievementId" label="成果ID" width="80" />
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
      <el-table-column prop="txHash" label="授权 txHash" class-name="mono" min-width="280">
        <template #default="{ row }">
          <a v-if="row.txHash" :href="`https://sepolia.etherscan.io/tx/${row.txHash}`" target="_blank">
            {{ row.txHash.slice(0, 12) }}…{{ row.txHash.slice(-6) }} ↗
          </a>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="danger" v-if="row.status === 'ACTIVE'" @click="confirmRevoke(row)" :loading="revokingId === row.authorizationId">
            钱包签名撤销
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { achievementApi, authorizationApi, recordApi } from '../api'
import { chainGrantAccess, chainRevokeAccess } from '../utils/contracts'

const route = useRoute()
const formRef = ref(null)

const form = ref({
  achievementId: route.query.id ? Number(route.query.id) : null,
  granteeAddress: '',
  permissionType: 'READ',
  expireDate: null
})
const rules = {
  achievementId: [{ required: true, message: '请选择成果', trigger: 'change' }],
  granteeAddress: [
    { required: true, message: '请输入被授权地址', trigger: 'blur' },
    { pattern: /^0x[a-fA-F0-9]{40}$/, message: '地址格式不正确', trigger: 'blur' }
  ]
}

const allMine = ref([])
const granted = ref([])
const loadingGranted = ref(false)
const submitting = ref(false)
const submitStage = ref('')
const revokingId = ref(null)

const registeredList = computed(() =>
  allMine.value.filter((a) => a.status === 'REGISTERED' || a.status === 'SHARED')
)

async function reload() {
  loadingGranted.value = true
  try {
    allMine.value = await achievementApi.mine()
    granted.value = await authorizationApi.granted()
  } finally { loadingGranted.value = false }
}

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const expireSeconds = form.value.expireDate
      ? Math.floor(Number(form.value.expireDate) / 1000)
      : 0

    // 1. 查链下成果记录拿 chainRecordId
    submitStage.value = '查询链上 recordId...'
    const localRec = await recordApi.byAchievement(form.value.achievementId)
    if (!localRec || !localRec.chainRecordId) {
      ElMessage.error('该成果还未链上存证，无法授权')
      submitting.value = false; submitStage.value = ''
      return
    }

    // 2. 钱包签名调 grantAccess
    submitStage.value = '请在 MetaMask 弹窗中签名授权...'
    const grantee = form.value.granteeAddress.toLowerCase()
    const { txHash, blockNumber } = await chainGrantAccess(
      localRec.chainRecordId,
      grantee,
      form.value.permissionType,
      expireSeconds
    )

    // 3. 回传后端落库
    submitStage.value = '同步链下镜像...'
    await authorizationApi.confirmGrant({
      achievementId: form.value.achievementId,
      granteeAddress: grantee,
      permissionType: form.value.permissionType,
      expireTime: expireSeconds || null,
      txHash,
      blockNumber
    })
    ElMessage.success('授权成功，已奖励 1 ACP 积分')
    form.value.granteeAddress = ''
    form.value.expireDate = null
    reload()
  } catch (e) {
    ElMessage.error('授权失败：' + parseError(e))
  } finally {
    submitting.value = false; submitStage.value = ''
  }
}

async function confirmRevoke(row) {
  try {
    await ElMessageBox.confirm(
      `确认撤销对 ${row.granteeAddress.slice(0, 8)}… 的授权？需要 MetaMask 签名。`,
      '撤销授权',
      { confirmButtonText: '签名撤销', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (e) { return }

  revokingId.value = row.authorizationId
  try {
    const { txHash, blockNumber } = await chainRevokeAccess(row.chainRecordId, row.granteeAddress)
    await authorizationApi.confirmRevoke({
      authorizationId: row.authorizationId,
      txHash,
      blockNumber
    })
    ElMessage.success('已撤销')
    reload()
  } catch (e) {
    ElMessage.error('撤销失败：' + parseError(e))
  } finally { revokingId.value = null }
}

function parseError(e) {
  if (e?.code === 'ACTION_REJECTED') return '用户在 MetaMask 中拒绝了交易'
  if (e?.shortMessage) return e.shortMessage
  return e?.message || '未知错误'
}

onMounted(reload)
</script>
