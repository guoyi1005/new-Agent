import { request } from './request'

const unwrap = (promise) => promise.then((response) => response.data)
const encode = (value) => encodeURIComponent(String(value))

/** 我保存过的 Python 练习卷 */
export const listPythonPapers = () => unwrap(request({
  url: '/api/app/learning/python/papers',
}))

/** 单份试卷详情 */
export const getPythonPaper = (id) => unwrap(request({
  url: `/api/app/learning/python/papers/${encode(id)}`,
}))

/** 保存一份试卷（只存题目 ID 快照） */
export const savePythonPaper = (data) => unwrap(request({
  url: '/api/app/learning/python/papers',
  method: 'POST',
  data,
}))

/** 删除一份试卷 */
export const deletePythonPaper = (id) => unwrap(request({
  url: `/api/app/learning/python/papers/${encode(id)}`,
  method: 'DELETE',
}))
