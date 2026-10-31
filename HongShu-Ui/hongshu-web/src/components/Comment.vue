<template>
  <div class="comments-container">
    <div class="total">共{{ computedTotal }}条评论</div>
    <div class="list-container">
      <div class="parent-comment" v-for="(oneComment, oneIndex) in dataList" :key="oneIndex">
        <div class="comment-item">
          <div class="comment-inner-container">
            <div class="avatar">
              <img alt="用户头像" class="avatar-item" :src="oneComment.avatar" />
            </div>
            <div class="right">
              <div class="author-wrapper">
                <div class="author">
                  <a class="name">{{ oneComment.username }}</a>
                </div>
              </div>
              <div class="content">{{ oneComment.content }}</div>

              <div class="info">
                <div class="date">
                  <span>{{ oneComment.time }}</span>
                </div>
                <div class="interactions">
                  <div class="like">
                    <span
                      class="like-wrapper"
                      v-if="oneComment.isLike"
                      @click="likeComment(oneComment, -1, oneIndex, -1)"
                    >
                      <i
                        class="iconfont icon-follow-fill"
                        :style="{ width: '1em', height: '1em', color: oneComment.isLike ? 'red' : 'black' }"
                        v-if="oneComment.isLike"
                      >
                      </i>
                      <i class="iconfont icon-follow" style="width: 1em; height: 1em" v-else></i>
                      <span class="count">{{ oneComment.likeCount }}</span>
                    </span>
                    <span class="like-wrapper" v-else @click="likeComment(oneComment, 1, oneIndex, -1)">
                      <i class="iconfont icon-follow" style="width: 1em; height: 1em"></i>
                      <span class="count">{{ oneComment.likeCount }}</span>
                    </span>
                  </div>
                  <div class="reply" @click="saveComment(oneComment, oneIndex, 0)">
                    <span class="like-wrapper">
                      <ChatRound style="width: 1.2em; height: 1.2em" />
                      <span class="count">{{ oneComment.twoCommentCount }}</span>
                    </span>
                  </div>
                  <div class="reply" v-if="canDeleteComment(oneComment)" @click.stop="removeComment(oneComment, oneIndex, -1)">
                    <span class="like-wrapper"><span class="count">删除</span></span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="reply-container">
          <div class="list-container">
            <div class="comment-item" v-for="(twoComment, twoIndex) in oneComment.children" :key="twoIndex">
              <div class="comment-inner-container">
                <div class="avatar">
                  <img alt="用户头像" class="avatar-item" :src="twoComment.avatar" />
                </div>
                <div class="right">
                  <div class="author-wrapper">
                    <div class="author">
                      <a class="name">{{ twoComment.username }}</a>
                    </div>
                  </div>
                  <div class="content">
                    回复<span style="color: rgba(61, 61, 61, 0.8)">{{ twoComment.replyUsername }}: </span
                    >{{ twoComment.content }}
                  </div>

                  <div class="info">
                    <div class="date">
                      <span>{{ twoComment.time }}</span>
                    </div>
                    <div class="interactions">
                      <div class="like">
                        <span
                          class="like-wrapper"
                          v-if="twoComment.isLike"
                          @click="likeComment(twoComment, -1, oneIndex, twoIndex)"
                        >
                          <i
                            class="iconfont icon-follow-fill"
                            style="width: 1em; height: 1em; color: red"
                          >
                          </i>
                          <span class="count">{{ twoComment.likeCount }}</span>
                        </span>
                        <span class="like-wrapper" @click="likeComment(twoComment, 1, oneIndex, twoIndex)" v-else>
                          <i class="iconfont icon-follow" style="width: 1em; height: 1em"></i>
                          <span class="count">{{ twoComment.likeCount }}</span>
                        </span>
                      </div>
                      <div class="reply" @click="saveComment(twoComment, oneIndex, twoIndex)">
                        <span class="like-wrapper">
                          <ChatRound style="width: 1.2em; height: 1.2em" />
                          <span class="count">回复</span>
                        </span>
                      </div>
                      <div
                        class="reply"
                        v-if="canDeleteComment(twoComment)"
                        @click.stop="removeComment(twoComment, oneIndex, twoIndex)"
                      >
                        <span class="like-wrapper"><span class="count">删除</span></span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div
            class="show-more"
            v-if="
              oneComment.twoCommentCount >= commentTotalMap.get(oneComment.id) &&
              oneComment.twoCommentCount > showTwoCommentCount
            "
            @click="loadTwoMore(oneComment.id, oneIndex)"
          >
            展开更多的回复
          </div>
          <div
            class="show-more"
            v-if="
              oneComment.twoCommentCount < commentTotalMap.get(oneComment.id) &&
              oneComment.twoCommentCount > showTwoCommentCount
            "
            @click="reback(oneComment.id, oneIndex)"
          >
            收起所有回复
          </div>
        </div>
      </div>
    </div>
    <div style="padding-bottom: 100px"></div>
  </div>
</template>
<script lang="ts" setup>
import { ChatRound } from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { ref, watch } from "vue";
import { getCommentWithCommentByNoteId, getTwoCommentPageByOneCommentId, deleteCommentById } from "@/api/comment";
import { likeOrCollectionByDTO } from "@/api/likeOrCollection";
import type { LikeOrCollectionDTO } from "@/type/likeOrCollection";
import { formateTime } from "@/utils/util";
import { useUserStore } from "@/store/userStore";
const userStore = useUserStore();
const props = defineProps({
  nid: {
    type: String,
    default: "",
  },
  noteUid: {
    type: String,
    default: "",
  },
  replyComment: {
    type: Object,
    default: () => ({}),
  },
  currentPage: {
    type: Number,
    default: 1,
  },
  seed: {
    type: String,
    default: "",
  },
});
const emit = defineEmits(["clickComment"]);

