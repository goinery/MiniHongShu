/** @type {string} */
export const brandName = String(import.meta.env.VITE_APP_BRAND_NAME || '').trim() || '测试'
export const brandMonogram = [...brandName][0]
export const communityName = `${brandName}社区`
export const adminTitle = `${brandName}管理端`
export const consoleTitle = `${brandName}管理控制台`
