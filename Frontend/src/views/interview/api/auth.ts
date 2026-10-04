import { post, get } from '../utls/request'

// 登录请求参数
export interface LoginParams {
  phone?: string
  email?: string
  password: string
}

// 登录响应数据
export interface LoginResponse {
  id: number
  nickname: string
  phone: string
  email: string
  is_manager: number
  session_token: string
  target_position?: string | null  // 用户配置的目标岗位，面试页可自动填充
}

// 注册请求参数
export interface RegisterParams {
  nickname: string
  phone: string
  password: string
  gender: number
  work_experience_years: number
  education: string
  email: string
  major: string
  graduation_year: number
  school: string
  target_position: string
  skill_tags: string
  resume_url: string
}

// 注册响应数据
export interface RegisterResponse {
  id: number
  nickname: string
  phone: string
  email: string
  created_at: string
}

// 用户完整档案（与数据库字段一致）
export interface UserProfileDetail {
  id: number
  nickname: string
  phone: string
  email: string
  avatar_url: string | null         // 头像链接
  gender: number                    // 0=未知 1=男 2=女
  work_experience_years: number     // 工作经验（年）
  work_experience: string | null    // 工作经历详情
  education: string                 // 学历
  major: string | null              // 专业
  graduation_year: number | null    // 毕业年份
  school: string | null             // 学校
  target_position: string | null    // 目标岗位
  skill_tags: string | null         // 技能标签，逗号分隔
  tech_stack: string | null         // 技术栈，逗号或换行分隔
  resume_url: string | null         // 简历地址
  interview_count: number
  average_score: number
  is_manager: number
}

// 更新用户档案请求参数
export interface UpdateProfileParams {
  id: number
  nickname?: string
  phone?: string
  email?: string
  avatar_url?: string | null
  gender?: number
  work_experience_years?: number
  work_experience?: string | null
  education?: string
  major?: string | null
  graduation_year?: number | null
  school?: string | null
  target_position?: string | null
  skill_tags?: string | null
  tech_stack?: string | null
  resume_url?: string | null
}

export interface InterviewJobPositionItem {
  id: number
  job_position: string
  tech_stack: string
}

export interface ResetPasswordParams {
  id: number
  phone: string
  new_password: string
}

export interface UploadAvatarResponse {
  url: string
  key: string
  bucket: string
  etag: string
}

function redirectToCampusLogin(): Promise<never> {
  window.location.href = '/login'
  return Promise.reject(new Error('请使用校园账号登录'))
}

// 认证 API
export const authApi = {
  /**
   * 面试独立登录已停用，统一走知航校园登录
   */
  login: (_params: LoginParams): Promise<LoginResponse> => {
    return redirectToCampusLogin()
  },

  /**
   * 面试独立注册已停用，统一走知航校园登录
   */
  register: (_params: Partial<RegisterParams>): Promise<RegisterResponse> => {
    return redirectToCampusLogin()
  },

  /**
   * 获取用户完整档案（面试 profile 接口）
   * @param userId 用户 ID
   * @returns 用户完整档案
   */
  getProfile: (userId: number): Promise<UserProfileDetail> => {
    return get<UserProfileDetail>('/user/detail', { params: { id: userId } })
  },

  /**
   * 通过 session 获取当前登录用户档案（面试 profile 接口）
   */
  getCurrentProfile: (): Promise<UserProfileDetail> => {
    return get<UserProfileDetail>('/user/detail')
  },

  /**
   * 更新用户档案
   * @param params 更新参数（必须包含 id）
   * @returns 更新后的用户信息
   */
  updateProfile: (params: UpdateProfileParams): Promise<any> => {
    return post<any>('/user/update', params)
  },

  /**
   * 获取可选面试岗位列表（来自岗位技术栈表）
   */
  getInterviewJobPositions: (): Promise<{ items: InterviewJobPositionItem[] }> => {
    return get<{ items: InterviewJobPositionItem[] }>('/user/interview-job-positions')
  },

  /**
   * 重置密码（后端会校验手机号与用户是否匹配）
   */
  resetPassword: (params: ResetPasswordParams): Promise<{ message: string; nickname?: string }> => {
    return post<{ message: string; nickname?: string }>('/user/reset-password', params)
  },

  /**
   * 上传头像图片到 COS，返回可访问 URL
   */
  uploadAvatar: (file: File): Promise<UploadAvatarResponse> => {
    const formData = new FormData()
    formData.append('file', file)
    return post<UploadAvatarResponse>('/user/upload-avatar', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
  },
}

// 为了保持向后兼容，保留 loginApi 别名
export const loginApi = authApi
