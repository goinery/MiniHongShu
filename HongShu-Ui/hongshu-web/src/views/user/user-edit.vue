<template></template>

<script lang="ts" setup>
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { Camera, Upload, Link, Check, Male, Female, Lock, Plus, Location } from "@element-plus/icons-vue";
import { changePassword, updateUser, uploadAvatarImage } from "@/api/user";
import { useUserStore } from "@/store/userStore";
import { normalizeImageUrl, readImageSize, validateLocalImage } from "@/utils/noteImages";
import CameraAvatar from "@/components/CameraAvatar.vue";

const props = defineProps<{
  modelValue: boolean;
  userInfo: any;
}>();

const emit = defineEmits<{
  (e: "update:modelValue", value: boolean): void;
  (e: "success"): void;
}>();

const userStore = useUserStore();
const saving = ref(false);
const changingPassword = ref(false);
const busy = computed(() => saving.value || changingPassword.value);
const activeTab = ref("profile");
const passwordForm = reactive({ oldPassword: "", newPassword: "", confirmPassword: "" });
const passwordErrors = reactive({ oldPassword: "", newPassword: "", confirmPassword: "" });
const clearPasswords = () => {
  Object.assign(passwordForm, { oldPassword: "", newPassword: "", confirmPassword: "" });
  Object.assign(passwordErrors, { oldPassword: "", newPassword: "", confirmPassword: "" });
};
const disableFutureDate = (date: Date) => date.getTime() > Date.now();
const avatarUrl = ref("");
const avatarError = ref("");
const checkingAvatar = ref(false);
const uploadingAvatar = ref(false);
const cameraVisible = ref(false);
const avatarFileInput = ref<HTMLInputElement>();
let pendingAvatar: File | null = null;
let avatarPreview = "";
let avatarRequest = 0;
const inputValue = ref("");
const tagError = ref("");
const profileFields = ref<HTMLElement>();
const passwordFields = ref<HTMLFormElement>();
const profileErrors = reactive({ username: "", birthday: "", phone: "", email: "" });
type ProfileField = keyof typeof profileErrors;
const profileBaseline = ref("");
const form = reactive({
  avatar: "",
  username: "",
  gender: "2",
  phone: "",
  email: "",
  birthday: "",
  description: "",
  tags: [] as string[],
});

const serializeProfile = () => JSON.stringify({ ...form, username: form.username.trim(), phone: form.phone.trim(), email: form.email.trim(), birthday: form.birthday || "" });
const profileDirty = computed(() => serializeProfile() !== profileBaseline.value || Boolean(avatarUrl.value.trim() && avatarUrl.value.trim() !== form.avatar));
const avatarChanged = computed(() => form.avatar !== (props.userInfo?.avatar || ""));
const phoneChanged = computed(() => Boolean(form.phone.trim() && form.phone.trim() !== String(props.userInfo?.phone || "").trim()));

const fallbackAvatar = (event: Event) => {
  const image = event.target as HTMLImageElement;
  if (image.getAttribute("src") !== "/default-avatar.png") image.src = "/default-avatar.png";
};

const focusInvalid = async (container: HTMLElement | undefined, id: string) => {
  await nextTick();
  const field = container?.querySelector<HTMLElement>(`#${id}`);
  field?.focus();
  field?.scrollIntoView({ block: "nearest", behavior: "smooth" });
};

const validateProfileField = (field: ProfileField): boolean => {
  let error = "";
  if (field === "username" && (!form.username.trim() || form.username.trim().length > 50)) error = "昵称不能为空，且不能超过50字";
  if (field === "phone" && !/^1\d{10}$/.test(form.phone.trim())) error = "请输入有效的11位手机号";
  if (field === "email" && form.email.trim() && (form.email.trim().length > 100 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim()))) error = "请输入有效的邮箱地址";
  if (field === "birthday" && form.birthday) {
    const date = new Date(`${form.birthday}T00:00:00`);
    const normalized = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
    if (!/^\d{4}-\d{2}-\d{2}$/.test(form.birthday) || Number.isNaN(date.getTime()) || normalized !== form.birthday || disableFutureDate(date)) error = "请选择有效日期，且不能晚于今天";
  }
  profileErrors[field] = error;
  return !error;
};

const validateProfile = async () => {
  const fields: ProfileField[] = ["username", "birthday", "phone", "email"];
  fields.forEach(validateProfileField);
  const invalid = fields.find(field => profileErrors[field]);
  if (!invalid) return true;
  await focusInvalid(profileFields.value, `profile-${invalid}`);
  return false;
};

