<template>
  <div class="container" id="container" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
    <div class="top">
      <header class="mask-paper">
        <div class="brand-navigation">
          <button
            type="button"
            class="sidebar-toggle"
            aria-controls="primary-sidebar"
            :aria-expanded="sidebarExpanded"
            :aria-label="sidebarCollapsed ? '展开左栏' : '收起左栏'"
            :title="sidebarCollapsed ? '展开左栏' : '收起左栏'"
            @click="toggleSidebar"
          >
            <Expand v-if="sidebarCollapsed" />
            <Fold v-else />
          </button>
          <RouterLink class="logo-wrap" :to="{ name: 'dashboard' }" aria-label="返回发现页" title="返回发现页">
            <span class="logo" :title="brandName">{{ brandName }}</span>
          </RouterLink>
        </div>
        <div class="input-box" id="sujContainer">
          <input
            type="text"
            v-model="keyword"
            class="search-input"
            placeholder="搜索内容"
            @input="changeInput"
            @focus="focusInput"
            @keyup.enter="searchPage"
            ref="SearchInput"
          />
          <div class="input-button">
            <div class="close-icon round-close" v-show="showClose" @click="clearInput">
              <Close style="width: 1.2em; height: 1.2em" />
            </div>
            <div class="search-icon" @click="searchPage">
              <a href="#">
                <Search style="width: 1.2em; height: 1.2em; margin-right: 20px; margin-top: 5px" />
              </a>
            </div>
          </div>
          <SearchContainer v-show="showSearch" :recordList="recordList"></SearchContainer>
          <SujContainer v-show="showHistory" :closeHistoryRecord="showHistory"></SujContainer>
        </div>
        <div class="right">
          <RepoLinks />
        </div>
      </header>
    </div>
    <div class="main">
      <aside
        id="primary-sidebar"
        class="side-bar"
        :class="{ 'is-expanded': sidebarExpanded }"
        aria-label="主导航"
        @pointerenter="onSidebarPointerEnter"
        @pointerleave="sidebarHovered = false"
        @focusin="onSidebarFocusIn"
        @focusout="onSidebarFocusOut"
        @keydown.esc.stop="closeSidebarPreview"
      >
        <ul class="channel-list">
          <li :class="{ 'active-channel': activeLink == 0 }" @click="toLink(0)">
            <a class="link-wrapper" role="button" tabindex="0" aria-label="发现" :title="sidebarExpanded ? undefined : '发现'" @keydown.enter.prevent="toLink(0)" @keydown.space.prevent="toLink(0)">
              <House class="sidebar-nav-icon" />
              <span class="channel">发现</span>
            </a>
          </li>
          <li :class="{ 'active-channel': activeLink == 5 }" @click="toLink(5)">
            <a class="link-wrapper" role="button" tabindex="0" aria-label="关注" :title="sidebarExpanded ? undefined : '关注'" @keydown.enter.prevent="toLink(5)" @keydown.space.prevent="toLink(5)"><User class="sidebar-nav-icon" /><span class="channel">关注</span></a>
          </li>
          <li :class="{ 'active-channel': activeLink == 1 }" @click="toLink(1)">
            <a class="link-wrapper" role="button" tabindex="0" :aria-label="messageCount > 0 ? '通知，有新消息' : '通知'" :title="sidebarExpanded ? undefined : '通知'" @keydown.enter.prevent="toLink(1)" @keydown.space.prevent="toLink(1)">
              <el-badge is-dot class="nav-badge" :hidden="!(messageCount > 0 && userInfo != null)">
                <Bell class="sidebar-nav-icon" />
              </el-badge>
              <span class="channel">通知</span>
            </a>
          </li>
          <li :class="{ 'active-channel': activeLink == 2 }" @click="toLink(2)">
            <a class="link-wrapper" role="button" tabindex="0" aria-label="发布" :title="sidebarExpanded ? undefined : '发布'" @keydown.enter.prevent="toLink(2)" @keydown.space.prevent="toLink(2)">
              <CirclePlus class="sidebar-nav-icon" />
              <span class="channel">发布</span>
            </a>
          </li>
          <li v-if="userInfo == null" class="login-item">
            <el-button type="primary" round class="custom-button" :icon="sidebarExpanded ? undefined : User" aria-label="登录" @click.stop="login"><span v-if="sidebarExpanded">登录</span></el-button>
          </li>
          <li v-else :class="{ 'active-channel': activeLink == 3 }" @click="toLink(3)">
            <a class="link-wrapper" role="button" tabindex="0" aria-label="我" :title="sidebarExpanded ? undefined : '我'" @keydown.enter.prevent="toLink(3)" @keydown.space.prevent="toLink(3)">
              <el-avatar class="nav-avatar" :src="userInfo.avatar" :size="22" />
              <span class="channel">我</span>
            </a>
          </li>
        </ul>

        <div v-if="userInfo == null" v-show="sidebarExpanded">
          <div data-v-6432121e="" data-v-7d49aed8="" class="floating-box visible">
            <div data-v-6432121e="" class="title">马上登录即可</div>
            <div data-v-6432121e="" class="line-container">
              <svg data-v-23d27ada="" data-v-6432121e="" class="reds-icon icon" width="16" height="16">
                <use data-v-23d27ada="" xlink:href="#thumbUp"></use>
              </svg>
              <span data-v-6432121e="" class="desc">刷到更懂你的优质内容</span>
            </div>
            <div data-v-6432121e="" class="line-container">
              <svg data-v-23d27ada="" data-v-6432121e="" class="reds-icon icon" width="16" height="16">
                <use data-v-23d27ada="" xlink:href="#convention_b"></use>
              </svg>
              <span data-v-6432121e="" class="desc">搜索最新种草、拔草信息</span>
            </div>
            <div data-v-6432121e="" class="line-container">
              <svg data-v-23d27ada="" data-v-6432121e="" class="reds-icon icon" width="16" height="16">
                <use data-v-23d27ada="" xlink:href="#collect"></use>
              </svg>
              <span data-v-6432121e="" class="desc">查看收藏、点赞的笔记</span>
            </div>
            <div data-v-6432121e="" class="line-container">
              <svg data-v-23d27ada="" data-v-6432121e="" class="reds-icon icon" width="16" height="16">
                <use data-v-23d27ada="" xlink:href="#chat"></use>
              </svg>
              <span data-v-6432121e="" class="desc">与他人更好地互动、交流</span>
            </div>
          </div>
        </div>

        <!-- 嵌入 SVG 图标定义 -->
        <svg style="display: none">
          <symbol id="thumbUp" viewBox="0 0 24 24">
            <!-- SVG 路径数据 -->
            <path
              d="M1 21h4V9H1v12zM23 10h-6.31l.95-4.57.03-.32c0-.41-.17-.79-.44-1.06L16.17 3 9.59 9.59C9.21 9.95 9 10.45 9 11v8c0 .55.45 1 1 1h6c.38 0 .72-.21.89-.55l3.66-7.33c.08-.14.13-.3.13-.46V11c0-.55-.45-1-1-1zm-2 7h-4v-6h4v6z"
            />
          </symbol>
          <symbol id="convention_b" viewBox="0 0 24 24">
            <!-- SVG 路径数据 -->
            <path
              d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z"
            />
          </symbol>
          <symbol id="collect" viewBox="0 0 24 24">
            <!-- SVG 路径数据 -->
            <path
              d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
            />
          </symbol>
          <symbol id="chat" viewBox="0 0 24 24">
            <!-- SVG 路径数据 -->
            <path d="M20 2H4c-1.1 0-2 .9-2 2v14l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-2 9H6V9h12v2zm0-3H6V6h12v2z" />
          </symbol>
        </svg>

        <div
          class="information-container"
          id="informationContainer"
          @pointerenter="onMoreMenuPointerEnter"
          @pointerleave="onMoreMenuPointerLeave"
          @focusin="cancelMoreMenuClose"
          @focusout="onMoreMenuFocusOut"
        >
          <Transition name="more-menu" @before-enter="moreMenuVisible = true" @after-leave="moreMenuVisible = false">
            <div class="information-pad" id="sidebar-more-menu" v-show="padShow">
              <div class="container">
                <div class="group-wrapper">
                  <button type="button" class="menu-item hover-effect menu-action" @click="openAgreement('about')">
                    <span>关于{{ brandName }}</span>
                  </button>
                  <button type="button" class="menu-item hover-effect menu-action" @click="openAgreement('privacy')">
                    <span>隐私政策</span>
                  </button>
                  <button type="button" class="menu-item hover-effect menu-action" @click="openAgreement('manual')">
                    <span>用户协议</span>
                  </button>
                  <button type="button" class="menu-item hover-effect menu-action" @click="openFeedback">
                    <span>帮助与客服</span>
                  </button>
                </div>
                <div>
                  <div class="group-wrapper">
                    <div class="group-header">设置</div>
                    <div class="theme-settings">
                      <span class="theme-setting-label">主题</span>
                      <div class="theme-setting-controls">
                        <ThemeColorPicker ref="themeColorPicker" :model-value="themeStore.themeColor" :default-color="DEFAULT_THEME_COLOR" placement="top-start" @change="themeStore.setThemeColor" @visible-change="onThemePickerVisibleChange" />
                        <div class="appearance-toggle" role="group" aria-label="主题模式">
                          <button type="button" class="appearance-mode" :class="{ active: themeStore.theme === 'light' }" :aria-pressed="themeStore.theme === 'light'" aria-label="浅色模式" title="浅色模式" @click.stop="themeStore.setTheme('light')"><Sunny /></button>
                          <button type="button" class="appearance-mode" :class="{ active: themeStore.theme === 'dark' }" :aria-pressed="themeStore.theme === 'dark'" aria-label="深色模式" title="深色模式" @click.stop="themeStore.setTheme('dark')"><Moon /></button>
                          <button type="button" class="appearance-mode" :class="{ active: themeStore.theme === 'system' }" :aria-pressed="themeStore.theme === 'system'" aria-label="跟随系统" title="跟随系统" @click.stop="themeStore.setTheme('system')"><Monitor /></button>
                        </div>
                      </div>
                    </div>
                    <div v-if="userInfo != null">
                      <div class="menu-item hover-effect" @click="logout">
                        <a href="#"><span>退出登录</span></a>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

          </Transition>

          <button type="button" class="information-wrapper" aria-label="更多" aria-controls="sidebar-more-menu" :aria-expanded="padShow" :title="sidebarExpanded ? undefined : '更多'" @click="loadPad">
            <svg class="sidebar-nav-icon more-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
              <path
                fill="currentColor"
                fill-rule="evenodd"
                d="M19.75 12a.75.75 0 0 0-.75-.75H5a.75.75 0 0 0 0 1.5h14a.75.75 0 0 0 .75-.75m0-5a.75.75 0 0 0-.75-.75H5a.75.75 0 0 0 0 1.5h14a.75.75 0 0 0 .75-.75m0 10a.75.75 0 0 0-.75-.75H5a.75.75 0 0 0 0 1.5h14a.75.75 0 0 0 .75-.75"
                clip-rule="evenodd"
              />
            </svg>
            <span class="channel">更多</span>
          </button>
        </div>
      </aside>
      <div class="main-content with-side-bar">
        <router-view />
      </div>
    </div>
    <Login v-show="loginShow" @click-child="close"></Login>
    <ToUP v-model="toUpshow" />
    <AgreementDialog v-model="agreementShow" :type="agreementType" />
  </div>
