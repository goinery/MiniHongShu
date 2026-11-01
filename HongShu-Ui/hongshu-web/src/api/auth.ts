import { createApi } from "@/utils/request";
import type { UserLogin } from "@/type/user";

const request = createApi("/web/auth");

export const login = (data: any) => {
  return request<any>({
    url: "/login",
    method: "post",
    data,
  });
};

export const getUserInfoByToken = () => {
  return request<any>({
    url: "/getUserInfoByToken",
    method: "get",
    // 由请求拦截器携带最新 token，刷新后的重试不会继续使用旧值。
    timeout: 10000,
  });
};

export const refreshToken = (refreshToken: string) => {
  return request<any>({
    url: "/refreshToken",
    method: "get",
    params: {
      refreshToken,
    },
  });
};

export const loginByCode = (data: UserLogin) => {
  return request<any>({
    url: "/loginByCode",
    method: "post",
    data,
  });
};

export const loginOut = (userId: string) => {
  return request<any>({
    url: "/loginOut",
    method: "get",
    params: {
      userId,
    },
  });
};
