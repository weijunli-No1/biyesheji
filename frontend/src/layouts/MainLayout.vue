<template>
  <el-container class="main-layout">
    <!-- Mobile overlay -->
    <div v-if="isMobile && !collapsed" class="sidebar-overlay" @click="collapsed = true" />

    <!-- 侧边栏 -->
    <el-aside
      :width="sidebarWidth"
      class="aside"
      :class="{ 'aside-mobile': isMobile, 'aside-hidden': isMobile && collapsed }">
      <div class="aside-logo">
        <el-icon size="24" color="#fff"><School /></el-icon>
        <span v-show="!collapsed || isMobile" class="logo-text">毕设管理系统</span>
      </div>
      <el-menu :collapse="!isMobile && collapsed" :default-active="activeMenu"
        background-color="#001529" text-color="#ffffffa0"
        active-text-color="#fff" router @select="onMenuSelect">
        <el-menu-item index="/dashboard">
          <el-icon><DataBoard /></el-icon>
          <template #title>工作台</template>
        </el-menu-item>

        <!-- 学生菜单 -->
        <template v-if="auth.isStudent">
          <el-menu-item index="/topics"><el-icon><Collection /></el-icon><template #title>选题</template></el-menu-item>
          <el-menu-item index="/proposal"><el-icon><Document /></el-icon><template #title>开题报告</template></el-menu-item>
          <el-menu-item index="/midcheck"><el-icon><DataAnalysis /></el-icon><template #title>中期检查</template></el-menu-item>
          <el-menu-item index="/thesis"><el-icon><Upload /></el-icon><template #title>论文提交</template></el-menu-item>
          <el-menu-item index="/defense/my"><el-icon><Microphone /></el-icon><template #title>我的答辩</template></el-menu-item>
          <el-menu-item index="/score/my"><el-icon><Trophy /></el-icon><template #title>我的成绩</template></el-menu-item>
        </template>

        <!-- 教师菜单 -->
        <template v-if="auth.isTeacher">
          <el-menu-item index="/topics/my"><el-icon><Collection /></el-icon><template #title>我的课题</template></el-menu-item>
          <el-menu-item index="/topics/apply"><el-icon><Plus /></el-icon><template #title>申报课题</template></el-menu-item>
          <el-menu-item index="/selections"><el-icon><UserFilled /></el-icon><template #title>指导学生</template></el-menu-item>
          <el-menu-item index="/proposals"><el-icon><Document /></el-icon><template #title>开题审核</template></el-menu-item>
          <el-menu-item index="/midchecks"><el-icon><DataAnalysis /></el-icon><template #title>中期检查</template></el-menu-item>
          <el-menu-item index="/thesis/review"><el-icon><Edit /></el-icon><template #title>论文审阅</template></el-menu-item>
          <el-menu-item index="/scores"><el-icon><Trophy /></el-icon><template #title>成绩录入</template></el-menu-item>
        </template>

        <!-- 答辩委员菜单 -->
        <template v-if="auth.isDefenseMember">
          <el-menu-item index="/defense"><el-icon><Microphone /></el-icon><template #title>答辩管理</template></el-menu-item>
        </template>

        <!-- 管理员菜单（专业/学院/教务） -->
        <template v-if="auth.isManager">
          <el-sub-menu index="topic-manage">
            <template #title><el-icon><Files /></el-icon><span>课题管理</span></template>
            <el-menu-item index="/topics">课题列表</el-menu-item>
            <el-menu-item index="/selections">选题管理</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="process-manage">
            <template #title><el-icon><List /></el-icon><span>过程管理</span></template>
            <el-menu-item index="/proposals">开题审核</el-menu-item>
            <el-menu-item index="/midchecks">中期检查</el-menu-item>
          </el-sub-menu>
          <el-menu-item index="/defense"><el-icon><Microphone /></el-icon><template #title>答辩管理</template></el-menu-item>
          <el-menu-item index="/scores"><el-icon><Trophy /></el-icon><template #title>成绩管理</template></el-menu-item>
          <el-sub-menu index="sys-manage">
            <template #title><el-icon><Setting /></el-icon><span>系统管理</span></template>
            <el-menu-item index="/workflow">时间节点配置</el-menu-item>
            <el-menu-item v-if="auth.isCollegeAdmin || auth.isAdmin" index="/org">组织架构管理</el-menu-item>
            <el-menu-item index="/users">用户管理</el-menu-item>
          </el-sub-menu>
        </template>
      </el-menu>
    </el-aside>

    <!-- 主区域 -->
    <el-container>
      <!-- 顶部 -->
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" size="20" @click="toggleSidebar"
            :aria-label="collapsed ? '展开侧边栏' : '收起侧边栏'">
            <Fold v-if="!collapsed" /><Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/" class="breadcrumb">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">工作台</el-breadcrumb-item>
            <el-breadcrumb-item v-if="parentTitle">{{ parentTitle }}</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar size="small" :style="{ background: '#1a6af0' }" aria-label="用户头像">
                {{ auth.userInfo?.realName?.charAt(0) }}
              </el-avatar>
              <span class="user-name">{{ auth.userInfo?.realName }}</span>
              <el-tag size="small" :type="roleTagType" class="role-tag">{{ roleName }}</el-tag>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容 -->
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { roleNames, roleTagTypes } from '@/utils/statusConfig'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const collapsed = ref(false)
const isMobile = ref(false)

