<template>
  <div class="header-search">
    <button type="button" class="search-trigger" :class="{ collapsed }" aria-label="快速搜索菜单，快捷键 Control K" @click="open">
      <span class="search-trigger-inner" aria-hidden="true"><el-icon :size="15"><Search /></el-icon><span class="search-trigger-label">快速搜索...</span><kbd>Ctrl K</kbd></span>
    </button>
    <el-dialog
      v-model="visible"
      title="搜索工作空间"
      class="command-dialog"
      modal-class="command-overlay"
      width="648px"
      top="84px"
      :show-close="false"
      append-to-body
      @opened="focusInput"
      @closed="handleDialogClosed"
    >
      <div class="command-shell">
        <div class="command-searchbar">
          <el-icon class="command-search-icon" :size="18"><Search /></el-icon>
          <input
            ref="inputRef"
            v-model="query"
            class="command-input"
            type="text"
            placeholder="搜索页面、功能或菜单..."
            aria-label="搜索页面、功能或菜单"
            role="combobox"
            aria-autocomplete="list"
            aria-haspopup="listbox"
            :aria-expanded="visible"
            :aria-controls="listId"
            :aria-activedescendant="activeOptionId"
            autocomplete="off"
            spellcheck="false"
            @keydown="handleInputKeydown"
          />
          <button class="command-escape" type="button" aria-label="关闭搜索" @click="visible = false"><kbd>Esc</kbd></button>
        </div>
        <div :id="listId" ref="resultsRef" class="command-results" role="listbox" aria-label="工作空间页面">
          <section v-for="group in groups" :key="group.id" class="command-group" role="group" :aria-labelledby="`${listId}-${group.id}`">
            <h2 :id="`${listId}-${group.id}`" class="command-label">{{ group.label }}</h2>
            <button
              v-for="item in group.items"
              :id="optionId(item.index)"
              :key="`${group.id}-${item.path}`"
              type="button"
              role="option"
              tabindex="-1"
              class="command-result"
              :class="{ active: item.index === activeIndex }"
              :aria-selected="item.index === activeIndex"
              @mouseenter="activeIndex = item.index"
              @mousedown.prevent
              @click="navigate(item)"
            >
              <el-icon v-if="group.id === 'recent'" class="command-result-icon" :size="16"><Clock /></el-icon>
              <svg-icon v-else-if="item.icon && item.icon !== '#'" class-name="command-result-icon" :icon-class="item.icon" />
              <el-icon v-else class="command-result-icon" :size="16"><Document /></el-icon>
              <span class="command-result-copy">
                <span class="command-result-title">{{ item.title[item.title.length - 1] }}</span>
                <span class="command-result-parent">{{ item.title.slice(0, -1).join(' / ') || '工作空间' }}</span>
              </span>
              <el-icon class="command-result-arrow" :size="14"><ArrowRight /></el-icon>
            </button>
          </section>
          <div v-if="!results.length" class="command-empty" role="status">
            <el-icon :size="25"><Search /></el-icon>
            <span>{{ query.trim() ? '没有找到匹配的页面' : '暂无可访问的页面' }}</span>
            <small v-if="query.trim()">试试其他页面名称或关键词</small>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="command-help">
          <span><kbd>↑</kbd><kbd>↓</kbd><span>导航</span></span>
          <span><kbd class="enter-key">↵</kbd><span>选择</span></span>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ArrowRight, Clock, Document, Search } from '@element-plus/icons-vue'

