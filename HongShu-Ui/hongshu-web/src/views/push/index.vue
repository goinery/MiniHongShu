<template>
  <div class="container publish-page">
    <div v-if="isLogin" v-loading="pushLoading" class="push-container" :element-loading-text="note.id ? '正在保存，请稍候' : '正在发布，请稍候'">
      <header class="header">
        <span class="header-icon"></span>
        <h1 class="header-title">{{ note.id ? '编辑笔记' : '发布图文' }}</h1>
      </header>

      <div class="editor-layout">
        <section class="media-section" aria-label="笔记图片">
          <div class="field-heading">
            <span class="section-label"><span class="required">*</span>图片</span>
            <span class="field-count">{{ fileList.length }} / {{ MAX_NOTE_IMAGES }}</span>
          </div>
          <ul class="image-grid" :class="{ 'is-empty': !fileList.length }" aria-label="已选图片">
            <li
              v-for="(file, index) in fileList"
              :key="file.uid"
              class="image-card"
              :class="{ 'is-dragging': draggedImageUid === file.uid, 'is-drop-target': dropImageUid === file.uid }"
              :draggable="!pushLoading && fileList.length > 1"
              tabindex="0"
              aria-keyshortcuts="Alt+ArrowLeft Alt+ArrowRight"
              @dragstart="startImageDrag($event, file)"
              @dragover="overImageDrag($event, file)"
              @drop="dropImage($event, file)"
              @dragend="endImageDrag"
              @keydown.alt.left.prevent="moveImage(index, index - 1)"
              @keydown.alt.right.prevent="moveImage(index, index + 1)"
            >
              <button type="button" class="image-preview-button" :aria-label="'预览图片：' + file.name" @click="handlePreview(file)">
                <img :src="file.url" :alt="file.name" draggable="false" />
              </button>
              <span v-if="fileList[0]?.uid === file.uid" class="cover-badge">封面</span>
              <button v-else type="button" class="image-cover-button" :disabled="pushLoading" @click="setCover(file)">设为封面</button>
              <button type="button" class="image-remove-button round-close" :aria-label="'移除图片：' + file.name" :disabled="pushLoading" @click.stop="removeImage(file)">
                <el-icon><Close /></el-icon>
              </button>
            </li>
            <li v-show="fileList.length < MAX_NOTE_IMAGES" class="image-upload-slot">
              <el-upload
                v-model:file-list="fileList"
                class="gallery-upload"
                list-type="picture-card"
                multiple
                :show-file-list="false"
                :limit="MAX_NOTE_IMAGES"
                accept="image/jpeg,image/png,image/webp,.jpg,.jpeg,.png,.webp"
                :auto-upload="false"
                :disabled="pushLoading"
                :on-exceed="() => ElMessage.warning('最多上传 9 张图片')"
              >
                <div class="upload-plus">
                  <el-icon :size="28"><Plus /></el-icon>
                  <span>{{ fileList.length ? '继续添加' : '上传图片' }}</span>
                </div>
              </el-upload>
            </li>
          </ul>

          <div class="image-link-field">
            <label for="publish-image-url" class="section-label">图片链接</label>
            <div class="image-url-input">
              <el-input
                id="publish-image-url"
                v-model="imageUrl"
                :prefix-icon="Link"
                placeholder="粘贴图片链接"
                clearable
                :disabled="addingImage || pushLoading || fileList.length >= MAX_NOTE_IMAGES"
                @keyup.enter.prevent="addImageUrl"
              />
              <el-button type="primary" :icon="Plus" aria-label="添加链接图片" :loading="addingImage" :disabled="pushLoading || fileList.length >= MAX_NOTE_IMAGES || !imageUrl.trim()" @click="addImageUrl" />
            </div>
            <p v-if="imageUrlError" class="image-url-error" role="alert">{{ imageUrlError }}</p>
          </div>
          <el-dialog v-model="dialogVisible" title="图片预览" width="min(640px, 92vw)" append-to-body>
            <img class="preview-img" :src="dialogImageUrl" alt="预览" />
          </el-dialog>
        </section>

        <section class="writing-section" aria-label="笔记内容">
          <div class="field-block">
            <label for="publish-title" class="section-label"><span class="required">*</span>标题</label>
            <el-input id="publish-title" ref="TitleRef" v-model="note.title" maxlength="20" show-word-limit placeholder="填写标题会有更多赞哦" class="input-title" :disabled="pushLoading" />
          </div>
          <div class="field-block">
            <label for="publish-content" class="section-label">正文</label>
            <el-input id="publish-content" v-model="note.content" maxlength="250" show-word-limit :autosize="{ minRows: 7, maxRows: 12 }" type="textarea" placeholder="添加正文描述，让更多人看到你" :disabled="pushLoading" />
          </div>

          <div class="field-block">
            <div class="field-heading">
              <span class="section-label">标签</span>
              <span class="field-count">{{ dynamicTags.length }} / 5</span>
            </div>
            <div class="tag-controls">
              <HoverPopover id="publish-tags" ref="tagPicker" class="tag-picker" title="添加标签" :width="360" :disabled="pushLoading">
                <template #trigger><el-icon><Plus /></el-icon><span>添加标签</span></template>
                <div class="custom-tag-input">
                  <el-input v-model="inputValue" maxlength="12" placeholder="输入标签名称" aria-label="自定义标签" clearable :disabled="dynamicTags.length >= 5" @keyup.enter.prevent="handleInputConfirm" />
                  <el-button type="primary" :disabled="!inputValue.trim() || dynamicTags.length >= 5" @click="handleInputConfirm">添加</el-button>
                </div>
                <div v-if="hotTags.length" class="suggested-tags">
                  <div class="popover-section-label">推荐标签</div>
                  <div class="hot-tag">
                    <button v-for="tag in hotTags" :key="tag" type="button" class="tag-option" :class="{ 'is-selected': dynamicTags.includes(tag) }" :aria-pressed="dynamicTags.includes(tag)" :disabled="dynamicTags.length >= 5 && !dynamicTags.includes(tag)" @click="selectHotTag(tag)">
                      <el-icon v-if="dynamicTags.includes(tag)"><Check /></el-icon><span># {{ tag }}</span>
                    </button>
                  </div>
                </div>
              </HoverPopover>
            </div>
            <div v-if="dynamicTags.length" class="tag-list">
              <el-tag v-for="tag in dynamicTags" :key="tag" :closable="!pushLoading" disable-transitions round effect="plain" type="primary" class="tag-item" @close="handleClose(tag)"># {{ tag }}</el-tag>
            </div>
          </div>

          <div class="field-block categorys">
            <span class="section-label"><span class="required">*</span>分类</span>
            <HoverPopover id="publish-category" ref="categoryPicker" class="category-picker" title="选择分类" :width="360" :disabled="pushLoading" :pin-on-panel-interaction="false">
              <template #trigger><el-icon><CollectionTag /></el-icon><span class="category-label" :title="categoryLabel">{{ categoryLabel }}</span></template>
              <el-skeleton v-if="categoryLoading" :rows="3" animated />
              <el-empty v-else-if="categoryFailed" :image-size="64" description="分类加载失败">
                <el-button type="primary" plain @click="loadCategories">重新加载</el-button>
              </el-empty>
              <template v-else-if="options.length">
                <el-cascader-panel v-model="categoryList" :options="options" :props="categoryProps" class="category-panel" />
                <div v-if="categoryList.length" class="category-actions"><el-button type="primary" link @click="categoryList = []">清除选择</el-button></div>
              </template>
              <el-empty v-else :image-size="64" description="暂无可选分类" />
            </HoverPopover>
          </div>
        </section>
      </div>

      <footer class="submit">
        <span class="publish-summary">{{ fileList.length }} 张图片<span class="summary-dot">·</span>{{ dynamicTags.length }} 个标签</span>
        <div class="submit-actions">
          <el-button round :disabled="pushLoading" @click="resetData">清空</el-button>
          <el-button type="primary" round :loading="pushLoading" :disabled="addingImage" @click="pubslish">{{ note.id ? '保存修改' : '发布笔记' }}</el-button>
        </div>
      </footer>
    </div>
    <div v-else class="push-guest">
      <el-empty description="登录后即可发布笔记"><el-button type="primary" round @click="openLogin()">去登录</el-button></el-empty>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, nextTick, inject, onBeforeUnmount, watch } from "vue";
