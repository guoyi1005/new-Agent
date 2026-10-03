import request from '../utils/request'

// ========== 校友企业 ==========

export const getAlumniEnterpriseList = (params = {}) =>
  request({
    url: '/api/admin/employment/alumni',
    method: 'get',
    params,
  })

export const createAlumniEnterprise = (data) =>
  request({
    url: '/api/admin/employment/alumni',
    method: 'post',
    data,
  })

export const updateAlumniEnterprise = (id, data) =>
  request({
    url: `/api/admin/employment/alumni/${id}`,
    method: 'put',
    data,
  })

export const deleteAlumniEnterprise = (id) =>
  request({
    url: `/api/admin/employment/alumni/${id}`,
    method: 'delete',
  })

// ========== 校园招聘 ==========

export const getCampusRecruitmentList = (params = {}) =>
  request({
    url: '/api/admin/employment/campus-recruitments',
    method: 'get',
    params,
  })

export const createCampusRecruitment = (data) =>
  request({
    url: '/api/admin/employment/campus-recruitments',
    method: 'post',
    data,
  })

export const updateCampusRecruitment = (id, data) =>
  request({
    url: `/api/admin/employment/campus-recruitments/${id}`,
    method: 'put',
    data,
  })

export const deleteCampusRecruitment = (id) =>
  request({
    url: `/api/admin/employment/campus-recruitments/${id}`,
    method: 'delete',
  })
