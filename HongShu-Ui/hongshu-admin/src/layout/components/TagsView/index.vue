<template>
  <div id="tags-view-container" class="tags-view-container">
    <scroll-pane ref="scrollPaneRef" class="tags-view-wrapper" @scroll="handleScroll">
      <router-link
        v-for="tag in visitedViews"
        :key="tag.path"
        :data-path="tag.path"
        :class="isActive(tag) ? 'active' : ''"
        :to="{ path: tag.path, query: tag.query, fullPath: tag.fullPath }"
        class="tags-view-item"
        @click.middle="!isAffix(tag) ? closeSelectedTag(tag) : ''"
        @contextmenu.prevent="openMenu(tag, $event)"
      >
        {{ tag.title }}
        <span v-if="!isAffix(tag)" @click.prevent.stop="closeSelectedTag(tag)">
          <close class="el-icon-close" style="width: 1em; height: 1em;vertical-align: middle;" />
        </span>
      </router-link>
    </scroll-pane>
    <ul v-show="visible" :style="{ left: left + 'px', top: top + 'px' }" class="contextmenu">
      <li @click="refreshSelectedTag(selectedTag)">
        <refresh-right style="width: 1em; height: 1em;" /> 刷新页面
      </li>
      <li v-if="!isAffix(selectedTag)" @click="closeSelectedTag(selectedTag)">
        <close style="width: 1em; height: 1em;" /> 关闭当前
      </li>
      <li @click="closeOthersTags">
        <circle-close style="width: 1em; height: 1em;" /> 关闭其他
      </li>
      <li v-if="!isFirstView()" @click="closeLeftTags">
        <back style="width: 1em; height: 1em;" /> 关闭左侧
      </li>
      <li v-if="!isLastView()" @click="closeRightTags">
        <right style="width: 1em; height: 1em;" /> 关闭右侧
      </li>
      <li @click="closeAllTags(selectedTag)">
        <circle-close style="width: 1em; height: 1em;" /> 全部关闭
      </li>
    </ul>
  </div>
</template>

<script setup>
import { Back, CircleClose, Close, RefreshRight, Right } from '@element-plus/icons-vue'

