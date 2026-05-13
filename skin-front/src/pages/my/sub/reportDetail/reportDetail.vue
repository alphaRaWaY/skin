<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { deleteMedicalCase, getMedicalCaseDetail, type MedicalCaseItem } from '@/services/medicalCaseService'

const loading = ref(false)
const caseId = ref<number>(0)
const detail = ref<MedicalCaseItem | null>(null)

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
  const y = date.getFullYear()
  const m = `${date.getMonth() + 1}`.padStart(2, '0')
  const d = `${date.getDate()}`.padStart(2, '0')
  const hh = `${date.getHours()}`.padStart(2, '0')
  const mm = `${date.getMinutes()}`.padStart(2, '0')
  return `${y}-${m}-${d} ${hh}:${mm}`
}

const statusText = (status?: string) => {
  if (status === 'PENDING') return '待处理'
  if (status === 'IN_PROGRESS') return '进行中'
  if (status === 'FOLLOWUP') return '待复查'
  if (status === 'DONE') return '已完成'
  if (status === 'CLOSED') return '已关闭'
  return status || '--'
}

const fetchDetail = async () => {
  caseId.value = getPageId()
  if (!caseId.value) {
    uni.showToast({ title: '缺少病历ID', icon: 'none' })
    return
  }
  loading.value = true
  try {
    const res = await getMedicalCaseDetail(caseId.value)
    if (res.code === 0 && res.result) {
      detail.value = res.result
    } else {
      uni.showToast({ title: res.msg || '获取病历详情失败', icon: 'none' })
    }
  } catch {
    uni.showToast({ title: '获取病历详情失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const removeCase = () => {
  if (!caseId.value) return
  uni.showModal({
    title: '删除病历',
    content: '确认删除该病历吗？',
    success: async (res) => {
      if (!res.confirm) return
      uni.showLoading({ title: '删除中...' })
      try {
        const ret = await deleteMedicalCase(caseId.value)
        if (ret.code === 0) {
          uni.showToast({ title: '删除成功', icon: 'success' })
          setTimeout(() => uni.navigateBack(), 600)
        } else {
          uni.showToast({ title: ret.msg || '删除失败', icon: 'none' })
        }
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
    <view v-else-if="!detail" class="state">未找到病历数据</view>

    <view v-else class="content">
      <view class="header">
        <text class="title">病历详情</text>
        <text class="sub">ID: {{ detail.id }}</text>
      </view>

      <view class="section">
        <text class="section-title">基础信息</text>
        <view class="item"><text class="label">患者</text><text class="value">{{ detail.patientName || '--' }}</text></view>
        <view class="item"><text class="label">病历号</text><text class="value">{{ detail.caseNo || '--' }}</text></view>
        <view class="item"><text class="label">状态</text><text class="value">{{ statusText(detail.status) }}</text></view>
        <view class="item"><text class="label">就诊时间</text><text class="value">{{ formatTime(detail.checkTime) }}</text></view>
        <view class="item"><text class="label">创建时间</text><text class="value">{{ formatTime(detail.createdAt) }}</text></view>
      </view>

      <view class="section">
        <text class="section-title">诊疗信息</text>
        <view class="block"><text class="block-label">主诉</text><text class="block-text">{{ detail.chiefComplaint || '无' }}</text></view>
        <view class="block"><text class="block-label">现病史</text><text class="block-text">{{ detail.presentHistory || '无' }}</text></view>
        <view class="block"><text class="block-label">治疗史</text><text class="block-text">{{ detail.treatmentHistory || '无' }}</text></view>
        <view class="item"><text class="label">病程</text><text class="value">{{ detail.duration || '--' }}</text></view>
        <view class="item"><text class="label">诊断类型</text><text class="value">{{ detail.diagnosedType || '--' }}</text></view>
      </view>

      <view class="section">
        <text class="section-title">AI结果</text>
        <view class="block"><text class="block-label">AI建议</text><text class="block-text">{{ detail.aiAdvice || '暂无' }}</text></view>
        <view class="block"><text class="block-label">疾病介绍</text><text class="block-text">{{ detail.aiIntroduction || '暂无' }}</text></view>
      </view>

      <view class="section" v-if="detail.extraNotes">
        <text class="section-title">备注</text>
        <view class="block"><text class="block-text">{{ detail.extraNotes }}</text></view>
      </view>

      <button class="delete-btn" @tap="removeCase">删除病历</button>
    </view>
  </view>
</template>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: #f5f5f7;
  padding: 20rpx;
  box-sizing: border-box;
}

.state {
  text-align: center;
  color: #888;
  font-size: 28rpx;
  padding: 80rpx 0;
}

.content {
  background: #fff;
  border-radius: 14rpx;
  padding: 20rpx;
}

.header {
  margin-bottom: 20rpx;
  padding-bottom: 12rpx;
  border-bottom: 1rpx solid #eee;
}

.title {
  display: block;
  font-size: 34rpx;
  font-weight: 700;
  color: #222;
}

.sub {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #888;
}

.section {
  margin-bottom: 20rpx;
}

.section-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: #8a2b31;
  margin-bottom: 10rpx;
}

.item {
  display: flex;
  margin-bottom: 8rpx;
}

.label {
  width: 150rpx;
  font-size: 26rpx;
  color: #666;
}

.value {
  flex: 1;
  font-size: 26rpx;
  color: #222;
}

.block {
  background: #f8f8fb;
  border-radius: 10rpx;
  padding: 14rpx;
  margin-bottom: 10rpx;
}

.block-label {
  display: block;
  font-size: 24rpx;
  color: #777;
  margin-bottom: 4rpx;
}

.block-text {
  font-size: 26rpx;
  color: #333;
  line-height: 1.5;
  user-select: text;
}

.delete-btn {
  margin-top: 24rpx;
  background: #b3353c;
  color: #fff;
  border: none;
  border-radius: 12rpx;
  font-size: 30rpx;
}

.delete-btn::after {
  border: none;
}
</style>
