import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";
import { resolve } from "path";

export default defineConfig(({ mode }) => {
  const rootBrand = loadEnv(mode, resolve(__dirname, "../.."), "APP_BRAND_NAME");
  const brandName = (rootBrand.APP_BRAND_NAME || "").trim() || "测试";
  const htmlEntities: Record<string, string> = { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" };
  const escapeHtml = (value: string) => value.replace(/[&<>"']/g, character => htmlEntities[character]);

  return {
    // 默认头像以固定地址发布，避免将构建哈希写入用户资料。
    publicDir: resolve(__dirname, "src/assets/image"),
    define: {
      "import.meta.env.VITE_APP_BRAND_NAME": JSON.stringify(brandName),
      "import.meta.env.VITE_APP_TITLE": JSON.stringify(brandName),
    },
    plugins: [
      vue(),
      {
        name: "app-branding",
        transformIndexHtml: {
          order: "pre",
          handler: (html: string) => html.replace(/%VITE_APP_(BRAND_NAME|TITLE)%/g, () => escapeHtml(brandName)),
        },
      },
    ],
    resolve: {
      alias: { "@": resolve(__dirname, "./src") },
      extensions: [".mjs", ".js", ".ts", ".jsx", ".tsx", ".json", ".vue"],
    },
    server: {
      port: 80,
      host: true,
      open: true,
      proxy: {
        "/dev-api": {
          target: "http://localhost:8080",
          changeOrigin: true,
          ws: true,
          rewrite: (path: string) => path.replace(/^\/dev-api/, ""),
        },
        "/profile": { target: "http://localhost:8080", changeOrigin: true },
      },
    },
  };
});
