<template>
  <div class="page-container">
    <div v-if="!selectionStore.isConfirmed()" class="page-card">
      <el-result icon="warning" title="尚未完成选题" sub-title="开题报告需要在导师确认选题后才能提交">
        <template #extra>
          <el-button type="primary" @click="$router.push('/topics')">去选题</el-button>
        </template>
      </el-result>
    </div>

    <div v-else class="page-card" style="max-width:900px">
      <div style="display:flex;align-items:center;gap:16px;margin-bottom:24px">
        <h3>开题报告</h3>
        <el-tag v-if="proposal?.status !== undefined" :type="statusColors[proposal.status]">
          {{ statusLabels[proposal.status] }}
        </el-tag>
        <span style="color:#909399;font-size:13px">{{ selectionStore.topicTitle() }}</span>
      </div>

      <!-- 状态说明 -->
      <el-alert v-if="proposal?.status === 1" type="info" show-icon :closable="false" style="margin-bottom:16px"
        title="开题报告正在等待导师审核，审核期间不可修改" />
      <el-alert v-if="proposal?.status === 2" type="success" show-icon :closable="false" style="margin-bottom:16px"
        title="开题报告已通过导师审核，等待院系评审" />
      <el-alert v-if="proposal?.status === 3" type="success" show-icon :closable="false" style="margin-bottom:16px"
        title="开题报告已通过院系评审，请继续推进论文工作" />
      <el-alert v-if="proposal?.status === 4" :title="'退回原因：' + (proposal.rejectReason || '请联系导师了解详情')"
        type="error" show-icon :closable="false" style="margin-bottom:16px" />
      <el-alert v-if="proposal?.teacherComment && proposal?.status === 4"
        :title="'导师意见：' + proposal.teacherComment"
        type="warning" show-icon :closable="false" style="margin-bottom:16px" />

      <!--
        状态禁用规则（与后端一致）：
          0 草稿      → 可编辑
          1 待导师审核 → 禁止编辑（审核中）
          2 待评审    → 禁止编辑（审核中）
          3 已通过    → 禁止编辑
          4 已退回    → 可编辑（修改后重新提交）
      -->
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px"
        :disabled="proposal?.status === 1 || proposal?.status === 2 || proposal?.status === 3">
        <el-divider>研究基础</el-divider>
        <el-form-item label="研究背景与意义" prop="background">
          <el-input v-model="form.background" type="textarea" :rows="5" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="文献综述" prop="literature">
          <el-input v-model="form.literature" type="textarea" :rows="6" maxlength="3000" show-word-limit />
        </el-form-item>

        <el-divider>研究方案</el-divider>
        <el-form-item label="研究内容与方法" prop="method">
          <el-input v-model="form.method" type="textarea" :rows="5" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="技术路线/进度安排" prop="plan">
          <el-input v-model="form.plan" type="textarea" :rows="4" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item label="预期成果" prop="expectedResult">
          <el-input v-model="form.expectedResult" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>

        <el-divider>附件上传</el-divider>
        <el-form-item label="开题报告文件">
          <el-upload action="/api/files/upload" :headers="uploadHeaders"
            :on-success="(res, file) => onFileSuccess('fileUrl', res, file)"
            :on-error="() => ElMessage.error('文件上传失败，请重试')"
            :file-list="fileList" accept=".pdf,.doc,.docx" :limit="1"
            :disabled="isFormDisabled">
            <el-button :disabled="isFormDisabled">上传文件</el-button>
            <template #tip><div class="el-upload__tip">支持 PDF/Word，不超过 20MB</div></template>
          </el-upload>
        </el-form-item>
        <el-form-item label="外文翻译文件">
          <el-upload action="/api/files/upload" :headers="uploadHeaders"
            :on-success="(res, file) => onFileSuccess('translationUrl', res, file)"
            :on-error="() => ElMessage.error('文件上传失败，请重试')"
            :file-list="translationFileList" accept=".pdf,.doc,.docx" :limit="1"
            :disabled="isFormDisabled">
            <el-button :disabled="isFormDisabled">上传文件</el-button>
            <template #tip><div class="el-upload__tip">外文原文及译文（≥3000汉字）</div></template>
          </el-upload>
        </el-form-item>

        <!-- 操作按钮：只有草稿(0)或已退回(4)可操作 -->
        <el-form-item v-if="canEdit">
          <el-button @click="saveDraft" :loading="saving">保存草稿</el-button>
          <el-button type="primary" @click="doSubmit" :loading="submitting">提交开题报告</el-button>
        </el-form-item>
      </el-form>

      <!-- 审核意见展示（审核中或已通过时展示） -->
      <template v-if="proposal?.teacherComment && proposal.status >= 2">
        <el-divider>审核意见</el-divider>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="导师意见">{{ proposal.teacherComment }}</el-descriptions-item>
          <el-descriptions-item v-if="proposal.reviewComment" label="评审组意见">
            {{ proposal.reviewComment }}
          </el-descriptions-item>
          <el-descriptions-item v-if="proposal.reviewScore" label="开题评分">
            <el-rate :model-value="proposal.reviewScore / 20" disabled />（{{ proposal.reviewScore }} 分）
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { proposalApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { useSelectionStore } from '@/stores/selection'

const auth = useAuthStore()
const selectionStore = useSelectionStore()
const formRef = ref()
const saving = ref(false)
const submitting = ref(false)
const proposal = ref(null)
const fileList = ref([])
const translationFileList = ref([])

// 从 store 动态获取 selectionId，不再硬编码
const selectionId = computed(() => selectionStore.selectionId())

const uploadHeaders = computed(() => ({ Authorization: `Bearer ${auth.token}` }))

// 可编辑状态：草稿(0) 或 已退回(4)
const canEdit = computed(() =>
  proposal.value === null || proposal.value.status === 0 || proposal.value.status === 4
)
const isFormDisabled = computed(() => !canEdit.value)

const form = ref({
  background: '', literature: '', method: '', plan: '',
  expectedResult: '', fileUrl: '', translationUrl: ''
})

const rules = {
  background: [{ required: true, message: '请填写研究背景与意义' }],
  literature:  [{ required: true, message: '请填写文献综述' }],
  method:      [{ required: true, message: '请填写研究内容与方法' }],
}

const statusLabels = { 0: '草稿', 1: '待导师审核', 2: '导师已通过', 3: '评审已通过', 4: '已退回' }
const statusColors  = { 0: 'info', 1: 'warning', 2: '', 3: 'success', 4: 'danger' }

function onFileSuccess(field, res, file) {
  if (res.code === 200) {
    form.value[field] = res.data.url
    // 更新对应文件列表，让 el-upload 展示已上传的文件名和链接
    const fileObj = { name: file.name, url: res.data.url }
    if (field === 'fileUrl') fileList.value = [fileObj]
    else translationFileList.value = [fileObj]
    ElMessage.success('文件上传成功')
  } else {
    ElMessage.error(res.message || '文件上传失败，请重试')
  }
}

async function saveDraft() {
  if (!selectionId.value) return ElMessage.error('选题信息未加载')
  saving.value = true
  try {
    const res = await proposalApi.save({ ...form.value, selectionId: selectionId.value })
    proposal.value = res.data
    ElMessage.success('草稿已保存')
  } finally {
    saving.value = false
  }
}

async function doSubmit() {
  if (!selectionId.value) return ElMessage.error('选题信息未加载')
  await formRef.value.validate()
  await ElMessageBox.confirm('提交后导师将收到审核通知，提交中将无法修改，确认提交？', '提交确认', { type: 'info' })
  submitting.value = true
  try {
    const res = await proposalApi.submit({ ...form.value, selectionId: selectionId.value })
    proposal.value = res.data
    ElMessage.success('开题报告已提交，等待导师审核')
  } finally {
    submitting.value = false
  }
}

async function load() {
  const currentYear = new Date().getFullYear()
  // 确保 selection store 已初始化
  if (!selectionStore.selection) {
    await selectionStore.fetchMySelection(currentYear)
  }
  if (!selectionId.value) return

  const res = await proposalApi.getBySelection(selectionId.value)
  if (res.data) {
    proposal.value = res.data
    Object.assign(form.value, res.data)
    // 回显已上传文件到 el-upload 文件列表
    if (res.data.fileUrl) {
      const name = res.data.fileUrl.split('/').pop() || '开题报告文件'
      fileList.value = [{ name, url: res.data.fileUrl }]
    }
    if (res.data.translationUrl) {
      const name = res.data.translationUrl.split('/').pop() || '外文翻译文件'
      translationFileList.value = [{ name, url: res.data.translationUrl }]
    }
  }
}

onMounted(load)
</script>
