import axios from 'axios'
import { resolveApiBase } from '../../../config/androidApi'

// 错误码翻译映射
const errorCodeMap = {
  missing_or_empty_field: '缺少必填字段',
  invalid_gender: '性别格式不正确',
  invalid_education: '学历格式不正确',
  invalid_graduation_year: '毕业年份格式不正确',
  invalid_work_experience_years: '工作年限格式不正确',
  invalid_interview_count: '面试次数格式不正确',
  invalid_average_score: '平均分数格式不正确',
  forbidden_field: '包含不允许修改的字段',
  invalid_file_type: '文件格式不正确，请上传图片文件',
  file_too_large: '文件过大，请上传 5MB 以内图片',
  cos_upload_failed: '头像上传失败，请稍后重试',
  missing_cos_credentials: '服务器未配置对象存储凭证',
  missing_bucket: '服务器对象存储 Bucket 未配置',
  cos_sdk_not_installed: '服务器缺少 COS SDK 依赖',
  duplicate_phone_or_email: '手机号或邮箱已被注册',
  invalid_credentials: '账号或密码错误',
  not_found: '用户不存在',
  ai_grade_failed: 'AI判题失败，请稍后重试',
  save_grade_result_failed: '判题结果保存失败，请稍后重试',
  invalid_or_expired_session: '登录状态已失效，请重新登录',
  chroma_query_failed: '向量数据库查询失败，请确认 ChromaDB 已启动且端口配置正确',
  forbidden: '没有权限访问该资源',
  unknown_error: '未知错误',
  server_error: '服务器内部错误',
}

function translateErrorCode(code) {
  return errorCodeMap[code] || code
}

function clearInterviewTokens() {
  localStorage.removeItem('session_token')
  localStorage.removeItem('token')
  localStorage.removeItem('user_id')
  localStorage.removeItem('is_manager')
  localStorage.removeItem('nickname')
}

const baseURL = resolveApiBase()

const request = axios.create({
  baseURL,
  timeout: 10000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
})

request.interceptors.request.use(
  (config) => {
    const token =
      localStorage.getItem('token') ||
      localStorage.getItem('session_token') ||
      ''
    if (token) {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${token}`
      config.headers['X-Session-Token'] = token
    }
    return config
  },
  (error) => Promise.reject(error),
)

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.code === 'ECONNABORTED') {
      const requestUrl = String(error.config?.url || '')
      const isGradeRequest = requestUrl.includes('/knowledge/grade')
      error.message = isGradeRequest
        ? '请求超时，AI判题耗时较长，请稍后重试'
        : '请求超时，请稍后重试'
      console.error(error.message)
      return Promise.reject(error)
    }

    if (error.response) {
      const { status, data } = error.response

      let errorMessage = '请求失败'
      if (data) {
        if (data.error) {
          errorMessage = translateErrorCode(data.error)
          if (data.field) {
            errorMessage += `: ${data.field}`
          }
        } else if (data.message) {
          errorMessage = data.message
        }
      }

      switch (status) {
        case 401:
          clearInterviewTokens()
          window.location.href = '/login'
          break
        case 403:
          console.error('没有权限访问该资源:', errorMessage)
          break
        case 404:
          console.error('请求的资源不存在:', errorMessage)
          break
        case 500:
          console.error('服务器内部错误:', errorMessage)
          break
        default:
          console.error(errorMessage)
      }

      error.message = errorMessage
    } else if (error.request) {
      console.error('网络请求失败，请检查网络连接')
    } else {
      console.error('请求配置错误:', error.message)
    }

    return Promise.reject(error)
  },
)

export const get = (url, config) => request.get(url, config)
export const post = (url, data, config) => request.post(url, data, config)
export const put = (url, data, config) => request.put(url, data, config)
export const del = (url, config) => request.delete(url, config)
export const patch = (url, data, config) => request.patch(url, data, config)

export default request
