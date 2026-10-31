<template>
  <div class="app-container console-page monitor-page">
      <header class="cf-page-header">
         <div><h1>缓存监控</h1></div>
      </header>
    <el-row>
      <el-col :span="24" class="card-box">
        <el-card shadow="never">
          <template #header><Monitor style="width: 1em; height: 1em; vertical-align: middle;" /> <span style="vertical-align: middle;">基本信息</span></template>
          <div class="el-table el-table--enable-row-hover el-table--medium">
            <table style="border-spacing: 0; width: 100%">
              <tbody>
                <tr>
                  <td class="el-table__cell is-leaf"><div class="cell">Redis版本</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="cache.info">{{ cache.info.redis_version }}</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">运行模式</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="cache.info">{{ cache.info.redis_mode === "standalone" ? "单机" : "集群" }}</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">端口</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="cache.info">{{ cache.info.tcp_port }}</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">客户端数</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="cache.info">{{ cache.info.connected_clients }}</div></td>
                </tr>
                <tr>
                  <td class="el-table__cell is-leaf"><div class="cell">运行时间(天)</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="cache.info">{{ cache.info.uptime_in_days }}</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">使用内存</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="cache.info">{{ cache.info.used_memory_human }}</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">使用CPU</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="cache.info">{{ parseFloat(cache.info.used_cpu_user_children).toFixed(2) }}</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">内存配置</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="cache.info">{{ cache.info.maxmemory_human }}</div></td>
                </tr>
                <tr>
                  <td class="el-table__cell is-leaf"><div class="cell">AOF是否开启</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="cache.info">{{ cache.info.aof_enabled === "0" ? "否" : "是" }}</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">RDB是否成功</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="cache.info">{{ cache.info.rdb_last_bgsave_status }}</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">Key数量</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="cache.dbSize">{{ cache.dbSize }} </div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">网络入口/出口</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="cache.info">{{ cache.info.instantaneous_input_kbps }}kps/{{cache.info.instantaneous_output_kbps}}kps</div></td>
                </tr>
              </tbody>
            </table>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :lg="12" class="card-box">
        <el-card shadow="never">
          <template #header><PieChart style="width: 1em; height: 1em; vertical-align: middle;" /> <span style="vertical-align: middle;">命令统计</span></template>
          <div class="el-table el-table--enable-row-hover el-table--medium">
            <div ref="commandstats" style="height: 420px" />
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :lg="12" class="card-box">
        <el-card shadow="never">
          <template #header><Odometer style="width: 1em; height: 1em; vertical-align: middle;" /> <span style="vertical-align: middle;">内存信息</span></template>
          <div class="el-table el-table--enable-row-hover el-table--medium">
            <div ref="usedmemory" style="height: 420px" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { Monitor, Odometer, PieChart } from '@element-plus/icons-vue'

defineOptions({ name: "Cache" })
import { getCurrentInstance, onBeforeUnmount, onMounted, ref, watch } from 'vue'

import { getCache } from '@/api/monitor/cache';
import * as echarts from 'echarts';
import 'echarts/theme/v5';
import useSettingsStore from '@/store/modules/settings';
import { getChartAppearance, getChartPalette } from '@/utils/chartTheme';

const settingsStore = useSettingsStore();
const cache = ref([]);
const commandstats = ref(null);
const usedmemory = ref(null);
const { proxy } = getCurrentInstance();
let commandstatsInstance;
let usedmemoryInstance;

function chartPalette() {
  return getChartPalette(settingsStore.theme, 6, settingsStore.isDark);
}

function updateChartTheme() {
  const appearance = getChartAppearance(settingsStore.isDark);
  const tooltip = {
    backgroundColor: appearance.tooltip,
    borderColor: appearance.border,
    textStyle: { color: appearance.text },
    extraCssText: `box-shadow: ${appearance.shadow};`
  };
  commandstatsInstance?.setOption({
    color: chartPalette(),
    textStyle: { color: appearance.muted },
    tooltip,
    series: [{
      label: { color: appearance.text },
      labelLine: { lineStyle: { color: appearance.muted } },
      itemStyle: { borderColor: appearance.surface }
    }]
  });
  usedmemoryInstance?.setOption({
    textStyle: { color: appearance.muted },
    tooltip,
    series: [{
      axisLine: { lineStyle: { color: [[1, appearance.grid]] } },
      progress: { itemStyle: { color: chartPalette()[0] } },
      pointer: { itemStyle: { color: chartPalette()[0] } },
      axisTick: { lineStyle: { color: appearance.border } },
      splitLine: { lineStyle: { color: appearance.border } },
      axisLabel: { color: appearance.muted },
      title: { color: appearance.muted },
      detail: { color: appearance.text }
    }]
  });
}

