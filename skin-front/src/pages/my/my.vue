<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useAuthorizationStore } from '@/stores'
import { getOssImageUrl } from '@/services/accountService'
import { getDashboardSummary } from '@/services/dashboardService'

const auth = useAuthorizationStore()

const doctorName = computed(() => auth.profile.nickname || auth.profile.username || 'XXX医生')
const avatarRenderUrl = ref('/static/design/设计素材/医生头像.png')
const avatarUrl = computed(() => avatarRenderUrl.value)
const deptText = computed(() => (auth.profile.mobile ? `手机号：${auth.profile.mobile}` : '手机号：'))
const titleText = computed(() => (auth.profile.jobNumber ? `医工号：${auth.profile.jobNumber}` : '医工号：'))
const todayDiagnosed = ref(0)

const resolveAvatar = async () => {
  const raw = auth.profile.avatar || ''
  if (!raw) {
    avatarRenderUrl.value = '/static/design/设计素材/医生头像.png'
    return
  }
  if (raw.startsWith('http://') || raw.startsWith('https://')) {
    avatarRenderUrl.value = raw
    return
  }
  try {
    const res = await getOssImageUrl(raw)
    avatarRenderUrl.value = res.code === 0 && res.result ? res.result : '/static/design/设计素材/医生头像.png'
  } catch {
    avatarRenderUrl.value = '/static/design/设计素材/医生头像.png'
  }
}

watch(
  () => auth.profile.avatar,
  () => {
    resolveAvatar()
  },
  { immediate: true },
)

const fetchDashboard = async () => {
  try {
    const res = await getDashboardSummary()
    if (res.code === 0 && res.result) {
      todayDiagnosed.value = Number(res.result.todayDiagnosed || 0)
    } else {
      todayDiagnosed.value = 0
    }
  } catch {
    todayDiagnosed.value = 0
  }
}

fetchDashboard()

const goNewDiagnosis = () => {
  uni.switchTab({ url: '/pages/index/index' })
}

const goPendingCases = () => {
  uni.setStorageSync('caseStatusFilter', 'PENDING')
  uni.switchTab({ url: '/pages/case/records' })
}

const goHistoryCases = () => {
  uni.setStorageSync('caseStatusFilter', 'ALL')
  uni.switchTab({ url: '/pages/case/records' })
}

const goFollowupCases = () => {
  uni.setStorageSync('caseStatusFilter', 'FOLLOWUP')
  uni.switchTab({ url: '/pages/case/records' })
}

const openAnnouncement = () => {
  uni.showToast({ title: '暂无此功能', icon: 'none' })
}

const editProfile = () => {
  uni.navigateTo({ url: '/pages/my/sub/settings' })
}
</script>

<template>
  <view class="page">
    <view class="top-banner">
      <image class="banner-bg" src="/static/login/login-header.jpg" mode="aspectFill" />
      <view class="banner-mask"></view>
      <text class="title">灵镜智诊</text>
    </view>

    <view class="profile-card">
      <image class="avatar" :src="avatarUrl" mode="aspectFill" />
      <view class="profile-main">
        <text class="name">{{ doctorName }}，您好</text>
        <view class="meta-row"><text>{{ deptText }}</text><text>{{ titleText }}</text></view>
        <view class="meta-row">
          <text>在线状态：</text>
          <text class="edit" @tap="editProfile">修改信息 ></text>
        </view>
      </view>
    </view>

    <view class="card stat-card">
      <text class="card-title">今日统计</text>
      <text class="stat-text">今日诊断数：<text class="accent">{{ todayDiagnosed }}</text></text>
    </view>

    <view class="card announce-card">
      <text class="card-title">公告</text>
      <view class="announce-item" @tap="openAnnouncement">
        <text>医院通知</text>
        <text class="arrow">></text>
      </view>
      <view class="divider"></view>
      <view class="announce-item" @tap="openAnnouncement">
        <text>皮肤镜诊断规范更新</text>
        <text class="arrow">></text>
      </view>
    </view>

    <view class="section-title">诊疗数据</view>
    <view class="grid">
      <view class="grid-item" @tap="goNewDiagnosis">
        <image class="grid-icon" src="/static/design/设计素材/诊疗数据“新建皮肤镜诊断”图标.png" mode="aspectFit" />
        <text>新建皮肤镜诊断</text>
      </view>
      <view class="grid-item" @tap="goPendingCases">
        <image class="grid-icon" src="/static/design/设计素材/诊疗数据“待处理病例”图标.png" mode="aspectFit" />
        <text>查看待处理病例</text>
      </view>
      <view class="grid-item" @tap="goHistoryCases">
        <image class="grid-icon" src="/static/design/设计素材/诊疗数据“历史病例”图标.png" mode="aspectFit" />
        <text>历史病例</text>
      </view>
      <view class="grid-item" @tap="goFollowupCases">
        <image class="grid-icon" src="/static/design/设计素材/诊疗数据“待复查病例”图标.png" mode="aspectFit" />
        <text>待复查病例</text>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
$theme: #8a2b31;

.page {
  min-height: 100vh;
  background: #f7f7f8;
  padding-bottom: 24rpx;
}

.top-banner {
  position: relative;
  height: 220rpx;
  overflow: hidden;
  background: linear-gradient(120deg, #7f1f28, $theme);
  display: flex;
  align-items: center;
  justify-content: center;
}

.banner-bg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 0.2;
}

.banner-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.08);
}

.title {
  position: relative;
  z-index: 2;
  color: #fff;
  font-size: 38rpx;
  font-weight: 700;
}

.profile-card {
  margin: 18rpx 22rpx;
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.avatar {
  width: 132rpx;
  height: 132rpx;
  border-radius: 66rpx;
  border: 2rpx solid #ddd;
  background: #fff;
}

.profile-main {
  flex: 1;
}

.name {
  font-size: 46rpx;
  font-weight: 700;
  color: #171717;
  margin-bottom: 8rpx;
}

.meta-row {
  display: flex;
  justify-content: space-between;
  font-size: 34rpx;
  color: #323232;
}

.edit {
  color: #555;
}

.card {
  margin: 0 22rpx 18rpx;
  background: #f8f8f8;
  border: 2rpx solid #2a2a2a;
  border-radius: 28rpx;
  box-shadow: 0 6rpx 12rpx rgba(0, 0, 0, 0.08);
  padding: 18rpx 24rpx;
}

.card-title {
  display: block;
  text-align: center;
  font-size: 46rpx;
  font-weight: 700;
  margin-bottom: 10rpx;
}

.stat-text {
  display: block;
  text-align: center;
  font-size: 40rpx;
  color: #333;
}

.accent {
  color: $theme;
}

.announce-item {
  height: 76rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 38rpx;
  color: #333;
}

.arrow {
  color: #989898;
}

.divider {
  border-top: 1px solid #cfcfcf;
}

.section-title {
  margin: 8rpx 22rpx 12rpx;
  font-size: 46rpx;
  font-weight: 700;
  color: #121212;
}

.grid {
  margin: 0 22rpx;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14rpx;
}

.grid-item {
  min-height: 164rpx;
  border: 2rpx solid #333;
  border-radius: 26rpx;
  background: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  font-size: 36rpx;
  color: #2f2f2f;
}

.grid-icon {
  width: 54rpx;
  height: 54rpx;
}
</style>
