import request from './request'

// 认证
export const authApi = {
  login: (data) => request.post('/auth/login', data),
  me: () => request.get('/auth/me'),
}

// 工作流时间节点
export const workflowApi = {
  list: (year) => request.get('/workflow', { params: { year } }),
  currentStage: (year) => request.get('/workflow/current-stage', { params: { year } }),
  batchSave: (configs) => request.post('/workflow/batch', configs),
  update: (id, data) => request.put(`/workflow/${id}`, data),
}

// 课题管理
export const topicApi = {
  list: (params) => request.get('/topics', { params }),
  get: (id) => request.get(`/topics/${id}`),
  apply: (data) => request.post('/topics', data),
  update: (id, data) => request.put(`/topics/${id}`, data),
  approve: (id, data) => request.post(`/topics/${id}/approve`, data),
  myTopics: () => request.get('/topics/my'),
  delete: (id) => request.delete(`/topics/${id}`),
  select: (id, year) => request.post(`/topics/${id}/select`, null, { params: { year } }),
  confirmSelection: (selectionId, data) => request.post(`/topics/selections/${selectionId}/confirm`, data),
  withdraw: (selectionId) => request.delete(`/topics/selections/${selectionId}`),
  mySelection: (year) => request.get('/topics/selections/my', { params: { year } }),
  teacherSelections: () => request.get('/topics/selections/teacher'),
}

// 开题报告
export const proposalApi = {
  save: (data) => request.post('/proposals', data),
  submit: (data) => request.post('/proposals/submit', data),
  getBySelection: (selectionId) => request.get(`/proposals/selection/${selectionId}`),
  teacherReview: (id, data) => request.post(`/proposals/${id}/teacher-review`, data),
  deptReview: (id, data) => request.post(`/proposals/${id}/dept-review`, data),
  list: (params) => request.get('/proposals', { params }),
}

// 中期检查
export const midCheckApi = {
  save: (data) => request.post('/mid-checks', data),
  submit: (data) => request.post('/mid-checks/submit', data),
  getBySelection: (selectionId) => request.get(`/mid-checks/selection/${selectionId}`),
  review: (id, data) => request.post(`/mid-checks/${id}/review`, data),
  list: (params) => request.get('/mid-checks', { params }),
}

// 论文管理
export const thesisApi = {
  upload: (data) => request.post('/thesis/upload', data),
  versions: (selectionId) => request.get(`/thesis/versions/${selectionId}`),
  latest: (selectionId) => request.get(`/thesis/latest/${selectionId}`),
  review: (id, data) => request.post(`/thesis/${id}/review`, data),
  teacherList: () => request.get('/thesis/teacher-list'),
}

// 工作台统计
export const dashboardApi = {
  stats: () => request.get('/dashboard/stats'),
}

// 成绩管理
export const scoreApi = {
  // 导师录入自己的评分
  saveTeacherScore: (selectionId, data) => request.post(`/scores/${selectionId}/teacher`, data),
  // 管理员录入各项分数
  saveProposalScore: (selectionId, data) => request.post(`/scores/${selectionId}/proposal`, data),
  saveReviewScore:   (selectionId, data) => request.post(`/scores/${selectionId}/review`, data),
  saveDefenseScore:  (selectionId, data) => request.post(`/scores/${selectionId}/defense`, data),
  // 锁定成绩（四项齐全后）
  lock: (selectionId) => request.post(`/scores/${selectionId}/lock`),
  list: (params) => request.get('/scores', { params }),
  stats: () => request.get('/scores/stats'),
  getBySelection: (selectionId) => request.get(`/scores/selection/${selectionId}`),
}

// 用户管理
export const userApi = {
  list: (params) => request.get('/users', { params }),
  updateStatus: (id, status) => request.put(`/users/${id}/status`, null, { params: { status } }),
  resetPassword: (id) => request.post(`/users/${id}/reset-password`),
}

// 答辩管理
export const defenseApi = {
  listGroups: (year) => request.get('/defense/groups', { params: { year } }),
  createGroup: (data) => request.post('/defense/groups', data),
  listGroupStudents: (groupId) => request.get(`/defense/groups/${groupId}/students`),
  assignStudent: (groupId, selectionId) => request.post(`/defense/groups/${groupId}/students`, { selectionId }),
  saveRecord: (data) => request.post('/defense/records', data),
  myRecord: () => request.get('/defense/records/my'),
  unassignedStudents: (year) => request.get('/defense/students/unassigned', { params: { year } }),
}

// 组织架构管理（学院/专业/班级）
export const orgApi = {
  listColleges: () => request.get('/org/colleges'),
  createCollege: (data) => request.post('/org/colleges', data),
  updateCollege: (id, data) => request.put(`/org/colleges/${id}`, data),
  deleteCollege: (id) => request.delete(`/org/colleges/${id}`),

  listMajors: (collegeId) => request.get('/org/majors', { params: { collegeId } }),
  createMajor: (data) => request.post('/org/majors', data),
  updateMajor: (id, data) => request.put(`/org/majors/${id}`, data),
  deleteMajor: (id) => request.delete(`/org/majors/${id}`),

  listClasses: (params) => request.get('/org/classes', { params }),
  createClass: (data) => request.post('/org/classes', data),
  updateClass: (id, data) => request.put(`/org/classes/${id}`, data),
  deleteClass: (id) => request.delete(`/org/classes/${id}`),
}

// 文件上传（MinIO）
export const fileApi = {
  upload: (formData) => request.post('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
}
