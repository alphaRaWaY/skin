<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useReportStore } from '@/stores/modules/reportStore'
import { getImage, postReport } from '@/services/reportService';
import type {Report} from '@/types/report'

const report = useReportStore();
const uploadedImage = ref('');
const formData = report.form;
const result = report.resultValue;

// 疾病类型对应的指标名称映射
const diseaseIndicators = {
  '瘢痕': ['色素沉着', '高度', '血管分布', '柔软度'],
  '特应性皮炎': ['红斑', '浸润', '脱屑', '苔藓样变'],
  '银屑病': ['皮肤面积', '红斑', '脱屑', '硬化'],
  // 默认情况
  'default': ['指标', '指标2', '指标3', '指标4']
}

// 获取当前疾病对应的指标名称
const currentIndicators = computed(() => {
  const diseaseType = result?.diseaseType || '';
  if (diseaseType.includes('瘢痕')) return diseaseIndicators['瘢痕'];
  if (diseaseType.includes('特应性皮炎')) return diseaseIndicators['特应性皮炎'];
  if (diseaseType.includes('银屑病')) return diseaseIndicators['银屑病'];
  return diseaseIndicators['default'];
});



const reset = ()=>{
  uni.showModal({
    title: '提示',
    content: '你确定要放弃此次结果吗？',
    success: function (res) {
      if (res.confirm) {
        report.reset()
        uni.showToast({
          title:'已放弃结果',
          icon:'none'
        })
        setTimeout(()=>{
          uni.switchTab({
            url: '/pages/index/index' ,
          })
        },500);
      }
    }
  })
}

const save = async () => {
  const data = report.getReport();
  // 确保日期格式正确
  const postData = {
    ...data,
    checkTime: convertToISOTime(data.checkTime)
  };
  console.log(postData);
  try {
    const response = await postReport(postData);
    uni.showToast({
      title:'保存成功',
      icon:'success'
    })
    uni.switchTab({
      url:'/pages/index/index'
    })
  } catch (error) {
    console.log(error)
  }
}

const convertToISOTime = (timeString: string) => {
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/.test(timeString)) {
    return timeString;
  }
  if (timeString.includes('上午') || timeString.includes('下午')) {
    return convertChineseTimeToISO(timeString);
  }
  try {
    const date = new Date(timeString);
    return date.toISOString().split('.')[0];
  } catch (e) {
    console.warn('无法解析日期，使用当前时间:', timeString);
    return new Date().toISOString().split('.')[0];
  }
}

const convertChineseTimeToISO = (chineseTime: string) => {
  try {
    const normalized = chineseTime
      .replace('上午', 'AM')
      .replace('下午', 'PM')
      .replace(/\s+/g, ' ');
    const date = new Date(normalized);
    if (isNaN(date.getTime())) {
      throw new Error('Invalid date format');
    }
    return date.toISOString().split('.')[0];
  } catch (error) {
    console.error('日期转换失败:', error);
    return new Date().toISOString();
  }
}

const indicators = computed(() => {
  return result.value
    ? result.value.split(',').map(s => parseFloat(s.trim()))
    : [];
});

onMounted(async () => {
  if (report.imageUrl) {
    try {
      const res = await getImage(report.imageUrl);
      console.log(res);
      uploadedImage.value = res.result;
    } catch (e) {
      console.error('图片获取失败', e);
    }
  } else {
    uni.showToast({
      title: '未上传照片',
      icon: 'error'
    });
  }
});
</script>

<template>
  <view class="container">
    <!-- 指标展示 -->
    <uni-notice-bar
      show-icon
      scrollable
      text="AI 检测完成，以下为分析结果，请根据建议及时就医"
    />

    <view class="result-box">
      <text class="result-label">检测指标</text>
      <view class="indicator-grid">
        <view v-for="(item, index) in indicators" :key="index" class="indicator-card">
          <text class="indicator-value">{{ item }}</text>
          <text class="indicator-index">{{ currentIndicators[index] }}</text>
        </view>
      </view>
    </view>

    <!-- 操作按钮 -->
    <view class="btn-group">
      <button type="primary" class="btn" @click="reset">重新检测</button>
      <button class="btn" @click="save">保存结果</button>
    </view>

    <!-- 基本信息卡片 -->
    <uni-section title="基本信息" type="line" padding>
      <view class="info">
        <view>用户名：{{ formData.username }}</view>
        <view>性别：<uni-tag :text="formData.gender" type="success" /></view>
        <view>年龄：{{ formData.age }}</view>
        <view>症状：{{ formData.symptoms }}</view>
        <view>持续时间：{{ formData.duration }}</view>
        <view>治疗措施：{{ formData.treatment }}</view>
        <view>其他信息：{{ formData.other }}</view>
        <view>疾病类型：<uni-tag :text="result.diseaseType" type="warning" /></view>
        <view>检测时间：{{ formData.checkTime }}</view>
      </view>
    </uni-section>

    <!-- 图片展示 -->
    <view v-if="uploadedImage" class="image-wrapper">
      <image :src="uploadedImage" mode="aspectFit" />
    </view>

    <!-- AI建议 -->
    <uni-card title="AI 建议" sub-title="智能分析" mode="style" :is-shadow="true">
      <text>{{ result.advice }}</text>
    </uni-card>

    <!-- 疾病介绍 -->
    <uni-card title="疾病介绍" sub-title="健康科普" mode="style" :is-shadow="true">
      <text>{{ result.introduction }}</text>
    </uni-card>
  </view>
</template>

<style scoped>
/* 原有样式保持不变 */
.container {
  padding: 24rpx;
  background-color: #f4f4f4;
}

.result-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: 32rpx 0;
}
.result-value {
  font-size: 80rpx;
  color: #e43d33;
  font-weight: bold;
}
.result-label {
  font-size: 28rpx;
  color: #666;
}

.btn-group {
  display: flex;
  justify-content: space-around;
  margin: 20rpx 0;
}
.btn {
  flex: 1;
  margin: 0 10rpx;
  padding: 20rpx;
  font-size: 30rpx;
}

.info view {
  padding: 10rpx;
  font-size: 28rpx;
  margin-bottom: 10rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.image-wrapper {
  margin: 24rpx 0;
  display: flex;
  justify-content: center;
}
image {
  width: 100%;
  height: 300rpx;
  border-radius: 16rpx;
}

.indicator-grid {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 20rpx;
  margin-top: 20rpx;
  width: 100%;
}

.indicator-card {
  flex: 1 1 45%;
  background-color: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  text-align: center;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
}

.indicator-value {
  font-size: 60rpx;
  color: #e43d33;
  font-weight: bold;
}

.indicator-index {
  margin-top: 10rpx;
  font-size: 28rpx;
  color: #888;
}
</style>