const validatePasswordConfirmation = () => {
  passwordErrors.confirmPassword = !passwordForm.confirmPassword ? "请再次输入新密码" : passwordForm.newPassword !== passwordForm.confirmPassword ? "两次输入的新密码不一致" : "";
};

const clearPendingAvatar = () => {
  if (avatarPreview) URL.revokeObjectURL(avatarPreview);
  avatarPreview = "";
  pendingAvatar = null;
};

const closeDialog = (open: boolean) => {
  if (busy.value) return;
  if (!open) {
    avatarRequest++;
    clearPendingAvatar();
    clearPasswords();
  }
  emit("update:modelValue", open);
};

const restoreAvatar = () => {
  if (saving.value || uploadingAvatar.value) return;
  avatarRequest++;
  checkingAvatar.value = false;
  clearPendingAvatar();
  form.avatar = props.userInfo?.avatar || "";
  avatarUrl.value = "";
  avatarError.value = "";
};

const resetForm = () => {
  form.avatar = props.userInfo?.avatar || "";
  avatarUrl.value = "";
  form.username = props.userInfo?.username || "";
  const gender = String(props.userInfo?.gender ?? "2");
  form.gender = ["0", "1", "2"].includes(gender) ? gender : "2";
  form.phone = props.userInfo?.phone || "";
  form.email = props.userInfo?.email || "";
  form.birthday = props.userInfo?.birthday || "";
  form.description = props.userInfo?.description || "";
  try {
    const tags = typeof props.userInfo?.tags === "string" ? JSON.parse(props.userInfo.tags) : props.userInfo?.tags;
    form.tags = Array.isArray(tags) ? tags.filter((tag): tag is string => typeof tag === "string") : [];
  } catch {
    form.tags = [];
  }
  inputValue.value = "";
  tagError.value = "";
  Object.assign(profileErrors, { username: "", birthday: "", phone: "", email: "" });
  profileBaseline.value = serializeProfile();
};

watch(
  () => props.modelValue,
  (open) => {
    // 关闭或重新打开时，旧图片请求不能覆盖本次编辑的预览。
    avatarRequest++;
    if (!open) cameraVisible.value = false;
    clearPendingAvatar();
    checkingAvatar.value = false;
    uploadingAvatar.value = false;
    avatarError.value = "";
    clearPasswords();
    if (open) {
      activeTab.value = "profile";
      resetForm();
    }
  },
  { immediate: true }
);

watch(avatarUrl, () => { avatarError.value = ""; });
watch(inputValue, () => { tagError.value = ""; });
watch(activeTab, () => { cameraVisible.value = false; });
onBeforeUnmount(() => { avatarRequest++; clearPendingAvatar(); clearPasswords(); });

const applyAvatarUrl = async (): Promise<boolean> => {
  if (checkingAvatar.value || uploadingAvatar.value || saving.value) return false;
  const request = ++avatarRequest;
  avatarError.value = "";
  try {
    const url = normalizeImageUrl(avatarUrl.value);
    checkingAvatar.value = true;
    await readImageSize(url);
    if (request !== avatarRequest || !props.modelValue) return false;
    clearPendingAvatar();
    form.avatar = url;
    avatarUrl.value = url;
    return true;
  } catch (error) {
    if (request === avatarRequest) avatarError.value = (error as Error).message;
    return false;
  } finally {
    if (request === avatarRequest) checkingAvatar.value = false;
  }
};

const openAvatarUpload = () => {
  if (saving.value || checkingAvatar.value || uploadingAvatar.value) return;
  avatarFileInput.value?.click();
};

const selectAvatarFile = (event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = "";
  if (file) void setAvatarFromFile(file);
};

const setAvatarFromFile = async (file: File) => {
  if (!file || saving.value || !props.modelValue) return;
  const request = ++avatarRequest;
  checkingAvatar.value = true;
  avatarError.value = "";
  let preview = "";
  try {
    validateLocalImage(file);
    preview = URL.createObjectURL(file);
    await readImageSize(preview);
    if (request === avatarRequest && props.modelValue) {
      clearPendingAvatar();
      pendingAvatar = file;
      avatarPreview = preview;
      form.avatar = preview;
      preview = "";
      avatarUrl.value = "";
    }
  } catch (error) {
    if (request === avatarRequest) avatarError.value = (error as Error).message;
  } finally {
    if (preview) URL.revokeObjectURL(preview);
    if (request === avatarRequest) checkingAvatar.value = false;
  }
};

const removeTag = (tag: string) => {
  if (saving.value) return;
  form.tags = form.tags.filter((item) => item !== tag);
  tagError.value = "";
};</script>

<style lang="less" scoped></style>
