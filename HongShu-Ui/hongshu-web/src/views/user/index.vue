<template>
  <el-empty v-if="!uid" description="登录后查看个人主页"><el-button type="primary" @click="openLogin">登录</el-button></el-empty>
  <div v-else class="user-page">
    <div class="user">
      <div class="user-info" :class="{ 'is-own-profile': isOwnProfile }">
        <button type="button" class="avatar-preview-button" aria-label="放大查看头像" :disabled="!userInfo.avatar" @click="avatarPreviewShow = true">
          <img :src="userInfo.avatar" class="user-image" alt="用户头像" />
        </button>
        <div class="info-part">
          <div class="profile-heading">
            <div class="basic-info">
              <h1 class="user-name" :title="userInfo.username">{{ userInfo.username }}</h1>
              <div class="user-content">
                <span class="user-redId">会员号：{{ userInfo.hsId ?? '—' }}</span>
                <span v-if="!isOwnProfile" :title="userInfo.address">IP属地：{{ userInfo.address || '未知' }}</span>
              </div>
            </div>
            <div class="profile-actions">
              <template v-if="isOwnProfile">
                <el-button class="edit-profile-button" type="primary" plain round @click="editShow = true">编辑资料</el-button>
                <UserMoreInfo :user-info="userInfo" />
              </template>
              <template v-else>
                <el-button class="follow-button" :class="{ 'followed-button': _isFollow }" type="primary" :plain="_isFollow" round :aria-label="(_isFollow ? '取消关注' : '关注') + (userInfo.username || '用户')" :aria-pressed="_isFollow" @click="follow(uid, _isFollow ? 1 : 0)">
                  {{ _isFollow ? '取消关注' : '关注' }}
                </el-button>
                <el-button class="message-button" :icon="ChatLineRound" circle aria-label="发私信" @click="openChat" />
              </template>
            </div>
          </div>
          <div class="user-desc" :title="userInfo.description || '还没有简介'">{{ userInfo.description || '还没有简介' }}</div>
          <div class="user-tags">
            <el-tag v-if="!isOwnProfile && (profileGender === '0' || profileGender === '1')" class="gender-badge" :class="{ 'is-female': profileGender === '1' }" type="info" effect="light" round size="small" :title="profileGender === '0' ? '男' : '女'" :aria-label="profileGender === '0' ? '男' : '女'">
              <el-icon><Male v-if="profileGender === '0'" /><Female v-else /></el-icon>
            </el-tag>
            <el-tag v-for="tag in tagList" :key="tag" type="info" effect="light" round size="small" :title="tag">{{ tag }}</el-tag>
          </div>
          <div class="user-interactions">
            <button type="button" class="stat-button" aria-label="查看关注列表" aria-haspopup="dialog" @click="openRelations('following')">
              <span class="count">{{ userInfo.followerCount ?? 0 }}</span><span class="shows">关注</span>
            </button>
            <button type="button" class="stat-button" aria-label="查看粉丝列表" aria-haspopup="dialog" @click="openRelations('fans')">
              <span class="count">{{ userInfo.fanCount ?? 0 }}</span><span class="shows">粉丝</span>
            </button>
            <button type="button" class="stat-button" aria-label="查看作品" @click="openWorks">
              <span class="count">{{ userInfo.trendCount ?? 0 }}</span><span class="shows">作品</span>
            </button>
          </div>
        </div>
      </div>
    </div>
    <div ref="userTabs" class="user-tabs">
      <button ref="worksTab" type="button" :class="['tab-item', { active: type == 1 }]" :aria-pressed="type == 1" @click="toPage(1)">笔记</button>
      <button type="button" :class="['tab-item', { active: type == 2 }]" :aria-pressed="type == 2" @click="toPage(2)">点赞</button>
      <button type="button" :class="['tab-item', { active: type == 3 }]" :aria-pressed="type == 3" @click="toPage(3)">收藏</button>
    </div>
    <div class="feeds-tab-container">
      <Chat
        v-if="chatShow"
        :acceptUid="uid"
        @click-chat="chatShow = false"
      >
      </Chat>
      <Note v-if="userInfo.id" :key="uid" :type="type"> </Note>
    </div>
    <UserEdit v-model="editShow" :user-info="userInfo" @success="initData" />
    <UserRelations v-model="relationsShow" :user-id="uid" :type="relationType" @change="initData" />
    <el-image-viewer
      v-if="avatarPreviewShow && userInfo.avatar"
      :url-list="[userInfo.avatar]"
      teleported
      hide-on-click-modal
      @close="avatarPreviewShow = false"
    />
  </div>
