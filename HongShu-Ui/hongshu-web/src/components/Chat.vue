<template>
  <Teleport to="body">
    <div class="chat-overlay" @click.self="close">
      <section
        ref="dialogRef"
        class="chat-container"
        role="dialog"
        aria-modal="true"
        aria-labelledby="chat-dialog-title"
        tabindex="-1"
        @keydown.esc="close"
      >
        <header class="chat-header">
          <div class="header-user">
            <el-avatar :src="acceptUser.avatar" :size="40" />
            <span id="chat-dialog-title" class="header-name">{{ acceptUser.username || "私信" }}</span>
          </div>
          <div class="header-tool">
            <el-dropdown trigger="click" @command="clearHistory">
              <button type="button" class="icon-button" aria-label="聊天选项">
                <More class="icon-item" />
              </button>
              <template #dropdown>
                <el-dropdown-menu><el-dropdown-item command="clear">清空我的聊天记录</el-dropdown-item></el-dropdown-menu>
              </template>
            </el-dropdown>
            <button type="button" class="icon-button chat-close" aria-label="关闭私聊窗口" :disabled="sending" @click="close">
              <Close class="icon-item" />
            </button>
          </div>
        </header>
        <main class="chat-main">
          <div class="chat-record" ref="ChatRef" @scroll="showScroll()">
            <div v-if="!loading && !dataList.length" class="chat-empty">暂无聊天记录</div>
            <div v-for="(item, index) in dataList" :key="index">
              <div class="message-my-item" v-if="item.acceptUid === acceptUser.id">
                <el-button v-if="item.isFailed" link type="danger" @click="sendMessage(item, true)">发送失败，重试</el-button>
                <Loading v-show="item.isLoading" style="width: 0.8em; height: 0.8em" />
                <div class="message-my-conent" v-if="item.msgType == 1">{{ item.content }}</div>
                <img alt="消息图片" :src="item.content" class="message-img" v-if="item.msgType == 2" />
                <div class="user-avatar"><el-avatar :src="currentUser.avatar" /></div>
              </div>
              <div class="message-item" v-else>
                <div class="user-avatar"><el-avatar :src="acceptUser.avatar" /></div>
                <div class="message-conent" v-if="item.msgType == 1">{{ item.content }}</div>
                <img alt="消息图片" :src="item.content" class="message-img" v-if="item.msgType == 2" />
              </div>
            </div>
          </div>
          <div class="chat-input">
            <div class="input-tool">
              <div class="tool-left">
                <el-popover v-model:visible="emojiPickerVisible" trigger="click" placement="top-start" :width="280" :teleported="false">
                  <template #reference>
                    <button type="button" class="icon-button emoji-trigger" aria-label="选择表情" title="选择表情" :disabled="sending" @mousedown.prevent="rememberEditorSelection">
                      <svg class="icon-item" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
                        <circle cx="12" cy="12" r="9" />
                        <path d="M8 14a4.5 4.5 0 0 0 8 0" stroke-linecap="round" />
                        <circle cx="8.5" cy="9" r=".75" fill="currentColor" stroke="none" />
                        <circle cx="15.5" cy="9" r=".75" fill="currentColor" stroke="none" />
                      </svg>
                    </button>
                  </template>
                  <div class="emoji-grid" role="group" aria-label="常用表情">
                    <button v-for="emoji in emojis" :key="emoji" type="button" class="emoji-option" :aria-label="emoji" :disabled="sending" @mousedown.prevent @click="insertEmoji(emoji)">{{ emoji }}</button>
                  </div>
                </el-popover>
                <el-upload ref="imageUpload" multiple :auto-upload="false" :show-file-list="false" :on-change="handleChange" accept="image/jpeg,image/png,image/webp,image/gif,image/bmp" :disabled="sending || imageLimitReached">
                  <button type="button" class="icon-button" aria-label="选择图片预览" :title="imageLimitReached ? `最多添加 ${MAX_IMAGE_COUNT} 张图片，删除后可继续添加` : `选择图片，最多 ${MAX_IMAGE_COUNT} 张`" :disabled="sending || imageLimitReached">
                    <Picture class="icon-item" />
                  </button>
                </el-upload>
              </div>
              <button type="button" class="icon-button history-button" aria-label="加载更早的聊天记录" title="加载更早的聊天记录" :disabled="loading" @click="loadEarlierMessages">
                <Loading v-if="loading" class="icon-item is-loading" />
                <Clock v-else class="icon-item" />
                <span>{{ loading ? "加载中…" : "聊天记录" }}</span>
              </button>
            </div>
            <div class="input-content">
              <div v-if="imagePreviews.length" class="draft-images" aria-label="待发送的图片">
                <div v-for="image in imagePreviews" :key="image.id" class="draft-image">
                  <img :src="image.preview" :alt="image.name" />
                  <button type="button" class="remove-image" :aria-label="`移除图片 ${image.name}`" :disabled="sending" @click="removeImage(image.id)">
                    <Close />
                  </button>
                </div>
              </div>
              <textarea
                id="post-textarea"
                ref="postContent"
                class="post-content"
                aria-label="消息内容"
                aria-describedby="chat-draft-count"
                v-model="draftText"
                :maxlength="MAX_MESSAGE_LENGTH"
                :readonly="sending"
                placeholder="请输入消息，支持发送图片哦～"
                @keydown="handleEditorKeydown"
                @input="handleDraftInput"
                @compositionstart="composing = true"
                @compositionend="handleCompositionEnd"
                @keyup="rememberEditorSelection"
                @mouseup="rememberEditorSelection"
                @select="rememberEditorSelection"
              ></textarea>
              <div class="input-btn">
                <div class="input-counts">
                  <span id="chat-draft-count" class="input-count" :class="{ 'is-limit': draftText.length >= MAX_MESSAGE_LENGTH }" :title="draftText.length >= MAX_MESSAGE_LENGTH ? '已达到字数上限，可删除或选中文字替换' : '消息字数上限'">{{ draftText.length }}/{{ MAX_MESSAGE_LENGTH }}</span>
                  <span v-if="pendingImages.size" class="input-count" :class="{ 'is-limit': imageLimitReached }" :title="imageLimitReached ? '已达到图片数量上限，删除后可继续添加' : `每次最多发送 ${MAX_IMAGE_COUNT} 张图片`" role="status">图片 {{ pendingImages.size }}/{{ MAX_IMAGE_COUNT }}</span>
                </div>
                <el-button type="primary" round class="send-button" :loading="sending" :disabled="composing || (!draftText.trim() && !pendingImages.size)" @click="submit">发送</el-button>
              </div>
            </div>
          </div>
        </main>
      </section>
    </div>
  </Teleport>
