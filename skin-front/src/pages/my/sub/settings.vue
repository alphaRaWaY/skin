<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useAuthorizationStore } from '@/stores/modules/userStore'
import {
  changeMyPassword,
  getMyAccount,
  getOssImageUrl,
  logoutAccount,
  updateMyProfile,
  uploadAvatarImage,
  verifyMyOldPassword,
} from '@/services/accountService'

const authStore = useAuthorizationStore()
const loading = ref(false)
const avatarPreview = ref('/static/design/设计素材/医生头像.png')
const profile = ref({
  username: '',
  nickname: '',
  mobile: '',
  avatar: '',
  jobNumber: '',
})

const accountRows = ['个人资料', '账号安全', '个人信息与权限', '修改密码']
const featureRows = ['诊断记录导出', '图像质量', '报告格式']
const helpRows = ['帮助中心', '关于我们', '联系客服']

const goBack = () => {
  uni.navigateBack({
    fail: () => uni.switchTab({ url: '/pages/my/my' }),
  })
}

const syncStoreProfile = () => {
  authStore.profile = {
    username: profile.value.username || '',
    nickname: profile.value.nickname || '',
    mobile: profile.value.mobile || '',
    avatar: profile.value.avatar || '',
    jobNumber: profile.value.jobNumber || '',
  }
}

const refreshAvatarPreview = async () => {
  const avatar = profile.value.avatar || ''
  if (!avatar) {
    avatarPreview.value = '/static/design/设计素材/医生头像.png'
    return
  }
  if (avatar.startsWith('http://') || avatar.startsWith('https://')) {
    avatarPreview.value = avatar
    return
  }
  try {
    const res = await getOssImageUrl(avatar)
    avatarPreview.value = res.code === 0 && res.result ? res.result : '/static/design/设计素材/医生头像.png'
  } catch {
    avatarPreview.value = '/static/design/设计素材/医生头像.png'
  }
}

const fetchProfile = async () => {
  loading.value = true
  try {
    const res = await getMyAccount()
    if (res.code === 0 && res.result) {
      profile.value = {
        username: res.result.username || '',
        nickname: res.result.nickname || '',
        mobile: res.result.mobile || '',
        avatar: res.result.avatar || '',
        jobNumber: res.result.jobNumber || '',
      }
      syncStoreProfile()
      await refreshAvatarPreview()
    } else {
      uni.showToast({ title: res.msg || '获取资料失败', icon: 'none' })
    }
  } catch {
    uni.showToast({ title: '获取资料失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const saveProfilePatch = async (payload: Partial<typeof profile.value>) => {
  const res = await updateMyProfile(payload)
  if (res.code !== 0) {
    uni.showToast({ title: res.msg || '保存失败', icon: 'none' })
    return false
  }
  await fetchProfile()
  uni.showToast({ title: '保存成功', icon: 'success' })
  return true
}

const editSimpleField = (
  title: string,
  key: 'nickname' | 'mobile' | 'jobNumber',
  currentValue: string,
  placeholder: string,
) => {
  uni.showModal({
    title,
    editable: true,
    placeholderText: placeholder,
    content: currentValue || '',
    success: async (res) => {
      if (!res.confirm) return
      const next = (res.content || '').trim()
      await saveProfilePatch({ [key]: next })
    },
  })
}

const uploadAvatar = () => {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      const filePath = res.tempFilePaths?.[0]
      if (!filePath) return
      if (!/\.(jpg|jpeg)$/i.test(filePath)) {
        uni.showToast({ title: '请上传 jpg/jpeg 图片', icon: 'none' })
        return
      }
      uni.showLoading({ title: '上传中...' })
      try {
        const up = await uploadAvatarImage(filePath)
        if (up.code !== 0 || !up.result) {
          uni.showToast({ title: up.msg || '头像上传失败', icon: 'none' })
          return
        }
        await saveProfilePatch({ avatar: up.result })
      } finally {
        uni.hideLoading()
      }
    },
  })
}

const editProfile = () => {
  uni.showActionSheet({
    itemList: ['上传头像(jpg)', '修改昵称', '修改手机号', '修改医工号'],
    success: ({ tapIndex }) => {
      if (tapIndex === 0) {
        uploadAvatar()
        return
      }
      if (tapIndex === 1) {
        editSimpleField('修改昵称', 'nickname', profile.value.nickname, '请输入昵称')
        return
      }
      if (tapIndex === 2) {
        editSimpleField('修改手机号', 'mobile', profile.value.mobile, '请输入手机号')
        return
      }
      if (tapIndex === 3) {
        editSimpleField('修改医工号', 'jobNumber', profile.value.jobNumber, '请输入医工号')
      }
    },
  })
}

const promptInput = (title: string, placeholder: string) => {
  return new Promise<string | null>((resolve) => {
    uni.showModal({
      title,
      editable: true,
      placeholderText: placeholder,
      success: (res) => {
        if (!res.confirm) {
          resolve(null)
          return
        }
        resolve((res.content || '').trim())
      },
      fail: () => resolve(null),
    })
  })
}

