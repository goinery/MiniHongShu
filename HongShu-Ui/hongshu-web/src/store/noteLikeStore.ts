import { defineStore } from "pinia";
import { reactive, watch } from "vue";
import { likeOrCollectionByDTO } from "@/api/likeOrCollection";
import { useUserStore } from "@/store/userStore";

interface LikeState {
  isLike: boolean;
  likeCount: number;
}

export const useNoteLikeStore = defineStore("noteLikes", () => {
  const userStore = useUserStore();
  const states = reactive(new Map<string, LikeState>());
  const pending = reactive(new Set<string>());
  let sessionVersion = 0;

  watch(() => userStore.getUserInfo()?.id, () => {
    sessionVersion++;
    states.clear();
    pending.clear();
  }, { flush: "sync" });

  const getState = (id: string, snapshot: { isLike?: boolean; likeCount: number }): LikeState =>
    states.get(id) ?? { isLike: Boolean(snapshot.isLike), likeCount: Number(snapshot.likeCount || 0) };

  // 同一篇笔记的卡片与详情共用状态和请求锁，避免连续点击触发多次切换。
  const toggle = async (id: string, authorId: string, snapshot: LikeState) => {
    if (!id || !userStore.isLogin() || pending.has(id)) return;
    const before = getState(id, snapshot);
    const version = sessionVersion;
    pending.add(id);
    try {
      await likeOrCollectionByDTO({ likeOrCollectionId: id, publishUid: authorId, type: 1 });
      if (version === sessionVersion) {
        states.set(id, {
          isLike: !before.isLike,
          likeCount: Math.max(0, before.likeCount + (before.isLike ? -1 : 1)),
        });
      }
    } catch {
      // 统一请求拦截器展示失败原因，保留原有点赞状态。
    } finally {
      if (version === sessionVersion) pending.delete(id);
    }
  };

  return { states, pending, getState, toggle };
});