import { computed, getCurrentInstance, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Fuse from 'fuse.js'
import { getNormalPath } from '@/utils/ruoyi'
import { isHttp } from '@/utils/validate'
import usePermissionStore from '@/store/modules/permission'
import useTagsViewStore from '@/store/modules/tagsView'

defineProps({ collapsed: Boolean })
const emit = defineEmits(['overlay-change'])
const visible = ref(false)
const query = ref('')
const activeIndex = ref(0)
const inputRef = ref(null)
const resultsRef = ref(null)
const router = useRouter()
const route = useRoute()
const permissionStore = usePermissionStore()
const tagsViewStore = useTagsViewStore()
const sessionVisits = ref([])
const listId = `workspace-command-${getCurrentInstance().uid}`

function collectRoutes(routes, basePath = '', prefix = []) {
  return routes.flatMap(route => {
    if (route.hidden) return []
    const routePath = route.path || ''
    const path = isHttp(routePath) ? routePath : getNormalPath(routePath.startsWith('/') ? routePath : basePath + '/' + routePath) || '/'
    const title = route.meta?.title ? [...prefix, route.meta.title] : prefix
    if (route.children?.length) return collectRoutes(route.children, path, title)
    return title.length ? [{ path, title, icon: route.meta?.icon, query: route.query }] : []
  })
}

const pages = computed(() => {
  const unique = new Map()
  collectRoutes(permissionStore.routes).forEach(page => unique.set(page.path, page))
  return [...unique.values()]
})
const recentPages = computed(() => {
  const allowedPages = new Map(pages.value.map(page => [page.path, page]))
  const seen = new Set()
  return [...sessionVisits.value, ...tagsViewStore.visitedViews.slice().reverse()]
    .filter(visit => {
      if (!allowedPages.has(visit.path) || seen.has(visit.path)) return false
      seen.add(visit.path)
      return true
    })
    .slice(0, 4)
    .map(visit => ({ ...allowedPages.get(visit.path), query: visit.query, hash: visit.hash }))
})
const searchIndex = computed(() => new Fuse(pages.value, { threshold: 0.35, keys: ['title', 'path'] }))
const groups = computed(() => {
  let source
  if (query.value.trim()) {
    source = [{ id: 'search', label: '搜索结果', items: searchIndex.value.search(query.value.trim()).map(result => result.item) }]
  } else {
    const recentPaths = new Set(recentPages.value.map(page => page.path))
    source = [
      { id: 'recent', label: '最近访问', items: recentPages.value },
      { id: 'pages', label: '页面导航', items: pages.value.filter(page => !recentPaths.has(page.path)) },
    ]
  }
  let index = 0
  return source.filter(group => group.items.length).map(group => ({
    ...group,
    items: group.items.map(item => ({ ...item, index: index++ })),
  }))
})
const results = computed(() => groups.value.flatMap(group => group.items))
const activeOptionId = computed(() => results.value[activeIndex.value] ? optionId(activeIndex.value) : undefined)

function optionId(index) { return `${listId}-option-${index}` }
function resetSearch() { query.value = ''; activeIndex.value = 0 }
function handleDialogClosed() { resetSearch(); emit('overlay-change', false) }
function open() { resetSearch(); visible.value = true }
function focusInput() { inputRef.value?.focus() }
function recordVisit() {
  sessionVisits.value = [
    { path: route.path, query: { ...route.query }, hash: route.hash },
    ...sessionVisits.value.filter(visit => visit.path !== route.path),
  ].slice(0, 20)
}
watch(() => route.fullPath, recordVisit, { immediate: true })
watch(visible, opened => { if (opened) emit('overlay-change', true) }, { flush: 'sync' })
watch(query, () => {
  activeIndex.value = 0
  nextTick(() => { if (resultsRef.value) resultsRef.value.scrollTop = 0 })
})
watch(results, (items) => { if (activeIndex.value >= items.length) activeIndex.value = 0 })

async function move(step) {
  if (!results.value.length) return
  activeIndex.value = (activeIndex.value + step + results.value.length) % results.value.length
  await nextTick()
  const selected = resultsRef.value?.querySelector(`[id="${optionId(activeIndex.value)}"]`)
  selected?.scrollIntoView({ block: 'nearest' })
}
function handleInputKeydown(event) {
  if (event.isComposing || event.keyCode === 229) return
  if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
    event.preventDefault()
    move(event.key === 'ArrowDown' ? 1 : -1)
  } else if (event.key === 'Enter') {
    event.preventDefault()
    selectActive()
  }
}
function selectActive() {
  if (results.value[activeIndex.value]) navigate(results.value[activeIndex.value])
}
function navigate(item) {
  visible.value = false
  if (isHttp(item.path)) window.open(item.path, '_blank', 'noopener,noreferrer')
  else {
    let queryValue
    try { queryValue = typeof item.query === 'string' ? JSON.parse(item.query) : item.query } catch { queryValue = undefined }
    router.push({ path: item.path, query: queryValue, hash: item.hash || '' })
  }
}
function handleShortcut(event) {
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
    event.preventDefault()
    if (visible.value) visible.value = false
    else open()
  }
}
onMounted(() => document.addEventListener('keydown', handleShortcut))
onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleShortcut)
  emit('overlay-change', false)
})
</script>

