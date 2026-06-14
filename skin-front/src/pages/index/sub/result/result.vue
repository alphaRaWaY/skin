<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useReportStore } from '@/stores/modules/reportStore'
import {
  deleteUploadedImage,
  getImage,
  postReport,
  uploadImageToServer,
} from '@/services/reportService'

const report = useReportStore()
const originalImage = ref('')
const heatmapImage = ref('')
const saving = ref(false)

const isLocalPath = (value: string) => {
  if (!value) return false
  return (
    value.startsWith('wxfile://') ||
    value.startsWith('http://tmp/') ||
    value.startsWith('https://tmp/') ||
    value.startsWith('file://') ||
    value.startsWith('/')
  )
}

const formatTime = computed(() => {
  const raw = report.form.checkTime
  const date = raw ? new Date(raw) : new Date()
  if (Number.isNaN(date.getTime())) return raw || '--'
  const pad = (value: number) => `${value}`.padStart(2, '0')
  return `${date.getFullYear()}/${pad(date.getMonth() + 1)}/${pad(date.getDate())} ${pad(
    date.getHours(),
  )}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
})

const topConcepts = computed(() =>
  (report.resultValue.conceptScores || [])
    .slice()
    .sort((a, b) => (a.rankNo || 0) - (b.rankNo || 0))
    .slice(0, 3)
    .map((item) => item.conceptNameCn || item.conceptNameEn || `概念 ${item.conceptIndex}`),
)

const heatmapSubtitle = computed(() =>
  topConcepts.value.length
    ? `Top3：${topConcepts.value.join('、')}`
    : 'Top3 概念加权',
)

const convertToISOTime = (value: string) => {
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/.test(value)) return value
  const date = new Date(value)
  return (Number.isNaN(date.getTime()) ? new Date() : date).toISOString().split('.')[0]
}

const resolveImage = async (value: string) => {
  if (!value || isLocalPath(value)) return value
  const response = await getImage(value)
  return response.result || ''
}

const writeHeatmapToLocalFile = () =>
  new Promise<string>((resolve, reject) => {
    if (!report.heatmapBase64) {
      resolve('')
      return
    }
    const runtime = globalThis as any
    const userDataPath =
      runtime.wx?.env?.USER_DATA_PATH || runtime.uni?.env?.USER_DATA_PATH
    if (!userDataPath) {
      reject(new Error('当前环境不支持临时文件写入'))
      return
    }
    const filePath = `${userDataPath}/skin-heatmap-${Date.now()}.png`
    uni.getFileSystemManager().writeFile({
      filePath,
      data: report.heatmapBase64,
      encoding: 'base64',
      success: () => resolve(filePath),
      fail: reject,
    })
  })

const abandonResult = () => {
  uni.showModal({
    title: '放弃结果',
    content: '本次结果尚未保存，确认返回首页吗？',
    success: (res) => {
      if (!res.confirm) return
      report.reset()
      uni.switchTab({ url: '/pages/index/index' })
    },
  })
}

const rollbackUploads = async (keys: string[]) => {
  await Promise.allSettled(keys.filter(Boolean).map((key) => deleteUploadedImage(key)))
}

const saveReport = async () => {
  if (saving.value) return
  saving.value = true
  const uploadedKeys: string[] = []

  try {
    uni.showLoading({ title: '正在保存...' })
    const data = report.getReport()
    let imageUrl = data.imageUrl
    let heatmapUrl = data.heatmapUrl || heatmapImage.value

    if (isLocalPath(imageUrl)) {
      const upload = await uploadImageToServer(imageUrl)
      if (!upload.result) throw new Error('原始图像上传失败')
      imageUrl = upload.result
      uploadedKeys.push(imageUrl)
    }

    if (isLocalPath(heatmapUrl)) {
      const upload = await uploadImageToServer(heatmapUrl)
      if (!upload.result) throw new Error('热力图上传失败')
      heatmapUrl = upload.result
      uploadedKeys.push(heatmapUrl)
    }

    const response = await postReport({
      ...data,
      imageUrl,
      heatmapUrl,
      heatmapBase64: undefined,
      checkTime: convertToISOTime(data.checkTime),
    })
    if (!response || response.code !== 0) {
      throw new Error(response?.msg || '保存失败')
    }

    report.setImageUrl(imageUrl)
    report.setHeatmapUrl(heatmapUrl)
    uni.hideLoading()
    uni.showToast({ title: '保存成功', icon: 'success' })
    setTimeout(() => uni.switchTab({ url: '/pages/case/records' }), 500)
  } catch (error) {
    await rollbackUploads(uploadedKeys)
    uni.hideLoading()
    uni.showToast({
      title: error instanceof Error ? error.message : '保存失败',
      icon: 'none',
    })
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  try {
    originalImage.value = await resolveImage(report.imageUrl)
    if (report.heatmapUrl) {
      heatmapImage.value = await resolveImage(report.heatmapUrl)
    } else {
      heatmapImage.value = await writeHeatmapToLocalFile()
      report.setHeatmapUrl(heatmapImage.value)
    }
  } catch (error) {
    console.error('加载结果图像失败', error)
    uni.showToast({ title: '热力图加载失败', icon: 'none' })
  }
})
</script>

<template>
  <view class="page">
    <view class="top-banner">
      <image class="banner-bg" src="/static/login/login-header.jpg" mode="aspectFill" />
      <view class="banner-mask"></view>
      <view class="banner-title">
        <text class="brand">灵镜智诊</text>
        <text class="title">检测结果</text>
      </view>
    </view>

    <view class="time-row">检测时间：{{ formatTime }}</view>

    <view class="content">
      <view class="visual-title">局部证据匹配可视化</view>
      <view class="compare">
        <view class="image-column">
          <view class="image-frame">
            <image v-if="originalImage" :src="originalImage" mode="aspectFit" />
            <text v-else>原图加载中</text>
          </view>
          <text class="image-label">原始图像</text>
        </view>
        <view class="image-column">
          <view class="image-frame">
            <image v-if="heatmapImage" :src="heatmapImage" mode="aspectFit" />
            <text v-else>暂无热力图</text>
          </view>
          <text class="image-label">混合热力图</text>
          <text class="image-note">{{ heatmapSubtitle }}</text>
        </view>
      </view>

      <view class="diagnosis">
        <view>
          <text class="diagnosis-label">模型预测</text>
          <text v-if="report.resultValue.confidence != null" class="confidence">
            置信度 {{ (report.resultValue.confidence * 100).toFixed(1) }}%
          </text>
        </view>
        <text class="diagnosis-value">{{ report.resultValue.diseaseType || '待确认' }}</text>
      </view>

      <view class="result-card">
        <text class="card-title">AI建议</text>
        <text class="card-subtitle">智能分析</text>
        <text class="card-content" user-select>{{ report.resultValue.advice || '暂无建议' }}</text>
      </view>

      <view class="result-card">
        <text class="card-title">疾病介绍</text>
        <text class="card-subtitle">健康科普</text>
        <text class="card-content" user-select>
          {{ report.resultValue.introduction || '暂无疾病介绍' }}
        </text>
      </view>

      <view class="actions">
        <button class="secondary" @tap="abandonResult">放弃结果</button>
        <button class="primary" :loading="saving" :disabled="saving" @tap="saveReport">
          保存报告
        </button>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
$theme: #8a2b31;

.page {
  min-height: 100vh;
  background: #f5f2f2;
  color: #242124;
}

.top-banner {
  position: relative;
  height: 220rpx;
  overflow: hidden;
  background: $theme;
}

.banner-bg,
.banner-mask {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.banner-bg {
  opacity: 0.22;
}

.banner-mask {
  background: rgba(115, 8, 20, 0.25);
}

.banner-title {
  position: relative;
  z-index: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  color: #fff;
}

.brand {
  font-size: 40rpx;
  font-weight: 700;
}

.title {
  font-size: 34rpx;
  font-weight: 600;
}

.time-row {
  padding: 24rpx 28rpx;
  background: #fff;
  font-size: 28rpx;
}

.content {
  padding: 24rpx;
}

.visual-title {
  margin-bottom: 20rpx;
  text-align: center;
  font-size: 32rpx;
  font-weight: 600;
}

.compare {
  display: flex;
  gap: 18rpx;
}

.image-column {
  width: calc(50% - 9rpx);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.image-frame {
  width: 100%;
  height: 330rpx;
  box-sizing: border-box;
  border: 3rpx solid #2d292a;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 24rpx;
  overflow: hidden;
}

.image-frame image {
  width: 100%;
  height: 100%;
}

.image-label {
  margin-top: 12rpx;
  font-size: 28rpx;
}

.image-note {
  min-height: 54rpx;
  margin-top: 4rpx;
  color: #777;
  font-size: 21rpx;
  line-height: 1.3;
  text-align: center;
}

.diagnosis {
  margin: 26rpx 0;
  padding: 20rpx 24rpx;
  border-left: 8rpx solid $theme;
  border-radius: 8rpx;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.diagnosis-label {
  display: block;
  color: #777;
  font-size: 25rpx;
}

.confidence {
  display: block;
  margin-top: 6rpx;
  color: #999;
  font-size: 21rpx;
}

.diagnosis-value {
  color: $theme;
  font-size: 29rpx;
  font-weight: 700;
}

.result-card {
  min-height: 260rpx;
  margin-bottom: 24rpx;
  padding: 28rpx;
  box-sizing: border-box;
  border: 2rpx solid #383234;
  border-radius: 22rpx;
  background: rgba(255, 255, 255, 0.9);
}

.card-title,
.card-subtitle,
.card-content {
  display: block;
}

.card-title {
  font-size: 32rpx;
  font-weight: 600;
}

.card-subtitle {
  margin-top: 8rpx;
  color: #777;
  font-size: 24rpx;
}

.card-content {
  margin-top: 22rpx;
  font-size: 26rpx;
  line-height: 1.7;
  white-space: pre-wrap;
}

.actions {
  display: flex;
  gap: 20rpx;
  padding: 4rpx 0 32rpx;
}

.actions button {
  flex: 1;
  height: 82rpx;
  line-height: 82rpx;
  border-radius: 14rpx;
  font-size: 29rpx;
}

.actions button::after {
  border: none;
}

.secondary {
  color: $theme;
  border: 2rpx solid $theme;
  background: #fff;
}

.primary {
  color: #fff;
  background: $theme;
}
</style>
