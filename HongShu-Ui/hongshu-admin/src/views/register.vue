<template>
  <AuthShell>
    <section class="auth-card">
      <p class="auth-eyebrow">{{ adminTitle }}</p>
      <h1 class="auth-title">创建管理账号</h1>
      <el-form ref="registerRef" :model="registerForm" :rules="registerRules" label-position="top" class="auth-form">
        <el-form-item label="账号" prop="username">
          <el-input v-model="registerForm.username" type="text" size="large" autocomplete="username" placeholder="请输入 2–20 位账号">
            <template #prefix><svg-icon icon-class="user" class="input-icon" /></template>
          </el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="registerForm.password" type="password" size="large" autocomplete="new-password" placeholder="请输入 5–20 位密码" show-password @keyup.enter="handleRegister">
            <template #prefix><svg-icon icon-class="password" class="input-icon" /></template>
          </el-input>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" size="large" autocomplete="new-password" placeholder="请再次输入密码" show-password @keyup.enter="handleRegister">
            <template #prefix><svg-icon icon-class="password" class="input-icon" /></template>
          </el-input>
        </el-form-item>
        <el-form-item v-if="captchaEnabled" label="验证码" prop="code">
          <div class="auth-captcha">
            <el-input v-model="registerForm.code" size="large" autocomplete="off" placeholder="请输入验证码" @keyup.enter="handleRegister">
              <template #prefix><svg-icon icon-class="validCode" class="input-icon" /></template>
            </el-input>
            <button type="button" class="auth-captcha__refresh" aria-label="刷新验证码" title="点击刷新验证码" @click="getCode">
              <img v-if="codeUrl" :src="codeUrl" alt="注册验证码" />
              <span v-else>加载中…</span>
            </button>
          </div>
        </el-form-item>
        <el-form-item class="auth-submit-item">
          <el-button :loading="loading" size="large" type="primary" class="auth-submit" @click.prevent="handleRegister">
            {{ loading ? '正在注册…' : '创建账号' }}
          </el-button>
        </el-form-item>
      </el-form>
      <p class="auth-switch">已有账号？<router-link to="/login">返回登录</router-link></p>
    </section>
  </AuthShell>
</template>
<script setup>
import { getCurrentInstance, ref } from 'vue'
import { useRouter } from 'vue-router'

import { adminTitle } from "@/config/brand";
import AuthShell from "@/components/auth-shell/index.vue";
import { ElMessageBox } from "element-plus";
import { getCodeImg, register } from "@/api/login";

const router = useRouter();
const { proxy } = getCurrentInstance();

const registerForm = ref({
  username: "",
  password: "",
  confirmPassword: "",
  code: "",
  uuid: "",
});

const equalToPassword = (rule, value, callback) => {
  if (registerForm.value.password !== value) {
    callback(new Error("两次输入的密码不一致"));
  } else {
    callback();
  }
};

const registerRules = {
  username: [
    { required: true, trigger: "blur", message: "请输入您的账号" },
    {
      min: 2,
      max: 20,
      message: "用户账号长度必须介于 2 和 20 之间",
      trigger: "blur",
    },
  ],
  password: [
    { required: true, trigger: "blur", message: "请输入您的密码" },
    {
      min: 5,
      max: 20,
      message: "用户密码长度必须介于 5 和 20 之间",
      trigger: "blur",
    },
    {
      pattern: /^[^<>"'|\\]+$/,
      message: "不能包含非法字符：< > \" ' \\\ |",
      trigger: "blur",
    },
  ],
  confirmPassword: [
    { required: true, trigger: "blur", message: "请再次输入您的密码" },
    { required: true, validator: equalToPassword, trigger: "blur" },
  ],
  code: [{ required: true, trigger: "change", message: "请输入验证码" }],
};

const codeUrl = ref("");
const loading = ref(false);
const captchaEnabled = ref(true);

function handleRegister() {
  proxy.$refs.registerRef.validate((valid) => {
    if (valid) {
      loading.value = true;
      register(registerForm.value)
        .then(() => {
          const username = registerForm.value.username;
          ElMessageBox.alert(
            "<span style='color: red'>恭喜你，您的账号 " +
              username +
              " 注册成功！</span>",
            "系统提示",
            {
              dangerouslyUseHTMLString: true,
              type: "success",
            }
          )
            .then(() => {
              router.push("/login");
            })
            .catch(() => {});
        })
        .catch(() => {
          loading.value = false;
          if (captchaEnabled) {
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
      registerForm.value.uuid = res.uuid;
    }
  });
}

getCode();
</script>
