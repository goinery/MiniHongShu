<template></template>

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

<style lang="less" scoped></style>
