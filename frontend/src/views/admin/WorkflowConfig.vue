<template>
  <div class="page-container">
    <div class="page-card">
      <div class="toolbar">
        <b class="section-title">工作流时间节点配置</b>
        <div style="display:flex;gap:8px;align-items:center">
          <span>届次：</span>
          <el-select v-model="year" style="width:100px" @change="loadData">
            <el-option v-for="y in years" :key="y" :label="y + '届'" :value="y" />
          </el-select>
          <el-button type="primary" @click="saveAll" :loading="saving">保存全部</el-button>
        </div>
      </div>

      <el-table :data="stages" stripe>
        <el-table-column label="阶段" width="150">
          <template #default="{ row }">
            <b>{{ row.stageName }}</b>
          </template>
        </el-table-column>
        <el-table-column label="开放时间" width="220">
          <template #default="{ row }">
            <el-date-picker v-model="row.startTime" type="datetime" format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" />
          </template>
        </el-table-column>
        <el-table-column label="截止时间" width="220">
          <template #default="{ row }">
            <el-date-picker v-model="row.endTime" type="datetime" format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" />
          </template>
        </el-table-column>
        <el-table-column label="当前状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStageStatus(row).type" size="small">{{ getStageStatus(row).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注">
          <template #default="{ row }">
            <el-input v-model="row.description" placeholder="可选备注" size="small" />
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { workflowApi } from '@/api'
import dayjs from 'dayjs'

const year = ref(new Date().getFullYear())
const years = [2024, 2025, 2026, 2027]
const stages = ref([])
const saving = ref(false)

const stageNames = {
  TOPIC_APPLY: '课题申报',
  TOPIC_SELECT: '学生选题',
  TASK_BOOK: '任务书下达',
  PROPOSAL: '开题报告',
  MID_CHECK: '中期检查',
  THESIS: '论文提交',
  CHECK: '查重检测',
  REVIEW: '论文评阅',
  DEFENSE: '毕业答辩',
}

function getStageStatus(row) {
  const now = dayjs()
  const start = dayjs(row.startTime)
  const end = dayjs(row.endTime)
  if (now.isBefore(start)) return { type: 'info', label: '未开始' }
  if (now.isAfter(end)) return { type: 'success', label: '已结束' }
  return { type: 'primary', label: '进行中' }
}

async function loadData() {
  const res = await workflowApi.list(year.value)
  const fromServer = (res.data || []).map(s => ({ ...s, stageName: stageNames[s.stage] || s.stage }))
  const existingStages = fromServer.map(s => s.stage)
  // 补全缺少的阶段
  for (const [stage, stageName] of Object.entries(stageNames)) {
    if (!existingStages.includes(stage)) {
      fromServer.push({ stage, stageName, year: year.value, startTime: null, endTime: null })
    }
  }
  // 按固定顺序排序
  const order = Object.keys(stageNames)
  fromServer.sort((a, b) => order.indexOf(a.stage) - order.indexOf(b.stage))
  stages.value = fromServer
}

function validateDates() {
  for (const s of stages.value) {
    if (s.startTime && s.endTime && dayjs(s.endTime).isBefore(dayjs(s.startTime))) {
      ElMessage.error(`「${s.stageName}」截止时间不能早于开放时间`)
      return false
    }
  }
  return true
}

async function saveAll() {
  if (!validateDates()) return
  saving.value = true
  try {
    await workflowApi.batchSave(stages.value)
    ElMessage.success('时间节点已保存')
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>
