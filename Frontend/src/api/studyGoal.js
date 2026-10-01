import { request } from './request'

const unwrap = (promise) => promise.then((response) => response.data)

export const decomposeStudyText = (planText) => unwrap(request({
  url: '/api/study-goal/decompose-text',
  method: 'POST',
  data: { planText },
}))

export const saveStudyGoal = (payload) => unwrap(request({
  url: '/api/study-goal/save',
  method: 'POST',
  data: payload,
}))

export const listMyStudyGoals = (page = 1, size = 20) => unwrap(request({
  url: '/api/study-goal/my',
  params: { page, size },
}))

export const getStudyGoalDetail = (goalId, filter = 'all') => unwrap(request({
  url: `/api/study-goal/${goalId}`,
  params: { filter },
}))

export const updateStudyGoalTaskCompletion = (taskId, isCompleted) => unwrap(request({
  url: `/api/study-goal/tasks/${taskId}/completion`,
  method: 'PUT',
  data: { isCompleted },
}))

export const updateStudyGoalTaskProgress = (taskId, progressPercent) => unwrap(request({
  url: `/api/study-goal/tasks/${taskId}/progress`,
  method: 'PUT',
  data: { progressPercent },
}))

export const updateStudyGoalSubtaskCompletion = (subtaskId, isCompleted) => unwrap(request({
  url: `/api/study-goal/subtasks/${subtaskId}/completion`,
  method: 'PUT',
  data: { isCompleted },
}))

export const updateStudyGoalSubtaskProgress = (subtaskId, progressPercent) => unwrap(request({
  url: `/api/study-goal/subtasks/${subtaskId}/progress`,
  method: 'PUT',
  data: { progressPercent },
}))

export const postponeStudyGoalTask = (taskId, days = 1) => unwrap(request({
  url: `/api/study-goal/tasks/${taskId}/postpone`,
  method: 'POST',
  data: { days },
}))

export const expandStudyGoalSubtasks = (goalId) => unwrap(request({
  url: `/api/study-goal/${goalId}/expand-subtasks`,
  method: 'POST',
}))

export const deleteStudyGoal = (goalId) => unwrap(request({
  url: `/api/study-goal/${goalId}`,
  method: 'DELETE',
}))