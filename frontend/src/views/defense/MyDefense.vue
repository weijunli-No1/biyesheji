<template>
  <div class="page-container">
    <div class="page-card" v-loading="loading">
      <div class="toolbar">
        <b class="section-title">我的答辩信息</b>
      </div>

      <template v-if="record">
        <el-descriptions :column="2" border class="defense-desc">
          <el-descriptions-item label="学号">{{ record.studentNo }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ record.studentName }}</el-descriptions-item>
          <el-descriptions-item label="课题" :span="2">{{ record.topicTitle }}</el-descriptions-item>
          <el-descriptions-item label="答辩时间">
            {{ record.defenseTime ? dayjs(record.defenseTime).format('YYYY-MM-DD HH:mm') : '待安排' }}
          </el-descriptions-item>
          <el-descriptions-item label="答辩结果">
            <el-tag v-if="record.result" :type="defenseResultColors[record.result]">
              {{ defenseResultLabels[record.result] }}
            </el-tag>
            <el-tag v-else type="info">待答辩</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="汇报评分">
            {{ record.presentScore ?? '待录入' }}
          </el-descriptions-item>
          <el-descriptions-item label="问答评分">
            {{ record.qaScore ?? '待录入' }}
          </el-descriptions-item>
          <el-descriptions-item label="答辩总分">
            <b v-if="record.totalScore" style="font-size:18px;color:var(--color-primary)">
              {{ record.totalScore }}
            </b>
            <span v-else class="text-secondary">待录入</span>
          </el-descriptions-item>
          <el-descriptions-item label="答辩意见" :span="2">
            <span v-if="record.comment">{{ record.comment }}</span>
            <span v-else class="text-secondary">暂无</span>
          </el-descriptions-item>
          <el-descriptions-item v-if="record.questions" label="评委提问" :span="2">
            {{ record.questions }}
          </el-descriptions-item>
        </el-descriptions>

        <el-alert v-if="record.result === 2" type="warning" :closable="false" style="margin-top:16px"
          title="请根据评委意见修改论文后提交，如有疑问请联系指导教师" show-icon />
        <el-alert v-else-if="record.result === 3" type="error" :closable="false" style="margin-top:16px"
          title="本次答辩未通过，请联系院系管理员了解后续安排" show-icon />
        <el-alert v-else-if="record.result === 1" type="success" :closable="false" style="margin-top:16px"
          title="恭喜！答辩通过，请关注后续成绩发布通知" show-icon />
      </template>

      <el-empty v-else-if="!loading" description="尚未安排答辩，请关注通知公告">
        <template #image>
          <el-icon size="64" color="var(--color-text-secondary)"><Microphone /></el-icon>
        </template>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import dayjs from 'dayjs'
import { defenseApi } from '@/api'
import { defenseResultLabels, defenseResultColors } from '@/utils/statusConfig'

const loading = ref(false)
const record = ref(null)

async function loadRecord() {
  loading.value = true
  try {
    const res = await defenseApi.myRecord()
    record.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(loadRecord)
</script>

<style scoped>
.defense-desc {
  margin-top: 8px;
}
</style>
