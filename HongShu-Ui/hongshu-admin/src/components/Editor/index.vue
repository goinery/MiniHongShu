<template>
  <input
    ref="imageInput"
    type="file"
    hidden
    accept="image/jpeg,image/png,image/webp,image/gif,image/bmp"
    :disabled="disabled || readOnly || preparing"
    @change="selectImage"
  />
  <div class="editor">
    <quill-editor
      ref="quillEditorRef"
      v-model:content="content"
      contentType="html"
      @textChange="emit('update:modelValue', content)"
      @ready="configureEditor"
      :options="options"
      :style="styles"
    />
  </div>
  <p v-if="type === 'url'" class="editor-image-tip">图片先在编辑器中预览，点击表单“确定”后上传。</p>
</template>

<script setup>
import { computed, getCurrentInstance, onBeforeUnmount, ref, watch } from "vue";
import { resolveFileUrl } from "@/utils/fileUrl";
import { uploadImage } from "@/utils/uploadImage";
import { QuillEditor } from "@vueup/vue-quill";
import "@vueup/vue-quill/dist/vue-quill.snow.css";

const props = defineProps({
  modelValue: { type: String },
  height: { type: Number, default: null },
  minHeight: { type: Number, default: null },
  readOnly: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  fileSize: { type: Number, default: 5 },
  type: { type: String, default: "url" },
});

const emit = defineEmits(["update:modelValue"]);
const { proxy } = getCurrentInstance();
const quillEditorRef = ref();
const imageInput = ref();
const content = ref("");
const preparing = ref(false);
const localImages = new Map();
const uploadedImages = new Map();
let fileReader;
let disposed = false;
let insertIndex = 0;

const options = {
  theme: "snow",
  bounds: document.body,
  debug: /** @type {"warn"} */ ("warn"),
  modules: {
    toolbar: [
      ["bold", "italic", "underline", "strike"],
      ["blockquote", "code-block"],
      [{ list: "ordered" }, { list: "bullet" }],
      [{ indent: "-1" }, { indent: "+1" }],
      [{ size: ["small", false, "large", "huge"] }],
      [{ header: [1, 2, 3, 4, 5, 6, false] }],
      [{ color: [] }, { background: [] }],
      [{ align: [] }],
      ["clean"],
      ["link", "image", "video"],
    ],
  },
  placeholder: "请输入内容",
  readOnly: props.readOnly || props.disabled,
};

const styles = computed(() => ({
  ...(props.minHeight ? { minHeight: props.minHeight + "px" } : {}),
  ...(props.height ? { height: props.height + "px" } : {}),
}));

watch(() => props.modelValue, value => {
  if (value !== content.value) content.value = value === undefined ? "<p></p>" : value;
}, { immediate: true });

watch(() => props.disabled || props.readOnly, disabled => {
  quillEditorRef.value?.getQuill()?.enable(!disabled);
});

function configureEditor(quill) {
  quill.enable(!props.disabled && !props.readOnly);
  if (props.type !== "url") return;
  const toolbar = /** @type {{addHandler(name: string, handler: (value: boolean) => void): void}} */ (quill.getModule("toolbar"));
  toolbar.addHandler("image", value => {
    if (!value || props.disabled || props.readOnly || preparing.value) return;
    insertIndex = quill.getSelection(true)?.index ?? quill.getLength() - 1;
    imageInput.value?.click();
  });
}

function selectImage(event) {
  const file = event.target.files?.[0];
  event.target.value = "";
  if (!file || props.disabled || props.readOnly) return;
  if (!/\.(jpe?g|png|webp|gif|bmp)$/i.test(file.name) || !file.size
      || (props.fileSize && file.size > props.fileSize * 1024 * 1024)) {
    proxy.$modal.msgError("请选择格式正确且大小不超过 " + props.fileSize + "MB 的图片");
    return;
  }
  preparing.value = true;
  fileReader = new FileReader();
  fileReader.onload = () => {
    if (disposed) return;
    const preview = fileReader.result;
    localImages.set(preview, file);
    const quill = quillEditorRef.value.getQuill();
    const index = Math.min(insertIndex, quill.getLength() - 1);
    // Quill 支持 data URL；此处只读本地文件，不调用上传接口。
    quill.insertEmbed(index, "image", preview, "user");
    quill.setSelection(index + 1);
  };
  fileReader.onerror = () => {
    if (!disposed) proxy.$modal.msgError("图片读取失败，请重新选择");
  };
  fileReader.onloadend = () => { preparing.value = false; };
  fileReader.readAsDataURL(file);
}

function pastedImageFile(url) {
  const match = /^data:(image\/(?:png|jpeg|jpg|webp|gif|bmp));base64,([A-Za-z0-9+/=\r\n]+)$/i.exec(url);
  if (!match) throw new Error("图片格式不支持，请重新选择图片");
  const bytes = Uint8Array.from(atob(match[2]), value => value.charCodeAt(0));
  if (!bytes.length || (props.fileSize && bytes.length > props.fileSize * 1024 * 1024)) {
    throw new Error("图片大小不能超过 " + props.fileSize + "MB");
  }
  const extension = match[1].split("/")[1].replace("jpeg", "jpg");
  return new File([bytes], "image." + extension, { type: match[1] });
}

