<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import { createPatient, deletePatient, getPatients } from '@/services/patientService'
import type { Patient } from '@/types/patient'

const keyword = ref('')
const loading = ref(false)
const patients = ref<Patient[]>([])

const filtered = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  if (!k) return patients.value
  return patients.value.filter((p) => {
    return `${p.patientName || ''} ${p.phone || ''}`.toLowerCase().includes(k)
  })
})

const goBack = () => {
  uni.navigateBack({
    fail: () => {
      uni.switchTab({ url: '/pages/my/my' })
    },
  })
}

const fetchPatients = async () => {
  loading.value = true
  try {
    const res = await getPatients(keyword.value.trim())
    if (res.code === 0) {
      patients.value = res.result || []
    } else {
      uni.showToast({ title: res.msg || '获取患者失败', icon: 'none' })
    }
  } catch {
    uni.showToast({ title: '获取患者失败', icon: 'none' })
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
}

const quickAddPatient = () => {
  uni.showModal({
    title: '新增患者',
    editable: true,
    placeholderText: '请输入患者姓名',
    success: async (res) => {
      const name = (res.content || '').trim()
      if (!res.confirm || !name) return
      try {
        const r = await createPatient({ patientName: name, gender: '男', age: 0 })
        if (r.code === 0) {
          uni.showToast({ title: '新增成功', icon: 'success' })
          fetchPatients()
        } else {
          uni.showToast({ title: r.msg || '新增失败', icon: 'none' })
        }
      } catch {
        uni.showToast({ title: '新增失败', icon: 'none' })
      }
    },
  })
}

const removePatient = (p: Patient) => {
  uni.showModal({
    title: '删除患者',
    content: `确认删除 ${p.patientName || '该患者'} 吗？`,
    confirmColor: '#a12631',
    success: async (res) => {
      if (!res.confirm) return
      try {
        const r = await deletePatient(p.id)
        if (r.code === 0) {
          uni.showToast({ title: '删除成功', icon: 'success' })
          fetchPatients()
        } else {
          uni.showToast({ title: r.msg || '删除失败', icon: 'none' })
        }
      } catch {
        uni.showToast({ title: '删除失败', icon: 'none' })
      }
    },
  })
}

onPullDownRefresh(fetchPatients)
onMounted(fetchPatients)
</script>

<template>
  <view class="page">
    <view class="top-banner">
      <image class="banner-bg" src="/static/login/login-header.jpg" mode="aspectFill" />
      <view class="banner-mask"></view>
      <view class="banner-bar">
        <text class="back" @tap="goBack">‹</text>
        <view class="banner-title-wrap">
          <text class="brand">灵镜智诊</text>
          <text class="title">患者管理</text>
        </view>
        <view class="placeholder"></view>
      </view>
    </view>

    <view class="toolbar">
      <input v-model="keyword" class="search" placeholder="搜索患者姓名/手机号" @confirm="fetchPatients" />
      <button class="add-btn" @tap="quickAddPatient">新增</button>
    </view>

    <view v-if="loading" class="state">加载中...</view>
    <view v-else-if="filtered.length === 0" class="state">暂无患者</view>

    <scroll-view v-else class="list" scroll-y>
      <view v-for="p in filtered" :key="p.id" class="card">
        <view class="line1">
          <text class="name">{{ p.patientName || '未命名患者' }}</text>
          <text class="meta">{{ p.gender || '--' }} {{ p.age || 0 }}岁</text>
        </view>
        <text class="phone">{{ p.phone || '未填写手机号' }}</text>
        <view class="actions">
          <text class="danger" @tap="removePatient(p)">删除</text>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<style scoped lang="scss">
$theme: #8a2b31;
.page { min-height: 100vh; background: #f5f5f7; }
.top-banner { position: relative; height: 220rpx; overflow: hidden; background: linear-gradient(120deg, #7f1f28, $theme); }
.banner-bg { position: absolute; inset: 0; width: 100%; height: 100%; opacity: 0.2; }
.banner-mask { position: absolute; inset: 0; background: rgba(0, 0, 0, 0.08); }
.banner-bar { position: relative; z-index: 2; height: 100%; display: grid; grid-template-columns: 64rpx 1fr 64rpx; align-items: center; padding: 0 20rpx; }
.back { color: #fff; font-size: 56rpx; line-height: 1; }
.placeholder { width: 64rpx; }
.banner-title-wrap { display: flex; flex-direction: column; align-items: center; justify-content: center; color: #fff; gap: 10rpx; }
.brand { font-size: 40rpx; font-weight: 700; }
.title { font-size: 34rpx; font-weight: 600; }
.toolbar { padding: 18rpx 22rpx; display: grid; grid-template-columns: 1fr 140rpx; gap: 12rpx; }
.search { height: 74rpx; border: 1rpx solid #d6d6db; border-radius: 12rpx; background: #fff; padding: 0 18rpx; font-size: 28rpx; }
.add-btn { height: 74rpx; line-height: 74rpx; margin: 0; padding: 0; border: none; border-radius: 12rpx; background: $theme; color: #fff; font-size: 30rpx; }
.add-btn::after { border: none; }
.state { text-align: center; color: #888; padding: 80rpx 0; font-size: 28rpx; }
.list { height: calc(100vh - 330rpx - var(--window-bottom)); padding: 0 22rpx 22rpx; box-sizing: border-box; }
.card { background: #fff; border: 1rpx solid #e5e5ea; border-radius: 12rpx; padding: 16rpx; margin-bottom: 12rpx; }
.line1 { display: flex; justify-content: space-between; align-items: center; }
.name { font-size: 32rpx; font-weight: 700; color: #202020; }
.meta { font-size: 26rpx; color: #666; }
.phone { margin-top: 8rpx; font-size: 26rpx; color: #777; }
.actions { margin-top: 8rpx; display: flex; justify-content: flex-end; }
.danger { color: #b22c36; font-size: 26rpx; }
</style>
