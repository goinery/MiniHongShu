<template>
  <AuthShell>
    <section class="auth-card">
      <span class="auth-error-code"><svg-icon icon-class="lock" />错误 401</span>
      <h1 class="auth-title">暂时无法访问此页面</h1>
      <p class="auth-description">当前账号没有访问该页面的权限。你可以返回数据中心，或联系管理员开通所需权限。</p>
      <div class="auth-error-actions">
        <el-button type="primary" @click="goHome">返回数据中心</el-button>
        <el-button icon="ArrowLeft" @click="back">返回上一页</el-button>
      </div>
    </section>
  </AuthShell>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'

import AuthShell from "@/components/auth-shell/index.vue";

const route = useRoute();
const router = useRouter();

function goHome() {
  router.push("/dashboard");
}

function back() {
  if (route.query.noGoBack) {
    goHome();
  } else {
    router.go(-1);
  }
}
</script>
