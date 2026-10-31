<template>
  <div class="app-container business-page">
    <BusinessPageHeader title="会员管理" :count="total" />
    <el-form class="business-filter"
      :model="queryParams"
      ref="queryRef"
      :inline="true"
      v-show="showSearch"
    >
      <el-form-item label="会员号" prop="hsId">
        <el-input
          v-model="queryParams.hsId"
          placeholder="请输入会员号"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="会员名称" prop="username">
        <el-input
          v-model="queryParams.username"
          placeholder="请输入会员名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input
          v-model="queryParams.phone"
          placeholder="请输入手机号"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="会员状态"
          @change="handleQuery"
          clearable
          style="width: 200px"
        >
          <el-option
            v-for="dict in sys_normal_disable"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="注册时间" style="width: 308px">
        <el-date-picker
          v-model="dateRange"
          value-format="YYYY-MM-DD"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        >
        </el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery"
          >搜索</el-button
        >
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8 business-toolbar">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['web:member:add']"
          >新增</el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['web:member:edit']"
          >修改
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['web:member:remove']"
          >删除
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['web:member:export']"
          >导出</el-button
        >
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList">
      </right-toolbar>
    </el-row>

    <el-table class="business-table"
      v-loading="loading"
      :data="memberList"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="编号" align="center" prop="id" />
      <el-table-column label="会员号" width="150" align="center" prop="hsId" />
      <el-table-column label="头像" align="center" prop="avatar">
        <template #default="scope">
          <el-avatar :size="60" :src="scope.row.avatar"> </el-avatar>
        </template>
      </el-table-column>
      <el-table-column label="会员名称" align="center" prop="username" />
      <el-table-column label="性别" align="center" prop="gender">
        <template #default="scope">
          <dict-tag :options="sys_user_sex" :value="scope.row.gender" />
        </template>
      </el-table-column>
      <el-table-column label="手机号" width="150" align="center" prop="phone" />
      <el-table-column
        label="登录IP"
        width="150"
        align="center"
        prop="loginIp"
      />
      <el-table-column label="登录地址" align="center">
        <template #default="scope">
          <el-tag type="primary">{{ scope.row.address }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status">
        <template #default="scope">
          <dict-tag :options="sys_normal_disable" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column
        label="注册时间"
        align="center"
        prop="createTime"
        width="180"
        sortable="custom"
        :sort-orders="sortOrders"
      >
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column
        label="最后登录时间"
        align="center"
        prop="loginDate"
        width="180"
        sortable="custom"
        :sort-orders="sortOrders"
      >
        <template #default="scope">
          <span>{{ parseTime(scope.row.loginDate) }}</span>
        </template>
      </el-table-column>

      <el-table-column
        label="操作"
        align="center"
        fixed="right"
        min-width="200"
      >
        <template #default="scope">
          <el-button link
              type="primary"
              size="small"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['web:member:edit']">编辑</el-button>
          <el-button link
              type="danger"
              size="small"
              @click="handleDelete(scope.row)"
              v-hasPermi="['web:member:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改会员对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="memberRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="会员名称" prop="username">
          <el-input v-model="form.username" placeholder="请输入会员名称" />
        </el-form-item>
        <el-form-item label="头像" prop="avatar">
          <el-upload
            class="image-uploader"
            list-type="picture-card"
            :show-file-list="false"
            :auto-upload="false"
            :before-upload="beforeUpload"
            :on-change="handleChange"
          >
            <img alt="用户头像" v-if="form.avatar" :src="form.avatar" class="image" />
            <i v-else class="el-icon-plus image-uploader-icon">
              <el-icon>
                <Plus />
              </el-icon>
            </i>
          </el-upload>
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio
              v-for="dict in sys_user_sex"
              :key="dict.value"
              :label="dict.value"
              >{{ dict.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="form.id ? '不修改请留空' : '请输入密码'"
          />
        </el-form-item>
        <el-form-item label="会员状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in sys_normal_disable"
              :key="dict.value"
              :label="dict.value"
              >{{ dict.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            placeholder="请输入内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

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
