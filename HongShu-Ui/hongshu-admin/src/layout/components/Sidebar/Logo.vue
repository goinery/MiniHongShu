<template>
  <div class="sidebar-logo-container" :class="{ collapse, horizontal }">
    <router-link v-if="showBrand" to="/dashboard" class="brand-home" aria-label="返回数据中心" :title="title + ' · 数据中心'">
      <span class="brand-monogram" aria-hidden="true">{{ brandMonogram }}</span>
    </router-link>
    <el-popover v-if="!collapse" v-model:visible="accountOpen" placement="bottom-start" trigger="click" :width="280" :show-arrow="false" :offset="4" popper-class="workspace-account-menu" @hide="handleAccountHide">
      <template #reference>
        <button type="button" class="account-selector" aria-label="当前工作空间账户" :aria-expanded="accountOpen">
          <span>{{ accountName }}</span>
          <span class="account-chevrons" aria-hidden="true"><el-icon><ArrowUp /></el-icon><el-icon><ArrowDown /></el-icon></span>
        </button>
      </template>
      <div class="account-menu-search">
        <el-icon :size="14"><Search /></el-icon>
        <input v-model="accountFilter" type="search" placeholder="搜索账户…" aria-label="搜索账户" />
      </div>
      <div class="account-menu-options">
        <button v-if="accountMatches" type="button" class="account-option" @click="accountOpen = false">
          <span>{{ accountName }}</span><el-icon :size="14"><Check /></el-icon>
        </button>
        <p v-else class="account-menu-empty">没有匹配的账户</p>
      </div>
      <router-link class="account-settings-link" to="/user/profile" @click="accountOpen = false"><el-icon :size="15"><Setting /></el-icon><span>账户设置</span></router-link>
    </el-popover>
  </div>
</template>

<script setup>
import { ArrowDown, ArrowUp, Check, Search, Setting } from '@element-plus/icons-vue'

import { computed, onBeforeUnmount, ref, watch } from 'vue'

import useUserStore from '@/store/modules/user'
import { adminTitle, brandMonogram } from '@/config/brand'
defineProps({ collapse: { type: Boolean, required: true }, horizontal: Boolean, showBrand: { type: Boolean, default: true } })
const emit = defineEmits(['overlay-change'])
const title = adminTitle
const userStore = useUserStore()
const accountOpen = ref(false)
const accountFilter = ref('')
const accountName = computed(() => userStore.name ? `${userStore.name} 的账户` : title)
const accountMatches = computed(() => accountName.value.toLowerCase().includes(accountFilter.value.trim().toLowerCase()))
watch(accountOpen, opened => {
  if (opened) emit('overlay-change', true)
}, { flush: 'sync' })
function handleAccountHide() {
  accountFilter.value = ''
  emit('overlay-change', false)
}
onBeforeUnmount(() => emit('overlay-change', false))
</script>

<style lang="scss" scoped>
.sidebar-logo-container { display: flex; align-items: center; flex-shrink: 0; height: 56px; gap: 10px; padding: 0 17px; border-bottom: 1px solid var(--cf-border); }
.brand-home { display: flex !important; align-items: center; justify-content: center; width: 30px !important; height: 32px; flex-shrink: 0; color: var(--el-color-primary); }
.brand-monogram { display: grid; place-items: center; width: 30px; height: 30px; border-radius: 6px; background: var(--el-color-primary-light-9); font-size: 22px; font-weight: 700; line-height: 1; }
.account-selector { display: flex; align-items: center; gap: 10px; flex: 1; min-width: 0; height: 34px; padding: 0 5px; color: var(--cf-text); font: inherit; font-size: 12px; text-align: left; border: 0; border-radius: 5px; background: transparent; cursor: pointer; }
.account-selector > span:first-child { flex: 1; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.account-selector:hover, .account-selector[aria-expanded="true"] { background: var(--cf-hover); }
.account-chevrons { display: flex; flex-direction: column; flex-shrink: 0; gap: 1px; color: var(--cf-muted); font-size: 8px; }
.collapse { justify-content: flex-start; }
.horizontal { min-width: 0; width: 100%; height: 55px; gap: 8px; padding: 0; border-bottom: 0; }
</style>

<style lang="scss">
.workspace-account-menu:is(.el-popover):is(.el-popper) { padding: 0; min-width: 0; border: 1px solid var(--cf-border); border-radius: 7px; background: var(--cf-surface); box-shadow: var(--el-box-shadow-light); color: var(--cf-text); }
.workspace-account-menu {
  .account-menu-search { display: flex; align-items: center; gap: 9px; height: 38px; padding: 0 14px; color: var(--cf-muted); border-bottom: 1px solid var(--cf-border); }
  .account-menu-search input { width: 100%; min-width: 0; padding: 0; background: transparent; border: 0; outline: none; box-shadow: none; color: var(--cf-text); font: inherit; font-size: 12px; }
  .account-menu-search input::placeholder { color: var(--el-text-color-placeholder); }
  .account-menu-options { padding: 5px; }
  .account-option { display: flex; align-items: center; gap: 10px; width: 100%; padding: 8px 9px; border: 0; border-radius: 5px; background: var(--cf-hover); color: var(--cf-text); font: inherit; font-size: 12px; cursor: pointer; text-align: left; }
  .account-option span { flex: 1; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
  .account-menu-empty { margin: 0; padding: 12px 9px; font-size: 12px; color: var(--cf-muted); }
  .account-settings-link { display: flex; align-items: center; gap: 9px; padding: 12px 14px; border-top: 1px solid var(--cf-border); color: var(--el-text-color-regular); font-size: 12px; }
  .account-settings-link:hover { background: var(--cf-surface-subtle); color: var(--cf-text); }
}
</style>
