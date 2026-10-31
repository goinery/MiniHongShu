<template>
  <el-popover :visible="visible" placement="bottom-end" :width="360" :persistent="false" popper-class="profile-more-popover">
    <template #reference>
      <span ref="trigger" class="more-info-trigger" @mouseenter="open" @mouseleave="scheduleClose">
        <el-button
          class="more-info-button"
          :class="{ 'is-pinned': pinned }"
          plain
          round
          :aria-expanded="visible"
          :aria-controls="panelId"
          aria-haspopup="dialog"
          @click="togglePin"
        >更多信息</el-button>
      </span>
    </template>
    <section
      :id="panelId"
      ref="panel"
      class="more-info-panel"
      role="dialog"
      aria-label="更多信息，仅自己可见"
      @mouseenter="open"
      @mouseleave="scheduleClose"
      @focusin="open"
      @focusout="scheduleClose"
    >
      <div class="more-info-heading">
        <span>更多信息 <small>仅自己可见</small></span>
      </div>
      <dl class="more-info-fields">
        <div v-for="field in fields" :key="field.label">
          <dt>{{ field.label }}</dt>
          <dd>{{ field.value }}</dd>
        </div>
      </dl>
    </section>
  </el-popover>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";

const props = defineProps<{ userInfo: any }>();
const trigger = ref<HTMLElement>();
const panel = ref<HTMLElement>();
const visible = ref(false);
const pinned = ref(false);
const panelId = computed(() => `profile-more-info-${props.userInfo.id || 'self'}`);
let closeTimer: ReturnType<typeof setTimeout> | undefined;

const formatDate = (value?: string, withTime = false) => {
  if (!value) return "暂无记录";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const options: Intl.DateTimeFormatOptions = { year: "numeric", month: "2-digit", day: "2-digit", timeZone: "Asia/Shanghai" };
  if (withTime) Object.assign(options, { hour: "2-digit", minute: "2-digit", hour12: false });
  return new Intl.DateTimeFormat("zh-CN", options).format(date);
};
const genderLabels: Record<string, string> = { "0": "男", "1": "女", "2": "保密" };
const fields = computed(() => [
  { label: "性别", value: genderLabels[String(props.userInfo.gender)] || "未填写" },
  { label: "生日", value: props.userInfo.birthday || "未填写" },
  { label: "IP属地", value: props.userInfo.address || "未知" },
  { label: "手机号", value: props.userInfo.phone || "未填写" },
  { label: "邮箱", value: props.userInfo.email || "未填写" },
  { label: "注册时间", value: formatDate(props.userInfo.createTime) },
  { label: "账号状态", value: String(props.userInfo.status) === "1" ? "已停用" : "正常" },
  { label: "登录 IP", value: props.userInfo.loginIp || "暂无记录" },
  { label: "最近登录", value: formatDate(props.userInfo.loginDate, true) },
]);

const clearCloseTimer = () => {
  if (closeTimer !== undefined) clearTimeout(closeTimer);
  closeTimer = undefined;
};
const open = () => {
  clearCloseTimer();
  visible.value = true;
};
const close = () => {
  clearCloseTimer();
  pinned.value = false;
  visible.value = false;
};
const scheduleClose = () => {
  clearCloseTimer();
  if (pinned.value) return;
  // 留出跨过按钮与浮层间隙的时间，允许在浮层内查看和复制资料。
  closeTimer = setTimeout(() => {
    closeTimer = undefined;
    if (!pinned.value && !panel.value?.contains(document.activeElement)) visible.value = false;
  }, 180);
};
const togglePin = () => {
  if (pinned.value) close();
  else {
    pinned.value = true;
    open();
  }
};
const onOutsidePointer = (event: PointerEvent) => {
  const target = event.target;
  if (target instanceof Node && !trigger.value?.contains(target) && !panel.value?.contains(target)) close();
};
const onEscape = (event: KeyboardEvent) => {
  if (event.key !== "Escape" || !visible.value) return;
  if (panel.value?.contains(document.activeElement)) trigger.value?.querySelector("button")?.focus();
  close();
};
watch(() => props.userInfo.id, close);
onMounted(() => {
  document.addEventListener("pointerdown", onOutsidePointer, true);
  document.addEventListener("keydown", onEscape);
});
onBeforeUnmount(() => {
  clearCloseTimer();
  document.removeEventListener("pointerdown", onOutsidePointer, true);
  document.removeEventListener("keydown", onEscape);
});
</script>

<style lang="less" scoped>
.more-info-trigger { display: inline-flex; }
.more-info-button {
  height: 32px;
  padding: 0 16px;
  border-radius: 999px;
  border-color: var(--el-border-color);
  background: transparent;
  color: var(--el-text-color-secondary);
  &:hover, &.is-pinned { color: var(--el-color-primary); border-color: var(--el-color-primary); background: var(--el-color-primary-light-9); }
}
.more-info-heading {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
  small { margin-left: 8px; color: var(--el-text-color-secondary); font-size: 12px; font-weight: 400; }
}
.more-info-fields {
  display: grid;
  gap: 12px;
  max-height: min(440px, 60vh);
  overflow-y: auto;
  margin: 16px 0 0;
  font-size: 13px;
  line-height: 20px;
  > div { display: flex; gap: 16px; }
  dt { flex: 0 0 64px; color: var(--el-text-color-secondary); }
  dd { min-width: 0; margin: 0; color: var(--el-text-color-primary); overflow-wrap: anywhere; }
}
</style>

<style>
.profile-more-popover.el-popover.el-popper {
  box-sizing: border-box;
  max-width: calc(100vw - 24px);
  padding: 16px;
  border-radius: 14px;
}
</style>