</template>

<script lang="ts" setup>
import {
  Search,
  User,
  Sunny,
  Moon,
  Monitor,
  Close,
  House,
  Bell,
  CirclePlus,
  Expand,
  Fold,
} from "@element-plus/icons-vue";
import { useRouter, useRoute } from "vue-router";
import Login from "@/views/login.vue";
import { ref, watch, onMounted, onUnmounted, computed, watchEffect, provide } from "vue";
import { useUserStore } from "@/store/userStore";
import { useSearchStore } from "@/store/searchStore";
import SujContainer from "@/components/SujContainer.vue";
import SearchContainer from "@/components/SearchContainer.vue";
import RepoLinks from "@/components/RepoLinks.vue";
import ThemeColorPicker from "@/components/ThemeColorPicker.vue";
import { addRecord, getRecordByKeyWord } from "@/api/search";
import { getRandomString } from "@/utils/util";
import { useImStore } from "@/store/imStore";
import { loginOut } from "@/api/user";
import { useMessaging } from "@/composables/useMessaging";
import ToUP from "@/views/to-up/index.vue";
import AgreementDialog from "@/components/AgreementDialog.vue";
import { DEFAULT_THEME_COLOR, useThemeStore } from "@/store/themeStore";
import type { AgreementType } from "@/constants/agreements";
import { brandName } from "@/config/brand";

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const searchStore = useSearchStore();
const imStore = useImStore();
const themeStore = useThemeStore();
const loginShow = ref(false);
const agreementShow = ref(false);
const agreementType = ref<AgreementType>("about");
const userInfo = computed(() => userStore.getUserInfo());
const showHistory = ref(false);
const showSearch = ref(false);
const keyword = ref("");
const showClose = ref(false);
const SearchInput = ref();
const recordList = ref<Array<string>>([]);
const padShow = ref(false);
const moreMenuPinned = ref(false);
const moreMenuVisible = ref(false);
const themePickerOpen = ref(false);
const themeColorPicker = ref<{ close: () => void }>();
let moreMenuHovered = false;
let moreMenuCloseTimer: ReturnType<typeof setTimeout> | undefined;
const cancelMoreMenuClose = () => {
  if (moreMenuCloseTimer !== undefined) clearTimeout(moreMenuCloseTimer);
  moreMenuCloseTimer = undefined;
};
const closeMoreMenu = () => {
  cancelMoreMenuClose();
  moreMenuPinned.value = false;
  themeColorPicker.value?.close();
  padShow.value = false;
};
const scheduleMoreMenuClose = () => {
  cancelMoreMenuClose();
  moreMenuCloseTimer = setTimeout(() => {
    moreMenuCloseTimer = undefined;
    if (moreMenuPinned.value || moreMenuHovered || themePickerOpen.value) return;
    const focused = document.activeElement;
    const menu = document.getElementById("informationContainer");
    if (focused instanceof HTMLElement && menu?.contains(focused)
      && (focused.matches(":focus-visible") || focused instanceof HTMLInputElement)) return;
    padShow.value = false;
  }, 160);
};
const onMoreMenuPointerEnter = (event: PointerEvent) => {
  if (event.pointerType !== "mouse" && event.pointerType !== "pen") return;
  moreMenuHovered = true;
  cancelMoreMenuClose();
  padShow.value = true;
};
const onMoreMenuPointerLeave = (event: PointerEvent) => {
  if (event.pointerType !== "mouse" && event.pointerType !== "pen") return;
  moreMenuHovered = false;
  scheduleMoreMenuClose();
};
const onMoreMenuFocusOut = (event: FocusEvent) => {
  if (!(event.relatedTarget instanceof Node) || !(event.currentTarget as HTMLElement).contains(event.relatedTarget)) {
    if (!moreMenuHovered) scheduleMoreMenuClose();
  }
};
const onThemePickerVisibleChange = (visible: boolean) => {
  themePickerOpen.value = visible;
  if (visible) cancelMoreMenuClose();
  else if (!moreMenuHovered) scheduleMoreMenuClose();
};
onUnmounted(cancelMoreMenuClose);
const SIDEBAR_STORAGE_KEY = "sidebar-collapsed";
const readSidebarCollapsed = () => {
  try { return localStorage.getItem(SIDEBAR_STORAGE_KEY) === "true"; }
  catch { return false; }
};
const sidebarCollapsed = ref(readSidebarCollapsed());
const sidebarHovered = ref(false);
const sidebarKeyboardFocus = ref(false);
const sidebarExpanded = computed(() => !sidebarCollapsed.value || sidebarHovered.value || sidebarKeyboardFocus.value || padShow.value || moreMenuVisible.value || themePickerOpen.value);

