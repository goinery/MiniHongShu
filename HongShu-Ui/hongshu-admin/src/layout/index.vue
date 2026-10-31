<template>
  <div :class="classObj" class="app-wrapper" :style="{ '--current-color': theme }">
    <div v-if="!topNav && device === 'mobile' && sidebar.opened" class="drawer-bg" @click="handleClickOutside"/>
    <sidebar v-if="!topNav && !sidebar.hide" class="sidebar-container" />
    <div :class="{ hasTagsView: needTagsView, sidebarHide: topNav || sidebar.hide }" class="main-container">
      <div :class="{ 'fixed-header': fixedHeader }">
        <navbar @setLayout="setLayout" />
        <tags-view v-if="needTagsView" />
      </div>
      <app-main />
      <settings ref="settingRef" />
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import { useWindowSize } from '@vueuse/core'
import Sidebar from './components/Sidebar/index.vue'
import { AppMain, Navbar, Settings, TagsView } from './components'

import useAppStore from '@/store/modules/app'
import useSettingsStore from '@/store/modules/settings'

const settingsStore = useSettingsStore()
const appStore = useAppStore()
const route = useRoute()
const theme = computed(() => settingsStore.theme);
const sidebar = computed(() => appStore.sidebar);
const device = computed(() => appStore.device);
const needTagsView = computed(() => settingsStore.tagsView);
const fixedHeader = computed(() => settingsStore.fixedHeader);
const topNav = computed(() => settingsStore.topNav);

const classObj = computed(() => ({
  hideSidebar: !topNav.value && !sidebar.value.opened,
  openSidebar: !topNav.value && sidebar.value.opened,
  hoverSidebar: !topNav.value && device.value === 'desktop' && !sidebar.value.opened && sidebar.value.hovered,
  topNavigation: topNav.value,
  withoutAnimation: sidebar.value.withoutAnimation,
  mobile: device.value === 'mobile'
}))

const { width } = useWindowSize();
const WIDTH = 992; // refer to Bootstrap's responsive design

watch(width, (windowWidth) => {
  const nextDevice = windowWidth < WIDTH ? 'mobile' : 'desktop'
  if (nextDevice !== device.value) {
    appStore.toggleDevice(nextDevice)
    if (nextDevice === 'mobile' && !topNav.value) appStore.closeSideBar({ withoutAnimation: true })
  }
}, { immediate: true })

watch(() => route.fullPath, () => {
  if (!topNav.value && device.value === 'mobile' && sidebar.value.opened) {
    appStore.closeSideBar({ withoutAnimation: false })
  }
})

watch(topNav, enabled => {
  appStore.setSidebarHovered(false)
  if (!enabled && device.value === 'mobile') appStore.closeSideBar({ withoutAnimation: true })
})

function handleClickOutside() {
  appStore.closeSideBar({ withoutAnimation: false })
}

const settingRef = ref(null);
function setLayout() {
  settingRef.value.openSetting();
}
</script>

<style lang="scss" scoped>
  @import "@/assets/styles/mixin.scss";

.app-wrapper {
  @include clearfix;
  position: relative;
  height: 100%;
  width: 100%;

  &.mobile.openSidebar {
    position: fixed;
    top: 0;
  }
}

.drawer-bg {
  background: #000;
  opacity: 0.3;
  width: 100%;
  top: 0;
  height: 100%;
  position: absolute;
  z-index: 999;
}

.fixed-header {
  position: fixed;
  top: 0;
  right: 0;
    z-index: 100;
  width: calc(100% - var(--sidebar-layout-width));
  transition: width var(--sidebar-motion-duration) var(--sidebar-motion-easing);
}

@media (max-width: 900px) {
  .topNavigation :deep(.app-main) { min-height: calc(100vh - 100px); }
  .topNavigation :deep(.fixed-header + .app-main) { padding-top: 100px; }
  .topNavigation :deep(.hasTagsView .app-main) { min-height: calc(100vh - 148px); }
  .topNavigation :deep(.hasTagsView .fixed-header + .app-main) { padding-top: 148px; }
}
</style>
