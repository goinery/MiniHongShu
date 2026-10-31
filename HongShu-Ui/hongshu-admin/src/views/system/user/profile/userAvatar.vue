<template>
  <div class="avatar-editor">
    <button type="button" class="user-info-head" title="点击修改头像" aria-label="修改头像" @click="editAvatar">
      <img :src="resolveAvatarUrl(userStore.avatar)" alt="当前头像" class="img-circle img-lg" referrerpolicy="no-referrer" @error="handleAvatarError" />
      <span class="avatar-edit-overlay">修改头像</span>
    </button>
    <el-dialog title="修改头像" v-model="open" width="760px" append-to-body :close-on-click-modal="!saving" :close-on-press-escape="!saving" :show-close="!saving" @opened="modalOpened" @close="closeDialog">
      <el-tabs v-model="mode" class="avatar-mode-tabs" :before-leave="() => !saving">
        <el-tab-pane label="图片 URL" name="url">
          <p class="avatar-help">输入可直接访问的 HTTP 或 HTTPS 图片地址，预览后保存。</p>
          <el-form label-position="top" @submit.prevent="previewExternalAvatar">
            <el-form-item label="图片地址" :error="urlError">
              <div class="avatar-url-field">
                <el-input v-model="externalUrl" placeholder="https://example.com/avatar.png" :disabled="saving" maxlength="512" clearable aria-label="头像图片 URL" @input="resetUrlPreview" />
                <el-button :disabled="saving || !externalUrl.trim()" @click="previewExternalAvatar">预览</el-button>
              </div>
            </el-form-item>
          </el-form>
          <div class="avatar-url-preview">
            <div class="avatar-preview-circle">
              <img v-if="previewUrl && previewState !== 'error'" :key="previewUrl" :src="previewUrl" alt="头像预览" referrerpolicy="no-referrer" @load="completeUrlPreview($event, 'loaded')" @error="completeUrlPreview($event, 'error')" />
              <span v-else>头像预览</span>
            </div>
            <p v-if="previewState === 'loading'" role="status">正在加载图片…</p>
            <p v-else-if="previewState === 'loaded'" class="preview-success" role="status">图片已加载，可以保存</p>
            <p v-else-if="previewState === 'error'" class="preview-error" role="alert">图片无法加载，请检查地址或选择本地上传。</p>
          </div>
        </el-tab-pane>
        <el-tab-pane label="上传并裁剪" name="upload">
          <p class="avatar-help">支持 JPG、PNG、WEBP、GIF、BMP，最大 5MB。</p>
          <div class="avatar-crop-layout">
            <div class="avatar-crop-area">
              <vue-cropper v-if="visible && options.img" ref="cropper" :img="options.img" :info="true" :autoCrop="options.autoCrop" :autoCropWidth="options.autoCropWidth" :autoCropHeight="options.autoCropHeight" :fixedBox="options.fixedBox" :outputType="options.outputType" @realTime="realTime" />
              <div v-else class="avatar-crop-empty">选择图片后在此调整裁剪范围</div>
            </div>
            <div class="avatar-crop-preview">
              <div class="avatar-upload-preview">
                <img v-if="options.previews.url" :src="options.previews.url" :style="options.previews.img" alt="裁剪预览" />
                <span v-else>裁剪预览</span>
              </div>
            </div>
          </div>
          <div class="avatar-crop-toolbar">
            <el-upload ref="avatarPicker" :auto-upload="false" :show-file-list="false" :on-change="selectImage" :disabled="saving" accept="image/jpeg,image/png,image/webp,image/gif,image/bmp">
              <el-button :disabled="saving" icon="Upload">选择图片</el-button>
            </el-upload>
            <div class="avatar-crop-adjustments">
              <el-button icon="Plus" aria-label="放大图片" title="放大" :disabled="saving || !localImageSelected" @click="changeScale(1)" />
              <el-button icon="Minus" aria-label="缩小图片" title="缩小" :disabled="saving || !localImageSelected" @click="changeScale(-1)" />
              <el-button icon="RefreshLeft" aria-label="向左旋转图片" title="向左旋转" :disabled="saving || !localImageSelected" @click="rotateLeft" />
              <el-button icon="RefreshRight" aria-label="向右旋转图片" title="向右旋转" :disabled="saving || !localImageSelected" @click="rotateRight" />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button :disabled="saving" @click="open = false">取消</el-button>
        <el-button v-if="mode === 'url'" type="primary" :loading="saving" :disabled="previewState !== 'loaded'" @click="saveExternalAvatar">保存头像</el-button>
        <el-button v-else type="primary" :loading="saving" :disabled="!visible || !localImageSelected" @click="uploadImg">确认上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { getCurrentInstance, nextTick, onBeforeUnmount, reactive, ref } from 'vue'

