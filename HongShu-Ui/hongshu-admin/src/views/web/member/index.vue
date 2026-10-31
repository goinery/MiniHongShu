<template></template>

<script setup>
import { Plus } from '@element-plus/icons-vue'

defineOptions({ name: "Member" })
import { getCurrentInstance, reactive, ref, toRefs } from 'vue'

import BusinessPageHeader from "../components/BusinessPageHeader.vue";
import {
  listMember,
  addMember,
  delMember,
  getMember,
  updateMember,
} from "@/api/web/member";

const { proxy } = getCurrentInstance();
const { sys_normal_disable, sys_user_sex } = proxy.useDict(
  "sys_normal_disable",
  "sys_user_sex"
);
const memberList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const dateRange = ref(/** @type {[] | [string, string] | null} */ ([]));
/** @type {('ascending' | 'descending')[]} */
const sortOrders = ['ascending', 'descending'];

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    username: undefined,
    phone: undefined,
    status: undefined,
  },
  rules: {
    username: [
      { required: true, message: "会员名称不能为空", trigger: "blur" },
    ],
    phone: [
      { required: true, message: "手机号不能为空", trigger: "blur" },
      {
        pattern: /^1[3456789]\d{9}$/,
        message: "请输入正确手机号",
        trigger: "blur",
      },
    ],
    password: [
      {
        validator: (rule, value, callback) => {
          if (!form.value.id && !value) {
            callback(new Error("密码不能为空"));
          } else {
            callback();
          }
        },
        trigger: "blur",
      },
    ],
  },
});

const { queryParams, form, rules } = toRefs(data);

/** 查询会员列表 */
function getList() {
  loading.value = true;
  listMember(proxy.addDateRange(queryParams.value, dateRange.value)).then(
    (response) => {
      memberList.value = response.rows;
      total.value = response.total;
      loading.value = false;
    }
  );
}
/** 取消按钮 */
function cancel() {
  open.value = false;
  reset();
}
/** 表单重置 */
function reset() {
  form.value = {
    id: undefined,
    username: undefined,
    phone: undefined,
    password: undefined,
    status: "0",
    remark: undefined,
  };
  proxy.resetForm("memberRef");
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
  open.value = true;
  title.value = "添加会员";
}
/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const memberId = row.id || ids.value;
  getMember(memberId).then((response) => {
    form.value = response.data;
    form.value.password = undefined;
    open.value = true;
    title.value = "修改会员";
  });
}
function beforeUpload() {
  return false;
}
function handleChange(file) {
  const reader = new FileReader();
  reader.readAsDataURL(file.raw);
  reader.onload = (e) => {
    form.value.avatar = e.target.result; // 设置预览图URL
    form.value.file = file.raw; // 存储选择的文件
  };
}
/** 提交按钮 */
function submitForm() {
  proxy.$refs["memberRef"].validate((valid) => {
    if (valid) {
      let params = new FormData();
      params.append("file", form.value.file);
      params.append("user", JSON.stringify(form.value));
      if ((form.value.id !== undefined && form.value.id !== null)) {
        updateMember(params).then(() => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addMember(params).then(() => {
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
  const memberIds = row.id || ids.value;
  proxy.$modal
    .confirm('是否确认删除会员编号为"' + memberIds + '"的数据项？')
    .then(function () {
      return delMember(memberIds);
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
    "member/export",
    {
      ...queryParams.value,
    },
    `member_${new Date().getTime()}.xlsx`
  );
}

getList();
</script>

<style scoped></style>