</template>
<script lang="ts" setup>
import { More, Picture, Clock, Close, Loading } from "@element-plus/icons-vue";
import { computed, reactive, ref, onMounted, onBeforeUnmount, watch, nextTick } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { UploadInstance, UploadProps } from "element-plus";
import { getUserById } from "@/api/user";
import { getAllChatRecord, sendMsg, deleteAllChatRecord, clearMessageCount } from "@/api/im";
import { useUserStore } from "@/store/userStore";
import { useImStore } from "@/store/imStore";
import { getRandomString } from "@/utils/util";
import request from "@/utils/request";
import { validateLocalImage } from "@/utils/noteImages";

const imStore = useImStore();
const userStore = useUserStore();
const props = defineProps({ acceptUid: { type: String, default: "" } });
const emit = defineEmits(["clickChat"]);
const dialogRef = ref<HTMLElement | null>(null);
const ChatRef = ref<HTMLElement | null>(null);
const postContent = ref<HTMLTextAreaElement | null>(null);
// 与后端 String.length() 一致，按 UTF-16 长度限制；裁剪时不截断代理对。
const MAX_MESSAGE_LENGTH = 5000;
const MAX_IMAGE_COUNT = 9;
const draftText = ref("");
const composing = ref(false);
const currentUser = ref<any>(userStore.getUserInfo() || {});
const acceptUser = ref<any>({});
const dataList = ref<any[]>([]);
const currentPage = ref(0);
const pageSize = 15;
const messageTotal = ref(0);
const loading = ref(false);
const sending = ref(false);
const emojiPickerVisible = ref(false);
const emojis = ["😀", "😄", "😊", "🥰", "😍", "😂", "🤣", "😎", "🤔", "🥺", "😭", "😅", "👍", "👏", "🙌", "🙏", "❤️", "💖", "🎉", "✨", "🌹", "☀️", "🍀", "🐱"];
const hasMoreHistory = ref(true);
const imageUpload = ref<UploadInstance>();
const pendingImages = reactive(new Map<string, { file: File; preview: string; uploadedUrl?: string }>());
const imageLimitReached = computed(() => pendingImages.size >= MAX_IMAGE_COUNT);
const imagePreviews = computed(() => Array.from(pendingImages, ([id, image]) => ({ id, preview: image.preview, name: image.file.name })));
let editorSelection: { start: number; end: number } | null = null;
let disposed = false;

