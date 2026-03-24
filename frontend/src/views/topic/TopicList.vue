<template>
  <div class="page-container">
    <div class="page-card">
      <!-- 搜索栏 -->
      <el-form inline :model="query" @submit.prevent="loadData">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="课题名称" clearable style="width:200px" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.type" clearable style="width:120px">
            <el-option label="设计类" :value="1" />
            <el-option label="论文类" :value="2" />
            <el-option label="软件类" :value="3" />
            <el-option label="实验类" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!auth.isStudent" label="年份">
          <el-select v-model="query.year" clearable placeholder="全部年份" style="width:110px">
            <el-option v-for="y in yearOptions" :key="y" :label="y + '届'" :value="y" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!auth.isStudent" label="状态">
          <el-select v-model="query.status" clearable style="width:120px">
            <el-option label="待审批" :value="0" />
            <el-option label="已发布" :value="1" />
            <el-option label="已关闭" :value="2" />
            <el-option label="已驳回" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 学生选题状态横幅 -->
    <div v-if="auth.isStudent" class="page-card">
      <el-alert v-if="!selectionStore.selection" type="info" show-icon :closable="false"
        title="您本届尚未选题，可在下方列表中选择心仪课题" />
      <el-alert v-else-if="selectionStore.selection.status === 0" type="warning" show-icon :closable="false"
        :title="`您已申请课题「${selectionStore.selection.topicTitle}」，等待导师「${selectionStore.selection.teacherName}」确认`" />
      <el-alert v-else-if="selectionStore.selection.status === 1" type="success" show-icon :closable="false"
        :title="`您的选题「${selectionStore.selection.topicTitle}」已由导师确认，请前往撰写开题报告`">
        <template #default>
          <el-button size="small" type="success" style="margin-top:6px" @click="$router.push('/proposal')">
            去写开题报告
          </el-button>
        </template>
      </el-alert>
      <el-alert v-else-if="selectionStore.selection.status === 2" type="error" show-icon :closable="false"
        :title="`您对课题「${selectionStore.selection.topicTitle}」的申请被拒绝，可重新选题`" />
    </div>

    <div class="page-card">
      <div class="toolbar">
        <span class="section-title">课题列表</span>
        <el-button v-if="auth.isTeacher || auth.isManager" type="primary"
          @click="$router.push('/topics/apply')">
          <el-icon><Plus /></el-icon> 申报课题
        </el-button>
      </div>

      <div class="table-scroll">
        <el-table :data="list" v-loading="loading" stripe>
          <el-table-column prop="title" label="课题名称" min-width="220" show-overflow-tooltip />
          <el-table-column label="类型" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="topicTypeColors[row.type]">{{ topicTypeLabels[row.type] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="teacherName" label="指导教师" width="110" />
          <el-table-column label="名额" width="90" align="center" sortable>
            <template #default="{ row }">
              <el-tag v-if="row.selectedCount >= row.maxStudents" type="danger" size="small" effect="dark">已满</el-tag>
              <span v-else :style="{ color: '#67c23a' }">
                {{ row.selectedCount }}/{{ row.maxStudents }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="topicStatusColors[row.status]">{{ topicStatusLabels[row.status] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="申报时间" width="160">
            <template #default="{ row }">
              {{ row.createTime ? dayjs(row.createTime).format('YYYY-MM-DD HH:mm') : '—' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
              <!-- 学生选题：已发布、有名额、且本届没有进行中的选题 -->
              <el-button
                v-if="auth.isStudent && row.status === 1 && row.selectedCount < row.maxStudents && canStudentSelect"
                link type="success" @click="doSelect(row)">选题</el-button>
              <!-- 管理员审批 -->
              <template v-if="auth.isManager && row.status === 0">
                <el-button link type="success" @click="doApprove(row, true)">通过</el-button>
                <el-button link type="danger" @click="doApprove(row, false)">驳回</el-button>
              </template>
              <!-- 管理员关闭已发布课题 -->
              <el-button v-if="auth.isManager && row.status === 1"
                link type="info" @click="doClose(row)">关闭</el-button>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty description="暂无课题数据">
              <el-button v-if="auth.isTeacher" type="primary" @click="$router.push('/topics/apply')">
                立即申报课题
              </el-button>
            </el-empty>
          </template>
        </el-table>
      </div>

      <el-pagination class="pagination-row" v-model:current-page="query.page" v-model:page-size="query.size"
        :total="total" layout="total, prev, pager, next" @change="loadData" />
    </div>

    <!-- 课题详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="课题详情" :size="drawerSize">
      <template v-if="selected">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="课题名称">{{ selected.title }}</el-descriptions-item>
          <el-descriptions-item label="课题类型">
            <el-tag size="small">{{ topicTypeLabels[selected.type] }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="指导教师">{{ selected.teacherName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="所属院系">{{ selected.collegeName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="招收名额">
            {{ selected.selectedCount }}/{{ selected.maxStudents }} 人
            <el-tag v-if="selected.selectedCount >= selected.maxStudents" type="danger" size="small" style="margin-left:8px">已满</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="选题要求">{{ selected.requirement || '—' }}</el-descriptions-item>
          <el-descriptions-item label="课题简介">{{ selected.description || '—' }}</el-descriptions-item>
        </el-descriptions>
        <!-- 申请按钮：同样需要 canStudentSelect -->
        <div style="margin-top:20px"
          v-if="auth.isStudent && selected.status === 1 && selected.selectedCount < selected.maxStudents && canStudentSelect">
          <el-button type="primary" @click="doSelect(selected); drawerVisible = false">申请该课题</el-button>
        </div>
        <!-- 已有选题时在抽屉内提示 -->
        <div v-else-if="auth.isStudent && !canStudentSelect" style="margin-top:20px">
          <el-alert type="info" :closable="false"
            :title="selectionStore.selection?.status === 1 ? '您已完成选题，无需再次申请' : '您已提交选题申请，等待导师确认'" />
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { topicApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { useSelectionStore } from '@/stores/selection'
import {
  topicStatusLabels, topicStatusColors,
  topicTypeLabels, topicTypeColors
} from '@/utils/statusConfig'

const auth = useAuthStore()
const selectionStore = useSelectionStore()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const drawerVisible = ref(false)
const selected = ref(null)

const curYear = new Date().getFullYear()
const yearOptions = [curYear - 1, curYear, curYear + 1]

const query = ref({
  page: 1, size: 10, keyword: '', type: undefined,
  status: auth.isStudent ? 1 : undefined,
  year: auth.isManager ? undefined : curYear
})

// 学生是否可以选题：没有选题，或上次申请被拒绝(2)/已解除(3)
const canStudentSelect = computed(() => {
  const s = selectionStore.selection
  return !s || s.status === 2 || s.status === 3
})

const drawerSize = computed(() => window.innerWidth <= 640 ? '90%' : '480px')

async function loadData() {
  loading.value = true
  try {
    const res = await topicApi.list(query.value)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function reset() {
  query.value = {
    page: 1, size: 10, keyword: '', type: undefined,
    status: auth.isStudent ? 1 : undefined,
    year: auth.isManager ? undefined : curYear
  }
  loadData()
}

function viewDetail(row) {
  selected.value = row
  drawerVisible.value = true
}

async function doSelect(row) {
  await ElMessageBox.confirm(`确认申请课题「${row.title}」？`, '选题确认', { type: 'info' })
  await topicApi.select(row.id, query.value.year)
  ElMessage.success('选题申请已提交，等待导师确认')
  // 刷新选题状态，使横幅和按钮立即更新
  await selectionStore.fetchMySelection(curYear)
  loadData()
}

async function doApprove(row, approve) {
  if (!approve) {
    const { value: reason } = await ElMessageBox.prompt('请输入驳回原因', '驳回课题', {
      inputPlaceholder: '请说明原因',
      confirmButtonText: '确认驳回',
      type: 'warning'
    })
    if (!reason?.trim()) return ElMessage.warning('驳回原因不能为空')
    await topicApi.approve(row.id, { approve: false, rejectReason: reason })
  } else {
    await ElMessageBox.confirm(`确认通过课题「${row.title}」审批？`, '审批确认', { type: 'info' })
    await topicApi.approve(row.id, { approve: true })
  }
  ElMessage.success(approve ? '已通过审批' : '已驳回')
  loadData()
}

async function doClose(row) {
  await ElMessageBox.confirm(`确认关闭课题「${row.title}」？关闭后学生将无法选择该课题。`, '关闭确认', { type: 'warning' })
  await topicApi.close(row.id)
  ElMessage.success('课题已关闭')
  loadData()
}

onMounted(async () => {
  if (auth.isStudent) {
    await selectionStore.fetchMySelection(curYear)
  }
  loadData()
})
</script>
