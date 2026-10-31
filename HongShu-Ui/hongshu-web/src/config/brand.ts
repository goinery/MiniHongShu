const configuredBrandName = import.meta.env.VITE_APP_BRAND_NAME;

export const brandName =
  typeof configuredBrandName === "string" && configuredBrandName.trim()
    ? configuredBrandName.trim()
    : "测试";

export const communityName = `${brandName}社区`;
