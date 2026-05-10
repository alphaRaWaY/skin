<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useReportStore } from '@/stores/modules/reportStore'
import { getImage, postReport, uploadImageToServer } from '@/services/reportService'

const report = useReportStore()
const displayImage = ref('')

type IndicatorItem = {
  name: string
  value: string
}

const parseLegacyTopScores = (value: string): number[] => {
  if (!value) return []
  // New format: confidence=0.8734;topScores=0.3282,0.3142,...
  const match = value.match(/topScores=([^;]+)/i)
  if (match && match[1]) {
    return match[1]
      .split(',')
      .map((s) => Number(s.trim()))
      .filter((n) => Number.isFinite(n))
  }
  // Old format: "3.2,3.0,2.6,4.1"
  return value
    .split(',')
    .map((s) => Number(s.trim()))
    .filter((n) => Number.isFinite(n))
}

const indicators = computed<IndicatorItem[]>(() => {
  const conceptScores = report.resultValue.conceptScores || []
  if (conceptScores.length > 0) {
    return conceptScores
      .slice()
      .sort((a, b) => (a.rankNo || 0) - (b.rankNo || 0))
      .map((item) => ({
        name: item.conceptNameCn || item.conceptNameEn || `概念${item.conceptIndex}`,
        value: Number(item.score).toFixed(4)
      }))
  }

  const scores = parseLegacyTopScores(report.resultValue.value || '')
  return scores.slice(0, 8).map((n, idx) => ({
    name: `指标 ${idx + 1}`,
    value: n.toFixed(4)
  }))
})

const isLocalPath = (value: string) => {
  if (!value) return false
  return (
    value.startsWith('wxfile://') ||
    value.startsWith('http://tmp/') ||
    value.startsWith('https://tmp/') ||
    value.startsWith('/static/') ||
    value.startsWith('/')
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
    content: '放弃本次检测结果并返回首页？',
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

    // Persist to OSS only when saving report.
    if (isLocalPath(imageUrl)) {
      const upload = await uploadImageToServer(imageUrl)
      imageUrl = upload.result
    }

    const payload = {
      ...data,
      imageUrl,
      checkTime: convertToISOTime(data.checkTime)
    }

    const resp = await postReport(payload)
    if (!resp || resp.code !== 0) {
      uni.hideLoading()
      uni.showToast({ title: resp?.msg || '保存失败', icon: 'none' })
      return
    }

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
    <uni-notice-bar
      show-icon
      scrollable
      text="结果仅供健康参考，不替代医生诊断。"
    />

    <view class="result-box">
      <text class="result-label">检测指标</text>
      <view class="indicator-grid">
        <view v-for="(item, index) in indicators" :key="index" class="indicator-card">
          <text class="indicator-value">{{ item.value }}</text>
          <text class="indicator-index">{{ item.name }}</text>
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
  font-size: 56rpx;
  color: #e43d33;
  font-weight: bold;
}

.indicator-index {
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #888;
}
</style>

