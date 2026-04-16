<script lang="ts" setup>
import { ref, onMounted ,computed} from "vue";
import { getReportDetail, deleteReport, getImage } from "@/services/reportService";



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

const report = ref<Report | null>(null);
const loading = ref(false);
const reportId = ref<string | number>('');

// 获取页面参数
const getQueryParams = () => {
  // #ifdef H5
  const query = new URLSearchParams(window.location.search);
  return query.get('id');
  // #endif

  // #ifndef H5
  const pages = getCurrentPages();
  const currentPage = pages[pages.length - 1];
  return currentPage.options?.id || currentPage.$route?.query?.id;
  // #endif
};

const fetchReportDetail = async () => {
  try {
    loading.value = true;
    const id = getQueryParams();
    if (!id) {
      uni.showToast({
        title: '缺少报告ID',
        icon: 'none'
      });
      return;
    }

    reportId.value = id;
    const response = await getReportDetail(Number(id));
    if (response.code === 0) {
      report.value = response.result;
    } else {
      uni.showToast({
        title: response.msg || '获取报告详情失败',
        icon: 'none'
      });
    }
    if (report.value?.imageUrl) {
      try {
        const res = await getImage(report.value?.imageUrl);
        console.log(res);
        report.value.imageUrl = res.result;
      } catch (e) {
        console.error('图片获取失败', e);
      }
    } else {
      uni.showToast({
        title: '该历史没有照片',
        icon: 'error'
      });
    }
  } catch (error) {
    console.error('获取报告详情出错:', error);
    uni.showToast({
      title: '获取报告详情出错',
      icon: 'none'
    });
  } finally {
    loading.value = false;
  }
};

const handleDelete = async () => {
  try {
    uni.showModal({
      title: '提示',
      content: '确定要删除这条报告吗？',
      success: async (res) => {
        if (res.confirm) {
          uni.showLoading({ title: '删除中...' });
          const response = await deleteReport(Number(reportId.value));
          if (response.code === 0) {
            uni.showToast({ title: '删除成功' });
            setTimeout(() => {
              uni.navigateBack();
            }, 1500);
          } else {
            uni.showToast({
              title: response.msg || '删除失败',
              icon: 'none'
            });
          }
        }
      }
    });
  } catch (error) {
    console.error('删除报告出错:', error);
    uni.showToast({
      title: '删除报告出错',
      icon: 'none'
    });
  } finally {
    uni.hideLoading();
  }
};

onMounted(async() => {
  fetchReportDetail();
});

const previewImage=(url:string)=>{
  uni.previewImage({
    urls:[url]
  })
}


const diagnosisValues = computed(() => {
  if (!report.value || typeof report.value.value !== 'number' && typeof report.value.value !== 'string') return [];
  const raw = String(report.value.value);
  return raw.split(',').map(val => parseFloat(val));
});

const diseaseIndicatorsMap: Record<string, string[]> = {
  '瘢痕': ['色素沉着', '高度', '血管分布', '柔软度'],
  '特应性皮炎': ['红斑', '浸润', '脱屑', '苔藓样变'],
  '银屑病': ['皮肤面积', '红斑', '脱屑', '硬化'],
  // 默认情况
  'default': ['指标', '指标2', '指标3', '指标4']
};


const diagnosisValuesWithNames = computed(() => {
  const values = diagnosisValues.value;
  const names = indicatorNames.value;
  return values.map((val, idx) => ({
    name: names[idx] || `值${idx + 1}`,
    value: val.toFixed(2)
  }));
});

const indicatorNames = computed(() => {
  if (!report.value) return [];
  console.log(report.value.diseaseType);
  return diseaseIndicatorsMap[report.value.diseaseType] || diseaseIndicatorsMap["default"];
});

</script>

