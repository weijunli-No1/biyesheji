<template>
  <div class="page-container">
    <!-- 欢迎卡片 -->
    <el-card class="welcome-card" shadow="never">
      <div class="welcome-inner">
        <div class="welcome-info">
          <div class="welcome-greeting">你好，{{ auth.userInfo?.realName }}</div>
          <div class="welcome-meta text-secondary">
            {{ roleName }}
            <span v-if="auth.userInfo?.department"> · {{ auth.userInfo.department }}</span>
            <span> · {{ currentYear }} 届</span>
          </div>
        </div>
        <div class="welcome-stage">
          <el-tag v-if="currentStage" size="large" type="primary" effect="dark">
            当前阶段：{{ currentStage.stageName }}
            <span class="stage-deadline">截止 {{ dayjs(currentStage.endTime).format('MM月DD日') }}</span>
          </el-tag>
          <el-tag v-else size="large" type="info">暂无进行中的阶段</el-tag>
        </div>
      </div>
    </el-card>

    <!-- 学生视图 -->
    <template v-if="auth.isStudent">
      <div class="stat-row" v-if="stat">
        <StatCard icon="Collection"   label="选题状态"
          :value="selectionStatusText" :color="selectionStatusColor" />
        <StatCard icon="Document"     label="开题进度"
          :value="proposalStatusText" :color="proposalStatusColor" />
        <StatCard icon="DataAnalysis" label="中期检查"
          :value="midCheckStatusText" :color="midCheckStatusColor" />
        <StatCard icon="Upload"       label="论文版本"
          :value="stat.thesisVersion > 0 ? `第${stat.thesisVersion}版` : '未提交'"
          :color="stat.thesisVersion > 0 ? '#1a6af0' : '#909399'" />
      </div>
      <el-card shadow="never" class="page-card">
        <template #header>
          <div class="toolbar" style="margin-bottom:0">
            <b class="section-title">我的毕业设计进度</b>
          </div>
        </template>
        <el-steps :active="stat?.processStep ?? 0" finish-status="success" align-center>
          <el-step title="选题" />
          <el-step title="任务书" />
          <el-step title="开题报告" />
          <el-step title="中期检查" />
          <el-step title="论文提交" />
          <el-step title="查重" />
          <el-step title="评阅" />
          <el-step title="答辩" />
        </el-steps>
      </el-card>
    </template>

    <!-- 教师视图 -->
    <template v-if="auth.isTeacher">
      <div class="stat-row">
        <StatCard icon="UserFilled" label="指导学生" :value="stat?.studentCount ?? 0"  color="#1a6af0" />
        <StatCard icon="Document"   label="待审开题" :value="stat?.pendingProposal ?? 0" color="#e6a23c" />
        <StatCard icon="Edit"       label="待审论文" :value="stat?.pendingThesis ?? 0"  color="#f56c6c" />
        <StatCard icon="Trophy"     label="已录成绩" :value="stat?.scoredCount ?? 0"    color="#67c23a" />
      </div>
      <TeacherStudentTable />
    </template>

    <!-- 管理员视图 -->
    <template v-if="auth.isManager">
      <div class="stat-row">
        <StatCard icon="Collection" label="已发布课题"  :value="stat?.topicCount ?? 0"        color="#1a6af0" />
        <StatCard icon="UserFilled" label="已选题学生"  :value="stat?.selectedCount ?? 0"     color="#67c23a" />
        <StatCard icon="Document"   label="开题通过"    :value="stat?.proposalPassCount ?? 0" color="#e6a23c" />
        <StatCard icon="Trophy"     label="已锁成绩"    :value="stat?.scoredCount ?? 0"       color="#f56c6c" />
      </div>
      <el-row :gutter="16" style="margin-top:16px">
        <el-col :xs="24" :sm="14">
          <el-card shadow="never">
            <template #header><b class="section-title">各阶段完成率</b></template>
            <div ref="chartRef" style="height:280px" />
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="10">
          <el-card shadow="never">
            <template #header><b class="section-title">成绩分布</b></template>
            <div ref="pieRef" style="height:280px" />
          </el-card>
        </el-col>
      </el-row>
    </template>

    <!-- 工作流时间轴 -->
    <el-card shadow="never" class="page-card" style="margin-top:16px">
      <template #header><b class="section-title">本届工作计划</b></template>
      <el-empty v-if="!stages.length" description="暂无工作计划配置" :image-size="80" />
      <el-timeline v-else>
        <el-timeline-item v-for="stage in stages" :key="stage.stage"
          :timestamp="dayjs(stage.startTime).format('YYYY-MM-DD') + ' ~ ' + dayjs(stage.endTime).format('MM-DD')"
          :type="getStageType(stage)" placement="top">
          <b>{{ stage.stageName }}</b>
          <span v-if="stage.description" class="text-secondary" style="margin-left:8px">{{ stage.description }}</span>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import dayjs from 'dayjs'
