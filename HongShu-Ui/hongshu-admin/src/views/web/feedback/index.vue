<template>
  <div class="app-container business-page">
    <BusinessPageHeader title="用户反馈" :count="total" />
    <el-form class="business-filter" inline>
      <el-form-item label="状态"><el-select v-model="status" placeholder="全部状态" clearable style="width: 160px"><el-option label="待处理" value="0" /><el-option label="已处理" value="1" /></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="Search" @click="page = 1; load()">搜索</el-button></el-form-item>
    </el-form>
    <el-table class="business-table" :data="rows" v-loading="loading">
      <el-table-column prop="username" label="用户" width="140" />
      <el-table-column prop="email" label="联系邮箱" min-width="180" />
      <el-table-column prop="content" label="反馈内容" min-width="250" show-overflow-tooltip />
      <el-table-column prop="createTime" label="提交时间" width="180" />
      <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === '1' ? 'success' : 'warning'">{{ row.status === '1' ? '已处理' : '待处理' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="100"><template #default="{ row }"><el-button link type="primary" @click="open(row)">查看</el-button></template></el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="page" v-model:limit="size" @pagination="load" />
    <el-dialog v-model="visible" title="用户反馈" width="680px">
      <p class="feedback-meta"><strong>{{ form.username }}</strong><span>{{ form.email || '未提供联系邮箱' }}</span></p>
      <p class="content">{{ form.content }}</p>
      <el-form label-width="85px">
        <el-form-item label="处理状态"><el-radio-group v-model="form.status"><el-radio value="0">待处理</el-radio><el-radio value="1">已处理</el-radio></el-radio-group></el-form-item>
        <el-form-item label="处理记录"><el-input v-model="form.resolution" type="textarea" :rows="5" maxlength="2000" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button @click="visible = false">关闭</el-button><el-button v-hasPermi="['web:feedback:edit']" type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import BusinessPageHeader from "../components/BusinessPageHeader.vue";
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
const rows = ref([])
const total = ref(0)
const status = ref('')
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const saving = ref(false)
const visible = ref(false)
const form = ref({})
async function load() {
  loading.value = true
  try {
    const { data } = await request({ url: '/feedback/list', method: 'get', params: { status: status.value, pageNum: page.value, pageSize: size.value } })
    rows.value = data.rows
    total.value = data.total
  } finally { loading.value = false }
}
function open(row) { form.value = { ...row }; visible.value = true }
async function save() {
  saving.value = true
  try {
    await request({ url: '/feedback', method: 'put', data: { id: form.value.id, status: form.value.status, resolution: form.value.resolution } })
    visible.value = false
    ElMessage.success('已保存')
    await load()
  } finally { saving.value = false }
}
onMounted(load)
</script>
<style scoped>
.feedback-meta { display: flex; flex-wrap: wrap; gap: 8px 16px; margin: 0; color: var(--cf-muted); font-size: 13px; }
.feedback-meta strong { color: var(--cf-text); font-weight: 500; }
.content { white-space: pre-wrap; overflow-wrap: anywhere; margin: 18px 0 24px; padding: 16px; border: 1px solid var(--cf-border); border-radius: 6px; background: var(--cf-surface-subtle); line-height: 1.8; max-height: 280px; overflow-y: auto; }
</style>
