<template>
  <el-dialog :model-value="modelValue" title="拍摄头像" class="camera-avatar-dialog" width="min(480px, calc(100vw - 32px))" align-center append-to-body destroy-on-close @opened="startCamera" @update:model-value="close">
    <div v-if="error" class="camera-error" role="alert">
      <el-icon><Camera /></el-icon>
      <p>{{ error }}</p>
      <el-button type="primary" plain round @click="startCamera">重新开启摄像头</el-button>
    </div>
    <div v-else v-loading="loading" class="camera-preview" element-loading-text="正在开启摄像头">
      <img v-if="photoPreview" :src="photoPreview" alt="拍摄的头像预览" />
      <video v-else ref="video" autoplay muted playsinline aria-label="摄像头头像预览" />
      <div class="camera-frame" aria-hidden="true"></div>
    </div>
    <template #footer>
      <el-button round @click="close">取消</el-button>
      <el-button v-if="photoPreview" round :icon="RefreshLeft" @click="startCamera">重拍</el-button>
      <el-button v-if="photoPreview" type="primary" round @click="usePhoto">使用这张照片</el-button>
      <el-button v-else type="primary" round :icon="Camera" :disabled="!ready || loading || Boolean(error)" :loading="capturing" @click="capture">拍照</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from "vue";
import { Camera, RefreshLeft } from "@element-plus/icons-vue";

const props = defineProps<{ modelValue: boolean }>();
const emit = defineEmits<{
  (event: "update:modelValue", value: boolean): void;
  (event: "captured", file: File): void;
}>();
const video = ref<HTMLVideoElement>();
const loading = ref(false);
const ready = ref(false);
const capturing = ref(false);
const error = ref("");
const photoPreview = ref("");
let capturedFile: File | null = null;
let stream: MediaStream | null = null;
let generation = 0;

const stopCamera = () => {
  generation++;
  stream?.getTracks().forEach(track => track.stop());
  stream = null;
  if (video.value) video.value.srcObject = null;
  ready.value = false;
  loading.value = false;
  capturing.value = false;
};

const close = () => {
  stopCamera();
  clearPhoto();
  emit("update:modelValue", false);
};

const clearPhoto = () => {
  if (photoPreview.value) URL.revokeObjectURL(photoPreview.value);
  photoPreview.value = "";
  capturedFile = null;
};

const usePhoto = () => {
  if (!capturedFile) return;
  emit("captured", capturedFile);
  close();
};

const startCamera = async () => {
  if (!props.modelValue) return;
  stopCamera();
  clearPhoto();
  const request = generation;
  loading.value = true;
  error.value = "";
  try {
    if (!navigator.mediaDevices?.getUserMedia) throw new Error("当前浏览器无法使用摄像头，请使用 localhost 或 HTTPS 访问");
    const media = await navigator.mediaDevices.getUserMedia({ video: { facingMode: "user", width: { ideal: 720 }, height: { ideal: 720 } }, audio: false });
    if (request !== generation || !props.modelValue) {
      media.getTracks().forEach(track => track.stop());
      return;
    }
    stream = media;
    await nextTick();
    if (request !== generation) { media.getTracks().forEach(track => track.stop()); return; }
    if (!video.value) throw new Error("摄像头预览未就绪，请重试");
    video.value.srcObject = media;
    await video.value.play();
    if (request === generation && props.modelValue) ready.value = true;
  } catch (failure) {
    if (request !== generation || !props.modelValue) return;
    stream?.getTracks().forEach(track => track.stop());
    stream = null;
    const name = (failure as Error).name;
    error.value = name === "NotAllowedError" ? "未获得摄像头权限，请在浏览器中允许使用摄像头后重试"
      : name === "NotFoundError" ? "未检测到可用摄像头，请连接摄像头后重试"
      : name === "NotReadableError" ? "摄像头暂时无法使用，请关闭其他占用摄像头的程序后重试"
      : (failure as Error).message || "无法开启摄像头，请重试";
  } finally {
    if (request === generation) loading.value = false;
  }
};

const capture = async () => {
  const player = video.value;
  if (!ready.value || capturing.value || !player?.videoWidth || !player.videoHeight) return;
  const request = generation;
  capturing.value = true;
  try {
    const canvas = document.createElement("canvas");
    canvas.width = canvas.height = 512;
    const context = canvas.getContext("2d");
    if (!context) throw new Error("无法生成头像，请重试");
    const side = Math.min(player.videoWidth, player.videoHeight);
    context.drawImage(player, (player.videoWidth - side) / 2, (player.videoHeight - side) / 2, side, side, 0, 0, 512, 512);
    const blob = await new Promise<Blob>((resolve, reject) => {
      canvas.toBlob(result => result ? resolve(result) : reject(new Error("拍照失败，请重试")), "image/jpeg", 0.92);
    });
    if (request !== generation || !props.modelValue) return;
    capturedFile = new File([blob], "avatar-camera.jpg", { type: "image/jpeg" });
    photoPreview.value = URL.createObjectURL(blob);
    stopCamera();
  } catch (failure) {
    if (request === generation) { stopCamera(); error.value = (failure as Error).message; }
  } finally {
    if (request === generation) capturing.value = false;
  }
};

watch(() => props.modelValue, visible => {
  if (visible) { error.value = ""; loading.value = true; }
  else { stopCamera(); clearPhoto(); }
});
onBeforeUnmount(() => { stopCamera(); clearPhoto(); });
</script>

<style scoped>
.camera-preview { position: relative; width: 100%; aspect-ratio: 1; overflow: hidden; border-radius: 16px; background: #111827; }
.camera-preview video, .camera-preview img { display: block; width: 100%; height: 100%; object-fit: cover; }
.camera-frame { position: absolute; inset: 8%; border: 2px solid rgba(255, 255, 255, 0.8); border-radius: 50%; box-shadow: 0 0 0 150px rgba(0, 0, 0, 0.25); pointer-events: none; }
.camera-error { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; min-height: 240px; padding: 24px; color: var(--el-text-color-secondary); text-align: center; }
.camera-error > .el-icon { font-size: 40px; }
.camera-error p { margin: 0 0 8px; font-size: 14px; line-height: 1.7; }
</style>

<style>
.camera-avatar-dialog.el-dialog { padding: 22px; border-radius: 20px; }
.camera-avatar-dialog .el-dialog__header { padding-bottom: 18px; }
.camera-avatar-dialog .el-dialog__body { padding: 0; }
.camera-avatar-dialog .el-dialog__footer { padding-top: 18px; }
</style>
