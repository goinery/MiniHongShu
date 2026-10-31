import { defineConfig, loadEnv } from 'vite'
import path from 'path'
import createVitePlugins from './vite/plugins'

// https://vitejs.dev/config/
export default defineConfig(({ mode, command }) => {
  const env = loadEnv(mode, process.cwd())
  const { VITE_APP_ENV } = env
  const rootBrand = loadEnv(mode, path.resolve(__dirname, '../..'), 'APP_BRAND_NAME')
  const brandName = (rootBrand.APP_BRAND_NAME || '').trim() || '测试'
  const title = `${brandName}管理端`
  const escapeHtml = value => value.replace(/[&<>"']/g, character => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[character])
  return {
    // 与用户前台共用默认头像，两端均提供 /default-avatar.png。
    publicDir: path.resolve(__dirname, '../hongshu-web/src/assets/image'),
    // 部署生产环境和开发环境下的URL。
    // 默认情况下，vite 会假设你的应用是被部署在一个域名的根路径上
    // 例如 https://www.ruoyi.vip/。如果应用被部署在一个子路径上，你就需要用这个选项指定这个子路径。例如，如果你的应用被部署在 https://www.ruoyi.vip/admin/，则设置 baseUrl 为 /admin/。
    base: VITE_APP_ENV === 'production' ? '/' : '/',
    define: {
      'import.meta.env.VITE_APP_BRAND_NAME': JSON.stringify(brandName),
      'import.meta.env.VITE_APP_TITLE': JSON.stringify(title)
    },
    plugins: [
      ...createVitePlugins(env, command === 'build'),
      {
        name: 'app-branding',
        transformIndexHtml: {
          order: 'pre',
          handler: html => html.replace(/%VITE_APP_(BRAND_NAME|TITLE)%/g, (_, key) => escapeHtml(key === 'TITLE' ? title : brandName))
        }
      }
    ],
    resolve: {
      // https://cn.vitejs.dev/config/#resolve-alias
      alias: {
        // 设置路径
        '~': path.resolve(__dirname, './'),
        // 设置别名
        '@': path.resolve(__dirname, './src')
      },
      // https://cn.vitejs.dev/config/#resolve-extensions
      extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue']
    },
    // vite 相关配置
    server: {
      port: 80,
      host: true,
      open: true,
      proxy: {
        // https://cn.vitejs.dev/config/#server-proxy
        '/dev-api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
          rewrite: (p) => p.replace(/^\/dev-api/, '')
        },
        '/profile': {
          target: 'http://localhost:8080',
          changeOrigin: true
        }
      }
    },
    //fix:error:stdin>:7356:1: warning: "@charset" must be the first rule in the file
    css: {
      postcss: {
        plugins: [
          {
            postcssPlugin: 'internal:charset-removal',
            AtRule: {
              charset: (atRule) => {
                if (atRule.name === 'charset') {
                  atRule.remove();
                }
              }
            }
          }
        ]
      }
    }
  }
})
