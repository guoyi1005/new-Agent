import { request } from './request'

/** 成都本地就业岗位：数据由后端每天定时从公共招聘网站抓取。 */
export function getLocalJobSummary() {
  return request({ url: '/api/app/local-jobs/summary' })
}

export function listLocalJobs(params = {}) {
  return request({ url: '/api/app/local-jobs', params })
}

/** 手动触发一次抓取，一般不需要在页面里调用。 */
export function refreshLocalJobs() {
  return request({ url: '/api/app/local-jobs/refresh', method: 'POST' })
}
