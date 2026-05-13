<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { useReportStore } from '@/stores/modules/reportStore'
import type { Patient } from '@/types/patient'
import { getPatients } from '@/services/patientService'

const store = useReportStore()

const form = ref({
  username: '',
  gender: '男',
  age: 0,
  symptoms: '',
  duration: '',
  treatment: '',
  other: '',
})

const patientList = ref<Patient[]>([])
const selectedPatientId = ref<number | null>(null)

const patientDisplay = computed(() => {
  if (!selectedPatientId.value) return ''
  const p = patientList.value.find((x) => x.id === selectedPatientId.value)
  if (!p) return ''
  return p.patientName || ''
})

const fetchPatientList = async () => {
  try {
    const res = await getPatients()
    if (res.code === 0) {
      patientList.value = res.result || []
      if (selectedPatientId.value) {
        const stillExists = patientList.value.some((p) => p.id === selectedPatientId.value)
        if (!stillExists) selectedPatientId.value = null
      }
      return
    }
    throw new Error(res.msg || '获取患者列表失败')
  } catch (error: any) {
    uni.showToast({
      title: error?.message || '获取患者列表失败',
      icon: 'none',
    })
  }
}

watch(selectedPatientId, (id) => {
  const selected = patientList.value.find((item) => item.id === id)
  if (!selected) return
  form.value.username = selected.patientName || ''
  form.value.gender = selected.gender || '男'
  form.value.age = Number(selected.age || 0)
})

const openPatientPicker = () => {
  if (!patientList.value.length) {
    uni.showToast({ title: '暂无患者信息', icon: 'none' })
    return
  }
  const itemList = patientList.value.map((p) => p.patientName || '未命名患者')
  uni.showActionSheet({
    itemList,
    success: ({ tapIndex }) => {
      selectedPatientId.value = patientList.value[tapIndex].id
    },
  })
}

const clearPatient = () => {
  selectedPatientId.value = null
}

const clearField = (key: keyof typeof form.value) => {
  if (key === 'age') {
    form.value.age = 0
    return
  }
  form.value[key] = '' as never
}

const handleConfirm = () => {
  if (!form.value.username.trim()) {
    uni.showToast({ title: '请填写患者姓名', icon: 'none' })
    return
  }
  if (!form.value.symptoms.trim()) {
    uni.showToast({ title: '请填写症状', icon: 'none' })
    return
  }
  store.setForm(form.value)
  uni.showToast({ title: '信息已确认', icon: 'success' })
  uni.navigateTo({ url: '/pages/index/sub/upload/upload' })
}

onMounted(fetchPatientList)
onShow(fetchPatientList)

onPullDownRefresh(async () => {
  try {
    await fetchPatientList()
  } finally {
    uni.stopPullDownRefresh()
  }
})
</script>

