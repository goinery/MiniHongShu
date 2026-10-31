<template>
  <div class="note-detail-mask" @click.self="close">
    <div class="note-container">
      <div class="media-container">
        <el-carousel height="90vh" :autoplay="false">
          <el-carousel-item v-for="(item, index) in noteInfo.imgList" :key="index">
            <el-image
              style="width: 100%; height: 100%"
              :src="item"
              fit="contain"
              class="animate__animated animate__zoomIn animate__delay-0.5s"
            />
          </el-carousel-item>
        </el-carousel>
      </div>

      <div class="interaction-container">
        <div class="author-container">
          <div class="author-me">
            <RouterLink class="info author-link" :to="{ name: 'user', query: { uid: noteInfo.uid } }">
              <img alt="用户头像" class="avatar-item author-link__avatar" style="width: 40px; height: 40px" :src="noteInfo.avatar" />
              <span class="name author-link__name">{{ noteInfo.username }}</span>
            </RouterLink>
            <div class="follow-btn" v-if="currentUid !== noteInfo.uid">
              <el-button
                class="note-follow-button"
                :class="{ 'followed-button': noteInfo.isFollow }"
                type="primary"
                :plain="noteInfo.isFollow"
                round
                :aria-label="(noteInfo.isFollow ? '取消关注' : '关注') + noteInfo.username"
                :aria-pressed="noteInfo.isFollow"
                @click="follow(noteInfo.uid, noteInfo.isFollow ? 1 : 0)"
              >
                {{ noteInfo.isFollow ? '取消关注' : '关注' }}
              </el-button>
            </div>
            <div class="follow-btn" v-else>
              <el-dropdown @command="onOwnerCommand">
                <el-button type="danger" size="large" round>
                  编辑
                  <el-icon class="el-icon--right">
                    <arrow-down />
                  </el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-if="noteInfo.pinned === '0'" command="pin">置顶</el-dropdown-item>
                    <el-dropdown-item v-else command="unpin">取消置顶</el-dropdown-item>
                    <el-dropdown-item command="delete">删除</el-dropdown-item>
                    <el-dropdown-item command="edit">编辑</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>

          <div class="note-scroller" ref="noteScroller" @scroll="loadMoreData">
            <div class="note-content">
              <div class="title">{{ noteInfo.title }}</div>
              <div class="desc">
                <span>{{ noteInfo.content }} <br /></span>
                <a class="tag tag-search" v-for="(item, index) in noteInfo.tagList" :key="index">#{{ item.title }}</a>
              </div>
              <div class="bottom-container">
                <span class="date">{{ noteInfo.time }}</span>
              </div>
            </div>
            <div class="divider interaction-divider"></div>

            <!-- 评论 -->
            <div class="comments-el">
              <Comment
                :nid="props.nid"
                :note-uid="noteInfo.uid"
                :currentPage="currentPage"
                :replyComment="replyComment"
                :seed="seed"
                @click-comment="clickComment"
              >
              </Comment>
            </div>
            <!--  -->
          </div>

          <div class="interactions-footer">
            <div class="buttons">
              <div class="left">
                <button
                  type="button"
                  class="like-wrapper interaction-action"
                  :aria-label="noteInfo.isCollection ? '取消收藏' : '收藏'"
                  :aria-pressed="noteInfo.isCollection"
                  @click="toggleCollection()"
                >
                  <StarFilled v-if="noteInfo.isCollection" style="width: 24px; height: 24px; flex-shrink: 0; color: #ffcc00" />
                  <Star v-else class="action-icon" />
                  <span class="count">{{ noteInfo.collectionCount }}</span>
                </button>
                <NoteLikeButton
                  class="collect-wrapper interaction-action"
                  style="--note-like-icon-size: 24px"
                  :note-id="noteInfo.id"
                  :author-id="noteInfo.uid"
                  :is-like="noteInfo.isLike"
                  :like-count="noteInfo.likeCount"
                />
                <span class="chat-wrapper interaction-action">
                  <ChatRound class="action-icon" />
                  <span class="count">
                    {{ noteInfo.commentCount }}
                  </span>
                </span>
              </div>
              <div class="share-wrapper"></div>
            </div>
            <div :class="showSaveBtn ? 'comment-wrapper active comment-comp ' : 'comment-wrapper comment-comp '">
              <div class="input-wrapper">
                <input
                  ref="commentInput"
                  class="comment-input"
                  v-model="commentValue"
                  type="text"
                  :placeholder="commentPlaceVal"
                  @input="commenInput"
                  @keyup.enter="saveComment"
                />
                <div class="input-buttons round-close" @click="clearCommeent" v-show="showSaveBtn">
                  <Close style="width: 1.2em; height: 1.2em" />
                </div>
              </div>
              <button class="submit" @click="saveComment">发送</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="close-cricle" @click="close">
      <div class="close close-mask-white round-close">
        <Close style="width: 1.2em; height: 1.2em; color: rgba(51, 51, 51, 0.8)" />
      </div>
    </div>

    <div class="back-desk" @click.self="close"></div>
  </div>
</template>

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

