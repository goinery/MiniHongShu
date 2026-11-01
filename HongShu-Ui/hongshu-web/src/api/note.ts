import { createApi } from "@/utils/request";

const request = createApi("/web/note");

/**
 * 根据笔记id获取笔记
 * @param noteId 笔记id
 * @returns 笔记
 */
export const getNoteById = (noteId: string) => {
  return request<any>({
    url: "/getNoteById", // mock接口
    method: "get",
    params: {
      noteId,
    },
  });
};

/**
 * 保存笔记
 * @param data 笔记实体
 * @returns 笔记id
 */
export const saveNoteByDTO = (data: any) => {
  return request<any>({
    url: "/saveNoteByDTO",
    method: "post",
    data: data,
  });
};

/**
 * 更新笔记
 * @param data 笔记实体
 * @returns 笔记id
 */
export const updateNoteByDTO = (data: any) => {
  return request<any>({
    url: "/updateNoteByDTO",
    method: "post",
    data: data,
  });
};

/**
 * 置顶笔记
 * @param noteId 
 * @returns 
 */
export const pinnedNote = (noteId: string) => {
  return request<any>({
    url: "/pinnedNote", // mock接口
    method: "get",
    params: {
      noteId,
    },
  });
};

/**
 * 删除笔记
 * @param data 
 * @returns 
 */
export const deleteNoteByIds = (data: any) => {
  return request<any>({
    url: "/deleteNoteByIds", // mock接口
    method: "post",
    data: data,
  });
};
