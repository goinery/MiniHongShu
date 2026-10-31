<template>
  <div class="app-container console-page druid-page" v-loading="loading">
    <header class="cf-page-header">
      <div><h1>数据库监控</h1></div>
    </header>
    <div class="druid-panel">
      <i-frame v-if="enabled" :src="url" title="数据库监控" appearance="druid" />
      <el-empty v-else-if="!loading" description="数据库监控控制台尚未启用" />
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import iFrame from '@/components/iFrame'
import request from '@/utils/request'
const loading = ref(true)
const enabled = ref(false)
const url = ref(import.meta.env.VITE_APP_BASE_API + '/druid/login.html')
onMounted(async () => {
  try {
    const { data } = await request({ url: '/monitor/druidStatus', method: 'get' })
    enabled.value = data.enabled
  } finally { loading.value = false }
})
</script>

<style scoped>
/* 组件库内部类由子组件在运行时生成。 */
.druid-panel { overflow: hidden; border: 1px solid var(--cf-border); border-radius: 8px; background: var(--cf-surface); }
/*noinspection CssUnusedSymbol*/
.druid-panel :deep(.el-empty) { min-height: 360px; }
.druid-panel :deep(iframe) { display: block; }
</style>
