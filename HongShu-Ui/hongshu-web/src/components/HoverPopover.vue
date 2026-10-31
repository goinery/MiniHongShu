<template>
  <span class="hover-popover">
    <el-popover
      :visible="visible"
      :width="width"
      :append-to="appendTo"
      :persistent="false"
      placement="bottom-start"
      popper-class="publish-popover"
      :show-arrow="false"
      :offset="8"
    >
      <template #reference>
        <button
          ref="trigger"
          type="button"
          class="popover-trigger"
          :class="{ 'is-open': visible, 'is-pinned': pinned }"
          :disabled="disabled"
          :aria-expanded="visible"
          :aria-controls="panelId"
          aria-haspopup="dialog"
          @pointerenter="onPointerEnter"
          @pointerleave="onPointerLeave"
          @click="togglePin"
          @keydown.down.prevent="openPinned(true)"
        >
          <span class="trigger-label"><slot name="trigger">{{ title }}</slot></span>
          <el-icon class="trigger-chevron"><ArrowDown /></el-icon>
        </button>
      </template>
      <section
        :id="panelId"
        ref="panel"
        class="popover-panel"
        role="dialog"
        :aria-label="title"
        @pointerenter="onPointerEnter"
        @pointerleave="onPointerLeave"
        @pointerdown="onPanelInteraction"
        @focusin="onPanelInteraction"
        @focusout="scheduleClose"
      >
        <div class="popover-heading">
          <span>{{ title }}</span>
          <button type="button" class="popover-close round-close" :aria-label="`关闭${title}`" @click="close">
            <el-icon><Close /></el-icon>
          </button>
        </div>
        <slot />
      </section>
    </el-popover>
  </span>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { ArrowDown, Close } from "@element-plus/icons-vue";

const props = withDefaults(defineProps<{ id: string; title: string; width?: number; disabled?: boolean; pinOnPanelInteraction?: boolean; appendTo?: string | HTMLElement }>(), {
  width: 340,
  disabled: false,
  pinOnPanelInteraction: true,
});
const panelId = computed(() => `${props.id}-panel`);
const trigger = ref<HTMLButtonElement>();
const panel = ref<HTMLElement>();
const visible = ref(false);
const pinned = ref(false);
let pointerInside = false;
let closeTimer: ReturnType<typeof setTimeout> | undefined;

const cancelClose = () => {
  if (closeTimer !== undefined) clearTimeout(closeTimer);
  closeTimer = undefined;
};
const close = () => {
  cancelClose();
  pinned.value = false;
  visible.value = false;
  pointerInside = false;
};
const open = () => {
  if (props.disabled) return;
  cancelClose();
  visible.value = true;
};
const pin = () => {
  if (props.disabled) return;
  pinned.value = true;
  open();
};
const onPanelInteraction = () => {
  if (props.pinOnPanelInteraction) pin();
};
const openPinned = async (focusInput = false) => {
  pin();
  if (focusInput && !props.disabled) {
    await nextTick();
    panel.value?.querySelector<HTMLElement>('input:not(:disabled), [tabindex="0"], button:not(.popover-close):not(:disabled)')?.focus();
  }
};
const togglePin = () => {
  if (pinned.value) close();
  else pin();
};
const onPointerEnter = (event: PointerEvent) => {
  if (event.pointerType === "mouse" || event.pointerType === "pen") {
    pointerInside = true;
    open();
  }
};
const onPointerLeave = () => {
  pointerInside = false;
  scheduleClose();
};
const scheduleClose = () => {
  cancelClose();
  if (pinned.value || pointerInside) return;
  closeTimer = setTimeout(() => {
    closeTimer = undefined;
    const keepFocus = props.pinOnPanelInteraction && panel.value?.contains(document.activeElement);
    if (!pinned.value && !pointerInside && !keepFocus) visible.value = false;
  }, 180);
};
const onOutsidePointer = (event: PointerEvent) => {
  if (event.target instanceof Node && !trigger.value?.contains(event.target) && !panel.value?.contains(event.target)) close();
};
const onEscape = (event: KeyboardEvent) => {
  if (event.key !== "Escape" || !visible.value) return;
  if (panel.value?.contains(document.activeElement)) trigger.value?.focus();
  close();
};
watch(() => props.disabled, disabled => { if (disabled) close(); });
onMounted(() => {
  document.addEventListener("pointerdown", onOutsidePointer, true);
  document.addEventListener("keydown", onEscape);
});
onBeforeUnmount(() => {
  cancelClose();
  document.removeEventListener("pointerdown", onOutsidePointer, true);
  document.removeEventListener("keydown", onEscape);
});
defineExpose({ close, openPinned });
</script>

<style scoped>
.hover-popover { display: inline-flex; max-width: 100%; vertical-align: middle; }
.popover-trigger {
  display: inline-flex; align-items: center; justify-content: space-between; gap: 12px;
  width: 100%; min-width: 0; min-height: 36px; padding: 0 14px;
  border: 1px solid var(--el-border-color-light); border-radius: 10px;
  background: transparent; color: var(--el-text-color-regular);
  font: inherit; font-size: 13px; text-align: left; cursor: pointer;
  transition: background-color 160ms, border-color 160ms, color 160ms;
}
.popover-trigger:hover, .popover-trigger.is-open {
  border-color: var(--el-color-primary); background: var(--el-color-primary-light-9); color: var(--el-color-primary);
}
.popover-trigger.is-pinned { box-shadow: 0 0 0 2px var(--el-color-primary-light-9); }
.popover-trigger:disabled { cursor: not-allowed; opacity: 0.5; }
.popover-trigger:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 3px; }
.trigger-label { display: inline-flex; align-items: center; gap: 6px; min-width: 0; }
.trigger-chevron { flex-shrink: 0; transition: transform 160ms; }
.is-open .trigger-chevron { transform: rotate(180deg); }
.popover-heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 14px; color: var(--el-text-color-primary); font-size: 14px; font-weight: 600; }
.popover-close { --close-control-size: 24px; flex-shrink: 0; font-size: 12px; }
</style>

<style>
.publish-popover.el-popover.el-popper {
  max-width: calc(100vw - 32px); padding: 16px; border-radius: 16px;
  border-color: var(--el-border-color-lighter); box-shadow: var(--el-box-shadow-light);
}
</style>
