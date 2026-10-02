import { request } from './request'

export const getCampusCourses = () => request({ url: '/api/app/campus-courses' })
export const getCampusCourse = (id) => request({ url: `/api/app/campus-courses/${id}` })
export const enrollCampusCourse = (courseId) => request({
  url: `/api/app/campus-courses/${courseId}/enroll`,
  method: 'POST',
})

export const unenrollCampusCourse = (courseId) => request({
  url: `/api/app/campus-courses/${courseId}/enroll`,
  method: 'DELETE',
})

export const updateCampusCourseProgress = (courseId, chapterId, completed) => request({
  url: `/api/app/campus-courses/${courseId}/chapters/${chapterId}/progress`,
  method: 'PUT',
  data: { completed },
})
