<template>
  <el-menu
    ref="menuRef"
    class="workspace-topnav"
    :default-active="activeMenu"
    mode="horizontal"
    :menu-trigger="appStore.device === 'mobile' ? 'click' : 'hover'"
    :ellipsis="true"
    :ellipsis-icon="OverflowIcon"
    :show-timeout="70"
    :hide-timeout="120"
    :popper-offset="5"
    popper-class="workspace-topnav-popper"
    popper-effect="light"
    close-on-click-outside
    aria-label="工作空间导航"
    @select="handleSelect"
  >
    <top-nav-item v-for="item in topMenus" :key="item.id" :item="item" />
  </el-menu>
</template>

<script setup>
import { computed, defineComponent, h, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getNormalPath } from '@/utils/ruoyi'
import { isExternal } from '@/utils/validate'
import useAppStore from '@/store/modules/app'
import usePermissionStore from '@/store/modules/permission'
import TopNavItem from './TopNavItem.vue'

const appStore = useAppStore()
const permissionStore = usePermissionStore()
const route = useRoute()
const router = useRouter()
const menuRef = ref(null)
const OverflowIcon = defineComponent({
  name: 'TopNavOverflowIcon',
  setup: () => () => h('span', { class: 'topnav-overflow-label', title: '更多菜单', 'aria-label': '更多菜单' }, '更多 ···'),
})

function resolvePath(path, basePath) {
  if (isExternal(path)) return path
  if (isExternal(basePath)) return basePath
  return getNormalPath(path.startsWith('/') ? path : `${basePath}/${path}`) || '/'
}

function collectMenus(routes, basePath = '', trail = []) {
  return routes.flatMap((item, index) => {
    if (item.hidden) return []
    const position = [...trail, index]
    const path = resolvePath(item.path || '', basePath)
    const children = collectMenus(item.children || [], path, position)
    if (!item.meta?.title) return children
    if (children.length === 1 && !item.alwaysShow) {
      return [{ ...children[0], icon: children[0].icon || item.meta.icon, query: children[0].query ?? item.query }]
    }
    return [{
      id: `topnav-${position.join('-')}`,
      path,
      title: item.meta.title,
      icon: item.meta.icon,
      query: item.query,
      children,
    }]
  })
}

const topMenus = computed(() => collectMenus(permissionStore.topbarRouters))
const destinations = computed(() => {
  const entries = new Map()
  function visit(items) {
    items.forEach(item => {
      if (item.children.length) visit(item.children)
      else entries.set(item.id, item)
    })
  }
  visit(topMenus.value)
  return entries
})
const activeMenu = computed(() => {
  const path = route.meta.activeMenu || route.path
  return [...destinations.value.values()].find(item => item.path === path)?.id || ''
})

function handleSelect(index) {
  const item = destinations.value.get(index)
  if (!item) return
  if (isExternal(item.path)) {
    window.open(item.path, '_blank', 'noopener,noreferrer')
    return
  }
  let query
  try { query = typeof item.query === 'string' ? JSON.parse(item.query) : item.query } catch { query = undefined }
  router.push({ path: item.path, query })
}

async function resizeMenu() {
  await nextTick()
  menuRef.value?.handleResize()
}
watch(topMenus, resizeMenu)
onMounted(resizeMenu)
</script>

