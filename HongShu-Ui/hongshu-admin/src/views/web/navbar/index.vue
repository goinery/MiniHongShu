<template></template>

<script setup>
import { Plus } from '@element-plus/icons-vue'

defineOptions({ name: "Navbar" })
import { getCurrentInstance, nextTick, reactive, ref, toRefs } from 'vue'

import BusinessPageHeader from "../components/BusinessPageHeader.vue";
import {
  addNavbar,
  delNavbar,
  getNavbar,
  listNavbar,
  updateNavbar,
} from "@/api/web/navbar";

const { proxy } = getCurrentInstance();
const { sys_normal_disable } = proxy.useDict(
  "sys_show_hide",
  "sys_normal_disable"
);

const navbarList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const title = ref("");
const navbarOptions = ref([]);
const isExpandAll = ref(false);
const refreshTable = ref(true);

const data = reactive({
  form: {
    normalCover: "",
    file: null,
    id: undefined,
    pid: 0,
    title: "",
    sort: 0,
    isFrame: "1",
    visible: "0",
    status: "0",
  },
  queryParams: {
    title: undefined,
    visible: undefined,
  },
  rules: {
    pid: [{ required: true, message: "请选择分类", trigger: "blur" }],
    title: [{ required: true, message: "分类名称不能为空", trigger: "blur" }],
    sort: [{ required: true, message: "分类顺序不能为空", trigger: "blur" }],
  },
});

const { queryParams, form, rules } = toRefs(data);

/** 查询导航栏列表 */
function getList() {
  loading.value = true;
  listNavbar(queryParams.value).then((response) => {
    navbarList.value = proxy.handleTree(response.data, "id", "pid");
    loading.value = false;
  });
}
/** 查询导航栏下拉树结构 */
function getTreeSelect() {
  navbarOptions.value = [];
  return listNavbar().then((response) => {
    const navbar = { id: 0, title: "主类目", children: [] };
    navbar.children = proxy.handleTree(response.data, "id", "pid");
    navbarOptions.value.push(navbar);
  });
}
/** 取消按钮 */
function cancel() {
  open.value = false;
  reset();
}
/** 表单重置 */
function reset() {
  form.value = {
    normalCover: "",
    file: null,
    id: undefined,
    pid: 0,
    title: "",
    sort: 0,
    isFrame: "1",
    visible: "0",
    status: "0",
  };
  proxy.resetForm("navbarRef");
}
/** 搜索按钮操作 */
function handleQuery() {
  getList();
}
/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}
/** 新增按钮操作 */
function handleAdd(row) {
  reset();
  getTreeSelect();
  if (row != null && row.id) {
    form.value.pid = row.id;
  } else {
    form.value.pid = 0;
  }
  open.value = true;
  title.value = "添加分类";
}
/** 展开/折叠操作 */
function toggleExpandAll() {
  refreshTable.value = false;
  isExpandAll.value = !isExpandAll.value;
  nextTick(() => {
    refreshTable.value = true;
  });
}
/** 修改按钮操作 */
async function handleUpdate(row) {
  reset();
  await getTreeSelect();
  getNavbar(row.id).then((response) => {
    form.value = response.data;
    form.value.pid = 0;
    open.value = true;
    title.value = "修改分类";
  });
}
function beforeUpload() {
  return false;
}
function handleChange(file) {
  const reader = new FileReader();
  reader.readAsDataURL(file.raw);
  reader.onload = (e) => {
    form.value.normalCover = e.target.result; // 设置预览图URL
    form.value.file = file.raw; // 存储选择的文件
  };
}
/** 提交按钮 */
function submitForm() {
  proxy.$refs["navbarRef"].validate((valid) => {
    if (valid) {
      let params = new FormData();
      if (form.value.file) params.append("file", form.value.file);
      params.append("category", JSON.stringify(form.value));
      if ((form.value.id !== undefined && form.value.id !== null)) {
        updateNavbar(params).then(() => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addNavbar(params).then(() => {
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
  proxy.$modal
    .confirm('是否确认删除名称为"' + row.title + '"的数据项?')
    .then(function () {
      return delNavbar(row.id);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => {});
}

getList();
</script>

<style scoped></style>
