<template>
  <header class="cf-page-header business-page-header">
    <h1>{{ title }}</h1>
    <div v-if="count !== undefined || $slots.default" class="business-page-summary">
      <span v-if="count !== undefined" class="business-record-count">
        <span>共</span><strong>{{ count }}</strong><span>{{ unit }}</span>
      </span>
      <slot />
    </div>
  </header>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  count: { type: [Number, String], default: undefined },
  unit: { type: String, default: '条记录' }
})
</script>

<style scoped>
.business-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;
}
h1 {
  min-width: 0;
  margin: 0;
  color: var(--cf-text);
  font-size: 25px;
  line-height: 1.4;
  font-weight: 600;
  letter-spacing: -.4px;
  overflow-wrap: anywhere;
}
.business-page-summary { display: flex; align-items: center; gap: 12px; flex-shrink: 0; }
.business-record-count {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 10px;
  border: 1px solid var(--cf-border);
  border-radius: 6px;
  color: var(--cf-muted);
  background: var(--cf-surface);
  font-size: 12px;
  white-space: nowrap;
}
.business-record-count strong { color: var(--cf-text); font-size: 13px; font-weight: 600; font-variant-numeric: tabular-nums; }
@media (max-width: 640px) {
  .business-page-header { align-items: flex-start; gap: 12px; flex-wrap: wrap; margin-bottom: 18px; }
  h1 { font-size: 22px; }
}
</style>

<style>
/* 下方组件库内部类和共享布局类由运行时 DOM 或使用方页面提供。 */
/*noinspection CssUnusedSymbol*/
.business-page > .business-filter {
  padding: 18px 18px 0;
  margin-bottom: 16px;
  background: var(--cf-surface);
  border: 1px solid var(--cf-border);
  border-radius: 6px;
}
/*noinspection CssUnusedSymbol*/
.business-page > .business-filter .el-form-item { margin-right: 20px; margin-bottom: 18px; }
/*noinspection CssUnusedSymbol*/
.business-page > .business-filter .el-form-item__label { color: var(--cf-muted); font-size: 13px; }
/*noinspection CssUnusedSymbol*/
.business-page > .business-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 0;
  margin: 0 0 14px !important;
}
/*noinspection CssUnusedSymbol*/
.business-page > .business-toolbar .el-button { font-size: 13px; }
/*noinspection CssUnusedSymbol*/
.business-page > .business-table { border: 1px solid var(--cf-border); border-radius: 6px; }
/*noinspection CssUnusedSymbol*/
.business-page > .business-table .el-table__cell { padding: 11px 0; }
/*noinspection CssUnusedSymbol*/
.business-page > .business-table .el-avatar { width: 36px; height: 36px; font-size: 16px; }
/*noinspection CssUnusedSymbol*/
.business-page > .business-table .el-table__empty-block { min-height: 180px; }
/*noinspection CssUnusedSymbol*/
.business-page .cover-thumb { width: 42px; height: 42px; border: 1px solid var(--cf-border); border-radius: 4px; vertical-align: middle; }
/*noinspection CssUnusedSymbol*/
.business-page .pagination-container { margin-top: 0; padding-top: 18px; }
/*noinspection CssUnusedSymbol*/
.business-page .el-table .el-button.is-link { font-size: 13px; }
@media (max-width: 640px) {
/*noinspection CssUnusedSymbol*/
  .business-page > .business-filter { padding: 14px 14px 0; }
/*noinspection CssUnusedSymbol*/
  .business-page > .business-filter .el-form-item { display: flex; margin-right: 0; max-width: 100%; }
/*noinspection CssUnusedSymbol*/
  .business-page > .business-filter .el-form-item__content { min-width: 0; }
/*noinspection CssUnusedSymbol*/
  .business-page > .business-filter .el-input,
  .business-page > .business-filter .el-select,
  .business-page > .business-filter .el-date-editor { max-width: 100%; }
}
</style>