async function uploadPending() {
  if (preparing.value) throw new Error("图片正在读取，请稍候");
  if (disposed) throw new Error("编辑已关闭");
  const quill = quillEditorRef.value.getQuill();
  if (props.type !== "url") return quill.root.innerHTML;
  let index = 0;
  for (const operation of quill.getContents().ops || []) {
    if (disposed) throw new Error("编辑已关闭");
    const preview = typeof operation.insert === "object" ? operation.insert.image : null;
    if (typeof preview === "string" && preview.startsWith("data:")) {
      let url = uploadedImages.get(preview);
      if (!url) {
        const file = localImages.get(preview) || pastedImageFile(preview);
        url = resolveFileUrl(await uploadImage(file));
        if (disposed) throw new Error("编辑已关闭");
        uploadedImages.set(preview, url);
      }
      quill.deleteText(index, 1, "api");
      quill.insertEmbed(index, "image", url, "api");
      if (operation.attributes) quill.formatText(index, 1, operation.attributes, "api");
      localImages.delete(preview);
    } else if (typeof preview === "string" && preview.startsWith("blob:")) {
      throw new Error("图片已失效，请重新选择");
    }
    index += typeof operation.insert === "string" ? operation.insert.length : 1;
  }
  content.value = quill.root.innerHTML;
  emit("update:modelValue", content.value);
  return content.value;
}

onBeforeUnmount(() => {
  disposed = true;
  fileReader?.abort();
  localImages.clear();
  uploadedImages.clear();
});

defineExpose({ uploadPending, preparing });
</script>

<style>
/* 下方组件库内部类和共享布局类由运行时 DOM 或使用方页面提供。 */
.editor-image-tip {
  margin: 8px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
/*noinspection CssUnusedSymbol*/
.editor, .ql-toolbar {
  white-space: pre-wrap !important;
  line-height: normal !important;
}
/*noinspection CssUnusedSymbol*/
.ql-snow .ql-tooltip[data-mode="link"]::before {
  content: "请输入链接地址:";
}
/*noinspection CssUnusedSymbol*/
.ql-snow .ql-tooltip.ql-editing a.ql-action::after {
  border-right: 0;
  content: "保存";
  padding-right: 0;
}
/*noinspection CssUnusedSymbol*/
.ql-snow .ql-tooltip[data-mode="video"]::before {
  content: "请输入视频地址:";
}
/*noinspection CssUnusedSymbol*/
.ql-snow .ql-picker.ql-size .ql-picker-label::before,
.ql-snow .ql-picker.ql-size .ql-picker-item::before {
  content: "14px";
}
/*noinspection CssUnusedSymbol*/
.ql-snow .ql-picker.ql-size .ql-picker-label[data-value="small"]::before,
.ql-snow .ql-picker.ql-size .ql-picker-item[data-value="small"]::before {
  content: "10px";
}
/*noinspection CssUnusedSymbol*/
.ql-snow .ql-picker.ql-size .ql-picker-label[data-value="large"]::before,
.ql-snow .ql-picker.ql-size .ql-picker-item[data-value="large"]::before {
  content: "18px";
}
/*noinspection CssUnusedSymbol*/
.ql-snow .ql-picker.ql-size .ql-picker-label[data-value="huge"]::before,
.ql-snow .ql-picker.ql-size .ql-picker-item[data-value="huge"]::before {
  content: "32px";
}
/*noinspection CssUnusedSymbol*/
.ql-snow .ql-picker.ql-header .ql-picker-label::before,
.ql-snow .ql-picker.ql-header .ql-picker-item::before {
  content: "文本";
}
/*noinspection CssUnusedSymbol*/
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="1"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="1"]::before {
  content: "标题1";
}
/*noinspection CssUnusedSymbol*/
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="2"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="2"]::before {
  content: "标题2";
}
/*noinspection CssUnusedSymbol*/
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="3"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="3"]::before {
  content: "标题3";
}
/*noinspection CssUnusedSymbol*/
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="4"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="4"]::before {
  content: "标题4";
}
/*noinspection CssUnusedSymbol*/
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="5"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="5"]::before {
  content: "标题5";
}
/*noinspection CssUnusedSymbol*/
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="6"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="6"]::before {
  content: "标题6";
}
/*noinspection CssUnusedSymbol*/
.ql-snow .ql-picker.ql-font .ql-picker-label::before,
.ql-snow .ql-picker.ql-font .ql-picker-item::before {
  content: "标准字体";
}
/*noinspection CssUnusedSymbol*/
.ql-snow .ql-picker.ql-font .ql-picker-label[data-value="serif"]::before,
.ql-snow .ql-picker.ql-font .ql-picker-item[data-value="serif"]::before {
  content: "衬线字体";
}
/*noinspection CssUnusedSymbol*/
.ql-snow .ql-picker.ql-font .ql-picker-label[data-value="monospace"]::before,
.ql-snow .ql-picker.ql-font .ql-picker-item[data-value="monospace"]::before {
  content: "等宽字体";
}
</style>
