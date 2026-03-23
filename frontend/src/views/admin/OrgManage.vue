<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" type="border-card">
      <!-- ==================== 学院 ==================== -->
      <el-tab-pane label="学院管理" name="college">
        <div class="tab-toolbar">
          <el-button v-if="auth.isAdmin" type="primary" @click="openCollegeDialog()">
            <el-icon><Plus /></el-icon> 新建学院
          </el-button>
        </div>
        <el-table :data="colleges" border stripe>
          <el-table-column prop="id"   label="ID"   width="80" />
          <el-table-column prop="name" label="学院名称" />
          <el-table-column prop="code" label="学院代码" width="120" />
          <el-table-column label="操作" width="160" v-if="auth.isAdmin">
            <template #default="{ row }">
              <el-button size="small" @click="openCollegeDialog(row)">编辑</el-button>
              <el-popconfirm title="确认删除该学院？" @confirm="deleteCollege(row.id)">
                <template #reference>
                  <el-button size="small" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ==================== 专业 ==================== -->
      <el-tab-pane label="专业管理" name="major">
        <div class="tab-toolbar">
          <el-select v-model="majorFilter.collegeId" placeholder="按学院筛选" clearable style="width:200px"
            @change="loadMajors">
            <el-option v-for="c in colleges" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <el-button v-if="auth.isCollegeAdmin || auth.isAdmin" type="primary" @click="openMajorDialog()">
            <el-icon><Plus /></el-icon> 新建专业
          </el-button>
        </div>
        <el-table :data="majors" border stripe>
          <el-table-column prop="id"         label="ID"   width="80" />
          <el-table-column prop="name"        label="专业名称" />
          <el-table-column prop="code"        label="专业代码" width="120" />
          <el-table-column prop="collegeName" label="所属学院" />
          <el-table-column label="操作" width="160" v-if="auth.isCollegeAdmin || auth.isAdmin">
            <template #default="{ row }">
              <el-button size="small" @click="openMajorDialog(row)">编辑</el-button>
              <el-popconfirm title="确认删除该专业？" @confirm="deleteMajor(row.id)">
                <template #reference>
                  <el-button size="small" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ==================== 班级 ==================== -->
      <el-tab-pane label="班级管理" name="class">
        <div class="tab-toolbar">
          <el-select v-model="classFilter.collegeId" placeholder="按学院筛选" clearable style="width:180px"
            @change="() => { classFilter.majorId = null; loadClassMajors(); loadClasses() }">
            <el-option v-for="c in colleges" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <el-select v-model="classFilter.majorId" placeholder="按专业筛选" clearable style="width:180px"
            @change="loadClasses">
            <el-option v-for="m in classMajors" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
          <el-button v-if="auth.isMajorAdmin || auth.isCollegeAdmin || auth.isAdmin" type="primary"
            @click="openClassDialog()">
            <el-icon><Plus /></el-icon> 新建班级
          </el-button>
        </div>
        <el-table :data="classes" border stripe>
          <el-table-column prop="id"          label="ID"   width="80" />
          <el-table-column prop="name"         label="班级名称" />
          <el-table-column prop="majorName"    label="所属专业" />
          <el-table-column prop="enrollYear"   label="入学年份" width="100" />
          <el-table-column label="操作" width="160"
            v-if="auth.isMajorAdmin || auth.isCollegeAdmin || auth.isAdmin">
            <template #default="{ row }">
              <el-button size="small" @click="openClassDialog(row)">编辑</el-button>
              <el-popconfirm title="确认删除该班级？" @confirm="deleteClass(row.id)">
                <template #reference>
                  <el-button size="small" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 学院 dialog -->
    <el-dialog v-model="collegeDialog.visible" :title="collegeDialog.id ? '编辑学院' : '新建学院'" width="400px">
      <el-form :model="collegeDialog" label-width="80px">
        <el-form-item label="学院名称" required>
          <el-input v-model="collegeDialog.name" placeholder="如：计算机学院" />
        </el-form-item>
        <el-form-item label="学院代码">
          <el-input v-model="collegeDialog.code" placeholder="如：CS" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="collegeDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveCollege">保存</el-button>
      </template>
    </el-dialog>

    <!-- 专业 dialog -->
    <el-dialog v-model="majorDialog.visible" :title="majorDialog.id ? '编辑专业' : '新建专业'" width="400px">
      <el-form :model="majorDialog" label-width="80px">
        <el-form-item label="所属学院" required>
          <el-select v-model="majorDialog.collegeId" placeholder="选择学院" style="width:100%"
            :disabled="auth.isCollegeAdmin">
            <el-option v-for="c in colleges" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="专业名称" required>
          <el-input v-model="majorDialog.name" placeholder="如：软件工程" />
        </el-form-item>
        <el-form-item label="专业代码">
          <el-input v-model="majorDialog.code" placeholder="如：SE" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="majorDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveMajor">保存</el-button>
      </template>
    </el-dialog>

    <!-- 班级 dialog -->
    <el-dialog v-model="classDialog.visible" :title="classDialog.id ? '编辑班级' : '新建班级'" width="400px">
      <el-form :model="classDialog" label-width="80px">
        <el-form-item label="所属专业" required>
          <el-select v-model="classDialog.majorId" placeholder="选择专业" style="width:100%"
            :disabled="auth.isMajorAdmin">
            <el-option v-for="m in majors" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级名称" required>
          <el-input v-model="classDialog.name" placeholder="如：软工2021-1班" />
        </el-form-item>
        <el-form-item label="入学年份">
          <el-input-number v-model="classDialog.enrollYear" :min="2000" :max="2099" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="classDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveClass">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { orgApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

const activeTab = ref('college')

// ---- 学院 ----
const colleges = ref([])

async function loadColleges() {
  const res = await orgApi.listColleges()
  colleges.value = res.data || []
}

const collegeDialog = reactive({ visible: false, id: null, name: '', code: '' })

function openCollegeDialog(row = null) {
  collegeDialog.id = row?.id || null
  collegeDialog.name = row?.name || ''
  collegeDialog.code = row?.code || ''
  collegeDialog.visible = true
}

async function saveCollege() {
  if (!collegeDialog.name) return ElMessage.warning('请填写学院名称')
  const data = { name: collegeDialog.name, code: collegeDialog.code }
  if (collegeDialog.id) {
    await orgApi.updateCollege(collegeDialog.id, data)
  } else {
    await orgApi.createCollege(data)
  }
  ElMessage.success('保存成功')
  collegeDialog.visible = false
  loadColleges()
}

async function deleteCollege(id) {
  await orgApi.deleteCollege(id)
  ElMessage.success('删除成功')
  loadColleges()
}

// ---- 专业 ----
const majors = ref([])
const majorFilter = reactive({ collegeId: null })

async function loadMajors() {
  const res = await orgApi.listMajors(majorFilter.collegeId)
  const list = res.data || []
  // 补充学院名
  list.forEach(m => {
    const c = colleges.value.find(c => c.id === m.collegeId)
    m.collegeName = c?.name || ''
  })
  majors.value = list
}

const majorDialog = reactive({ visible: false, id: null, name: '', code: '', collegeId: null })

function openMajorDialog(row = null) {
  majorDialog.id = row?.id || null
  majorDialog.name = row?.name || ''
  majorDialog.code = row?.code || ''
  majorDialog.collegeId = row?.collegeId || (auth.isCollegeAdmin ? auth.collegeId : null)
  majorDialog.visible = true
}

async function saveMajor() {
  if (!majorDialog.name || !majorDialog.collegeId) return ElMessage.warning('请填写专业名称和所属学院')
  const data = { name: majorDialog.name, code: majorDialog.code, collegeId: majorDialog.collegeId }
  if (majorDialog.id) {
    await orgApi.updateMajor(majorDialog.id, data)
  } else {
    await orgApi.createMajor(data)
  }
  ElMessage.success('保存成功')
  majorDialog.visible = false
  loadMajors()
}

async function deleteMajor(id) {
  await orgApi.deleteMajor(id)
  ElMessage.success('删除成功')
  loadMajors()
}

// ---- 班级 ----
const classes = ref([])
const classMajors = ref([])
const classFilter = reactive({ collegeId: null, majorId: null })

async function loadClassMajors() {
  const res = await orgApi.listMajors(classFilter.collegeId)
  classMajors.value = res.data || []
}

async function loadClasses() {
  const res = await orgApi.listClasses({ majorId: classFilter.majorId, collegeId: classFilter.collegeId })
  const list = res.data || []
  list.forEach(c => {
    const m = majors.value.find(m => m.id === c.majorId)
    c.majorName = m?.name || ''
  })
  classes.value = list
}

const classDialog = reactive({ visible: false, id: null, name: '', majorId: null, enrollYear: new Date().getFullYear() })

function openClassDialog(row = null) {
  classDialog.id = row?.id || null
  classDialog.name = row?.name || ''
  classDialog.majorId = row?.majorId || (auth.isMajorAdmin ? auth.majorId : null)
  classDialog.enrollYear = row?.enrollYear || new Date().getFullYear()
  classDialog.visible = true
}

async function saveClass() {
  if (!classDialog.name || !classDialog.majorId) return ElMessage.warning('请填写班级名称和所属专业')
  const data = { name: classDialog.name, majorId: classDialog.majorId, enrollYear: classDialog.enrollYear }
  if (classDialog.id) {
    await orgApi.updateClass(classDialog.id, data)
  } else {
    await orgApi.createClass(data)
  }
  ElMessage.success('保存成功')
  classDialog.visible = false
  loadClasses()
}

async function deleteClass(id) {
  await orgApi.deleteClass(id)
  ElMessage.success('删除成功')
  loadClasses()
}

onMounted(async () => {
  await loadColleges()
  await loadMajors()
  await loadClasses()
})
</script>

<style scoped>
.page-container { padding: 20px; }
.tab-toolbar { display: flex; gap: 12px; align-items: center; margin-bottom: 16px; }
</style>
