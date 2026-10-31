const apiPrefix = import.meta.env.VITE_APP_BASE_API;

// OSS 返回完整 HTTPS 地址；旧本地资源仍通过 API 代理访问。
export function resolveFileUrl(value) {
  if (!value) return "";
  if (value === "/default-avatar.png") return value;
  if (/^(https?:\/\/|\/\/|blob:|data:)/i.test(value) || value.startsWith(apiPrefix + "/")) return value;
  return apiPrefix + value;
}

export function storedFileUrl(value) {
  return value.startsWith(apiPrefix + "/") ? value.slice(apiPrefix.length) : value;
}
