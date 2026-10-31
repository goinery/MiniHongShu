<template>
  <div class="top-right-btn" :style="style">
    <el-row>
      <el-tooltip class="item" effect="dark" :content="showSearch ? '隐藏搜索' : '显示搜索'" placement="top" v-if="search">
        <el-button aria-label="切换筛选条件" icon="Search" @click="toggleSearch()" />
      </el-tooltip>
      <el-tooltip class="item" effect="dark" content="刷新" placement="top">
        <el-button aria-label="刷新列表" icon="Refresh" @click="refresh()" />
      </el-tooltip>
      <el-tooltip class="item" effect="dark" content="显隐列" placement="top" v-if="columns">
        <el-button aria-label="设置显示列" icon="Menu" @click="showColumn()" v-if="showColumnsType === 'transfer'"/>
        <el-dropdown trigger="click" :hide-on-click="false" style="padding-left: 12px" v-if="showColumnsType === 'checkbox'">
          <el-button aria-label="设置显示列" icon="Menu" />
          <template #dropdown>
            <el-dropdown-menu>
              <template v-for="item in columns" :key="item.key">
                <el-dropdown-item>
                  <el-checkbox :model-value="item.visible" @change="checkboxChange($event, item.key)" :label="item.label" />
                </el-dropdown-item>
              </template>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-tooltip>
    </el-row>
    <el-dialog :title="title" v-model="open" append-to-body>
      <el-transfer
        :titles="['显示', '隐藏']"
        v-model="value"
        :data="columns"
      ></el-transfer>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  /* 是否显示检索条件 */
  showSearch: {
    type: Boolean,
    default: true,
  },
  /* 显隐列信息 */
  columns: {
    type: Array,
  },
  /* 是否显示检索图标 */
  search: {
    type: Boolean,
    default: true,
  },
  /* 显隐列类型（transfer穿梭框、checkbox复选框） */
  showColumnsType: {
    type: String,
    default: "checkbox",
  },
  /* 右外边距 */
  gutter: {
    type: Number,
    default: 10,
  },
})

const emits = defineEmits(['update:showSearch', 'update:columns', 'queryTable']);

// 显隐数据
const value = computed({
  get: () => (props.columns || []).filter(column => column.visible === false).map(column => column.key),
  set: dataChange,
});
// 弹出层标题
const title = ref("显示/隐藏");
// 是否显示弹出层
const open = ref(false);

const style = computed(() => {
  const ret = {};
  if (props.gutter) {
    ret.marginRight = `${props.gutter / 2}px`;
  }
  return ret;
});

// 搜索
function toggleSearch() {
  emits("update:showSearch", !props.showSearch);
}

// 刷新
function refresh() {
  emits("queryTable");
}

// 右侧列表元素变化
function dataChange(data) {
  emits('update:columns', (props.columns || []).map(column => ({
    ...column,
    visible: !data.includes(column.key),
  })));
}

// 打开显隐列dialog
function showColumn() {
  open.value = true;
}

// 勾选
function checkboxChange(visible, key) {
  emits('update:columns', (props.columns || []).map(column =>
    column.key === key ? { ...column, visible } : column
  ));
}

</script>

<style lang='scss' scoped>
:deep(.el-transfer__button) {
  border-radius: 50%;
  display: block;
  margin-left: 0;
}
:deep(.el-transfer__button:first-child) {
  margin-bottom: 10px;
}
:deep(.el-dropdown-menu__item) {
  line-height: 30px;
  padding: 0 17px;
}
</style>
