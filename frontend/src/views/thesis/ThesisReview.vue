<template>
  <div class="page-container">
    <div class="page-card">
      <div class="toolbar">
        <span class="section-title">论文审阅</span>
      </div>

      <div class="table-scroll">
        <el-table :data="list" v-loading="loading" stripe>
          <el-table-column prop="studentName" label="学生"   width="90" />
          <el-table-column prop="topicTitle"  label="课题"   min-width="200" show-overflow-tooltip />
          <el-table-column label="版本" width="80" align="center">
            <template #default="{ row }">
              <el-tag size="small" type="info">v{{ row.version }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="90">
            <template #default="{ row }">
              <el-tag size="small">{{ thesisVersionTypeLabels[row.versionType] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="thesisVersionStatusColors[row.status]">
                {{ thesisVersionStatusLabels[row.status] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="提交时间" width="160" />
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-button link type="primary" @click="download(row.fileUrl)"
                :disabled="!row.fileUrl" aria-label="下载论文文件">
                下载论文
              </el-button>
              <el-button v-if="row.status === 0" link type="success" @click="openReview(row)">
                审阅
              </el-button>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty description="暂无待审阅的论文" />
          </template>
        </el-table>
      </div>
    </div>

    <!-- 审阅意见对话框 -->
    <el-dialog v-model="dialogVisible" title="论文审阅意见" :width="dialogWidth">
      <el-alert type="info" :closable="false" style="margin-bottom:16px"
        description="请下载论文后仔细阅读，再填写审阅意见。审阅通过后学生可继续提交下一版本。" />
      <el-form :model="reviewForm" label-width="80px">
        <el-form-item label="审阅意见">
          <el-input v-model="reviewForm.comment" type="textarea" :rows="5"
            placeholder="请填写修改意见或审阅评语（退回时必填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="danger" @click="doReview(false)" :loading="saving">退回修改</el-button>
        <el-button type="primary" @click="doReview(true)" :loading="saving">审阅通过</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { thesisApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import {
  thesisVersionTypeLabels,
  thesisVersionStatusLabels,
  thesisVersionStatusColors
} from '@/utils/statusConfig'

const auth = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const list = ref([])
const dialogVisible = ref(false)
const selected = ref(null)
const reviewForm = ref({ comment: '' })

const dialogWidth = computed(() => window.innerWidth <= 640 ? '92%' : '500px')

async function loadList() {
  loading.value = true
  try {
    const res = await thesisApi.teacherList()
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

function download(url) {
  if (!url) return ElMessage.warning('暂无文件链接')
  window.open(url, '_blank')
}

function openReview(row) {
  selected.value = row
  reviewForm.value = { comment: '' }
  dialogVisible.value = true
}

async function doReview(pass) {
  if (!pass && !reviewForm.value.comment?.trim()) {
    return ElMessage.warning('退回时请填写修改意见')
  }
  if (pass) {
    await ElMessageBox.confirm('确认该论文版本审阅通过？', '审阅通过', { type: 'info' })
  }
  saving.value = true
  try {
    await thesisApi.review(selected.value.id, { pass, comment: reviewForm.value.comment })
    ElMessage.success(pass ? '已通过，学生可继续提交新版本' : '已退回，请学生修改后重新提交')
    dialogVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

onMounted(loadList)
</script>
