<template>
  <div class="page-card">
    <div class="page-title">授权共享</div>
    <p class="muted">向指定钱包地址授予成果访问权限。授权关系会同时写入区块链和数据库。</p>

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
        <span class="muted ml-8">单位：秒级 unix 时间戳</span>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="submit">发起授权</el-button>
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
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-popconfirm title="确认撤销该授权？" @confirm="revoke(row)" v-if="row.status === 'ACTIVE'">
            <template #reference>
              <el-button link type="danger">撤销</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { achievementApi, authorizationApi } from '../api'

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
    await authorizationApi.grant({
      achievementId: form.value.achievementId,
      granteeAddress: form.value.granteeAddress.toLowerCase(),
      permissionType: form.value.permissionType,
      expireTime: expireSeconds
    })
    ElMessage.success('授权成功')
    form.value.granteeAddress = ''
    form.value.expireDate = null
    reload()
  } catch (e) { /* toasted */ }
  finally { submitting.value = false }
}

async function revoke(row) {
  await authorizationApi.revoke(row.authorizationId)
  ElMessage.success('已撤销')
  reload()
}

onMounted(reload)
</script>