</template>

<script lang="ts" setup>
import { ChatLineRound, Female, Male } from "@element-plus/icons-vue";
import { ref, computed, inject, nextTick, watch } from "vue";
import { getUserById } from "@/api/user";
import Note from "@/components/Note.vue";
import { useUserStore } from "@/store/userStore";
import Chat from "@/components/Chat.vue";
import { followById, isFollow, type RelationType } from "@/api/follower";
import { useRoute, useRouter } from "vue-router";
import UserEdit from "./user-edit.vue";
import UserRelations from "./user-relations.vue";
import UserMoreInfo from "./user-more-info.vue";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const currentUid = computed(() => userStore.getUserInfo()?.id);
const userInfo = ref<any>({});
const uid = computed(() => String(route.query.uid || currentUid.value || ""));
const isOwnProfile = computed(() => uid.value === currentUid.value);
const openLogin = inject<() => void>("openLogin", () => {});
const openChat = () => { if (!userStore.isLogin()) { openLogin(); return; } chatShow.value = true; };
const type = ref(1);
const chatShow = ref(false);
const editShow = ref(false);
const avatarPreviewShow = ref(false);
const relationsShow = ref(false);
const relationType = ref<RelationType>("following");
const userTabs = ref<HTMLElement>();
const worksTab = ref<HTMLButtonElement>();
const _isFollow = ref(false);
const tagList = ref<string[]>([]);
const profileGender = computed(() => String(userInfo.value.gender ?? "2"));

const toPage = (val: number) => {
  type.value = val;
};

const openWorks = async () => {
  toPage(1);
  await nextTick();
  worksTab.value?.focus({ preventScroll: true });
  userTabs.value?.scrollIntoView({ behavior: "smooth", block: "start" });
};

const openRelations = (value: RelationType) => {
  relationType.value = value;
  relationsShow.value = true;
};

const follow = (fid: string, type: number) => {
  if (!userStore.isLogin()) { openLogin(); return; }
  followById(fid).then(() => {
    _isFollow.value = type == 0;
  });
};

const initData = () => {
  const userId = uid.value;
  const sessionVersion = userStore.getSessionVersion();
  if (!userId) return;
  getUserById(userId).then((res) => {
    if (userId !== uid.value || route.name !== "user" || sessionVersion !== userStore.getSessionVersion()) return;
    userInfo.value = res.data;
    try {
      tagList.value = res.data.tags ? JSON.parse(res.data.tags) : [];
    } catch {
      tagList.value = [];
    }
    if (userStore.isLogin()) {
      isFollow(userId).then((res) => {
        if (userId === uid.value && route.name === "user") _isFollow.value = res.data;
      }).catch(() => {});
    } else _isFollow.value = false;
  }).catch((error) => {
    if (userId !== uid.value || route.name !== "user") return;
    if (error?.code === 404 || error?.response?.status === 404) {
      void router.replace("/dashboard");
    }
  });
};

watch([uid, currentUid], () => {
  type.value = 1;
  chatShow.value = false;
  editShow.value = false;
  avatarPreviewShow.value = false;
  relationsShow.value = false;
  userInfo.value = {};
  tagList.value = [];
  _isFollow.value = false;
  initData();
}, { immediate: true });
</script>