import { Plus, Link, Close, Check, CollectionTag } from "@element-plus/icons-vue";
import { useRoute } from "vue-router";
import type { UploadUserFile, CascaderProps, ElInput } from "element-plus";
import { ElMessage, genFileId } from "element-plus";
import { useUserStore } from "@/store/userStore";
import { getCategoryTreeData } from "@/api/category";
import { saveNoteByDTO, getNoteById, updateNoteByDTO } from "@/api/note";
import { uploadNoteImage } from "@/api/oss";
import { getTagByKeyword } from "@/api/tag";
import { MAX_NOTE_IMAGES, normalizeImageUrl, readImageSize, resolveNoteImages } from "@/utils/noteImages";
import HoverPopover from "@/components/HoverPopover.vue";

const categoryProps: CascaderProps = {
  label: "title",
  value: "id",
  checkStrictly: true,
  checkOnClickNode: true,
  expandTrigger: "hover",
};
const userStore = useUserStore();
const route = useRoute();
const openLogin = inject("openLogin", () => {});

const fileList = ref<UploadUserFile[]>([]);
const draggedImageUid = ref<number>();
const dropImageUid = ref<number>();
let ignorePreviewUntil = 0;
const dialogImageUrl = ref("");
const dialogVisible = ref(false);
const imageUrl = ref("");
const imageUrlError = ref("");
const addingImage = ref(false);
let imageRequest = 0;
const categoryList = ref<Array<any>>([]);
const options = ref<Array<any>>([]);
const categoryLoading = ref(false);
const categoryFailed = ref(false);
const note = ref<any>({});
const pushLoading = ref(false);
const isLogin = ref(false);
const inputValue = ref("");
const dynamicTags = ref<Array<string>>([]);
const TitleRef = ref<InstanceType<typeof ElInput>>();
const tagPicker = ref<InstanceType<typeof HoverPopover>>();
const categoryPicker = ref<InstanceType<typeof HoverPopover>>();
const hotTags = ref<Array<string>>([]);

