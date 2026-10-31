<template>
  <div class="feeds-container" v-infinite-scroll="loadMoreData" :infinite-scroll-distance="50" :infinite-scroll-disabled="loading || loadFailed || noteList.length >= noteTotal">
    <Waterfall
      :list="noteList"
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
            >
            </el-image>

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
              <div class="image-container note-cover" role="button" tabindex="0" :aria-label="'查看笔记：' + item.title" @click="toMain(item.id)" @keydown.enter.prevent="toMain(item.id)" @keydown.space.prevent="toMain(item.id)">
                <el-image
                  :src="item.noteCover"
                  :style="{
                    width: '240px',
                    maxHeight: '300px',
                    height: item.noteCoverHeight + 'px',
                    borderRadius: '8px',
                  }"
                  fit="cover"
                >
                </el-image>
                <div v-if="item.auditStatus === '0'" class="overlay">审核中</div>
                <div v-if="item.auditStatus === '2'" class="overlay not-passed">未通过⚠️</div>
              </div>
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
              <div class="top-tag-area" v-show="type === 1 && item.pinned === '1'">
                <div class="top-wrapper">置顶</div>
              </div>
            </div>
          </template>
        </el-skeleton>
      </template>
    </Waterfall>
    <p v-if="loading" class="feed-status" role="status">加载中…</p>
    <div v-else-if="loadFailed" class="feed-status" role="alert">
      <p>笔记加载失败</p>
      <el-button type="primary" plain @click="getNoteList(type, requestedPage)">重新加载</el-button>
    </div>
    <el-empty
      v-else-if="!noteList.length"
      :description="type === 1 ? '还没有发布作品' : type === 2 ? '还没有点赞的笔记' : '还没有收藏的笔记'"
    />
  </div>

  <Main
    v-show="mainShow"
    :nid="nid"
    @click-main="close"
  ></Main>
</template>

<script lang="ts" setup>
import { Waterfall } from "vue-waterfall-plugin-next";
import "vue-waterfall-plugin-next/dist/style.css";
import { ref, onBeforeUnmount, onMounted, watch } from "vue";
import { getTrendByUser } from "@/api/user";
import Main from "@/views/main/main.vue";
import NoteLikeButton from "@/components/NoteLikeButton.vue";
import { options } from "@/constant/constant";
import { useRoute } from "vue-router";
import { useUserStore } from "@/store/userStore";
const route = useRoute();

const props = defineProps({
  type: {
    type: Number,
    default: 1,
  },
});

watch(
  () => [props.type],
  ([newType]) => {
    currentPage.value = 1;
    noteTotal.value = 0;
    noteList.value = [] as Array<any>;
    getNoteList(newType);
  }
);

const noteList = ref<Array<any>>([]);
const noteTotal = ref(0);
const uid = String(route.query.uid || useUserStore().getUserInfo()?.id || "");
const currentPage = ref(1);
const pageSize = 10;
const nid = ref("");
const mainShow = ref(false);
const userStore = useUserStore();
const currentUid = userStore.getUserInfo()?.id;
const loading = ref(true);
const loadFailed = ref(false);
const requestedPage = ref(1);
let requestVersion = 0;

const handleLoad = (item: any) => {
  item.isLoading = true;
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

const toMain = (noteId: string) => {
  nid.value = noteId;
  mainShow.value = true;
};

const setData = (res: any) => {
  const { records, total } = res.data;
  noteTotal.value = total;
  // 过滤掉不是当前用户且状态“审核中”或“未通过”的记录
  const filteredRecords = records.filter((item: any) => {
    return item.uid === currentUid || (item.auditStatus !== "0" && item.auditStatus !== "2");
  });
  noteList.value.push(...filteredRecords);
};

const getNoteList = async (type: number, page = 1) => {
  if (!uid) { loading.value = false; return; }
  const version = ++requestVersion;
  requestedPage.value = page;
  loadFailed.value = false;
  loading.value = true;
  try {
    const res = await getTrendByUser(page, pageSize, uid, type);
    if (version === requestVersion) {
      setData(res);
      currentPage.value = page;
    }
  } catch {
    if (version === requestVersion) loadFailed.value = true;
  } finally { if (version === requestVersion) loading.value = false; }
};

const loadMoreData = () => {
  if (loading.value || loadFailed.value || !uid || noteList.value.length >= noteTotal.value) return;
  getNoteList(props.type, currentPage.value + 1);
};

const initData = () => {
  getNoteList(props.type);
};

onMounted(() => {
  initData();
});
onBeforeUnmount(() => { requestVersion++; });
</script>

<style lang="less" scoped>
.feed-status {
  padding: 24px;
  text-align: center;
  color: var(--el-text-color-secondary);
}

.image-container {
  position: relative;
  display: inline-block;
}
.overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5); /* 半透明背景 */
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-size: 20px;
}
.overlay.not-passed {
  color: red; /* 设置未通过状态的字体颜色为红色 */
}
.feeds-container {
  position: relative;
  transition: width 0.5s;
  margin: 0 auto;

  .card {
    position: relative;

    .top-tag-area {
      pointer-events: none;
      position: absolute;
      left: 12px;
      top: 12px;
      z-index: var(--layer-local-control);

      .top-wrapper {
        background: var(--el-color-primary);
        border-radius: 999px;
        font-weight: 500;
        color: #fff;
        line-height: 120%;
        font-size: 12px;
        padding: 5px 8px;
        display: flex;
        align-items: center;
        justify-content: center;
      }
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
</style>