const closeSidebarPreview = () => {
  closeMoreMenu();
  moreMenuHovered = false;
  sidebarHovered.value = false;
  sidebarKeyboardFocus.value = false;
};
const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value;
  closeSidebarPreview();
  try { localStorage.setItem(SIDEBAR_STORAGE_KEY, String(sidebarCollapsed.value)); }
  catch { /* 本地存储不可用时仍允许在当前页面折叠。 */ }
};
const onSidebarPointerEnter = (event: PointerEvent) => {
  if (event.pointerType === "mouse" || event.pointerType === "pen") sidebarHovered.value = true;
};
const onSidebarFocusIn = (event: FocusEvent) => {
  sidebarKeyboardFocus.value = event.target instanceof HTMLElement && event.target.matches(":focus-visible");
};
const onSidebarFocusOut = (event: FocusEvent) => {
  if (!(event.relatedTarget instanceof Node) || !(event.currentTarget as HTMLElement).contains(event.relatedTarget)) {
    sidebarKeyboardFocus.value = false;
  }
};
const toUpshow = ref(false);
const openFeedback = () => {
  closeMoreMenu();
  if (!userStore.isLogin()) { loginShow.value = true; return; }
  toUpshow.value = true;
};
const isShowDot = ref(false);

const routerList = ["/dashboard", "/notice", "/push", "/user", "/search", "/followTrend"];
const activeLink = computed(() => {
  if (route.path === "/user") {
    const currentUid = userInfo.value?.id;
    const viewedUid = route.query.uid || currentUid;
    return currentUid && String(viewedUid) === String(currentUid) ? 3 : -1;
  }
  return route.path === "/search" ? 0 : routerList.findIndex((path) => path === route.path);
});

