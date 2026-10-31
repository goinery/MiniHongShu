<template>
  <el-dialog
    :model-value="modelValue"
    title="编辑资料"
    class="community-dialog profile-edit-dialog"
    align-center append-to-body destroy-on-close
    :close-on-click-modal="!busy && !cameraVisible"
    :close-on-press-escape="!busy && !cameraVisible"
    :show-close="!busy"
    @update:model-value="closeDialog"
  >
    <el-tabs v-model="activeTab" class="edit-tabs" :before-leave="() => !busy && !checkingAvatar && !uploadingAvatar">
      <el-tab-pane label="个人资料" name="profile">
        <div ref="profileFields" class="profile-layout">
          <aside class="avatar-settings" aria-label="头像设置">
            <div class="avatar-upload-content">
              <button type="button" class="avatar-uploader" :disabled="saving || checkingAvatar || uploadingAvatar" aria-label="选择本地图片上传头像" @click="openAvatarUpload">
                <div class="avatar-wrap" :class="{ 'is-uploading': checkingAvatar || uploadingAvatar }">
                  <img :src="form.avatar || '/default-avatar.png'" class="avatar" alt="头像预览" @error="fallbackAvatar" />
                  <div class="avatar-mask"><el-icon><Upload /></el-icon></div>
                </div>
              </button>
              <input ref="avatarFileInput" type="file" accept="image/jpeg,image/png,image/webp,image/gif,image/bmp,.jpg,.jpeg,.png,.webp,.gif,.bmp" hidden @change="selectAvatarFile" />
              <button type="button" class="avatar-action" :disabled="saving || checkingAvatar || uploadingAvatar" @click="cameraVisible = true"><el-icon><Camera /></el-icon>{{ checkingAvatar ? '校验中…' : uploadingAvatar ? '上传中…' : '拍照更换头像' }}</button>
            </div>
            <div class="avatar-identity"><strong>{{ form.username || '你的昵称' }}</strong><span>会员号：{{ userInfo?.hsId ?? '—' }}</span></div>
            <div class="ip-location"><el-icon><Location /></el-icon><span>IP属地</span><strong>{{ userInfo?.address || '待识别' }}</strong></div>
            <div class="avatar-url-field">
              <label for="profile-avatar-url" class="avatar-url-label">头像链接</label>
              <div class="avatar-url-controls">
                <el-input id="profile-avatar-url" v-model="avatarUrl" :prefix-icon="Link" placeholder="粘贴图片链接" clearable :disabled="saving || checkingAvatar || uploadingAvatar" @keyup.enter.prevent="applyAvatarUrl" />
                <el-button :icon="Check" aria-label="使用头像链接" :loading="checkingAvatar" :disabled="saving || uploadingAvatar || !avatarUrl.trim()" @click="applyAvatarUrl" />
              </div>
              <p v-if="avatarError" class="field-error" role="alert">{{ avatarError }}</p>
            </div>
            <el-button v-if="avatarChanged" type="primary" link :disabled="saving || uploadingAvatar" @click="restoreAvatar">恢复原头像</el-button>
          </aside>

          <div class="profile-fields">
            <section class="profile-section" aria-labelledby="profile-basic-heading">
              <h3 id="profile-basic-heading" class="section-heading">基本信息</h3>
              <div class="field-grid">
                <div class="field full-width" :class="{ 'is-invalid': profileErrors.username }">
                  <div class="field-head"><label for="profile-username"><span class="required">*</span>昵称</label><span>{{ form.username.length }}/50</span></div>
                  <el-input id="profile-username" v-model="form.username" maxlength="50" placeholder="请输入昵称" :disabled="saving" :aria-invalid="Boolean(profileErrors.username)" @input="profileErrors.username = ''" @blur="validateProfileField('username')" />
                  <p v-if="profileErrors.username" class="field-error" role="alert">{{ profileErrors.username }}</p>
                </div>
                <div class="field">
                  <div class="field-head"><span id="profile-gender-label">性别</span></div>
                  <div class="gender-options" role="group" aria-labelledby="profile-gender-label">
                    <button type="button" class="gender-choice is-male" :class="{ active: form.gender === '0' }" :aria-pressed="form.gender === '0'" :disabled="saving" @click="form.gender = '0'"><el-icon><Male /></el-icon>男</button>
                    <button type="button" class="gender-choice is-female" :class="{ active: form.gender === '1' }" :aria-pressed="form.gender === '1'" :disabled="saving" @click="form.gender = '1'"><el-icon><Female /></el-icon>女</button>
                    <button type="button" class="gender-choice" :class="{ active: form.gender === '2' }" :aria-pressed="form.gender === '2'" :disabled="saving" @click="form.gender = '2'">保密</button>
                  </div>
                </div>
                <div class="field" :class="{ 'is-invalid': profileErrors.birthday }">
                  <div class="field-head"><label for="profile-birthday">生日</label></div>
                  <el-date-picker id="profile-birthday" v-model="form.birthday" type="date" value-format="YYYY-MM-DD" format="YYYY-MM-DD" placeholder="选择生日" :disabled="saving" :disabled-date="disableFutureDate" clearable @change="validateProfileField('birthday')" />
                  <p v-if="profileErrors.birthday" class="field-error" role="alert">{{ profileErrors.birthday }}</p>
                </div>
              </div>
            </section>

            <section class="profile-section" aria-labelledby="profile-intro-heading">
              <h3 id="profile-intro-heading" class="section-heading">个人介绍</h3>
              <div class="field">
                <div class="field-head"><label for="profile-description">简介</label><span>{{ form.description.length }}/250</span></div>
                <el-input id="profile-description" v-model="form.description" class="description-input" type="textarea" :rows="3" resize="vertical" :disabled="saving" maxlength="250" placeholder="介绍一下自己吧" />
              </div>
              <div class="field tags-field">
                <div class="field-head"><span>标签</span><span>{{ form.tags.length }}/4</span></div>
                <div class="tag-input-row">
                  <el-input v-model="inputValue" aria-label="标签名称" maxlength="8" placeholder="输入标签，最多8个字" clearable :disabled="saving || form.tags.length >= 4" @keyup.enter.prevent="confirmTag" />
                  <el-button type="primary" round :icon="Plus" :disabled="saving || form.tags.length >= 4 || !inputValue.trim()" @click="confirmTag">添加</el-button>
                </div>
                <p v-if="tagError" class="field-error" role="alert">{{ tagError }}</p>
                <div v-if="form.tags.length" class="tags"><el-tag v-for="tag in form.tags" :key="tag" :closable="!saving" disable-transitions round effect="plain" type="primary" @close="removeTag(tag)">{{ tag }}</el-tag></div>
              </div>
            </section>

            <section class="profile-section contact-section" aria-labelledby="profile-contact-heading">
              <div class="section-heading-row"><h3 id="profile-contact-heading" class="section-heading">联系方式</h3><span class="private-label"><el-icon><Lock /></el-icon>仅自己可见</span></div>
              <div class="field-grid">
                <div class="field" :class="{ 'is-invalid': profileErrors.phone }">
                  <div class="field-head"><label for="profile-phone"><span class="required">*</span>手机号</label></div>
                  <el-input id="profile-phone" v-model="form.phone" type="tel" inputmode="numeric" autocomplete="tel" maxlength="11" placeholder="请输入11位手机号" :disabled="saving" :aria-invalid="Boolean(profileErrors.phone)" @input="profileErrors.phone = ''" @blur="validateProfileField('phone')" />
                  <p v-if="profileErrors.phone" class="field-error" role="alert">{{ profileErrors.phone }}</p>
                  <p v-else-if="phoneChanged" class="field-hint">保存后请使用新手机号登录</p>
                </div>
                <div class="field" :class="{ 'is-invalid': profileErrors.email }">
                  <div class="field-head"><label for="profile-email">邮箱</label><span>选填</span></div>
                  <el-input id="profile-email" v-model="form.email" type="email" autocomplete="email" maxlength="100" placeholder="请输入邮箱地址" :disabled="saving" :aria-invalid="Boolean(profileErrors.email)" clearable @input="profileErrors.email = ''" @blur="validateProfileField('email')" />
                  <p v-if="profileErrors.email" class="field-error" role="alert">{{ profileErrors.email }}</p>
                </div>
              </div>
            </section>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="修改密码" name="password">
        <form id="profile-password-form" ref="passwordFields" class="password-form" @submit.prevent="savePassword">
          <div class="password-intro"><span class="password-icon"><el-icon><Lock /></el-icon></span><div><h3>设置新密码</h3><p>修改成功后，请使用新密码重新登录。</p></div></div>
          <div class="field" :class="{ 'is-invalid': passwordErrors.oldPassword }">
            <div class="field-head"><label for="profile-old-password">原密码</label></div>
            <el-input id="profile-old-password" v-model="passwordForm.oldPassword" type="password" show-password autocomplete="current-password" maxlength="72" placeholder="请输入原密码" :disabled="changingPassword" @input="passwordErrors.oldPassword = ''" />
            <p v-if="passwordErrors.oldPassword" class="field-error" role="alert">{{ passwordErrors.oldPassword }}</p>
          </div>
          <div class="field" :class="{ 'is-invalid': passwordErrors.newPassword }">
            <div class="field-head"><label for="profile-new-password">新密码</label></div>
            <el-input id="profile-new-password" v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" maxlength="72" placeholder="至少6位的新密码" :disabled="changingPassword" @input="passwordErrors.newPassword = ''; passwordErrors.confirmPassword = ''" />
            <p v-if="passwordErrors.newPassword" class="field-error" role="alert">{{ passwordErrors.newPassword }}</p>
            <p v-else class="field-hint">至少6位，不超过72字节；中文等字符会占用多个字节。</p>
          </div>
          <div class="field" :class="{ 'is-invalid': passwordErrors.confirmPassword }">
            <div class="field-head"><label for="profile-confirm-password">确认新密码</label></div>
            <el-input id="profile-confirm-password" v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" maxlength="72" placeholder="请再次输入新密码" :disabled="changingPassword" @input="passwordErrors.confirmPassword = ''" @blur="validatePasswordConfirmation" />
            <p v-if="passwordErrors.confirmPassword" class="field-error" role="alert">{{ passwordErrors.confirmPassword }}</p>
            <p v-else-if="passwordForm.confirmPassword && passwordForm.confirmPassword === passwordForm.newPassword" class="field-hint is-valid"><el-icon><Check /></el-icon>两次输入一致</p>
          </div>
        </form>
      </el-tab-pane>
    </el-tabs>
    <CameraAvatar v-model="cameraVisible" @captured="setAvatarFromFile" />
    <template #footer>
      <div class="profile-footer">
        <span v-if="activeTab === 'profile' && profileDirty" class="unsaved-label">有未保存的修改</span>
        <div class="footer-actions">
          <el-button round :disabled="busy" @click="closeDialog(false)">取消</el-button>
          <el-button v-if="activeTab === 'profile'" type="primary" round :loading="saving" :disabled="!profileDirty || checkingAvatar || uploadingAvatar" @click="save">保存资料</el-button>
          <el-button v-else type="primary" round native-type="submit" form="profile-password-form" :loading="changingPassword">确认修改密码</el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

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
};

