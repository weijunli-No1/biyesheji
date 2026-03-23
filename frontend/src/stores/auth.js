import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  const role = computed(() => userInfo.value?.role)
  const userId = computed(() => userInfo.value?.userId)
  const collegeId = computed(() => userInfo.value?.collegeId)
  const majorId = computed(() => userInfo.value?.majorId)

  // 角色判断 (1=学生 2=指导教师 3=评阅教师 4=答辩委员 5=专业管理员 6=学院管理员 7=教务管理员)
  const isStudent      = computed(() => role.value === 1)
  const isTeacher      = computed(() => role.value === 2)
  const isReviewer     = computed(() => role.value === 3)
  const isDefenseMember = computed(() => role.value === 4)
  const isMajorAdmin   = computed(() => role.value === 5)
  const isCollegeAdmin = computed(() => role.value === 6)
  const isAdmin        = computed(() => role.value === 7)
  const isManager      = computed(() => role.value >= 5)

  function setLogin(data) {
    token.value = data.token
    userInfo.value = data
    localStorage.setItem('token', data.token)
    localStorage.setItem('userInfo', JSON.stringify(data))
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return {
    token, userInfo, isLoggedIn, role, userId, collegeId, majorId,
    isStudent, isTeacher, isReviewer, isDefenseMember,
    isMajorAdmin, isCollegeAdmin, isAdmin, isManager,
    setLogin, logout
  }
})
