import { defineStore } from 'pinia'
import { ref } from 'vue'
import { notificationApi } from '@/api/index.js'

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)

  async function fetchUnreadCount() {
    try {
      const res = await notificationApi.unreadCount()
      unreadCount.value = res.data ?? 0
    } catch {}
  }

  function decrement() {
    if (unreadCount.value > 0) unreadCount.value--
  }

  function clearAll() {
    unreadCount.value = 0
  }

  function increment() {
    unreadCount.value++
  }

  return { unreadCount, fetchUnreadCount, decrement, clearAll, increment }
})
