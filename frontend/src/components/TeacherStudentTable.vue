<template>
  <el-card shadow="never">
    <template #header>
      <b class="section-title">我指导的学生</b>
    </template>

    <div class="table-scroll">
      <el-table :data="students" v-loading="loading" stripe>
        <el-table-column prop="studentNo"   label="学号" width="120" />
        <el-table-column prop="studentName" label="姓名" width="90" />
        <el-table-column prop="topicTitle"  label="课题" show-overflow-tooltip />
        <el-table-column label="开题" width="100">
          <template #default="{ row }">
            <el-tag :type="proposalStatusColors[row.proposalStatus]" size="small">
              {{ proposalStatusLabels[row.proposalStatus] ?? '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="中期" width="100">
          <template #default="{ row }">
            <el-tag :type="midCheckStatusColors[row.midStatus]" size="small">
              {{ midCheckStatusLabels[row.midStatus] ?? '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="论文版本" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.thesisVersion" type="info" size="small">v{{ row.thesisVersion }}</el-tag>
            <span v-else class="text-placeholder">未提交</span>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无指导学生" :image-size="60" />
        </template>
      </el-table>
    </div>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { topicApi } from '@/api'
import { proposalStatusLabels, proposalStatusColors, midCheckStatusLabels, midCheckStatusColors } from '@/utils/statusConfig'

const loading = ref(false)
const students = ref([])

onMounted(async () => {
  loading.value = true
  try {
    const res = await topicApi.teacherSelections()
    students.value = res.data || []
  } catch {
    // 静默，错误已由拦截器处理
    students.value = []
  } finally {
    loading.value = false
  }
})
</script>
