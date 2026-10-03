import { request } from './request'

/** 校友企业：由管理端维护，学生端只读取展示中的企业 */
export function getEmploymentAlumni() {
  return request({ url: '/api/app/employment/alumni' })
}

/** 校园招聘汇总：招聘季、宣讲会/双选会场次、岗位数与近期日程 */
export function getCampusRecruitmentSummary() {
  return request({ url: '/api/app/employment/campus-recruitment-summary' })
}

/** 校园招聘明细：宣讲会、双选会与校招企业岗位 */
export function getCampusRecruitments() {
  return request({ url: '/api/app/employment/campus-recruitments' })
}
