<template>
  <div class="page-container">
    <!-- 统计卡片（仅管理员） -->
    <div class="stat-row" v-if="auth.isManager && stats">
      <StatCard icon="UserFilled"    label="参与人数" :value="stats.total || 0"   color="#1a6af0" />
      <StatCard icon="Medal"         label="优秀"     :value="stats.excellent || 0" color="#67c23a" />
      <StatCard icon="Trophy"        label="平均分"   :value="stats.average ? Number(stats.average).toFixed(1) : '-'" color="#e6a23c" />
      <StatCard icon="WarningFilled" label="不及格"   :value="stats.fail || 0"    color="#f56c6c" />
    </div>

    <div class="page-card">
      <el-form inline :model="query">
        <el-form-item label="学生">
          <el-input v-model="query.keyword" placeholder="姓名或学号" clearable
            style="width:160px" @keyup.enter="loadData" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="() => { query.keyword = ''; loadData() }">重置</el-button>
          <el-button v-if="auth.isManager" @click="exportScores">导出 Excel</el-button>
        </el-form-item>
      </el-form>

      <div class="table-scroll">
        <el-table :data="list" v-loading="loading" stripe style="margin-top:12px">
          <el-table-column prop="studentNo"   label="学号" width="130" />
          <el-table-column prop="studentName" label="姓名" width="90" />
          <el-table-column prop="topicTitle"  label="课题" min-width="180" show-overflow-tooltip />
          <el-table-column prop="teacherName" label="导师" width="90" />
          <!-- 分项成绩 -->
          <el-table-column label="开题(10%)" width="90" align="center" sortable>
            <template #default="{ row }">
              <span v-if="row.proposalScore !== null">{{ row.proposalScore }}</span>
              <span v-else class="text-placeholder">待录入</span>
            </template>
          </el-table-column>
          <el-table-column label="导师(30%)" width="90" align="center" sortable>
            <template #default="{ row }">
              <span v-if="row.teacherScore !== null">{{ row.teacherScore }}</span>
              <span v-else class="text-placeholder">待录入</span>
            </template>
          </el-table-column>
          <el-table-column label="评阅(20%)" width="90" align="center" sortable>
            <template #default="{ row }">
              <span v-if="row.reviewScore !== null">{{ row.reviewScore }}</span>
              <span v-else class="text-placeholder">待录入</span>
            </template>
          </el-table-column>
          <el-table-column label="答辩(40%)" width="90" align="center" sortable>
            <template #default="{ row }">
              <span v-if="row.defenseScore !== null">{{ row.defenseScore }}</span>
              <span v-else class="text-placeholder">待录入</span>
            </template>
          </el-table-column>
          <el-table-column label="综合" width="80" align="center" sortable prop="totalScore">
            <template #default="{ row }">
              <b v-if="row.totalScore !== null" :style="{ color: gradeColor(row.totalScore) }">
                {{ row.totalScore }}
              </b>
              <span v-else class="text-placeholder">待汇总</span>
            </template>
          </el-table-column>
          <el-table-column label="等级" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.grade" :type="gradeTagType(row.grade)" size="small">{{ row.grade }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="推优" width="60" align="center">
            <template #default="{ row }">
              <el-icon v-if="row.isExcellent" color="#67c23a" aria-label="推荐优秀"><StarFilled /></el-icon>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <!-- 指导教师：录入导师评分 -->
              <el-button v-if="auth.isTeacher && !row.isLocked"
                link type="primary" @click="editTeacherScore(row)">
                录入导师分
              </el-button>
              <!-- 评阅教师：录入评阅评分 -->
              <el-button v-if="auth.isReviewer && !row.isLocked"
                link type="warning" @click="openReviewScoreDialog(row)">
                录入评阅分
              </el-button>
              <!-- 管理员：录入各项分数 + 锁定 -->
              <template v-if="auth.isManager">
                <el-button v-if="!row.isLocked" link type="warning" @click="openAdminScoreDialog(row)">
                  录入成绩
                </el-button>
                <el-button v-if="!row.isLocked" link type="danger" @click="lock(row)">锁定</el-button>
                <el-tag v-if="row.isLocked" type="success" size="small">已锁定</el-tag>
              </template>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty description="暂无成绩数据" />
          </template>
        </el-table>
      </div>

      <el-pagination class="pagination-row" v-model:current-page="query.page"
        :total="total" layout="total, prev, pager, next" @change="loadData" />
    </div>

    <!-- 导师评分对话框 -->
    <el-dialog v-model="teacherDialogVisible" title="录入导师评分" :width="dialogWidth">
      <el-alert type="info" :closable="false" style="margin-bottom:16px"
        description="导师评分仅可录入自己指导学生的分数，且只能修改「导师评分」项（占综合成绩 30%）" />
      <el-form v-if="editRow" :model="teacherForm" label-width="100px">
        <el-form-item label="学生">
          {{ editRow.studentName }}（{{ editRow.studentNo }}）
        </el-form-item>
        <el-form-item label="导师评分">
          <el-input-number v-model="teacherForm.teacherScore" :min="0" :max="100" />
          <span class="text-secondary" style="margin-left:8px">分（0–100）</span>
        </el-form-item>
        <el-form-item label="导师评语">
          <el-input v-model="teacherForm.teacherComment" type="textarea" :rows="4"
            placeholder="请填写对学生论文的综合评价" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="teacherDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTeacherScore" :loading="savingScore">保存</el-button>
      </template>
    </el-dialog>

    <!-- 评阅教师录入评阅分对话框 -->
    <el-dialog v-model="reviewScoreDialogVisible" title="录入评阅评分" :width="dialogWidth">
      <el-alert type="info" :closable="false" style="margin-bottom:16px"
        description="评阅评分占综合成绩 20%，请仔细阅读论文后客观评分" />
      <el-form v-if="editRow" :model="reviewScoreForm" label-width="100px">
        <el-form-item label="学生">
          {{ editRow.studentName }}（{{ editRow.studentNo }}）
        </el-form-item>
        <el-form-item label="评阅评分">
          <el-input-number v-model="reviewScoreForm.reviewScore" :min="0" :max="100" />
          <span class="text-secondary" style="margin-left:8px">分（0–100）</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewScoreDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveReviewScore" :loading="savingScore">保存</el-button>
      </template>
    </el-dialog>

    <!-- 管理员录入各项分数对话框 -->
    <el-dialog v-model="adminScoreDialogVisible" title="录入各项成绩" :width="dialogWidth">
      <el-alert type="warning" :closable="false" style="margin-bottom:16px"
        description="综合成绩 = 开题×10% + 导师×30% + 评阅×20% + 答辩×40%，请确认数据准确后再保存" />
      <el-form v-if="editRow" :model="adminScoreForm" label-width="120px">
        <el-form-item label="学生">{{ editRow.studentName }}（{{ editRow.studentNo }}）</el-form-item>
        <el-form-item label="开题评分 (10%)">
          <el-input-number v-model="adminScoreForm.proposalScore" :min="0" :max="100" :precision="0" />
          <span class="text-secondary" style="margin-left:8px;font-size:12px">评审通过后自动同步</span>
        </el-form-item>
        <el-form-item label="导师评分 (30%)">
          <el-input-number v-model="adminScoreForm.teacherScore" :min="0" :max="100" :precision="0" />
        </el-form-item>
        <el-form-item label="评阅评分 (20%)">
          <el-input-number v-model="adminScoreForm.reviewScore" :min="0" :max="100" :precision="0" />
        </el-form-item>
        <el-form-item label="答辩评分 (40%)">
          <el-input-number v-model="adminScoreForm.defenseScore" :min="0" :max="100" :precision="0" />
          <span class="text-secondary" style="margin-left:8px;font-size:12px">答辩录入后自动同步</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adminScoreDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAdminScores" :loading="savingScore">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { scoreApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { gradeColor, gradeTagType } from '@/utils/statusConfig'
