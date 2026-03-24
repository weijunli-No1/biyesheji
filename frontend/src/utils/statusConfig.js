/**
 * Shared status label / tag-type mappings
 * Used across multiple views to ensure visual consistency.
 */

// Topic (课题)
export const topicStatusLabels = { 0: '待审批', 1: '已发布', 2: '已关闭', 3: '已驳回' }
export const topicStatusColors = { 0: 'warning', 1: 'success', 2: 'info', 3: 'danger' }
export const topicTypeLabels   = { 1: '设计类', 2: '论文类', 3: '软件类', 4: '实验类' }
export const topicTypeColors   = { 1: '', 2: 'success', 3: 'warning', 4: 'info' }

// TopicSelection (选题)
export const selectionStatusLabels = { 0: '待确认', 1: '已确认', 2: '已拒绝', 3: '已解除' }
export const selectionStatusColors = { 0: 'warning', 1: 'success', 2: 'danger', 3: 'info' }

// Proposal (开题报告)
// status=2 表示导师已审核通过，等待院系评审；status=3 表示院系评审也通过（最终通过）
export const proposalStatusLabels = { 0: '草稿', 1: '待导师审核', 2: '导师已通过', 3: '评审已通过', 4: '已退回' }
export const proposalStatusColors = { 0: 'info', 1: 'warning', 2: '', 3: 'success', 4: 'danger' }

// MidCheck (中期检查)
export const midCheckStatusLabels = { 0: '未提交', 1: '待审核', 2: '已通过', 3: '已退回' }
export const midCheckStatusColors = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
export const progressLabels       = { 1: '正常', 2: '滞后', 3: '严重滞后' }
export const progressColors       = { 1: 'success', 2: 'warning', 3: 'danger' }

// Thesis version (论文版本)
export const thesisVersionTypeLabels = { 1: '初稿', 2: '修改稿', 3: '送审稿', 4: '定稿' }
export const thesisVersionStatusLabels = { 0: '待审阅', 1: '已退回', 2: '已通过' }
export const thesisVersionStatusColors = { 0: 'warning', 1: 'danger', 2: 'success' }

// Defense result (答辩)
export const defenseResultLabels = { 1: '通过', 2: '修改后通过', 3: '不通过' }
export const defenseResultColors = { 1: 'success', 2: 'warning', 3: 'danger' }

// Grade (成绩等级)
export const gradeTagType = (grade) =>
  ({ '优秀': 'success', '良好': '', '中等': 'info', '及格': 'warning', '不及格': 'danger' }[grade] || 'info')

export function gradeColor(score) {
  if (score >= 90) return '#67c23a'
  if (score >= 80) return '#1a6af0'
  if (score >= 60) return '#e6a23c'
  return '#f56c6c'
}

// User roles (1=学生 2=指导教师 3=评阅教师 4=答辩委员 5=专业管理员 6=学院管理员 7=教务管理员)
export const roleNames    = { 1: '学生', 2: '指导教师', 3: '评阅教师', 4: '答辩委员', 5: '专业管理员', 6: '学院管理员', 7: '教务管理员' }
export const roleTagTypes = { 1: '', 2: 'success', 3: 'warning', 4: 'warning', 5: 'danger', 6: 'danger', 7: 'danger' }
