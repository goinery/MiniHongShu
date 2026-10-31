<template>
  <el-drawer v-model="showSettings" :withHeader="false" direction="rtl" size="300px" @close="themeColorPicker?.close()">
    <h3 class="drawer-title">外观设置</h3>
    <div class="appearance-settings">
      <span>主题</span>
      <div class="theme-setting-controls">
        <theme-color-picker ref="themeColorPicker" :model-value="theme" :default-color="DEFAULT_THEME_COLOR" :predefine="predefineColors" @change="themeChange" />
        <div class="appearance-toggle" role="group" aria-label="外观模式">
          <button type="button" class="appearance-mode" :class="{ active: settingsStore.colorMode === 'light' }" :aria-pressed="settingsStore.colorMode === 'light'" aria-label="浅色模式" title="浅色模式" @click="settingsStore.colorMode = 'light'"><Sunny /></button>
          <button type="button" class="appearance-mode" :class="{ active: settingsStore.colorMode === 'dark' }" :aria-pressed="settingsStore.colorMode === 'dark'" aria-label="深色模式" title="深色模式" @click="settingsStore.colorMode = 'dark'"><Moon /></button>
          <button type="button" class="appearance-mode" :class="{ active: settingsStore.colorMode === 'system' }" :aria-pressed="settingsStore.colorMode === 'system'" aria-label="跟随系统" title="跟随系统" @click="settingsStore.colorMode = 'system'"><Monitor /></button>
        </div>
      </div>
    </div>
    <el-divider />

    <h3 class="drawer-title">系统布局配置</h3>

    <div class="drawer-item">
      <span>顶部导航</span>
      <span class="comp-style">
        <el-switch v-model="settingsStore.topNav" @change="topNavChange" class="drawer-switch" />
      </span>
    </div>

    <div class="drawer-item">
      <span>显示页面标签</span>
      <span class="comp-style">
        <el-switch v-model="settingsStore.tagsView" class="drawer-switch" />
      </span>
    </div>

    <div class="drawer-item">
      <span>固定顶栏</span>
      <span class="comp-style">
        <el-switch v-model="settingsStore.fixedHeader" class="drawer-switch" />
      </span>
    </div>

    <div class="drawer-item">
      <span>显示品牌标识</span>
      <span class="comp-style">
        <el-switch v-model="settingsStore.sidebarLogo" class="drawer-switch" />
      </span>
    </div>

    <div class="drawer-item">
      <span>动态标题</span>
      <span class="comp-style">
        <el-switch v-model="settingsStore.dynamicTitle" class="drawer-switch" />
      </span>
    </div>

    <el-divider />

    <el-button plain icon="Refresh" @click="resetSetting">重置配置</el-button>
  </el-drawer>

</template>

<script setup>
import { Monitor, Moon, Sunny } from '@element-plus/icons-vue'

import { computed, getCurrentInstance, ref } from 'vue'
import ThemeColorPicker from './ThemeColorPicker.vue'

import useAppStore from '@/store/modules/app'
import useSettingsStore from '@/store/modules/settings'

import { DEFAULT_THEME_COLOR, normalizeThemeColor } from '@/utils/theme'

const { proxy } = getCurrentInstance();
const appStore = useAppStore()
const settingsStore = useSettingsStore()

const showSettings = ref(false);
const themeColorPicker = ref();
const theme = computed(() => settingsStore.theme);

const predefineColors = ref([DEFAULT_THEME_COLOR, "#2563eb", "#f6821f", "#18794e", "#6e56cf", "#41454b"]);

function topNavChange() {
  appStore.toggleSideBarHide(false);
  appStore.setSidebarHovered(false);
}
function themeChange(val) {
  settingsStore.theme = normalizeThemeColor(val);
}
function resetSetting() {
  proxy.$modal.loading("正在清除设置缓存并刷新，请稍候...");
  localStorage.removeItem("layout-setting")
  window.location.reload()
}
function openSetting() {
  showSettings.value = true;
}

defineExpose({
  openSetting,
})
</script>

<style lang='scss' scoped>
.drawer-title { margin: 0 0 16px; color: var(--cf-text); font-size: 14px; font-weight: 500; }
.appearance-settings { display: flex; align-items: center; justify-content: space-between; gap: 8px; min-height: 44px; background: transparent; font-size: 12px; color: var(--el-text-color-regular); }
.appearance-settings > span { flex-shrink: 0; }
.theme-setting-controls { display: inline-flex; align-items: center; flex-shrink: 0; gap: 6px; }
.appearance-toggle { display: inline-flex; flex-shrink: 0; padding: 2px; border-radius: 999px; background: var(--el-fill-color-light); }
.appearance-mode { display: inline-flex; align-items: center; justify-content: center; width: 28px; height: 28px; padding: 0; border: 0; border-radius: 999px; background: transparent; color: var(--el-text-color-secondary); cursor: pointer; transition: background-color 160ms, color 160ms, box-shadow 160ms; }
.appearance-mode svg { width: 16px; height: 16px; }
.appearance-mode:hover { color: var(--el-text-color-primary); }
.appearance-mode.active { background: var(--el-bg-color-overlay); color: var(--el-text-color-primary); box-shadow: 0 1px 4px #00000012; }
.appearance-mode:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 2px; }
:global(html.dark) .appearance-mode.active { background: var(--el-fill-color-dark); box-shadow: none; }
.drawer-item {
  color: var(--el-text-color-regular);
  padding: 12px 0;
  font-size: 14px;

  .comp-style {
    float: right;
    margin: -3px 8px 0px 0px;
  }
}

@media (prefers-reduced-motion: reduce) { .appearance-mode { transition: none; } }
</style>
