<template>
  <el-popover
    ref="popoverRef"
    trigger="click"
    :placement="placement"
    :width="334"
    :show-arrow="false"
    popper-class="theme-color-picker-popover"
    :hide-after="0"
    :popper-style="popoverStyle"
    @before-enter="open"
    @after-enter="focusHexInput"
    @after-leave="afterClose"
  >
    <template #reference>
      <button
        class="theme-color-trigger"
        type="button"
        aria-label="自定义主题色"
        title="自定义主题色"
        :aria-expanded="visible"
        @keydown.esc="onEscape"
      >
        <span :style="{ backgroundColor: color }" />
      </button>
    </template>

    <div class="color-customizer" @keydown.esc.stop="close">
      <div class="color-panel-heading">
        <span class="color-preview" :style="{ backgroundColor: color }" />
        <div class="color-panel-title">
          <strong>自定义主题色</strong>
          <span>调整后即时生效</span>
        </div>
        <button class="color-panel-close" type="button" aria-label="关闭配色面板" @click="close"><Close /></button>
      </div>

      <div class="palette-heading"><span>快捷配色</span><code>{{ color.toUpperCase() }}</code></div>
      <div class="color-presets" role="group" aria-label="快捷配色">
        <button
          v-for="swatch in swatches"
          :key="swatch"
          type="button"
          class="color-preset"
          :class="{ 'is-selected': color === swatch }"
          :style="{ backgroundColor: swatch, color: swatchTextColor(swatch) }"
          :aria-label="`选择主题色 ${swatch.toUpperCase()}`"
          :aria-pressed="color === swatch"
          :title="swatch.toUpperCase()"
          @click="updateColor(swatch)"
        ><Check v-if="color === swatch" /></button>
      </div>

      <el-color-picker-panel
        v-if="visible"
        ref="panelRef"
        class="theme-color-panel"
        :model-value="color"
        color-format="hex"
        :border="false"
        :validate-event="false"
        @update:model-value="updateColor"
      >
        <template #footer>
          <div class="rgb-row" role="group" aria-label="RGB 颜色，通道范围 0 至 255">
            <span class="format-label">RGB</span>
            <label v-for="(channel, index) in ['R', 'G', 'B']" :key="channel" class="rgb-channel">
              <span>{{ channel }}</span>
              <el-input-number
                :model-value="rgb[index]"
                :aria-label="`RGB ${channel}`"
                :min="0"
                :max="255"
                :precision="0"
                :controls="false"
                size="small"
                @update:model-value="updateRgb(index, $event)"
              />
            </label>
          </div>
          <div class="picker-actions">
            <span class="default-color-hint">默认 {{ defaultHex.toUpperCase() }}</span>
            <el-button size="small" :icon="Refresh" :disabled="color === defaultHex" title="恢复默认主题色" @click="reset">重置</el-button>
            <el-button size="small" type="primary" @click="close">完成</el-button>
          </div>
        </template>
      </el-color-picker-panel>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { Check, Close, Refresh } from "@element-plus/icons-vue";

const props = withDefaults(defineProps<{
  modelValue: string;
  defaultColor: string;
  predefine?: string[];
  placement?: "top-start" | "bottom-end";
}>(), { predefine: () => [], placement: "bottom-end" });
const emit = defineEmits<{
  (event: "change", value: string): void;
  (event: "visible-change", value: boolean): void;
}>();

