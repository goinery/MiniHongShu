import request from "@/utils/request";

// 调用方必须在表单确认后调用；选择图片只创建前端预览。
export async function uploadImage(file) {
  const data = new FormData();
  data.append("file", file);
  const response = await request({
    url: "/common/upload",
    method: "post",
    data,
    timeout: 60000,
    headers: { "Content-Type": "multipart/form-data", repeatSubmit: false },
  });
  if (typeof response.fileName !== "string" || !response.fileName) {
    throw new Error("图片上传失败，请重试");
  }
  return response.fileName;
}
