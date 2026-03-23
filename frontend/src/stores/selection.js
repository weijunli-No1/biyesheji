import { defineStore } from 'pinia'
import { ref } from 'vue'
import { topicApi } from '@/api'

/**
 * 当前登录学生的选题信息 store
 * 登录后由 Dashboard 初始化，各页面直接使用
 */
export const useSelectionStore = defineStore('selection', () => {
  const selection = ref(null)  // 完整选题对象（含 topicTitle、teacherName 等）
  const loading = ref(false)

  const selectionId = () => selection.value?.id ?? null
  const isConfirmed = () => selection.value?.status === 1
  const topicTitle = () => selection.value?.topicTitle ?? ''

  async function fetchMySelection(year) {
    loading.value = true
    try {
      const res = await topicApi.mySelection(year)
      selection.value = res.data
    } catch {
      selection.value = null
    } finally {
      loading.value = false
    }
  }

  function clear() {
    selection.value = null
  }

  return { selection, loading, selectionId, isConfirmed, topicTitle, fetchMySelection, clear }
})
