<template>
  <div id="table" class="app-container business-page calendar-list-container">
    <BusinessPageHeader title="专辑管理" :count="total" unit="个专辑" />
    <div class="album-controls">
      <div class="album-search">
        <el-input v-model="keyword" clearable placeholder="搜索专辑名称" aria-label="专辑名称" @keyup.enter="handleFind" />
        <el-button type="primary" icon="Search" @click="handleFind">搜索</el-button>
      </div>
      <el-button v-hasPermi="['web:album:add']" type="primary" icon="Plus" @click="handleAdd">新建专辑</el-button>
    </div>
    <div class="album-selection-bar">
      <div class="album-selection-actions">
        <el-button :disabled="!tableData.length" @click="checkAll()">{{ chooseTitle }}</el-button>
        <span>已选择 <strong>{{ selectUids.length }}</strong> 个专辑</span>
      </div>
      <el-button v-hasPermi="['web:album:remove']" type="danger" plain icon="Delete" :disabled="!selectUids.length" @click="handleDeleteBatch">删除选中</el-button>
    </div>
    <div v-if="tableData.length" class="album-grid">
      <article v-for="item in tableData" :key="item.id" class="album-card" :class="{ 'is-selected': selectUids.includes(item.id) }">
        <div class="album-card-heading">
          <label class="album-checkbox" :for="'album-' + item.id">
            <input type="checkbox" :id="'album-' + item.id" :aria-label="'选择专辑 ' + item.title" :checked="selectUids.includes(item.id)" @change="checked(item)" />
            <span>专辑</span>
          </label>
          <span class="album-id">#{{ item.id }}</span>
        </div>
        <button type="button" class="album-cover" :aria-label="'查看专辑 ' + item.title" @click="goSubjectItem(item)">
          <el-image v-if="item.albumCover" :src="item.albumCover" fit="cover" :alt="item.title">
            <template #error><span class="album-cover-placeholder">封面暂不可用</span></template>
          </el-image>
          <span v-else class="album-cover-placeholder">暂无封面</span>
        </button>
        <div class="album-card-body">
          <button type="button" class="album-title" @click="goSubjectItem(item)" :title="item.title">{{ item.title || '未命名专辑' }}</button>
          <p>展示顺序 <strong>{{ item.sort ?? 0 }}</strong></p>
        </div>
        <div class="album-card-actions">
          <el-button v-hasPermi="['web:album:query']" link type="primary" @click="goSubjectItem(item)">查看内容</el-button>
          <div>
            <el-button v-hasPermi="['web:album:edit']" link type="primary" @click="handleEdit(item)">编辑</el-button>
            <el-button v-hasPermi="['web:album:remove']" link type="danger" @click="handleDelete(item)">删除</el-button>
          </div>
        </div>
      </article>
    </div>
    <div v-else class="album-empty">
      <strong>暂无专辑</strong>
      <p>调整搜索条件，或新建专辑整理内容。</p>
    </div>
    <!--分页-->
    <div class="block">
      <el-pagination
        @current-change="handleCurrentChange"
        :current-page="queryParams.pageNum"
        :page-size="queryParams.pageSize"
        layout="total, prev, pager, next, jumper"
        :total="total"
      >
      </el-pagination>
    </div>

    <!-- 添加或修改对话框 -->
    <el-dialog :title="title" v-model="dialogFormVisible" destroy-on-close :close-on-click-modal="!saving" :close-on-press-escape="!saving" :show-close="!saving">
      <el-form :model="form" :rules="rules" ref="albumFormRef" :disabled="saving">
        <el-form-item
          label="封面图"
          :label-width="formLabelWidth"
          prop="albumCover"
        >
          <image-upload ref="coverUpload" v-model="form.albumCover" :limit="1" :disabled="saving" />
        </el-form-item>

        <el-form-item label="专辑名" :label-width="formLabelWidth" prop="title">
          <el-input v-model="form.title" auto-complete="off"></el-input>
        </el-form-item>

        <el-form-item label="用户ID" :label-width="formLabelWidth">
          <el-input v-model="form.uid" auto-complete="off"></el-input>
        </el-form-item>

        <el-form-item label="排序" :label-width="formLabelWidth" prop="sort">
          <el-input v-model="form.sort" auto-complete="off"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="saving" @click="dialogFormVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { toRefs } from 'vue'