<style lang="less" scoped>
:deep(.el-dropdown-menu__item:not(.is-disabled):focus) {
  background-color: #f8f8f8;
  color: black;
}

.note-detail-mask {
  position: fixed;
  left: 0;
  top: 0;
  display: flex;
  width: 100vw;
  height: 100vh;
  z-index: var(--layer-modal);
  overflow: auto;
  animation: none;
  transition: none;

  .back-desk {
    position: fixed;
    background-color: #f4f4f4;
    opacity: 0.5;
    width: 100vw;
    height: 100vh;
    z-index: var(--layer-local-backdrop);
    animation: none;
    transition: none;
  }

  .close-cricle {
    left: 1.3vw;
    top: 1.3vw;
    position: fixed;
    display: flex;
    z-index: var(--layer-local-control);
    cursor: pointer;

    .close-mask-white {
      box-shadow:
        0 2px 8px 0 rgba(0, 0, 0, 0.04),
        0 1px 2px 0 rgba(0, 0, 0, 0.02);
      border: 1px solid rgba(0, 0, 0, 0.08);
    }

    .close {
      display: flex;
      justify-content: center;
      align-items: center;
      width: 40px;
      height: 40px;
      border-radius: 40px;
      cursor: pointer;
      transition: background-color 0.2s, box-shadow 0.2s;
      background-color: #fff;
    }

    .close :deep(svg) {
      pointer-events: none;
    }
  }

  .note-container {
    width: 65%;
    height: 86%;
    transition:
      transform 0.4s ease 0s,
      width 0.4s ease 0s;
    transform: translate(280px, 60px) scale(1);
    overflow: visible;

    display: flex;
    box-shadow:
      0 8px 64px 0 rgba(0, 0, 0, 0.04),
      0 1px 4px 0 rgba(0, 0, 0, 0.02);
    border-radius: 20px;
    background: #f8f8f8;
    transform-origin: left top;
    z-index: var(--layer-local-content);

    .media-container {
      width: 65%;

      position: relative;
      background: rgba(0, 0, 0, 0.03);
      flex-shrink: 0;
      flex-grow: 0;
      -webkit-user-select: none;
      user-select: none;
      overflow: hidden;
      border-radius: 20px 0 0 20px;
      transform: translateZ(0);
      height: 100%;
      object-fit: contain;
      min-width: 440px;
    }

    .interaction-container {
      width: 35%;
      flex-shrink: 0;
      border-radius: 0 20px 20px 0;
      position: relative;
      display: flex;
      flex-direction: column;
      flex-grow: 1;
      height: 100%;
      background-color: #fff;
      overflow: hidden;
      border-left: 1px solid rgba(0, 0, 0, 0.08);

      .author-container {
        display: flex;
        flex-direction: column;
        height: 100%;
        min-height: 0;
      }

      .author-me {
        display: flex;
        flex-shrink: 0;
        align-items: center;
        justify-content: space-between;
        width: 100%;
        padding: 24px;
        border-bottom: 1px solid transparent;

        .note-follow-button {
          min-width: 88px;
          height: 32px;
          padding: 0 12px;
        }

        .info {
          display: flex;
          align-items: center;

          .avatar-item {
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            border-radius: 100%;
            border: 1px solid rgba(0, 0, 0, 0.08);
            object-fit: cover;
          }

          .name {
            padding-left: 12px;
            height: 40px;
            display: flex;
            align-items: center;
            font-size: 16px;
            color: rgba(51, 51, 51, 0.8);
          }
        }
      }

      .note-scroller::-webkit-scrollbar {
        display: none;
      }

      .note-scroller {
        transition: scroll 0.4s;
        overflow-y: scroll;
        flex: 1;
        min-height: 0;

        .note-content {
          padding: 0 24px 24px;
          color: var(--color-primary-label);

          .title {
            margin-bottom: 8px;
            font-weight: 600;
            font-size: 18px;
            line-height: 140%;
          }

          .desc {
            margin: 0;
            font-weight: 400;
            font-size: 16px;
            line-height: 150%;
            color: #333;
            white-space: pre-wrap;
            overflow-wrap: break-word;

            .tag-search {
              cursor: pointer;
            }

            .tag {
              margin-right: 2px;
              color: #13386c;
            }
          }

          .bottom-container {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-top: 12px;

            .date {
              font-size: 14px;
              line-height: 120%;
              color: rgba(51, 51, 51, 0.6);
            }
          }
        }

        .interaction-divider {
          margin: 0 24px;
        }

        .divider {
          margin: 4px 8px;
          list-style: none;
          height: 0;
          border: solid rgba(0, 0, 0, 0.08);
          border-width: 1px 0 0;
        }

        .comments-el {
          position: relative;
        }
      }

      .interactions-footer {
        position: relative;
        width: 100%;
        background: #fff;
        flex-shrink: 0;
        padding: 12px 24px 24px;
        border-top: 1px solid rgba(0, 0, 0, 0.08);
        z-index: var(--layer-local-control);

        .buttons {
          display: flex;
          align-items: center;
          justify-content: space-between;

          .left {
            display: flex;
            align-items: center;
}
}
}
}
}
}
</style>
