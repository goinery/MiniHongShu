<template>
  <AuthShell>
    <section class="auth-card">
      <span class="auth-error-code"><svg-icon icon-class="search" />错误 404</span>
      <h1 class="auth-title">没有找到这个页面</h1>
      <p class="auth-description">页面可能已被移动，或链接地址有误。请检查地址，或返回数据中心继续使用管理控制台。</p>
      <div class="auth-error-actions">
        <el-button type="primary" @click="router.push('/dashboard')">返回数据中心</el-button>
        <el-button icon="ArrowLeft" @click="router.back()">返回上一页</el-button>
      </div>
    </section>
  </AuthShell>
</template>

<script setup>
import { useRouter } from 'vue-router'

import AuthShell from "@/components/auth-shell/index.vue";

const router = useRouter();
</script>