import BusinessPageHeader from "../components/BusinessPageHeader.vue";
import { ref, reactive, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessageBox, ElMessage } from "element-plus";
import {
  getSubjectList,
  addSubject,
  editSubject,
  deleteBatchSubject,
} from "@/api/web/album";

const router = useRouter();
const tableData = ref([]);
const total = ref(0);
const title = ref("");
const keyword = ref("");
const chooseTitle = ref("全选");
const isCheckedAll = ref(false);
const selectUids = ref([]);
const formLabelWidth = ref("120px");
const dialogFormVisible = ref(false);
const isEditForm = ref(false);
const albumFormRef = ref();
const coverUpload = ref();
const saving = ref(false);

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    title: undefined,
  },
  rules: {
    albumCover: [{ required: true, message: "封面图片不能为空", trigger: "blur" }],
    title: [
      { required: true, message: "专辑名不能为空", trigger: "blur" },
      { min: 1, max: 20, message: "长度在1到20个字符" },
    ],
    sort: [
      { required: true, message: "排序字段不能为空", trigger: "blur" },
      { pattern: /^[0-9]\d*$/, message: "排序字段只能为自然数" },
    ],
  },
});
const { queryParams, form, rules } = toRefs(data);

function subjectList() {
  getSubjectList(queryParams.value).then((response) => {
    tableData.value = response.rows;
    total.value = response.total;
  });
}

function handleFind() {
  queryParams.value.title = keyword.value;
  queryParams.value.pageNum = 1;
  subjectList();
}

function getFormObject() {
  return {
    id: null,
    title: null,
    uid: null,
    albumCover: null,
    sort: 0,
  };
}

function checked(data) {
  const idIndex = selectUids.value.indexOf(data.id);
  if (idIndex >= 0) {
    selectUids.value.splice(idIndex, 1);
  } else {
    selectUids.value.push(data.id);
  }
}

function checkAll() {
  if (isCheckedAll.value) {
    selectUids.value = [];
    isCheckedAll.value = false;
    chooseTitle.value = "全选";
  } else {
    selectUids.value = [];
    tableData.value.forEach((album) => {
      selectUids.value.push(album.id);
    });
    isCheckedAll.value = true;
    chooseTitle.value = "取消全选";
  }
}

function handleCurrentChange(val) {
  queryParams.value.pageNum = val;
  subjectList();
}

function handleAdd() {
  title.value = "增加专辑";
  dialogFormVisible.value = true;
  Object.assign(form.value, getFormObject());
  isEditForm.value = false;
}

function handleEdit(row) {
  title.value = "编辑专辑";
  dialogFormVisible.value = true;
  isEditForm.value = true;
  Object.assign(form.value, row);
}

function goSubjectItem(row) {
  router.push({
    path: "/note/albumItem",
    query: { albumId: row.id },
  });
}

function handleDelete(row) {
  ElMessageBox.confirm("此操作将会把该专辑删除, 是否继续?", "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(() => {
      return deleteBatchSubject(row.id);
    })
    .then(() => {
      ElMessage.success("删除成功");
      subjectList();
    })
    .catch(() => {});
}

function handleDeleteBatch() {
  if (selectUids.value.length <= 0) {
    ElMessage.error("请先选中需要删除的内容！");
    return;
  }
  ElMessageBox.confirm("此操作将把选中的专辑删除, 是否继续?", "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(() => {
      return deleteBatchSubject(selectUids.value.join(","));
    })
    .then(() => {
      ElMessage.success("删除成功");
      selectUids.value = [];
      subjectList();
    })
    .catch(() => {});
}

