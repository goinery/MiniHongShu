import { createApi } from "@/utils/request";

const request = createApi("/web/tag");

/**
 * 
 * @param currentPage 
 * @param pageSize 
 * @param keyword 
 * @returns 
 */
export const getTagByKeyword = (currentPage: number, pageSize: number,keyword:string) => {
    return request<any>({
      url: `/getTagByKeyword/${currentPage}/${pageSize}`, // mock接口
      method: "get",
      params:{
        keyword
      }
    });
  };
