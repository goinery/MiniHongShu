<template>
  <div>
    <template v-for="item in options">
      <template v-if="values.includes(String(item.value))">
        <span
          v-if="(item.elTagType === 'default' || item.elTagType === '') && (item.elTagClass === '' || item.elTagClass == null)"
          :key="item.value"
          :class="item.elTagClass"
        >{{ item.label + " " }}</span>
        <el-tag
          v-else
          :disable-transitions="true"
          :key="item.value + ''"
          :type="item.elTagType === 'primary' ? '' : item.elTagType"
          :class="item.elTagClass"
        >{{ item.label + " " }}</el-tag>
      </template>
    </template>
    <template v-if="unmatch && showValue">
      {{ handleArray(unmatchArray) }}
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // 数据
  options: {
    type: Array,
    default: () => [],
  },
  // 当前的值
  value: [Number, String, Array],
  // 当未找到匹配的数据时，显示value
  showValue: {
    type: Boolean,
    default: true,
  },
  separator: {
    type: String,
    default: ",",
  }
});

const values = computed(() => {
  if (props.value === null || typeof props.value === 'undefined' || props.value === '') return [];
  return Array.isArray(props.value) ? props.value.map(item => '' + item) : String(props.value).split(props.separator);
});

const unmatchArray = computed(() => values.value.filter(value =>
  !(props.options || []).some(option => String(option.value) === value)
));
const unmatch = computed(() => unmatchArray.value.length > 0);

function handleArray(array) {
  if (array.length === 0) return "";
  return array.reduce((pre, cur) => {
    return pre + " " + cur;
  });
}
</script>

<style scoped>
/* 组件库内部类由子组件在运行时生成。 */
/*noinspection CssUnusedSymbol*/
.el-tag + .el-tag {
  margin-left: 10px;
}
</style>
