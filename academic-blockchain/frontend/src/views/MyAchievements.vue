<template>
  <div class="page-card">
    <div class="page-title">我的成果</div>
    <div class="flex-row mb-12">
      <el-button type="primary" @click="$router.push('/achievements/new')">
        <el-icon><Plus /></el-icon>新建成果
      </el-button>
      <el-button @click="reload" :icon="Refresh">刷新</el-button>
    </div>
    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column type="index" label="#" width="60" />
      <el-table-column prop="name" label="成果标题" min-width="220" />
      <el-table-column prop="category" label="类型" width="120">
        <template #default="{ row }">
          <el-tag>{{ categoryLabel(row.category) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="$router.push('/achievements/' + row.achievementId)">查看</el-button>
          <el-button link type="primary" @click="$router.push('/grant?id=' + row.achievementId)" v-if="row.status === 'REGISTERED' || row.status === 'SHARED'">授权</el-button>
          <el-popconfirm title="确认删除？已上链成果不可删除" @confirm="del(row)" v-if="row.status === 'CREATED'">
            <template #reference>
              <el-button link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { achievementApi } from '../api'

const list = ref([])
const loading = ref(false)

async function reload() {
  loading.value = true
  try { list.value = await achievementApi.mine() }
  finally { loading.value = false }
}

async function del(row) {
  await achievementApi.delete(row.achievementId)
  ElMessage.success('已删除')
  reload()
}

function categoryLabel(c) {
  const map = { PAPER: '论文', DATA: '实验数据', REPORT: '技术报告', CODE: '软件源码', OTHER: '其他' }
  return map[c] || c || '未指定'
}
function statusLabel(s) {
  const map = { CREATED: '未存证', REGISTERED: '已存证', SHARED: '已共享' }
  return map[s] || s
}
function statusTag(s) {
  return { CREATED: 'info', REGISTERED: 'success', SHARED: 'warning' }[s] || 'info'
}

onMounted(reload)
</script>
