<script lang="ts" setup>
import { ref, onMounted } from "vue";
import { getReports, searchReports } from "@/services/reportService";
import { onPullDownRefresh } from "@dcloudio/uni-app";



interface Report {
  id: number;
  username: string;
  gender: string;
  age: number;
  symptoms: string;
  duration: string;
  treatment: string;
  other: string;
  checkTime: string;
  imageUrl: string;
  diseaseType: string;
  value: number;
  advice: string;
  introduction: string;
}

interface ApiResponse {
  code: number;
  msg: string;
  result: Report[];
}

const reports = ref<Report[]>([]);
const loading = ref(false);

const searchKeyword = ref("");

// 筛选后的报告列表
const filteredReports = async() =>{
  try {
    loading.value = true;
    const response: ApiResponse = await searchReports(searchKeyword.value);
    if (response.code === 0) {
      reports.value = response.result;
    } else {
      uni.showToast({
        title: response.msg || '获取报告失败',
        icon: 'none'
      });
    }
  } catch (error) {
    console.error('获取报告出错:', error);
    uni.showToast({
      title: '获取报告出错',
      icon: 'none'
    });
  } finally {
    loading.value = false;
    uni.stopPullDownRefresh();
  }
}



const fetchReports = async () => {
  try {
    loading.value = true;
    const response: ApiResponse = await getReports();
    if (response.code === 0) {
      reports.value = response.result;
    } else {
      uni.showToast({
        title: response.msg || '获取报告失败',
        icon: 'none'
      });
    }
  } catch (error) {
    console.error('获取报告出错:', error);
    uni.showToast({
      title: '获取报告出错',
      icon: 'none'
    });
  } finally {
    loading.value = false;
    uni.stopPullDownRefresh();
  }
};

const navigateToDetail = (id: number) => {
  uni.navigateTo({
    url: `/pages/my/sub/reportDetail/reportDetail?id=${id}`
  });
};

// 下拉刷新
onPullDownRefresh(() => {
  fetchReports();
});

// 页面加载时获取数据
onMounted(() => {
  fetchReports();
});
</script>

<template>
  <view class="container">
    <view class="header">
      <text class="title">检查报告列表</text>
      <button class="refresh-btn" @click="fetchReports" :disabled="loading">
        {{ loading ? '加载中...' : '刷新' }}
      </button>
    </view>

    <view class="search-bar">
      <input
        class="search-input"
        v-model="searchKeyword"
        placeholder="输入姓名"
        confirm-type="search"
      />
      <button @click="filteredReports">筛选</button>
    </view>


    <view v-if="reports.length === 0 && !loading" class="empty">
      <text>暂无报告数据</text>
    </view>

    <scroll-view v-else scroll-y class="report-list" refresher-enabled @refresherrefresh="fetchReports">
      <view
        v-for="report in reports"
        :key="report.id"
        class="report-card"
        @click="navigateToDetail(report.id)"
      >
        <view class="report-header">
          <text class="patient-name">{{ report.username }}</text>
          <text class="patient-info">{{ report.gender }} {{ report.age }}岁</text>
        </view>

        <view class="report-content">
          <view class="info-row">
            <text class="label">症状:</text>
            <text class="value">{{ report.symptoms }}</text>
          </view>

          <view class="info-row">
            <text class="label">疾病类型:</text>
            <text class="value">{{ report.diseaseType }}</text>
          </view>

          <view class="info-row">
            <text class="label">检查时间:</text>
            <text class="value">{{ new Date(report.checkTime).toLocaleString() }}</text>
          </view>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<style scoped>
.container {
  padding: 20rpx;
  background-color: #f5f5f5;
  min-height: 100vh;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30rpx;
}

.title {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
}

.refresh-btn {
  background-color: #4a90e2;
  color: white;
  font-size: 28rpx;
  padding: 10rpx 20rpx;
  border-radius: 10rpx;
}

.empty {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200rpx;
  color: #999;
  font-size: 32rpx;
}

.report-list {
  height: calc(100vh - 120rpx);
}

.report-card {
  background-color: white;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid #eee;
}

.patient-name {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.patient-info {
  font-size: 28rpx;
  color: #666;
}

.report-content {
  margin-bottom: 20rpx;
}

.info-row {
  display: flex;
  margin-bottom: 15rpx;
}

.label {
  width: 160rpx;
  font-size: 28rpx;
  color: #666;
}

.value {
  flex: 1;
  font-size: 28rpx;
  color: #333;
}

.search-bar {
  margin-bottom: 20rpx;
  padding: 0 10rpx;
}

.search-input {
  width: 100%;
  height: 70rpx;
  background-color: #fff;
  border-radius: 12rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
  border: 1rpx solid #ccc;
}

</style>
