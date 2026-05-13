<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import { getMedicalCases, type MedicalCaseItem } from '@/services/medicalCaseService'

const loading = ref(false)
const keyword = ref('')
const list = ref<MedicalCaseItem[]>([])

const filteredList = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  if (!key) return list.value
  return list.value.filter((item) => {
    const text = `${item.patientName || ''} ${item.caseNo || ''} ${item.diagnosedType || ''} ${item.chiefComplaint || ''}`.toLowerCase()
    return text.includes(key)
  })
})

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

const fetchCases = async () => {
  loading.value = true
  try {
    const res = await getMedicalCases()
    if (res.code === 0 && Array.isArray(res.result)) {
      list.value = res.result
    } else {
      uni.showToast({ title: res.msg || '获取诊疗记录失败', icon: 'none' })
    }
  } catch {
    uni.showToast({ title: '获取诊疗记录失败', icon: 'none' })
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
}

const openDetail = (id: number) => {
  uni.navigateTo({ url: `/pages/my/sub/reportDetail/reportDetail?id=${id}` })
}

onPullDownRefresh(fetchCases)
onMounted(fetchCases)
</script>

<template>
  <view class="page">
    <view class="header">
      <text class="title">诊疗记录</text>
    </view>

    <view class="search">
      <input v-model="keyword" class="search-input" placeholder="搜索患者/病历号/诊断类型" />
    </view>

    <view v-if="loading" class="state">加载中...</view>
    <view v-else-if="filteredList.length === 0" class="state">暂无诊疗记录</view>

    <scroll-view v-else scroll-y class="list">
      <view
        v-for="item in filteredList"
        :key="item.id"
        class="card"
        @tap="openDetail(item.id)"
      >
        <view class="row">
          <text class="name">{{ item.patientName || '未命名患者' }}</text>
          <text class="status">{{ statusText(item.status) }}</text>
        </view>
        <text class="case-no">病历号：{{ item.caseNo || '--' }}</text>
        <text class="type">诊断类型：{{ item.diagnosedType || '未诊断' }}</text>
        <text class="complaint">主诉：{{ item.chiefComplaint || '无' }}</text>
        <text class="time">就诊时间：{{ formatTime(item.checkTime || item.createdAt) }}</text>
      </view>
    </scroll-view>
  </view>
</template>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: #f5f5f7;
  padding: 20rpx;
  box-sizing: border-box;
}

.header {
  margin-bottom: 16rpx;
}

.title {
  font-size: 34rpx;
  font-weight: 700;
  color: #222;
}

.search {
  margin-bottom: 16rpx;
}

.search-input {
  height: 72rpx;
  border: 1rpx solid #d8d8dd;
  border-radius: 12rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
  background: #fff;
}

.state {
  text-align: center;
  color: #888;
  font-size: 28rpx;
  padding: 80rpx 0;
}

.list {
  height: calc(100vh - 180rpx);
}

.card {
  background: #fff;
  border: 1rpx solid #e0e0e6;
  border-radius: 14rpx;
  padding: 18rpx;
  margin-bottom: 14rpx;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.name {
  font-size: 30rpx;
  font-weight: 700;
  color: #8a2b31;
}

.status {
  font-size: 24rpx;
  color: #666;
}

.case-no,
.type,
.complaint,
.time {
  font-size: 26rpx;
  color: #444;
}
</style>
