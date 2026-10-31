<template></template>

<script lang="ts" setup>
import { Waterfall } from "vue-waterfall-plugin-next";
import "vue-waterfall-plugin-next/dist/style.css";
import { ref, watch } from "vue";
import { getNoteByDTO, getCategoryAgg } from "@/api/search";
import type { NoteDTO, NoteSearch } from "@/type/note";
import type { Category } from "@/type/category";
import Main from "@/views/main/main.vue";
import NoteLikeButton from "@/components/NoteLikeButton.vue";
import FloatingBtn from "@/components/FloatingBtn.vue";
import { options } from "@/constant/constant";
import Loading from "@/components/Loading.vue";
import { refreshTab } from "@/utils/util";
import { useRoute } from "vue-router";
import { getUserByKeyword } from "@/api/user";
import { followById } from "@/api/follower";

const route = useRoute();
const topLoading = ref(false);
const botLoading = ref(false);
const noteList = ref<Array<NoteSearch>>([]);
const categoryList = ref<Array<Category>>([]);
const currentPage = ref(1);
const pageSize = 20;
const noteTotal = ref(0);
const currentUserPage = ref(1);
const userPageSize = 15;
const categoryClass = ref("0");
const typeClass = ref(0);
const mainShow = ref(false);
const nid = ref("");
const noteDTO = ref<NoteDTO>({
  keyword: "",
  type: 0,
  cid: "",
  cpid: "",
});
const loadingMore = ref(false);
const isShowUser = ref(false);
const userDataList = ref<Array<any>>([]);
const userTotal = ref(0);

const getUserData = () => {
  typeClass.value = 3;
  isShowUser.value = true;
  currentUserPage.value = 1;
  userDataList.value = [];
  getUserByKeyword(currentUserPage.value, userPageSize, noteDTO.value.keyword).then((res: any) => {
    const { records, total } = res.data;
    userDataList.value.push(...records);
    userTotal.value = total;
  });
};

const follow = (fid: string, index: number, type: number) => {
  followById(fid).then(() => {
    userDataList.value[index].isFollow = type == -1;
  });
};

const loadMoreUser = () => {
  botLoading.value = true;
  loadingMore.value = true;
  currentUserPage.value += 1;
  new Promise((resolve) => {
    getUserByKeyword(currentUserPage.value, userPageSize, noteDTO.value.keyword).then((res: any) => {
      const { records, total } = res.data;
      userDataList.value.push(...records);
      userTotal.value = total;
      resolve(false);
    });
  }).then((data) => {
    loadingMore.value = data as boolean;
    setTimeout(() => {
      botLoading.value = false;
    }, 500);
  });
};

watch(
  () => [route.query.keyword],
  (newVal) => {
    noteDTO.value.keyword = newVal[0] as string;
    noteDTO.value.cid = "";
    noteDTO.value.type = 0;
    categoryClass.value = "0";
    isShowUser.value = false;
    typeClass.value = 0;
    getNoteListByKeyword();
    getCategoryData();
  }
);

const getNoteByType = (type: number) => {
  isShowUser.value = false;
  noteDTO.value.type = type;
  typeClass.value = type;
  getNoteListByKeyword();
};

const toMain = (noteId: string) => {
  nid.value = noteId;
  mainShow.value = true;
};

const close = (id?: string, extra?: any) => {
  if (extra && extra.deleted) {
    const target = String(id || nid.value || "");
    if (target) {
      noteList.value = noteList.value.filter((item) => String(item.id) !== target);
    }
  }
  nid.value = "";
  mainShow.value = false;
};

const handleLoad = (item: any) => {
  item.isLoading = true;
};

const refresh = () => {
  // 使用回调函数优化代码
  refreshTab(() => {
    topLoading.value = true;
    isShowUser.value = false;
    typeClass.value = 0;
    currentPage.value = 1;
    currentUserPage.value = 1;
    noteList.value = [];
    userDataList.value = [];
    setTimeout(() => {
      getNoteList();
      topLoading.value = false;
    }, 1000);
  });
};

const loadMoreData = () => {
  botLoading.value = true;
  loadingMore.value = true;
  currentPage.value += 1;
  new Promise((resolve) => {
    getNoteByDTO(currentPage.value, pageSize, noteDTO.value).then((res) => {
      setData(res);
      resolve(false);
    });
  }).then((data) => {
    loadingMore.value = data as boolean;
    setTimeout(() => {
      botLoading.value = false;
    }, 500);
  });
};

const setData = (res: any) => {
  const { records, total } = res.data;
  noteTotal.value = total;
  noteList.value.push(...records);
};

const getNoteList = () => {
  noteDTO.value.type = 0;
  noteDTO.value.cid = "";
  categoryClass.value = "0";
  noteList.value = [] as Array<any>;
  currentPage.value = 1;
  getNoteByDTO(currentPage.value, pageSize, noteDTO.value).then((res) => {
    setData(res);
  });
};

const getNoteListByCategory = (id: string) => {
  categoryClass.value = id;
  noteDTO.value.cid = id;
  noteList.value = [] as Array<any>;
  currentPage.value = 1;
  getNoteByDTO(currentPage.value, pageSize, noteDTO.value).then((res) => {
    setData(res);
  });
};

const getNoteListByKeyword = () => {
  noteList.value = [] as Array<any>;
  currentPage.value = 1;
  getNoteByDTO(currentPage.value, pageSize, noteDTO.value).then((res) => {
    setData(res);
  });
};

const getCategoryData = () => {
  getCategoryAgg(noteDTO.value).then((res: any) => {
    categoryList.value = res.data;
  });
};

const initData = () => {
  const keyword = route.query.keyword as string;
  if (keyword.trim().length > 0) {
    noteDTO.value.keyword = keyword as string;
    noteDTO.value.cid = "";
    categoryClass.value = "0";
    getNoteListByKeyword();
    getCategoryData();
  }
};

initData();
</script>

<style lang="less" scoped></style>