// 配色面板传送到 body，面板内点击也应保留“更多”的展开状态。
const onContainerClick = (event: MouseEvent) => {
  const target = event.target;
  if (!(target instanceof Node)) return;
  if (!document.getElementById("sujContainer")?.contains(target)) {
    showHistory.value = false;
    showSearch.value = false;
  }
  const inColorPicker = target instanceof Element && target.closest(".theme-color-picker-popover");
  if (!document.getElementById("informationContainer")?.contains(target) && !inColorPicker) closeMoreMenu();
};
onMounted(() => {
  document.addEventListener("click", onContainerClick);
});
onUnmounted(() => document.removeEventListener("click", onContainerClick));

const searchPage = () => {
  // 1.storage中添加搜索记录
  searchStore.setKeyword(keyword.value);
  if (keyword.value.length > 0) {
    addRecord(keyword.value);
    searchStore.pushRecord(keyword.value);
    searchStore.setSeed(getRandomString(12));
  }
  showSearch.value = false;

  router.push({ name: "search", query: { keyword: keyword.value } });
};

watch(() => searchStore.seed, () => {
  keyword.value = searchStore.keyWord;
  showHistory.value = false;
  showSearch.value = false;
});
watch(() => userInfo.value?.id, (uid) => {
  if (uid) loginShow.value = false;
});

