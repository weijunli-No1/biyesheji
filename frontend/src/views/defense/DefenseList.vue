<template>
  <div class="page-container">
    <el-row :gutter="16">
      <!-- 答辩小组 -->
      <el-col :xs="24" :sm="8">
        <div class="page-card">
          <div class="toolbar">
            <b class="section-title">答辩小组</b>
            <el-button v-if="auth.isManager" size="small" type="primary" @click="openGroupDialog">
              新建小组
            </el-button>
          </div>
          <el-table :data="groups" size="small" stripe v-loading="groupLoading"
            :row-class-name="({ row }) => row.id === activeGroup?.id ? 'active-row' : ''">
            <el-table-column prop="name" label="小组名称" />
            <el-table-column prop="location" label="地点" width="80" />
            <el-table-column label="时间" width="90">
              <template #default="{ row }">
                {{ row.defenseTime ? dayjs(row.defenseTime).format('MM-DD HH:mm') : '—' }}
              </template>
            </el-table-column>
            <el-table-column label="" width="50">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="selectGroup(row)">选</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!groupLoading && !groups.length" description="暂无答辩小组" :image-size="60" />
        </div>
      </el-col>

      <!-- 答辩学生列表 -->
      <el-col :xs="24" :sm="16">
        <div class="page-card">
          <div class="toolbar">
            <div style="display:flex;align-items:center;gap:8px">
              <b class="section-title">答辩学生列表</b>
              <el-tag v-if="activeGroup" type="primary" size="small">{{ activeGroup.name }}</el-tag>
            </div>
            <div style="display:flex;gap:8px">
              <el-button v-if="auth.isManager && activeGroup" size="small" type="success"
                @click="openAssignDialog">分配学生</el-button>
              <el-button v-if="auth.isManager && activeGroup" size="small" @click="exportDefense">导出记录</el-button>
            </div>
          </div>

          <div class="table-scroll">
            <el-table :data="students" stripe v-loading="studentLoading">
              <el-table-column prop="studentNo" label="学号" width="115" />
              <el-table-column prop="studentName" label="姓名" width="75" />
              <el-table-column prop="topicTitle" label="课题" min-width="160" show-overflow-tooltip />
              <el-table-column label="答辩结果" width="110">
                <template #default="{ row }">
                  <el-tag v-if="row.result" :type="defenseResultColors[row.result]" size="small">
                    {{ defenseResultLabels[row.result] }}
                  </el-tag>
                  <el-tag v-else type="info" size="small">待答辩</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="总分" width="60">
                <template #default="{ row }">
                  {{ row.totalScore ?? '—' }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80" v-if="canEnterScore">
                <template #default="{ row }">
                  <el-button link type="primary" @click="recordDefense(row)">录入</el-button>
                </template>
              </el-table-column>

              <template #empty>
                <el-empty
                  :description="activeGroup ? '该小组暂无答辩学生，请分配' : '请先选择答辩小组'"
                  :image-size="60">
                  <el-button v-if="activeGroup && auth.isManager" type="primary" size="small"
                    @click="openAssignDialog">立即分配</el-button>
                </el-empty>
              </template>
            </el-table>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 新建小组对话框 -->
    <el-dialog v-model="groupDialogVisible" title="新建答辩小组" :width="dialogWidth">
      <el-form :model="groupForm" label-width="90px" :rules="groupRules" ref="groupFormRef">
        <el-form-item label="小组名称" prop="name">
          <el-input v-model="groupForm.name" placeholder="如：第一答辩组" />
        </el-form-item>
        <el-form-item label="答辩地点" prop="location">
          <el-input v-model="groupForm.location" placeholder="如：A101教室" />
        </el-form-item>
        <el-form-item label="答辩时间">
          <el-date-picker v-model="groupForm.defenseTime" type="datetime"
            placeholder="选择答辩时间" format="MM-DD HH:mm" value-format="YYYY-MM-DDTHH:mm:ss"
            style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="groupDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitGroup" :loading="groupSaving">创建</el-button>
      </template>
    </el-dialog>

    <!-- 分配学生对话框 -->
    <el-dialog v-model="assignDialogVisible" title="分配学生到答辩组" :width="dialogWidth">
      <div style="margin-bottom:12px;color:var(--color-text-secondary);font-size:13px">
        以下学生已确认选题且尚未分配答辩组：
      </div>
      <el-table :data="unassigned" v-loading="unassignedLoading" size="small" stripe
        @selection-change="selectedRows = $event">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="studentNo" label="学号" width="115" />
        <el-table-column prop="studentName" label="姓名" width="75" />
        <el-table-column prop="topicTitle" label="课题" show-overflow-tooltip />
      </el-table>
      <el-empty v-if="!unassignedLoading && !unassigned.length"
        description="所有学生均已分配答辩组" :image-size="60" />
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssign" :loading="assigning"
          :disabled="!selectedRows.length">
          分配 {{ selectedRows.length ? `(${selectedRows.length}人)` : '' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 录入答辩成绩对话框 -->
    <el-dialog v-model="scoreDialogVisible" title="录入答辩成绩" :width="dialogWidth">
      <el-form v-if="selectedStudent" :model="scoreForm" label-width="90px">
        <el-form-item label="学生">
          <b>{{ selectedStudent.studentName }}</b>
          <el-tag size="small" style="margin-left:8px">{{ selectedStudent.studentNo }}</el-tag>
        </el-form-item>
        <el-form-item label="课题">
          <span class="text-secondary" style="font-size:13px">{{ selectedStudent.topicTitle }}</span>
        </el-form-item>
        <el-form-item label="汇报评分">
          <el-slider v-model="scoreForm.presentScore" :min="0" :max="100" show-input />
        </el-form-item>
        <el-form-item label="问答评分">
          <el-slider v-model="scoreForm.qaScore" :min="0" :max="100" show-input />
        </el-form-item>
        <el-form-item label="答辩结果">
          <el-radio-group v-model="scoreForm.result">
            <el-radio :label="1">通过</el-radio>
            <el-radio :label="2">修改后通过</el-radio>
            <el-radio :label="3">不通过</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="评委提问">
          <el-input v-model="scoreForm.questions" type="textarea" :rows="2"
            placeholder="记录评委提出的问题（选填）" />
        </el-form-item>
        <el-form-item label="答辩意见">
          <el-input v-model="scoreForm.comment" type="textarea" :rows="3"
            placeholder="请填写对答辩表现的综合评价" />
        </el-form-item>
        <el-form-item label="秘书记录">
          <el-input v-model="scoreForm.secretaryNote" type="textarea" :rows="2"
            placeholder="秘书备注（选填）" />
        </el-form-item>
        <el-alert v-if="scoreForm.result" type="info" :closable="false" style="margin-top:4px">
          答辩总分：{{ Math.round((scoreForm.presentScore + scoreForm.qaScore) / 2) }} 分
          （汇报 {{ scoreForm.presentScore }} + 问答 {{ scoreForm.qaScore }}）÷ 2
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="scoreDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDefenseScore" :loading="saving">保存成绩</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { useAuthStore } from '@/stores/auth'
import { defenseApi } from '@/api'
import { defenseResultLabels, defenseResultColors } from '@/utils/statusConfig'

const auth = useAuthStore()
const canEnterScore = computed(() => auth.role === 4 || auth.isManager)
const dialogWidth = computed(() => window.innerWidth <= 640 ? '92%' : '560px')

// ---- 小组数据 ----
const groups = ref([])
const groupLoading = ref(false)
const activeGroup = ref(null)

async function loadGroups() {
  groupLoading.value = true
  try {
    const res = await defenseApi.listGroups(2026)
    groups.value = res.data
  } finally {
    groupLoading.value = false
  }
}

function selectGroup(g) {
  activeGroup.value = g
  loadStudents(g.id)
}

// ---- 小组学生 ----
const students = ref([])
const studentLoading = ref(false)

async function loadStudents(groupId) {
  studentLoading.value = true
  try {
    const res = await defenseApi.listGroupStudents(groupId)
    students.value = res.data
  } finally {
    studentLoading.value = false
  }
}

// ---- 新建小组 ----
const groupDialogVisible = ref(false)
const groupSaving = ref(false)
const groupFormRef = ref(null)
const groupForm = ref({ name: '', location: '', defenseTime: null })
const groupRules = {
  name: [{ required: true, message: '请输入小组名称', trigger: 'blur' }],
  location: [{ required: true, message: '请输入答辩地点', trigger: 'blur' }],
}

function openGroupDialog() {
  groupForm.value = { name: '', location: '', defenseTime: null }
  groupDialogVisible.value = true
}

async function submitGroup() {
  await groupFormRef.value.validate()
  groupSaving.value = true
  try {
    await defenseApi.createGroup(groupForm.value)
    ElMessage.success('答辩小组已创建')
    groupDialogVisible.value = false
    loadGroups()
  } finally {
    groupSaving.value = false
  }
}

// ---- 分配学生 ----
const assignDialogVisible = ref(false)
const unassigned = ref([])
const unassignedLoading = ref(false)
const selectedRows = ref([])
const assigning = ref(false)

async function openAssignDialog() {
  selectedRows.value = []
  assignDialogVisible.value = true
  unassignedLoading.value = true
  try {
    const res = await defenseApi.unassignedStudents(2026)
    unassigned.value = res.data
  } finally {
    unassignedLoading.value = false
  }
}

async function submitAssign() {
  if (!selectedRows.value.length) return
  assigning.value = true
  try {
    for (const row of selectedRows.value) {
      await defenseApi.assignStudent(activeGroup.value.id, row.selectionId)
    }
    ElMessage.success(`已将 ${selectedRows.value.length} 名学生分配到 ${activeGroup.value.name}`)
    assignDialogVisible.value = false
    loadStudents(activeGroup.value.id)
  } finally {
    assigning.value = false
  }
}

// ---- 录入答辩成绩 ----
const scoreDialogVisible = ref(false)
const selectedStudent = ref(null)
const saving = ref(false)
const scoreForm = ref({ presentScore: 80, qaScore: 80, result: 1, questions: '', comment: '', secretaryNote: '' })

function recordDefense(row) {
  selectedStudent.value = row
  scoreForm.value = {
    presentScore: row.presentScore ?? 80,
    qaScore: row.qaScore ?? 80,
    result: row.result ?? 1,
    questions: row.questions ?? '',
    comment: row.comment ?? '',
    secretaryNote: row.secretaryNote ?? '',
  }
  scoreDialogVisible.value = true
}

async function saveDefenseScore() {
  const defenseScore = Math.round((scoreForm.value.presentScore + scoreForm.value.qaScore) / 2)
  await ElMessageBox.confirm(
    `答辩总分 ${defenseScore} 分，结果：${defenseResultLabels[scoreForm.value.result]}，确认保存？`,
    '确认成绩', { type: 'info', confirmButtonText: '确认保存' }
  )
  saving.value = true
  try {
    await defenseApi.saveRecord({
      selectionId: selectedStudent.value.selectionId,
      ...scoreForm.value,
    })
    ElMessage.success(`答辩成绩已录入（${defenseScore} 分）`)
    scoreDialogVisible.value = false
    loadStudents(activeGroup.value.id)
  } finally {
    saving.value = false
  }
}

// ---- 导出 ----
function exportDefense() {
  if (!students.value.length) {
    ElMessage.warning('当前没有可导出的答辩记录')
    return
  }
  const groupName = activeGroup.value ? activeGroup.value.name : '全部'
  const rows = [
    ['学号', '姓名', '课题', '汇报分', '问答分', '总分', '答辩结果', '评委意见'],
    ...students.value.map(s => [
      s.studentNo,
      s.studentName,
      s.topicTitle,
      s.presentScore ?? '',
      s.qaScore ?? '',
      s.totalScore ?? '',
      s.result ? defenseResultLabels[s.result] : '待答辩',
      s.comment ?? ''
    ])
  ]
  const csv = rows.map(r => r.map(cell => `"${String(cell).replace(/"/g, '""')}"`).join(',')).join('\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `答辩记录_${groupName}_${dayjs().format('YYYY-MM-DD')}.csv`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('导出成功')
}

onMounted(loadGroups)
</script>

<style scoped>
:deep(.active-row td) {
  background-color: var(--el-color-primary-light-9) !important;
}
</style>
