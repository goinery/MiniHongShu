<template>
  <div>
    <el-empty v-if="dataList.length === 0" description="暂无评论和@" />
    <ul v-else class="message-container" v-infinite-scroll="loadMore">
      <li class="message-item" v-for="(item, index) in dataList" :key="index">
        <a class="user-avatar">
          <!-- https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png -->
          <img alt="用户头像" class="avatar-item" :src="item.avatar" @click="toUser(item.uid)" />
        </a>
        <div class="main">
          <div class="info">
            <div class="user-info">
              <a class>{{ item.username }}</a>
            </div>
            <div class="interaction-hint">
              <span v-if="item.pid === '0'">评论了您的笔记</span>
              <span v-if="item.replyUid === currentUid && item.pid !== '0'">回复了您的评论</span>
              <span v-if="item.replyUid !== currentUid && item.pid !== '0'">回复了{{ item.replyUsername }}的评论</span>
              &nbsp;<span>{{ item.time }}</span>
            </div>
            <div class="interaction-content">
              <span>{{ item.content }}</span>
            </div>
            <div class="quote-info" v-show="item.replyContent !== null">
              {{ item.replyContent }}
            </div>
          </div>
          <div class="extra" @click="toMain(item.nid)">
            <img alt="笔记封面" class="extra-image" :src="item.noteCover" />
          </div>
        </div>
      </li>
    </ul>
  </div>
</template>

<script lang="ts" setup>
import { ref } from "vue";
import { formatSessionListTime } from "@/utils/util";
import { getNoticeComment } from "@/api/comment";
import { useUserStore } from "@/store/userStore";
import { useRouter } from "vue-router";

const userStore = useUserStore();
const router = useRouter();
const currentPage = ref(1);
const pageSize = 12;
const dataList = ref<Array<any>>([]);
const dataTotal = ref(0);
const currentUid = ref("");

const emit = defineEmits(["clickMain"]);

const loadMore = () => {
  currentPage.value += 1;
  getPageData();
};

const toMain = (nid: string) => {
  emit("clickMain", nid);
};

const toUser = (uid: string) => {
  router.push({ name: "user", query: { uid: uid } });
};

const getPageData = () => {
  getNoticeComment(currentPage.value, pageSize).then((res) => {
    const { records, total } = res.data;
    dataTotal.value = total;
    records.forEach((item: any) => {
      item.time = formatSessionListTime(item.time);
      dataList.value.push(item);
    });
  });
};

const initData = () => {
  currentUid.value = userStore.getUserInfo().id;
  getPageData();
};
initData();
</script>

<style lang="less" scoped>
textarea {
  overflow: auto;
}

.message-container {
  width: 100%;

  .message-item {
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

      .extra {
        min-width: 48px;
        flex-shrink: 0;
        margin-left: 24px;

        .extra-image {
          cursor: pointer;
          width: 48px;
          height: 48px;
          border: 1px solid rgba(0, 0, 0, 0.02);
          border-radius: 6px;
          object-fit: cover;
        }
      }
    }
  }
}
</style>
