<template>
  <header class="navbar" :class="{ 'navbar-top': settingsStore.topNav }">
    <template v-if="settingsStore.topNav">
      <workspace-logo class="navbar-brand" :collapse="false" :show-brand="settingsStore.sidebarLogo" horizontal />
      <top-nav class="navbar-navigation" />
    </template>
    <div v-else class="navbar-context">
      <button class="nav-toggle" type="button" :aria-label="appStore.sidebar.opened ? '收起导航' : '展开导航'" @click="toggleSideBar">
        <el-icon :size="18"><Fold v-if="appStore.sidebar.opened" /><Expand v-else /></el-icon>
      </button>
      <breadcrumb id="breadcrumb-container" class="breadcrumb-container" />
    </div>
    <div class="right-menu">
      <header-search v-if="settingsStore.topNav" class="navbar-search" />
      <el-dropdown ref="accountDropdown" @command="handleCommand" @visible-change="onAccountMenuVisibleChange" :trigger="accountMenuPinned ? [] : 'hover'" :show-timeout="0" :hide-timeout="160">
        <button class="avatar-wrapper" type="button" aria-label="打开账户菜单" :aria-expanded="accountMenuOpen" @click="toggleAccountMenu">
          <img :src="resolveAvatarUrl(userStore.avatar)" class="user-avatar" alt="账户头像" referrerpolicy="no-referrer" @error="handleAvatarError" />
          <span class="user-name">{{ userStore.name || '管理员' }}</span>
          <el-icon :size="12" class="account-chevron" :class="{ 'is-open': accountMenuOpen }"><ArrowRight /></el-icon>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile" icon="User">个人中心</el-dropdown-item>
            <el-dropdown-item v-if="settingsStore.showSettings" command="setLayout" icon="Setting">界面设置</el-dropdown-item>
            <el-dropdown-item divided command="logout" icon="SwitchButton" class="danger-action">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup>
import { ArrowRight, Expand, Fold } from '@element-plus/icons-vue'

import { ref } from 'vue'
import { useRouter } from 'vue-router'

import { ElMessageBox } from 'element-plus'
import Breadcrumb from '@/components/Breadcrumb'
import TopNav from '@/components/TopNav'
import HeaderSearch from '@/components/HeaderSearch'
import WorkspaceLogo from './Sidebar/Logo.vue'
import { resolveAvatarUrl, handleAvatarError } from '@/utils/avatar'
import useAppStore from '@/store/modules/app'
import useUserStore from '@/store/modules/user'
import useSettingsStore from '@/store/modules/settings'

const appStore = useAppStore()
const userStore = useUserStore()
const settingsStore = useSettingsStore()
const router = useRouter()
const accountDropdown = ref()
const accountMenuOpen = ref(false)
const accountMenuPinned = ref(false)
const emits = defineEmits(['setLayout'])
function toggleSideBar() { appStore.toggleSideBar() }
function toggleAccountMenu() {
  // A pinned menu ignores pointer leave; outside clicks and keyboard dismissal still close it.
  accountMenuPinned.value = !accountMenuPinned.value
  if (accountMenuPinned.value) accountDropdown.value?.handleOpen()
  else accountDropdown.value?.handleClose()
}
function onAccountMenuVisibleChange(opened) {
  accountMenuOpen.value = opened
  if (!opened) accountMenuPinned.value = false
}
function handleCommand(command) {
  if (command === 'profile') router.push('/user/profile')
  if (command === 'setLayout') emits('setLayout')
  if (command === 'logout') {
    ElMessageBox.confirm('确定退出当前账户吗？', '退出登录', {
      confirmButtonText: '退出登录', cancelButtonText: '取消', type: 'warning', confirmButtonClass: 'el-button--danger'
    }).then(() => userStore.logOut()).then(() => { location.href = '/login' }).catch(() => {})
  }
}
</script>

<style lang="scss" scoped>
.navbar { height: 56px; display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 0 24px 0 14px; background: var(--cf-surface); border-bottom: 1px solid var(--cf-border); }
.navbar-top { display: grid; grid-template-columns: minmax(150px, 210px) minmax(0, 1fr) auto; column-gap: 18px; padding: 0 18px; }
.navbar-brand, .navbar-navigation { min-width: 0; }
.navbar-search { --search-trigger-width: 190px; flex: 0 0 190px; width: 190px; }
.navbar-top .right-menu { gap: 12px; }
.navbar-context, .right-menu { display: flex; align-items: center; gap: 16px; min-width: 0; }
.navbar-context { flex: 1; }
.nav-toggle { display: grid; place-items: center; flex-shrink: 0; width: 32px; height: 32px; padding: 0; border: 0; border-radius: 5px; background: transparent; color: var(--cf-muted); cursor: pointer; }
.nav-toggle:hover, .avatar-wrapper:hover { background: var(--cf-hover); }
.breadcrumb-container { min-width: 0; overflow: hidden; white-space: nowrap; }
.account-chevron { flex-shrink: 0; transition: transform .2s ease; }
.account-chevron.is-open { transform: rotate(90deg); }
.avatar-wrapper { display: flex; align-items: center; gap: 8px; border: 0; background: transparent; padding: 5px 6px; border-radius: 5px; color: var(--cf-text); cursor: pointer; font: inherit; }
.user-avatar { width: 26px; height: 26px; object-fit: cover; border-radius: 50%; border: 1px solid var(--cf-border); }
.user-name { max-width: 100px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 13px; }
@media (min-width: 901px) and (max-width: 1200px) { .navbar-top { grid-template-columns: minmax(140px, 180px) minmax(0, 1fr) auto; column-gap: 12px; } .navbar-top .user-name { display: none; } .navbar-search { --search-trigger-width: 150px; flex-basis: 150px; width: 150px; } }
@media (max-width: 900px) { .navbar-top { grid-template-columns: minmax(0, 1fr) auto; grid-template-rows: 55px 44px; height: 100px; gap: 0 12px; padding: 0 14px; } .navbar-top .navbar-brand { grid-column: 1; grid-row: 1; max-width: 230px; } .navbar-top .right-menu { grid-column: 2; grid-row: 1; } .navbar-navigation { grid-column: 1 / -1; grid-row: 2; border-top: 1px solid var(--cf-border); } }
@media (max-width: 600px) { .navbar-search { flex-basis: 36px; width: 36px; } .navbar-top .right-menu { gap: 8px; } .navbar-top .user-name { display: none; } }
@media (max-width: 600px) { .navbar:not(.navbar-top) { padding: 0 12px; gap: 8px; } .navbar-context { gap: 8px; } .right-menu { gap: 8px; } .user-name { display: none; } }
</style>