import "vue-cropper/dist/index.css";
import { VueCropper } from "vue-cropper";
import { uploadAvatar, updateAvatarUrl } from "@/api/system/user";
import { resolveAvatarUrl, handleAvatarError, normalizeExternalAvatarUrl } from "@/utils/avatar";
import useUserStore from "@/store/modules/user";

const userStore = useUserStore();
const { proxy } = getCurrentInstance();
const open = ref(false);
const visible = ref(false);
const saving = ref(false);
const avatarPicker = ref();
const mode = ref("url");
const externalUrl = ref("");
const urlError = ref("");
const previewUrl = ref("");
const previewState = ref("idle");
const localImageSelected = ref(false);
let localPreview = "";

const options = reactive({
  img: "",
  autoCrop: true,
  autoCropWidth: 200,
  autoCropHeight: 200,
  fixedBox: true,
  outputType: "png",
  previews: {}
});

function editAvatar() {
  mode.value = "url";
  externalUrl.value = /^https?:\/\//i.test(userStore.avatar || "") ? userStore.avatar : "";
  resetUrlPreview();
  open.value = true;
}
function modalOpened() { visible.value = true; }
function rotateLeft() { proxy.$refs.cropper?.rotateLeft(); }
function rotateRight() { proxy.$refs.cropper?.rotateRight(); }
function changeScale(num) { proxy.$refs.cropper?.changeScale(num || 1); }

function resetUrlPreview() {
  urlError.value = "";
  previewUrl.value = "";
  previewState.value = "idle";
}
function previewExternalAvatar() {
  if (saving.value) return;
  try {
    const url = normalizeExternalAvatarUrl(externalUrl.value);
    const input = externalUrl.value;
    urlError.value = "";
    previewState.value = "loading";
    previewUrl.value = "";
    nextTick(() => {
      if (open.value && externalUrl.value === input) previewUrl.value = url;
    });
  } catch (error) {
    resetUrlPreview();
    urlError.value = error.message;
  }
}
function completeUrlPreview(event, state) {
  if (event.target.getAttribute("src") === previewUrl.value) previewState.value = state;
}
async function saveExternalAvatar() {
  if (saving.value || !open.value) return;
  let url;
  try { url = normalizeExternalAvatarUrl(externalUrl.value); }
  catch (error) { urlError.value = error.message; return; }
  if (previewState.value !== "loaded" || previewUrl.value !== url) {
    urlError.value = "请先预览当前图片地址";
    return;
  }
  saving.value = true;
  try { finishSave(await updateAvatarUrl(url)); }
  catch { /* 请求拦截器展示服务端错误，保留地址以便重试。 */ }
  finally { saving.value = false; }
}

