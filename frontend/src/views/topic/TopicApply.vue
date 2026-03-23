<template>
  <div class="page-container">
    <div class="page-card" style="max-width:800px">
      <div style="display:flex;align-items:center;gap:12px;margin-bottom:24px">
        <el-button text @click="$router.back()" aria-label="返回上一页">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <h3 class="page-title">申报课题</h3>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="课题名称" prop="title">
          <el-input v-model="form.title" maxlength="100" show-word-limit
            placeholder="请填写完整的课题名称" />
        </el-form-item>
        <el-form-item label="课题类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :label="1">设计类</el-radio>
            <el-radio :label="2">论文类</el-radio>
            <el-radio :label="3">软件类</el-radio>
            <el-radio :label="4">实验类</el-radio>
          </el-radio-group>
          <div class="text-secondary" style="margin-top:4px;font-size:12px">
            设计类：工程设计方案；论文类：理论研究；软件类：系统/应用开发；实验类：实验验证研究
          </div>
        </el-form-item>
        <el-form-item label="招收人数" prop="maxStudents">
          <el-input-number v-model="form.maxStudents" :min="1" :max="3" />
          <span class="text-secondary" style="margin-left:8px">人（上限 3 人）</span>
        </el-form-item>
        <el-form-item label="课题简介" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4"
            maxlength="500" show-word-limit
            placeholder="简要描述课题背景、研究内容和预期目标（建议 100 字以上）" />
        </el-form-item>
        <el-form-item label="选题要求">
          <el-input v-model="form.requirement" type="textarea" :rows="3"
            maxlength="500" show-word-limit
            placeholder="对有意选择本课题的学生的知识背景和能力要求（可选）" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submit">提交申报</el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { topicApi } from '@/api'

const router = useRouter()
const formRef = ref()
const submitting = ref(false)

const form = ref({
  title: '', type: 3, maxStudents: 1,
  description: '', requirement: '',
  year: new Date().getFullYear()
})

const rules = {
  title:       [{ required: true, message: '请填写课题名称', trigger: 'blur' }],
  type:        [{ required: true, message: '请选择课题类型', trigger: 'change' }],
  description: [{ required: true, message: '请填写课题简介', trigger: 'blur' }],
}

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    await topicApi.apply(form.value)
    ElMessage.success('申报成功，等待院系审批（通常 1-3 个工作日内完成）')
    router.push('/topics/my')
  } finally {
    submitting.value = false
  }
}
</script>
