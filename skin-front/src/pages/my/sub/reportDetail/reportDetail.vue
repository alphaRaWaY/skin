<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue'
import { deleteReport, getImage, getReportDetail } from '@/services/reportService'

interface ConceptScore {
  conceptIndex: number
  conceptNameEn?: string
  conceptNameCn?: string
  score: number
  rankNo: number
}

interface Report {
  id: number
  username: string
  gender: string
  age: number
  symptoms: string
  duration: string
  treatment: string
  other: string
  checkTime: string
  imageUrl: string
  diseaseType: string
  value: string | number
  advice: string
  introduction: string
  conceptScores?: ConceptScore[]
}

const report = ref<Report | null>(null)
const loading = ref(false)
const reportId = ref<string | number>('')

const getQueryParams = () => {
  // #ifdef H5
  const query = new URLSearchParams(window.location.search)
  return query.get('id')
  // #endif

  // #ifndef H5
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  return currentPage.options?.id || currentPage.$route?.query?.id
  // #endif
}

const fetchReportDetail = async () => {
  try {
    loading.value = true
    const id = getQueryParams()
    if (!id) {
      uni.showToast({ title: '缺少报告 ID', icon: 'none' })
      return
    }

    reportId.value = id
    const response = await getReportDetail(Number(id))
    if (response.code !== 0 || !response.result) {
      uni.showToast({ title: response.msg || '获取报告失败', icon: 'none' })
      return
    }

    report.value = response.result

    if (report.value.imageUrl) {
      try {
        const imgRes = await getImage(report.value.imageUrl)
        if (imgRes.code === 0 && imgRes.result) {
          report.value.imageUrl = imgRes.result
        }
      } catch {
        // ignore image sign failure
      }
    }
  } catch {
    uni.showToast({ title: '获取报告失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const handleDelete = async () => {
  uni.showModal({
    title: '提示',
    content: '确认删除这份报告吗？',
    success: async (res) => {
      if (!res.confirm) return
      uni.showLoading({ title: '删除中...' })
      try {
        const response = await deleteReport(Number(reportId.value))
        if (response.code === 0) {
          uni.showToast({ title: '删除成功', icon: 'success' })
          setTimeout(() => uni.navigateBack(), 800)
        } else {
          uni.showToast({ title: response.msg || '删除失败', icon: 'none' })
        }
      } finally {
        uni.hideLoading()
      }
    }
  })
}

onMounted(fetchReportDetail)

const previewImage = (url: string) => {
  uni.previewImage({ urls: [url] })
}

const diagnosisValuesWithNames = computed(() => {
  if (!report.value) return []
  if (report.value.conceptScores && report.value.conceptScores.length > 0) {
    return report.value.conceptScores
      .slice()
      .sort((a, b) => (a.rankNo || 0) - (b.rankNo || 0))
      .map((item) => ({
        name: item.conceptNameCn || item.conceptNameEn || `概念${item.conceptIndex}`,
        value: Number(item.score || 0).toFixed(4)
      }))
  }

  const raw = String(report.value.value || '')
  return raw
    .split(',')
    .map((v) => Number(v))
    .filter((n) => !Number.isNaN(n))
    .map((n, idx) => ({
      name: `指标${idx + 1}`,
      value: n.toFixed(4)
    }))
})
</script>

<template>
  <view class="container">
    <view v-if="loading" class="loading"><text>加载中...</text></view>
    <view v-else-if="!report" class="empty"><text>未找到报告数据</text></view>

    <view v-else class="detail-content">
      <view class="header">
        <text class="title">检测报告详情</text>
        <text class="report-id">报告ID: {{ report.id }}</text>
      </view>

      <view class="section">
        <text class="section-title">基本信息</text>
        <view class="info-grid">
          <view class="info-item"><text class="info-label">姓名</text><text class="info-value">{{ report.username }}</text></view>
          <view class="info-item"><text class="info-label">性别</text><text class="info-value">{{ report.gender }}</text></view>
          <view class="info-item"><text class="info-label">年龄</text><text class="info-value">{{ report.age }}岁</text></view>
          <view class="info-item"><text class="info-label">检测时间</text><text class="info-value">{{ new Date(report.checkTime).toLocaleString() }}</text></view>
        </view>
      </view>

      <view class="section">
        <text class="section-title">症状信息</text>
        <view class="info-card"><text class="info-text">{{ report.symptoms }}</text></view>
        <view class="info-row"><text class="info-label">病程</text><text class="info-value">{{ report.duration }}</text></view>
      </view>

      <view class="section">
        <text class="section-title">诊断结果</text>
        <view class="info-item"><text class="info-label">疾病类型</text><text class="info-value">{{ report.diseaseType }}</text></view>
        <view class="info-item">
          <text class="info-label">检测指标</text>
          <view class="diagnosis-values">
            <view class="diagnosis-item" v-for="(item, index) in diagnosisValuesWithNames" :key="index">
              <text>{{ item.name }}: {{ item.value }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="section">
        <text class="section-title">治疗史</text>
        <view class="info-card"><text class="info-text">{{ report.treatment }}</text></view>
      </view>

      <view class="section">
        <text class="section-title">AI建议</text>
        <view class="info-card"><text class="info-text">{{ report.advice }}</text></view>
      </view>

      <view v-if="report.other" class="section">
        <text class="section-title">补充信息</text>
        <view class="info-card"><text class="info-text">{{ report.other }}</text></view>
      </view>

      <view v-if="report.introduction" class="section">
        <text class="section-title">疾病介绍</text>
        <view class="info-card"><text class="info-text">{{ report.introduction }}</text></view>
      </view>

      <view v-if="report.imageUrl" class="section">
        <text class="section-title">患处图片</text>
        <image :src="report.imageUrl" mode="widthFix" class="report-image" @click="previewImage(report.imageUrl)" />
      </view>

      <view class="action-buttons">
        <button class="delete-btn" @click="handleDelete">删除报告</button>
      </view>
    </view>
  </view>
</template>

<style scoped>
.container { padding: 20rpx; background-color: #f7f7f7; min-height: 100vh; }
.loading, .empty { display: flex; justify-content: center; align-items: center; height: 200rpx; color: #999; font-size: 32rpx; }
.detail-content { background-color: #fff; border-radius: 16rpx; padding: 20rpx; margin-bottom: 30rpx; }
.header { margin-bottom: 30rpx; padding-bottom: 20rpx; border-bottom: 1rpx solid #eee; }
.title { font-size: 36rpx; font-weight: bold; color: #333; display: block; margin-bottom: 10rpx; }
.report-id { font-size: 26rpx; color: #999; }
.section { margin-bottom: 30rpx; }
.section-title { font-size: 32rpx; font-weight: bold; color: #333; display: block; margin-bottom: 20rpx; padding-left: 10rpx; border-left: 6rpx solid #4a90e2; }
.info-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20rpx; }
.info-item { margin-bottom: 15rpx; }
.info-label { font-size: 28rpx; color: #666; display: block; margin-bottom: 5rpx; }
.info-value { font-size: 28rpx; color: #333; font-weight: 500; }
.info-card { background-color: #f9f9f9; border-radius: 12rpx; padding: 20rpx; margin-bottom: 20rpx; }
.info-text { font-size: 28rpx; color: #333; line-height: 1.6; user-select: text; }
.info-row { display: flex; align-items: center; margin-bottom: 15rpx; }
.info-row .info-label { width: 160rpx; margin-bottom: 0; }
.info-row .info-value { flex: 1; }
.report-image { width: 100%; border-radius: 12rpx; margin-top: 10rpx; }
.action-buttons { margin-top: 40rpx; display: flex; justify-content: center; }
.delete-btn { background-color: #ff4d4f; color: white; font-size: 30rpx; height: 80rpx; line-height: 80rpx; border-radius: 40rpx; width: 60%; }
.diagnosis-values { display: flex; flex-wrap: wrap; gap: 20rpx; margin-top: 10rpx; }
.diagnosis-item { font-size: 28rpx; color: #444; background-color: #f1f1f1; padding: 10rpx 20rpx; border-radius: 8rpx; }
</style>
