import request from '../utils/request'

const base = '/api/admin/learning-taxonomy'

// 技能字典
export const getSkillList = () => request.get(`${base}/skills`, { skipGlobalErrorMessage: true })
export const getSkillOptions = () => request.get(`${base}/skill-options`, { skipGlobalErrorMessage: true })
export const createSkill = (data) => request.post(`${base}/skills`, data, { skipGlobalErrorMessage: true })
export const updateSkill = (id, data) => request.put(`${base}/skills/${id}`, data, { skipGlobalErrorMessage: true })
export const deleteSkill = (id) => request.delete(`${base}/skills/${id}`, { skipGlobalErrorMessage: true })

// 岗位技能要求
export const getTaxonomyJobs = () => request.get(`${base}/jobs`, { skipGlobalErrorMessage: true })
export const getRequirementList = (jobCode) => request.get(`${base}/requirements`, {
  skipGlobalErrorMessage: true,
  params: jobCode ? { jobCode } : undefined,
})
export const createRequirement = (data) => request.post(`${base}/requirements`, data, { skipGlobalErrorMessage: true })
export const updateRequirement = (id, data) => request.put(`${base}/requirements/${id}`, data, { skipGlobalErrorMessage: true })
export const deleteRequirement = (id) => request.delete(`${base}/requirements/${id}`, { skipGlobalErrorMessage: true })

// 岗位实战任务
export const getProjectList = () => request.get(`${base}/projects`, { skipGlobalErrorMessage: true })
export const createProject = (data) => request.post(`${base}/projects`, data, { skipGlobalErrorMessage: true })
export const updateProject = (id, data) => request.put(`${base}/projects/${id}`, data, { skipGlobalErrorMessage: true })
export const changeProjectStatus = (id, status) => request.post(`${base}/projects/${id}/status`, null, {
  skipGlobalErrorMessage: true,
  params: { status },
})
export const deleteProject = (id) => request.delete(`${base}/projects/${id}`, { skipGlobalErrorMessage: true })
