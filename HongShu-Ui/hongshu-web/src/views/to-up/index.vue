<template>
  <el-dialog
    :model-value="modelValue"
    title="帮助与客服"
    class="community-dialog feedback-dialog"
    align-center
    append-to-body
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form class="feedback-form" label-position="top" @submit.prevent="submit">
      <el-form-item label="联系邮箱" for="feedback-email">
        <el-input id="feedback-email" v-model="emailVal" type="email" maxlength="254" placeholder="请输入邮箱" />
      </el-form-item>
      <el-form-item class="feedback-content" label="问题或建议" for="feedback-content">
        <el-input
          id="feedback-content"
          v-model="content"
          type="textarea"
          placeholder="请描述遇到的问题或建议"
          maxlength="5000"
          show-word-limit
          resize="none"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" :loading="submitting" @click="submit">发送</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref } from "vue";
import { ElMessage } from "element-plus";
import { toUp } from "@/api/util";

const emailVal = ref("");
const content = ref("");
const submitting = ref(false);

defineProps<{ modelValue: boolean }>();
const emit = defineEmits<{ (e: "update:modelValue", value: boolean): void }>();

const close = () => {
  emit("update:modelValue", false);
};

const submit = async () => {
  if (submitting.value) return;
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailVal.value.trim())) {
    ElMessage.warning("请输入正确的邮箱");
    return;
  }
  if (!content.value.trim()) {
    ElMessage.warning("请输入反馈内容");
    return;
  }
  submitting.value = true;
  try {
    await toUp({ email: emailVal.value.trim(), content: content.value.trim() });
    content.value = "";
    ElMessage.success("反馈已提交");
    close();
  } finally { submitting.value = false; }
};

</script>

<style lang="less">
.feedback-dialog:is(.el-dialog) {
  .feedback-form {
    display: flex;
    flex-direction: column;
    height: 100%;
    min-height: 200px;
  }

  .feedback-content {
    display: flex;
    flex-direction: column;
    flex: 1;
    min-height: 100px;
    margin-bottom: 0;

    .el-form-item__content {
      flex: 1;
      min-height: 0;
    }

    .el-textarea,
    .el-textarea__inner {
      height: 100%;
    }

    .el-textarea__inner {
      padding-bottom: 28px;
    }
  }

}
</style>
