<template></template>

<script setup>
import { Plus } from '@element-plus/icons-vue'

defineOptions({ name: "Note" })
import { getCurrentInstance, onMounted, reactive, ref, toRefs } from 'vue'
import { useRoute } from 'vue-router'

import BusinessPageHeader from "../components/BusinessPageHeader.vue";
import {
  listNote,
  addNote,
  delNote,
  getNote,
  updateNote,
} from "@/api/web/note";
import { listNavbar } from "@/api/web/navbar";

const { proxy } = getCurrentInstance();
const { audit_status, note_type, pinned } = proxy.useDict(
  "audit_status",
  "note_type",
  "pinned"
);

const route = useRoute();
const blogSort = ref(route.query.blogSort);
const noteList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const dateRange = ref([]);
const navbarOptions = ref([]);
const categoriesMap = ref({});

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    title: undefined,
    noteType: undefined,
    auditStatus: undefined,
    pid: "",
  },
  rules: {
    title: [{ required: true, message: "笔记标题不能为空", trigger: "blur" }],
    noteType: [
      { required: true, message: "笔记类型不能为空", trigger: "blur" },
    ],
  },
});

const { queryParams, form, rules } = toRefs(data);

/** 查询笔记列表 */
function getList() {
  loading.value = true;
  listNote(proxy.addDateRange(queryParams.value, dateRange.value)).then(
    (response) => {
      noteList.value = response.rows;
      total.value = response.total;
      loading.value = false;
    }
  );
}
/** 查询导航栏下拉树结构 */
function getTreeSelect() {
  navbarOptions.value = [];
  listNavbar().then((response) => {
    const navbar = { id: 0, title: "主类目", children: [], disabled: true };
    navbar.children = proxy.handleTree(response.data, "id", "pid");
    navbarOptions.value.push(navbar);
    createCategoriesMap(response.data);
  });
}
/** 生成分类映射 */
function createCategoriesMap(data) {
  categoriesMap.value = {};
  (data || []).forEach((item) => {
    if (!item || item.id === undefined || item.id === null) return;
    categoriesMap.value[item.id] = item.title;
    categoriesMap.value[String(item.id)] = item.title;
  });
}

/** 获取分类名称 */
function getCategoryTitle(pid, cid) {
  const lookup = (val) => {
    if (val === undefined || val === null || val === "") return "";
    const key = String(val);
    if (key === "0" || key === "-1") return "";
    return categoriesMap.value[val] || categoriesMap.value[key] || "";
  };
  return lookup(pid) || lookup(cid) || "未知分类";
}

/** 分类名称格式化器 */
function categoryFormatter(row, column, cellValue) {
  return getCategoryTitle(cellValue, row && row.cid);
}

function displayNoteType(val) {
  if (val === 0 || val === "0") return "1";
  return val;
}
/** 取消按钮 */
function cancel() {
  open.value = false;
  reset();
}
/** 表单重置 */
function reset() {
  form.value = {
    title: undefined,
    type: undefined,
    status: "0",
  };
  proxy.resetForm("noteRef");
}
/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}
/** 重置按钮操作 */
function resetQuery() {
  dateRange.value = [];
  proxy.resetForm("queryRef");
  handleQuery();
}
/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.id);
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
}
/** 新增按钮操作 */
function handleAdd() {
  reset();
  getTreeSelect();
  open.value = true;
  title.value = "添加笔记";
}
/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  getTreeSelect();
  const noteId = row.id || ids.value;
  getNote(noteId).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = "修改笔记";
  });
}
function beforeUpload() {
  return false;
}
function handleChange(file) {
  const reader = new FileReader();
  reader.readAsDataURL(file.raw);
  reader.onload = (e) => {
    form.value.noteCover = e.target.result; // 设置预览图URL
    form.value.file = file.raw; // 存储选择的文件
  };
}
/** 提交按钮 */
function submitForm() {
  proxy.$refs["noteRef"].validate((valid) => {
    if (valid) {
      let params = new FormData();
      params.append("file", form.value.file);
      params.append("note", JSON.stringify(form.value));
      if ((form.value.id !== undefined && form.value.id !== null)) {
        updateNote(params).then(() => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addNote(params).then(() => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        });
      }
    }
  });
}
/** 删除按钮操作 */
function handleDelete(row) {
  const noteIds = row.id || ids.value;
  proxy.$modal
    .confirm(
      row?.title
        ? `是否确认删除笔记「${row.title}」？`
        : `是否确认删除选中的 ${Array.isArray(noteIds) ? noteIds.length : 1} 条笔记？`
    )
    .then(function () {
      return delNote(noteIds);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download(
    "note/export",
    {
      ...queryParams.value,
    },
    `note_${new Date().getTime()}.xlsx`
  );
}

onMounted(() => {
  if (route.query.blogSort) {
    try {
      blogSort.value = JSON.parse(route.query.blogSort);
      queryParams.value.pid = blogSort.value.blogSortUid;
    } catch (e) {
      console.error("Failed to parse blogSort:", e);
    }
  }
  getList();
  getTreeSelect();
});
</script>

<style scoped></style>