const categoryLabel = computed(() => {
  let nodes = options.value;
  const labels: string[] = [];
  for (const id of categoryList.value) {
    const node = nodes.find(item => String(item.id) === String(id));
    if (!node) break;
    labels.push(node.title);
    nodes = node.children || [];
  }
  return labels.join(" / ") || "选择分类";
});

watch(imageUrl, () => { imageUrlError.value = ""; });

const addImageUrl = async () => {
  if (addingImage.value || pushLoading.value) return;
  const request = ++imageRequest;
  imageUrlError.value = "";
  try {
    if (fileList.value.length >= MAX_NOTE_IMAGES) {
      imageUrlError.value = "最多添加 9 张图片";
      return;
    }
    const url = normalizeImageUrl(imageUrl.value);
    addingImage.value = true;
    await readImageSize(url);
    if (request !== imageRequest) return;
    if (fileList.value.length >= MAX_NOTE_IMAGES) {
      imageUrlError.value = "最多添加 9 张图片";
      return;
    }
    fileList.value.push({ name: "链接图片", url, uid: genFileId(), status: "success" });
    imageUrl.value = "";
  } catch (error) {
    if (request === imageRequest) imageUrlError.value = (error as Error).message;
  } finally {
    if (request === imageRequest) addingImage.value = false;
  }
};

const handlePreview = (file: UploadUserFile) => {
  if (Date.now() < ignorePreviewUntil) return;
  dialogImageUrl.value = file.url || "";
  dialogVisible.value = true;
};

const removeImage = async (file: UploadUserFile) => {
  if (pushLoading.value) return;
  fileList.value = fileList.value.filter((item) => item.uid !== file.uid);
  await nextTick();
  if (file.url?.startsWith("blob:")) URL.revokeObjectURL(file.url);
};

const setCover = (file: UploadUserFile) => {
  if (pushLoading.value) return;
  const index = fileList.value.findIndex(item => item.uid === file.uid);
  if (index <= 0) return;
  const files = [...fileList.value];
  const [cover] = files.splice(index, 1);
  fileList.value = [cover, ...files];
};

const moveImage = (from: number, to: number) => {
  if (pushLoading.value || from < 0 || to < 0 || from >= fileList.value.length || to >= fileList.value.length || from === to) return;
  const files = [...fileList.value];
  const [moved] = files.splice(from, 1);
  files.splice(to, 0, moved);
  fileList.value = files;
};

const startImageDrag = (event: DragEvent, file: UploadUserFile) => {
  if (pushLoading.value || fileList.value.length < 2 || file.uid === undefined || !event.dataTransfer) {
    event.preventDefault();
    return;
  }
  event.dataTransfer.effectAllowed = "move";
  event.dataTransfer.setData("text/plain", String(file.uid));
  draggedImageUid.value = file.uid;
  dropImageUid.value = undefined;
  ignorePreviewUntil = Infinity;
};

