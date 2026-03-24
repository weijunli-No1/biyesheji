<template>
  <div class="page-container">
    <div class="page-card">
      <div class="toolbar">
        <span class="section-title">中期检查管理</span>
        <el-form inline :model="query" style="margin-bottom:0">
          <el-form-item label="关键词">
            <el-input v-model="query.keyword" placeholder="学生姓名/学号" clearable
              style="width:160px" @keyup.enter="load" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.status" clearable style="width:120px">
              <el-option label="未提交" :value="0" />
              <el-option label="待审核" :value="1" />
              <el-option label="已通过" :value="2" />
              <el-option label="已退回" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="load">查询</el-button>
            <el-button @click="reset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="table-scroll">
        <el-table :data="list" v-loading="loading" stripe style="margin-top:8px">
          <el-table-column prop="studentNo"   label="学号" width="130" />
          <el-table-column prop="studentName" label="学生" width="90" />
          <el-table-column prop="topicTitle"  label="课题" min-width="180" show-overflow-tooltip />
          <el-table-column label="进度" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.progress" :type="progressColors[row.progress]" size="small">
                {{ progressLabels[row.progress] }}
              </el-tag>
              <span v-else class="text-placeholder">—</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="midCheckStatusColors[row.status]" size="small">
                {{ midCheckStatusLabels[row.status] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="提交时间" width="160">
            <template #default="{ row }">
              {{ row.submitTime ? dayjs(row.submitTime).format('YYYY-MM-DD HH:mm') : '—' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button link type="primary" @click="view(row)">查看</el-button>
              <el-button v-if="auth.isTeacher && row.status === 1"
                link type="success" @click="openReview(row)">审核</el-button>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty description="暂无中期检查记录" />
          </template>
        </el-table>
      </div>

      <el-pagination class="pagination-row" v-model:current-page="query.page"
        :total="total" layout="total, prev, pager, next" @change="load" />
    </div>

    <!-- 查看/审核对话框 -->
    <el-dialog v-model="dialogVisible" :title="reviewMode ? '中期检查审核' : '查看中期检查'"
      :width="dialogWidth">
      <template v-if="selected">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="已完成工作">{{ selected.completedWork || '—' }}</el-descriptions-item>
          <el-descriptions-item label="存在问题">{{ selected.problems || '—' }}</el-descriptions-item>
          <el-descriptions-item label="下阶段计划">{{ selected.nextPlan || '—' }}</el-descriptions-item>
        </el-descriptions>

        <el-form v-if="reviewMode" :model="reviewForm" label-width="100px" style="margin-top:16px">
          <el-form-item label="进度评估">
            <el-radio-group v-model="reviewForm.progress">
              <el-radio :label="1">正常</el-radio>
              <el-radio :label="2">滞后</el-radio>
              <el-radio :label="3">严重滞后</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="导师意见">
            <el-input v-model="reviewForm.comment" type="textarea" :rows="3"
              placeholder="请填写对学生中期进度的综合评价" />
          </el-form-item>
        </el-form>
      </template>

      <template #footer v-if="reviewMode">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="danger" @click="doReject" :loading="saving">退回</el-button>
        <el-button type="primary" @click="doPass" :loading="saving">审核通过</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { midCheckApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import {
  midCheckStatusLabels, midCheckStatusColors,
  progressLabels, progressColors
} from '@/utils/statusConfig'

const auth = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const reviewMode = ref(false)
const selected = ref(null)
const reviewForm = ref({ progress: 1, comment: '' })
const query = ref({ page: 1, size: 10, keyword: '', status: auth.isTeacher ? 1 : undefined })

const dialogWidth = computed(() => window.innerWidth <= 640 ? '92%' : '600px')

async function load() {
  loading.value = true
  try {
    const res = await midCheckApi.list(query.value)
    if (res.data?.records !== undefined) {
      list.value = res.data.records
      total.value = res.data.total
    } else {
      list.value = res.data || []
      total.value = list.value.length
    }
  } finally {
    loading.value = false
  }
}

function reset() {
  query.value = { page: 1, size: 10, keyword: '', status: auth.isTeacher ? 1 : undefined }
  load()
}

function view(row) {
  selected.value = row
  reviewMode.value = false
  dialogVisible.value = true
}

function openReview(row) {
  selected.value = row
  reviewForm.value = { progress: 1, comment: '' }
  reviewMode.value = true
  dialogVisible.value = true
}

async function doPass() {
  if (!reviewForm.value.comment?.trim()) {
    return ElMessage.warning('请填写导师意见')
  }
  saving.value = true
  try {
    await midCheckApi.review(selected.value.id, { pass: true, ...reviewForm.value })
    ElMessage.success('审核通过')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function doReject() {
  if (!reviewForm.value.comment?.trim()) {
    return ElMessage.warning('退回时请填写意见，告知学生原因')
  }
  await ElMessageBox.confirm('确认退回该中期检查？学生将需要重新提交。', '退回确认', { type: 'warning' })
  saving.value = true
  try {
    await midCheckApi.review(selected.value.id, { pass: false, ...reviewForm.value })
    ElMessage.success('已退回')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.pagination-row { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
