import { createApi } from "@/utils/request";

const request = createApi("/web/follower");

export type RelationType = "following" | "fans";

export interface RelationUser {
  uid: string;
  username: string;
  avatar: string;
  hsId: number | null;
  fanCount: number | null;
  isFollow: boolean;
}

export const getUserRelations = (userId: string, type: RelationType, currentPage: number, pageSize: number) => {
  return request<any>({
    url: `/getUserRelations/${currentPage}/${pageSize}`,
    method: "get",
    params: { userId, type },
  });
};

/**
 * 得到关注用户的所有动态
 * @param currentPage 当前页
 * @param pageSize 分页数
 * @returns 
 */
export const getFollowTrend = (currentPage: number, pageSize: number) => {
  return request<any>({
    url: `/getFollowTrend/${currentPage}/${pageSize}`, // mock接口
    method: "get",
  });
};

/**
 * 关注用户
 * @param followerId 关注用户id
 * @returns 
 */
export const followById = (followerId: string) => {
  return request<any>({
    url: "/followById", // mock接口
    method: "get",
    params: {
      followerId,
    },
  });
};

/**
 * 当前用户是否关注
 * @param followerId 关注的用户id
 * @returns 
 */
export const isFollow = (followerId: string) => {
  return request<any>({
    url: "/isFollow", // mock接口
    method: "get",
    params: {
      followerId,
    },
  });
};

/**
 * 得到当前用户的最新关注信息
 * @param currentPage 当前页
 * @param pageSize 分页数
 * @returns FollowerVo
 */
export const getNoticeFollower = (currentPage: number, pageSize: number) => {
  return request<any>({
    url: `/getNoticeFollower/${currentPage}/${pageSize}`, // mock接口
    method: "get",
  });
};
