<template>
  <div class="page-card" v-if="user">
    <div class="page-title">个人中心</div>
    <el-form :model="form" label-width="100px" style="max-width: 540px">
      <el-form-item label="钱包地址">
        <el-input :value="user.walletAddress" readonly class="mono" />
      </el-form-item>
      <el-form-item label="角色">
        <el-tag :type="user.role === 'ADMIN' ? 'danger' : ''">{{ user.role }}</el-tag>
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="form.username" />
      </el-form-item>
      <el-form-item label="联系电话">
        <el-input v-model="form.phone" />
      </el-form-item>
      <el-form-item label="电子邮件">
        <el-input v-model="form.email" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="save">保存修改</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { userApi } from '../api'
import { useUserStore } from '../store/user'

const userStore = useUserStore()
const user = ref(null)
const form = ref({ username: '', phone: '', email: '' })
const saving = ref(false)

onMounted(async () => {
  user.value = await userApi.me()
  form.value = { username: user.value.username || '', phone: user.value.phone || '', email: user.value.email || '' }
})

async function save() {
  saving.value = true
  try {
    const updated = await userApi.updateMe(form.value)
    user.value = updated
    userStore.updateUser(updated)
    ElMessage.success('已保存')
  } finally { saving.value = false }
}
</script>
