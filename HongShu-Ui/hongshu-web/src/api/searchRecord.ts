import { createApi } from "@/utils/request";
import { useUserStore } from "@/store/userStore";

const request = createApi("/web/es/record");

export const getRecordByKeyWord = (keyword: string) => {
  if (!useUserStore().isLogin()) return Promise.resolve({ data: [] });
  return request<any>({
    url: "/getRecordByKeyWord",
    method: "get",
    params: {
      keyword,
    },
  });
};

export const getHotRecord = () => {
  return request<any>({
    url: "/getHotRecord",
    method: "get",
  });
};

export const addRecord = (keyword: string) => {
  if (!useUserStore().isLogin()) return Promise.resolve({ data: null });
  return request<any>({
    url: "/addRecord",
    method: "post",
    data: {
      keyword,
    },
  });
};
