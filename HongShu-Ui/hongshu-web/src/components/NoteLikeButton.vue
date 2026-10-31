<template>
  <button
    type="button"
    class="like-wrapper note-like-button"
    :class="{ 'is-liked': state.isLike }"
    :aria-label="state.isLike ? '取消点赞' : '点赞'"
    :aria-pressed="state.isLike"
    :aria-busy="pending"
    :disabled="pending || !noteId"
    @click.stop="toggle"
  >
    <i class="iconfont" :class="state.isLike ? 'icon-follow-fill' : 'icon-follow'" aria-hidden="true"></i>
    <span class="count">{{ state.likeCount }}</span>
  </button>
</template>

<script setup lang="ts">
import { computed, inject } from "vue";
import { ElMessage } from "element-plus";
import { useUserStore } from "@/store/userStore";
import { useNoteLikeStore } from "@/store/noteLikeStore";

const props = defineProps<{ noteId: string; authorId: string; isLike?: boolean; likeCount: number }>();
const userStore = useUserStore();
const likes = useNoteLikeStore();
const openLogin = inject<() => void>("openLogin");
const state = computed(() => likes.getState(String(props.noteId), props));
const pending = computed(() => likes.pending.has(String(props.noteId)));

const toggle = () => {
  if (!userStore.isLogin()) {
    if (openLogin) openLogin();
    else ElMessage.warning("请先登录后再点赞");
    return;
  }
  void likes.toggle(String(props.noteId), props.authorId, state.value);
};
</script>

<style scoped>
.note-like-button {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  gap: 2px;
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
  font: inherit;
  line-height: inherit;
  cursor: pointer;
}
.note-like-button .iconfont { font-size: var(--note-like-icon-size, 16px); }
.note-like-button.is-liked .iconfont { color: #f87171; }
.note-like-button:hover { color: var(--el-color-primary); }
.note-like-button:disabled { cursor: wait; }
.note-like-button:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 4px;
  border-radius: 4px;
}
</style>