import { computed, getCurrentInstance, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import ScrollPane from './ScrollPane'
import { getNormalPath } from '@/utils/ruoyi'
import useTagsViewStore from '@/store/modules/tagsView'
import usePermissionStore from '@/store/modules/permission'

const visible = ref(false);
const top = ref(0);
const left = ref(0);
const selectedTag = ref(/** @type {Partial<import('@/types/router').TagView>} */ ({}));
const affixTags = ref(/** @type {import('@/types/router').TagView[]} */ ([]));
const scrollPaneRef = ref(null);

const { proxy } = getCurrentInstance();
const route = useRoute();
const router = useRouter();

const visitedViews = computed(() => useTagsViewStore().visitedViews);
const routes = computed(() => usePermissionStore().routes);

watch(route, () => {
  addTags()
  moveToCurrentTag()
})
watch(visible, (value) => {
  if (value) {
    document.body.addEventListener('click', closeMenu)
  } else {
    document.body.removeEventListener('click', closeMenu)
  }
})
onMounted(() => {
  initTags()
  addTags()
})

function isActive(r) {
  return r.path === route.path
}
function isAffix(tag) {
  return tag.meta && tag.meta.affix
}
function isFirstView() {
  try {
    return selectedTag.value.fullPath === '/dashboard' || selectedTag.value.fullPath === visitedViews.value[1].fullPath
  } catch (err) {
    return false
  }
}
function isLastView() {
  try {
    return selectedTag.value.fullPath === visitedViews.value[visitedViews.value.length - 1].fullPath
  } catch (err) {
    return false
  }
}
function filterAffixTags(routes, basePath = '') {
  let tags = []
  routes.forEach(route => {
    if (route.meta && route.meta.affix) {
      const tagPath = getNormalPath(basePath + '/' + route.path)
      tags.push({
        fullPath: tagPath,
        path: tagPath,
        name: route.name,
        meta: { ...route.meta }
      })
    }
    if (route.children) {
      const tempTags = filterAffixTags(route.children, route.path)
      if (tempTags.length >= 1) {
        tags = [...tags, ...tempTags]
      }
    }
  })
  return tags
}
function initTags() {
  const res = filterAffixTags(routes.value);
  affixTags.value = res;
  for (const tag of res) {
    // Must have tag name
    if (tag.name) {
       useTagsViewStore().addVisitedView(tag)
    }
  }
}
function addTags() {
  const { name } = route
  if (name) {
    useTagsViewStore().addView(route)
    if (route.meta.link) {
      useTagsViewStore().addIframeView(route);
    }
  }
  return false
}
function moveToCurrentTag() {
  nextTick(() => {
    for (const r of visitedViews.value) {
      if (r.path === route.path) {
        scrollPaneRef.value.moveToTarget(r);
        // when query is different then update
        if (r.fullPath !== route.fullPath) {
          useTagsViewStore().updateVisitedView(route)
        }
      }
    }
  })
}
function refreshSelectedTag(view) {
  proxy.$tab.refreshPage(view);
  if (route.meta.link) {
    useTagsViewStore().delIframeView(route);
  }
}
function closeSelectedTag(view) {
  proxy.$tab.closePage(view).then(({ visitedViews }) => {
    if (isActive(view)) {
      toLastView(visitedViews, view)
    }
  })
}
function closeRightTags() {
  proxy.$tab.closeRightPage(selectedTag.value).then(visitedViews => {
    if (!visitedViews.find(i => i.fullPath === route.fullPath)) {
      toLastView(visitedViews)
    }
  })
}
function closeLeftTags() {
  proxy.$tab.closeLeftPage(selectedTag.value).then(visitedViews => {
    if (!visitedViews.find(i => i.fullPath === route.fullPath)) {
      toLastView(visitedViews)
    }
  })
}
function closeOthersTags() {
  router.push(selectedTag.value).catch(() => { });
  proxy.$tab.closeOtherPage(selectedTag.value).then(() => {
    moveToCurrentTag()
  })
}
function closeAllTags(view) {
  proxy.$tab.closeAllPage().then(({ visitedViews }) => {
    if (affixTags.value.some(tag => tag.path === route.path)) {
      return
    }
    toLastView(visitedViews, view)
  })
}
function toLastView(visitedViews, view) {
  const latestView = visitedViews.slice(-1)[0]
  if (latestView) {
    router.push(latestView.fullPath)
  } else {
    // now the default is to redirect to the home page if there is no tags-view,
    // you can adjust it according to your needs.
    if (view.name === 'Dashboard') {
      // to reload home page
      router.replace({ path: '/redirect' + view.fullPath })
    } else {
      router.push('/')
    }
  }
}
function openMenu(tag, e) {
  left.value = Math.max(8, Math.min(e.clientX + 8, document.documentElement.clientWidth - 168))
  top.value = Math.max(8, Math.min(e.clientY + 8, window.innerHeight - 280))
  visible.value = true
  selectedTag.value = tag
}
function closeMenu() {
  visible.value = false
}
function handleScroll() {
  closeMenu()
}
</script>

<style lang="scss" scoped>
.tags-view-container {
  height: 48px;
  width: 100%;
  background: var(--cf-background);
  border-bottom: 1px solid var(--cf-border);
  position: relative;
  .tags-view-wrapper {
    .tags-view-item {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      position: relative;
      cursor: pointer;
      height: 32px;
      line-height: 32px;
      border: 1px solid transparent;
      border-radius: 5px;
      color: var(--cf-muted);
      padding: 0 12px;
      font-size: 12px;
      margin: 8px 2px 0;
      &:first-of-type { margin-left: 24px; }
      &:last-of-type { margin-right: 24px; }
      &:hover { color: var(--cf-text); background: var(--cf-hover); }
      &.active { background: var(--cf-surface); color: var(--el-color-primary); font-weight: 500; border-color: var(--el-color-primary-light-7); box-shadow: 0 1px 2px #0000000a; }
    }
  }
  .contextmenu {
    min-width: 160px;
    margin: 0;
    background: var(--cf-surface);
    z-index: 3000;
    position: fixed;
    list-style: none;
    padding: 6px;
    border: 1px solid var(--cf-border);
    border-radius: 6px;
    font-size: 12px;
    color: var(--el-text-color-regular);
    box-shadow: 0 4px 16px #00000012;
    li { display: flex; align-items: center; gap: 8px; margin: 0; padding: 9px 12px; cursor: pointer; border-radius: 4px; }
    li:hover { background: var(--cf-hover); }
  }
}
.tags-view-item .el-icon-close { width: 14px; height: 14px; padding: 1px; border-radius: 3px; color: var(--cf-muted); }
.tags-view-item .el-icon-close:hover { background: var(--cf-hover); color: var(--cf-text); }
</style>
