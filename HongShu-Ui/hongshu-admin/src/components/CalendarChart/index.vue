<template>
  <div class="contribution-body" v-loading="loading">
    <div v-if="error" class="contribution-empty">
      <span>发布活动加载失败</span>
      <el-button link type="primary" @click="refresh">重新加载</el-button>
    </div>
    <template v-else-if="hasData">
      <div class="contribution-summary">累计发布 <strong>{{ total.toLocaleString('zh-CN') }}</strong> 篇笔记</div>
      <div class="contribution-scroll"><div ref="chartRef" class="contribution-chart" role="img" aria-label="过去一年每日笔记发布数量热力图"></div></div>
    </template>
    <el-empty v-else-if="!loading" description="暂无发布活动" :image-size="64" />
    <div v-else class="contribution-placeholder"></div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { getBlogContributeCount } from "@/api/web/index";
import * as echarts from "echarts";
import "echarts/theme/v5";
import useSettingsStore from "@/store/modules/settings";
import { getChartAppearance, getChartHeatmapColors } from "@/utils/chartTheme";

const settingsStore = useSettingsStore();
const chartRef = ref(null);
const contributeDate = ref([]);
const blogContributeCount = ref([]);
const loading = ref(false);
const error = ref(false);
const hasData = computed(() => contributeDate.value.length === 2 && blogContributeCount.value.length > 0);
const total = computed(() => blogContributeCount.value.reduce((sum, item) => sum + (Number(item[1]) || 0), 0));
let chart = null;
let resizeObserver = null;
let disposed = false;

function disposeChart() {
  resizeObserver?.disconnect();
  resizeObserver = null;
  chart?.dispose();
  chart = null;
}

async function refresh() {
  if (loading.value) return;
  loading.value = true;
  error.value = false;
  try {
    const response = await getBlogContributeCount();
    if (disposed) return;
    if (response.code !== 200) {
      error.value = true;
      disposeChart();
      return;
    }
    contributeDate.value = response.data?.contributeDate || [];
    blogContributeCount.value = response.data?.blogContributeCount || [];
    await nextTick();
    renderChart();
  } catch {
    if (!disposed) {
      error.value = true;
      disposeChart();
    }
  } finally {
    loading.value = false;
  }
}

function renderChart() {
  if (!hasData.value || !chartRef.value || disposed) {
    disposeChart();
    return;
  }
  if (!chart) {
    chart = echarts.init(chartRef.value, "v5");
    resizeObserver = new ResizeObserver(() => chart?.resize());
    resizeObserver.observe(chartRef.value);
  }
  const maximum = Math.max(1, ...blogContributeCount.value.map((item) => Number(item[1]) || 0));
  const appearance = getChartAppearance(settingsStore.isDark);
  const heatmapColors = getChartHeatmapColors(settingsStore.theme, settingsStore.isDark);
  chart.setOption({
    animation: false,
    darkMode: settingsStore.isDark,
    backgroundColor: "transparent",
    textStyle: { color: appearance.text },
    tooltip: {
      trigger: "item",
      confine: true,
      renderMode: "richText",
      backgroundColor: appearance.tooltip,
      borderColor: appearance.border,
      textStyle: { color: appearance.text, fontSize: 12 },
      formatter: (params) => `${params.data[0]}\n发布笔记：${params.data[1]} 篇`,
    },
    visualMap: {
      type: "continuous",
      min: 0,
      max: maximum,
      orient: "horizontal",
      left: "right",
      bottom: 8,
      right: 20,
      itemWidth: 9,
      itemHeight: 90,
      text: ["多", "少"],
      textGap: 8,
      textStyle: { color: appearance.muted, fontSize: 11 },
      inRange: { color: heatmapColors },
      calculable: false,
    },
    calendar: {
      top: 32,
      left: 50,
      right: 20,
      cellSize: ["auto", 17],
      range: contributeDate.value,
      splitLine: { show: false },
      yearLabel: { show: false },
      dayLabel: { firstDay: 1, nameMap: ["日", "一", "二", "三", "四", "五", "六"], color: appearance.muted, fontSize: 10, margin: 12 },
      monthLabel: { nameMap: "cn", color: appearance.muted, fontSize: 11, margin: 12 },
      itemStyle: { color: heatmapColors[0], borderColor: appearance.surface, borderWidth: 3 },
    },
    series: [{
      name: "发布笔记",
      type: "heatmap",
      coordinateSystem: "calendar",
      data: blogContributeCount.value,
      emphasis: { itemStyle: { borderColor: heatmapColors[3], borderWidth: 1 } },
    }],
  }, true);
}

onMounted(refresh);
watch(() => [settingsStore.theme, settingsStore.isDark], renderChart);
onBeforeUnmount(() => { disposed = true; disposeChart(); });
defineExpose({ refresh });
</script>

<style scoped>
.contribution-body { min-height: 240px; padding: 18px 0 10px; }
.contribution-summary { padding: 0 20px; color: var(--cf-muted); font-size: 12px; }
.contribution-summary strong { margin: 0 3px; color: var(--cf-text); font-weight: 600; font-variant-numeric: tabular-nums; }
.contribution-scroll { overflow-x: auto; scrollbar-width: thin; }
.contribution-chart { width: 100%; min-width: 720px; height: 195px; }
.contribution-placeholder { height: 210px; }
.contribution-empty { display: flex; align-items: center; justify-content: center; gap: 12px; min-height: 210px; color: var(--cf-muted); font-size: 13px; }
</style>
