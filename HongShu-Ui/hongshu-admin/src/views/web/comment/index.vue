<template>
  <div class="app-container business-page">
    <BusinessPageHeader title="评论管理" :count="total" />
    <el-form class="business-filter"
      :model="queryParams"
      ref="queryRef"
      :inline="true"
      v-show="showSearch"
    >
      <el-form-item label="评论人" prop="uid">
        <el-input
          v-model="queryParams.uid"
          placeholder="请输入评论人ID"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="被评论人" prop="noteUid">
        <el-input
          v-model="queryParams.noteUid"
          placeholder="请输入被评论人ID"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="被评论笔记" prop="nid">
        <el-input
          v-model="queryParams.nid"
          placeholder="请输入笔记ID"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="评论内容" prop="content">
        <el-input
          v-model="queryParams.content"
          placeholder="请输入评论内容"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="评论时间" style="width: 308px">
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
        <el-button type="primary" icon="Search" @click="handleQuery">
          搜索
        </el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8 business-toolbar">
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['web:comment:remove']"
        >
          删除
        </el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList">
      </right-toolbar>
    </el-row>

    <el-table class="business-table"
      v-loading="loading"
      :data="commentList"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="序号" align="center" prop="id" />
      <el-table-column label="评论人" align="center" prop="username" />
      <el-table-column label="头像" align="center" prop="avatar">
        <template #default="scope">
          <el-avatar :size="50" :src="scope.row.avatar"> </el-avatar>
        </template>
      </el-table-column>
      <el-table-column label="被评论人" align="center" prop="replyUsername" />
      <el-table-column label="头像" align="center" prop="replyAvatar">
        <template #default="scope">
          <el-avatar :size="50" :src="scope.row.replyAvatar"> </el-avatar>
        </template>
      </el-table-column>
      <el-table-column
        label="笔记"
        width="120"
        align="center"
        :show-overflow-tooltip="true"
      >
        <template #default="scope">
          <router-link
            :to="'/web/comment-data/index/' + scope.row.nid"
            class="link-type"
          >
            <span>{{ scope.row.title }}</span>
          </router-link>
        </template>
      </el-table-column>
      <el-table-column label="封面" align="center" prop="noteCover" width="80">
        <template #default="scope">
          <el-image
            v-if="scope.row.noteCover"
            class="cover-thumb"
            :src="scope.row.noteCover"
            :preview-src-list="[scope.row.noteCover]"
            fit="cover"
            preview-teleported
          />
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="笔记作者" align="center" prop="pushUsername" width="110" show-overflow-tooltip />
      <el-table-column
        label="评论内容"
        align="left"
        prop="content"
        min-width="180"
        show-overflow-tooltip
      />
      <el-table-column label="评论等级" align="center" prop="level">
        <template #default="scope">
          <el-tag type="primary">
            {{ getLevelText(scope.row.level) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="评论时间"
        align="center"
        prop="createTime"
        width="180"
      >
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
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
              type="danger"
              size="small"
              @click="handleDelete(scope.row)"
              v-hasPermi="['web:comment:remove']">删除</el-button>
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

  </div>
</template>

<script setup>
defineOptions({ name: "Comment" })
import { getCurrentInstance, reactive, ref, toRefs } from 'vue'

import BusinessPageHeader from "../components/BusinessPageHeader.vue";
import { listComment, delComment } from "@/api/web/comment";

const { proxy } = getCurrentInstance();

const commentList = ref([]);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const dateRange = ref([]);

const levelMap = {
  1: "一级评论",
  2: "二级评论",
  3: "三级评论",
  4: "四级评论",
  5: "五级评论",
};
// 根据 level 返回对应的文本
const getLevelText = (level) => {
  return levelMap[level] || "其他评论";
};

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    uid: undefined,
    noteUid: undefined,
    content: undefined,
  },
  rules: {},
});

const { queryParams, form } = toRefs(data);

/** 查询评论列表 */
function getList() {
  loading.value = true;
  listComment(proxy.addDateRange(queryParams.value, dateRange.value)).then(
    (response) => {
      commentList.value = response.rows;
      total.value = response.total;
      loading.value = false;
    }
  );
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
/** 删除按钮操作 */
function handleDelete(row) {
  const commentIds = row.id || ids.value;
  proxy.$modal
    .confirm(
      row?.content
        ? `是否确认删除评论「${String(row.content).slice(0, 20)}」？`
        : `是否确认删除选中的 ${Array.isArray(commentIds) ? commentIds.length : 1} 条评论？`
    )
    .then(function () {
      return delComment(commentIds);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => {});
}
getList();
</script>
