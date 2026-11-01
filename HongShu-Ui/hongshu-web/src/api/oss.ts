import { createApi } from "@/utils/request";

const request = createApi("/web/oss");

export const uploadNoteImage = async (file: File): Promise<string> => {
  const data = new FormData();
  data.append("uploadFiles", file);
  const result = await request<any>({ url: "/saveBatch", method: "post", data });
  return result.data?.[0];
};

export const uploadAvatarImage = async (file: File): Promise<string> => {
  const data = new FormData();
  data.append("file", file);
  const response = await request<any>({ url: "/save", method: "post", data });
  if (typeof response.data !== "string" || !response.data) {
    throw new Error("头像上传失败，请重试");
  }
  return response.data;
};
