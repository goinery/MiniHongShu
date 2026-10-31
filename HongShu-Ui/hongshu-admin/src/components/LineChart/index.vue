<template>
  <div :class="className" :style="{ height: height, width: width }"></div>
</template>

<script>
import * as echarts from "echarts";
import "echarts/theme/v5";
import { debounce } from "@/utils";
import useSettingsStore from "@/store/modules/settings";
import { getChartAppearance, getChartPalette, themeColorWithAlpha } from "@/utils/chartTheme";

function areaLineSeries({ name, data, color, surface, lineType = "solid" }) {
  return {
    name,
    type: "line",
    data,
    smooth: false,
    showSymbol: false,
    symbol: "circle",
    symbolSize: 7,
    sampling: "lttb",
    itemStyle: {
      color,
      borderWidth: 2,
      borderColor: surface,
    },
    lineStyle: {
      width: 2,
      color,
      type: lineType,
    },
    areaStyle: {
      color: {
        type: "linear",
        x: 0,
        y: 0,
        x2: 0,
        y2: 1,
        colorStops: [
          { offset: 0, color: themeColorWithAlpha(color, 0.06) },
          { offset: 1, color: themeColorWithAlpha(color, 0.005) },
        ],
      },
    },
    emphasis: {
      focus: "series",
      showSymbol: true,
      symbolSize: 8,
      lineStyle: { width: 2.5 },
    },
    animationDuration: 900,
    animationEasing: "cubicOut",
  };
}

export default {
  setup() {
    return { settingsStore: useSettingsStore() };
  },
  props: {
    className: {
      type: String,
      default: "chart",
    },
    width: {
      type: String,
      default: "100%",
    },
    height: {
      type: String,
      default: "340px",
    },
    autoResize: {
      type: Boolean,
      default: true,
    },
    chartData: {
      type: Object,
      default: () => ({ date: [], expectedData: [], actualData: [] }),
    },
  },
  data() {
    return {
      chart: null,
    };
  },
  computed: {
    chartAppearanceKey() {
      return [this.settingsStore.theme, this.settingsStore.isDark];
    },
  },
  mounted() {
    this.initChart();
    if (this.autoResize) {
      this.__resizeHanlder = debounce(() => {
        if (this.chart) this.chart.resize();
      }, 100);
      window.addEventListener("resize", this.__resizeHanlder);
      this.__resizeObserver = new ResizeObserver(this.__resizeHanlder);
      this.__resizeObserver.observe(this.$el);
      const sidebarElm = document.getElementsByClassName("sidebar-container")[0];
      if (sidebarElm) {
        sidebarElm.addEventListener("transitionend", this.__resizeHanlder);
      }
    }
  },
  beforeUnmount() {
    this.__resizeObserver?.disconnect();
    if (!this.chart) return;
    if (this.autoResize && this.__resizeHanlder) {
      window.removeEventListener("resize", this.__resizeHanlder);
      const sidebarElm = document.getElementsByClassName("sidebar-container")[0];
      if (sidebarElm) {
        sidebarElm.removeEventListener("transitionend", this.__resizeHanlder);
      }
    }
    this.chart.dispose();
    this.chart = null;
  },
  watch: {
    chartAppearanceKey() {
      this.setOptions(this.chartData);
    },
    chartData: {
      deep: true,
      handler(val) {
        this.setOptions(val);
      },
    },
  },
  methods: {
    setOptions({ date, expectedData, actualData } = {}) {
      if (!this.chart) return;
      const labels = date || [];
      const pv = (expectedData || []).map((n) => Number(n) || 0);
      const uv = (actualData || []).map((n) => Number(n) || 0);
      const appearance = getChartAppearance(this.settingsStore.isDark);
      const [pvColor, uvColor] = getChartPalette(this.settingsStore.theme, 2, this.settingsStore.isDark);
      this.chart.setOption(
        {
          color: [pvColor, uvColor],
          darkMode: this.settingsStore.isDark,
          backgroundColor: "transparent",
          textStyle: { color: appearance.text },
          legend: {
            data: ["访问量(PV)", "独立用户(UV)"],
            top: 0,
            left: 6,
            itemWidth: 8,
            itemHeight: 8,
            itemGap: 16,
            icon: "circle",
            textStyle: {
              color: appearance.muted,
              fontSize: 12,
            },
          },
          tooltip: {
            trigger: "axis",
            confine: true,
            backgroundColor: appearance.tooltip,
            borderColor: appearance.border,
            borderWidth: 1,
            padding: [8, 12],
            textStyle: { color: appearance.text, fontSize: 12 },
            extraCssText: `box-shadow: ${appearance.shadow}; border-radius: 5px;`,
            axisPointer: {
              type: "line",
              lineStyle: {
                color: appearance.muted,
                width: 1,
                type: "dashed",
              },
            },
            formatter: (params) => {
              if (!params || !params.length) return "";
              const header = `<div style="font-weight:600;color:${appearance.text};margin-bottom:6px;">${params[0].axisValue}</div>`;
              const rows = params
                .map((item) => {
                  return `
                    <div style="display:flex;align-items:center;justify-content:space-between;gap:16px;margin:4px 0;min-width:140px;">
                      <span style="display:inline-flex;align-items:center;gap:6px;color:${appearance.muted};">
                        <i style="display:inline-block;width:8px;height:8px;border-radius:2px;background:${item.color};"></i>
                        ${item.seriesName}
                      </span>
                      <strong style="color:${appearance.text};font-variant-numeric:tabular-nums;">${item.value}</strong>
                    </div>`;
                })
                .join("");
              return header + rows;
            },
          },
          grid: {
            left: "2%",
            right: "3%",
            bottom: "6%",
            top: "16%",
            containLabel: true,
          },
          xAxis: {
            type: "category",
            data: labels,
            boundaryGap: false,
            axisLine: { lineStyle: { color: appearance.border } },
            axisTick: { show: false },
            axisLabel: {
              color: appearance.muted,
              fontSize: 11,
              margin: 10,
            },
          },
          yAxis: {
            type: "value",
            min: 0,
            minInterval: 1,
            axisLine: { show: false },
            axisTick: { show: false },
            axisLabel: {
              color: appearance.muted,
              fontSize: 11,
            },
            splitLine: {
              lineStyle: {
                color: appearance.grid,
                type: "dashed",
              },
            },
          },
          series: [
            areaLineSeries({
              name: "访问量(PV)",
              data: pv,
              color: pvColor,
              surface: appearance.surface,
            }),
            areaLineSeries({
              name: "独立用户(UV)",
              data: uv,
              color: uvColor,
              surface: appearance.surface,
              lineType: "dashed",
            }),
          ],
        },
        true
      );
    },
    initChart() {
      this.chart = echarts.init(this.$el, "v5");
      this.setOptions(this.chartData);
    },
  },
};
</script>
