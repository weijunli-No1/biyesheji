import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  { path: '/login', component: () => import('@/views/Login.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('@/views/Dashboard.vue'), meta: { title: '工作台' } },

      // ---- 课题管理 ----
      { path: 'topics',       component: () => import('@/views/topic/TopicList.vue'),     meta: { title: '课题列表' } },
      { path: 'topics/my',    component: () => import('@/views/topic/MyTopics.vue'),      meta: { title: '我的课题',  roles: [2] } },
      { path: 'topics/apply', component: () => import('@/views/topic/TopicApply.vue'),    meta: { title: '申报课题',  parent: '课题管理', roles: [2] } },
      { path: 'topics/:id',   component: () => import('@/views/topic/TopicDetail.vue'),   meta: { title: '课题详情',  parent: '课题列表' } },
      { path: 'selections',   component: () => import('@/views/topic/SelectionList.vue'), meta: { title: '选题管理',  parent: '课题管理', roles: [2, 5, 6, 7] } },

      // ---- 开题报告 ----
      { path: 'proposal',  component: () => import('@/views/proposal/ProposalEdit.vue'), meta: { title: '开题报告', roles: [1] } },
      { path: 'proposals', component: () => import('@/views/proposal/ProposalList.vue'), meta: { title: '开题审核', parent: '过程管理', roles: [2, 5, 6, 7] } },

      // ---- 中期检查 ----
      { path: 'midcheck',  component: () => import('@/views/midcheck/MidCheckEdit.vue'), meta: { title: '中期检查', roles: [1] } },
      { path: 'midchecks', component: () => import('@/views/midcheck/MidCheckList.vue'), meta: { title: '中期检查管理', parent: '过程管理', roles: [2, 5, 6, 7] } },

      // ---- 论文管理 ----
      { path: 'thesis',        component: () => import('@/views/thesis/ThesisUpload.vue'), meta: { title: '论文提交', roles: [1] } },
      { path: 'thesis/review', component: () => import('@/views/thesis/ThesisReview.vue'), meta: { title: '论文审阅', parent: '论文管理', roles: [2] } },

      // ---- 答辩管理 ----
      { path: 'defense',    component: () => import('@/views/defense/DefenseList.vue'), meta: { title: '答辩管理', parent: '答辩管理', roles: [4, 5, 6, 7] } },
      { path: 'defense/my', component: () => import('@/views/defense/MyDefense.vue'),  meta: { title: '我的答辩', roles: [1] } },

      // ---- 成绩管理 ----
      { path: 'scores',   component: () => import('@/views/score/ScoreList.vue'), meta: { title: '成绩管理', roles: [2, 3, 5, 6, 7] } },
      { path: 'score/my', component: () => import('@/views/score/MyScore.vue'),   meta: { title: '我的成绩', roles: [1] } },

      // ---- 系统管理 ----
      { path: 'workflow', component: () => import('@/views/admin/WorkflowConfig.vue'), meta: { title: '时间节点配置', parent: '系统管理', roles: [5, 6, 7] } },
      { path: 'org',      component: () => import('@/views/admin/OrgManage.vue'),      meta: { title: '组织架构管理',  parent: '系统管理', roles: [6, 7] } },
      { path: 'users',    component: () => import('@/views/admin/UserList.vue'),        meta: { title: '用户管理',      parent: '系统管理', roles: [5, 6, 7] } },
      { path: 'import',   component: () => import('@/views/admin/ImportManager.vue'),   meta: { title: '批量导入',      parent: '系统管理', roles: [5, 6, 7] } },

      // ---- 通知 ----
      { path: 'notifications', component: () => import('@/views/NotificationPage.vue'), meta: { title: '消息通知' } },
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isLoggedIn) {
    return next('/login')
  }
  if (to.meta.roles && !to.meta.roles.includes(auth.role)) {
    return next('/dashboard')
  }
  document.title = to.meta.title ? `${to.meta.title} - 毕设管理系统` : '毕设管理系统'
  next()
})

export default router
