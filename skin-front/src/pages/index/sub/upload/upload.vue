<script setup lang="ts">
import { computed, ref } from 'vue'
import { analysReport, analysReportWithImage } from '@/services/reportService'
import { useReportStore } from '@/stores/modules/reportStore'

const store = useReportStore()
const imageList = ref<string[]>([])
const uploadProgress = ref(0)
const uploading = ref(false)

const previewImage = computed(() => (imageList.value.length > 0 ? imageList.value[0] : ''))
const progress = computed(() => uploadProgress.value)

function chooseImage() {
  uni.chooseImage({
    count: 1,
    sourceType: ['album', 'camera'],
    success: (res) => {
      const filePath = Array.isArray(res.tempFilePaths) ? res.tempFilePaths[0] : res.tempFilePaths
      imageList.value = [filePath]
      uploadProgress.value = 0
    },
  })
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

const isLocalFilePath = (path: string) => {
  if (!path) return false
  return (
    path.startsWith('wxfile://') ||
    path.startsWith('http://tmp/') ||
    path.startsWith('https://tmp/') ||
    path.startsWith('/')
  )
}

async function handleAnalyze() {
  if (!imageList.value[0]) {
    uni.showToast({ title: '请先上传图片', icon: 'none' })
    return
  }

  const localPath = imageList.value[0]
  store.setImageUrl(localPath)

  const data = store.getReport()
  const requestData = {
    ...data,
    checkTime: convertToISOTime(data.checkTime),
  }

  try {
    uploading.value = true
    uploadProgress.value = 0
    uni.showLoading({ title: '正在分析...' })
    const response = isLocalFilePath(localPath)
      ? await analysReportWithImage(localPath, requestData, (p) => {
          uploadProgress.value = Math.max(0, Math.min(100, Number(p || 0)))
        })
      : await analysReport(requestData)

    if (!response || response.code !== 0 || !response.result) {
      uni.hideLoading()
      uni.showToast({ title: response?.msg || '分析失败，请稍后重试', icon: 'none' })
      return
    }

    uploadProgress.value = 100
    store.setResult(response.result)
    uni.hideLoading()
    uni.navigateTo({ url: '/pages/index/sub/result/result' })
  } catch (error) {
    uni.hideLoading()
    uni.showToast({ title: '分析失败，请稍后重试', icon: 'none' })
    console.error(error)
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <view class="page">
    <view class="top-banner">
      <image class="banner-bg" src="/static/login/login-header.jpg" mode="aspectFill" />
      <view class="banner-mask"></view>
      <view class="banner-title-wrap">
        <text class="brand">灵镜智诊</text>
        <text class="title">上传照片</text>
      </view>
    </view>

    <view class="content">
      <view class="upload-card" @tap="chooseImage">
        <image v-if="previewImage" class="preview" :src="previewImage" mode="aspectFit" />
        <view v-else class="empty-upload">
          <text class="empty-plus">+</text>
          <text class="empty-text">点击上传皮肤照片</text>
        </view>
      </view>

      <view class="tips">
        <text>请保证对焦清晰</text>
        <text>避免反光</text>
        <text>包含皮损全貌 + 少量正常皮肤对照</text>
      </view>

      <view class="progress-text">已上传{{ progress }}%</view>
      <view class="progress-track">
        <view class="progress-fill" :style="{ width: progress + '%' }"></view>
      </view>

      <button class="submit" :disabled="uploading" @tap="handleAnalyze">上传并查看结果</button>
    </view>
  </view>
</template>

<style scoped lang="scss">
$theme: #8a2b31;

.page {
  min-height: 100vh;
  background: #f5f5f7;
}

.top-banner {
  position: relative;
  height: 220rpx;
  background: linear-gradient(120deg, #7f1f28, $theme);
  overflow: hidden;
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

.banner-title-wrap {
  position: relative;
  z-index: 2;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  gap: 10rpx;
}

.brand {
  font-size: 40rpx;
  font-weight: 700;
}

.title {
  font-size: 34rpx;
  font-weight: 600;
}

.content {
  padding: 20rpx 22rpx 26rpx;
}

.upload-card {
  border: 2rpx solid #2f2f2f;
  border-radius: 10rpx;
  background: #fff;
  height: 560rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview {
  width: 100%;
  height: 100%;
}

.empty-upload {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14rpx;
  color: #8c8c92;
}

.empty-plus {
  font-size: 72rpx;
  line-height: 1;
  color: #9d9da3;
}

.empty-text {
  font-size: 30rpx;
}

.tips {
  margin-top: 18rpx;
  color: #58585e;
  font-size: 34rpx;
  line-height: 1.5;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.progress-text {
  margin-top: 16rpx;
  text-align: center;
  color: #6f6f75;
  font-size: 30rpx;
}

.progress-track {
  margin-top: 10rpx;
  width: 100%;
  height: 14rpx;
  border: 1rpx solid #b8b8bd;
  border-radius: 8rpx;
  background: #f0f0f2;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #b22c36;
}

.submit {
  margin-top: 24rpx;
  width: 100%;
  height: 92rpx;
  line-height: 92rpx;
  border-radius: 12rpx;
  background: #a12631;
  color: #fff;
  font-size: 44rpx;
  font-weight: 700;
  border: none;
}

.submit::after {
  border: none;
}

.submit[disabled] {
  opacity: 0.65;
}
</style>
