<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    class="community-dialog"
    align-center
    append-to-body
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="relations-body" :aria-busy="loading">
      <el-skeleton v-if="loading" :rows="5" animated />
      <el-empty v-else-if="failed" description="列表加载失败">
        <el-button type="primary" plain @click="loadPage(requestedPage)">重新加载</el-button>
      </el-empty>
      <el-empty v-else-if="!users.length" :description="type === 'following' ? '还没有关注任何人' : '还没有粉丝'" />
      <ul v-else class="relation-list">
        <li v-for="user in users" :key="user.uid" class="relation-row">
          <RouterLink
            class="relation-user"
            :to="{ name: 'user', query: { uid: user.uid } }"
            @click="emit('update:modelValue', false)"
          >
            <el-avatar :size="52" :src="user.avatar">{{ (user.username || '用户').slice(0, 1) }}</el-avatar>
            <div class="relation-info">
              <span class="relation-name">{{ user.username || '用户' }}</span>
              <span class="relation-meta">会员号：{{ user.hsId ?? '—' }} · 粉丝 {{ user.fanCount ?? 0 }}</span>
            </div>
            <el-icon class="relation-arrow"><ArrowRight /></el-icon>
          </RouterLink>
          <el-button
            v-if="String(user.uid) !== currentUid"
            class="relation-follow"
            :class="{ 'followed-button': user.isFollow }"
            type="primary"
            :plain="user.isFollow"
            :loading="pendingUid === String(user.uid)"
            :disabled="Boolean(pendingUid)"
            :aria-busy="pendingUid === String(user.uid)"
            :aria-label="(user.isFollow ? '取消关注' : '关注') + (user.username || '用户')"
            round
            @click.stop="toggleFollow(user)"
          >{{ user.isFollow ? '取消关注' : '关注' }}</el-button>
        </li>
      </ul>
    </div>
    <template v-if="total > pageSize" #footer>
      <el-pagination
        class="relations-pagination"
        :current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        :pager-count="5"
        :disabled="loading || Boolean(pendingUid)"
        layout="prev, pager, next"
        @current-change="loadPage"
      />
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { computed, inject, onBeforeUnmount, ref, watch } from "vue";
import { ArrowRight } from "@element-plus/icons-vue";
import { followById, getUserRelations, type RelationType, type RelationUser } from "@/api/follower";
import { useUserStore } from "@/store/userStore";

const props = defineProps<{ modelValue: boolean; userId: string; type: RelationType }>();
const emit = defineEmits<{
  (e: "update:modelValue", value: boolean): void;
  (e: "change"): void;
}>();
const userStore = useUserStore();
const currentUid = computed(() => String(userStore.getUserInfo()?.id || ""));
const openLogin = inject<() => void>("openLogin", () => {});
const title = computed(() => props.type === "following" ? "关注" : "粉丝");
const users = ref<RelationUser[]>([]);
const currentPage = ref(1);
const requestedPage = ref(1);
const pageSize = 10;
const total = ref(0);
const loading = ref(false);
const failed = ref(false);
const pendingUid = ref("");
// 当前窗口内保留已展示的行，关闭后再从服务端重新获取。
const pageCache = new Map<number, RelationUser[]>();
const unfollowedPages = new Map<string, number>();
let requestVersion = 0;
let disposed = false;

const loadPage = async (page: number): Promise<void> => {
  const version = ++requestVersion;
  requestedPage.value = page;
  loading.value = true;
  failed.value = false;
  try {
    const cached = pageCache.get(page);
    if (cached) {
      users.value = cached;
      currentPage.value = page;
      return;
    }
    // 前面页面的取关行仍占原位置，补偿服务端列表缩短后的偏移，避免翻页漏人。
    const removedBefore = [...unfollowedPages.values()].filter(value => value < page).length;
    const offset = Math.max(0, (page - 1) * pageSize - removedBefore);
    const serverPage = Math.floor(offset / pageSize) + 1;
    const skip = offset % pageSize;
    const response = await getUserRelations(props.userId, props.type, serverPage, pageSize);
    if (version !== requestVersion) return;
    const serverTotal = Number(response.data.total);
    total.value = serverTotal + unfollowedPages.size;
    const lastPage = Math.max(1, Math.ceil(total.value / pageSize));
    if (page > lastPage) {
      await loadPage(lastPage);
      return;
    }
    const records: RelationUser[] = response.data.records.slice(skip);
    if (skip && serverPage * pageSize < serverTotal) {
      const next = await getUserRelations(props.userId, props.type, serverPage + 1, pageSize);
      if (version !== requestVersion) return;
      records.push(...next.data.records.slice(0, pageSize - records.length));
    }
    pageCache.set(page, records);
    users.value = records;
    currentPage.value = page;
  } catch {
    if (version === requestVersion) failed.value = true;
  } finally {
    if (version === requestVersion) loading.value = false;
  }
};

