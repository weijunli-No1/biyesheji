<template>
  <div class="page-container">
    <div class="page-card">
      <el-form inline :model="query">
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable style="width:140px">
            <el-option label="待导师审核" :value="1" />
            <el-option label="导师已通过" :value="2" />
            <el-option label="评审已通过" :value="3" />
            <el-option label="已退回"    :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="page-card">
      <div class="table-scroll">
        <el-table :data="list" v-loading="loading" stripe>
          <el-table-column prop="studentNo"   label="学号" width="130" />
          <el-table-column prop="studentName" label="学生" width="90" />
          <el-table-column prop="topicTitle"  label="课题" min-width="200" show-overflow-tooltip />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag size="small" :type="proposalStatusColors[row.status]">{{ proposalStatusLabels[row.status] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="提交时间" width="160">
            <template #default="{ row }">
              {{ row.submitTime ? dayjs(row.submitTime).format('YYYY-MM-DD HH:mm') : '—' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="viewDetail(row)">查看</el-button>
              <!-- 导师审核 -->
              <template v-if="auth.isTeacher && row.status === 1">
                <el-button link type="success" @click="teacherPass(row)">通过</el-button>
                <el-button link type="danger" @click="teacherReject(row)">退回</el-button>
              </template>
              <!-- 管理员评审 -->
              <template v-if="auth.isManager && row.status === 2">
                <el-button link type="success" @click="deptPass(row)">评审通过</el-button>
                <el-button link type="danger" @click="deptReject(row)">评审退回</el-button>
              </template>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty description="暂无开题报告" />
          </template>
        </el-table>
      </div>

      <el-pagination class="pagination-row" v-model:current-page="query.page"
        :total="total" layout="total, prev, pager, next" @change="loadData" />
    </div>

    <!-- P11: 管理员评审通过对话框（替代两次弹窗，合并为单个表单） -->
    <el-dialog v-model="deptPassDialogVisible" title="开题评审通过" :width="dialogWidth">
      <el-form :model="deptPassForm" label-width="100px">
        <el-form-item label="开题评分">
          <el-input-number v-model="deptPassForm.score" :min="0" :max="100" :precision="0" />
          <span class="text-secondary" style="margin-left:8px">分（0–100）</span>
        </el-form-item>
        <el-form-item label="评审意见">
          <el-input v-model="deptPassForm.comment" type="textarea" :rows="3"
            placeholder="请填写对开题报告的评审意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deptPassDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="doConfirmDeptPass" :loading="passing">确认通过</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="开题报告详情" :size="drawerSize">
      <el-descriptions v-if="selected" :column="1" border>
        <el-descriptions-item label="学生">{{ selected.studentName }}（{{ selected.studentNo }}）</el-descriptions-item>
        <el-descriptions-item label="课题">{{ selected.topicTitle }}</el-descriptions-item>
        <el-descriptions-item label="研究背景">{{ selected.background || '—' }}</el-descriptions-item>
        <el-descriptions-item label="文献综述">{{ selected.literature || '—' }}</el-descriptions-item>
        <el-descriptions-item label="研究方法">{{ selected.method || '—' }}</el-descriptions-item>
        <el-descriptions-item label="进度安排">{{ selected.plan || '—' }}</el-descriptions-item>
        <el-descriptions-item label="预期成果">{{ selected.expectedResult || '—' }}</el-descriptions-item>
        <el-descriptions-item label="开题报告文件">
          <template v-if="selected.fileUrl">
            <a :href="selected.fileUrl" target="_blank" rel="noopener">
              <el-button type="primary" text size="small">查看 / 下载</el-button>
            </a>
          </template>
          <span v-else style="color:#909399">未上传</span>
        </el-descriptions-item>
        <el-descriptions-item label="外文翻译文件">
          <template v-if="selected.translationUrl">
            <a :href="selected.translationUrl" target="_blank" rel="noopener">
              <el-button type="primary" text size="small">查看 / 下载</el-button>
            </a>
          </template>
          <span v-else style="color:#909399">未上传</span>
        </el-descriptions-item>
        <el-descriptions-item v-if="selected.teacherComment" label="导师意见">
          {{ selected.teacherComment }}
        </el-descriptions-item>
        <el-descriptions-item v-if="selected.reviewComment" label="评审意见">
          {{ selected.reviewComment }}
        </el-descriptions-item>
        <el-descriptions-item v-if="selected.reviewScore != null" label="开题评分">
          {{ selected.reviewScore }} 分
        </el-descriptions-item>
        <el-descriptions-item v-if="selected.rejectReason" label="退回原因">
          <el-text type="danger">{{ selected.rejectReason }}</el-text>
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { proposalApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { proposalStatusLabels, proposalStatusColors } from '@/utils/statusConfig'

const auth = useAuthStore()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const drawerVisible = ref(false)
const selected = ref(null)
const query = ref({ page: 1, size: 10, status: auth.isTeacher ? 1 : undefined })
const passing = ref(false)
const deptPassDialogVisible = ref(false)
const deptPassRow = ref(null)
const deptPassForm = ref({ score: null, comment: '' })

const drawerSize = computed(() => window.innerWidth <= 640 ? '90%' : '600px')
const dialogWidth = computed(() => window.innerWidth <= 640 ? '92%' : '480px')

async function loadData() {
  loading.value = true
  try {
    const res = await proposalApi.list(query.value)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function viewDetail(row) {
  selected.value = row
  drawerVisible.value = true
}

async function teacherPass(row) {
  const { value: comment } = await ElMessageBox.prompt(
    '请填写审核意见（可选）', '导师审核通过', {
      inputPlaceholder: '对学生开题报告的综合评价',
      confirmButtonText: '确认通过',
      type: 'info'
    }
  )
  await proposalApi.teacherReview(row.id, { pass: true, comment })
  ElMessage.success('已通过，进入评审环节')
  loadData()
}

async function teacherReject(row) {
  const { value: reason } = await ElMessageBox.prompt(
    '请填写退回原因', '退回开题报告', {
      inputPlaceholder: '请说明需要修改的问题',
      confirmButtonText: '确认退回',
      type: 'warning'
    }
  )
  if (!reason?.trim()) return ElMessage.warning('退回原因不能为空')
  await proposalApi.teacherReview(row.id, { pass: false, rejectReason: reason })
  ElMessage.success('已退回')
  loadData()
}

function deptPass(row) {
  deptPassRow.value = row
  deptPassForm.value = { score: null, comment: '' }
  deptPassDialogVisible.value = true
}

async function doConfirmDeptPass() {
  if (deptPassForm.value.score === null || deptPassForm.value.score < 0 || deptPassForm.value.score > 100) {
    return ElMessage.warning('请填写 0–100 之间的评分')
  }
  if (!deptPassForm.value.comment?.trim()) {
    return ElMessage.warning('请填写评审意见')
  }
  passing.value = true
  try {
    await proposalApi.deptReview(deptPassRow.value.id, {
      pass: true,
      reviewScore: deptPassForm.value.score,
      reviewComment: deptPassForm.value.comment
    })
    ElMessage.success('评审通过')
    deptPassDialogVisible.value = false
    loadData()
  } finally {
    passing.value = false
  }
}

async function deptReject(row) {
  const { value: reason } = await ElMessageBox.prompt(
    '请填写退回原因', '评审退回', {
      inputPlaceholder: '请说明退回原因',
      confirmButtonText: '确认退回',
      type: 'warning'
    }
  )
  if (!reason?.trim()) return ElMessage.warning('退回原因不能为空')
  await proposalApi.deptReview(row.id, { pass: false, rejectReason: reason })
  ElMessage.success('已退回')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.pagination-row { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