function resizeCharts() {
  commandstatsInstance?.resize();
  usedmemoryInstance?.resize();
}

watch(() => [settingsStore.theme, settingsStore.isDark], updateChartTheme);

function getList() {
  proxy.$modal.loading("正在加载缓存监控数据，请稍候！");
  getCache().then(response => {
    proxy.$modal.closeLoading();
    cache.value = response.data;

    if (!commandstats.value || !usedmemory.value) return;
    const appearance = getChartAppearance(settingsStore.isDark);
    commandstatsInstance = echarts.init(commandstats.value, 'v5');
    commandstatsInstance.setOption({
      color: chartPalette(),
      textStyle: { fontFamily: "Arial, Microsoft YaHei, sans-serif", color: appearance.muted },
      tooltip: {
        trigger: "item",
        backgroundColor: appearance.tooltip,
        borderColor: appearance.border,
        textStyle: { color: appearance.text, fontSize: 12 },
        extraCssText: `box-shadow: ${appearance.shadow};`,
        formatter: "{a} <br/>{b} : {c} ({d}%)"
      },
      series: [
        {
          name: "命令",
          type: "pie",
          radius: ["38%", "62%"],
          center: ["50%", "48%"],
          itemStyle: { borderWidth: 3, borderColor: appearance.surface, borderRadius: 3 },
          label: { color: appearance.text },
          labelLine: { lineStyle: { color: appearance.muted } },
          data: response.data.commandStats,
          animationEasing: "cubicInOut",
          animationDuration: 1000
        }
      ]
    });
    usedmemoryInstance = echarts.init(usedmemory.value, 'v5');
    usedmemoryInstance.setOption({
      textStyle: { fontFamily: "Arial, Microsoft YaHei, sans-serif", color: appearance.muted },
      tooltip: {
        backgroundColor: appearance.tooltip,
        borderColor: appearance.border,
        textStyle: { color: appearance.text, fontSize: 12 },
        extraCssText: `box-shadow: ${appearance.shadow};`,
        formatter: "{b} <br/>{a} : " + cache.value.info.used_memory_human
      },
      series: [
        {
          name: "峰值",
          type: "gauge",
          min: 0,
          max: 1000,
          axisLine: { lineStyle: { width: 10, color: [[1, appearance.grid]] } },
          progress: { show: true, width: 10, itemStyle: { color: chartPalette()[0] } },
          pointer: { itemStyle: { color: chartPalette()[0] } },
          axisTick: { lineStyle: { color: appearance.border } },
          splitLine: { lineStyle: { color: appearance.border, width: 1 } },
          axisLabel: { color: appearance.muted, fontSize: 11 },
          title: { color: appearance.muted, fontSize: 13 },
          detail: {
            color: appearance.text,
            fontSize: 26,
            formatter: cache.value.info.used_memory_human
          },
          data: [
            {
              value: parseFloat(cache.value.info.used_memory_human),
              name: "内存消耗"
            }
          ]
        }
      ]
    })
  }).catch(() => { proxy.$modal.closeLoading(); });
}

onMounted(() => {
  getList();
  window.addEventListener("resize", resizeCharts);
});
onBeforeUnmount(() => {
  window.removeEventListener("resize", resizeCharts);
  commandstatsInstance?.dispose();
  usedmemoryInstance?.dispose();
});
</script>

<style lang="scss" scoped>
.monitor-page > .el-row { margin: 0 -8px; }
.monitor-page .card-box { padding: 0 8px 16px; }
.monitor-page :deep(.el-card) { border: 1px solid var(--cf-border); border-radius: 8px; box-shadow: none; }
.monitor-page :deep(.el-card__header) { padding: 15px 18px; color: var(--cf-muted); font-size: 13px; font-weight: 500; background: var(--cf-surface-subtle); border-bottom: 1px solid var(--cf-border); }
.monitor-page :deep(.el-card__body) { padding: 16px; }
.monitor-page .el-table { overflow-x: auto; font-size: 13px; color: var(--cf-text); }
.monitor-page .el-table .cell { padding: 10px 12px; line-height: 1.6; overflow-wrap: anywhere; }
.monitor-page .el-table th { background: var(--cf-surface-subtle); color: var(--cf-muted); font-weight: 500; }
.monitor-page .el-table td { border-bottom: 1px solid var(--cf-border); }
.monitor-page .el-table tr:last-child td { border-bottom: 0; }
.monitor-page .el-table tbody td:nth-child(odd) { color: var(--cf-muted); }
.monitor-page .el-table .text-danger { color: var(--el-color-danger); }
@media (max-width: 767px) {
  .monitor-page .el-table table { min-width: 480px; }
  .monitor-page :deep(.el-card__body) { padding: 12px; }
}
</style>