<style scoped lang="scss">
.search-trigger { position: relative; display: block; width: 100%; height: 34px; padding: 0; overflow: hidden; background: var(--cf-surface); border: 1px solid var(--cf-border); border-radius: 6px; color: var(--cf-muted); font: inherit; font-size: 12px; cursor: pointer; }
.search-trigger:hover { border-color: var(--el-border-color-dark); }
.search-trigger { transition: width var(--sidebar-motion-duration, .16s) var(--sidebar-motion-easing, ease-out); }
.search-trigger-inner { position: absolute; top: 0; left: 0; display: flex; align-items: center; gap: 9px; width: var(--search-trigger-width, 219px); height: 32px; padding: 0 10px; text-align: left; }
.search-trigger-inner > .el-icon { flex: 0 0 15px; }
.search-trigger-label { flex: 1; min-width: 0; white-space: nowrap; overflow: hidden; }
.search-trigger kbd { font-size: 10px; font-family: inherit; color: var(--el-text-color-placeholder); white-space: nowrap; }
.search-trigger.collapsed { width: 36px; }
.search-trigger.collapsed .search-trigger-label, .search-trigger.collapsed kbd { visibility: hidden; }
.command-shell { display: flex; flex: 1; flex-direction: column; min-width: 0; min-height: 0; }
.command-searchbar { display: flex; align-items: center; flex-shrink: 0; gap: 11px; height: 62px; padding: 0 18px; border-bottom: 1px solid var(--cf-border); background: var(--cf-surface); transition: border-color .15s; }
.command-searchbar:focus-within { border-bottom-color: var(--el-border-color-dark); }
.command-search-icon { flex-shrink: 0; color: var(--el-text-color-placeholder); }
.command-input { flex: 1; min-width: 0; height: 100%; margin: 0; padding: 0; border: 0 !important; border-radius: 0; outline: none !important; box-shadow: none !important; background: transparent; color: var(--cf-text); font: inherit; font-size: 14px; line-height: normal; }
.command-input::placeholder { color: var(--el-text-color-placeholder); opacity: 1; }
.command-escape { display: flex; align-items: center; flex-shrink: 0; margin: 0; padding: 4px 0 4px 4px; border: 0; background: transparent; color: var(--cf-muted); cursor: pointer; }
.command-escape kbd { display: inline-flex; align-items: center; justify-content: center; height: 24px; min-width: 31px; padding: 0 5px; border: 1px solid var(--cf-border); border-radius: 4px; background: var(--cf-surface-subtle); font: inherit; font-size: 11px; }
.command-escape:hover kbd { border-color: var(--el-border-color-dark); color: var(--el-text-color-regular); }
.command-escape:focus-visible { background: var(--cf-hover); border-radius: 3px; }
.command-results { flex: 1; min-height: 0; overflow-y: auto; overscroll-behavior: contain; padding: 8px; scrollbar-width: thin; scrollbar-color: var(--el-border-color) transparent; }
.command-group + .command-group { margin-top: 9px; padding-top: 5px; }
.command-label { margin: 0; padding: 12px 12px 7px; color: var(--cf-muted); font-size: 11px; font-weight: 400; line-height: 1.5; }
.command-result { display: flex; align-items: center; gap: 11px; width: 100%; min-height: 42px; padding: 10px 12px; border: 0; border-radius: 5px; background: var(--cf-surface); color: var(--el-text-color-regular); text-align: left; font: inherit; font-size: 13px; cursor: pointer; outline: none; }
.command-result.active, .command-result:hover, .command-result:focus-visible { background: var(--cf-hover); color: var(--cf-text); }
.command-result-icon { flex-shrink: 0; width: 15px; height: 15px; color: var(--cf-muted); }
.command-result-copy { display: flex; align-items: baseline; flex: 1; flex-wrap: wrap; gap: 5px 10px; min-width: 0; }
.command-result-title { overflow: hidden; max-width: 100%; color: inherit; text-overflow: ellipsis; white-space: nowrap; font-weight: 400; line-height: 1.5; }
.command-result-parent { overflow: hidden; max-width: 100%; color: var(--el-text-color-placeholder); font-size: 11px; text-overflow: ellipsis; white-space: nowrap; line-height: 1.5; }
.command-result-arrow { flex-shrink: 0; margin-left: 6px; color: var(--el-text-color-placeholder); }
.command-result.active .command-result-arrow { color: var(--cf-muted); }
.command-empty { display: flex; align-items: center; justify-content: center; flex-direction: column; gap: 12px; min-height: 250px; padding: 30px 16px; color: var(--cf-muted); font-size: 13px; text-align: center; }
.command-empty > .el-icon { color: var(--el-text-color-placeholder); }
.command-empty small { margin-top: -3px; color: var(--el-text-color-placeholder); font-size: 12px; }
.command-help { display: flex; align-items: center; justify-content: flex-start; gap: 18px; color: var(--cf-muted); font-size: 11px; }
.command-help > span { display: inline-flex; align-items: center; gap: 4px; }
.command-help > span > span { margin-left: 3px; }
.command-help kbd { display: inline-flex; align-items: center; justify-content: center; min-width: 19px; height: 20px; padding: 0 4px; border: 1px solid var(--cf-border); border-radius: 3px; background: var(--cf-surface); color: var(--cf-muted); font-family: inherit; font-size: 12px; }
.command-help .enter-key { min-width: 24px; font-size: 14px; }
@media (max-width: 600px) { .command-searchbar { gap: 9px; padding: 0 14px; } .command-result-copy { flex-direction: column; gap: 1px; } }
</style>

