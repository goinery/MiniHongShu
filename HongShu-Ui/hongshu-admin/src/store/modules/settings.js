
import { defineStore } from 'pinia'
import defaultSettings from '@/settings'
import { useDynamicTitle } from '@/utils/dynamicTitle'
import { normalizeThemeColor } from '@/utils/theme'

const { colorMode, showSettings, topNav, tagsView, fixedHeader, sidebarLogo, dynamicTitle } = defaultSettings

let storageSetting = {}
try {
  storageSetting = JSON.parse(localStorage.getItem('layout-setting')) || {}
} catch {
  storageSetting = {}
}
// Adopt the new console defaults once while retaining unrelated preferences.
const currentDesign = storageSetting.designVersion === 'cf-1'
const savedColorMode = ['light', 'dark', 'system'].includes(storageSetting.colorMode)
  ? storageSetting.colorMode
  : currentDesign && storageSetting.sideTheme
    ? (storageSetting.sideTheme === 'theme-dark' ? 'dark' : 'light')
    : colorMode

const useSettingsStore = defineStore(
  'settings',
  {
    state: () => ({
      title: '',
      theme: normalizeThemeColor(currentDesign ? storageSetting.theme : undefined),
      colorMode: savedColorMode,
      systemDark: window.matchMedia('(prefers-color-scheme: dark)').matches,
      showSettings: showSettings,
      topNav: currentDesign && storageSetting.topNav !== undefined ? storageSetting.topNav : topNav,
      tagsView: storageSetting.tagsView === undefined ? tagsView : storageSetting.tagsView,
      fixedHeader: currentDesign && storageSetting.fixedHeader !== undefined ? storageSetting.fixedHeader : fixedHeader,
      sidebarLogo: currentDesign && storageSetting.sidebarLogo !== undefined ? storageSetting.sidebarLogo : sidebarLogo,
      dynamicTitle: storageSetting.dynamicTitle === undefined ? dynamicTitle : storageSetting.dynamicTitle
    }),
    getters: {
      isDark: state => state.colorMode === 'dark' || (state.colorMode === 'system' && state.systemDark),
      sideTheme() { return this.isDark ? 'theme-dark' : 'theme-light' }
    },
    actions: {
      // 修改布局设置
      changeSetting(data) {
        const { key, value } = data
        if (Object.prototype.hasOwnProperty.call(this, key)) {
          this[key] = value
        }
      },
      // 设置网页标题
      setTitle(title) {
        this.title = title
        useDynamicTitle();
      }
    }
  })

export default useSettingsStore
