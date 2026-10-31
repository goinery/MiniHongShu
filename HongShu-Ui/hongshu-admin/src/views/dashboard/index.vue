<template>
  <div class="dashboard-editor-container">
    <header class="dashboard-heading">
      <h1>数据分析</h1>
      <div class="dashboard-actions">
        <span v-if="lastUpdated" class="update-time">更新于 {{ lastUpdated }}</span>
        <el-button :loading="loading" icon="Refresh" @click="fetchData(true)">刷新数据</el-button>
      </div>
    </header>

    <div class="overview-toolbar">
      <span class="overview-label"><span class="overview-dot"></span>数据概览</span>
      <span class="overview-period"><svg-icon icon-class="date" /> {{ today }}</span>
    </div>

    <el-alert
      v-if="failedSections.length"
      :title="`${failedSections.join('、')}暂时无法加载，请稍后刷新。`"
      type="warning"
      :closable="false"
      show-icon
      class="dashboard-alert"
    />

    <section class="metric-grid" aria-label="站点数据概览" :aria-busy="loading">
      <button v-for="metric in metrics" :key="metric.id" class="metric-card" @click="btnClick(metric.id)">
        <span class="metric-topline">
          <span>{{ metric.label }}</span>
          <svg-icon :icon-class="metric.icon" />
        </span>
        <span class="metric-value-row">
          <strong class="metric-value">{{ formatCount(metric.value) }}</strong>
          <span class="metric-link" aria-hidden="true">↗</span>
        </span>
      </button>
    </section>

    <section class="chart-grid">
      <article class="analytics-card traffic-card">
        <header class="analytics-card-header">
          <h2>访问趋势</h2>
          <span class="period-label"><svg-icon icon-class="date" /> 近 7 日</span>
        </header>
        <div class="chart-body" v-loading="loading && !showLineChart">
          <LineChart v-if="showLineChart" :chart-data="lineChartData" height="310px" />
          <el-empty v-else :description="failedSections.includes('访问趋势') ? '访问数据加载失败' : '暂无访问数据'" :image-size="72" />
        </div>
      </article>
      <article class="analytics-card category-card">
        <header class="analytics-card-header">
          <h2>内容分类</h2>
          <span class="period-label">笔记分布</span>
        </header>
        <div class="chart-body" v-loading="loading && !showPieBlogSortChart">
          <PieChart
            v-if="showPieBlogSortChart"
            :value="blogCountByBlogSort"
            :tag-name="blogSortNameArray"
            height="310px"
            @click-pie="clickBlogSortPie"
          />
          <el-empty v-else :description="failedSections.includes('内容分类') ? '分类数据加载失败' : '暂无分类数据'" :image-size="72" />
        </div>
      </article>
    </section>

    <article class="analytics-card contribution-card">
      <header class="analytics-card-header">
        <h2>内容发布活动</h2>
        <span class="period-label">过去一年</span>
      </header>
      <CalendarChart ref="calendarChart" />
    </article>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { init, getVisitByWeek, getBlogCountByBlogSort } from "@/api/web/index";
import CalendarChart from "@/components/CalendarChart";
import PieChart from "@/components/PieChart";
import LineChart from "@/components/LineChart";

const router = useRouter();
const calendarChart = ref(null);
const loading = ref(false);
const lastUpdated = ref("");
const failedSections = ref([]);
const blogTotal = ref(null);
const commentTotal = ref(null);
const userTotal = ref(null);
const visitAddTotal = ref(null);
const today = new Date().toLocaleDateString("zh-CN", { year: "numeric", month: "long", day: "numeric" });
const metrics = computed(() => [
  { id: "1", label: "今日 IP 数", value: visitAddTotal.value, icon: "eye" },
  { id: "2", label: "会员总数", value: userTotal.value, icon: "peoples" },
  { id: "4", label: "笔记总数", value: blogTotal.value, icon: "form" },
  { id: "3", label: "评论总数", value: commentTotal.value, icon: "message" },
]);
const lineChartData = ref({ date: [], expectedData: [], actualData: [] });
const blogCountByBlogSort = ref([]);
const blogSortNameArray = ref([]);
const showLineChart = ref(false);
const showPieBlogSortChart = ref(false);

function formatCount(value) {
  return value == null ? "—" : Number(value).toLocaleString("zh-CN");
}

function clickBlogSortPie(payload) {
  const blogSort = payload && typeof payload === "object" ? payload : blogCountByBlogSort.value[payload];
  if (!blogSort || blogSort.__others) return;
  router.push({ path: "/note/note", query: { blogSort: JSON.stringify(blogSort) } });
}

