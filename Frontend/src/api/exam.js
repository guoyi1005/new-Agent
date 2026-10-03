import { request } from './request'

const data = (promise) => promise.then((response) => response.data)

export const listExamPapers = (params = {}) => data(request({
  url: '/api/app/exam-papers', params,
}))
/** 试卷基本信息（标题、题量、满分）：历史记录页用来标明是哪一场考试 */
export const getExamPaper = (paperId) => data(request({
  url: `/api/app/exam-papers/${paperId}`,
}))
export const startExam = (paperId) => data(request({
  url: `/api/app/exam-papers/${paperId}/attempts`, method: 'POST',
}))
export const listExamHistory = (paperId) => data(request({
  url: `/api/app/exam-papers/${paperId}/attempts`,
}))
export const getExamAttempt = (attemptId) => data(request({
  url: `/api/app/exam-attempts/${attemptId}`,
}))
export const saveExamAnswer = (attemptId, questionId, payload) => data(request({
  url: `/api/app/exam-attempts/${attemptId}/answers/${questionId}`,
  method: 'PUT', data: payload,
}))
export const submitExam = (attemptId) => data(request({
  url: `/api/app/exam-attempts/${attemptId}/submit`, method: 'POST',
}))
export const getExamResult = (attemptId) => data(request({
  url: `/api/app/exam-attempts/${attemptId}/result`,
}))