const rememberEditorSelection = () => {
  const editor = postContent.value;
  if (editor) editorSelection = { start: editor.selectionStart, end: editor.selectionEnd };
};

const limitDraftText = (text: string) => {
  let result = text.slice(0, MAX_MESSAGE_LENGTH);
  if (text.length > MAX_MESSAGE_LENGTH && /[\uD800-\uDBFF]$/.test(result)) result = result.slice(0, -1);
  return result;
};

const syncDraftText = (editor: HTMLTextAreaElement) => {
  const text = limitDraftText(editor.value);
  if (editor.value !== text) editor.value = text;
  draftText.value = text;
  rememberEditorSelection();
};

const handleDraftInput = (event: Event) => {
  if (composing.value || (event as InputEvent).isComposing) return;
  syncDraftText(event.currentTarget as HTMLTextAreaElement);
};

const handleCompositionEnd = (event: CompositionEvent) => {
  composing.value = false;
  syncDraftText(event.currentTarget as HTMLTextAreaElement);
};

const handleEditorKeydown = (event: KeyboardEvent) => {
  if (event.key !== "Enter" || event.shiftKey || event.ctrlKey || event.altKey || event.metaKey
      || event.isComposing || composing.value || event.keyCode === 229) return;
  event.preventDefault();
  void submit();
};

const insertEmoji = async (emoji: string) => {
  if (sending.value || composing.value || !postContent.value) return;
  emojiPickerVisible.value = false;
  await nextTick();
  const editor = postContent.value;
  if (!editor || disposed) return;
  const { start, end } = editorSelection || { start: editor.value.length, end: editor.value.length };
  editor.focus();
  editor.setSelectionRange(start, end);
  if (editor.value.length - (end - start) + emoji.length > MAX_MESSAGE_LENGTH) {
    ElMessage.warning("剩余字数不足，请先删除部分文字");
    return;
  }
  editor.setRangeText(emoji, start, end, "end");
  syncDraftText(editor);
};

const scrollBottom = async () => {
  await nextTick();
  ChatRef.value?.lastElementChild?.scrollIntoView({ block: "end", behavior: "smooth" });
};

watch(() => imStore.message, async (message) => {
  if (message.sendUid === props.acceptUid && message.acceptUid === currentUser.value.id) {
    dataList.value.push({ ...message });
    messageTotal.value++;
    await clearMessageCount(props.acceptUid, 3);
    await scrollBottom();
  }
});

const clearImagePreviews = () => {
  for (const image of pendingImages.values()) URL.revokeObjectURL(image.preview);
  pendingImages.clear();
  imageUpload.value?.clearFiles();
};

const close = () => {
  if (sending.value) return;
  clearImagePreviews();
  draftText.value = "";
  editorSelection = null;
  emit("clickChat", props.acceptUid);
};

const removeImage = (id: string) => {
  if (sending.value) return;
  const image = pendingImages.get(id);
  if (image) URL.revokeObjectURL(image.preview);
  pendingImages.delete(id);
};

const handleChange: UploadProps["onChange"] = (uploadFile) => {
  const file = uploadFile.raw;
  imageUpload.value?.clearFiles();
  if (!file || sending.value || !postContent.value) return;
  if (imageLimitReached.value) {
    ElMessage.warning({ message: `每次最多添加 ${MAX_IMAGE_COUNT} 张图片`, grouping: true });
    return;
  }
  try {
    validateLocalImage(file);
    const id = getRandomString(12);
    const preview = URL.createObjectURL(file);
    pendingImages.set(id, { file, preview });
    postContent.value.focus();
  } catch (error) {
    ElMessage.warning((error as Error).message);
  }
};

const sendMessage = async (message: any, retry = false) => {
  message.isLoading = true;
  message.isFailed = false;
  if (!retry) { dataList.value.push(message); await scrollBottom(); }
  const item = dataList.value.find(row => row.id === message.id) || message;
  try {
    await sendMsg({ acceptUid: message.acceptUid, content: message.content, msgType: message.msgType, chatType: 0 });
    messageTotal.value++;
  } catch { item.isFailed = true; }
  finally { item.isLoading = false; }
};