import StatCard from '@/components/StatCard.vue'

const auth = useAuthStore()
const loading = ref(false)
const savingScore = ref(false)
const list = ref([])
const total = ref(0)
const stats = ref(null)
const query = ref({ page: 1, size: 10, keyword: '' })

const teacherDialogVisible = ref(false)
const reviewScoreDialogVisible = ref(false)
const adminScoreDialogVisible = ref(false)
const editRow = ref(null)
const teacherForm = ref({ teacherScore: null, teacherComment: '' })
const reviewScoreForm = ref({ reviewScore: null })
const adminScoreForm = ref({ proposalScore: null, teacherScore: null, reviewScore: null, defenseScore: null })

const dialogWidth = computed(() => window.innerWidth <= 640 ? '92%' : '480px')

async function loadData() {
  loading.value = true
  try {
    const [listRes, statsRes] = await Promise.all([
      scoreApi.list(query.value),
      auth.isManager ? scoreApi.stats() : Promise.resolve({ data: null })
    ])
    list.value = listRes.data.records
    total.value = listRes.data.total
    if (statsRes.data) stats.value = statsRes.data
  } finally {
    loading.value = false
  }
}

function editTeacherScore(row) {
  editRow.value = row
  teacherForm.value = { teacherScore: row.teacherScore ?? null, teacherComment: row.teacherComment ?? '' }
  teacherDialogVisible.value = true
}

