<template></template>

<script lang="ts" setup>
import { Close, Star, ChatRound, StarFilled, ArrowDown } from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { ref, watch } from "vue";
import { getNoteById, pinnedNote, deleteNoteByIds } from "@/api/note";
import { likeOrCollectionByDTO } from "@/api/likeOrCollection";
import type { NoteInfo } from "@/type/note";
import NoteLikeButton from "@/components/NoteLikeButton.vue";
import { formateTime, getRandomString } from "@/utils/util";
import { followById } from "@/api/follower";
import Comment from "@/components/Comment.vue";
import type { CommentDTO } from "@/type/comment";
import { saveCommentByDTO, syncCommentByIds } from "@/api/comment";
import { useRouter } from "vue-router";
import { useUserStore } from "@/store/userStore";
const userStore = useUserStore();
const router = useRouter();

// 这是路由传参
// nid.value = history.state.nid;

const emit = defineEmits(["clickMain"]);

const props = defineProps({
  nid: {
    type: String,
    default: "",
  },
});

const currentUid = ref("");
const noteInfo = ref<NoteInfo>({
  id: "",
  title: "",
  content: "",
  noteCover: "",
  uid: "",
  username: "",
  avatar: "",
  imgList: [],
  type: -1,
  likeCount: 0,
  collectionCount: 0,
  commentCount: 0,
  tagList: [],
  time: "",
  isFollow: false,
  isLike: false,
  isCollection: false,
  pinned: "0",
});
const commentInput = ref<HTMLInputElement | null>(null);
const focusCommentInput = () => {
  commentInput.value?.focus({ preventScroll: true });
};
defineExpose({ focusCommentInput });

const commentValue = ref("");
const commentPlaceVal = ref("请输入内容");
const commentObject = ref<any>({});
const replyComment = ref<any>({});
const showSaveBtn = ref(false);
const currentPage = ref(1);
const seed = ref("");
const commentIds = ref<Array<string>>([]);
const noteScroller = ref(null);
const isLogin = ref(false);
const likeOrComment = ref({
  isComment: false,
});

watch(
  () => props.nid,
  (noteId) => {
    currentPage.value = 1;
    likeOrComment.value.isComment = false;
    if (noteId) {
      getNoteById(noteId).then((res: any) => {
        if (props.nid !== noteId) return;
        noteInfo.value = res.data;
        noteInfo.value.imgList = parseNoteUrls(res.data && res.data.urls);
        noteInfo.value.time = formateTime(res.data.time);
      }).catch(() => {});
    }
  },
  { immediate: true }
);

const noLoginNotice = () => {
  if (!userStore.isLogin()) {
    ElMessage.warning("用户未登录");
    return false;
  }
  return true;
};

const close = () => {
  const ids = [...commentIds.value];
  commentIds.value = [];
  emit("clickMain", props.nid, { ...likeOrComment.value });
  // 关闭操作立即生效；评论同步独立执行，网络失败由统一拦截器提示。
  if (userStore.isLogin() && ids.length > 0) {
    void syncCommentByIds(ids).catch(() => {});
  }
};

const follow = (fid: string, type: number) => {
  const _login = noLoginNotice();
  if (!_login) {
    return;
  }
  followById(fid).then(() => {
    noteInfo.value.isFollow = type == 0;
  });
};

const collecting = ref(false);
const toggleCollection = async () => {
  if (!noLoginNotice() || collecting.value) return;
  const note = noteInfo.value;
  collecting.value = true;
  try {
    await likeOrCollectionByDTO({ likeOrCollectionId: note.id, publishUid: note.uid, type: 3 });
    note.collectionCount = Math.max(0, note.collectionCount + (note.isCollection ? -1 : 1));
    note.isCollection = !note.isCollection;
  } catch {
    // 统一请求拦截器展示失败原因。
  } finally {
    collecting.value = false;
  }
};

const pinned = (noteId: string, type: string) => {
  pinnedNote(noteId)
    .then((res: any) => {
      if (res.data) {
        noteInfo.value.pinned = type;
      }
    })
    .catch(() => {
      ElMessage.warning("最多只能置顶3个笔记");
    });
};

const parseNoteUrls = (raw: any): string[] => {
  if (Array.isArray(raw)) return raw.filter(Boolean);
  if (typeof raw !== "string" || !raw) return [];
  try {
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed.filter(Boolean) : [];
  } catch {
    return [];
  }
};

const onOwnerCommand = (cmd: string) => {
  const id = noteInfo.value.id;
  if (cmd === "pin") pinned(id, "1");
  else if (cmd === "unpin") pinned(id, "0");
  else if (cmd === "delete") deleteNote(id);
  else if (cmd === "edit") toEdit(id);
};

const deleteNote = async (noteId: string) => {
  try {
    await ElMessageBox.confirm("确定删除这篇笔记？", "提示", {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning",
    });
  } catch {
    return;
  }
  try {
    await deleteNoteByIds([noteId]);
    ElMessage.success("删除成功");
    emit("clickMain", noteId, { deleted: true });
  } catch (e: any) {
    ElMessage.error((e && (e.message || e.msg)) || "删除失败");
  }
};

const toEdit = (noteId: string) => {
  router.push({ path: "/push", query: { date: Date.now(), noteId: noteId } });
};

const clickComment = (comment: any) => {
  commentObject.value = comment;
  commentPlaceVal.value = "回复" + comment.username;
};

const commenInput = (e: any) => {
  const { value } = e.target;
  commentValue.value = value;
  showSaveBtn.value = commentValue.value.length > 0 || commentObject.value.pid !== undefined;
};

const saveComment = () => {
  const _login = noLoginNotice();
  if (!_login) {
    return;
  }
  const comment = {} as CommentDTO;
  comment.nid = props.nid;
  comment.noteUid = noteInfo.value.uid;
  if (commentObject.value.pid === undefined) {
    comment.pid = "0";
    comment.replyId = "0";
    comment.replyUid = noteInfo.value.uid;
    comment.level = 1;
  } else if (commentObject.value.pid == "0") {
    comment.pid = commentObject.value.id;
    comment.replyId = commentObject.value.id;
    comment.replyUid = commentObject.value.uid;
    comment.level = 2;
  } else {
    comment.pid = commentObject.value.pid;
    comment.replyId = commentObject.value.id;
    comment.replyUid = commentObject.value.uid;
    comment.level = 2;
  }

  comment.content = commentValue.value;
  saveCommentByDTO(comment).then((res: any) => {
    replyComment.value = res.data;
    replyComment.value.replyUsername = commentObject.value.username;
    commentValue.value = "";
    commentObject.value = {};
    commentPlaceVal.value = "请输入内容";
    showSaveBtn.value = false;
    seed.value = getRandomString(12);
    commentIds.value.push(res.data.id);
    likeOrComment.value.isComment = true;
  });
};

const clearCommeent = () => {
  commentValue.value = "";
  commentObject.value = {};
  commentPlaceVal.value = "请输入内容";
  showSaveBtn.value = false;
};

const loadMoreData = () => {
  console.log("main加载更多");
  const container = noteScroller.value as any;
  if (container.scrollTop + container.clientHeight >= container.scrollHeight) {
    currentPage.value += 1;
    console.log("到底了");
  }
};

const initData = () => {
  isLogin.value = userStore.isLogin();
  if (isLogin.value) {
    currentUid.value = userStore.getUserInfo().id;
  }
};

initData();
</script>

<style lang="less" scoped></style>
