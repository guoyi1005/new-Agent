import { request } from './request'

export const getCareerNebulaMap = async () => {
  const response = await request({ url: '/api/app/career-nebula' })
  return response?.data || { careers: [], skills: [], edges: [] }
}

/** 人岗匹配：匹配度、已掌握、待提升与逐技能差距（由岗位要求 + 我的技能等级算出） */
export const getCareerJobFit = async (jobName) => {
  const response = await request({
    url: '/api/app/career/job-fit',
    params: { jobName },
  })
  return response?.data || null
}

/** 岗位匹配排行：返回与当前学生匹配度较高的其它岗位 */
export const getCareerFitJobs = async (jobName, limit = 3) => {
  const response = await request({
    url: '/api/app/career/fit-jobs',
    params: { jobName, limit },
  })
  return Array.isArray(response?.data) ? response.data : []
}

export const getCareerLearningProgress = async () => {
  const response = await request({ url: '/api/app/career-nebula/progress' })
  return response?.data || { completedItemIds: [] }
}

export const updateCareerLearningProgress = async (itemId, data) => {
  const response = await request({
    url: `/api/app/career-nebula/progress/items/${encodeURIComponent(itemId)}`,
    method: 'PUT',
    data,
  })
  return response?.data
}

export const getCareerExploration = async (careerId) => {
  const response = await request({
    url: `/api/app/career-exploration/careers/${encodeURIComponent(careerId)}`,
  })
  return response?.data || { career: null, planets: [], edges: [] }
}

export const getCareerPlanet = async (careerId, skillId) => {
  const response = await request({
    url: `/api/app/career-exploration/careers/${encodeURIComponent(careerId)}/planets/${encodeURIComponent(skillId)}`,
  })
  return response?.data
}

export const getCareerChapter = async (careerId, skillId, chapterId) => {
  const response = await request({
    url: `/api/app/career-exploration/careers/${encodeURIComponent(careerId)}/planets/${encodeURIComponent(skillId)}/chapters/${chapterId}`,
  })
  return response?.data
}

export const updateCareerVideoProgress = async (careerId, skillId, chapterId, data) => {
  const response = await request({
    url: `/api/app/career-exploration/careers/${encodeURIComponent(careerId)}/planets/${encodeURIComponent(skillId)}/chapters/${chapterId}/video-progress`,
    method: 'PUT', data,
  })
  return response?.data
}

export const answerCareerChapterQuestion = async (careerId, skillId, chapterId, questionId, answer) => {
  const response = await request({
    url: `/api/app/career-exploration/careers/${encodeURIComponent(careerId)}/planets/${encodeURIComponent(skillId)}/chapters/${chapterId}/questions/${encodeURIComponent(questionId)}/answer`,
    method: 'POST', data: { answer },
  })
  return response?.data
}

export const completeCareerChapter = async (careerId, skillId, chapterId) => {
  const response = await request({
    url: `/api/app/career-exploration/careers/${encodeURIComponent(careerId)}/planets/${encodeURIComponent(skillId)}/chapters/${chapterId}/complete`,
    method: 'POST',
  })
  return response?.data
}

export const getCareerFinalExam = async (careerId, skillId) => {
  const response = await request({
    url: `/api/app/career-exploration/careers/${encodeURIComponent(careerId)}/planets/${encodeURIComponent(skillId)}/final-exam`,
  })
  return response?.data
}

export const syncCareerFinalExam = async (careerId, skillId, attemptId) => {
  const response = await request({
    url: `/api/app/career-exploration/careers/${encodeURIComponent(careerId)}/planets/${encodeURIComponent(skillId)}/sync-final-exam`,
    method: 'POST', data: { attemptId },
  })
  return response?.data
}

export const enrollCareerCourse = (courseId) => request({
  url: `/api/app/campus-courses/${courseId}/enroll`, method: 'POST',
})

export const getCareerMaterialUrl = async (courseId, chapterId, materialId) => {
  const response = await request({
    url: `/api/app/campus-courses/${courseId}/chapters/${chapterId}/materials/${materialId}/url`,
  })
  return response?.data
}