<template>
  <view class="page">
    <view class="top-banner">
      <image class="banner-bg" src="/static/login/login-header.jpg" mode="aspectFill" />
      <view class="banner-mask"></view>
      <view class="banner-title-wrap">
        <text class="brand">灵镜智诊</text>
        <text class="title">信息填写</text>
      </view>
    </view>

    <view class="form">
      <view class="row">
        <text class="label">选择患者</text>
        <view class="input like-picker" @tap="openPatientPicker">
          <text class="input-text" :class="{ placeholder: !patientDisplay }">
            {{ patientDisplay || '例：张三' }}
          </text>
          <text v-if="patientDisplay" class="clear" @tap.stop="clearPatient">×</text>
        </view>
      </view>

      <view class="row">
        <text class="label">用户名</text>
        <view class="input">
          <input v-model="form.username" class="field" placeholder="例：张三" />
          <text v-if="form.username" class="clear" @tap="clearField('username')">×</text>
        </view>
      </view>

      <view class="row gender-row">
        <text class="label">性别</text>
        <view class="gender-wrap">
          <view class="radio" @tap="form.gender = '男'">
            <view class="dot" :class="{ active: form.gender === '男' }"></view>
            <text class="gender-text">男</text>
          </view>
          <view class="radio" @tap="form.gender = '女'">
            <view class="dot" :class="{ active: form.gender === '女' }"></view>
            <text class="gender-text">女</text>
          </view>
        </view>
      </view>

      <view class="row">
        <text class="label">年龄</text>
        <view class="input">
          <input v-model="form.age" type="number" class="field" placeholder="例：18" />
          <text v-if="form.age" class="clear" @tap="clearField('age')">×</text>
        </view>
      </view>

      <view class="row">
        <text class="label">症状</text>
        <view class="input">
          <input v-model="form.symptoms" class="field" placeholder="例：皮肤红肿、抓痒有脱屑" />
          <text v-if="form.symptoms" class="clear" @tap="clearField('symptoms')">×</text>
        </view>
      </view>

      <view class="row">
        <text class="label">持续时间</text>
        <view class="input">
          <input v-model="form.duration" class="field" placeholder="例：四天" />
          <text v-if="form.duration" class="clear" @tap="clearField('duration')">×</text>
        </view>
      </view>

      <view class="row">
        <text class="label">治疗措施</text>
        <view class="input">
          <input v-model="form.treatment" class="field" placeholder="例：外用药" />
          <text v-if="form.treatment" class="clear" @tap="clearField('treatment')">×</text>
        </view>
      </view>

      <view class="row textarea-row">
        <text class="label">其他信息</text>
        <view class="input textarea-wrap">
          <textarea v-model="form.other" class="textarea" placeholder="例：偶尔抓挠" />
        </view>
      </view>

      <button class="submit" @tap="handleConfirm">确认信息</button>
    </view>
  </view>
</template>

<style scoped lang="scss">
$theme: #8a2b31;

.page {
  min-height: 100vh;
  background: #f6f6f7;
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
  font-size: 34rpx;
  font-weight: 700;
}

.title {
  font-size: 30rpx;
  font-weight: 600;
}

.form {
  padding: 18rpx 24rpx 26rpx;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.row {
  display: grid;
  grid-template-columns: 150rpx 1fr;
  align-items: center;
  column-gap: 16rpx;
}

.label {
  font-size: 34rpx;
  color: #151515;
  font-weight: 700;
}

.input {
  height: 74rpx;
  border: 1px solid #cfcfd3;
  border-radius: 12rpx;
  background: #fff;
  display: flex;
  align-items: center;
  padding: 0 16rpx;
  box-sizing: border-box;
}

.like-picker {
  justify-content: space-between;
}

.input-text {
  font-size: 26rpx;
  color: #2a2a2a;
}

.placeholder {
  color: #9a9aa0;
}

.field {
  flex: 1;
  font-size: 26rpx;
  color: #202020;
}

.clear {
  width: 38rpx;
  height: 38rpx;
  border-radius: 19rpx;
  background: #b7b7bc;
  color: #fff;
  text-align: center;
  line-height: 34rpx;
  font-size: 26rpx;
  flex-shrink: 0;
}

.gender-row {
  align-items: center;
}

.gender-wrap {
  display: flex;
  gap: 60rpx;
}

.radio {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.dot {
  width: 30rpx;
  height: 30rpx;
  border-radius: 50%;
  border: 3rpx solid #9d9da3;
  position: relative;
}

.dot.active {
  border-color: #c3353d;
}

.dot.active::after {
  content: '';
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  background: #c3353d;
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
}

.gender-text {
  font-size: 30rpx;
  color: #505054;
}

.textarea-row {
  align-items: flex-start;
}

.textarea-wrap {
  min-height: 186rpx;
  height: auto;
  align-items: flex-start;
  padding-top: 14rpx;
  padding-bottom: 14rpx;
}

.textarea {
  width: 100%;
  min-height: 156rpx;
  font-size: 26rpx;
  line-height: 1.45;
}

.submit {
  margin-top: 6rpx;
  width: 320rpx;
  height: 84rpx;
  line-height: 84rpx;
  background: $theme;
  color: #fff;
  border-radius: 14rpx;
  font-size: 32rpx;
  font-weight: 700;
  border: none;
}

.submit::after {
  border: none;
}
</style>