const confirmTag = () => {
  if (saving.value) return;
  const tag = inputValue.value.trim();
  if (!tag) return;
  if (form.tags.includes(tag)) { tagError.value = "标签已添加"; return; }
  if (form.tags.length >= 4) { tagError.value = "最多添加4个标签"; return; }
  if (tag.length > 8) { tagError.value = "每个标签最多8个字"; return; }
  form.tags.push(tag);
  inputValue.value = "";
  tagError.value = "";
};

const save = async () => {
  if (busy.value || checkingAvatar.value || uploadingAvatar.value) return;
  if (!(await validateProfile())) return;
  // 粘贴链接后直接保存，也先加载并应用头像，避免保存旧预览。
  if (avatarUrl.value.trim() && avatarUrl.value.trim() !== form.avatar) {
    if (!(await applyAvatarUrl())) return;
  }
  if (!props.modelValue) return;
  if (!(await validateProfile())) return;
  const request = avatarRequest;
  const sessionVersion = userStore.getSessionVersion();
  saving.value = true;
  const payload = {
    id: props.userInfo.id,
    avatar: form.avatar,
    username: form.username.trim(),
    gender: form.gender,
    phone: form.phone.trim(),
    email: form.email.trim(),
    birthday: form.birthday || "",
    description: form.description,
    tags: JSON.stringify(form.tags),
  };
  try {
    if (pendingAvatar) {
      uploadingAvatar.value = true;
      const url = await uploadAvatarImage(pendingAvatar);
      if (request !== avatarRequest || !props.modelValue) return;
      form.avatar = url;
      payload.avatar = url;
      clearPendingAvatar();
      uploadingAvatar.value = false;
    }
    const response = await updateUser(payload);
    if (request !== avatarRequest || sessionVersion !== userStore.getSessionVersion() || !props.modelValue) return;
    const user = userStore.getUserInfo();
    if (user && String(user.id) === String(payload.id)) userStore.setUserInfo({ ...user, ...response.data });
    ElMessage.success("资料已更新");
    if (request === avatarRequest) emit("update:modelValue", false);
    emit("success");
  } catch {
    // 保存失败由统一请求拦截器提示，保留表单供重试。
  } finally {
    uploadingAvatar.value = false;
    saving.value = false;
  }
};

