<template>
  <div class="notification-bell">
    <el-popover
      placement="bottom-end"
      :width="360"
      trigger="click"
      popper-class="notification-popover"
      @show="loadRecent"
    >
      <template #reference>
        <el-badge :value="notifStore.unreadCount > 0 ? notifStore.unreadCount : ''" :max="99"
                  :hidden="notifStore.unreadCount === 0">
          <el-button circle size="default" text style="font-size:20px;">
            <el-icon><Bell /></el-icon>
          </el-button>
        </el-badge>
      </template>

      <div class="notif-header">
        <span class="notif-title">消息通知</span>
        <el-button v-if="notifStore.unreadCount > 0" text size="small" @click="markAll">全部已读</el-button>
      </div>

      <el-scrollbar max-height="400px">
        <div v-if="loading" class="notif-loading">
          <el-icon class="is-loading"><Loading /></el-icon>
        </div>
        <div v-else-if="recentList.length === 0" class="notif-empty">
          暂无未读通知
        </div>
        <div
          v-for="n in recentList"
          :key="n.id"
          class="notif-item"
          :class="{ unread: !n.isRead }"
          @click="handleClick(n)"
        >
          <div class="notif-dot" v-if="!n.isRead" />
          <div class="notif-body">
            <div class="notif-item-title">{{ n.title }}</div>
            <div class="notif-item-content">{{ n.content }}</div>
            <div class="notif-time">{{ formatTime(n.createTime) }}</div>
          </div>
        </div>
      </el-scrollbar>

      <div class="notif-footer">
        <el-button text size="small" @click="goAll">查看全部通知</el-button>
      </div>
    </el-popover>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, Loading } from '@element-plus/icons-vue'
import { notificationApi } from '@/api/index.js'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const router = useRouter()
const auth = useAuthStore()
const notifStore = useNotificationStore()
const recentList = ref([])
const loading = ref(false)
let eventSource = null
let pollTimer = null

const formatTime = (t) => t ? dayjs(t).fromNow() : ''

async function loadRecent() {
  loading.value = true
  try {
    const res = await notificationApi.recent()
    recentList.value = res.data ?? []
  } finally {
    loading.value = false
  }
}

async function markAll() {
  try {
    await notificationApi.markAllRead()
    notifStore.clearAll()
    recentList.value.forEach(n => (n.isRead = true))
  } catch {}
}

async function handleClick(n) {
  if (!n.isRead) {
    try {
      await notificationApi.markRead(n.id)
      n.isRead = true
      notifStore.decrement()
    } catch {}
  }
  if (n.relatedUrl) {
    router.push(n.relatedUrl)
  }
}

function goAll() {
  router.push('/notifications')
}

function connectSse() {
  const token = auth.token
  if (!token) return
  eventSource = new EventSource(`/api/notifications/sse?token=${token}`)
  eventSource.addEventListener('notification', (e) => {
    try {
      const notif = JSON.parse(e.data)
      recentList.value.unshift(notif)
      if (recentList.value.length > 10) recentList.value.pop()
      notifStore.increment()
    } catch {}
  })
  eventSource.onerror = () => {
    eventSource?.close()
    setTimeout(connectSse, 30000)
  }
}

onMounted(() => {
  notifStore.fetchUnreadCount()
  connectSse()
  // 每分钟轮询一次兜底
  pollTimer = setInterval(() => notifStore.fetchUnreadCount(), 60000)
})

onUnmounted(() => {
  eventSource?.close()
  clearInterval(pollTimer)
})
</script>

<style scoped>
.notification-bell { display: inline-flex; align-items: center; }

.notif-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8px 12px; border-bottom: 1px solid #f0f0f0; font-weight: 600;
}
.notif-title { font-size: 14px; }

.notif-loading, .notif-empty {
  text-align: center; padding: 32px 0; color: #999; font-size: 13px;
}

.notif-item {
  display: flex; align-items: flex-start; gap: 8px;
  padding: 12px 16px; cursor: pointer; transition: background .15s;
}
.notif-item:hover { background: #f5f7fa; }
.notif-item.unread { background: #f0f5ff; }

.notif-dot {
  width: 8px; height: 8px; border-radius: 50%;
  background: #1a6af0; flex-shrink: 0; margin-top: 5px;
}

.notif-body { flex: 1; min-width: 0; }
.notif-item-title { font-size: 13px; font-weight: 500; color: #303133; margin-bottom: 2px; }
.notif-item-content {
  font-size: 12px; color: #606266;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.notif-time { font-size: 11px; color: #909399; margin-top: 4px; }

.notif-footer {
  text-align: center; padding: 8px 0; border-top: 1px solid #f0f0f0;
}
</style>