function openReviewScoreDialog(row) {
  editRow.value = row
  reviewScoreForm.value = { reviewScore: row.reviewScore ?? null }
  reviewScoreDialogVisible.value = true
}

async function saveReviewScore() {
  if (reviewScoreForm.value.reviewScore === null) return ElMessage.warning('请填写评阅评分')
  savingScore.value = true
  try {
    await scoreApi.saveReviewScore(editRow.value.selectionId, reviewScoreForm.value)
    ElMessage.success('评阅评分已保存')
    reviewScoreDialogVisible.value = false
    loadData()
  } finally {
    savingScore.value = false
  }
}

function openAdminScoreDialog(row) {
  editRow.value = row
  adminScoreForm.value = {
    proposalScore: row.proposalScore ?? null,
    teacherScore: row.teacherScore ?? null,
    reviewScore: row.reviewScore ?? null,
    defenseScore: row.defenseScore ?? null
  }
  adminScoreDialogVisible.value = true
}

async function saveTeacherScore() {
  if (teacherForm.value.teacherScore === null) return ElMessage.warning('请填写评分')
  savingScore.value = true
  try {
    await scoreApi.saveTeacherScore(editRow.value.selectionId, teacherForm.value)
    ElMessage.success('导师评分已保存')
    teacherDialogVisible.value = false
    loadData()
  } finally {
    savingScore.value = false
  }
}

async function saveAdminScores() {
  savingScore.value = true
  try {
    const sid = editRow.value.selectionId
    const { proposalScore, teacherScore, reviewScore, defenseScore } = adminScoreForm.value
    const tasks = []
    if (proposalScore !== null) tasks.push(scoreApi.saveProposalScore(sid, { proposalScore }))
    if (teacherScore !== null)  tasks.push(scoreApi.saveTeacherScore(sid, { teacherScore }))
    if (reviewScore !== null)   tasks.push(scoreApi.saveReviewScore(sid, { reviewScore }))
    if (defenseScore !== null)  tasks.push(scoreApi.saveDefenseScore(sid, { defenseScore }))
    await Promise.all(tasks)
    ElMessage.success('成绩已保存')
    adminScoreDialogVisible.value = false
    loadData()
  } finally {
    savingScore.value = false
  }
}

async function lock(row) {
  const missing = []
  if (row.proposalScore === null) missing.push('开题评分')
  if (row.teacherScore === null)  missing.push('导师评分')
  if (row.reviewScore === null)   missing.push('评阅评分')
  if (row.defenseScore === null)  missing.push('答辩评分')
  if (missing.length) {
    return ElMessage.warning(`以下分项尚未录入：${missing.join('、')}`)
  }
  await ElMessageBox.confirm(
    '锁定后成绩不可修改，请确认四项评分均已录入完毕。',
    '确认锁定',
    { type: 'warning', confirmButtonText: '确认锁定' }
  )
  try {
    await scoreApi.lock(row.selectionId)
    ElMessage.success('成绩已锁定')
    loadData()
  } catch {
    // 后端校验错误已通过拦截器弹出
  }
}

function exportScores() {
  if (!list.value.length) {
    ElMessage.warning('暂无数据可导出')
    return
  }
  const rows = [
    ['学号', '姓名', '课题', '导师', '开题(10%)', '导师(30%)', '评阅(20%)', '答辩(40%)', '综合成绩', '等级', '推优'],
    ...list.value.map(r => [
      r.studentNo,
      r.studentName,
      r.topicTitle,
      r.teacherName,
      r.proposalScore ?? '',
      r.teacherScore ?? '',
      r.reviewScore ?? '',
      r.defenseScore ?? '',
      r.totalScore ?? '',
      r.grade ?? '',
      r.isExcellent ? '是' : ''
    ])
  ]
  const csv = rows.map(r => r.map(cell => `"${String(cell).replace(/"/g, '""')}"`).join(',')).join('\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `成绩表_${new Date().toLocaleDateString('zh-CN').replace(/\//g, '-')}.csv`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('导出成功')
}

onMounted(loadData)
</script>

<style scoped>
.pagination-row { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
