import request from '../utils/request'

const base = '/api/admin/learning-stats'

export const getLearningStatsOverview = () => request.get(`${base}/overview`, {
  skipGlobalErrorMessage: true,
})