const changePasswordFlow = async () => {
  const oldPassword = await promptInput('旧密码', '请输入旧密码')
  if (oldPassword == null) return
  if (!oldPassword) {
    uni.showToast({ title: '旧密码不能为空', icon: 'none' })
    return
  }

  const verifyRes = await verifyMyOldPassword(oldPassword)
  if (verifyRes.code !== 0) {
    uni.showToast({ title: verifyRes.msg || '旧密码不正确', icon: 'none' })
    return
  }

  const newPassword = await promptInput('新密码', '请输入新密码（至少6位）')
  if (newPassword == null) return
  if (!newPassword || newPassword.length < 6) {
    uni.showToast({ title: '新密码至少 6 位', icon: 'none' })
    return
  }

  const confirmPassword = await promptInput('确认密码', '请再次输入新密码')
  if (confirmPassword == null) return
  if (confirmPassword !== newPassword) {
    uni.showToast({ title: '两次输入的新密码不一致', icon: 'none' })
    return
  }

  const ret = await changeMyPassword({ oldPassword, newPassword })
  if (ret.code === 0) {
    uni.showToast({ title: '密码修改成功', icon: 'success' })
  } else {
    uni.showToast({ title: ret.msg || '修改失败', icon: 'none' })
  }
}

const doLogout = () => {
  uni.showModal({
    title: '退出登录',
    content: '确认退出当前账号吗？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await logoutAccount()
      } catch {}
      authStore.logout()
      uni.reLaunch({ url: '/pages/login/login' })
    },
  })
}

const onTapRow = (name: string) => {
  if (name === '个人资料') return editProfile()
  if (name === '修改密码') return changePasswordFlow()
  if (name === '账号安全') return uni.showToast({ title: '当前已启用登录态校验', icon: 'none' })
  if (name === '个人信息与权限') return uni.showToast({ title: '个人信息按最小必要原则使用', icon: 'none' })
  uni.showToast({ title: `${name} 暂未开放`, icon: 'none' })
}

onMounted(fetchProfile)
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
          <text class="title">设置</text>
        </view>
        <view class="placeholder"></view>
      </view>
    </view>

    <view class="profile-card">
      <image class="avatar" :src="avatarPreview" mode="aspectFill" />
      <view class="profile-main">
        <text class="name">{{ profile.nickname || profile.username || '未命名用户' }}</text>
        <text class="line">账号：{{ profile.username || '--' }}</text>
        <text class="line">手机号：{{ profile.mobile || '--' }}</text>
        <text class="line">医工号：{{ profile.jobNumber || '--' }}</text>
      </view>
    </view>

    <view class="section">
      <view class="section-title">账号</view>
      <view class="group">
        <view v-for="item in accountRows" :key="item" class="row" @tap="onTapRow(item)">
          <text class="row-text">{{ item }}</text>
          <text class="arrow">></text>
        </view>
      </view>
    </view>

    <view class="section">
      <view class="section-title">功能</view>
      <view class="group">
        <view v-for="item in featureRows" :key="item" class="row" @tap="onTapRow(item)">
          <text class="row-text">{{ item }}</text>
          <text class="arrow">></text>
        </view>
      </view>
    </view>

    <view class="section">
      <view class="section-title">帮助与关于</view>
      <view class="group">
        <view v-for="item in helpRows" :key="item" class="row" @tap="onTapRow(item)">
          <text class="row-text">{{ item }}</text>
          <text class="arrow">></text>
        </view>
      </view>
    </view>

    <button class="logout-btn" :loading="loading" @tap="doLogout">退出登录</button>
    <view class="footer-tip">个人信息手机清单和第三方信息共享清单</view>
  </view>
</template>

<style scoped lang="scss">
$theme: #8a2b31;
.page { min-height: 100vh; background: #f3f3f5; }
.top-banner { position: relative; height: 220rpx; background: linear-gradient(120deg, #7f1f28, $theme); overflow: hidden; }
.banner-bg { position: absolute; inset: 0; width: 100%; height: 100%; opacity: 0.2; }
.banner-mask { position: absolute; inset: 0; background: rgba(0, 0, 0, 0.08); }
.banner-bar { position: relative; z-index: 2; height: 100%; display: grid; grid-template-columns: 64rpx 1fr 64rpx; align-items: center; padding: 0 20rpx; }
.back { color: #fff; font-size: 56rpx; line-height: 1; }
.placeholder { width: 64rpx; }
.banner-title-wrap { display: flex; flex-direction: column; align-items: center; justify-content: center; color: #fff; gap: 10rpx; }
.brand { font-size: 42rpx; font-weight: 700; }
.title { font-size: 38rpx; font-weight: 600; }
.profile-card { margin: 16rpx 18rpx 0; background: #fff; border-radius: 12rpx; padding: 16rpx; display: flex; gap: 14rpx; align-items: center; }
.avatar { width: 104rpx; height: 104rpx; border-radius: 52rpx; border: 1rpx solid #ddd; }
.profile-main { flex: 1; display: flex; flex-direction: column; gap: 4rpx; }
.name { font-size: 32rpx; font-weight: 700; color: #222; }
.line { font-size: 24rpx; color: #666; }
.section { margin-top: 14rpx; }
.section-title { height: 64rpx; padding: 0 20rpx; display: flex; align-items: center; font-size: 30rpx; color: #6e6e74; background: #e9e9ec; }
.group { background: #fff; }
.row { height: 86rpx; padding: 0 22rpx; display: flex; align-items: center; justify-content: space-between; border-bottom: 1rpx solid #ececef; }
.row:last-child { border-bottom: none; }
.row-text { font-size: 34rpx; color: #1d1d1f; }
.arrow { font-size: 32rpx; color: #a5a5ab; }
.logout-btn { margin: 30rpx 20rpx 0; border: none; border-radius: 12rpx; background: $theme; color: #fff; font-size: 30rpx; }
.logout-btn::after { border: none; }
.footer-tip { margin-top: 40rpx; text-align: center; color: $theme; font-size: 26rpx; }
</style>