const submit = async () => {
  if (sending.value || composing.value || !postContent.value || !acceptUser.value.id) return;
  if (pendingImages.size > MAX_IMAGE_COUNT) { ElMessage.warning(`每次最多发送 ${MAX_IMAGE_COUNT} 张图片`); return; }
  const recipient = props.acceptUid;
  const parts: { content: string; msgType: number; imageId?: string }[] = [];
  const text = draftText.value.trim();
  if (text) parts.push({ content: text, msgType: 1 });
  for (const imageId of pendingImages.keys()) parts.push({ content: "", msgType: 2, imageId });
  if (!parts.length) return;
  if (draftText.value.length > MAX_MESSAGE_LENGTH) { ElMessage.warning(`单条消息不能超过 ${MAX_MESSAGE_LENGTH} 字`); return; }
  sending.value = true;
  emojiPickerVisible.value = false;
  try {
    // 发送按钮是上传的唯一入口；失败时保留预览及已上传地址，重试不重复上传。
    for (const part of parts) {
      if (disposed) return;
      if (!part.imageId) continue;
      const image = pendingImages.get(part.imageId)!;
      if (!image.uploadedUrl) {
        const form = new FormData();
        form.append("file", image.file);
        const result = await request<any>({ url: "/web/oss/save", method: "post", data: form });
        if (typeof result.data !== "string" || !result.data) {
          ElMessage.error("图片上传失败，请重试");
          return;
        }
        image.uploadedUrl = result.data;
      }
      part.content = image.uploadedUrl!;
    }
    if (disposed) return;
    draftText.value = "";
    editorSelection = null;
    clearImagePreviews();
    for (const part of parts) {
      if (disposed) return;
      await sendMessage({ content: part.content, msgType: part.msgType, id: getRandomString(12), sendUid: currentUser.value.id, acceptUid: recipient });
    }
  } catch {
    // 请求拦截器显示错误，保留草稿以便重试。
  } finally { sending.value = false; }
};

const loadMoreData = async () => {
  if (loading.value || !hasMoreHistory.value) return;
  loading.value = true;
  const oldHeight = ChatRef.value?.scrollHeight || 0;
  const nextPage = currentPage.value + 1;
  try {
    const { data } = await getAllChatRecord(nextPage, pageSize, props.acceptUid);
    messageTotal.value = data.total;
    dataList.value.unshift(...[...data.records].reverse());
    currentPage.value = nextPage;
    hasMoreHistory.value = nextPage * pageSize < data.total;
    await nextTick();
    if (nextPage === 1) await scrollBottom();
    else if (ChatRef.value) ChatRef.value.scrollTop = ChatRef.value.scrollHeight - oldHeight;
    return data.records.length as number;
  } catch {
    // 请求拦截器显示错误，保留当前页以便再次加载。
  } finally { loading.value = false; }
};
const loadEarlierMessages = async () => {
  if (loading.value) return;
  if (!hasMoreHistory.value) {
    ElMessage.info(messageTotal.value ? "已加载全部聊天记录" : "暂无聊天记录");
    return;
  }
  const count = await loadMoreData();
  if (count === undefined) return;
  if (count > 0) ElMessage.success(`已加载 ${count} 条更早的消息`);
  else ElMessage.info("没有更早的聊天记录了");
};
const showScroll = () => { if (ChatRef.value?.scrollTop === 0) loadMoreData(); };
const clearHistory = async () => {
  try { await ElMessageBox.confirm("清空你与此用户的聊天记录？", "清空聊天记录", { type: "warning" }); }
  catch { return; }
  await deleteAllChatRecord(props.acceptUid);
  dataList.value = [];
  messageTotal.value = 0;
  currentPage.value = 0;
  hasMoreHistory.value = false;
  imStore.setUserList(imStore.userList.filter(item => String(item.uid) !== props.acceptUid));
};

onMounted(async () => {
  dialogRef.value?.focus();
  const { data } = await getUserById(props.acceptUid);
  acceptUser.value = data;
  await clearMessageCount(props.acceptUid, 3);
  await loadMoreData();
});

onBeforeUnmount(() => { disposed = true; clearImagePreviews(); });
</script>
<style lang="less" scoped></style>
