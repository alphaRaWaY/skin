<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useReportStore } from '@/stores/modules/reportStore'
import CustomerNavbar from './components/CustomerNavbar.vue'
import type { responseFamily } from '@/types/family'
import { getFamily } from '@/services/familyService'
import { onPullDownRefresh } from '@dcloudio/uni-app'
const store = useReportStore()

// 表单数据
const form = ref({
  username: '',
  gender: '',
  age: 0,
  symptoms: '',
  duration: '',
  treatment: '',
  other: ''
})

// 监听下拉刷新事件
onPullDownRefresh(() => {
  console.log('触发下拉刷新')
  // 清空之前选中的家人，并重新获取列表
  selectedFamilyId.value = null;
  fetchFamilyList()
})


// 家人列表和选中项
const familyList = ref<responseFamily[]>([])
const selectedFamilyId = ref<number | null>(null)

// 获取家人列表（请根据你的接口替换）
const fetchFamilyList = async () => {
  try {
    uni.showLoading({ title: '加载中...' })
    const res = await getFamily();

    if (res.code === 0) {
      familyList.value = res.result || []
    } else {
      throw new Error(res.msg || '加载失败')
    }
  } catch (error) {
    console.error('加载家人列表失败:', error)
    uni.showToast({ title: error.message, icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}


// 选择家人后自动填充表单
watch(selectedFamilyId, (id) => {
  const selected = familyList.value.find(item => item.id === id)
  if (selected) {
    form.value.username = selected.name
    form.value.gender = selected.gender
    form.value.age = selected.age
  }
})

// 初始化
onMounted(() => {
  fetchFamilyList()
})

// 提交表单
const handleConfirm = () => {
  store.setForm(form.value)
  uni.showToast({ title: '信息已提交', icon: 'success' })
  uni.navigateTo({ url: '/pages/index/sub/upload/upload' })
}
</script>

<template>
  <CustomerNavbar />
  <view class="form-container">
    <uni-forms :modelValue="form">
      <!-- 家人下拉选择 -->
      <uni-forms-item label="选择家人">
        <uni-data-select
          v-model="selectedFamilyId"
          :localdata="familyList.map(f => ({ value: f.id, text: `${f.name}（${f.relation}）` }))"
          placeholder="请选择家人"
        />
      </uni-forms-item>

      <!-- 基本信息表单 -->
      <uni-forms-item label="用户名">
        <uni-easyinput v-model="form.username" placeholder="请输入用户名" />
      </uni-forms-item>
      <uni-forms-item label="性别">
        <uni-data-checkbox v-model="form.gender" :localdata="[
          { value: '男', text: '男' },
          { value: '女', text: '女' }
        ]" />
      </uni-forms-item>
      <uni-forms-item label="年龄">
        <uni-easyinput v-model="form.age" type="number" placeholder="请输入年龄" />
      </uni-forms-item>
      <uni-forms-item label="症状">
        <uni-easyinput v-model="form.symptoms" placeholder="请输入症状" />
      </uni-forms-item>
      <uni-forms-item label="持续时间">
        <uni-easyinput v-model="form.duration" placeholder="如：2天/1周/3个月" />
      </uni-forms-item>
      <uni-forms-item label="治疗措施">
        <uni-easyinput v-model="form.treatment" placeholder="请输入已采取的治疗措施" />
      </uni-forms-item>
      <uni-forms-item label="其他信息">
        <uni-easyinput v-model="form.other" type="textarea" placeholder="如有其他补充，请输入" />
      </uni-forms-item>

      <!-- 提交按钮 -->
      <view class="confirm-button">
        <button type="primary" @click="handleConfirm">确认信息</button>
      </view>
    </uni-forms>
  </view>
</template>

<style lang="scss" scoped>
.form-container {
  padding: 20rpx;
}
.confirm-button {
  margin-top: 40rpx;
  display: flex;
  justify-content: center;
}
</style>
