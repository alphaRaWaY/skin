<script setup lang="ts">
import { ref } from 'vue'
import { analysReport, analysReportWithImage } from '@/services/reportService'
import { useReportStore } from '@/stores/modules/reportStore'

const store = useReportStore()
const imageList = ref<string[]>([])
const defaultImage = '/static/images/default.png'

function chooseImage() {
  uni.chooseImage({
    count: 1,
    sourceType: ['album', 'camera'],
    success: (res) => {
      const filePath = Array.isArray(res.tempFilePaths) ? res.tempFilePaths[0] : res.tempFilePaths
      imageList.value = [filePath]
    }
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
  const localPath = imageList.value[0] || defaultImage
  store.setImageUrl(localPath)

  const data = store.getReport()
  const requestData = {
    ...data,
    checkTime: convertToISOTime(data.checkTime)
  }

  try {
    uni.showLoading({ title: '正在分析...' })
    const response = isLocalFilePath(localPath)
      ? await analysReportWithImage(localPath, requestData)
      : await analysReport(requestData)

    if (!response || response.code !== 0 || !response.result) {
      uni.hideLoading()
      uni.showToast({ title: response?.msg || '分析失败，请检查模型服务连接', icon: 'none' })
      return
    }

    store.setResult(response.result)
    uni.hideLoading()
    uni.navigateTo({ url: '/pages/index/sub/result/result' })
  } catch (error) {
    uni.hideLoading()
    uni.showToast({ title: '分析失败，请稍后重试', icon: 'none' })
    console.error(error)
  }
}
</script>

<template>
  <view class="container">
    <button @click="chooseImage">选择图片</button>
    <view>
      <image
        :src="imageList.length > 0 ? imageList[0] : defaultImage"
        style="width: 100%; height: 500rpx"
        mode="aspectFit"
        @click="chooseImage"
      />
    </view>
    <button type="primary" @click="handleAnalyze">开始检测</button>
  </view>
</template>

<style scoped>
.container {
  padding: 20rpx;
}
</style>

