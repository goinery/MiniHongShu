import defaultAvatar from '@/assets/images/profile.jpg'
import { resolveFileUrl } from '@/utils/fileUrl'

export { defaultAvatar }

export function resolveAvatarUrl(value) {
  const avatar = typeof value === 'string' ? value.trim() : ''
  return !avatar || avatar === defaultAvatar ? defaultAvatar : resolveFileUrl(avatar)
}

// Keep a failed remote image from leaving a broken image in profile or navigation.
export function handleAvatarError(event) {
  const image = event?.target
  if (image && image.getAttribute('src') !== defaultAvatar) image.src = defaultAvatar
}

export function normalizeExternalAvatarUrl(value) {
  const input = typeof value === 'string' ? value.trim() : ''
  if (!/^https?:\/\//i.test(input) || /[\s\\]/.test(input)) {
    throw new Error('请输入有效的 HTTP 或 HTTPS 图片地址')
  }
  let url
  try { url = new URL(input) } catch {
    throw new Error('请输入有效的 HTTP 或 HTTPS 图片地址')
  }
  if (!url.hostname || url.username || url.password) {
    throw new Error('图片地址不能包含用户名或密码')
  }
  const normalized = url.href
  if (normalized.length > 512) throw new Error('图片地址不能超过 512 个字符')
  return normalized
}
