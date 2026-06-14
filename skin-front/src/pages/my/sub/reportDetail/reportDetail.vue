<script setup lang="ts">
import { onMounted, ref } from 'vue'
import {
  deleteMedicalCase,
  getMedicalCaseDetail,
  type MedicalCaseItem,
} from '@/services/medicalCaseService'
import { getImage } from '@/services/reportService'

const loading = ref(false)
const caseId = ref(0)
const detail = ref<MedicalCaseItem | null>(null)
const originalImage = ref('')
const heatmapImage = ref('')

const getPageId = () => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const raw = currentPage?.options?.id
  return raw ? Number(raw) : 0
}

const formatTime = (value?: string) => {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = (part: number) => `${part}`.padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(
    date.getHours(),
  )}:${pad(date.getMinutes())}`
}

const statusText = (status?: string) => {
  const labels: Record<string, string> = {
    PENDING: '待处理',
    IN_PROGRESS: '进行中',
    FOLLOWUP: '待复查',
    DONE: '已完成',
    CLOSED: '已关闭',
  }
  return status ? labels[status] || status : '--'
}

const resolveImage = async (value?: string) => {
  if (!value) return ''
  if (value.startsWith('http://') || value.startsWith('https://')) return value
  const response = await getImage(value)
  return response.result || ''
}

const fetchDetail = async () => {
  caseId.value = getPageId()
  if (!caseId.value) {
    uni.showToast({ title: '缺少病例 ID', icon: 'none' })
    return
  }

  loading.value = true
  try {
    const response = await getMedicalCaseDetail(caseId.value)
    if (response.code !== 0 || !response.result) {
      uni.showToast({ title: response.msg || '获取病例详情失败', icon: 'none' })
      return
    }
    detail.value = response.result
    const [original, heatmap] = await Promise.all([
      resolveImage(response.result.imageUrl),
      resolveImage(response.result.heatmapUrl),
    ])
    originalImage.value = original
    heatmapImage.value = heatmap
  } catch (error) {
    console.error(error)
    uni.showToast({ title: '获取病例详情失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const removeCase = () => {
  if (!caseId.value) return
  uni.showModal({
    title: '删除病例',
    content: '删除后，原始图像和热力图也会从云端清理。确认继续吗？',
    success: async (res) => {
      if (!res.confirm) return
      uni.showLoading({ title: '正在删除...' })
      try {
        const response = await deleteMedicalCase(caseId.value)
        if (response.code !== 0) {
          uni.showToast({ title: response.msg || '删除失败', icon: 'none' })
          return
        }
        uni.showToast({ title: '删除成功', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 500)
      } finally {
        uni.hideLoading()
      }
    },
  })
}

onMounted(fetchDetail)
</script>

<template>
  <view class="page">
    <view v-if="loading" class="state">加载中...</view>
    <view v-else-if="!detail" class="state">未找到病例数据</view>

    <view v-else class="content">
      <view class="header">
        <text class="title">病例详情</text>
        <text class="case-no">{{ detail.caseNo || `ID: ${detail.id}` }}</text>
      </view>

      <view v-if="originalImage || heatmapImage" class="section">
        <text class="section-title">局部证据匹配可视化</text>
        <view class="image-compare">
          <view class="image-item">
            <view class="image-frame">
              <image v-if="originalImage" :src="originalImage" mode="aspectFit" />
              <text v-else>暂无原图</text>
            </view>
            <text>原始图像</text>
          </view>
          <view class="image-item">
            <view class="image-frame">
              <image v-if="heatmapImage" :src="heatmapImage" mode="aspectFit" />
              <text v-else>暂无热力图</text>
            </view>
            <text>混合热力图</text>
          </view>
        </view>
      </view>

      <view class="section">
        <text class="section-title">基础信息</text>
        <view class="row"><text>患者</text><text>{{ detail.patientName || '--' }}</text></view>
        <view class="row"><text>状态</text><text>{{ statusText(detail.status) }}</text></view>
        <view class="row"><text>检测时间</text><text>{{ formatTime(detail.checkTime) }}</text></view>
        <view class="row"><text>诊断类型</text><text class="accent">{{ detail.diagnosedType || '--' }}</text></view>
      </view>

      <view class="section">
        <text class="section-title">诊疗信息</text>
        <view class="block"><text class="label">主诉</text><text>{{ detail.chiefComplaint || '无' }}</text></view>
        <view class="block"><text class="label">现病史</text><text>{{ detail.presentHistory || '无' }}</text></view>
        <view class="block"><text class="label">治疗史</text><text>{{ detail.treatmentHistory || '无' }}</text></view>
        <view class="block"><text class="label">病程</text><text>{{ detail.duration || '无' }}</text></view>
      </view>

      <view class="section">
        <text class="section-title">AI建议</text>
        <text class="long-text" user-select>{{ detail.aiAdvice || '暂无建议' }}</text>
      </view>

      <view class="section">
        <text class="section-title">疾病介绍</text>
        <text class="long-text" user-select>{{ detail.aiIntroduction || '暂无疾病介绍' }}</text>
      </view>

      <button class="delete-btn" @tap="removeCase">删除病例</button>
    </view>
  </view>
</template>

<style scoped lang="scss">
$theme: #8a2b31;

.page {
  min-height: 100vh;
  padding: 24rpx;
  box-sizing: border-box;
  background: #f5f2f2;
}

.state {
  padding: 100rpx 0;
  text-align: center;
  color: #888;
}

.header,
.section {
  margin-bottom: 22rpx;
  padding: 24rpx;
  border-radius: 18rpx;
  background: #fff;
}

.header {
  border-top: 8rpx solid $theme;
}

.title,
.case-no,
.section-title,
.label,
.long-text {
  display: block;
}

.title {
  font-size: 34rpx;
  font-weight: 700;
}

.case-no {
  margin-top: 8rpx;
  color: #888;
  font-size: 23rpx;
}

.section-title {
  margin-bottom: 18rpx;
  color: $theme;
  font-size: 29rpx;
  font-weight: 700;
}

.image-compare {
  display: flex;
  gap: 16rpx;
}

.image-item {
  width: calc(50% - 8rpx);
  text-align: center;
  color: #555;
  font-size: 24rpx;
}

.image-frame {
  width: 100%;
  height: 280rpx;
  margin-bottom: 10rpx;
  border: 2rpx solid #333;
  background: #f7f7f7;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
}

.image-frame image {
  width: 100%;
  height: 100%;
}

.row {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
  padding: 12rpx 0;
  border-bottom: 1rpx solid #eee;
  font-size: 26rpx;
}

.row:last-child {
  border-bottom: 0;
}

.accent {
  color: $theme;
  font-weight: 600;
}

.block {
  margin-bottom: 12rpx;
  padding: 16rpx;
  border-radius: 10rpx;
  background: #f8f6f6;
  font-size: 26rpx;
  line-height: 1.6;
}

.label {
  margin-bottom: 6rpx;
  color: #777;
  font-size: 23rpx;
}

.long-text {
  font-size: 26rpx;
  line-height: 1.75;
  white-space: pre-wrap;
}

.delete-btn {
  margin: 30rpx 0;
  color: #fff;
  background: $theme;
  border-radius: 14rpx;
  font-size: 29rpx;
}

.delete-btn::after {
  border: none;
}
</style>
