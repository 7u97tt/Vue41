/**
 * 浏览器内用 URL 拼绝对地址，避免在 /login 页用相对路径变成 /login/api/...
 * 未配置 VITE_API_BASE 时走当前站点 origin（Vite /api 代理）
 */
function resolveApiBase() {
  const raw = import.meta.env.VITE_API_BASE
  if (typeof raw === 'string' && raw.trim() !== '') {
    return raw.trim().replace(/\/$/, '')
  }
  if (typeof window !== 'undefined' && window.location?.origin) {
    return window.location.origin
  }
  return ''
}

export function getApiBase() {
  return resolveApiBase()
}

export function apiUrl(path) {
  const normalized = path.startsWith('/') ? path : `/${path}`
  const base = resolveApiBase()
  if (base) {
    return new URL(normalized, base).href
  }
  if (typeof window !== 'undefined' && window.location?.origin) {
    return new URL(normalized, window.location.origin).href
  }
  return normalized
}