watch(
  () => [imStore.countMessage],
  (val) => {
    const allCount = val[0].chatCount + val[0].likeOrCollectionCount + val[0].commentCount + val[0].followCount;
    if (allCount === 0) {
      isShowDot.value = false;
    }
  },
  {
    deep: true,
  }
);

const messageCount = computed(() =>
  imStore.countMessage.chatCount + imStore.countMessage.likeOrCollectionCount +
  imStore.countMessage.commentCount + imStore.countMessage.followCount
);

watchEffect(() => {
  const url = window.location.href;
  const _keyword = "keyword";
  if (url.indexOf("?") != -1 && url.indexOf(_keyword) != -1) {
    const val = url.substring(url.lastIndexOf(_keyword) + _keyword.length + 1, url.length);
    keyword.value = decodeURI(val);
  }
});

const openAgreement = (type: AgreementType) => {
  agreementType.value = type;
  agreementShow.value = true;
  closeMoreMenu();
};

const changeInput = (e: any) => {
  const { value } = e.target;
  keyword.value = value;
  showClose.value = keyword.value !== "";
  showSearch.value = keyword.value.length !== 0;
  showHistory.value = keyword.value.length === 0;
  if (keyword.value.length > 0) {
    getRecordByKeyWord(keyword.value).then((res) => {
      recordList.value = res.data;
    });
  }
};

const focusInput = () => {
  showClose.value = keyword.value !== "";
  showSearch.value = keyword.value.length !== 0;
  showHistory.value = keyword.value.length === 0;
};

const clearInput = () => {
  keyword.value = "";
  showClose.value = false;
  showHistory.value = true;
  showSearch.value = false;
  SearchInput.value.focus();
};

const login = () => {
  loginShow.value = true;
};

provide("openLogin", login);

const toLink = (num: number) => {
  const url = routerList[num];
  if ((url === "/notice" || url === "/push" || url === "/user" || url === "/followTrend") && !userStore.isLogin()) {
    login();
    return;
  }
  if (url === "/user") {
    router.push({ name: "user", query: { uid: userInfo.value.id } });
    return;
  }
  router.push({ path: url });
};

const close = (val: boolean) => {
  loginShow.value = val;
};

const loadPad = () => {
  cancelMoreMenuClose();
  if (moreMenuPinned.value) closeMoreMenu();
  else {
    moreMenuPinned.value = true;
    padShow.value = true;
  }
};

useMessaging();

const logout = () => {
  loginOut(userInfo.value.id).then(() => {
    userStore.loginOut();
    loginShow.value = true;
    router.push({ path: "/" });
  });
};

const onSessionEnded = () => { loginShow.value = true; };
onMounted(() => window.addEventListener("hongshu:logout", onSessionEnded));
onUnmounted(() => window.removeEventListener("hongshu:logout", onSessionEnded));
</script>

<style lang="less" scoped></style>