const savePassword = async () => {
  if (busy.value || !props.modelValue) return;
  Object.assign(passwordErrors, { oldPassword: "", newPassword: "", confirmPassword: "" });
  if (!passwordForm.oldPassword.trim()) passwordErrors.oldPassword = "请输入原密码";
  if (!passwordForm.newPassword.trim() || passwordForm.newPassword.length < 6 || new TextEncoder().encode(passwordForm.newPassword).length > 72) {
    passwordErrors.newPassword = "新密码至少6位，且不能超过72字节";
  } else if (passwordForm.oldPassword === passwordForm.newPassword) {
    passwordErrors.newPassword = "新密码不能与原密码相同";
  }
  validatePasswordConfirmation();
  const invalid = (["oldPassword", "newPassword", "confirmPassword"] as const).find(field => passwordErrors[field]);
  if (invalid) {
    const ids = { oldPassword: "profile-old-password", newPassword: "profile-new-password", confirmPassword: "profile-confirm-password" };
    await focusInvalid(passwordFields.value, ids[invalid]);
    return;
  }
  const sessionVersion = userStore.getSessionVersion();
  changingPassword.value = true;
  try {
    await changePassword({ ...passwordForm });
    if (sessionVersion !== userStore.getSessionVersion()) return;
    clearPasswords();
    emit("update:modelValue", false);
    ElMessage.success("密码已修改，请使用新密码重新登录");
    userStore.loginOut();
  } catch {
    // 保留输入，错误由统一请求拦截器展示。
  } finally {
    changingPassword.value = false;
  }
};
</script>