function selectImage(uploadFile) {
  const file = uploadFile.raw;
  avatarPicker.value?.clearFiles();
  if (!file || saving.value || !open.value) return;
  if (!/\.(jpe?g|png|webp|gif|bmp)$/i.test(file.name) || !file.size || file.size > 5 * 1024 * 1024) {
    proxy.$modal.msgError("请选择不超过 5MB 的 JPG / PNG / WEBP / GIF / BMP 图片");
    return;
  }
  clearLocalPreview();
  localPreview = URL.createObjectURL(file);
  options.img = localPreview;
  options.previews = {};
  localImageSelected.value = true;
}
function uploadImg() {
  if (saving.value || !open.value || !visible.value || !localImageSelected.value) return;
  saving.value = true;
  try {
    proxy.$refs.cropper.getCropBlob(async data => {
      if (!data || !open.value) { saving.value = false; return; }
      const formData = new FormData();
      formData.append("avatarfile", data, "avatar.png");
      try { finishSave(await uploadAvatar(formData)); }
      catch { /* 请求拦截器展示错误，保留本地裁剪以便重试。 */ }
      finally { saving.value = false; }
    });
  } catch {
    saving.value = false;
    proxy.$modal.msgError("图片读取失败，请重新选择本地图片");
  }
}
function finishSave(response) {
  userStore.avatar = resolveAvatarUrl(response.imgUrl);
  open.value = false;
  proxy.$modal.msgSuccess("头像已更新");
}
function realTime(data) { options.previews = data; }
function closeDialog() {
  clearLocalPreview();
  avatarPicker.value?.clearFiles();
  options.img = "";
  options.previews = {};
  visible.value = false;
  localImageSelected.value = false;
  resetUrlPreview();
}
function clearLocalPreview() {
  if (localPreview) URL.revokeObjectURL(localPreview);
  localPreview = "";
}
onBeforeUnmount(clearLocalPreview);
</script>

<style lang="scss" scoped>
.avatar-editor { display: inline-block; }
.user-info-head { position: relative; display: block; width: 120px; height: 120px; padding: 0; border: 1px solid var(--cf-border); border-radius: 50%; overflow: hidden; background: var(--cf-surface-subtle); cursor: pointer; }
.user-info-head .img-lg { display: block; width: 100%; height: 100%; object-fit: cover; }
.avatar-edit-overlay { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; opacity: 0; background: rgba(24, 28, 34, .5); color: var(--el-color-white); font-size: 13px; transition: opacity .15s; }
.user-info-head:hover .avatar-edit-overlay, .user-info-head:focus-visible .avatar-edit-overlay { opacity: 1; }
.avatar-help { margin: 0 0 20px; color: var(--cf-muted); font-size: 13px; line-height: 1.8; }
.avatar-url-field { display: flex; gap: 10px; width: 100%; }
.avatar-url-preview { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 230px; margin: 24px 0 4px; padding: 24px; border: 1px solid var(--cf-border); border-radius: 8px; background: var(--cf-surface-subtle); }
.avatar-preview-circle { display: flex; align-items: center; justify-content: center; width: 120px; height: 120px; overflow: hidden; border-radius: 50%; border: 1px solid var(--cf-border); background: var(--cf-surface); color: var(--cf-muted); font-size: 12px; }
.avatar-preview-circle img { width: 100%; height: 100%; object-fit: cover; }
.avatar-url-preview p { margin: 16px 0 0; color: var(--cf-muted); font-size: 13px; line-height: 1.6; text-align: center; }
.avatar-url-preview .preview-success { color: var(--el-color-success); }
.avatar-url-preview .preview-error { color: var(--el-color-danger); }
.avatar-crop-layout { display: grid; grid-template-columns: 1.3fr 1fr; gap: 18px; }
.avatar-crop-area { height: 300px; overflow: hidden; border: 1px solid var(--cf-border); border-radius: 6px; background: var(--cf-surface-subtle); }
.avatar-crop-empty { height: 100%; display: flex; align-items: center; justify-content: center; color: var(--cf-muted); font-size: 13px; }
.avatar-crop-preview { display: flex; align-items: center; justify-content: center; border: 1px solid var(--cf-border); border-radius: 6px; background: var(--cf-surface-subtle); }
.avatar-upload-preview { position: relative; top: auto; transform: none; display: block; width: 200px; height: 200px; margin: 0; border: 1px solid var(--cf-border); border-radius: 50%; box-shadow: none; overflow: hidden; background: var(--cf-surface); color: var(--cf-muted); font-size: 12px; }
.avatar-upload-preview > span { display: flex; align-items: center; justify-content: center; height: 100%; }
.avatar-crop-toolbar, .avatar-crop-adjustments { display: flex; align-items: center; gap: 8px; }
.avatar-crop-toolbar { justify-content: space-between; margin-top: 16px; flex-wrap: wrap; }
.avatar-crop-adjustments .el-button + .el-button { margin-left: 0; }
@media (max-width: 600px) {
  .avatar-crop-layout { grid-template-columns: 1fr; }
  .avatar-crop-area { height: 250px; }
  .avatar-crop-preview { padding: 16px; }
}
</style>
