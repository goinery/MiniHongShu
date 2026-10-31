import axios, { type InternalAxiosRequestConfig } from "axios";
import { useUserStore } from "@/store/userStore";
import { ElMessage } from "element-plus";

type SessionRequestConfig = InternalAxiosRequestConfig & { retried?: boolean; sessionVersion?: number };
let pendingRefresh: { version: number; promise: Promise<string> } | null = null;

const service = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API,
  timeout: 50000,
  headers: { "Content-Type": "application/json;charset=utf-8" },
});

const clearLogin = () => {
  if (!useUserStore().hasSession()) return;
  useUserStore().loginOut();
  ElMessage.warning("登录已失效，请重新登录");
};

const isCurrentSession = (config?: SessionRequestConfig) =>
  config?.sessionVersion === undefined || config.sessionVersion === useUserStore().getSessionVersion();

service.interceptors.request.use((config: SessionRequestConfig) => {
  if (!isCurrentSession(config)) return Promise.reject(new Error("登录状态已变化"));
  config.sessionVersion = useUserStore().getSessionVersion();
  const token = useUserStore().getToken();
  if (token) config.headers.accessToken = token;
  else delete config.headers.accessToken;
  if (config.data instanceof FormData) delete config.headers["Content-Type"];
  return config;
});

service.interceptors.response.use(async (response) => {
  const config = response.config as SessionRequestConfig;
  // 旧账号的迟到响应不能结束新会话，也不能把旧请求以新账号身份重发。
  if (!isCurrentSession(config)) return Promise.reject(new Error("登录状态已变化"));
  const body = response.data;
  if (body.code === 200) return body;
  if (body.code === 501 && !config.retried && !config.url?.includes("/refreshToken")) {
    config.retried = true;
    const userStore = useUserStore();
    if (config.headers.accessToken !== userStore.getToken() && userStore.getToken()) {
      return service.request(config);
    }
    const version = userStore.getSessionVersion();
    if (!pendingRefresh || pendingRefresh.version !== version) {
      const token = userStore.getRefreshToken();
      if (!token) { clearLogin(); return Promise.reject(body); }
      const promise = userStore.getNewToken(token).finally(() => {
        if (pendingRefresh?.promise === promise) pendingRefresh = null;
      });
      pendingRefresh = { version, promise };
    }
    await pendingRefresh.promise;
    return service.request(config);
  }
  if (body.code === 401 || body.code === 501) {
    clearLogin();
  } else {
    ElMessage.error(body.message || body.msg || "操作失败，请稍后再试");
  }
  return Promise.reject(body);
}, (error) => {
  if (!isCurrentSession(error.config)) return Promise.reject(error);
  if (error.response?.status === 401) clearLogin();
  else ElMessage.error(error.response?.data?.message || error.response?.data?.msg || "请求失败，请检查网络后重试");
  return Promise.reject(error);
});

export default service;