<style lang="less" scoped>
.edit-tabs { display: flex; flex-direction: column; flex: 1; min-height: 0; }
.edit-tabs :deep(.el-tabs__header) { flex-shrink: 0; margin: 0 0 22px; padding-bottom: 12px; background: var(--el-bg-color-overlay); }
.edit-tabs :deep(.el-tabs__content) { flex: 1; min-height: 0; overflow: auto; }
.edit-tabs :deep(.el-tabs__nav-wrap::after), .edit-tabs :deep(.el-tabs__active-bar) { display: none; }
.edit-tabs :deep(.el-tabs__item) { height: 38px; margin-right: 6px; padding: 0 16px !important; border-radius: 10px; color: var(--el-text-color-secondary); font-size: 14px; transition: color 160ms, background-color 160ms; }
.edit-tabs :deep(.el-tabs__item:hover), .edit-tabs :deep(.el-tabs__item:focus-visible) { color: var(--el-color-primary); background: var(--el-color-primary-light-9); }
.edit-tabs :deep(.el-tabs__item.is-active) { color: var(--el-color-primary); background: var(--el-color-primary-light-9); font-weight: 600; }
.profile-layout { display: grid; grid-template-columns: 200px minmax(0, 1fr); align-items: start; gap: 28px; padding: 2px 2px 6px; }
.avatar-settings { display: flex; flex-direction: column; align-items: center; gap: 18px; padding: 22px 16px 18px; border: 1px solid var(--el-border-color-extra-light); border-radius: 16px; background: var(--el-fill-color-extra-light); }
.avatar-uploader { padding: 0; border: 0; border-radius: 12px; background: transparent; cursor: pointer; }
.avatar-uploader:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 4px; }
.avatar-uploader:disabled { cursor: not-allowed; opacity: 0.6; }
.avatar-upload-content { display: flex; flex-direction: column; align-items: center; gap: 12px; }
.avatar-wrap { position: relative; width: 104px; height: 104px; overflow: hidden; border: 4px solid var(--el-color-primary-light-9); border-radius: 50%; }
.avatar { display: block; width: 100%; height: 100%; object-fit: cover; }
.avatar-mask { position: absolute; inset: 0; display: grid; place-items: center; background: rgba(0, 0, 0, 0.35); color: #fff; font-size: 24px; opacity: 0; transition: opacity 160ms; }
.avatar-uploader:hover .avatar-mask, .avatar-wrap.is-uploading .avatar-mask, .avatar-uploader:focus-within .avatar-mask { opacity: 1; }
.avatar-action { display: inline-flex; align-items: center; gap: 6px; padding: 0; border: 0; background: transparent; color: var(--el-color-primary); font-size: 13px; cursor: pointer; }
.avatar-action:disabled { cursor: not-allowed; opacity: 0.6; }
.avatar-action:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 4px; border-radius: 4px; }
.avatar-identity { display: flex; flex-direction: column; align-items: center; gap: 5px; min-width: 0; max-width: 100%; strong { max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 15px; color: var(--el-text-color-primary); } span { font-size: 11px; color: var(--el-text-color-secondary); } }
.ip-location { display: flex; align-items: center; gap: 5px; max-width: 100%; font-size: 11px; color: var(--el-text-color-secondary); strong { min-width: 0; overflow-wrap: anywhere; color: var(--el-text-color-regular); font-weight: 500; } }
.avatar-url-field { width: 100%; min-width: 0; }
.avatar-url-label { display: block; margin-bottom: 8px; color: var(--el-text-color-regular); font-size: 12px; }
.avatar-url-controls { display: flex; align-items: center; gap: 6px; .el-input { min-width: 0; flex: 1; } .el-button { flex-shrink: 0; width: 32px; min-width: 32px; padding: 0; border-radius: 8px; } }
.profile-fields { display: flex; flex-direction: column; gap: 24px; min-width: 0; }
.profile-section + .profile-section { padding-top: 20px; border-top: 1px solid var(--el-border-color-extra-light); }
.section-heading { margin: 0 0 15px; color: var(--el-text-color-primary); font-size: 14px; line-height: 20px; font-weight: 600; }
.section-heading-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 15px; .section-heading { margin: 0; } }
.private-label { display: inline-flex; align-items: center; gap: 4px; color: var(--el-text-color-secondary); font-size: 11px; }
.field-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
.field { min-width: 0; }
.full-width { grid-column: 1 / -1; }
.field-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 8px; font-size: 12px; line-height: 18px; color: var(--el-text-color-regular); span:not(.required) { color: var(--el-text-color-secondary); } }
.required { margin-right: 4px; color: var(--el-color-primary); }
.field :deep(.el-select), .field :deep(.el-date-editor.el-input) { width: 100%; }
.field :deep(.el-input__wrapper) { min-height: 36px; border-radius: 9px; }
.field :deep(.el-textarea__inner) { padding: 10px 12px; border-radius: 9px; line-height: 1.6; min-height: 90px; }
.field.is-invalid :deep(.el-input__wrapper) { box-shadow: 0 0 0 1px var(--el-color-danger) inset !important; }
.field-hint, .field-error { margin: 6px 0 0; font-size: 12px; line-height: 1.6; overflow-wrap: anywhere; }
.field-hint { color: var(--el-text-color-secondary); }
.field-error { color: var(--el-color-danger); }
.is-valid { display: flex; align-items: center; gap: 5px; color: var(--el-color-success); }
.gender-options { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 6px; }
.gender-choice { display: inline-flex; align-items: center; justify-content: center; gap: 4px; height: 36px; border: 1px solid var(--el-border-color-light); border-radius: 9px; background: transparent; color: var(--el-text-color-secondary); font-size: 12px; cursor: pointer; }
.gender-choice:hover, .gender-choice.active { border-color: var(--el-color-primary); background: var(--el-color-primary-light-9); color: var(--el-color-primary); }
.gender-choice.is-male.active { border-color: #409eff; background: rgba(64, 158, 255, 0.12); color: #409eff; }
.gender-choice.is-female.active { border-color: #ff6699; background: rgba(255, 102, 153, 0.12); color: #ff6699; }
.gender-choice:disabled { cursor: not-allowed; opacity: 0.5; }
.gender-choice:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 2px; }
.tags-field { margin-top: 16px; }
.tags { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-top: 10px; .el-tag { min-height: 28px; padding: 0 10px; } }
.tag-input-row { display: flex; align-items: center; gap: 8px; width: min(100%, 340px); .el-input { flex: 1; min-width: 0; } .el-button { margin: 0; min-height: 36px; } }
.password-form { display: flex; flex-direction: column; gap: 20px; max-width: 440px; margin: 8px auto 20px; padding: 0 4px; }
.password-intro { display: flex; align-items: center; gap: 14px; padding-bottom: 6px; h3 { margin: 0 0 5px; font-size: 17px; color: var(--el-text-color-primary); } p { margin: 0; font-size: 12px; line-height: 1.6; color: var(--el-text-color-secondary); } }
.password-icon { display: grid; place-items: center; flex-shrink: 0; width: 48px; height: 48px; border-radius: 14px; background: var(--el-color-primary-light-9); color: var(--el-color-primary); font-size: 24px; }
.profile-footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.unsaved-label { color: var(--el-text-color-secondary); font-size: 12px; }
.footer-actions { display: flex; align-items: center; gap: 10px; margin-left: auto; .el-button { margin: 0; min-height: 36px; } }
@media (max-width: 760px) {
  .profile-layout { grid-template-columns: minmax(0, 1fr); gap: 24px; }
  .avatar-settings { display: grid; grid-template-columns: 112px minmax(0, 1fr); justify-items: start; align-items: center; gap: 14px 18px; padding: 18px; }
  .avatar-upload-content { grid-row: span 2; }
  .avatar-wrap { width: 88px; height: 88px; }
  .avatar-identity { align-items: flex-start; }
  .avatar-settings .ip-location { grid-column: 2; }
  .avatar-url-field, .avatar-settings > .el-button { grid-column: 1 / -1; }
}
@media (max-width: 480px) {
  .edit-tabs :deep(.el-tabs__header) { margin-bottom: 16px; }
  .avatar-settings { grid-template-columns: 88px minmax(0, 1fr); gap: 12px; padding: 14px; }
  .avatar-url-field { grid-column: 1 / -1; }
  .avatar-upload-content { grid-row: auto; }
  .avatar-settings > .el-button { grid-column: 1 / -1; }
  .field-grid { grid-template-columns: minmax(0, 1fr); gap: 16px; }
  .profile-fields { gap: 20px; }
  .profile-footer { flex-wrap: wrap; }
  .unsaved-label { font-size: 11px; }
}
</style>
