<template></template>

<script lang="ts" setup>
import { ChatRound } from "@element-plus/icons-vue";
import { nextTick, ref } from "vue";
import { getFollowTrend } from "@/api/follower";
import { formateTime, refreshTab } from "@/utils/util";
import FloatingBtn from "@/components/FloatingBtn.vue";
import Main from "@/views/main/main.vue";
import NoteLikeButton from "@/components/NoteLikeButton.vue";
import { useUserStore } from "@/store/userStore";

const userStore = useUserStore();
const currentPage = ref(1);
const pageSize = ref(5);
const trendData = ref<Array<any>>([]);
const trendTotal = ref(0);
const topLoading = ref(false);
const loading = ref(false);
const mainShow = ref(false);
const noteDetail = ref<InstanceType<typeof Main>>();
const nid = ref("");
const isLogin = ref(false);

const handleLoad = (item: any) => {
  item.isLoading = true;
};

const getFollowTrends = () => {
  if (!isLogin.value || loading.value) return;
  loading.value = true;
  getFollowTrend(currentPage.value, pageSize.value).then((res) => {
    const { records, total } = res.data;
    records.forEach((item: any) => {
      item.time = formateTime(item.time);
      trendData.value.push(item);
    });
    trendTotal.value = total;
  }).catch(() => { currentPage.value = Math.max(1, currentPage.value - 1); }).finally(() => { loading.value = false; });
};

const loadMoreData = () => {
  if (loading.value || !isLogin.value || trendData.value.length >= trendTotal.value) return;
  currentPage.value += 1;
  getFollowTrends();
};

const toMain = async (noteId: string, focusComment = false) => {
  nid.value = noteId;
  mainShow.value = true;
  if (focusComment) {
    // 等详情显示后再聚焦，避免对 v-show 隐藏的输入框调用 focus。
    await nextTick();
    if (mainShow.value && nid.value === noteId) {
      noteDetail.value?.focusCommentInput();
    }
  }
};

const close = (noteId: string, val: any) => {
  if (val && val.deleted) {
    const target = String(noteId || nid.value || "");
    trendData.value = trendData.value.filter((item) => String(item.nid || item.id) !== target);
    nid.value = "";
    mainShow.value = false;
    return;
  }
  const index = trendData.value.findIndex((item) => item.nid === noteId);
  const _data = trendData.value[index];
  if (_data && val) {
    if (val.isComment) {
      _data.commentCount += 1;
    }
  }
  nid.value = "";
  mainShow.value = false;
};

const refresh = () => {
  refreshTab(() => {
    topLoading.value = true;
    setTimeout(() => {
      currentPage.value = 1;
      trendData.value = [];
      getFollowTrends();
      topLoading.value = false;
    }, 500);
  });
};

const initData = () => {
  isLogin.value = userStore.isLogin();
  if (isLogin.value) getFollowTrends();
};

initData();
</script>

<style lang="less" scoped></style>
