<template>
  <div class="notification-page">
    <div class="page-header">
      <h2>消息通知</h2>
      <el-button v-if="notifStore.unreadCount > 0" @click="markAll" :loading="marking">
        全部标记已读 ({{ notifStore.unreadCount }})
      </el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column width="12">
        <template #default="{ row }">
          <div v-if="!row.isRead" class="unread-dot" />
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" width="200">
        <template #default="{ row }">
          <span :class="{ 'unread-title': !row.isRead }">{{ row.title }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="content" label="内容" min-width="300" show-overflow-tooltip />
      <el-table-column prop="type" label="类型" width="140">
        <template #default="{ row }">
          <el-tag :type="typeTagMap[row.type] || 'info'" size="small">
            {{ typeNameMap[row.type] || row.type }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="时间" width="160">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button v-if="!row.isRead" text size="small" @click="markOne(row)">标为已读</el-button>
          <el-button v-if="row.relatedUrl" text size="small" type="primary" @click="go(row)">
            前往查看
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { notificationApi } from '@/api/index.js'
import { useNotificationStore } from '@/stores/notification'
import dayjs from 'dayjs'

const router = useRouter()
const notifStore = useNotificationStore()
const list = ref([])
const loading = ref(false)
const marking = ref(false)
const total = ref(0)
const query = reactive({ page: 1, size: 20 })

const typeNameMap = {
  TOPIC_APPROVED: '课题通过', TOPIC_REJECTED: '课题驳回',
  SELECTION_APPLIED: '选题申请', SELECTION_CONFIRMED: '选题确认', SELECTION_REJECTED: '选题拒绝',
  PROPOSAL_SUBMITTED: '开题提交', PROPOSAL_TEACHER_PASSED: '导师通过', PROPOSAL_TEACHER_REJECTED: '导师退回',
  PROPOSAL_DEPT_PASSED: '评审通过', PROPOSAL_DEPT_REJECTED: '评审退回',
  MIDCHECK_SUBMITTED: '中检提交', MIDCHECK_PASSED: '中检通过', MIDCHECK_REJECTED: '中检退回',
  THESIS_SUBMITTED: '论文提交', THESIS_PASSED: '论文通过', THESIS_REJECTED: '论文退回',
  THESIS_ANNOTATED: '论文批注', DEFENSE_ASSIGNED: '答辩安排', SCORE_RECORDED: '成绩录入',
}
const typeTagMap = {
  TOPIC_APPROVED: 'success', TOPIC_REJECTED: 'danger',
  SELECTION_CONFIRMED: 'success', SELECTION_REJECTED: 'danger',
  PROPOSAL_DEPT_PASSED: 'success', THESIS_PASSED: 'success',
  MIDCHECK_PASSED: 'success',
}

const formatTime = (t) => t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-'

async function load() {
  loading.value = true
  try {
    const res = await notificationApi.list(query)
    const d = res.data
    list.value = d?.records ?? []
    total.value = d?.total ?? 0
  } finally {
    loading.value = false
  }
}

async function markOne(row) {
  await notificationApi.markRead(row.id)
  row.isRead = true
  notifStore.decrement()
}

async function markAll() {
  marking.value = true
  try {
    await notificationApi.markAllRead()
    list.value.forEach(n => (n.isRead = true))
    notifStore.clearAll()
  } finally {
    marking.value = false
  }
}

function go(row) {
  router.push(row.relatedUrl)
}

onMounted(load)
</script>

<style scoped>
.notification-page { padding: 20px; }
.page-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px;
}
.page-header h2 { margin: 0; font-size: 18px; }
.unread-dot {
  width: 8px; height: 8px; border-radius: 50%;
  background: #1a6af0; margin: 0 auto;
}
.unread-title { font-weight: 600; color: #303133; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
