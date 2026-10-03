import { request } from './request'

const unwrap = (promise) => promise.then((response) => response.data)

export function parsePostImages(images) {
  if (Array.isArray(images)) return images.filter(Boolean)
  if (!images) return []
  if (typeof images === 'string') {
    try {
      const parsed = JSON.parse(images)
      return Array.isArray(parsed) ? parsed.filter(Boolean) : []
    } catch {
      return images.split(',').map((item) => item.trim()).filter(Boolean)
    }
  }
  return []
}

export const getPostList = (params = {}) => unwrap(request({
  url: '/api/forum/posts',
  params,
}))

export const getPostDetail = (postId) => unwrap(request({
  url: `/api/forum/posts/${postId}`,
}))

export const publishPost = (payload) => unwrap(request({
  url: '/api/forum/posts',
  method: 'POST',
  data: payload,
}))

export const getTopicList = (params = {}) => unwrap(request({
  url: '/api/forum/topics',
  params,
}))

export const getHotTopics = (params = {}) => unwrap(request({
  url: '/api/forum/topics/hot',
  params,
}))

export const getForumRules = () => unwrap(request({
  url: '/api/forum/statistics/rules',
}))

export const getCommentList = (params = {}) => unwrap(request({
  url: '/api/forum/comments',
  params,
}))

export const createComment = (payload) => unwrap(request({
  url: '/api/forum/comments',
  method: 'POST',
  data: payload,
}))

export const togglePostLike = (postId) => unwrap(request({
  url: '/api/forum/likes',
  method: 'POST',
  data: { targetId: postId, targetType: 'POST' },
}))

export const togglePostFavorite = (postId) => unwrap(request({
  url: `/api/forum/favorites/${postId}`,
  method: 'POST',
}))

export const getMyFavoritePosts = (params = {}) => unwrap(request({
  url: '/api/forum/favorites/my',
  params,
}))

export const getMyForumPosts = (params = {}) => unwrap(request({
  url: '/api/forum/users/posts/me',
  params,
}))

export const REPORT_REASONS = [
  { value: 1, label: '广告推销' },
  { value: 2, label: '内容不实' },
  { value: 3, label: '人身攻击' },
  { value: 4, label: '违法违规' },
  { value: 5, label: '其他' },
]

export const createReport = (payload) => unwrap(request({
  url: '/api/forum/reports',
  method: 'POST',
  data: payload,
}))
