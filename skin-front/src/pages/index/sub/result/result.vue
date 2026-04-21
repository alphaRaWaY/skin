<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useReportStore } from '@/stores/modules/reportStore'
import { getImage, postReport, uploadImageToServer } from '@/services/reportService'

const report = useReportStore()
const displayImage = ref('')

const indicators = computed(() => {
  return report.resultValue.value
    ? report.resultValue.value.split(',').map((s) => parseFloat(s.trim()))
    : []
})

const isLocalPath = (value: string) => {
  if (!value) return false
  return (
    value.startsWith('wxfile://') ||
    value.startsWith('/static/') ||
    value.startsWith('/') ||
    value.startsWith('http://') ||
    value.startsWith('https://')
  )
}

const convertToISOTime = (timeString: string) => {
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/.test(timeString)) {
    return timeString
  }
  const date = new Date(timeString)
  if (isNaN(date.getTime())) {
    return new Date().toISOString().split('.')[0]
  }
  return date.toISOString().split('.')[0]
}

const abandonResult = () => {
  uni.showModal({
    title: '提示',
    content: '确认放弃当前检测结果？',
    success: (res) => {
      if (!res.confirm) return
      report.reset()
      uni.switchTab({ url: '/pages/index/index' })
    }
  })
}

const saveReport = async () => {
  const data = report.getReport()
  let imageUrl = data.imageUrl

  try {
    uni.showLoading({ title: '保存中...' })

    // 只有保存报告时才上传图片到 OSS
    if (isLocalPath(imageUrl)) {
      const upload = await uploadImageToServer(imageUrl)
      imageUrl = upload.result
    }

    const payload = {
      ...data,
      imageUrl,
      checkTime: convertToISOTime(data.checkTime)
    }

    await postReport(payload)
    report.setImageUrl(imageUrl)
    uni.hideLoading()
    uni.showToast({ title: '保存成功', icon: 'success' })
    uni.switchTab({ url: '/pages/index/index' })
  } catch (error) {
    uni.hideLoading()
    uni.showToast({ title: '保存失败', icon: 'none' })
    console.error(error)
  }
}

onMounted(async () => {
  const raw = report.imageUrl
  if (!raw) return

  if (isLocalPath(raw)) {
    displayImage.value = raw
    return
  }

  try {
    const res = await getImage(raw)
    displayImage.value = res.result
  } catch (error) {
    console.error(error)
  }
})
</script>

<template>
  <view class="container">
    <uni-notice-bar show-icon scrollable text="AI结果仅供健康参考，不替代医生诊断。" />

    <view class="result-box">
      <text class="result-label">检测指标</text>
      <view class="indicator-grid">
        <view v-for="(item, index) in indicators" :key="index" class="indicator-card">
          <text class="indicator-value">{{ item }}</text>
          <text class="indicator-index">指标 {{ index + 1 }}</text>
        </view>
      </view>
    </view>

    <view class="btn-group">
      <button type="primary" class="btn" @click="abandonResult">放弃结果</button>
      <button class="btn" @click="saveReport">保存报告</button>
    </view>

    <uni-section title="基本信息" type="line" padding>
      <view class="info">
        <view>姓名：{{ report.form.username }}</view>
        <view>性别：{{ report.form.gender }}</view>
        <view>年龄：{{ report.form.age }}</view>
        <view>症状：{{ report.form.symptoms }}</view>
      </view>
    </uni-section>

    <view v-if="displayImage" class="image-wrapper">
      <image :src="displayImage" mode="aspectFit" />
    </view>

    <uni-card title="AI建议" sub-title="仅供参考" mode="style" :is-shadow="true">
      <text user-select>{{ report.resultValue.advice }}</text>
    </uni-card>

    <uni-card title="疾病介绍" sub-title="辅助信息" mode="style" :is-shadow="true">
      <text user-select>{{ report.resultValue.introduction }}</text>
    </uni-card>
  </view>
</template>

<style scoped>
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
