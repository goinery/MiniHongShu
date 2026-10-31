<template>
  <div class="feeds-page">
    <div class="channel-container">
      <div class="scroll-container channel-scroll-container">
        <div class="content-container">
          <div :class="categoryClass == '0' ? 'channel active' : 'channel'" @click="getNoteList">推荐</div>
          <div
            :class="categoryClass == item.id ? 'channel active' : 'channel'"
            v-for="item in categoryList"
            :key="item.id"
            @click="getNoteListByCategory(item.id)"
          >
            {{ item.title }}
          </div>
        </div>
      </div>
    </div>
    <div
      class="feeds-container"
      :class="{ 'is-replacing': replacingNotes }"
      v-infinite-scroll="loadMoreData"
      :infinite-scroll-distance="50"
      :infinite-scroll-disabled="loading || isEnd"
      :aria-busy="loading"
    >
      <div class="feeds-loading-top" :class="{ 'is-visible': topLoading }" :aria-hidden="!topLoading">
        <Loading style="width: 1.2em; height: 1.2em"></Loading>
      </div>
      <Waterfall
        ref="waterfall"
        :list="noteList"
        :delay="0"
        :width="options.width"
        :gutter="options.gutter"
        :hasAroundGutter="options.hasAroundGutter"
        :animation-effect="options.animationEffect"
        :animation-duration="options.animationDuration"
        :animation-delay="options.animationDelay"
        :breakpoints="options.breakpoints"
        style="min-width: 740px"
      >
        <template #item="{ item }">
          <el-skeleton style="width: 240px" :loading="!item.isLoading" animated>
            <template #template>
              <el-image
                :src="item.noteCover"
                :style="{
                  width: '240px',
                  maxHeight: '300px',
                  height: item.noteCoverHeight + 'px',
                  borderRadius: '8px',
                }"
                @load="handleLoad(item)"
              ></el-image>
              <div style="padding: 14px">
                <el-skeleton-item variant="h3" style="width: 100%" />
                <div style="display: flex; align-items: center; margin-top: 2px; height: 16px">
                  <el-skeleton style="--el-skeleton-circle-size: 20px">
                    <template #template>
                      <el-skeleton-item variant="circle" />
                    </template>
                  </el-skeleton>
                  <el-skeleton-item variant="text" style="margin-left: 10px" />
                </div>
              </div>
            </template>

            <template #default>
              <div class="card" style="max-width: 240px">
                <el-image
                  :src="item.noteCover"
                  :style="{
                    width: '240px',
                    maxHeight: '300px',
                    height: item.noteCoverHeight + 'px',
                    borderRadius: '8px',
                  }"
                  fit="cover"
                  class="note-cover"
                  role="button"
                  tabindex="0"
                  :aria-label="'查看笔记：' + item.title"
                  @keydown.enter.prevent="toMain(item.id)"
                  @keydown.space.prevent="toMain(item.id)"
                  @click="toMain(item.id)"
                ></el-image>
                <div class="footer">
                  <button type="button" class="title note-title" @click.stop="toMain(item.id)">
                    <span>{{ item.title }}</span>
                  </button>
                  <div class="author-wrapper">
                    <RouterLink class="author author-link" :to="{ name: 'user', query: { uid: item.uid } }" @click.stop>
                      <img alt="用户头像" class="author-avatar author-link__avatar" :src="item.avatar" />
                      <span class="name author-link__name">{{ item.username }}</span>
                    </RouterLink>
                    <NoteLikeButton :note-id="item.id" :author-id="item.uid" :is-like="item.isLike" :like-count="item.likeCount" />
                  </div>
                </div>
              </div>
            </template>
          </el-skeleton>
        </template>
      </Waterfall>

      <div class="feeds-loading" v-show="loading && !topLoading">
        <Loading style="width: 1.2em; height: 1.2em"></Loading>
      </div>
      <div class="feeds-end" v-show="isEnd">······ 已经到底了 ·····</div>
    </div>
    <FloatingBtn @click-refresh="refresh"></FloatingBtn>
    <Main
      v-show="mainShow"
      :nid="nid"
      @click-main="close"
    ></Main>
  </div>
</template>

<script lang="ts" setup>
import { Waterfall } from "vue-waterfall-plugin-next";
import "vue-waterfall-plugin-next/dist/style.css";
import { nextTick, onBeforeUnmount, ref, watch } from "vue";
import { getRecommendNote, getNoteByDTO, addRecord } from "@/api/search";
import { getCategoryTreeData } from "@/api/category";
import type { NoteDTO, NoteSearch } from "@/type/note";
import type { Category } from "@/type/category";
import Main from "@/views/main/main.vue";
import NoteLikeButton from "@/components/NoteLikeButton.vue";
import FloatingBtn from "@/components/FloatingBtn.vue";
import { options } from "@/constant/constant";
import { useSearchStore } from "@/store/searchStore";
import Loading from "@/components/Loading.vue";
import { refreshTab } from "@/utils/util";
const searchStore = useSearchStore();
const topLoading = ref(false);
const loading = ref(false);
const waterfall = ref<InstanceType<typeof Waterfall>>();
const replacingNotes = ref(false);
let requestVersion = 0;
const noteList = ref<Array<NoteSearch>>([]);
const categoryList = ref<Array<Category>>([]);
const currentPage = ref(1);
const pageSize = 20;
const noteTotal = ref(0);
const categoryClass = ref("0");
const mainShow = ref(false);
const nid = ref("");
const isEnd = ref(false);
const noteDTO = ref<NoteDTO>({
  keyword: "",
  type: 0,
  cid: "",
  cpid: "",
});
let requestedQuery: NoteDTO = { ...noteDTO.value };

