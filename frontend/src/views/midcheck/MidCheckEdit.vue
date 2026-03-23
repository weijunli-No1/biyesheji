<template>
  <div class="page-container">
    <!-- 前置条件：选题未确认 -->
    <div v-if="!selectionStore.isConfirmed()" class="page-card">
      <el-result icon="warning" title="选题未确认" sub-title="请先完成选题，并等待导师确认后再提交中期检查">
        <template #extra>
          <el-button type="primary" @click="$router.push('/topics')">去选题</el-button>
        </template>
      </el-result>
    </div>

    <!-- 前置条件：开题报告未通过 -->
    <div v-else-if="proposalStatus !== 3" class="page-card">
      <el-result icon="warning" title="开题报告未通过"
        :sub-title="proposalStatus === null ? '请先提交并通过开题报告，再进行中期检查' : '开题报告尚未通过，通过后方可提交中期检查'">
        <template #extra>
          <el-button type="primary" @click="$router.push('/proposal')">查看开题报告</el-button>
        </template>
      </el-result>
    </div>

    <div v-else class="page-card" style="max-width:800px">
      <div style="display:flex;align-items:center;gap:16px;margin-bottom:24px">
        <h3 class="page-title">中期检查</h3>
        <el-tag v-if="check" :type="midCheckStatusColors[check.status]">
          {{ midCheckStatusLabels[check.status] }}
        </el-tag>
      </div>

      <!-- 状态说明 -->
      <el-alert v-if="check?.status === 3" type="warning" show-icon :closable="false" style="margin-bottom:16px"
        :title="'导师退回原因：' + (check.teacherComment || '请按导师意见修改后重新提交')" />
      <el-alert v-if="check?.status === 1" type="info" show-icon :closable="false" style="margin-bottom:16px"
        title="中期检查已提交，正在等待导师审核，审核期间不可修改" />
      <el-alert v-if="check?.status === 2" type="success" show-icon :closable="false" style="margin-bottom:16px"
        title="中期检查已通过，请按计划继续推进论文撰写工作" />

      <el-form ref="formRef" :model="form" :rules="rules" label-width="130px"
        :disabled="check?.status === 1 || check?.status === 2">
        <el-form-item label="已完成工作" prop="completedWork">
          <el-input v-model="form.completedWork" type="textarea" :rows="5"
            placeholder="描述截至目前已完成的工作内容，包括调研、设计、实现等各环节" />
        </el-form-item>
        <el-form-item label="存在问题" prop="problems">
          <el-input v-model="form.problems" type="textarea" :rows="4"
            placeholder="目前遇到的困难和问题（没有可填'无'）" />
        </el-form-item>
        <el-form-item label="下阶段计划" prop="nextPlan">
          <el-input v-model="form.nextPlan" type="textarea" :rows="4"
            placeholder="后续工作计划和时间安排，尽量细化到周" />
        </el-form-item>

        <el-form-item v-if="canEdit">
          <el-button @click="saveDraft" :loading="saving">保存草稿</el-button>
          <el-button type="primary" @click="doSubmit" :loading="submitting">提交中期检查</el-button>
        </el-form-item>
      </el-form>

      <!-- 导师评价（已通过后展示） -->
      <template v-if="check?.status === 2">
        <el-divider>导师评价</el-divider>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="进度评估">
            <el-tag :type="progressColors[check.progress]">{{ progressLabels[check.progress] }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="导师意见">{{ check.teacherComment || '—' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { proposalApi, midCheckApi } from '@/api'
import { useSelectionStore } from '@/stores/selection'
import {
  midCheckStatusLabels, midCheckStatusColors,
  progressLabels, progressColors
} from '@/utils/statusConfig'

const selectionStore = useSelectionStore()
const formRef = ref()
const saving = ref(false)
const submitting = ref(false)
const check = ref(null)
const proposalStatus = ref(null)

const selectionId = computed(() => selectionStore.selectionId())
const canEdit = computed(() => !check.value || check.value.status === 0 || check.value.status === 3)

const form = ref({ completedWork: '', problems: '', nextPlan: '' })

const rules = {
  completedWork: [{ required: true, message: '请填写已完成工作', trigger: 'blur' }],
  nextPlan:      [{ required: true, message: '请填写下阶段计划', trigger: 'blur' }],
}

async function saveDraft() {
  if (!selectionId.value) return ElMessage.error('选题信息未加载')
  saving.value = true
  try {
    const res = await midCheckApi.save({ ...form.value, selectionId: selectionId.value })
    check.value = res.data
    ElMessage.success('草稿已保存')
  } finally {
    saving.value = false
  }
}

async function doSubmit() {
  if (!selectionId.value) return ElMessage.error('选题信息未加载')
  await formRef.value.validate()
  await ElMessageBox.confirm(
    '提交后导师将收到审核通知，审核期间不可修改。确认提交？',
    '提交中期检查',
    { type: 'info', confirmButtonText: '确认提交' }
  )
  submitting.value = true
  try {
    const res = await midCheckApi.submit({ ...form.value, selectionId: selectionId.value })
    check.value = res.data
    ElMessage.success('中期检查已提交，等待导师审核')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  const currentYear = new Date().getFullYear()
  if (!selectionStore.selection) {
    await selectionStore.fetchMySelection(currentYear)
  }
  if (selectionId.value) {
    const [proposalRes, checkRes] = await Promise.all([
      proposalApi.getBySelection(selectionId.value),
      midCheckApi.getBySelection(selectionId.value).catch(() => ({ data: null }))
    ])
    proposalStatus.value = proposalRes.data?.status ?? null
    if (checkRes.data) {
      check.value = checkRes.data
      Object.assign(form.value, checkRes.data)
    }
  }
})
</script>
