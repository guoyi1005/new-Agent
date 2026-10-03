import request from '../utils/request'

const base = '/api/admin/external-courses'

export const getExternalCourseList = () => request.get(base, {
  skipGlobalErrorMessage: true,
})

export const getExternalCourseSkillOptions = () => request.get(`${base}/skills`, {
  skipGlobalErrorMessage: true,
})

export const createExternalCourse = (data) => request.post(base, data, {
  skipGlobalErrorMessage: true,
})

export const updateExternalCourse = (id, data) => request.put(`${base}/${id}`, data, {
  skipGlobalErrorMessage: true,
})

export const changeExternalCourseStatus = (id, status) => request.post(`${base}/${id}/status`, null, {
  skipGlobalErrorMessage: true,
  params: { status },
})

export const deleteExternalCourse = (id) => request.delete(`${base}/${id}`, {
  skipGlobalErrorMessage: true,
})
