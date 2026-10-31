<template>
  <div v-loading="loading" class="follower-list" :aria-busy="loading">
    <el-empty v-if="loadFailed && dataList.length === 0" description="关注消息加载失败">
      <el-button type="primary" round @click="getPageData">重新加载</el-button>
    </el-empty>
    <el-empty v-else-if="!loading && dataList.length === 0" description="暂无新增关注" />
    <ul
      v-else-if="dataList.length > 0"
      class="agree-container"
      v-infinite-scroll="getPageData"
      :infinite-scroll-disabled="loading || finished || loadFailed"
    >
      <li class="agree-item" v-for="(item, index) in dataList" :key="index">
        <RouterLink class="user-avatar author-link" :to="{ name: 'user', query: { uid: item.uid } }">
          <!-- https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png -->
          <img alt="用户头像" class="avatar-item author-link__avatar" :src="item.avatar" />
        </RouterLink>
        <div class="main">
          <div class="info">
            <div class="user-info">
              <RouterLink class="author-link author-link__name" :to="{ name: 'user', query: { uid: item.uid } }">{{ item.username }}</RouterLink>
            </div>
            <div class="interaction-hint">
              <span>开始关注你了&nbsp;</span><span>{{ item.time }}</span>
            </div>
          </div>
          <div class="extra">
            <el-button
              class="button followed-button"
              type="primary"
              plain
              round
              size="large"
              v-if="item.isFollow"
              @click="follow(item.uid, index, 1)"
              >互相关注中</el-button
            >
            <el-button class="button" type="primary" round size="large" v-else @click="follow(item.uid, index, -1)"
              >回关</el-button
            >
          </div>
        </div>
      </li>
    </ul>
    <div v-if="loadFailed && dataList.length > 0" class="load-error">
      <el-button type="primary" link @click="getPageData">加载失败，点击重试</el-button>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref } from "vue";
import { getNoticeFollower } from "@/api/follower";
import { formatSessionListTime } from "@/utils/util";
import { followById } from "@/api/follower";

const currentPage = ref(0);
const pageSize = 12;
const dataList = ref<Array<any>>([]);
const dataTotal = ref(0);
const loading = ref(false);
const finished = ref(false);
const loadFailed = ref(false);

const getPageData = async () => {
  if (loading.value || finished.value) return;
  const nextPage = currentPage.value + 1;
  loading.value = true;
  loadFailed.value = false;
  try {
    const res = await getNoticeFollower(nextPage, pageSize);
    const { records, total } = res.data;
    dataTotal.value = total;
    records.forEach((item: any) => {
      item.time = formatSessionListTime(item.time);
      dataList.value.push(item);
    });
    currentPage.value = nextPage;
    finished.value = records.length === 0 || nextPage * pageSize >= dataTotal.value;
  } catch {
    // 统一拦截器提示错误；保留当前页码，让重试仍请求失败的那一页。
    loadFailed.value = true;
  } finally {
    loading.value = false;
  }
};

const follow = (fid: string, index: number, type: number) => {
  followById(fid).then(() => {
    dataList.value[index].isFollow = type == -1;
  });
};

const initData = () => {
  getPageData();
};
initData();
</script>

<style lang="less" scoped>
.follower-list {
  min-height: 160px;
}

.load-error {
  padding: 16px;
  text-align: center;
}

textarea {
  overflow: auto;
}

.agree-container {
  width: 100%;

  .agree-item {
    display: flex;
    flex-direction: row;
    padding-top: 24px;

    .user-avatar {
      margin-right: 24px;
      flex-shrink: 0;

      .avatar-item {
        width: 48px;
        height: 48px;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        border-radius: 100%;
        border: 1px solid rgba(0, 0, 0, 0.08);
        object-fit: cover;
      }
    }

    .main {
      flex-grow: 1;
      flex-shrink: 1;
      display: flex;
      flex-direction: row;
      padding-bottom: 12px;
      border-bottom: 1px solid rgba(0, 0, 0, 0.08);

      .info {
        flex-grow: 1;
        flex-shrink: 1;

        .user-info {
          display: flex;
          flex-direction: row;
          align-items: center;
          justify-content: space-between;
          margin-bottom: 4px;

          a {
            color: #333;
            font-size: 16px;
            font-weight: 600;
          }
        }

        .interaction-hint {
          font-size: 14px;
          color: rgba(51, 51, 51, 0.6);
          margin-bottom: 8px;
        }

        .interaction-content {
          display: flex;
          font-size: 14px;
          color: #333;
          line-height: 140%;
          cursor: pointer;
          margin-bottom: 12px;

          .msg-count {
            width: 20px;
            height: 20px;
            line-height: 20px;
            font-size: 13px;
            color: #fff;
            background-color: var(--el-color-primary);
            text-align: center;
            border-radius: 100%;
          }
        }

        .quote-info {
          font-size: 12px;
          display: flex;
          align-items: center;
          color: rgba(51, 51, 51, 0.6);
          margin-bottom: 12px;
          cursor: pointer;
        }

        .quote-info::before {
          content: "";
          display: inline-block;
          border-radius: 8px;
          margin-right: 6px;
          width: 4px;
          height: 17px;
          background: rgba(0, 0, 0, 0.08);
        }
      }

      .button {
        width: 100px;
        height: 40px;
        font-size: 14px;
      }

      .extra {
        min-width: 48px;
        flex-shrink: 0;
        margin-left: 24px;

        .follow-button {
          width: 96px;
        }

      }
    }
  }
}
</style>
