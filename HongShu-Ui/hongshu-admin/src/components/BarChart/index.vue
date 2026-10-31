<template>
  <div :class="className" :style="{ height: height, width: width }"></div>
</template>

<script>
import * as echarts from "echarts";
import "echarts/theme/v5";
import { debounce } from "@/utils";
import useSettingsStore from "@/store/modules/settings";
import { getChartAppearance, getChartPalette } from "@/utils/chartTheme";

const animationDuration = 6000;

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
      default: "400px",
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
    this.__resizeHanlder = debounce(() => {
      if (this.chart) {
        this.chart.resize();
      }
    }, 100);
    window.addEventListener("resize", this.__resizeHanlder);
  },
  beforeUnmount() {
    if (!this.chart) {
      return;
    }
    window.removeEventListener("resize", this.__resizeHanlder);
    this.chart.dispose();
    this.chart = null;
  },
  watch: {
    chartAppearanceKey() {
      this.setOptions();
    },
  },
  methods: {
    initChart() {
      this.chart = echarts.init(this.$el, "v5");
      this.setOptions();
    },
    setOptions() {
      if (!this.chart) return;
      const appearance = getChartAppearance(this.settingsStore.isDark);
      this.chart.setOption({
        color: getChartPalette(this.settingsStore.theme, 3, this.settingsStore.isDark),
        darkMode: this.settingsStore.isDark,
        backgroundColor: "transparent",
        textStyle: { color: appearance.text },
        tooltip: {
          trigger: "axis",
          backgroundColor: appearance.tooltip,
          borderColor: appearance.border,
          textStyle: { color: appearance.text },
          extraCssText: `box-shadow: ${appearance.shadow}; border-radius: 5px;`,
          axisPointer: {
            // 坐标轴指示器，坐标轴触发有效
            type: "shadow", // 默认为直线，可选为：'line' | 'shadow'
            shadowStyle: { color: appearance.surfaceSubtle },
          },
        },
        grid: {
          top: 10,
          left: "2%",
          right: "2%",
          bottom: "3%",
          containLabel: true,
        },
        xAxis: [
          {
            type: "category",
            data: ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"],
            axisLine: { lineStyle: { color: appearance.border } },
            axisLabel: { color: appearance.muted },
            axisTick: {
              alignWithLabel: true,
            },
          },
        ],
        yAxis: [
          {
            type: "value",
            axisLine: { lineStyle: { color: appearance.border } },
            axisLabel: { color: appearance.muted },
            splitLine: { lineStyle: { color: appearance.grid } },
            axisTick: {
              show: false,
            },
          },
        ],
        series: [
          {
            name: "pageA",
            type: "bar",
            stack: "vistors",
            barWidth: "60%",
            data: [79, 52, 200, 334, 390, 330, 220],
            animationDuration,
          },
          {
            name: "pageB",
            type: "bar",
            stack: "vistors",
            barWidth: "60%",
            data: [80, 52, 200, 334, 390, 330, 220],
            animationDuration,
          },
          {
            name: "pageC",
            type: "bar",
            stack: "vistors",
            barWidth: "60%",
            data: [30, 52, 200, 334, 390, 330, 220],
            animationDuration,
          },
        ],
      });
    },
  },
};
</script>
