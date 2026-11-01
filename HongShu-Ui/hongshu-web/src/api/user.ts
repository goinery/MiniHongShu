import { createApi } from "@/utils/request";

const request = createApi("/web/user");

/**
 * 
 * @param currentPage 
 * @param pageSize 
 * @param userId 
 * @param type 
 * @returns 
 */
export const getTrendByUser = (currentPage:number,pageSize:number,userId:string,type:number) => {
  return request<any>({
    url: `/getTrendByUser/${currentPage}/${pageSize}`, // mock接口
    method: "get",
    params: {
      userId,
      type
    },
  });
};

/**
 * 
 * @param userId 
 * @returns 
 */
export const getUserById = (userId:string) => {
  return request<any>({
    url: "/getUserById", // mock接口
    method: "get",
    params: {
      userId
    },
  });
};

export const updateUser = (data: any) => {
  return request<any>({
    url: "/updateUser", // mock接口
    method: "post",
    data,
  });
};

export const changePassword = (data: { oldPassword: string; newPassword: string; confirmPassword: string }) => {
  return request<any>({ url: "/changePassword", method: "post", data });
};

export const getUserByKeyword = (currentPage: number, pageSize: number, keyword: string) => {
  return request<any>({
    url: `/getUserByKeyword/${currentPage}/${pageSize}`, // mock接口
    method: "get",
    params: {
      keyword
    },
  });
};

