import { onMounted, onUnmounted, watch } from "vue";
import { getChatUserList, getCountMessage } from "@/api/im";
import { buildWsUrl } from "@/constant/constant";
import { useImStore } from "@/store/imStore";
import { useUserStore } from "@/store/userStore";

const MAX_RETRIES = 5;
const RETRY_INTERVAL_MS = 1000;
const MAX_RETRY_DELAY_MS = 5000;

/** 统一维护消息连接，丢弃已结束会话的异步结果。 */
export function useMessaging() {
  const userStore = useUserStore();
  const imStore = useImStore();
  let socket: WebSocket | null = null;
  let retryTimer: ReturnType<typeof setTimeout> | undefined;
  let retryCount = 0;
  let generation = 0;
  let sessionVersion = userStore.getSessionVersion();
  let disposed = false;

  const disconnect = () => {
    clearTimeout(retryTimer);
    retryTimer = undefined;
    if (!socket) return;
    socket.onopen = null;
    socket.onclose = null;
    socket.onmessage = null;
    socket.close();
    socket = null;
  };

  const isCurrentSession = (uid: string, version: number) =>
    !disposed && generation === version && sessionVersion === userStore.getSessionVersion() &&
    userStore.isLogin() && userStore.getUserInfo()?.id === uid;

  const receiveMessage = (event: MessageEvent) => {
    try {
      const message = JSON.parse(event.data);
      if (!message || typeof message !== "object") return;
      if (message.msgType === 0 && message.content && typeof message.content === "object") {
        imStore.setCountMessage({
          ...imStore.countMessage,
          likeOrCollectionCount: message.content.likeOrCollectionCount,
          commentCount: message.content.commentCount,
          followCount: message.content.followCount,
        });
      } else if (message.msgType === 1 || message.msgType === 2) {
        imStore.setMessage(message);
      } else if (message.msgType === 5 && Array.isArray(message.content)) {
        imStore.setUserList(message.content);
      }
    } catch {
      console.warn("忽略无法解析的实时消息");
    }
  };

  const connect = (uid: string, version: number) => {
    disconnect();
    if (!isCurrentSession(uid, version)) return;
    const connection = new WebSocket(buildWsUrl(uid, userStore.getToken()));
    socket = connection;
    connection.onopen = () => {
      if (socket === connection) retryCount = 0;
    };
    connection.onmessage = (event) => {
      if (socket === connection && isCurrentSession(uid, version)) receiveMessage(event);
    };
    connection.onclose = () => {
      if (socket !== connection || !isCurrentSession(uid, version)) return;
      socket = null;
      if (retryCount >= MAX_RETRIES) return;
      retryCount++;
      retryTimer = setTimeout(() => connect(uid, version),
        Math.min(RETRY_INTERVAL_MS * retryCount, MAX_RETRY_DELAY_MS));
    };
  };

  const reset = () => {
    generation++;
    retryCount = 0;
    disconnect();
    imStore.reset();
  };

  const initialize = async (uid: string | undefined) => {
    reset();
    sessionVersion = userStore.getSessionVersion();
    if (!uid || !userStore.isLogin()) return;
    const version = generation;
    connect(uid, version);
    try {
      const [chats, counts] = await Promise.all([getChatUserList(), getCountMessage()]);
      if (!isCurrentSession(uid, version)) return;
      imStore.setUserList(chats.data);
      imStore.setCountMessage({
        ...counts.data,
        chatCount: chats.data.reduce((total: number, item: { count: number }) => total + item.count, 0),
      });
    } catch {
      // 请求层统一处理网络错误和登录失效提示。
    }
  };

  const reconnect = () => {
    const uid = userStore.getUserInfo()?.id;
    if (!uid) return;
    retryCount = 0;
    connect(uid, generation);
  };

  watch([() => userStore.getUserInfo()?.id, () => userStore.getSessionVersion()] as const,
    ([uid]) => initialize(uid), { immediate: true });
  onMounted(() => {
    window.addEventListener("hongshu:logout", reset);
    window.addEventListener("hongshu:token", reconnect);
  });
  onUnmounted(() => {
    disposed = true;
    reset();
    window.removeEventListener("hongshu:logout", reset);
    window.removeEventListener("hongshu:token", reconnect);
  });
}
