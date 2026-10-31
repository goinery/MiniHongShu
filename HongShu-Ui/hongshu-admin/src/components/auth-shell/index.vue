<template>
  <div class="auth-shell">
    <header class="auth-shell__header">
      <router-link to="/" class="auth-shell__brand" :aria-label="adminTitle + '首页'" :title="consoleTitle">
        <span class="auth-shell__brand-mark" aria-hidden="true">{{ brandMonogram }}</span>
        <span class="auth-shell__brand-name">{{ brandName }}</span>
        <span class="auth-shell__divider" aria-hidden="true"></span>
        <span>管理控制台</span>
      </router-link>
      <span class="auth-shell__header-note">{{ communityName }}</span>
    </header>
    <main class="auth-shell__main">
      <div class="auth-shell__content">
        <slot />
      </div>
    </main>
    <footer class="auth-shell__footer">
      <span>Copyright © {{ communityName }}</span>
      <span>{{ consoleTitle }}</span>
    </footer>
  </div>
</template>

<script setup>
import { adminTitle, brandName, brandMonogram, communityName, consoleTitle } from '@/config/brand'
</script>

<style lang="scss" scoped>
.auth-shell {
  --el-border-radius-base: 6px;
  min-height: 100vh;
  @supports (min-height: 100dvh) { min-height: 100dvh; }
  display: flex;
  flex-direction: column;
  background: var(--cf-background);
  color: var(--cf-text);

  &__header {
    min-height: 64px;
    padding: 14px 32px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20px;
    border-bottom: 1px solid var(--cf-border);
    background: var(--cf-surface);
  }

  &__brand {
    display: inline-flex;
    align-items: center;
    gap: 16px;
    color: var(--cf-text);
    font-size: 14px;
    font-weight: 500;

  }

  &__brand-mark { display: grid; place-items: center; width: 30px; height: 30px; border-radius: 6px; background: var(--el-color-primary-light-9); color: var(--el-color-primary); font-size: 22px; font-weight: 700; }
  &__brand-name { margin-left: -10px; color: var(--el-color-primary); font-size: 18px; font-weight: 600; }
  &__divider { width: 1px; height: 20px; background: var(--cf-border); }
  &__header-note { color: var(--cf-muted); font-size: 12px; }

  &__main {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 56px 24px;
  }

  &__content { width: 100%; max-width: 440px; }

  &__footer {
    min-height: 56px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    padding: 16px 32px;
    border-top: 1px solid var(--cf-border);
    color: var(--cf-muted);
    font-size: 12px;
  }

  :deep(.auth-card) {
    padding: 32px;
    border: 1px solid var(--cf-border);
    border-radius: 8px;
    background: var(--cf-surface);
    box-shadow: var(--el-box-shadow-lighter);
  }

  :deep(.auth-eyebrow) {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 0 0 14px;
    color: var(--cf-muted);
    font-size: 12px;
    font-weight: 500;

    &::before {
      content: '';
      width: 7px;
      height: 7px;
      border-radius: 2px;
      background: var(--el-color-primary);
    }
  }

  :deep(.auth-title) {
    margin: 0;
    color: var(--cf-text);
    font-size: 25px;
    font-weight: 600;
    letter-spacing: -.5px;
    line-height: 1.4;
  }

  :deep(.auth-description) {
    margin: 10px 0 26px;
    color: var(--cf-muted);
    font-size: 13px;
    line-height: 1.8;
  }

  :deep(.auth-form) { margin-top: 26px; }
  :deep(.el-form-item) { margin-bottom: 22px; }
  :deep(.el-form-item__label) { color: var(--cf-text); font-size: 13px; font-weight: 500; line-height: 22px; padding: 0; margin-bottom: 7px; }
  :deep(.el-input__wrapper) { min-height: 42px; border-radius: 6px; background: var(--cf-surface); box-shadow: 0 0 0 1px var(--el-border-color) inset; }
  :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px var(--el-color-primary) inset; }
  :deep(.el-input__inner) { font-size: 14px; }
  :deep(.input-icon) { width: 14px; height: 16px; color: var(--cf-muted); }
  :deep(.auth-submit) { width: 100%; height: 42px; font-size: 14px; font-weight: 500; border-radius: 6px; }
  :deep(.auth-submit-item) { margin-bottom: 0; }
  :deep(.auth-remember) { margin: -4px 0 18px; }
  :deep(.el-checkbox__label) { font-size: 13px; color: var(--cf-muted); }

  :deep(.auth-captcha) { display: flex; gap: 12px; width: 100%; align-items: center; }
  :deep(.auth-captcha .el-input) { flex: 1; min-width: 0; }
  :deep(.auth-captcha__refresh) {
    display: flex;
    align-items: center;
    justify-content: center;
    flex: 0 0 116px;
    height: 42px;
    padding: 0;
    border: 1px solid var(--el-border-color);
    border-radius: 6px;
    background: var(--cf-surface-subtle);
    cursor: pointer;
    overflow: hidden;

    &:focus-visible { background: var(--cf-hover); }
    img { max-width: 100%; height: 40px; object-fit: contain; }
  }

  :deep(.auth-switch) {
    margin: 24px 0 0;
    padding-top: 20px;
    border-top: 1px solid var(--cf-border);
    text-align: center;
    color: var(--cf-muted);
    font-size: 13px;

    a { color: var(--el-color-primary); margin-left: 6px; }
    a:hover { text-decoration: underline; }
  }

  :deep(.auth-error-code) {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 24px;
    padding: 7px 10px;
    border: 1px solid var(--el-color-warning-light-7);
    border-radius: 5px;
    background: var(--el-color-warning-light-9);
    color: var(--el-color-warning-dark-2);
    font-size: 12px;
    font-weight: 600;
  }

  :deep(.auth-error-actions) { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; }
  :deep(.auth-error-actions .el-button) { height: 36px; margin: 0; }

  @media (max-width: 520px) {
    &__header { padding: 14px 20px; }
    &__header-note { display: none; }
    &__main { padding: 32px 16px; }
    &__footer { padding: 16px 20px; }
    :deep(.auth-card) { padding: 26px 22px; }
    :deep(.auth-title) { font-size: 23px; }
  }
}
</style>
