<template>
  <div class="page-card">
    <div class="page-title">我的成果</div>
    <div class="flex-row mb-12" style="justify-content: space-between">
      <div class="flex-row">
        <el-button type="primary" @click="$router.push('/achievements/new')">
          <el-icon><Plus /></el-icon>新建成果
        </el-button>
        <el-button @click="reload" :icon="Refresh">刷新</el-button>
      </div>
      <span class="muted" v-if="total > 0">共 {{ total }} 条</span>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column type="index" label="#" width="60" :index="rowIndex" />
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

    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[5, 10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="onSizeChange"
        @current-change="reload"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Refresh, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { achievementApi } from '../api'

const list = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

function rowIndex(i) {
  return (pageNum.value - 1) * pageSize.value + i + 1
}

async function reload() {
  loading.value = true
  try {
    const page = await achievementApi.mine({ pageNum: pageNum.value, pageSize: pageSize.value })
    list.value = page.list || []
    total.value = page.total || 0
  } finally { loading.value = false }
}

function onSizeChange(size) {
  pageSize.value = size
  pageNum.value = 1
  reload()
}

async function del(row) {
  await achievementApi.delete(row.achievementId)
  ElMessage.success('已删除')
  // 删除后若当前页空了且不是第一页，回退一页
  if (list.value.length === 1 && pageNum.value > 1) pageNum.value -= 1
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

<style scoped>
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--c-border-soft);
}
</style>