// const currentPage = ref(1);

const dataList = ref<Array<any>>([]);
const commentTotal = ref(0);
const computedTotal = ref(0);
const oneIndex = ref(-1);
const twoIndex = ref(-1);

const pageSize = 7;
const twoPageSize = 10;
const showTwoCommentCount = 3;
const commentMap = new Map();
const commentTotalMap = new Map();

const currentUserId = () => {
  try {
    const user = userStore.getUserInfo();
    return user && user.id ? String(user.id) : "";
  } catch {
    return "";
  }
};

const canDeleteComment = (comment: any) => {
  const me = currentUserId();
  if (!me || !comment) return false;
  return String(comment.uid) === me || (props.noteUid && String(props.noteUid) === me);
};

const removeComment = async (comment: any, one: number, two: number) => {
  try {
    await ElMessageBox.confirm("确定删除这条评论？", "提示", {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning",
    });
  } catch {
    return;
  }
  try {
    await deleteCommentById(comment.id);
    if (two === -1) {
      const removed = dataList.value[one];
      const childCount = Number((removed && removed.twoCommentCount) || (removed && removed.children && removed.children.length) || 0);
      dataList.value.splice(one, 1);
      computedTotal.value = Math.max(0, computedTotal.value - 1 - childCount);
    } else {
      dataList.value[one].children.splice(two, 1);
      if (dataList.value[one].twoCommentCount > 0) dataList.value[one].twoCommentCount -= 1;
      computedTotal.value = Math.max(0, computedTotal.value - 1);
    }
    ElMessage.success("删除成功");
  } catch (e: any) {
    ElMessage.error((e && (e.message || e.msg)) || "删除失败");
  }
};

const likeComment = (comment: any, status: number, one: number, two: number) => {
  const data = {} as LikeOrCollectionDTO;
  data.likeOrCollectionId = comment.id;
  data.publishUid = comment.uid;
  data.type = 2;
  likeOrCollectionByDTO(data).then(() => {
    if (two === -1) {
      dataList.value[one].isLike = status == 1;
      dataList.value[one].likeCount += status;
    } else {
      dataList.value[one].children[two].isLike = status == 1;
      dataList.value[one].children[two].likeCount += status;
    }
  });
};

const saveComment = (comment: any, one: number, two: number) => {
  oneIndex.value = one;
  twoIndex.value = two;
  emit("clickComment", comment);
};

const addComment = () => {
  //   if (props.replyComment.pid === undefined) return;

  let comment = props.replyComment;
  console.log("comment", comment);
  comment.likeCount = 0;
  comment.twoCommentCount = 0;
  comment.time = formateTime(new Date().getTime());
  if (comment.pid === "0") {
    dataList.value.splice(0, 0, comment);
  } else {
    if (dataList.value[oneIndex.value].children == null) {
      dataList.value[oneIndex.value].children = [];
    }
    dataList.value[oneIndex.value].children.splice(twoIndex.value + 1, 0, comment);
  }
  computedTotal.value += 1;
};

const loadTwoMore = (oneCommentId: string, index: number) => {
  let page = commentMap.get(oneCommentId);
  page += 1;
  getTwoCommentPageByOneCommentId(page, twoPageSize, oneCommentId).then((res) => {
    const { records } = res.data;
    if (page === 1) {
      const spliceData = records.splice(showTwoCommentCount, records.length);
      spliceData.forEach((item: any) => {
        item.time = formateTime(item.time);
        dataList.value[index].children.push(item);
      });
    } else {
      records.forEach((item: any) => {
        item.time = formateTime(item.time);
        dataList.value[index].children.push(item);
      });
    }
    commentTotalMap.set(oneCommentId, commentTotalMap.get(oneCommentId) + twoPageSize);
    commentMap.set(oneCommentId, page);
  });
};

const reback = (oneCommentId: string, index: number) => {
  commentTotalMap.set(oneCommentId, 0);
  commentMap.set(oneCommentId, 0);
  dataList.value[index].children = dataList.value[index].children.splice(0, showTwoCommentCount);
};

const getCommentData = () => {
  computedTotal.value = 0;
  getCommentWithCommentByNoteId(props.currentPage, pageSize, props.nid).then((res: any) => {
    const { records, total } = res.data;
    records.forEach((item: any) => {
      item.time = formateTime(item.time);
      const twoComments = item.children;
      // 设置每一个一级评论的集合
      commentMap.set(item.id, 0);
      commentTotalMap.set(item.id, 0);
      if (twoComments != null) {
        const twoData = [] as Array<any>;
        twoComments.forEach((element: any) => {
          element.time = formateTime(element.time);
          twoData.push(element);
        });

        item.children = twoData;
      }
      computedTotal.value += item.twoCommentCount + 1;
      dataList.value.push(item);
    });
    console.log("---所有评论", dataList.value);
    commentTotal.value = total;
    if (pageSize * props.currentPage >= commentTotal.value) return;
  });
};

watch(
  () => [props.nid, props.seed, props.currentPage],
  ([newNid, newSeed], [oldNid, oldSeed]) => {
    console.log("评论功能", newNid, oldNid, props.currentPage);
    if (newNid !== oldNid) {
      dataList.value = [];
      if (!newNid) return;
      getCommentData();
    }
    if (newSeed !== oldSeed) {
      addComment();
    }
  }
);
</script>
<style lang="less" scoped></style>