const toggleFollow = async (user: RelationUser) => {
  if (pendingUid.value || loading.value || String(user.uid) === currentUid.value) return;
  if (!userStore.isLogin()) {
    emit("update:modelValue", false);
    openLogin();
    return;
  }
  const version = requestVersion;
  const session = userStore.getSessionVersion();
  const wasFollowing = user.isFollow;
  pendingUid.value = String(user.uid);
  try {
    const response = await followById(user.uid);
    if (disposed || session !== userStore.getSessionVersion()) return;
    const following = response.data === true;
    if (version === requestVersion) {
      user.isFollow = following;
      user.fanCount = Math.max(0, Number(user.fanCount || 0) + Number(following) - Number(wasFollowing));
      if (props.userId === currentUid.value && props.type === "following") {
        if (following) unfollowedPages.delete(String(user.uid));
        else unfollowedPages.set(String(user.uid), currentPage.value);
      }
    }
    emit("change");
    // 同一窗口只更新按钮和数量；请求期间已重开窗口时才刷新新列表。
    if (props.modelValue && version !== requestVersion) {
      pageCache.clear();
      unfollowedPages.clear();
      await loadPage(currentPage.value);
    }
  } catch {
    // 统一请求拦截器提示错误，保留原状态供重试。
  } finally {
    pendingUid.value = "";
  }
};

watch(() => [props.modelValue, props.userId, props.type, userStore.getSessionVersion()], () => {
  requestVersion++;
  pageCache.clear();
  unfollowedPages.clear();
  users.value = [];
  total.value = 0;
  currentPage.value = 1;
  requestedPage.value = 1;
  failed.value = false;
  loading.value = false;
  if (props.modelValue && props.userId) loadPage(1);
}, { immediate: true });

onBeforeUnmount(() => { disposed = true; requestVersion++; });
</script>

<style lang="less" scoped>
.relations-body {
  min-height: 240px;
}

.relation-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 16px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.relation-row {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 8px;
  padding-right: 8px;
}

.relation-user {
  display: flex;
  flex: 1;
  min-width: 0;
  align-items: center;
  gap: 12px;
  padding: 16px 8px;
  border-radius: 12px;
  color: var(--el-text-color-primary);
  text-decoration: none;
  transition: background-color 0.2s;

  &:hover { background: var(--el-fill-color-light); }
  &:focus-visible {
    outline: 2px solid var(--el-color-primary);
    outline-offset: 2px;
  }

  .el-avatar { flex-shrink: 0; }
}

.relation-follow {
  position: relative;
  flex: 0 0 88px;
  width: 88px;
  min-width: 88px;
  height: 32px;
  margin-left: 0;
  padding: 0 12px;
  transition: color 0.15s, background-color 0.15s, border-color 0.15s;

  // 加载图标覆盖在按钮中央，不挤占文字或相邻用户信息的空间。
  &.is-loading :deep(> .el-icon) {
    position: absolute;
    inset: 0;
    margin: auto;
  }

  &.is-loading :deep(> span) {
    visibility: hidden;
    margin-left: 0;
  }
}

.relation-info {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  gap: 8px;
}

.relation-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 16px;
  font-weight: 600;
}

.relation-meta {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  overflow-wrap: anywhere;
}

.relation-arrow { color: var(--el-text-color-placeholder); }
.relations-pagination { justify-content: center; }

@media (max-width: 1100px) {
  .relation-list { grid-template-columns: minmax(0, 1fr); }
}

@media (max-width: 480px) {
  .relation-row { gap: 4px; padding-right: 0; }
  .relation-user { gap: 8px; padding: 12px 4px; }
  .relation-arrow { display: none; }
}
</style>
