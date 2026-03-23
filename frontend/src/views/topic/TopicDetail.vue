<template>
  <div class="page-container">
    <div class="page-card" style="max-width:800px" v-loading="loading">
      <div style="display:flex;align-items:center;gap:12px;margin-bottom:24px">
        <el-button text @click="$router.back()" aria-label="返回上一页">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <h3 class="page-title">课题详情</h3>
        <el-tag v-if="topic" :type="topicStatusColors[topic.status]">
          {{ topicStatusLabels[topic.status] }}
        </el-tag>
      </div>

      <el-empty v-if="!loading && !topic" description="课题不存在或已被删除">
        <el-button @click="$router.push('/topics')">返回课题列表</el-button>
      </el-empty>

      <el-descriptions v-else-if="topic" :column="2" border>
        <el-descriptions-item label="课题名称" :span="2">{{ topic.title }}</el-descriptions-item>
        <el-descriptions-item label="课题类型">
          <el-tag :type="topicTypeColors[topic.type]">{{ topicTypeLabels[topic.type] }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="招收名额">
          {{ topic.selectedCount || 0 }}/{{ topic.maxStudents }} 人
          <el-tag v-if="(topic.selectedCount || 0) >= topic.maxStudents"
            type="danger" size="small" style="margin-left:8px">已满</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="指导教师">{{ topic.teacherName }}</el-descriptions-item>
        <el-descriptions-item label="所属院系">{{ topic.department }}</el-descriptions-item>
        <el-descriptions-item label="课题简介" :span="2">{{ topic.description || '—' }}</el-descriptions-item>
        <el-descriptions-item label="选题要求" :span="2">{{ topic.requirement || '无特殊要求' }}</el-descriptions-item>
        <el-descriptions-item v-if="topic.rejectReason" label="驳回原因" :span="2">
          <span style="color:var(--color-danger)">{{ topic.rejectReason }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { topicApi } from '@/api'
import {
  topicTypeLabels, topicTypeColors,
  topicStatusLabels, topicStatusColors
} from '@/utils/statusConfig'

const route = useRoute()
const loading = ref(false)
const topic = ref(null)

async function load() {
  loading.value = true
  try {
    const res = await topicApi.get(route.params.id)
    topic.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
