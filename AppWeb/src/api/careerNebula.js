import request from '../utils/request'

export const getCareerNebulaMap = async () => {
  const response = await request.get('/api/admin/career-nebula', { timeout: 30000 })
  return response.data
}

export const saveCareerNebulaMap = async (payload) => {
  const response = await request.put('/api/admin/career-nebula', payload, { timeout: 30000 })
  return response.data
}

/* ---------- 岗位关系（职业路径图谱的连线配置） ---------- */

export const listCareerJobRelations = async () => {
  const response = await request.get('/api/admin/career-job-relations', { timeout: 30000 })
  return response.data
}

export const createCareerJobRelation = async (payload) => {
  const response = await request.post('/api/admin/career-job-relations', payload, { timeout: 30000 })
  return response.data
}

export const updateCareerJobRelation = async (id, payload) => {
  const response = await request.put(`/api/admin/career-job-relations/${id}`, payload, { timeout: 30000 })
  return response.data
}

export const deleteCareerJobRelation = async (id) => {
  const response = await request.delete(`/api/admin/career-job-relations/${id}`, { timeout: 30000 })
  return response.data
}
