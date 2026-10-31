<template>
  <div class="sug-container-wrapper query-trending sug-pad">
    <div class="sug-container query-trending">
      <div class="history" v-show="historyRecordList != null && historyRecordList.length > 0">
        <div class="header">
          <span> 历史记录 </span>
          <div class="icon-group">
            <div class="icon-box" @click="showDeleteTag">
              <Delete style="width: 1.2em; height: 1.2em"></Delete>
            </div>
            <!---->
          </div>
        </div>
        <div class="history-list">
          <div v-for="(item, index) in historyRecordList" :key="index">
            <div class="history-item">
              <span @click="searchPage(item)">{{ item }}</span>
              <!---->
              <span class="close-tag round-close" v-show="showTagState" @click="deleteRecord(index)">×</span>
            </div>
          </div>
          <!---->
        </div>
      </div>
      <div class="sug-box">
        <div class="header">猜你想搜</div>
        <div class="sug-wrapper">
          <div
            class="sug-item query-trending hotspot"
            v-for="(item, index) in recommendRecords"
            :key="index"
            @click="searchPage(item)"
          >
            <div class="sug-text">
              {{ item }}
            </div>
          </div>
        </div>
      </div>
      <div>
        <div class="hotspots">
          <div class="header">
            <span class="hotspot-label">热点</span>
            <span class="hotspot-brand">
              <svg class="hotspot-fire" viewBox="0 0 24 24" width="14" height="14" aria-hidden="true">
                <path
                  fill="currentColor"
                  d="M12 2s3.2 3.1 3.2 6.2c0 1.6-.7 3-1.8 4 .8-.2 1.5-.6 2.1-1.2C16.8 9.6 18 7.4 18 5.5 18 8.8 20 11 20 14.2 20 18.4 16.4 22 12 22S4 18.4 4 14.2C4 10.6 7.1 8.1 8.6 6.3 9.8 4.9 12 2 12 2z"
                />
              </svg>
              热门内容
            </span>
          </div>
          <div class="hotspot-list">
            <div class="hotspot-item" v-for="(item, index) in hotList" :key="index">
              <p class="hotspot-index">{{ index + 1 }}</p>
              <div class="hotspot-title" @click="searchPage(item.content)">
                <span class="text">{{ item.content }}</span>
                <span class="hot-tag is-new">新</span>
                <span v-show="item.searchCount > 10" class="hot-tag is-hot">热</span>
              </div>
              <span class="hotspot-score">{{ item.searchCount }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script lang="ts" setup>
import { Delete } from "@element-plus/icons-vue";
import { ref, onMounted, watch } from "vue";
import { useSearchStore } from "@/store/searchStore";
import { getHotRecord, addRecord } from "@/api/search";
import { getRandomString } from "@/utils/util";
import { storage } from "@/utils/storage";
import { useRouter } from "vue-router";

const router = useRouter();
const searchStore = useSearchStore();

const historyRecordList = ref<Array<string>>([]);
const hotList = ref<Array<any>>([]);
const showTagState = ref(false);
const recommendRecords = ["壁纸", "风景", "情侣", "头像", "动漫", "动物"];

const props = defineProps({
  closeHistoryRecord: {
    type: Boolean,
    default: false,
  },
});

const showDeleteTag = () => {
  showTagState.value = !showTagState.value;
};

const deleteRecord = (index: number) => {
  historyRecordList.value.splice(index, 1);
  // 使用store失效
  storage.set("historyRecords", historyRecordList.value);
};

watch(
  () => [searchStore.seed, props.closeHistoryRecord],
  () => {
    showTagState.value = false;
    historyRecordList.value = searchStore.getRecords();
    getHotRecord().then((res) => {
      hotList.value = res.data;
    });
  }
);

const searchPage = (keyword: string) => {
  addRecord(keyword);
  searchStore.setKeyword(keyword);
  searchStore.pushRecord(keyword);
  const seed = getRandomString(12);
  searchStore.setSeed(seed);
  router.push({ name: "search", query: { keyword: keyword } });
};

onMounted(() => {
  historyRecordList.value = searchStore.getRecords();
  getHotRecord().then((res) => {
    hotList.value = res.data;
  });
});
</script>
<style lang="less" scoped></style>