const overImageDrag = (event: DragEvent, file: UploadUserFile) => {
  if (pushLoading.value || draggedImageUid.value === undefined) return;
  event.preventDefault();
  if (event.dataTransfer) event.dataTransfer.dropEffect = "move";
  dropImageUid.value = file.uid === draggedImageUid.value ? undefined : file.uid;
};

const endImageDrag = () => {
  draggedImageUid.value = undefined;
  dropImageUid.value = undefined;
  ignorePreviewUntil = Date.now() + 200;
};

const dropImage = (event: DragEvent, file: UploadUserFile) => {
  if (draggedImageUid.value === undefined) return;
  event.preventDefault();
  event.stopPropagation();
  const from = fileList.value.findIndex(item => item.uid === draggedImageUid.value);
  const to = fileList.value.findIndex(item => item.uid === file.uid);
  moveImage(from, to);
  endImageDrag();
};

const handleClose = (tag: string) => {
  dynamicTags.value = dynamicTags.value.filter((item) => item !== tag);
};

const addTag = (raw: string) => {
  const tag = raw.trim();
  if (!tag) return false;
  if (dynamicTags.value.includes(tag)) {
    ElMessage.warning("标签已添加");
    return false;
  }
  if (dynamicTags.value.length >= 5) {
    ElMessage.warning("最多只能选择 5 个标签");
    return false;
  }
  dynamicTags.value.push(tag);
  return true;
};

const handleInputConfirm = () => {
  if (addTag(inputValue.value)) inputValue.value = "";
};

const selectHotTag = (val: string) => {
  if (dynamicTags.value.includes(val)) handleClose(val);
  else addTag(val);
};

const getHotTag = () => {
  getTagByKeyword(1, 10, "").then((res) => {
    const { records } = res.data || {};
    hotTags.value = (records || []).map((item: any) => item.title).filter(Boolean);
  }).catch(() => { hotTags.value = []; });
};

const loadCategories = async () => {
  if (categoryLoading.value) return;
  categoryLoading.value = true;
  categoryFailed.value = false;
  try {
    const response = await getCategoryTreeData();
    options.value = response.data || [];
  } catch {
    categoryFailed.value = true;
  } finally {
    categoryLoading.value = false;
  }
};

const getNoteByIdMethod = (noteId: string) => {
  getNoteById(noteId).then((res) => {
    const { data } = res;
    note.value = data;
    const urls = JSON.parse(data.urls);
    fileList.value = urls.map((url: string) => ({
      name: "笔记图片", url, uid: genFileId(), status: "success",
    }));
    categoryList.value = [data.cpid, data.cid].filter(Boolean);
    dynamicTags.value = (data.tagList || []).map((item: any) => item.title);
  });
};

const pubslish = async () => {
  if (pushLoading.value || addingImage.value) return;
  if (!fileList.value.length) {
    ElMessage.warning("请上传图片或添加图片链接");
    return;
  }
  if (!String(note.value.title || "").trim()) {
    ElMessage.warning("请填写标题");
    await nextTick();
    TitleRef.value?.focus();
    return;
  }
  if (!categoryList.value.length) {
    ElMessage.warning("请选择分类");
    categoryPicker.value?.openPinned(true);
    return;
  }
  const coverUrl = fileList.value[0]?.url;
  if (!coverUrl) {
    ElMessage.warning("图片读取失败，请重新上传");
    return;
  }
  pushLoading.value = true;
  try {
    const { width, height } = await readImageSize(coverUrl);
    const urls = await resolveNoteImages(fileList.value, uploadNoteImage);
    const params = new FormData();
    Object.assign(note.value, {
      urls,
      noteCover: urls[0],
      noteCoverHeight: width / height >= 1.3 ? 200 : 300,
      count: urls.length,
      type: 1,
      title: String(note.value.title).trim(),
      cpid: categoryList.value[0],
      cid: categoryList.value[1] || categoryList.value[0],
      tagList: dynamicTags.value,
    });
    params.append("noteData", JSON.stringify(note.value));
    const editing = Boolean(note.value.id);
    await (editing ? updateNoteByDTO(params) : saveNoteByDTO(params));
    resetData();
    ElMessage.success(editing ? "修改成功" : "发布成功，请等待审核");
  } catch (error) {
    // 接口错误由请求拦截器提示；这里只处理图片加载等本地错误。
    if (error instanceof Error && !(error as any).isAxiosError) ElMessage.error(error.message);
  } finally {
    pushLoading.value = false;
  }
};

