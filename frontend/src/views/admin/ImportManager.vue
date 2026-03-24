<template>
  <div class="import-manager">
    <el-tabs v-model="activeTab">
      <!-- ── 导入学生 ── -->
      <el-tab-pane label="批量导入学生" name="student">
        <div class="import-section">
          <div class="tip-box">
            <el-alert type="info" :closable="false">
              <template #title>
                Excel 列顺序：<b>学号*、姓名*、学院ID、专业ID、班级ID、邮箱、手机号</b>（* 为必填）。
                默认密码为学号。
              </template>
            </el-alert>
          </div>

          <div class="form-row">
            <el-select v-model="studentYear" placeholder="选择届别" style="width:140px;">
              <el-option v-for="y in yearOptions" :key="y" :label="`${y}届`" :value="y" />
            </el-select>
            <a :href="templateUrl('student')" download="学生导入模板.xlsx">
              <el-button>下载模板</el-button>
            </a>
            <el-upload
              action="#"
              :auto-upload="false"
              :limit="1"
              :on-change="(f) => (studentFile = f)"
              accept=".xlsx,.xls"
              :show-file-list="false"
            >
              <el-button type="primary" :loading="importing.student">
                {{ studentFile ? studentFile.name : '选择Excel文件' }}
              </el-button>
            </el-upload>
            <el-button type="success" :disabled="!studentFile" :loading="importing.student"
                       @click="doImport('student')">
              开始导入
            </el-button>
          </div>

          <ImportResult v-if="result.student" :result="result.student" />
        </div>
      </el-tab-pane>

      <!-- ── 导入教师 ── -->
      <el-tab-pane label="批量导入教师" name="teacher">
        <div class="import-section">
          <div class="tip-box">
            <el-alert type="info" :closable="false">
              <template #title>
                Excel 列顺序：<b>工号*、姓名*、学院ID、专业ID、职称、邮箱、手机号</b>（* 为必填）。
                默认密码为工号。
              </template>
            </el-alert>
          </div>

          <div class="form-row">
            <a :href="templateUrl('teacher')" download="教师导入模板.xlsx">
              <el-button>下载模板</el-button>
            </a>
            <el-upload
              action="#"
              :auto-upload="false"
              :limit="1"
              :on-change="(f) => (teacherFile = f)"
              accept=".xlsx,.xls"
              :show-file-list="false"
            >
              <el-button type="primary" :loading="importing.teacher">
                {{ teacherFile ? teacherFile.name : '选择Excel文件' }}
              </el-button>
            </el-upload>
            <el-button type="success" :disabled="!teacherFile" :loading="importing.teacher"
                       @click="doImport('teacher')">
              开始导入
            </el-button>
          </div>

          <ImportResult v-if="result.teacher" :result="result.teacher" />
        </div>
      </el-tab-pane>

      <!-- ── 导入历史 ── -->
      <el-tab-pane label="导入历史" name="history">
        <el-table :data="historyList" v-loading="historyLoading" stripe>
          <el-table-column prop="type" label="类型" width="130">
            <template #default="{ row }">
              {{ row.type === 'USER_STUDENT' ? '学生' : '教师' }}
            </template>
          </el-table-column>
          <el-table-column prop="year" label="届别" width="80" />
          <el-table-column prop="fileName" label="文件名" show-overflow-tooltip />
          <el-table-column prop="totalCount" label="总行数" width="80" />
          <el-table-column prop="successCount" label="成功" width="80">
            <template #default="{ row }">
              <el-text type="success">{{ row.successCount }}</el-text>
            </template>
          </el-table-column>
          <el-table-column prop="failCount" label="失败" width="80">
            <template #default="{ row }">
              <el-text :type="row.failCount > 0 ? 'danger' : 'info'">{{ row.failCount }}</el-text>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'warning' : 'info'" size="small">
                {{ row.status === 0 ? '处理中' : row.status === 1 ? '全部成功' : '部分失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="时间" width="160">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="错误详情" width="100">
            <template #default="{ row }">
              <el-button v-if="row.failCount > 0" text size="small" type="danger"
                         @click="showErrors(row)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 错误详情弹窗 -->
    <el-dialog v-model="errorDialog.visible" title="失败行详情" width="600px">
      <el-table :data="errorDialog.list" size="small" stripe>
        <el-table-column prop="row" label="行号" width="80" />
        <el-table-column prop="value" label="学号/工号" width="150" />
        <el-table-column prop="error" label="原因" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, defineComponent, h } from 'vue'
import { importApi } from '@/api/index.js'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'

// 内嵌导入结果组件
const ImportResult = defineComponent({
  props: { result: Object },
  setup(props) {
    return () => h('div', { class: 'import-result' }, [
      h('el-alert', {
        type: props.result.failCount > 0 ? 'warning' : 'success',
        closable: false,
        title: `导入完成：成功 ${props.result.successCount} 条，失败 ${props.result.failCount} 条`
      })
    ])
  }
})

const activeTab = ref('student')
const studentFile = ref(null)
const teacherFile = ref(null)
const studentYear = ref(new Date().getFullYear())
const importing = reactive({ student: false, teacher: false })
const result = reactive({ student: null, teacher: null })
const historyList = ref([])
const historyLoading = ref(false)
const errorDialog = reactive({ visible: false, list: [] })

const yearOptions = Array.from({ length: 5 }, (_, i) => new Date().getFullYear() - 2 + i)
const templateUrl = (type) => `/api/import/template/${type}`
const formatTime = (t) => t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-'

async function doImport(type) {
  const file = type === 'student' ? studentFile.value : teacherFile.value
  if (!file) return
  importing[type] = true
  try {
    const formData = new FormData()
    formData.append('file', file.raw)
    let res
    if (type === 'student') {
      res = await importApi.importStudents(formData, studentYear.value)
    } else {
      res = await importApi.importTeachers(formData)
    }
    result[type] = res.data?.data
    ElMessage.success('导入完成')
    loadHistory()
    if (type === 'student') studentFile.value = null
    else teacherFile.value = null
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '导入失败')
  } finally {
    importing[type] = false
  }
}

async function loadHistory() {
  historyLoading.value = true
  try {
    const res = await importApi.history()
    historyList.value = res.data?.data ?? []
  } finally {
    historyLoading.value = false
  }
}

function showErrors(row) {
  try {
    errorDialog.list = JSON.parse(row.errorDetail || '[]')
  } catch {
    errorDialog.list = []
  }
  errorDialog.visible = true
}

onMounted(loadHistory)
</script>

<style scoped>
.import-manager { padding: 20px; }
.import-section { padding: 16px 0; }
.tip-box { margin-bottom: 16px; }
.form-row {
  display: flex; align-items: center; gap: 12px; flex-wrap: wrap;
  margin-bottom: 16px;
}
.import-result { margin-top: 12px; }
</style>
