<template>
  <AuthShell>
    <section class="auth-card">
      <p class="auth-eyebrow">{{ adminTitle }}</p>
      <h1 class="auth-title">登录管理控制台</h1>
      <el-form ref="loginRef" :model="loginForm" :rules="loginRules" label-position="top" class="auth-form">
        <el-form-item label="账号" prop="username">
          <el-input v-model="loginForm.username" type="text" size="large" autocomplete="username" placeholder="请输入账号" @keyup.enter="handleLogin">
            <template #prefix><svg-icon icon-class="user" class="input-icon" /></template>
          </el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="loginForm.password" type="password" size="large" autocomplete="current-password" placeholder="请输入密码" show-password @keyup.enter="handleLogin">
            <template #prefix><svg-icon icon-class="password" class="input-icon" /></template>
          </el-input>
        </el-form-item>
        <el-form-item v-if="captchaEnabled" label="验证码" prop="code">
          <div class="auth-captcha">
            <el-input v-model="loginForm.code" size="large" autocomplete="off" placeholder="请输入验证码" @keyup.enter="handleLogin">
              <template #prefix><svg-icon icon-class="validCode" class="input-icon" /></template>
            </el-input>
            <button type="button" class="auth-captcha__refresh" aria-label="刷新验证码" title="点击刷新验证码" @click="getCode">
              <img v-if="codeUrl" :src="codeUrl" alt="登录验证码" />
              <span v-else>加载中…</span>
            </button>
          </div>
        </el-form-item>
        <el-checkbox v-model="loginForm.rememberMe" class="auth-remember">记住密码</el-checkbox>
        <el-form-item class="auth-submit-item">
          <el-button :loading="loading" size="large" type="primary" class="auth-submit" @click.prevent="handleLogin">
            {{ loading ? '正在登录…' : '登录' }}
          </el-button>
        </el-form-item>
      </el-form>
      <p v-if="register" class="auth-switch">还没有账号？<router-link to="/register">立即注册</router-link></p>
    </section>
  </AuthShell>
</template>
<script setup>
import { getCurrentInstance, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { adminTitle } from "@/config/brand";
import AuthShell from "@/components/auth-shell/index.vue";
import { getCodeImg } from "@/api/login";
import Cookies from "js-cookie";
import { encrypt, decrypt } from "@/utils/jsencrypt";
import useUserStore from "@/store/modules/user";

const userStore = useUserStore();
const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance();

const loginForm = ref({
  username: "",
  password: "",
  rememberMe: false,
  code: "",
  uuid: "",
});

const loginRules = {
  username: [{ required: true, trigger: "blur", message: "请输入您的账号" }],
  password: [{ required: true, trigger: "blur", message: "请输入您的密码" }],
  code: [{ required: true, trigger: "change", message: "请输入验证码" }],
};

const codeUrl = ref("");
const loading = ref(false);
// 验证码开关
const captchaEnabled = ref(true);
// 注册开关
const register = ref(false);
const redirect = ref(undefined);

watch(
  route,
  (newRoute) => {
    redirect.value = newRoute.query && newRoute.query.redirect;
  },
  { immediate: true }
);

function handleLogin() {
  proxy.$refs.loginRef.validate((valid) => {
    if (valid) {
      loading.value = true;
      // 勾选了需要记住密码设置在 cookie 中设置记住用户名和密码
      if (loginForm.value.rememberMe) {
        Cookies.set("username", loginForm.value.username, { expires: 30 });
        Cookies.set("password", encrypt(loginForm.value.password), {
          expires: 30,
        });
        Cookies.set("rememberMe", loginForm.value.rememberMe, { expires: 30 });
      } else {
        // 否则移除
        Cookies.remove("username");
        Cookies.remove("password");
        Cookies.remove("rememberMe");
      }
      // 调用action的登录方法
      userStore
        .login(loginForm.value)
        .then(() => {
          const query = route.query;
          const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
            if (cur !== "redirect") {
              acc[cur] = query[cur];
            }
            return acc;
          }, {});
          const target =
            redirect.value && redirect.value !== "/" && redirect.value !== "/index"
              ? redirect.value
              : "/dashboard";
          router.push({ path: target, query: otherQueryParams });
        })
        .catch(() => {
          loading.value = false;
          // 重新获取验证码
          if (captchaEnabled.value) {
            getCode();
          }
        });
    }
  });
}

function getCode() {
  getCodeImg().then((res) => {
    captchaEnabled.value =
      res.captchaEnabled === undefined ? true : res.captchaEnabled;
    if (captchaEnabled.value) {
      codeUrl.value = "data:image/gif;base64," + res.img;
      loginForm.value.uuid = res.uuid;
    }
  });
}

function getCookie() {
  const username = Cookies.get("username");
  const password = Cookies.get("password");
  const rememberMe = Cookies.get("rememberMe");
  loginForm.value = {
    ...loginForm.value,
    username: username === undefined ? loginForm.value.username : username,
    password:
      password === undefined ? loginForm.value.password : (decrypt(password) || ""),
    rememberMe: rememberMe === undefined ? false : rememberMe === "true",
  };
}

getCode();
getCookie();
</script>
