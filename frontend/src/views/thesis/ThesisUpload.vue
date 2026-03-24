<template>
  <div class="page-container">
    <!-- 前置条件检查 -->
    <div v-if="!selectionStore.isConfirmed()" class="page-card">
      <el-result icon="warning" title="选题未确认" sub-title="请先完成选题，并等待导师确认后再提交论文" />
    </div>
    <div v-else-if="proposalStatus !== 3" class="page-card">
      <el-result icon="warning" title="开题报告未通过"
        :sub-title="proposalStatus === null ? '请先提交并通过开题报告' : '开题报告尚未通过评审，通过后方可提交论文'" />
    </div>

    <template v-else>
      <!-- 版本历史 -->
      <div class="page-card">
        <div style="display:flex;justify-content:space-between;margin-bottom:12px">
          <b>提交历史</b>
          <el-tag>当前版本：v{{ versions.length }}</el-tag>
        </div>
        <el-timeline v-if="versions.length">
          <el-timeline-item v-for="v in versions" :key="v.id"
            :type="v.status === 2 ? 'success' : v.status === 1 ? 'danger' : 'primary'"
            :timestamp="v.createTime">
            <div style="display:flex;align-items:center;gap:12px">
              <span>第 {{ v.version }} 版（{{ versionTypeLabels[v.versionType] }}）</span>
              <span class="text-secondary" style="font-size:12px">{{ formatTime(v.createTime) }}</span>
              <el-tag size="small" :type="vStatusColors[v.status]">{{ vStatusLabels[v.status] }}</el-tag>
              <el-button v-if="v.fileUrl" link type="primary" size="small" @click="download(v.fileUrl)">下载</el-button>
            </div>
            <div v-if="v.comment" style="margin-top:6px;color:#f56c6c;font-size:13px">
              导师意见：{{ v.comment }}
            </div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂未提交论文" :image-size="60" />
      </div>

      <!-- 上传新版本（上一版为"待审阅"时禁用） -->
      <div class="page-card">
        <el-alert v-if="lastVersionPending"
          title="上一版论文导师尚未审阅完毕，请等待审阅结果后再提交新版本"
          type="warning" show-icon :closable="false" style="margin-bottom:16px" />

        <el-form ref="formRef" :model="form" :rules="rules" label-width="110px"
          :disabled="lastVersionPending">
          <el-form-item label="版本类型">
            <el-tag>{{ versionTypeLabels[form.versionType] }}</el-tag>
            <span class="text-secondary" style="margin-left:8px;font-size:12px">（系统自动判断）</span>
          </el-form-item>
          <el-form-item label="论文文件" prop="fileUrl">
            <el-upload drag action="/api/files/upload" :headers="uploadHeaders"
              :on-success="onUploadSuccess" :on-error="onUploadError"
              accept=".pdf,.doc,.docx" :limit="1" :file-list="fileList"
              :disabled="lastVersionPending">
              <el-icon size="40" color="#c0c4cc"><Upload /></el-icon>
              <div class="el-upload__text">拖拽文件到此处，或 <em>点击上传</em></div>
              <template #tip><div class="el-upload__tip">支持 PDF/Word，不超过 50MB</div></template>
            </el-upload>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting"
              :disabled="lastVersionPending" @click="doUpload">
              提交新版本
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { thesisApi, proposalApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { useSelectionStore } from '@/stores/selection'
import { thesisVersionTypeLabels, thesisVersionStatusLabels, thesisVersionStatusColors } from '@/utils/statusConfig'

const auth = useAuthStore()
const selectionStore = useSelectionStore()
const formRef = ref()
const submitting = ref(false)
const versions = ref([])
const fileList = ref([])
const proposalStatus = ref(null) // null=未提交，0-4 对应开题报告状态

// 从 store 动态获取 selectionId
const selectionId = computed(() => selectionStore.selectionId())

// 上一版论文是否处于"待审阅(0)"状态
const lastVersionPending = computed(() =>
  versions.value.length > 0 && versions.value[0].status === 0
)

const uploadHeaders = computed(() => ({ Authorization: `Bearer ${auth.token}` }))
const form = ref({ versionType: 1, fileUrl: '', fileName: '', fileSize: 0 })

const rules = {
  fileUrl: [{ required: true, message: '请上传论文文件' }],
}

const versionTypeLabels = thesisVersionTypeLabels
const vStatusLabels = thesisVersionStatusLabels
const vStatusColors = thesisVersionStatusColors

function formatTime(t) { return t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '—' }

function onUploadSuccess(res, file) {
  if (res.code === 200) {
    form.value.fileUrl = res.data.url
    form.value.fileName = file.name
    form.value.fileSize = file.size
    ElMessage.success('文件上传成功')
  } else {
    ElMessage.error(res.message || '文件上传失败，请重试')
  }
}

function onUploadError(err) {
  const msg = err?.message || ''
  if (msg.includes('413') || msg.includes('size')) {
    ElMessage.error('文件超过大小限制（50MB），请压缩后重试')
  } else if (msg.includes('415') || msg.includes('type')) {
    ElMessage.error('不支持的文件格式，请上传 PDF 或 Word 文档')
  } else {
    ElMessage.error('文件上传失败，请检查网络后重试')
  }
}
function download(url) { window.open(url, '_blank') }

async function doUpload() {
  if (!selectionId.value) return ElMessage.error('选题信息未加载')
  await formRef.value.validate()
  submitting.value = true
  try {
    await thesisApi.upload({ ...form.value, selectionId: selectionId.value })
    ElMessage.success('论文版本已提交，等待导师审阅')
    fileList.value = []
    form.value.fileUrl = ''
    await loadVersions()
  } finally {
    submitting.value = false
  }
}

async function loadVersions() {
  if (!selectionId.value) return
  const res = await thesisApi.versions(selectionId.value)
  versions.value = res.data || []
  // P14: 根据提交历史自动推断下一版本类型
  const count = versions.value.length
  if (count === 0) {
    form.value.versionType = 1 // 初稿
  } else if (versions.value[0]?.status === 1) {
    form.value.versionType = 2 // 上一版被退回 → 修改稿
  } else {
    form.value.versionType = 2 // 默认修改稿，学生无需手动选
  }
}

async function loadProposalStatus() {
  if (!selectionId.value) return
  const res = await proposalApi.getBySelection(selectionId.value)
  proposalStatus.value = res.data?.status ?? null
}

onMounted(async () => {
  const currentYear = new Date().getFullYear()
  if (!selectionStore.selection) {
    await selectionStore.fetchMySelection(currentYear)
  }
  await Promise.all([loadVersions(), loadProposalStatus()])
})
</script>
