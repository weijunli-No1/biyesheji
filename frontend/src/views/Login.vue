<template>
  <div class="login-page">
    <div class="login-box">
      <div class="login-header">
        <img src="/logo.svg" alt="logo" class="logo" onerror="this.style.display='none'" />
        <h1>毕业设计管理系统</h1>
        <p class="subtitle">Graduation Design Management System</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入学号/工号" prefix-icon="User" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码"
            prefix-icon="Lock" show-password clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="login-btn" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="demo-accounts">
        <el-divider>演示账号（密码均为 password123）</el-divider>
        <el-space wrap>
          <el-tag v-for="acc in demoAccounts" :key="acc.username"
            style="cursor:pointer" @click="fillAccount(acc)">
            {{ acc.label }}：{{ acc.username }}
          </el-tag>
        </el-space>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const formRef = ref()
const loading = ref(false)
const form = ref({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入学号/工号' }],
  password: [{ required: true, message: '请输入密码' }],
}

const demoAccounts = [
  { label: '学生', username: 'student001' },
  { label: '指导教师', username: 'teacher001' },
  { label: '评阅教师', username: 'teacher002' },
  { label: '院系管理员', username: 'dean001' },
  { label: '教务管理员', username: 'admin' },
]

function fillAccount(acc) {
  form.value.username = acc.username
  form.value.password = 'password123'
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await authApi.login(form.value)
    auth.setLogin(res.data)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a6af0 0%, #0d47a1 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-box {
  background: #fff;
  border-radius: var(--radius-xl, 16px);
  padding: 40px 48px;
  width: 440px;
  max-width: calc(100vw - 32px);
  box-shadow: 0 20px 60px rgba(0,0,0,0.2);
}

@media (max-width: 480px) {
  .login-box {
    padding: 28px 24px;
    border-radius: var(--radius-md, 8px);
  }
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo {
  width: 64px;
  height: 64px;
  margin-bottom: 12px;
}

.login-header h1 {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
}

.subtitle {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
}

.demo-accounts {
  margin-top: 8px;
}
</style>
