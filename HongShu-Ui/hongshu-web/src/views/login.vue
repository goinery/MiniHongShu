<template>
  <div>
    <div class="reds-modal login-modal" role="presentation" @click.self="close">
    <i tabindex="-1" class="reds-mask" aria-label="弹窗遮罩" @click.self="close"></i>
    <div class="login-container">
      <button type="button" class="icon-btn-wrapper close-button round-close" aria-label="关闭登录弹窗" @click="close">
        <Close style="width: 1em; height: 1em" />
      </button>
      <div class="left">
        <div class="login-reason">登录后推荐更懂你的笔记</div>
        <span class="logo" :title="brandName">{{ brandName }}</span>
        <div class="brand-board">
          <p class="brand-title">{{ communityName }}</p>
          <p class="brand-desc">记录生活，发现同好</p>
        </div>
      </div>
      <div class="right">
        <div class="title">账号登录</div>
        <div class="input-container">
          <form @submit.prevent="loginMethod">
            <label class="phone">
              <input
                v-model.trim="userLogin.phone"
                placeholder="请输入账号（手机号）"
                type="text"
                name="account"
                autocomplete="username"
                maxlength="11"
              />
            </label>
            <div style="height: 16px"></div>
            <label class="auth-code">
              <input
                v-model="userLogin.password"
                type="password"
                placeholder="请输入密码"
                autocomplete="current-password"
              />
            </label>
            <div class="err-msg">{{ errMsg }}</div>
            <button class="submit" type="submit" :disabled="submitting">
              {{ submitting ? "登录中..." : "登录" }}
            </button>
          </form>
        </div>
        <div class="agreements">
          <label class="agreement-label">
            <input v-model="agreedToTerms" type="checkbox" class="agreement-checkbox" />
            <span class="checkmark"></span>
          </label>
          <span class="agree-text">
            我已阅读并同意
            <a href="javascript:void(0)" class="agree-link" @click.stop="openAgreement('manual')">《用户协议》</a>
            <a href="javascript:void(0)" class="agree-link" @click.stop="openAgreement('privacy')">《隐私政策》</a>
          </span>
        </div>
        <div class="oauth-tip">
          <span class="oauth-tip-line">新用户可直接登录</span>
        </div>
      </div>
    </div>
    </div>
    <AgreementDialog v-model="agreementShow" :type="agreementType" />
  </div>
</template>

<script lang="ts" setup>
import { Close } from "@element-plus/icons-vue";
import type { UserLogin } from "@/type/user";
import { login } from "@/api/user";
import { ref } from "vue";
import { useUserStore } from "@/store/userStore";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import AgreementDialog from "@/components/AgreementDialog.vue";
import type { AgreementType } from "@/constants/agreements";
import { brandName, communityName } from "@/config/brand";

const userStore = useUserStore();
const router = useRouter();

const agreedToTerms = ref(false);
const submitting = ref(false);
const errMsg = ref("");
const agreementShow = ref(false);
const agreementType = ref<AgreementType>("manual");

const userLogin = ref<UserLogin>({
  phone: "",
  email: "",
  code: "",
  username: "",
  password: "",
});

const emit = defineEmits(["clickChild"]);
const close = () => {
  emit("clickChild", false);
  if (router.currentRoute.value.name === "login") router.replace({ name: "dashboard" });
};

const openAgreement = (type: AgreementType) => {
  agreementType.value = type;
  agreementShow.value = true;
};

const phoneRegex = /^1[3-9]\d{9}$/;

const loginMethod = () => {
  errMsg.value = "";
  if (!agreedToTerms.value) {
    ElMessage.warning("请先阅读并同意用户协议和隐私政策");
    return;
  }
  const phoneNumber = userLogin.value.phone;
  if (!phoneNumber) {
    ElMessage.warning("请输入账号");
    return;
  }
  if (!phoneRegex.test(phoneNumber)) {
    ElMessage.warning("请输入正确的11位手机号");
    return;
  }
  if (!userLogin.value.password) {
    ElMessage.warning("请输入密码");
    return;
  }
  submitting.value = true;
  login(userLogin.value)
    .then((res: any) => {
      const { data } = res;
      userStore.startSession(data);
      router.push({ path: "/", query: { date: Date.now() } });
      emit("clickChild", false);
    })
    .catch((error: any) => {
      console.log(error);
      errMsg.value = "账号或密码不正确";
      ElMessage.error("登录失败");
    })
    .finally(() => {
      submitting.value = false;
    });
};
</script>

<style lang="less" scoped></style>