<template>
  <view class="container">
    <!-- 加载状态 -->
    <view v-if="loading" class="loading">
      <text>加载中...</text>
    </view>

    <!-- 空状态 -->
    <view v-else-if="!report" class="empty">
      <text>未找到报告信息</text>
    </view>

    <!-- 报告详情内容 -->
    <view v-else class="detail-content">
      <!-- 头部信息 -->
      <view class="header">
        <text class="title">检查报告详情</text>
        <text class="report-id">报告ID: {{ report.id }}</text>
      </view>

      <!-- 患者基本信息 -->
      <view class="section">
        <text class="section-title">患者信息</text>
        <view class="info-grid">
          <view class="info-item">
            <text class="info-label">姓名</text>
            <text class="info-value">{{ report.username }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">性别</text>
            <text class="info-value">{{ report.gender }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">年龄</text>
            <text class="info-value">{{ report.age }}岁</text>
          </view>
          <view class="info-item">
            <text class="info-label">检查时间</text>
            <text class="info-value">{{ new Date(report.checkTime).toLocaleString() }}</text>
          </view>
        </view>
      </view>

      <!-- 症状信息 -->
      <view class="section">
        <text class="section-title">症状描述</text>
        <view class="info-card">
          <text class="info-text">{{ report.symptoms }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">持续时间:</text>
          <text class="info-value">{{ report.duration }}</text>
        </view>
      </view>

      <!-- 诊断信息 -->
      <view class="section">
        <text class="section-title">诊断信息</text>
        <view class="info-grid">
          <view class="info-item">
            <text class="info-label">疾病类型</text>
            <text class="info-value">{{ report.diseaseType }}</text>
          </view>
         <!-- <view class="info-item">
            <text class="info-label">诊断值</text>
            <text class="info-value">{{ report.value }}</text>
          </view> -->

          <!-- <view class="info-item">
            <text class="info-label">诊断值</text>
            <view class="diagnosis-values">
              <view
                class="diagnosis-item"
                v-for="(val, index) in diagnosisValues"
                :key="index"
              >
                <text>值 {{ index + 1 }}: {{ val.toFixed(2) }}</text>
              </view>
            </view>
          </view> -->

          <view class="info-item">
            <text class="info-label">诊断指标</text>
            <view class="diagnosis-values">
              <view
                class="diagnosis-item"
                v-for="(item, index) in diagnosisValuesWithNames"
                :key="index"
              >
                <text>{{ item.name }}: {{ item.value }}</text>
              </view>
            </view>
          </view>


        </view>
      </view>

      <!-- 治疗方案 -->
      <view class="section">
        <text class="section-title">治疗方案</text>
        <view class="info-card">
          <text class="info-text">{{ report.treatment }}</text>
        </view>
      </view>

      <!-- 医生建议 -->
      <view class="section">
        <text class="section-title">医生建议</text>
        <view class="info-card">
          <text class="info-text">{{ report.advice }}</text>
        </view>
      </view>

      <!-- 其他信息 -->
      <view v-if="report.other" class="section">
        <text class="section-title">其他信息</text>
        <view class="info-card">
          <text class="info-text">{{ report.other }}</text>
        </view>
      </view>

      <!-- 病情介绍 -->
      <view v-if="report.introduction" class="section">
        <text class="section-title">病情介绍</text>
        <view class="info-card">
          <text class="info-text">{{ report.introduction }}</text>
        </view>
      </view>

      <!-- 图片展示 -->
      <view v-if="report.imageUrl" class="section">
        <text class="section-title">相关图片</text>
        <image
          :src="report.imageUrl"
          mode="widthFix"
          class="report-image"
          @click="previewImage(report.imageUrl)"
        ></image>
      </view>

      <!-- 操作按钮 -->
      <view class="action-buttons">
        <button class="delete-btn" @click="handleDelete">删除报告</button>
      </view>
    </view>
  </view>
</template>

<style scoped>
.container {
  padding: 20rpx;
  background-color: #f7f7f7;
  min-height: 100vh;
}

.loading, .empty {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200rpx;
  color: #999;
  font-size: 32rpx;
}

.detail-content {
  background-color: #fff;
  border-radius: 16rpx;
  padding: 20rpx;
  margin-bottom: 30rpx;
}

.header {
  margin-bottom: 30rpx;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid #eee;
}

.title {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
  display: block;
  margin-bottom: 10rpx;
}

.report-id {
  font-size: 26rpx;
  color: #999;
}

.section {
  margin-bottom: 30rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  display: block;
  margin-bottom: 20rpx;
  padding-left: 10rpx;
  border-left: 6rpx solid #4a90e2;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;
}

.info-item {
  margin-bottom: 15rpx;
}

.info-label {
  font-size: 28rpx;
  color: #666;
  display: block;
  margin-bottom: 5rpx;
}

.info-value {
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
}

.info-card {
  background-color: #f9f9f9;
  border-radius: 12rpx;
  padding: 20rpx;
  margin-bottom: 20rpx;
}

.info-text {
  font-size: 28rpx;
  color: #333;
  line-height: 1.6;
}

.info-row {
  display: flex;
  align-items: center;
  margin-bottom: 15rpx;
}

.info-row .info-label {
  width: 160rpx;
  margin-bottom: 0;
}

.info-row .info-value {
  flex: 1;
}

.report-image {
  width: 100%;
  border-radius: 12rpx;
  margin-top: 10rpx;
}

.action-buttons {
  margin-top: 40rpx;
  display: flex;
  justify-content: center;
}

.delete-btn {
  background-color: #ff4d4f;
  color: white;
  font-size: 30rpx;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 40rpx;
  width: 60%;
}

.diagnosis-values {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
  margin-top: 10rpx;
}

.diagnosis-item {
  font-size: 28rpx;
  color: #444;
  background-color: #f1f1f1;
  padding: 10rpx 20rpx;
  border-radius: 8rpx;
}

</style>
