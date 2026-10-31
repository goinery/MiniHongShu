import type { UploadUserFile } from "element-plus";

export const MAX_NOTE_IMAGES = 9;

export function validateLocalImage(file: File, maxSizeMb = 5): void {
  if (!/\.(jpe?g|png|webp|gif|bmp)$/i.test(file.name) || (file.type && !file.type.startsWith("image/"))) {
    throw new Error("请选择 JPG / PNG / WEBP / GIF / BMP 图片");
  }
  if (!file.size || file.size > maxSizeMb * 1024 * 1024) {
    throw new Error(`请选择不超过 ${maxSizeMb}MB 的非空图片`);
  }
}

export function normalizeImageUrl(value: string): string {
  let url: URL;
  try {
    url = new URL(value.trim());
  } catch {
    throw new Error("请输入完整的 HTTP 或 HTTPS 图片链接");
  }
  if (!["http:", "https:"].includes(url.protocol) || url.username || url.password) {
    throw new Error("请输入不含账号密码的 HTTP 或 HTTPS 图片链接");
  }
  // 与笔记接口接受的外链范围一致，避免提交后被忽略。
  if (url.protocol === "http:" && /^(localhost|127\.0\.0\.1)/i.test(url.hostname)) {
    throw new Error("请使用可公开访问的图片链接");
  }
  return url.href;
}

export function readImageSize(src: string): Promise<{ width: number; height: number }> {
  return new Promise((resolve, reject) => {
    const image = new Image();
    const finish = () => {
      clearTimeout(timer);
      image.onload = null;
      image.onerror = null;
    };
    const timer = window.setTimeout(() => {
      finish();
      image.src = "";
      reject(new Error("图片加载超时，请检查链接后重试"));
    }, 15000);
    image.onload = () => {
      finish();
      resolve({ width: image.naturalWidth, height: image.naturalHeight });
    };
    image.onerror = () => {
      finish();
      reject(new Error("图片无法加载，请检查链接是否可直接访问"));
    };
    image.src = src;
  });
}

// 先将本地文件转换为存储地址，再按原顺序提交所有图片，避免混合添加时封面变化。
// 每张上传成功后保留地址；后续上传或发布失败时，重试不重复上传已成功的文件。
export async function resolveNoteImages(
  files: UploadUserFile[],
  upload: (file: File) => Promise<string>,
): Promise<string[]> {
  if (!files.length || files.length > MAX_NOTE_IMAGES) throw new Error("请选择 1～9 张图片");
  const urls: string[] = [];
  for (const file of files) {
    if (file.raw) {
      const url = await upload(file.raw);
      if (!url) throw new Error("图片上传失败，请重试");
      if (file.url?.startsWith("blob:")) URL.revokeObjectURL(file.url);
      file.url = url;
      file.raw = undefined;
      file.status = "success";
    }
    if (!file.url) throw new Error("图片读取失败，请重新添加");
    urls.push(file.url);
  }
  return urls;
}
