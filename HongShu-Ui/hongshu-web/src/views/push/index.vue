<template></template>

<script lang="ts" setup>
import { ref, computed, nextTick, inject, onBeforeUnmount, watch } from "vue";
import { Plus, Link, Close, Check, CollectionTag } from "@element-plus/icons-vue";
import { useRoute } from "vue-router";
import type { UploadUserFile, CascaderProps, ElInput } from "element-plus";
import { ElMessage, genFileId } from "element-plus";
import { useUserStore } from "@/store/userStore";
import { getCategoryTreeData } from "@/api/category";
import { saveNoteByDTO, getNoteById, updateNoteByDTO, uploadNoteImage } from "@/api/note";
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

<style lang="less" scoped></style>
