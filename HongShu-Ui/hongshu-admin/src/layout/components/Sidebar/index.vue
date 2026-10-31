<template>
  <aside ref="sidebarRef" class="console-sidebar" :class="[sideTheme, { 'is-previewing': isPreviewing, 'is-compact': contentCollapsed }]" @pointerenter="handlePointerEnter" @pointerleave="handlePointerLeave" @pointerdown="handlePointerDown" @focusin="handleFocusIn" @focusout="handleFocusOut">
    <div class="sidebar-content">
    <logo v-if="showLogo" :collapse="contentCollapsed" @overlay-change="setOverlayOpen('account', $event)" />
    <div class="sidebar-search"><header-search :collapsed="contentCollapsed" @overlay-change="setOverlayOpen('search', $event)" /></div>
    <el-scrollbar class="sidebar-menu-scroll" wrap-class="scrollbar-wrapper">
      <el-menu :default-active="activeMenu" :collapse="contentCollapsed" :unique-opened="true" :collapse-transition="false" mode="vertical">
        <sidebar-item v-for="(item, index) in sidebarRouters" :key="item.path + index" :item="item" :base-path="item.path" />
      </el-menu>
    </el-scrollbar>
    <div class="sidebar-footer">
      <button type="button" class="sidebar-collapse" :aria-label="toggleLabel" :title="toggleLabel" :aria-expanded="appStore.sidebar.opened" @click="appStore.toggleSideBar()">
        <svg width="16" height="16" viewBox="0 0 20 20" fill="none" aria-hidden="true"><rect x="2.5" y="3" width="15" height="14" rx="2" stroke="currentColor"/><path d="M7 3v14" stroke="currentColor"/><path v-if="!appStore.sidebar.opened" d="m11 7 3 3-3 3" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round"/></svg>
      </button>
    </div>
    </div>
  </aside>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import Logo from './Logo'
import SidebarItem from './SidebarItem'
import HeaderSearch from '@/components/HeaderSearch'
import useAppStore from '@/store/modules/app'
import useSettingsStore from '@/store/modules/settings'
import usePermissionStore from '@/store/modules/permission'
const route = useRoute()
const appStore = useAppStore()
const settingsStore = useSettingsStore()
const permissionStore = usePermissionStore()
const sidebarRouters = computed(() => permissionStore.sidebarRouters)
const showLogo = computed(() => settingsStore.sidebarLogo)
const sideTheme = computed(() => settingsStore.sideTheme)
const sidebarRef = ref(null)
const pointerInside = ref(false)
const keyboardFocusInside = ref(false)
const revealSuppressed = ref(false)
const overlayLocks = reactive({ account: false, search: false })
const canPreview = computed(() => appStore.device === 'desktop' && !appStore.sidebar.opened && !appStore.sidebar.hide)
const isPreviewing = computed(() => canPreview.value && appStore.sidebar.hovered)
const isCollapse = computed(() => !appStore.sidebar.opened && !isPreviewing.value)
const contentCollapsed = isCollapse
const toggleLabel = computed(() => appStore.sidebar.opened ? '收起侧栏' : isPreviewing.value ? '固定展开侧栏' : '展开侧栏')
const activeMenu = computed(() => route.meta.activeMenu || route.path)
let closeTimer
let disposed = false

function wantsPreview() {
  return overlayLocks.account || overlayLocks.search || (!revealSuppressed.value && (pointerInside.value || keyboardFocusInside.value))
}

function updatePreview(immediate = false) {
  clearTimeout(closeTimer)
  if (disposed || !canPreview.value) {
    appStore.setSidebarHovered(false)
  } else if (wantsPreview()) {
    appStore.setSidebarHovered(true)
  } else if (immediate) {
    appStore.setSidebarHovered(false)
  } else {
    closeTimer = setTimeout(() => {
      if (!wantsPreview()) appStore.setSidebarHovered(false)
    }, 80)
  }
}

function handlePointerEnter(event) {
  if (event.pointerType === 'touch') return
  pointerInside.value = true
  revealSuppressed.value = false
  updatePreview()
}

function handlePointerLeave() {
  pointerInside.value = false
  revealSuppressed.value = false
  updatePreview()
}

function handlePointerDown() {
  // A mouse click may leave a button focused; it must not pin the hover preview.
  keyboardFocusInside.value = false
}

function handleFocusIn(event) {
  keyboardFocusInside.value = event.target instanceof Element && event.target.matches(':focus-visible')
  if (keyboardFocusInside.value) revealSuppressed.value = false
  updatePreview()
}

async function handleFocusOut() {
  await nextTick()
  const focused = document.activeElement
  keyboardFocusInside.value = Boolean(sidebarRef.value?.contains(focused) && focused?.matches(':focus-visible'))
  updatePreview()
}

function setOverlayOpen(kind, opened) {
  // Teleported menus keep an existing expansion alive until their close animation ends.
  overlayLocks[kind] = opened && !isCollapse.value
  updatePreview()
}

watch(() => [appStore.sidebar.opened, appStore.device, appStore.sidebar.hide], ([opened, device, hidden], previous) => {
  if (device !== 'desktop' || hidden) {
    pointerInside.value = false
    keyboardFocusInside.value = false
    revealSuppressed.value = false
  } else if (previous && previous[0] && !opened) {
    // An explicit collapse stays collapsed until a new pointer entry or keyboard focus.
    revealSuppressed.value = true
  } else if (opened) {
    revealSuppressed.value = false
  }
  updatePreview(true)
})

onBeforeUnmount(() => {
  disposed = true
  clearTimeout(closeTimer)
  appStore.setSidebarHovered(false)
})
</script>
