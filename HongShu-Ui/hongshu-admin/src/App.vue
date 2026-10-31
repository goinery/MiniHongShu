<template>
  <router-view />
</template>

<script setup>
import { onBeforeUnmount, watch } from 'vue'

import useSettingsStore from '@/store/modules/settings'
import { handleThemeStyle } from '@/utils/theme'

const settingsStore = useSettingsStore()
const systemAppearance = window.matchMedia('(prefers-color-scheme: dark)')
function updateSystemAppearance(event) { settingsStore.systemDark = event.matches }
systemAppearance.addEventListener('change', updateSystemAppearance)
onBeforeUnmount(() => systemAppearance.removeEventListener('change', updateSystemAppearance))

watch(() => [settingsStore.theme, settingsStore.isDark], ([theme, isDark]) => {
  document.documentElement.classList.toggle('dark', isDark)
  document.documentElement.style.colorScheme = isDark ? 'dark' : 'light'
  handleThemeStyle(theme, isDark)
}, { immediate: true })

// 只保存用户可编辑的偏好；同步写入，避免修改后立即刷新时丢失设置。
watch(() => ({
  designVersion: 'cf-1',
  topNav: settingsStore.topNav,
  tagsView: settingsStore.tagsView,
  fixedHeader: settingsStore.fixedHeader,
  sidebarLogo: settingsStore.sidebarLogo,
  dynamicTitle: settingsStore.dynamicTitle,
  colorMode: settingsStore.colorMode,
  theme: settingsStore.theme,
}), layoutSetting => {
  try {
    localStorage.setItem('layout-setting', JSON.stringify(layoutSetting))
  } catch {
    console.warn('无法保存界面设置，请检查浏览器本地存储是否可用')
  }
}, { flush: 'sync' })
</script>
