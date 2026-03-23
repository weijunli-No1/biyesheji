<template>
  <div class="page-container">
    <div class="page-card">
      <el-form inline :model="query">
        <el-form-item label="姓名/工号">
          <el-input v-model="query.keyword" clearable style="width:180px" @keyup.enter="loadData" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="query.role" clearable style="width:120px">
            <el-option label="学生"       :value="1" />
            <el-option label="指导教师"   :value="2" />
            <el-option label="评阅教师"   :value="3" />
            <el-option label="答辩委员"   :value="4" />
            <el-option label="专业管理员" :value="5" />
            <el-option label="学院管理员" :value="6" />
            <el-option label="教务管理员" :value="7" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="page-card">
      <div class="table-scroll">
        <el-table :data="list" v-loading="loading" stripe>
          <el-table-column prop="username"   label="学号/工号" width="130" />
          <el-table-column prop="realName"   label="姓名" width="90" />
          <el-table-column label="角色" width="110">
            <template #default="{ row }">
              <el-tag :type="roleTagTypes[row.role]" size="small">{{ roleNames[row.role] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="学院" width="130">
            <template #default="{ row }">{{ collegeMap[row.collegeId] || '—' }}</template>
          </el-table-column>
          <el-table-column label="专业" width="130">
            <template #default="{ row }">{{ majorMap[row.majorId] || '—' }}</template>
          </el-table-column>
          <el-table-column prop="email"      label="邮箱" show-overflow-tooltip />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-switch v-model="row.status" :active-value="1" :inactive-value="0"
                @change="toggleStatus(row)" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button link type="warning" @click="resetPwd(row)">重置密码</el-button>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty description="暂无用户数据" />
          </template>
        </el-table>
      </div>

      <el-pagination class="pagination-row" v-model:current-page="query.page"
        v-model:page-size="query.size" :total="total"
        layout="total, prev, pager, next" @change="loadData" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { userApi, orgApi } from '@/api'
import { roleNames, roleTagTypes } from '@/utils/statusConfig'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = ref({ page: 1, size: 10, keyword: '', role: undefined })
const collegeMap = ref({})
const majorMap = ref({})

async function loadOrgData() {
  const [cRes, mRes] = await Promise.all([orgApi.listColleges(), orgApi.listMajors()])
  cRes.data?.forEach(c => { collegeMap.value[c.id] = c.name })
  mRes.data?.forEach(m => { majorMap.value[m.id] = m.name })
}

async function loadData() {
  loading.value = true
  try {
    const res = await userApi.list(query.value)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function reset() {
  query.value = { page: 1, size: 10, keyword: '', role: undefined }
  loadData()
}

async function toggleStatus(row) {
  try {
    await userApi.updateStatus(row.id, row.status)
    ElMessage.success(`已${row.status === 1 ? '启用' : '禁用'}用户 ${row.realName}`)
  } catch {
    row.status = row.status === 1 ? 0 : 1  // 回滚
  }
}

async function resetPwd(row) {
  await ElMessageBox.confirm(
    `确认重置「${row.realName}」的密码为 password123？`,
    '重置密码',
    { type: 'warning', confirmButtonText: '确认重置' }
  )
  await userApi.resetPassword(row.id)
  ElMessage.success(`已重置 ${row.realName} 的密码`)
}

onMounted(() => { loadOrgData(); loadData() })
</script>
