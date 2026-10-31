import { defineStore } from "pinia";
import { ref } from "vue";
import { storage } from "@/utils/storage";
import { getUserInfoByToken, refreshToken } from "@/api/user";
import { store } from "@/store";
import type { User } from "@/type/user";

// 使用setup模式定义
export const userStore = defineStore("userStore", () => {
  const token = ref("");
  // 缓存资料不能证明会话有效；首次访问先向服务端校验，再恢复登录 UI。
  const profile = ref<User | null>(null);
  let sessionChecked = false;
  let pendingRestore: Promise<void> | null = null;
  const sessionVersion = ref(0);

  const getSessionVersion = () => sessionVersion.value;
  const getRefreshToken = () => {
    const value = storage.get("refreshToken");
    return typeof value === "string" ? value : "";
  };
  const hasSession = () => Boolean(getToken() || getRefreshToken() || profile.value);

  const getToken = (): string => {
    const value = storage.get("accessToken");
    return typeof value === "string" ? value : "";
  };

  const getUserInfo = (): User => {
    return profile.value as User;
  };

  const setUserInfo = (data: User) => {
    profile.value = data;
    storage.set("userInfo", data);
    sessionChecked = true;
  };

  const isLogin = () => {
    const user = profile.value;
    return Boolean(user?.id && getToken());
  };

  const setTokens = (accessToken: string, refreshToken: string) => {
    token.value = accessToken;
    storage.set("accessToken", accessToken);
    storage.set("refreshToken", refreshToken);
  };

  const startSession = (data: { accessToken: string; refreshToken: string; userInfo: User }) => {
    sessionVersion.value++;
    setTokens(data.accessToken, data.refreshToken);
    setUserInfo(data.userInfo);
  };

  const getNewToken = async (previousRefreshToken: string) => {
    const version = sessionVersion.value;
    const result = await refreshToken(previousRefreshToken);
    if (version !== sessionVersion.value || getRefreshToken() !== previousRefreshToken) {
      throw new Error("登录状态已变化");
    }
    const data = result.data;
    if (!data?.accessToken || !data?.refreshToken) {
      loginOut();
      throw new Error("登录凭证无效");
    }
    setTokens(data.accessToken, data.refreshToken);
    window.dispatchEvent(new Event("hongshu:token"));
    return data.accessToken as string;
  };

  const loginOut = () => {
    sessionVersion.value++;
    profile.value = null;
    token.value = "";
    sessionChecked = true;
    storage.remove("accessToken");
    storage.remove("refreshToken");
    storage.remove("userInfo");
    window.dispatchEvent(new Event("hongshu:logout"));
  };

  const restoreSession = async () => {
    if (sessionChecked) return;
    if (!getToken()) {
      loginOut();
      return;
    }
    if (!pendingRestore) {
      const version = sessionVersion.value;
      pendingRestore = getUserInfoByToken().then((result) => {
        if (version === sessionVersion.value && getToken()) setUserInfo(result.data);
      }).catch(() => {
        // 鉴权失败由统一拦截器清理；网络错误保留凭证，下次导航可再次校验。
      }).finally(() => { pendingRestore = null; });
    }
    await pendingRestore;
  };

  return { token, getToken, getRefreshToken, getSessionVersion, hasSession, getNewToken,
    getUserInfo, setUserInfo, startSession, loginOut, isLogin, restoreSession };
});

export function useUserStore() {
  return userStore(store);
}
