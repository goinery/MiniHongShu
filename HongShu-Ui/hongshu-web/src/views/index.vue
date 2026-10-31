<template></template>

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