<style lang="scss">
.workspace-topnav:is(.el-menu--horizontal) {
  --el-menu-horizontal-height: 56px;
  --el-menu-bg-color: transparent;
  --el-menu-hover-bg-color: var(--cf-hover);
  --el-menu-text-color: var(--cf-text);
  --el-menu-active-color: var(--el-color-primary);
  flex: 1;
  min-width: 0;
  width: 100%;
  height: 56px;
  border: 0;
  padding: 0;
  background: transparent;
}
.workspace-topnav:is(.el-menu--horizontal) > .el-menu-item,
.workspace-topnav:is(.el-menu--horizontal) > .el-sub-menu > .el-sub-menu__title {
  display: flex;
  align-items: center;
  gap: 7px;
  height: 56px;
  margin: 0;
  padding: 0 12px;
  border-bottom: 2px solid transparent;
  color: var(--cf-muted);
  font-size: 13px;
}
.workspace-topnav:is(.el-menu--horizontal) > .el-menu-item.is-active,
.workspace-topnav:is(.el-menu--horizontal) > .el-sub-menu.is-active > .el-sub-menu__title {
  border-bottom-color: var(--el-color-primary);
  color: var(--el-color-primary);
}
.workspace-topnav:is(.el-menu--horizontal) > .el-menu-item:not(.is-disabled):hover,
.workspace-topnav:is(.el-menu--horizontal) > .el-menu-item:not(.is-disabled):focus,
.workspace-topnav:is(.el-menu--horizontal) > .el-sub-menu > .el-sub-menu__title:hover {
  background: var(--cf-hover);
  color: var(--cf-text);
}
.workspace-topnav .svg-icon { width: 14px; height: 14px; flex-shrink: 0; margin: 0; }
.workspace-topnav > .el-sub-menu > .el-sub-menu__title .el-sub-menu__icon-arrow { position: static; margin: 0; width: 12px; font-size: 11px; }
.workspace-topnav .el-sub-menu__icon-more { width: auto; margin: 0 !important; font-size: 13px; }
.topnav-overflow-label { font-style: normal; white-space: nowrap; font-size: 12px; }
.workspace-topnav-popper:is(.el-popper) {
  --el-menu-bg-color: var(--cf-surface);
  --el-menu-text-color: var(--cf-text);
  --el-menu-active-color: var(--el-color-primary);
  --el-menu-hover-bg-color: var(--cf-hover);
  border: 1px solid var(--cf-border);
  border-radius: 7px;
  background: var(--cf-surface);
  color: var(--cf-text);
  overflow: visible;
}
.workspace-topnav-popper .el-menu--popup { min-width: 190px; max-width: min(290px, calc(100vw - 24px)); padding: 5px; border: 0; border-radius: 7px; background: var(--cf-surface); box-shadow: none; max-height: min(560px, calc(100dvh - 100px)); overflow-y: auto; }
.workspace-topnav-popper .el-menu-item,
.workspace-topnav-popper .el-sub-menu__title { display: flex; align-items: center; gap: 9px; min-width: 0; height: 36px; line-height: 36px; padding: 0 12px; border-radius: 4px; color: var(--cf-text); font-size: 12px; }
.workspace-topnav-popper .el-sub-menu__title { padding-right: 32px; }
.workspace-topnav-popper .svg-icon { width: 14px; height: 14px; flex-shrink: 0; margin: 0; color: var(--cf-muted); }
.workspace-topnav-popper .topnav-item-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.workspace-topnav-popper .el-menu-item.is-active { background: var(--el-color-primary-light-9); color: var(--el-color-primary); }
.workspace-topnav-popper.el-zoom-in-top-enter-active,
.workspace-topnav-popper:is(.el-zoom-in-top-leave-active) { transition: opacity .14s ease, transform .14s ease; }
.workspace-topnav-popper.el-zoom-in-top-enter-from,
.workspace-topnav-popper:is(.el-zoom-in-top-leave-to) { opacity: 0; transform: translateY(-5px); }
@media (max-width: 900px) {
  .workspace-topnav:is(.el-menu--horizontal) { --el-menu-horizontal-height: 44px; height: 44px; }
  .workspace-topnav:is(.el-menu--horizontal) > .el-menu-item,
  .workspace-topnav:is(.el-menu--horizontal) > .el-sub-menu > .el-sub-menu__title { height: 44px; }
}
@media (prefers-reduced-motion: reduce) {
  .workspace-topnav-popper.el-zoom-in-top-enter-active,
  .workspace-topnav-popper:is(.el-zoom-in-top-leave-active) { transition: none; }
}
</style>