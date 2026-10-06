export const isAndroidApp = import.meta.env.VITE_ANDROID_APP === '1'

const STORAGE_KEY = 'zhihang.android.apiOrigin'
const defaultWebOrigin = import.meta.env.VITE_API_BASE_URL

export function normalizeApiOrigin(value) {
  const input = String(value || '').trim()
  let url
  try {
    url = new URL(input)
  } catch {
    throw new Error('请输入完整地址，例如 https://example.com 或 http://192.168.1.5:8080')
  }
  if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password || url.search || url.hash) {
    throw new Error('仅支持不含账号、参数的 HTTP 或 HTTPS 地址')
  }
  if (!['/', '/api', '/api/'].includes(url.pathname)) {
    throw new Error('请填写服务器根地址，不要附加页面路径')
  }
  if (url.protocol === 'http:') {
    const host = url.hostname.toLowerCase()
    const privateNetwork = /^10\./.test(host) || /^192\.168\./.test(host) || /^172\.(1[6-9]|2\d|3[01])\./.test(host)
    if (!privateNetwork) throw new Error('HTTP 仅供局域网调试；公网服务器请使用 HTTPS')
  }
  return url.origin
}

export function getStoredApiOrigin() {
  if (!isAndroidApp || typeof localStorage === 'undefined') return ''
  try {
    return normalizeApiOrigin(localStorage.getItem(STORAGE_KEY))
  } catch {
    return ''
  }
}

export function saveApiOrigin(value) {
  const origin = normalizeApiOrigin(value)
  localStorage.setItem(STORAGE_KEY, origin)
  return origin
}

export function resolveApiOrigin() {
  if (isAndroidApp) return getStoredApiOrigin()
  return defaultWebOrigin == null ? 'http://localhost:8080' : String(defaultWebOrigin).replace(/\/+$/, '')
}

export function resolveApiBase() {
  const origin = resolveApiOrigin()
  return origin ? `${origin}/api` : '/api'
}