<style lang="less" scoped>
.user-page {
  --profile-avatar-size: 176px;
  background: #fff;
  min-height: 100vh;
}
.user {
  display: flex;
  justify-content: center;
  padding-top: 4.5rem;
}
.user-info {
  display: grid;
  grid-template-columns: var(--profile-avatar-size) minmax(0, 1fr);
  align-items: start;
  column-gap: 40px;
  width: min(750px, 100%);
  box-sizing: border-box;
  padding: 32px 24px 28px;
}
.avatar-preview-button {
  width: var(--profile-avatar-size);
  height: var(--profile-avatar-size);
  padding: 0;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 50%;
  background: transparent;
  overflow: hidden;
  cursor: zoom-in;
  &:disabled { cursor: default; }
  &:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 4px; }
}
.user-image { display: block; width: 100%; height: 100%; object-fit: cover; }
.info-part {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
  height: var(--profile-avatar-size);
}
.profile-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; }
.basic-info { min-width: 0; }
.user-name {
  margin: 0;
  color: var(--el-text-color-primary);
  font-size: 24px;
  line-height: 30px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.user-content {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 16px;
  white-space: nowrap;
  > span { overflow: hidden; text-overflow: ellipsis; }
  .user-redId { flex-shrink: 0; }
}
.profile-actions {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 8px;
  .el-button { margin-left: 0; }
  .follow-button {
    min-width: 96px;
    height: 36px;
    font-weight: 600;
  }
  .message-button { width: 36px; height: 36px; background: transparent; }
}
.user-desc {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  flex-shrink: 0;
  max-height: 40px;
  overflow: hidden;
  white-space: pre-line;
  overflow-wrap: anywhere;
  color: var(--el-text-color-primary);
  font-size: 14px;
  line-height: 20px;
}
.user-tags {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 6px;
  height: 24px;
  min-width: 0;
  overflow: hidden;
  .el-tag { min-width: 0; border: 0; }
  :deep(.el-tag__content) { display: inline-flex; align-items: center; overflow: hidden; text-overflow: ellipsis; }
  .gender-badge {
    flex-shrink: 0;
    color: #409eff;
    background: rgba(64, 158, 255, 0.14);
    &.is-female { color: #ff6699; background: rgba(255, 102, 153, 0.14); }
  }
}
.user-interactions { display: flex; align-items: center; gap: 20px; margin-top: auto; }
.stat-button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  padding: 2px 0;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: var(--el-text-color-primary);
  font: inherit;
  font-size: 14px;
  line-height: 20px;
  cursor: pointer;
  .count { font-weight: 500; }
  .shows { color: var(--el-text-color-secondary); }
  &:hover, &:hover .shows { color: var(--el-color-primary); }
  &:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 4px; }
}
.user-tabs {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  padding: 8px 0 16px;
  scroll-margin-top: 88px;
  .tab-item {
    height: 36px;
    padding: 0 16px;
    border-radius: 999px;
    font-size: 15px;
    color: var(--el-text-color-secondary);
    cursor: pointer;
    &:hover { background: var(--el-fill-color-light); }
    &:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 3px; }
    &.active { color: var(--el-color-primary); background: var(--el-color-primary-light-9); font-weight: 600; }
  }
}
.feeds-tab-container { padding-left: 2rem; }
@media (max-width: 1000px) {
  .user-info { column-gap: 32px; }
  .profile-heading { gap: 12px; }
  .profile-actions .follow-button { min-width: 80px; }
}
@media (max-width: 760px) {
  .user-info { grid-template-columns: 100px minmax(0, 1fr); gap: 24px; padding: 24px 20px; }
  .avatar-preview-button { width: 100px; height: 100px; }
  .info-part { height: auto; min-height: var(--profile-avatar-size); }
  .profile-heading { flex-wrap: wrap; }
  .profile-actions { flex-wrap: wrap; }
  .user-content { flex-wrap: wrap; gap: 4px 12px; }
}
@media (max-width: 480px) {
  .user-info { grid-template-columns: 72px minmax(0, 1fr); gap: 16px; padding: 20px 12px; }
  .avatar-preview-button { width: 72px; height: 72px; }
  .user-name { font-size: 22px; }
  .user-interactions { gap: 12px; }
  .stat-button { font-size: 13px; }
}
</style>
