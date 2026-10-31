<template>
  <router-view :key="routerKey"></router-view>
</template>

<script setup lang="ts">
import { useRoute } from "vue-router";
import { computed } from "vue";
import { useFeedbackLayer } from "@/constants/layers";
useFeedbackLayer();
const route = useRoute();
const routerKey = computed(() => {
  return route.path + Math.random();
});
</script>

<style scoped></style>
