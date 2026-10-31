<template>
  <div class="app-container business-page">
    <BusinessPageHeader title="导航管理" />
    <el-form class="business-filter"
      :model="queryParams"
      ref="queryRef"
      :inline="true"
      v-show="showSearch"
    >
      <el-form-item label="分类名称" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入分类名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="分类状态"
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
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">
          搜索
        </el-button>
        <el-button icon="Refresh" @click="resetQuery"> 重置 </el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8 business-toolbar">
      <el-col :span="1.5">
        <el-button
          type="primary"
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['web:navbar:add']"
        >
          添加分类
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button plain icon="Sort" @click="toggleExpandAll">
          展开/折叠
        </el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList">
      </right-toolbar>
    </el-row>

    <el-table class="business-table"
      v-if="refreshTable"
      v-loading="loading"
      :data="navbarList"
      row-key="id"
      :default-expand-all="isExpandAll"
      :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
    >
      <el-table-column
        prop="id"
        label="序号"
        :show-overflow-tooltip="true"
        width="150"
        align="center"
      >
      </el-table-column>
      <el-table-column
        prop="title"
        label="分类名称"
        :show-overflow-tooltip="true"
        width="150"
        align="center"
      >
      </el-table-column>
      <el-table-column label="封面" align="center" prop="normalCover" width="80">
        <template #default="scope">
          <el-image
            v-if="scope.row.normalCover"
            class="cover-thumb"
            :src="scope.row.normalCover"
            :preview-src-list="[scope.row.normalCover]"
            fit="cover"
            preview-teleported
          />
          <span v-else>—</span>
        </template>
      </el-table-column>
      <!-- <el-table-column label="热门封面" align="center" prop="hotCover">
        <template #default="scope">
          <el-avatar :size="60" :src="scope.row.hotCover" shape="square">
          </el-avatar>
        </template>
      </el-table-column> -->
      <el-table-column
        prop="description"
        label="分类描述"
        :show-overflow-tooltip="true"
        width="160"
        align="center"
      >
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="scope">
          <dict-tag :options="sys_normal_disable" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="60" align="center">
      </el-table-column>
      <el-table-column
        label="创建时间"
        align="center"
        width="180"
        prop="createTime"
      >
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column
        label="更新时间"
        align="center"
        width="180"
        prop="updateTime"
      >
        <template #default="scope">
          <span>{{ parseTime(scope.row.updateTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column
        label="操作"
        align="center"
        fixed="right"
        min-width="150"
      >
        <template #default="scope">
          <el-button link
              type="primary"
              size="small"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['web:navbar:edit']">编辑</el-button>
          <el-button link
              type="danger"
              size="small"
              @click="handleDelete(scope.row)"
              v-hasPermi="['web:navbar:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加或修改导航栏对话框 -->
    <el-dialog :title="title" v-model="open" width="680px" append-to-body>
      <el-form ref="navbarRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="13">
            <el-form-item label="上级分类" prop="pid">
              <el-tree-select
                v-model="form.pid"
                :data="navbarOptions"
                :props="{
                  value: 'id',
                  label: 'title',
                  children: 'children',
                }"
                value-key="id"
                placeholder="选择上级分类"
                check-strictly
              />
            </el-form-item>
          </el-col>
          <el-col :span="13">
            <el-form-item label="分类名称" prop="title">
              <el-input v-model="form.title" placeholder="请输入分类名称" />
            </el-form-item>
            <el-form-item label="分类描述" prop="description">
              <el-input
                v-model="form.description"
                type="textarea"
                :autosize="{ minRows: 2, maxRows: 4 }"
                placeholder="选填，简要说明分类"
              />
            </el-form-item>
          </el-col>
          <el-col :span="13">
            <el-form-item label="封面图" prop="normalCover">
              <el-upload
                class="image-uploader"
                list-type="picture-card"
                :show-file-list="false"
                :auto-upload="false"
                :before-upload="beforeUpload"
                :on-change="handleChange"
              >
                <img alt="图片预览"
                  v-if="form.normalCover"
                  :src="form.normalCover"
                  class="image"
                />
                <i v-else class="el-icon-plus image-uploader-icon">
                  <el-icon>
                    <Plus />
                  </el-icon>
                </i>
              </el-upload>
            </el-form-item>
          </el-col>
          <el-col :span="13">
            <el-form-item label="显示排序" prop="sort">
              <el-input-number v-model="form.sort" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="13">
            <el-form-item>
              <template #label>
                <span> 分类状态 </span>
              </template>
              <el-radio-group v-model="form.status">
                <el-radio
                  v-for="dict in sys_normal_disable"
                  :key="dict.value"
                  :label="dict.value"
                >
                  {{ dict.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
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

<style scoped>
/* 组件库内部类由子组件在运行时生成。 */
/*noinspection CssUnusedSymbol*/
.image-uploader .el-upload {
  border: 1px dashed var(--cf-border);
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

/*noinspection CssUnusedSymbol*/
.image-uploader .el-upload:hover {
  border-color: var(--el-color-primary);
}

.image-uploader-icon {
  font-size: 28px;
  color: var(--cf-muted);
  width: 100%;
  height: 100%;
  line-height: 145px;
  margin-left: 1px;
  text-align: center;
}

.image {
  width: 100%;
  height: 100%;
  display: block;
}
</style>