async function submitForm() {
  if (saving.value) return;
  const valid = await albumFormRef.value.validate().catch(() => false);
  if (!valid || !dialogFormVisible.value || saving.value) return;
  saving.value = true;
  try {
    form.value.albumCover = await coverUpload.value.uploadPending();
    const payload = { ...form.value };
    await (isEditForm.value ? editSubject(payload) : addSubject(payload));
    ElMessage.success(isEditForm.value ? "修改成功" : "新增成功");
    dialogFormVisible.value = false;
    subjectList();
  } catch (error) {
    // 保留表单和已上传地址以便重试。
    if (error instanceof Error && !error.isAxiosError) ElMessage.error(error.message);
  } finally {
    saving.value = false;
  }
}

onMounted(subjectList);
</script>

<style scoped>
/* 组件库内部类由子组件在运行时生成。 */
.album-controls, .album-selection-bar, .album-selection-actions, .album-card-heading, .album-card-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.album-controls { padding: 16px; background: var(--cf-surface); border: 1px solid var(--cf-border); border-radius: 6px; }
.album-search { display: flex; align-items: center; gap: 8px; max-width: 100%; }
/*noinspection CssUnusedSymbol*/
.album-search .el-input { width: 260px; }
.album-selection-bar { margin: 16px 0; }
.album-selection-actions { justify-content: flex-start; color: var(--cf-muted); font-size: 13px; }
.album-selection-actions strong { font-weight: 500; color: var(--cf-text); }
.album-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 16px; }
.album-card { min-width: 0; overflow: hidden; background: var(--cf-surface); border: 1px solid var(--cf-border); border-radius: 6px; transition: border-color .15s; }
.album-card:hover { border-color: var(--el-border-color-dark); }
.album-card.is-selected { border-color: var(--el-color-primary); }
.album-card-heading { padding: 12px 14px; font-size: 12px; color: var(--cf-muted); }
.album-checkbox { display: flex; gap: 8px; align-items: center; cursor: pointer; }
.album-checkbox input { margin: 0; width: 15px; height: 15px; accent-color: var(--el-color-primary); cursor: pointer; }
.album-id { font-variant-numeric: tabular-nums; color: var(--cf-muted); }
.album-cover { display: block; width: 100%; height: 160px; padding: 0; border: 0; background: var(--cf-surface-subtle); cursor: pointer; }
/*noinspection CssUnusedSymbol*/
.album-cover .el-image { width: 100%; height: 100%; }
.album-cover-placeholder { height: 100%; display: flex; align-items: center; justify-content: center; color: var(--cf-muted); font-size: 13px; }
.album-card-body { padding: 16px 14px 14px; }
.album-title { padding: 0; border: 0; background: none; color: var(--cf-text); font: inherit; font-size: 15px; font-weight: 600; line-height: 1.5; text-align: left; cursor: pointer; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; max-width: 100%; }
.album-title:hover { color: var(--el-color-primary); }
.album-card-body p { display: flex; gap: 8px; margin: 6px 0 0; font-size: 12px; color: var(--cf-muted); }
.album-card-body p strong { color: var(--cf-muted); font-weight: 500; font-variant-numeric: tabular-nums; }
.album-card-actions { padding: 12px 14px; border-top: 1px solid var(--cf-border); }
/*noinspection CssUnusedSymbol*/
.album-card-actions .el-button { font-size: 12px; }
.album-card-actions > div { display: flex; gap: 12px; }
/*noinspection CssUnusedSymbol*/
.album-card-actions .el-button + .el-button { margin: 0; }
.album-empty { padding: 64px 20px; background: var(--cf-surface); border: 1px solid var(--cf-border); border-radius: 6px; text-align: center; }
.album-empty strong { font-size: 15px; font-weight: 500; color: var(--cf-muted); }
.album-empty p { color: var(--cf-muted); font-size: 13px; margin: 10px 0 0; }
.block { display: flex; justify-content: flex-end; padding: 20px 0 0; overflow-x: auto; }
@media (max-width: 640px) {
  .album-controls { align-items: stretch; flex-wrap: wrap; }
  .album-search { width: 100%; }
/*noinspection CssUnusedSymbol*/
  .album-search .el-input { width: auto; flex: 1; min-width: 0; }
  .album-selection-bar { gap: 8px; flex-wrap: wrap; }
  .album-grid { grid-template-columns: 1fr; }
}
</style>