onBeforeUnmount(() => { requestVersion++; });

watch(
  () => [searchStore.seed],
  () => {
    getNoteListByKeyword();
    addRecord(searchStore.keyWord);
  }
);

const toMain = (noteId: string) => {
  nid.value = noteId;
  mainShow.value = true;
};

const close = (id?: string, extra?: any) => {
  if (extra && extra.deleted) {
    const target = String(id || nid.value || "");
    if (target) {
      noteList.value = noteList.value.filter((item) => String(item.id) !== target);
      noteTotal.value = Math.max(0, Number(noteTotal.value || 0) - 1);
    }
  }
  nid.value = "";
  mainShow.value = false;
};

const handleLoad = (item: any) => {
  item.isLoading = true;
};

const fetchNotes = (page: number, query: NoteDTO) => {
  return !query.cpid && !query.cid && !query.keyword
    ? getRecommendNote(page, pageSize)
    : getNoteByDTO(page, pageSize, query);
};

const setData = async (res: any, replace = false) => {
  const records = (res?.data?.records || []).map((item: any) => ({
    ...item,
    isLoading: true,
  }));
  noteTotal.value = res?.data?.total || 0;
  if (replace) {
    replacingNotes.value = true;
    noteList.value = records;
  } else {
    noteList.value.push(...records);
  }
  await nextTick();
  // 替换或追加都在 DOM 更新后重新布局；同 id 卡片直接排到新位置，再恢复缩放过渡。
  // delay=0 确保 renderer 同步布局，不让过渡提前恢复。
  waterfall.value?.renderer();
  if (replace) {
    // 复用的卡片也在新位置重新播放放大入场，外层位置过渡仍保持关闭。
    const root = waterfall.value?.$el as HTMLElement | undefined;
    const cards = root?.querySelectorAll<HTMLElement>(".waterfall-card");
    if (root && cards?.length) {
      cards.forEach((card) => card.classList.remove(options.animationEffect));
      void root.offsetWidth;
      cards.forEach((card) => card.classList.add(options.animationEffect));
    }
  }
  replacingNotes.value = false;
  isEnd.value = records.length === 0 || noteList.value.length >= noteTotal.value;
};

// 频道点击与悬浮刷新共用入口；成功拿到新列表前保留原内容和分页状态。
const reloadNotes = (query: NoteDTO, animate = true) => {
  const version = ++requestVersion;
  const nextQuery = { ...query };
  requestedQuery = nextQuery;
  categoryClass.value = nextQuery.cpid || "0";
  loading.value = true;

  const load = async () => {
    if (version !== requestVersion) return;
    topLoading.value = animate;
    try {
      const [res] = await Promise.all([
        fetchNotes(1, nextQuery),
        // 快请求也留出展开动画时间，慢请求则一直保持加载状态。
        animate ? new Promise<void>((resolve) => setTimeout(resolve, 350)) : Promise.resolve(),
      ]);
      if (version !== requestVersion) return;
      await setData(res, true);
      if (version !== requestVersion) return;
      noteDTO.value = nextQuery;
      currentPage.value = 1;
    } catch {
      if (version !== requestVersion) return;
      // 请求拦截器负责提示错误，原列表仍可继续浏览和翻页。
      requestedQuery = { ...noteDTO.value };
      categoryClass.value = noteDTO.value.cpid || "0";
    } finally {
      if (version === requestVersion) {
        loading.value = false;
        topLoading.value = false;
      }
    }
  };

  if (animate) refreshTab(load);
  else void load();
};

const loadMoreData = async () => {
  if (loading.value || isEnd.value || !noteList.value.length) return;
  const version = requestVersion;
  const nextPage = currentPage.value + 1;
  loading.value = true;
  try {
    const res = await fetchNotes(nextPage, { ...noteDTO.value });
    if (version !== requestVersion) return;
    await setData(res);
    currentPage.value = nextPage;
  } catch {
    // 失败不推进页码，下次滚动可重新加载同一页。
  } finally {
    if (version === requestVersion) loading.value = false;
  }
};

const refresh = () => reloadNotes(requestedQuery);

const getNoteList = () => reloadNotes({ keyword: "", type: 0, cid: "", cpid: "" });

const getNoteListByCategory = (id: string) => reloadNotes({ keyword: "", type: 0, cid: "", cpid: id });

