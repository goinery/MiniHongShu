import { createApp } from "vue";
import "./style.css";
import App from "./App.vue";

const app = createApp(App);

import router from "./router/index";
import { setupStore } from "@/store";
import { useThemeStore } from "@/store/themeStore";
import { installUiLayers, UI_LAYERS } from "@/constants/layers";
import { installBrandFavicon } from "@/utils/brandFavicon";

import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import "element-plus/theme-chalk/dark/css-vars.css";
import "./styles/dark-overrides.css";
import "./styles/community-dialog.css";
import "./styles/close-controls.css";
import "./styles/layers.css";
import "./styles/interactions.css";

// 引入动画
import 'animate.css';
import "@/assets/iconfont/iconfont.css";

setupStore(app);
installUiLayers();
useThemeStore().initTheme();
installBrandFavicon();
app.use(router);
app.use(ElementPlus, { zIndex: UI_LAYERS.component });
app.mount("#app");

