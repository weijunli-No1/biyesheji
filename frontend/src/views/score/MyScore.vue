<template>
  <div class="page-container">
    <div class="page-card" style="max-width:700px">
      <h3 class="page-title" style="margin-bottom:20px">我的毕业设计成绩</h3>

      <el-skeleton :loading="loading" :rows="6" animated>
        <template #default>
          <el-empty v-if="!score" description="成绩尚未公布，请耐心等待">
            <template #extra>
              <p class="text-secondary">成绩在答辩完成后由管理员锁定后可见</p>
            </template>
          </el-empty>
          <template v-else>
            <el-result :icon="score.grade === '优秀' ? 'success' : 'info'"
              :title="score.grade + '（' + score.totalScore + ' 分）'">
              <template #extra>
                <el-tag v-if="score.isExcellent" type="success" size="large">★ 推荐优秀论文</el-tag>
              </template>
            </el-result>

            <el-descriptions :column="2" border>
              <el-descriptions-item label="开题报告（10%）">
                <span :style="{ color: score.proposalScore !== null ? '#303133' : '#c0c4cc' }">
                  {{ score.proposalScore !== null ? score.proposalScore + ' 分' : '待录入' }}
                </span>
              </el-descriptions-item>
              <el-descriptions-item label="导师评分（30%）">
                <span :style="{ color: score.teacherScore !== null ? '#303133' : '#c0c4cc' }">
                  {{ score.teacherScore !== null ? score.teacherScore + ' 分' : '待录入' }}
                </span>
              </el-descriptions-item>
              <el-descriptions-item label="评阅评分（20%）">
                <span :style="{ color: score.reviewScore !== null ? '#303133' : '#c0c4cc' }">
                  {{ score.reviewScore !== null ? score.reviewScore + ' 分' : '待录入' }}
                </span>
              </el-descriptions-item>
              <el-descriptions-item label="答辩评分（40%）">
                <span :style="{ color: score.defenseScore !== null ? '#303133' : '#c0c4cc' }">
                  {{ score.defenseScore !== null ? score.defenseScore + ' 分' : '待录入' }}
                </span>
              </el-descriptions-item>
              <el-descriptions-item label="综合成绩" :span="2">
                <b style="font-size:20px;color:var(--color-primary)">{{ score.totalScore }}</b>
              </el-descriptions-item>
              <el-descriptions-item label="导师评语" :span="2">
                {{ score.teacherComment || '（暂无）' }}
              </el-descriptions-item>
            </el-descriptions>

            <div class="text-placeholder" style="margin-top:16px;text-align:center">
              综合成绩 = 开题报告×10% + 导师评分×30% + 评阅评分×20% + 答辩评分×40%
            </div>
          </template>
        </template>
      </el-skeleton>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { scoreApi } from '@/api'
import { useSelectionStore } from '@/stores/selection'

const selectionStore = useSelectionStore()
const loading = ref(false)
const score = ref(null)

const selectionId = computed(() => selectionStore.selectionId())

onMounted(async () => {
  const currentYear = new Date().getFullYear()
  if (!selectionStore.selection) {
    await selectionStore.fetchMySelection(currentYear)
  }
  if (!selectionId.value) return
  loading.value = true
  try {
    const res = await scoreApi.getBySelection(selectionId.value)
    score.value = res.data
  } finally {
    loading.value = false
  }
})
</script>
