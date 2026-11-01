import { createApi } from "@/utils/request";
import type { NoteDTO } from "@/type/note";

const request = createApi("/web/es/note");

/**
 * 
 * @param currentPage 
 * @param pageSize 
 * @returns 
 */
export const getRecommendNote = (currentPage: number, pageSize: number) => {
  return request<any>({
    url: `/getRecommendNote/${currentPage}/${pageSize}`, // mock接口
    method: "get",
  });
};

/**
 * 
 * @param currentPage 
 * @param pageSize 
 * @param data 
 * @returns 
 */
export const getNoteByDTO = (currentPage: number, pageSize: number, data: NoteDTO) => {
  return request<any>({
    url: `/getNoteByDTO/${currentPage}/${pageSize}`, // mock接口
    method: "post",
    data: data
  });
};

export const getCategoryAgg = (data: NoteDTO) => {
  return request<any>({
    url: "/getCategoryAgg", // mock接口
    method: "post",
    data: data
  });
};