const resetData = () => {
  endImageDrag();
  imageRequest++;
  addingImage.value = false;
  imageUrl.value = "";
  imageUrlError.value = "";
  dialogVisible.value = false;
  dialogImageUrl.value = "";
  note.value = {};
  categoryList.value = [];
  const previousFiles = fileList.value;
  fileList.value = [];
  void nextTick(() => clearLocalPreviews(previousFiles));
  pushLoading.value = false;
  dynamicTags.value = [];
  inputValue.value = "";
  tagPicker.value?.close();
  categoryPicker.value?.close();
};

const clearLocalPreviews = (files = fileList.value) => {
  for (const file of files) {
    if (file.url?.startsWith("blob:")) URL.revokeObjectURL(file.url);
  }
};

onBeforeUnmount(() => { imageRequest++; clearLocalPreviews(); });

const initData = () => {
  isLogin.value = userStore.isLogin();
  if (!isLogin.value) return;
  const noteId = route.query.noteId as string;
  if (noteId) getNoteByIdMethod(noteId);
  void loadCategories();
  getHotTag();
};

initData();
</script>

<style lang="less" scoped>
.publish-page {
  width: 100%; min-width: 0; min-height: 100vh; min-height: 100dvh;
  padding: 96px 28px 32px; box-sizing: border-box;
}
.push-container {
  width: 100%; max-width: 1080px; margin: 0 auto; overflow: hidden;
  border: 1px solid var(--el-border-color-extra-light); border-radius: 20px;
  background: var(--el-bg-color-overlay); box-shadow: 0 6px 28px rgba(0, 0, 0, 0.04);
}
.header {
  display: flex; align-items: center; gap: 10px; padding: 22px 28px;
  border-bottom: 1px solid var(--el-border-color-extra-light);
  .header-icon { width: 4px; height: 20px; flex-shrink: 0; border-radius: 4px; background: var(--el-color-primary); }
  .header-title { margin: 0; font-size: 20px; line-height: 28px; font-weight: 600; color: var(--el-text-color-primary); }
}
.editor-layout { display: grid; grid-template-columns: minmax(0, 0.85fr) minmax(0, 1.3fr); align-items: start; gap: 28px; padding: 28px; }
.media-section { min-width: 0; padding: 18px; border: 1px solid var(--el-border-color-extra-light); border-radius: 16px; background: var(--el-fill-color-extra-light); }
.writing-section { min-width: 0; display: flex; flex-direction: column; gap: 22px; }
.field-block { min-width: 0; }
.section-label { display: block; margin: 0 0 10px; color: var(--el-text-color-regular); font-size: 13px; line-height: 20px; font-weight: 500; }
.required { margin-right: 5px; color: var(--el-color-primary); }
.field-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 12px; .section-label { margin: 0; } }
.field-count { color: var(--el-text-color-secondary); font-size: 12px; font-variant-numeric: tabular-nums; }
.input-title :deep(.el-input__wrapper) { min-height: 42px; }
.writing-section :deep(.el-input__wrapper), .writing-section :deep(.el-textarea__inner) { border-radius: 10px; }
.writing-section :deep(.el-textarea__inner) { padding: 12px 14px 24px; line-height: 1.7; resize: vertical; }
.image-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; width: 100%; margin: 0; padding: 0; }
.image-card { position: relative; min-width: 0; aspect-ratio: 1; overflow: hidden; border: 1px solid var(--el-border-color-lighter); border-radius: 12px; }
.image-card[draggable="true"], .image-card[draggable="true"] .image-preview-button { cursor: grab; }
.image-card.is-dragging { opacity: 0.4; cursor: grabbing; }
.image-card.is-drop-target { border-color: var(--el-color-primary); box-shadow: 0 0 0 2px var(--el-color-primary-light-7); }
.image-card:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 2px; }
.image-upload-slot { min-width: 0; }
.gallery-upload :deep(.el-upload--picture-card) { display: flex; width: 100%; height: auto; aspect-ratio: 1; margin: 0; border-radius: 12px; border-color: var(--el-color-primary-light-7); background: var(--el-color-primary-light-9); }
.image-grid.is-empty .image-upload-slot { grid-column: 1 / -1; }
.image-grid.is-empty .gallery-upload :deep(.el-upload--picture-card) { aspect-ratio: auto; height: 208px; }
.upload-plus { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 10px; color: var(--el-color-primary); font-size: 13px; }
.image-preview-button { display: block; width: 100%; height: 100%; padding: 0; background: transparent; cursor: zoom-in; img { display: block; width: 100%; height: 100%; object-fit: cover; } }
.cover-badge { position: absolute; top: 7px; left: 7px; padding: 3px 7px; border-radius: 6px; background: rgba(0, 0, 0, 0.6); color: #fff; font-size: 11px; line-height: 16px; pointer-events: none; }
.image-remove-button { --close-control-size: 24px; position: absolute; top: 6px; right: 6px; font-size: 12px; }
.image-cover-button { position: absolute; left: 50%; bottom: 8px; transform: translateX(-50%); width: max-content; max-width: calc(100% - 12px); padding: 4px 8px; border-radius: 999px; background: rgba(0, 0, 0, 0.65); color: #fff; font-size: 11px; line-height: 16px; cursor: pointer; }
.image-remove-button, .image-cover-button { opacity: 0; transition: opacity 160ms; }
.image-card:hover, .image-card:focus-within { .image-remove-button, .image-cover-button { opacity: 1; } }
.image-preview-button:focus-visible, .image-cover-button:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: -2px; }
.image-link-field { margin-top: 18px; }
.image-url-input { display: flex; align-items: center; gap: 8px; .el-input { flex: 1; min-width: 0; } .el-button { width: 36px; min-width: 36px; height: 36px; margin: 0; padding: 0; border-radius: 10px; } :deep(.el-input__wrapper) { min-height: 36px; border-radius: 10px; } }
.image-url-error { margin: 8px 0 0; color: var(--el-color-danger); font-size: 12px; line-height: 18px; }
.preview-img { display: block; width: 100%; max-height: 75vh; object-fit: contain; }
.tag-controls { display: flex; }
.tag-list { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-top: 12px; .tag-item { min-height: 30px; padding: 0 10px; } }
.custom-tag-input { display: flex; align-items: center; gap: 8px; .el-input { min-width: 0; } }
.suggested-tags { margin-top: 18px; }
.popover-section-label { margin-bottom: 10px; color: var(--el-text-color-secondary); font-size: 12px; }
.hot-tag { display: flex; flex-wrap: wrap; gap: 8px; }
.tag-option { display: inline-flex; align-items: center; gap: 4px; padding: 6px 10px; border: 1px solid var(--el-border-color-light); border-radius: 999px; background: transparent; color: var(--el-text-color-regular); font-size: 12px; cursor: pointer; }
.tag-option:hover, .tag-option.is-selected { border-color: var(--el-color-primary); background: var(--el-color-primary-light-9); color: var(--el-color-primary); }
.tag-option:disabled { opacity: 0.45; cursor: not-allowed; }
.tag-option:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 2px; }
.tag-picker, .category-picker { width: 128px; }
.category-label { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.category-panel { width: 100%; max-width: 100%; overflow-x: auto; border: 0; :deep(.el-cascader-menu) { min-width: 150px; flex: 1; } }
.category-actions { display: flex; justify-content: flex-end; padding-top: 12px; border-top: 1px solid var(--el-border-color-extra-light); }
.submit { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 18px 28px; border-top: 1px solid var(--el-border-color-extra-light); }
.publish-summary { color: var(--el-text-color-secondary); font-size: 12px; }
.summary-dot { margin: 0 8px; }
.submit-actions { display: flex; align-items: center; gap: 10px; .el-button { min-width: 88px; height: 38px; margin: 0; } }
.push-guest { padding-top: 64px; }
@media (hover: none) { .image-remove-button, .image-cover-button { opacity: 1; } }
@media (max-width: 959px) {
  .publish-page { padding: 88px 16px 24px; }
  .editor-layout { grid-template-columns: minmax(0, 1fr); gap: 24px; }
  .media-section { padding: 16px; }
  .image-grid.is-empty .gallery-upload :deep(.el-upload--picture-card) { height: 160px; }
}
@media (max-width: 560px) {
  .publish-page { padding: 84px 8px 16px; }
  .push-container { border-radius: 16px; }
  .header { padding: 18px; .header-title { font-size: 18px; } }
  .editor-layout { padding: 18px; gap: 22px; }
  .submit { padding: 16px 18px; flex-wrap: wrap; }
  .submit-actions { margin-left: auto; }
}
</style>