function checkMobile() {
  isMobile.value = window.innerWidth <= 768
  if (isMobile.value) collapsed.value = true
}

function toggleSidebar() {
  collapsed.value = !collapsed.value
}

function onMenuSelect() {
  if (isMobile.value) collapsed.value = true
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})
onUnmounted(() => window.removeEventListener('resize', checkMobile))

const sidebarWidth = computed(() => {
  if (isMobile.value) return collapsed.value ? '0px' : '220px'
  return collapsed.value ? '64px' : '220px'
})

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta?.title || '')
const parentTitle = computed(() => route.meta?.parent || '')

const roleName = computed(() => roleNames[auth.role] || '')
const roleTagType = computed(() => roleTagTypes[auth.role] || '')

function handleCommand(cmd) {
  if (cmd === 'logout') {
    ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' }).then(() => {
      auth.logout()
      router.push('/login')
    })
  }
}
</script>

<style scoped>
.main-layout { height: 100vh; overflow: hidden; }

.aside {
  background: #001529;
  transition: width 0.2s;
  overflow: hidden;
  flex-shrink: 0;
  position: relative;
}

.aside-mobile {
  position: fixed;
  top: 0;
  left: 0;
  height: 100vh;
  z-index: 1001;
  transition: width 0.25s ease;
}

.aside-hidden {
  width: 0 !important;
}

.sidebar-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1000;
}

.aside-logo {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 10px;
  border-bottom: 1px solid #ffffff15;
  flex-shrink: 0;
}

.logo-text {
  color: #fff;
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-bold);
  white-space: nowrap;
}

.header {
  background: #fff;
  border-bottom: 1px solid var(--color-border, #f0f0f0);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;
  box-shadow: var(--shadow-card);
  flex-shrink: 0;
}

.header-left { display: flex; align-items: center; gap: 16px; min-width: 0; }
.breadcrumb { min-width: 0; overflow: hidden; }
.collapse-btn { cursor: pointer; color: #606266; flex-shrink: 0; }
.header-right { display: flex; align-items: center; flex-shrink: 0; }

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--radius-sm, 4px);
  outline: none;
}
.user-info:hover, .user-info:focus { background: var(--color-bg-page, #f5f7fa); }
.user-name {
  font-size: var(--font-size-md);
  color: var(--color-text-regular);
}

@media (max-width: 768px) {
  .user-name { display: none; }
  .role-tag { display: none; }
  .breadcrumb { font-size: var(--font-size-sm); }
}

.main-content {
  background: var(--color-bg-page, #f5f7fa);
  overflow-y: auto;
  padding: 0;
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.15s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

:deep(.el-menu) { border-right: none; }
:deep(.el-menu--collapse) { width: 64px; }
:deep(.el-container) { height: 100%; overflow: hidden; }
</style>
