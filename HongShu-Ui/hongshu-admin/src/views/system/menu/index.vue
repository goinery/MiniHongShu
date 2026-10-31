<template></template>

<script setup>
import { QuestionFilled } from '@element-plus/icons-vue'

defineOptions({ name: "Menu" })
import { getCurrentInstance, nextTick, reactive, ref, toRefs } from 'vue'

import {
  addMenu,
  delMenu,
  getMenu,
  listMenu,
  updateMenu,
} from "@/api/system/menu";
import SvgIcon from "@/components/SvgIcon";
import IconSelect from "@/components/IconSelect";

const { proxy } = getCurrentInstance();
const { sys_show_hide, sys_normal_disable } = proxy.useDict(
  "sys_show_hide",
  "sys_normal_disable"
);

const menuList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const title = ref("");
const menuOptions = ref([]);
const isExpandAll = ref(false);
const refreshTable = ref(true);
const iconSelectRef = ref(null);

const data = reactive({
  form: {},
  queryParams: {
    menuName: undefined,
    visible: undefined,
  },
  rules: {
    menuName: [
      { required: true, message: "菜单名称不能为空", trigger: "blur" },
    ],
    orderNum: [
      { required: true, message: "菜单顺序不能为空", trigger: "blur" },
    ],
    path: [{ required: true, message: "路由地址不能为空", trigger: "blur" }],
  },
});

const { queryParams, form, rules } = toRefs(data);

/** 查询菜单列表 */
function getList() {
  loading.value = true;
  listMenu(queryParams.value).then((response) => {
    menuList.value = proxy.handleTree(response.data, "menuId");
    loading.value = false;
  });
}
/** 查询菜单下拉树结构 */
function getTreeselect() {
  menuOptions.value = [];
  return listMenu().then((response) => {
    const menu = { menuId: 0, menuName: "主类目", children: [] };
    menu.children = proxy.handleTree(response.data, "menuId");
    menuOptions.value.push(menu);
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
    menuId: undefined,
    parentId: 0,
    menuName: undefined,
    icon: undefined,
    menuType: "M",
    orderNum: undefined,
    isFrame: "1",
    isCache: "0",
    visible: "0",
    status: "0",
  };
  proxy.resetForm("menuRef");
}
/** 展示下拉图标 */
function showSelectIcon() {
  iconSelectRef.value.reset();
}
/** 选择图标 */
function selected(name) {
  form.value.icon = name;
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
  getTreeselect();
  if (row != null && row.menuId) {
    form.value.parentId = row.menuId;
  } else {
    form.value.parentId = 0;
  }
  open.value = true;
  title.value = "添加菜单";
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
  await getTreeselect();
  getMenu(row.menuId).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = "修改菜单";
  });
}
/** 提交按钮 */
function submitForm() {
  proxy.$refs["menuRef"].validate((valid) => {
    if (valid) {
      if ((form.value.menuId !== undefined && form.value.menuId !== null)) {
        updateMenu(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addMenu(form.value).then(() => {
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
    .confirm('是否确认删除名称为"' + row.menuName + '"的数据项?')
    .then(function () {
      return delMenu(row.menuId);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => {});
}

getList();
</script>
