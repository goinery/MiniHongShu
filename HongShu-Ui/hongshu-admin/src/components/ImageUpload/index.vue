<template>
  <div class="component-upload-image">
    <el-upload
      v-model:file-list="fileList"
      multiple
      list-type="picture-card"
      :auto-upload="false"
      :disabled="disabled || uploading"
      :limit="limit"
      :accept="fileType.map(type => '.' + type).join(',')"
      :on-change="selectImage"
      :on-remove="removeImage"
      :before-remove="() => !disabled && !uploading"
      :on-exceed="handleExceed"
      :on-preview="handlePictureCardPreview"
      :class="{ hide: fileList.length >= limit }"
    >
      <el-icon class="avatar-uploader-icon"><plus /></el-icon>
    </el-upload>
    <div class="el-upload__tip" v-if="showTip">
      选择图片仅在本地预览，表单确认后上传。
      <template v-if="fileSize">大小不超过 {{ fileSize }}MB。</template>
      <template v-if="fileType.length">支持 {{ fileType.join(" / ") }}。</template>
    </div>
    <el-dialog v-model="dialogVisible" title="图片预览" width="min(800px, 92vw)" append-to-body>
      <img :src="dialogImageUrl" alt="图片预览" style="display: block; max-width: 100%; margin: 0 auto" />
    </el-dialog>
  </div>
</template>

<script setup>
import { Plus } from '@element-plus/icons-vue'

import { computed, getCurrentInstance, onBeforeUnmount, ref, watch } from "vue";
import { genFileId } from "element-plus";
import { resolveFileUrl, storedFileUrl } from "@/utils/fileUrl";
import { uploadImage } from "@/utils/uploadImage";

const props = defineProps({
  modelValue: [String, Object, Array],
  limit: { type: Number, default: 5 },
  fileSize: { type: Number, default: 5 },
  fileType: { type: Array, default: () => ["png", "jpg", "jpeg"] },
  isShowTip: { type: Boolean, default: true },
  disabled: { type: Boolean, default: false },
});

const { proxy } = getCurrentInstance();
const emit = defineEmits(["update:modelValue"]);
const fileList = ref([]);
const uploading = ref(false);
const dialogImageUrl = ref("");
const dialogVisible = ref(false);
const showTip = computed(() => props.isShowTip);
let disposed = false;

function revokePreview(file) {
  if (file.url?.startsWith("blob:")) URL.revokeObjectURL(file.url);
}

function draftValue() {
  return fileList.value.map(file => storedFileUrl(file.url || "")).filter(Boolean).join(",");
}

function emitDraft() {
  emit("update:modelValue", draftValue());
}

watch(() => props.modelValue, value => {
  const values = !value ? [] : Array.isArray(value) ? value : typeof value === "string" ? value.split(",") : [value];
  const next = values.map(item => {
    const url = resolveFileUrl(typeof item === "string" ? item : item.url);
    // v-model 保存本地预览地址，但 File 始终留在组件内，确认前不发请求。
    return fileList.value.find(file => file.url === url) || {
      ...(typeof item === "object" ? item : {}),
      uid: genFileId(),
      name: typeof item === "string" ? item : item.name,
      url,
      status: "success",
    };
  });
  for (const file of fileList.value) {
    if (!next.includes(file)) revokePreview(file);
  }
  fileList.value = next;
}, { deep: true, immediate: true });

function selectImage(file) {
  if (!file.raw || file.status !== "ready") return;
  const extension = file.name.split(".").pop().toLowerCase();
  const allowed = props.fileType.length
    ? props.fileType.some(type => type.toLowerCase() === extension)
    : file.raw.type.startsWith("image/");
  if (!allowed || !file.raw.size || (props.fileSize && file.raw.size > props.fileSize * 1024 * 1024)) {
    proxy.$modal.msgError("请选择格式正确且大小不超过 " + props.fileSize + "MB 的图片");
    fileList.value = fileList.value.filter(item => item.uid !== file.uid);
    revokePreview(file);
  }
  emitDraft();
}

function removeImage(file) {
  revokePreview(file);
  emitDraft();
}

function handleExceed() {
  proxy.$modal.msgError("图片数量不能超过 " + props.limit + " 张");
}

function handlePictureCardPreview(file) {
  dialogImageUrl.value = file.url;
  dialogVisible.value = true;
}

// 仅由父表单的确认/保存操作调用。
async function uploadPending() {
  if (uploading.value) throw new Error("图片正在上传");
  uploading.value = true;
  try {
    for (const file of fileList.value) {
      if (disposed) throw new Error("编辑已关闭");
      if (!file.raw) continue;
      const url = await uploadImage(file.raw);
      if (disposed) throw new Error("编辑已关闭");
      revokePreview(file);
      file.url = resolveFileUrl(url);
      file.raw = undefined;
      file.status = "success";
      emitDraft();
    }
    if (fileList.value.some(file => file.url?.startsWith("blob:"))) {
      throw new Error("图片已失效，请重新选择");
    }
    return draftValue();
  } finally {
    uploading.value = false;
  }
}

onBeforeUnmount(() => {
  disposed = true;
  fileList.value.forEach(revokePreview);
  fileList.value = [];
});

defineExpose({ uploadPending });
</script>

<style scoped lang="scss">
:deep(.hide .el-upload--picture-card) {
  display: none;
}
</style>