function normalizeHex(value: unknown): string {
  if (typeof value !== "string") return "";
  const hex = value.trim().toLowerCase();
  if (/^#[\da-f]{6}$/.test(hex)) return hex;
  if (/^#[\da-f]{3}$/.test(hex)) return "#" + hex.slice(1).split("").map(channel => channel + channel).join("");
  return "";
}

const defaultHex = computed(() => normalizeHex(props.defaultColor) || "#ff6699");
const color = ref(normalizeHex(props.modelValue) || defaultHex.value);
const visible = ref(false);
const popoverRef = ref<any>();
const panelRef = ref<any>();
const rgb = computed(() => [1, 3, 5].map(start => parseInt(color.value.slice(start, start + 2), 16)));
const swatches = computed(() => [...new Set([
  defaultHex.value,
  ...(props.predefine.length ? props.predefine : ["#2563eb", "#f6821f", "#18794e", "#6e56cf", "#41454b"]),
].map(normalizeHex).filter(Boolean))]);
const popoverStyle = {
  padding: "16px",
  borderRadius: "16px",
  borderColor: "var(--el-border-color-lighter)",
  background: "var(--el-bg-color-overlay)",
  boxShadow: "var(--el-box-shadow)",
  maxWidth: "calc(100vw - 24px)",
};

watch(() => props.modelValue, value => { color.value = normalizeHex(value) || defaultHex.value; });

function updateColor(value: unknown) {
  const normalized = normalizeHex(value);
  if (!normalized) return;
  color.value = normalized;
  if (normalized !== props.modelValue) emit("change", normalized);
}
function updateRgb(index: number, value: number | undefined) {
  if (value === undefined || !Number.isInteger(value) || value < 0 || value > 255) return;
  const channels = [...rgb.value];
  channels[index] = value;
  updateColor("#" + channels.map(channel => channel.toString(16).padStart(2, "0")).join(""));
}
function swatchTextColor(value: string) {
  const channels = [1, 3, 5].map(start => parseInt(value.slice(start, start + 2), 16));
  return channels[0] * 0.299 + channels[1] * 0.587 + channels[2] * 0.114 > 150 ? "#111827" : "#ffffff";
}
function reset() { updateColor(defaultHex.value); }
function open() { visible.value = true; emit("visible-change", true); }
function afterClose() { visible.value = false; emit("visible-change", false); }
function focusHexInput() { panelRef.value?.update?.(); panelRef.value?.inputRef?.focus?.(); }
function close() { popoverRef.value?.hide(); }
function onEscape(event: KeyboardEvent) {
  if (!visible.value) return;
  event.stopPropagation();
  close();
}
defineExpose({ close });
</script>

<style scoped>
.theme-color-trigger {
  display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0;
  width: 28px; height: 28px; padding: 3px; border: 1px solid var(--el-border-color);
  border-radius: 50%; background: transparent; cursor: pointer;
  transition: border-color 160ms, box-shadow 160ms;
}
.theme-color-trigger > span { width: 100%; height: 100%; border-radius: 50%; }
.theme-color-trigger:hover { border-color: var(--el-color-primary); box-shadow: 0 0 0 3px var(--el-color-primary-light-9); }
.theme-color-trigger:focus-visible,
.color-panel-close:focus-visible,
.color-preset:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 3px; }
.color-customizer { color: var(--el-text-color-primary); }
.color-panel-heading { display: flex; align-items: center; gap: 10px; margin-bottom: 18px; }
.color-preview { width: 36px; height: 36px; flex-shrink: 0; border-radius: 50%; box-shadow: inset 0 0 0 1px #0000000a; }
.color-panel-title { display: flex; flex: 1; flex-direction: column; gap: 4px; }
.color-panel-title strong { font-size: 14px; font-weight: 600; }
.color-panel-title > span { font-size: 12px; color: var(--el-text-color-secondary); }
.color-panel-close { display: grid; place-items: center; width: 28px; height: 28px; padding: 6px; border: 0; border-radius: 8px; background: transparent; color: var(--el-text-color-secondary); cursor: pointer; }
.color-panel-close:hover { color: var(--el-text-color-primary); background: var(--el-fill-color-light); }
.color-panel-close svg { width: 16px; height: 16px; }
.palette-heading { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; font-size: 12px; color: var(--el-text-color-secondary); }
.palette-heading code { font-size: 11px; color: var(--el-text-color-regular); }
.color-presets { display: flex; justify-content: space-between; gap: 8px; margin-bottom: 16px; }
.color-preset { display: grid; place-items: center; flex-shrink: 0; width: 30px; height: 30px; padding: 0; border: 1px solid #0000000a; border-radius: 50%; cursor: pointer; transition: transform 160ms, box-shadow 160ms; }
.color-preset:hover { transform: translateY(-2px); }
.color-preset.is-selected { box-shadow: 0 0 0 2px var(--el-bg-color-overlay), 0 0 0 3px var(--el-color-primary); }
.color-preset svg { width: 16px; height: 16px; }
.theme-color-panel { width: 300px; max-width: 100%; padding: 0; border: 0; background: transparent; }
.theme-color-panel :deep(.el-color-svpanel) { border-radius: 10px; }
.theme-color-panel :deep(.el-color-picker-panel__footer) { display: grid; grid-template-columns: 32px minmax(0, 1fr); align-items: center; gap: 10px 8px; margin-top: 14px; padding-top: 14px; border-top: 1px solid var(--el-border-color-lighter); text-align: left; }
.theme-color-panel :deep(.el-color-picker-panel__footer)::before { content: 'HEX'; font-size: 11px; font-weight: 600; color: var(--el-text-color-secondary); }
.theme-color-panel :deep(.el-color-picker-panel__footer > .el-input) { width: 100%; }
.theme-color-panel :deep(.el-input__wrapper) { border-radius: 7px; }
.rgb-row { grid-column: 1 / -1; display: grid; grid-template-columns: 32px repeat(3, minmax(0, 1fr)); align-items: center; gap: 8px; }
.format-label { font-size: 11px; font-weight: 600; color: var(--el-text-color-secondary); }
.rgb-channel { display: flex; align-items: center; gap: 5px; min-width: 0; font-size: 11px; color: var(--el-text-color-secondary); }
.rgb-channel :deep(.el-input-number) { width: 100%; min-width: 0; }
.rgb-channel :deep(.el-input__wrapper) { padding: 0 5px; }
.picker-actions { grid-column: 1 / -1; display: flex; align-items: center; gap: 8px; padding-top: 4px; }
.default-color-hint { flex: 1; font-size: 10px; white-space: nowrap; color: var(--el-text-color-secondary); }
.picker-actions :deep(.el-button) { height: 30px; margin-left: 0; padding: 0 10px; border-radius: 7px; }
@media (prefers-reduced-motion: reduce) { .theme-color-trigger, .color-preset { transition: none; } }
</style>
