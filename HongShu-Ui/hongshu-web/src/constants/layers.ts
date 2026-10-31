import { watchEffect } from "vue";
import { useZIndex } from "element-plus";

/**
 * 全局层级的唯一数值来源；每级间隔 10，与组件内部的 0～2 层分开。
 * Element Plus 从 component 开始管理弹层打开顺序，消息始终高于其当前最高层。
 */
export const UI_LAYERS = Object.freeze({
  content: 0,
  sticky: 10,
  navigation: 20,
  popover: 30,
  modal: 40,
  authentication: 50,
  component: 60,
});

const LOCAL_LAYERS = Object.freeze({
  backdrop: 0,
  content: 1,
  control: 2,
});

export function installUiLayers(): void {
  const style = document.documentElement.style;
  for (const [name, value] of Object.entries(UI_LAYERS)) {
    style.setProperty(`--layer-${name}`, String(value));
  }
  for (const [name, value] of Object.entries(LOCAL_LAYERS)) {
    style.setProperty(`--layer-local-${name}`, String(value));
  }
  style.setProperty("--layer-feedback", String(UI_LAYERS.component + 1));
}

/** 在应用根组件调用一次，与 Element Plus 共用动态层级计数器。 */
export function useFeedbackLayer(): void {
  const { currentZIndex } = useZIndex();
  watchEffect(() => {
    document.documentElement.style.setProperty(
      "--layer-feedback",
      String(currentZIndex.value + 1),
    );
  });
}
