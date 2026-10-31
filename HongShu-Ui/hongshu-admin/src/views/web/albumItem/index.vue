<template>
  <div class="app-container business-page" v-loading="loading">
    <BusinessPageHeader :title="album.title || '专辑内容'" :count="rows.length" unit="篇笔记"><el-button icon="Back" @click="router.push('/note/album')">返回专辑</el-button></BusinessPageHeader>
    <div class="toolbar business-toolbar">
      <el-button v-hasPermi="['web:album:edit']" type="primary" @click="openPicker">添加笔记</el-button>
      <el-button v-hasPermi="['web:album:edit']" :disabled="!rows.length || saving" @click="sortItems">按发布时间排序</el-button>
      <el-button v-hasPermi="['web:album:edit']" type="danger" :disabled="!selected.length || saving" @click="removeItems(selected)">移出专辑</el-button>
    </div>
    <el-table class="business-table" :data="rows" row-key="id" @selection-change="selected = $event.map(row => row.id)">
      <el-table-column type="selection" width="50" />
      <el-table-column type="index" label="顺序" width="70" />
      <el-table-column label="封面" width="100"><template #default="{ row }"><image-preview :src="row.noteCover" :width="60" :height="60" /></template></el-table-column>
      <el-table-column prop="title" label="标题" min-width="200" />
      <el-table-column prop="createTime" label="发布时间" width="180" />
      <el-table-column label="审核" width="100"><template #default="{ row }">{{ row.auditStatus === '1' ? '已通过' : row.auditStatus === '2' ? '未通过' : '待审核' }}</template></el-table-column>
      <el-table-column label="操作" width="220"><template #default="{ row, $index }">
        <el-button v-hasPermi="['web:album:edit']" link type="primary" :disabled="$index === 0 || saving" @click="move($index, -1)">上移</el-button>
        <el-button v-hasPermi="['web:album:edit']" link type="primary" :disabled="$index === rows.length - 1 || saving" @click="move($index, 1)">下移</el-button>
        <el-button v-hasPermi="['web:album:edit']" link type="danger" :disabled="saving" @click="removeItems([row.id])">移出</el-button>
      </template></el-table-column>
    </el-table>
    <el-dialog v-model="picker" title="添加笔记" width="760px" destroy-on-close>
      <el-table :data="available" row-key="id" @selection-change="picked = $event.map(row => row.id)">
        <el-table-column type="selection" width="50" :selectable="row => !rows.some(item => item.nid === String(row.id))" />
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="author" label="作者" width="150" />
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="page" v-model:limit="size" @pagination="loadAvailable" />
      <template #footer><el-button @click="picker = false">取消</el-button><el-button type="primary" :disabled="!picked.length" :loading="saving" @click="addItems">添加</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import BusinessPageHeader from "../components/BusinessPageHeader.vue";
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { getSubjectItemList, addSubjectItemList, editSubjectItem, deleteBatchSubjectItem, sortByCreateTime } from '@/api/web/albumItem'

const route = useRoute()
const router = useRouter()
const albumId = computed(() => String(route.query.albumId || ''))
const album = ref({})
const rows = ref([])
const selected = ref([])
const loading = ref(false)
const saving = ref(false)
const picker = ref(false)
const available = ref([])
const picked = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

async function load() {
  if (!albumId.value) return
  loading.value = true
  try {
    const { data } = await getSubjectItemList({ albumId: albumId.value })
    album.value = data.album
    rows.value = data.items
    selected.value = []
  } finally { loading.value = false }
}
async function loadAvailable() {
  const result = await request({ url: '/note/allList', method: 'get', params: { pageNum: page.value, pageSize: size.value } })
  available.value = result.rows
  total.value = result.total
}
async function openPicker() {
  picked.value = []
  page.value = 1
  await loadAvailable()
  picker.value = true
}
async function mutate(action) {
  saving.value = true
  try { await action(); await load(); ElMessage.success('已保存') }
  finally { saving.value = false }
}
async function addItems() {
  await mutate(() => addSubjectItemList({ albumId: albumId.value, noteIds: picked.value }))
  picker.value = false
}
async function move(index, direction) {
  const items = [...rows.value]
  const next = index + direction
  ;[items[index], items[next]] = [items[next], items[index]]
  await mutate(() => editSubjectItem({ albumId: albumId.value, ids: items.map(item => item.id) }))
}
async function sortItems() { await mutate(() => sortByCreateTime({ albumId: albumId.value })) }
async function removeItems(ids) {
  try { await ElMessageBox.confirm('将所选笔记移出此专辑？笔记本身会保留。', '移出专辑', { type: 'warning' }) }
  catch { return }
  await mutate(() => deleteBatchSubjectItem({ albumId: albumId.value, ids }))
}
watch(albumId, load, { immediate: true })
</script>

<style scoped>
/* 组件库内部类由子组件在运行时生成。 */
.toolbar { display: flex; flex-wrap: wrap; gap: 8px; margin: 0 0 16px; }
/*noinspection CssUnusedSymbol*/
.toolbar .el-button + .el-button { margin-left: 0; }
</style>