async function fetchData(refreshCalendar = false) {
  if (loading.value) return;
  loading.value = true;
  failedSections.value = [];
  const sections = ["数据概览", "访问趋势", "内容分类"];
  try {
    const results = await Promise.allSettled([init(), getVisitByWeek(), getBlogCountByBlogSort()]);
    results.forEach((result, index) => {
      if (result.status !== "fulfilled" || result.value.code !== 200) {
        failedSections.value.push(sections[index]);
        return;
      }
      const data = result.value.data;
      if (index === 0) {
        blogTotal.value = Number(data?.blogCount) || 0;
        commentTotal.value = Number(data?.commentCount) || 0;
        userTotal.value = Number(data?.userCount) || 0;
        visitAddTotal.value = Number(data?.visitCount) || 0;
      } else if (index === 1) {
        lineChartData.value = { date: data?.date || [], expectedData: data?.pv || [], actualData: data?.uv || [] };
        showLineChart.value = lineChartData.value.date.length > 0;
      } else {
        blogCountByBlogSort.value = Array.isArray(data) ? data : [];
        blogSortNameArray.value = blogCountByBlogSort.value.map((item) => item.name);
        showPieBlogSortChart.value = true;
      }
    });
    if (failedSections.value.length < sections.length) {
      lastUpdated.value = new Date().toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" });
    }
    if (refreshCalendar) await calendarChart.value?.refresh();
  } finally {
    loading.value = false;
  }
}

function btnClick(id) {
  const routes = { 1: "/loginlog/loginlog", 2: "/member/member", 3: "/message/comment", 4: "/note/note" };
  router.push({ path: routes[id] });
}

onMounted(() => fetchData());
</script>

<style scoped>
/* 组件库内部类由子组件在运行时生成。 */
.dashboard-editor-container { padding: 26px 28px 32px; color: var(--cf-text); background: var(--cf-background); }
.dashboard-heading { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 24px; }
.dashboard-heading h1 { margin: 0; font-size: 25px; font-weight: 600; letter-spacing: -.5px; }
.dashboard-actions { display: flex; align-items: center; gap: 14px; }
.update-time { color: var(--cf-muted); font-size: 12px; white-space: nowrap; }
.overview-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding-bottom: 13px; margin-bottom: 18px; border-bottom: 1px solid var(--cf-border); }
.overview-label { display: inline-flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 600; }
.overview-dot { width: 7px; height: 7px; background: var(--el-color-primary); border-radius: 50%; }
.overview-period { display: inline-flex; align-items: center; gap: 7px; color: var(--cf-muted); font-size: 12px; }
.dashboard-alert { margin-bottom: 16px; }
.metric-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; margin-bottom: 18px; }
.metric-card { padding: 18px 18px 13px; text-align: left; font-family: inherit; background: var(--cf-surface); border: 1px solid var(--cf-border); border-radius: 7px; color: inherit; cursor: pointer; transition: border-color .16s, background .16s; }
.metric-card:hover, .metric-card:focus-visible { border-color: var(--el-color-primary-light-5); background: var(--el-color-primary-light-9); }
.metric-topline { display: flex; justify-content: space-between; align-items: center; gap: 8px; color: var(--cf-muted); font-size: 12px; }
/*noinspection CssUnusedSymbol*/
.metric-topline .svg-icon { color: var(--cf-muted); width: 15px; height: 15px; }
.metric-value-row { display: flex; justify-content: space-between; align-items: baseline; gap: 12px; margin-top: 13px; }
.metric-value { display: block; font-size: 30px; line-height: 1.15; font-weight: 600; letter-spacing: -.7px; font-variant-numeric: tabular-nums; }
.metric-link { color: var(--el-color-primary); font-size: 14px; }
.chart-grid { display: grid; grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr); gap: 16px; margin-bottom: 18px; }
.analytics-card { min-width: 0; background: var(--cf-surface); border: 1px solid var(--cf-border); border-radius: 7px; overflow: hidden; }
.analytics-card-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; min-height: 49px; padding: 13px 18px; background: var(--cf-surface-subtle); border-bottom: 1px solid var(--cf-border); }
.analytics-card-header h2 { margin: 0; font-size: 13px; font-weight: 600; }
.period-label { display: inline-flex; align-items: center; gap: 6px; color: var(--cf-muted); font-size: 12px; white-space: nowrap; }
.chart-body { min-height: 338px; padding: 18px 14px 10px; }
/*noinspection CssUnusedSymbol*/
.chart-body > .el-empty { min-height: 310px; }
@media (min-width: 1600px) { .dashboard-editor-container { padding: 30px 36px 40px; } }
@media (max-width: 1100px) { .chart-grid { grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); } .metric-card { padding: 16px 13px 12px; } .update-time { display: none; } }
@media (max-width: 850px) { .chart-grid { grid-template-columns: minmax(0, 1fr); } .metric-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 600px) { .dashboard-editor-container { padding: 20px 14px 24px; } .dashboard-heading { gap: 12px; } .dashboard-heading h1 { font-size: 22px; } .metric-grid { gap: 10px; } .metric-value { font-size: 27px; } .overview-period { font-size: 11px; } }
</style>
