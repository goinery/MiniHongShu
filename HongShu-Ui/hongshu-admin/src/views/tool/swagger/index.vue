<template>
   <div class="app-container console-page swagger-page">
      <header class="cf-page-header">
         <div><h1>接口文档</h1></div>
      </header>
      <div class="swagger-panel"><i-frame :src="url" title="接口文档" appearance="swagger" /></div>
   </div>
</template>

<script setup>
import { ref } from 'vue'
import iFrame from '@/components/iFrame'

const base = import.meta.env.VITE_APP_BASE_API
const url = ref(base + "/swagger-ui/index.html?configUrl=" + encodeURIComponent(base + "/v3/api-docs/swagger-config"))
</script>

<style scoped>
.swagger-panel { overflow: hidden; border: 1px solid var(--cf-border); border-radius: 8px; background: var(--cf-surface); }
.swagger-panel :deep(iframe) { display: block; }
</style>
