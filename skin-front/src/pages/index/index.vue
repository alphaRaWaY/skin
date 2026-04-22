<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import CustomerNavbar from './components/CustomerNavbar.vue'
import { useReportStore } from '@/stores/modules/reportStore'
import type { responseFamily } from '@/types/family'
import { getFamily } from '@/services/familyService'

const store = useReportStore()

const form = ref({
  username: '',
  gender: '',
  age: 0,
  symptoms: '',
  duration: '',
  treatment: '',
  other: ''
})

const familyList = ref<responseFamily[]>([])
const selectedFamilyId = ref<number | null>(null)

const fetchFamilyList = async () => {
  try {
    const res = await getFamily()
    if (res.code === 0) {
      familyList.value = res.result || []
      if (selectedFamilyId.value) {
        const stillExists = familyList.value.some((f) => f.id === selectedFamilyId.value)
        if (!stillExists) selectedFamilyId.value = null
      }
      return
    }
    throw new Error(res.msg || '获取家人列表失败')
  } catch (error: any) {
    uni.showToast({
      title: error?.message || '获取家人列表失败',
      icon: 'none'
    })
  }
}

watch(selectedFamilyId, (id) => {
  const selected = familyList.value.find((item) => item.id === id)
  if (!selected) return
  form.value.username = selected.name || selected.username || ''
  form.value.gender = selected.gender || ''
  form.value.age = Number(selected.age || 0)
})

onMounted(() => {
  fetchFamilyList()
})

onShow(() => {
  fetchFamilyList()
})

onPullDownRefresh(async () => {
  try {
    await fetchFamilyList()
  } finally {
    uni.stopPullDownRefresh()
  }
})

const handleConfirm = () => {
  store.setForm(form.value)
  uni.showToast({ title: '已保存', icon: 'success' })
  uni.navigateTo({ url: '/pages/index/sub/upload/upload' })
}
</script>

<template>
  <CustomerNavbar />
  <view class="form-container">
    <uni-forms :modelValue="form">
      <uni-forms-item label="选择家人">
        <uni-data-select
          v-model="selectedFamilyId"
          :localdata="familyList.map((f) => ({ value: f.id, text: `${f.name || f.username}（${f.relation}）` }))"
          placeholder="请选择"
        />
      </uni-forms-item>

      <uni-forms-item label="用户名">
        <uni-easyinput v-model="form.username" placeholder="请输入用户名" />
      </uni-forms-item>

      <uni-forms-item label="性别">
        <uni-data-checkbox
          v-model="form.gender"
          :localdata="[
            { value: '男', text: '男' },
            { value: '女', text: '女' }
          ]"
        />
      </uni-forms-item>

      <uni-forms-item label="年龄">
        <uni-easyinput v-model="form.age" type="number" placeholder="请输入年龄" />
      </uni-forms-item>

      <uni-forms-item label="症状">
        <uni-easyinput v-model="form.symptoms" placeholder="请输入症状描述" />
      </uni-forms-item>

      <uni-forms-item label="持续时间">
        <uni-easyinput v-model="form.duration" placeholder="例如：2周 / 3个月" />
      </uni-forms-item>

      <uni-forms-item label="既往治疗">
        <uni-easyinput v-model="form.treatment" placeholder="请输入既往治疗情况" />
      </uni-forms-item>

      <uni-forms-item label="其他信息">
        <uni-easyinput v-model="form.other" type="textarea" placeholder="可选补充信息" />
      </uni-forms-item>

      <view class="confirm-button">
        <button type="primary" @click="handleConfirm">下一步：上传图片</button>
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