<style lang="scss">
.command-overlay:is(.el-overlay) { background: var(--cf-overlay); backdrop-filter: blur(1px); }
.command-dialog:is(.el-dialog) { margin-top: 84px !important; }
.command-dialog:is(.el-dialog) { display: flex; flex-direction: column; width: 648px; max-width: calc(100vw - 32px); height: 510px; max-height: calc(100dvh - 108px); padding: 0; overflow: hidden; background: var(--cf-surface); border: 1px solid var(--cf-border); border-radius: 9px; box-shadow: var(--el-box-shadow); }
.command-dialog:is(.el-dialog) .el-dialog__header { display: none; }
.command-dialog:is(.el-dialog) .el-dialog__body { display: flex; flex: 1; min-height: 0; max-height: none; padding: 0; overflow: hidden; color: var(--el-text-color-regular); }
.command-dialog:is(.el-dialog) .el-dialog__footer { flex-shrink: 0; padding: 12px 18px; border-top: 1px solid var(--cf-border); background: var(--cf-surface-subtle); text-align: left; }
@media (max-width: 600px) { .command-dialog:is(.el-dialog) { margin-top: 28px !important; max-width: calc(100vw - 24px); max-height: calc(100dvh - 56px); } }
@media (max-height: 500px) { .command-dialog:is(.el-dialog) { margin-top: 20px !important; max-height: calc(100dvh - 40px); } }
</style>
