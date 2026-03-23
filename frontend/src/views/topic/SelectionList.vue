<template>
  <div class="page-container">
    <div class="page-card">
      <div class="toolbar">
        <span class="section-title">选题管理</span>
      </div>

      <div class="table-scroll">
        <el-table :data="list" v-loading="loading" stripe>
          <el-table-column prop="studentNo"   label="学号"     width="130" />
          <el-table-column prop="studentName" label="学生姓名"  width="100" />
          <el-table-column prop="topicTitle"  label="课题"     min-width="200" show-overflow-tooltip />
          <el-table-column prop="teacherName" label="指导教师"  width="110" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="selectionStatusColors[row.status]">
                {{ selectionStatusLabels[row.status] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="申请时间" width="160">
            <template #default="{ row }">
              {{ row.applyTime ? dayjs(row.applyTime).format('YYYY-MM-DD HH:mm') : '—' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" v-if="auth.isTeacher">
            <template #default="{ row }">
              <template v-if="row.status === 0">
                <el-button link type="success" @click="handleConfirm(row, true)"
                  aria-label="确认该学生的选题申请">确认</el-button>
                <el-button link type="danger" @click="handleConfirm(row, false)"
                  aria-label="拒绝该学生的选题申请">拒绝</el-button>
              </template>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty description="暂无选题记录">
              <template #description>
                <p>{{ auth.isTeacher ? '尚未有学生申请您的课题' : '暂无选题记录' }}</p>
              </template>
            </el-empty>
          </template>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { topicApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { selectionStatusLabels, selectionStatusColors } from '@/utils/statusConfig'

const auth = useAuthStore()
const loading = ref(false)
const list = ref([])

async function load() {
  loading.value = true
  try {
    const res = await topicApi.teacherSelections()
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function handleConfirm(row, ok) {
  const action = ok ? '确认' : '拒绝'
  await ElMessageBox.confirm(
    `确认${action}学生「${row.studentName}」的选题申请？`,
    `${action}选题`,
    { type: ok ? 'info' : 'warning', confirmButtonText: `确认${action}` }
  )
  await topicApi.confirmSelection(row.id, { confirm: ok })
  ElMessage.success(ok ? '已确认，学生可开始撰写开题报告' : '已拒绝')
  load()
}

onMounted(load)
</script>