const getNoteListByKeyword = () => reloadNotes({ keyword: searchStore.keyWord, type: 0, cid: "", cpid: "" });

const getCategoryData = () => {
  getCategoryTreeData().then((res: any) => {
    categoryList.value = res.data;
  });
};

const initData = () => {
  getCategoryData();
  reloadNotes(noteDTO.value, false);
};

initData();
</script>

<style lang="less" scoped>
.feeds-page {
  flex: 1;
  padding: 0 var(--page-inline-padding);
  padding-top: 72px;
  height: 100vh;

  .channel-container {
    // 扩展滚动区域，为首尾按钮的悬浮放大留白，按钮仍与卡片左边缘对齐。
    margin: 0 -12px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    user-select: none;
    -webkit-user-select: none;

    .channel-scroll-container {
      backdrop-filter: blur(20px);
      background-color: transparent;
      width: 100%;
      min-width: 0;

      position: relative;
      overflow: visible;
      display: flex;
      user-select: none;
      -webkit-user-select: none;
      align-items: center;
      font-size: 16px;
      color: rgba(51, 51, 51, 0.8);
      white-space: nowrap;
      height: 72px;

      .content-container::-webkit-scrollbar {
        display: none;
      }

      .content-container {
        display: flex;
        max-width: 100%;
        padding: 12px;
        overflow-x: auto;
        overflow-y: hidden;
        white-space: nowrap;
        color: rgba(51, 51, 51, 0.8);

        .active {
          font-weight: 600;
          background: rgba(0, 0, 0, 0.03);
          border-radius: 999px;
          color: #333;
        }

        .channel {
          flex-shrink: 0;
          height: 40px;
          display: flex;
          justify-content: center;
          align-items: center;
          padding: 0 16px;
          cursor: pointer;
          -webkit-user-select: none;
          user-select: none;
        }
        .channel:hover {
          cursor: pointer; /* 显示小手指针 */
          transform: scale(1.2); /* 鼠标移入时按钮稍微放大 */
        }
      }
    }
  }

  .feeds-container {
    position: relative;
    transition: width 0.5s;
    margin: 0 auto;

    &.is-replacing :deep(.waterfall-item) {
      // 覆盖插件写入的行内 transform 过渡，仅在整批换数据时生效。
      transition: none !important;
    }

    .feeds-loading {
      margin: 3vh;
      text-align: center;
    }

    .feeds-loading-top {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 0;
      overflow: hidden;
      opacity: 0;
      transition: height 0.25s ease, opacity 0.2s ease;

      &.is-visible {
        height: 6vh;
        opacity: 1;
      }
    }

    .footer {
      padding: 12px;

      .title {
        margin-bottom: 8px;
        word-break: break-all;
        display: -webkit-box;
        -webkit-box-orient: vertical;
        -webkit-line-clamp: 2;
        overflow: hidden;
        font-weight: 500;
        font-size: 14px;
        line-height: 140%;
        color: #333;
      }

      .author-wrapper {
        display: flex;
        align-items: center;
        justify-content: space-between;
        height: 20px;
        color: rgba(51, 51, 51, 0.8);
        font-size: 12px;
        transition: color 1s;

        .author {
          display: flex;
          align-items: center;
          color: inherit;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          margin-right: 12px;

          .author-avatar {
            margin-right: 6px;
            width: 20px;
            height: 20px;
            border-radius: 20px;
            border: 1px solid rgba(0, 0, 0, 0.08);
            flex-shrink: 0;
            object-fit: cover;
          }

          .name {
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }

        .like-wrapper {
          position: relative;
          cursor: pointer;
          display: flex;
          align-items: center;

          .count {
            margin-left: 2px;
          }
        }
      }
    }
  }

  .floating-btn-sets {
    position: fixed;
    display: flex;
    flex-direction: column;
    width: 40px;
    grid-gap: 8px;
    gap: 8px;
    right: 24px;
    bottom: 24px;

    .back-top {
      width: 40px;
      height: 40px;
      background: #fff;
      border: 1px solid rgba(0, 0, 0, 0.08);
      border-radius: 100px;
      color: rgba(51, 51, 51, 0.8);
      display: flex;
      align-items: center;
      justify-content: center;
      // transition: background 0.2s;
      cursor: pointer;
    }

    .reload {
      width: 40px;
      height: 40px;
      background: #fff;
      border: 1px solid rgba(0, 0, 0, 0.08);
      box-shadow:
        0 2px 8px 0 rgba(0, 0, 0, 0.1),
        0 1px 2px 0 rgba(0, 0, 0, 0.02);
      border-radius: 100px;
      color: rgba(51, 51, 51, 0.8);
      display: flex;
      align-items: center;
      justify-content: center;
      //transition: background 0.2s;
      cursor: pointer;
    }
  }
  .feeds-end {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 50px;
    color: #999;
    font-size: 16px;
    margin-top: 20px;
    border-radius: 10px;
  }
}
</style>
