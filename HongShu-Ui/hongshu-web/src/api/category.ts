import { createApi } from "@/utils/request";

const request = createApi("/web/category");

/**
 * 获取树形分类数据
 * @returns 分类数据
 */
export const getCategoryTreeData = () => {
  return request<any>({
    url: "/getCategoryTreeData",
    method: "get",
  });
};
