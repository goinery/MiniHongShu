<template>
  <el-sub-menu v-if="item.children.length" :index="item.id" popper-class="workspace-topnav-popper" :teleported="true">
    <template #title>
      <svg-icon v-if="item.icon && item.icon !== '#'" :icon-class="item.icon" />
      <span class="topnav-item-title">{{ item.title }}</span>
    </template>
    <top-nav-item v-for="child in item.children" :key="child.id" :item="child" />
  </el-sub-menu>
  <el-menu-item v-else :index="item.id">
    <svg-icon v-if="item.icon && item.icon !== '#'" :icon-class="item.icon" />
    <span class="topnav-item-title">{{ item.title }}</span>
  </el-menu-item>
</template>

<script setup>
defineProps({ item: { type: Object, required: true } })
</script>