import { useAuthStore } from '@/stores/auth'
import { workflowApi, dashboardApi } from '@/api'
import { roleNames } from '@/utils/statusConfig'
import StatCard from '@/components/StatCard.vue'
import TeacherStudentTable from '@/components/TeacherStudentTable.vue'
import * as echarts from 'echarts'

const auth = useAuthStore()
const currentYear = new Date().getFullYear()
const currentStage = ref(null)
const stages = ref([])
const stat = ref(null)

const roleName = computed(() => roleNames[auth.role] || '')

// ---- 学生状态文本计算 ----
const selectionStatusMap = { 0: '待确认', 1: '已确认', 2: '已拒绝', 3: '已解除' }
const selectionColorMap  = { 0: '#e6a23c', 1: '#67c23a', 2: '#f56c6c', 3: '#909399' }
const proposalStatusMap  = { 0: '草稿', 1: '待审核', 2: '待评审', 3: '已通过', 4: '已退回' }
const proposalColorMap   = { 0: '#909399', 1: '#e6a23c', 2: '#1a6af0', 3: '#67c23a', 4: '#f56c6c' }
const midCheckStatusMap  = { 0: '未提交', 1: '待审核', 2: '已通过', 3: '已退回' }
const midCheckColorMap   = { 0: '#909399', 1: '#e6a23c', 2: '#67c23a', 3: '#f56c6c' }

const selectionStatusText  = computed(() => stat.value?.selectionStatus != null ? selectionStatusMap[stat.value.selectionStatus] : '未选题')
const selectionStatusColor = computed(() => stat.value?.selectionStatus != null ? selectionColorMap[stat.value.selectionStatus] : '#909399')
const proposalStatusText   = computed(() => stat.value?.proposalStatus != null ? proposalStatusMap[stat.value.proposalStatus] : '未提交')
const proposalStatusColor  = computed(() => stat.value?.proposalStatus != null ? proposalColorMap[stat.value.proposalStatus] : '#909399')
const midCheckStatusText   = computed(() => stat.value?.midCheckStatus != null ? midCheckStatusMap[stat.value.midCheckStatus] : '未提交')
const midCheckStatusColor  = computed(() => stat.value?.midCheckStatus != null ? midCheckColorMap[stat.value.midCheckStatus] : '#909399')

function getStageType(stage) {
  const now = dayjs()
  if (now.isAfter(dayjs(stage.endTime))) return 'success'
  if (now.isAfter(dayjs(stage.startTime))) return 'primary'
  return 'info'
}

const chartRef = ref()
const pieRef = ref()

async function loadData() {
  try {
    const [stageRes, stagesRes, statRes] = await Promise.all([
      workflowApi.currentStage(currentYear),
      workflowApi.list(currentYear),
      dashboardApi.stats(),
    ])
    currentStage.value = stageRes.data
    stages.value = stagesRes.data || []
    stat.value = statRes.data

    if (auth.isManager) {
      await nextTick()
      initCharts(statRes.data)
    }
  } catch {
    // 错误已由 request 拦截器统一弹出
  }
}

function initCharts(data) {
  if (chartRef.value) {
    const chart = echarts.init(chartRef.value)
    const stageData = data?.stageCompletion || []
    chart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } },
      yAxis: { type: 'category', data: stageData.map(s => s.stage) },
      series: [{
        type: 'bar',
        data: stageData.map(s => s.percent),
        itemStyle: { color: '#1a6af0' },
        label: { show: true, position: 'right', formatter: '{c}%' }
      }]
    })
  }

  if (pieRef.value) {
    const pie = echarts.init(pieRef.value)
    const gradeData = data?.gradeDistribution || []
    pie.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie', radius: ['40%', '70%'],
        data: gradeData,
        emphasis: { itemStyle: { shadowBlur: 10 } }
      }]
    })
  }
}

onMounted(loadData)
</script>

<style scoped>
.welcome-card {
  margin-bottom: var(--space-md);
  background: linear-gradient(135deg, #e8f0fe 0%, #fff 100%);
}
.welcome-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: var(--space-md);
}
.welcome-greeting {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  margin-bottom: 6px;
}
.stage-deadline {
  margin-left: 6px;
  opacity: 0.85;
  font-size: var(--font-size-sm);
}

@media (max-width: 640px) {
  .welcome-inner { flex-direction: column; align-items: flex-start; }
}
</style>
