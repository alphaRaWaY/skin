<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getMedicalCases, type MedicalCaseItem } from '@/services/medicalCaseService'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'

type SortType = 'desc' | 'asc'

const list = ref<MedicalCaseItem[]>([])
const loading = ref(false)
const keyword = ref('')
const sortType = ref<SortType>('desc')
const statusFilter = ref('ALL')

const statusLabel = computed(() => {
  if (statusFilter.value === 'FOLLOWUP') return '待复查'
  if (statusFilter.value === 'PENDING') return '待处理'
  return '全部'
})

const displayList = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  let arr = [...list.value]
  if (text) {
    arr = arr.filter((x) => {
      const mix = `${x.patientName || ''} ${x.chiefComplaint || ''} ${x.diagnosedType || ''} ${x.caseNo || ''}`.toLowerCase()
      return mix.includes(text)
    })
  }
  arr.sort((a, b) => {
    const ta = new Date(a.checkTime || a.createdAt || 0).getTime()
    const tb = new Date(b.checkTime || b.createdAt || 0).getTime()
    return sortType.value === 'desc' ? tb - ta : ta - tb
  })
  return arr
})

const fetchList = async () => {
  loading.value = true
  try {
    const status = statusFilter.value === 'ALL' ? undefined : statusFilter.value
    const res = await getMedicalCases({ status, keyword: keyword.value.trim() || undefined })
    if (res.code === 0 && Array.isArray(res.result)) {
      list.value = res.result
    } else {
      uni.showToast({ title: res.msg || '获取记录失败', icon: 'none' })
    }
  } catch {
    uni.showToast({ title: '获取记录失败', icon: 'none' })
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
}

const applyStatusFromStorage = () => {
  const s = uni.getStorageSync('caseStatusFilter')
  if (s === 'FOLLOWUP' || s === 'PENDING' || s === 'ALL') {
    statusFilter.value = s
  } else {
    statusFilter.value = 'ALL'
  }
  uni.removeStorageSync('caseStatusFilter')
}

const openDetail = (id: number) => {
  uni.navigateTo({ url: `/pages/my/sub/reportDetail/reportDetail?id=${id}` })
}

const toggleSort = () => {
  sortType.value = sortType.value === 'desc' ? 'asc' : 'desc'
}

const onPickStatus = () => {
  const itemList = ['全部', '待处理', '待复查']
  uni.showActionSheet({
    itemList,
    success: ({ tapIndex }) => {
      statusFilter.value = tapIndex === 1 ? 'PENDING' : tapIndex === 2 ? 'FOLLOWUP' : 'ALL'
      fetchList()
    },
  })
}

const fmtTime = (t: string) => {
  if (!t) return '--'
  const d = new Date(t)
  if (Number.isNaN(d.getTime())) return t
  const m = `${d.getMonth() + 1}`.padStart(2, '0')
  const day = `${d.getDate()}`.padStart(2, '0')
  const hh = `${d.getHours()}`.padStart(2, '0')
  const mm = `${d.getMinutes()}`.padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day} ${hh}:${mm}`
}

onPullDownRefresh(fetchList)

onShow(() => {
  applyStatusFromStorage()
  fetchList()
})

onMounted(() => {
  applyStatusFromStorage()
  fetchList()
})
</script>

<template>
  <view class="page">
    <view class="top-banner">
      <image class="banner-bg" src="/static/login/login-header.jpg" mode="aspectFill" />
      <view class="banner-mask"></view>
      <view class="banner-title-wrap">
        <text class="brand">灵镜智诊</text>
        <text class="title">诊疗记录</text>
      </view>
    </view>

    <view class="toolbar">
      <view class="sort-box" @tap="toggleSort">
        <text class="label">时间</text>
        <text class="value">{{ sortType === 'desc' ? '倒序' : '正序' }}</text>
      </view>
      <view class="type-box" @tap="onPickStatus">
        <text class="label">状态</text>
        <text class="value ellipsis">{{ statusLabel }}</text>
      </view>
      <view class="search-box">
        <input v-model="keyword" class="search-input" placeholder="搜索患者/主诉/病例号" @confirm="fetchList" />
      </view>
    </view>

    <view v-if="loading" class="state">加载中...</view>
    <view v-else-if="displayList.length === 0" class="state">暂无诊疗记录</view>

    <scroll-view v-else class="list" scroll-y>
      <view class="grid">
        <view
          v-for="item in displayList"
          :key="item.id"
          class="card"
          @tap="openDetail(item.id)"
        >
          <text class="name">{{ item.patientName || '未命名患者' }}</text>
          <text class="meta">病例号 {{ item.caseNo || '--' }}</text>
          <text class="type">{{ item.diagnosedType || item.status || '未分类' }}</text>
          <text class="symptom">{{ item.chiefComplaint || '无主诉描述' }}</text>
          <text class="time">{{ fmtTime(item.checkTime || item.createdAt) }}</text>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<style scoped lang="scss">
$theme: #8a2b31;

.page { min-height: 100vh; background: #f4f4f6; }
.top-banner { position: relative; height: 220rpx; background: linear-gradient(120deg, #7f1f28, $theme); overflow: hidden; }
.banner-bg { position: absolute; inset: 0; width: 100%; height: 100%; opacity: 0.2; }
.banner-mask { position: absolute; inset: 0; background: rgba(0, 0, 0, 0.08); }
.banner-title-wrap { position: relative; z-index: 2; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #fff; gap: 12rpx; }
.brand { font-size: 42rpx; font-weight: 700; }
.title { font-size: 38rpx; font-weight: 600; }
.toolbar { display: grid; grid-template-columns: 150rpx 150rpx 1fr; gap: 16rpx; padding: 18rpx 22rpx; align-items: center; }
.sort-box,.type-box { height: 84rpx; border: 1px solid #ddd; border-radius: 14rpx; background: #fff; display: flex; flex-direction: column; justify-content: center; padding: 0 14rpx; }
.label { font-size: 22rpx; color: #666; }
.value { font-size: 24rpx; color: #222; }
.ellipsis { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.search-box { height: 84rpx; border: 1px solid #ddd; border-radius: 14rpx; background: #fff; padding: 0 18rpx; display: flex; align-items: center; }
.search-input { width: 100%; font-size: 28rpx; }
.state { text-align: center; color: #888; padding: 60rpx 0; font-size: 28rpx; }
.list { height: calc(100vh - 322rpx - var(--window-bottom)); padding: 0 22rpx 24rpx; box-sizing: border-box; }
.grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16rpx; }
.card { background: #fff; border: 1px solid #ddd; border-radius: 12rpx; padding: 16rpx; display: flex; flex-direction: column; gap: 8rpx; }
.name { font-size: 38rpx; color: $theme; font-weight: 700; }
.meta { font-size: 28rpx; color: #303030; font-weight: 600; }
.type { font-size: 30rpx; color: #555; }
.symptom { font-size: 26rpx; color: #666; line-height: 1.4; min-height: 72rpx; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.time { font-size: 26rpx; color: #8a8a8a; }
</style>

