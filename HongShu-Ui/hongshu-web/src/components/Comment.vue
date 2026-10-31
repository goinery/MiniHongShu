<template></template>
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
