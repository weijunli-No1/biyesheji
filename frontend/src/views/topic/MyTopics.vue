<template>
  <div class="page-container">
    <div class="page-card">
      <div class="toolbar">
        <span class="section-title">我的课题</span>
        <el-button type="primary" @click="$router.push('/topics/apply')">
          <el-icon><Plus /></el-icon> 申报新课题
        </el-button>
      </div>

      <div class="table-scroll">
        <el-table :data="list" v-loading="loading" stripe>
          <el-table-column prop="title" label="课题名称" min-width="200" show-overflow-tooltip />
          <el-table-column label="类型" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="topicTypeColors[row.type]">{{ topicTypeLabels[row.type] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="名额" width="90" align="center">
            <template #default="{ row }">
              <el-tag v-if="(row.selectedCount || 0) >= row.maxStudents" type="danger" size="small" effect="dark">已满</el-tag>
              <span v-else>{{ row.selectedCount || 0 }}/{{ row.maxStudents }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="topicStatusColors[row.status]">{{ topicStatusLabels[row.status] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="rejectReason" label="驳回原因" show-overflow-tooltip />
          <el-table-column label="申报时间" width="160">
            <template #default="{ row }">
              {{ row.createTime ? dayjs(row.createTime).format('YYYY-MM-DD HH:mm') : '—' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="$router.push(`/topics/${row.id}`)">详情</el-button>
              <!-- 待审批或已驳回才可编辑 -->
              <el-button v-if="canEdit(row)" link type="warning" @click="openEdit(row)">编辑</el-button>
              <!-- 待审批或已驳回且无学生选题才可删除 -->
              <el-popconfirm v-if="canDelete(row)" title="确认删除该课题？" @confirm="doDelete(row)">
                <template #reference>
                  <el-button link type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty description="您还没有申报任何课题">
              <el-button type="primary" @click="$router.push('/topics/apply')">立即申报</el-button>
            </el-empty>
          </template>
        </el-table>
      </div>
    </div>

    <!-- 编辑课题 dialog -->
    <el-dialog v-model="editVisible" title="编辑课题" width="600px">
      <el-form ref="editFormRef" :model="editForm" :rules="rules" label-width="100px">
        <el-form-item label="课题名称" prop="title">
          <el-input v-model="editForm.title" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="课题类型" prop="type">
          <el-radio-group v-model="editForm.type">
            <el-radio :label="1">设计类</el-radio>
            <el-radio :label="2">论文类</el-radio>
            <el-radio :label="3">软件类</el-radio>
            <el-radio :label="4">实验类</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="招收人数" prop="maxStudents">
          <el-input-number v-model="editForm.maxStudents" :min="1" :max="3" />
        </el-form-item>
        <el-form-item label="课题简介" prop="description">
          <el-input v-model="editForm.description" type="textarea" :rows="4"
            maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="选题要求">
          <el-input v-model="editForm.requirement" type="textarea" :rows="3"
            maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doSave">保存并重新提交审批</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { topicApi } from '@/api'
import {
  topicTypeLabels, topicTypeColors,
  topicStatusLabels, topicStatusColors
} from '@/utils/statusConfig'

const loading = ref(false)
const saving  = ref(false)
const list    = ref([])
const editVisible = ref(false)
const editFormRef = ref()
const editForm = reactive({ id: null, title: '', type: 3, maxStudents: 1, description: '', requirement: '' })

const rules = {
  title:       [{ required: true, message: '请填写课题名称', trigger: 'blur' }],
  type:        [{ required: true, message: '请选择课题类型', trigger: 'change' }],
  description: [{ required: true, message: '请填写课题简介', trigger: 'blur' }],
}

// 待审批(0)或已驳回(3)可编辑
const canEdit   = (row) => row.status === 0 || row.status === 3
// 待审批(0)或已驳回(3)且没有已确认的学生可删除
const canDelete = (row) => (row.status === 0 || row.status === 3) && !(row.selectedCount > 0)

async function load() {
  loading.value = true
  try {
    const res = await topicApi.myTopics()
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

function openEdit(row) {
  Object.assign(editForm, {
    id: row.id,
    title: row.title,
    type: row.type,
    maxStudents: row.maxStudents,
    description: row.description || '',
    requirement: row.requirement || '',
  })
  editVisible.value = true
}

async function doSave() {
  await editFormRef.value.validate()
  saving.value = true
  try {
    await topicApi.update(editForm.id, editForm)
    ElMessage.success('已保存，等待重新审批')
    editVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function doDelete(row) {
  await topicApi.delete(row.id)
  ElMessage.success('课题已删除')
  load()
}

onMounted(load)
</script>
