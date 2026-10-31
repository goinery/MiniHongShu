<template>
  <div class="app-container console-page profile-page">
    <header class="cf-page-header">
      <div><h1>个人中心</h1></div>
    </header>
    <el-row :gutter="20">
      <el-col :lg="8" :md="10" :sm="24" :xs="24">
        <el-card shadow="never" class="profile-card">
          <template #header><span>个人信息</span></template>
          <div class="profile-avatar"><userAvatar /><p>点击头像进行修改</p></div>
          <dl class="profile-details">
            <div><dt><svg-icon icon-class="user" />用户名称</dt><dd>{{ state.user.userName || '—' }}</dd></div>
            <div><dt><svg-icon icon-class="phone" />手机号码</dt><dd>{{ state.user.phonenumber || '—' }}</dd></div>
            <div><dt><svg-icon icon-class="email" />用户邮箱</dt><dd>{{ state.user.email || '—' }}</dd></div>
            <div><dt><svg-icon icon-class="tree" />所属部门</dt><dd><template v-if="state.user.dept">{{ state.user.dept.deptName }}<template v-if="state.postGroup"> / {{ state.postGroup }}</template></template><template v-else>—</template></dd></div>
            <div><dt><svg-icon icon-class="peoples" />所属角色</dt><dd>{{ state.roleGroup || '—' }}</dd></div>
            <div><dt><svg-icon icon-class="date" />创建日期</dt><dd>{{ state.user.createTime || '—' }}</dd></div>
          </dl>
        </el-card>
      </el-col>
      <el-col :lg="16" :md="14" :sm="24" :xs="24">
        <el-card shadow="never" class="profile-card profile-settings">
          <template #header><span>账号设置</span></template>
          <el-tabs v-model="activeTab">
            <el-tab-pane label="基本资料" name="userinfo"><userInfo v-model:user="state.user" /></el-tab-pane>
            <el-tab-pane label="修改密码" name="resetPwd"><resetPwd /></el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
<script setup>
defineOptions({ name: "Profile" })
import { reactive, ref } from 'vue'

import userAvatar from "./userAvatar";
import userInfo from "./userInfo";
import resetPwd from "./resetPwd";
import { getUserProfile } from "@/api/system/user";

const activeTab = ref("userinfo");
const state = reactive({
  user: {},
  roleGroup: {},
  postGroup: {}
});

function getUser() {
  getUserProfile().then(response => {
    state.user = response.data;
    state.roleGroup = response.roleGroup;
    state.postGroup = response.postGroup;
  });
}

getUser();
</script>

<style lang="scss" scoped>
.profile-card { margin-bottom: 20px; border: 1px solid var(--cf-border); border-radius: 8px; box-shadow: none; }
.profile-card :deep(.el-card__header) { padding: 16px 20px; color: var(--cf-text); font-size: 14px; font-weight: 600; border-bottom: 1px solid var(--cf-border); }
.profile-avatar { padding: 8px 0 20px; text-align: center; }
.profile-avatar p { margin: 10px 0 0; font-size: 12px; color: var(--cf-muted); }
.profile-details { margin: 0; font-size: 13px; }
.profile-details > div { padding: 14px 0; border-top: 1px solid var(--cf-border); }
.profile-details dt { display: flex; align-items: center; gap: 8px; color: var(--cf-muted); }
.profile-details dd { margin: 7px 0 0 22px; color: var(--cf-text); line-height: 1.5; overflow-wrap: anywhere; }
.profile-settings :deep(.el-tabs__header) { margin-bottom: 26px; }
.profile-settings :deep(.el-form) { max-width: 580px; padding: 6px 0; }
.profile-settings :deep(.el-form-item__label) { font-weight: 500; color: var(--cf-muted); }
@media (max-width: 767px) { .profile-card :deep(.el-card__body) { padding: 18px; } }
</style>
