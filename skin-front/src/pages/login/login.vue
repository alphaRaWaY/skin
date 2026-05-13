<script setup lang="ts">
import { ref } from 'vue'
import { useAuthorizationStore } from '@/stores/modules/userStore'
import { postLoginPwd } from '@/services/login'

type LoginType = 'password' | 'code'

const loginType = ref<LoginType>('password')
const account = ref('')
const password = ref('')
const code = ref('')
const agreed = ref(false)

const authStore = useAuthorizationStore()

const switchLoginType = (type: LoginType) => {
  loginType.value = type
}

const openAgreement = (type: 'user' | 'privacy') => {
  uni.navigateTo({ url: `/pages/login/policy?type=${type}` })
}

const handleForgotPassword = () => {
  uni.showToast({ title: '限于平台能力暂不支持此功能', icon: 'none' })
}

const handleGetCode = () => {
  uni.showToast({ title: '限于平台能力暂不支持此功能', icon: 'none' })
}

const validateForm = () => {
  if (!agreed.value) {
    uni.showToast({ title: '请先阅读并同意用户协议和隐私协议', icon: 'none' })
    return false
  }

  if (!account.value.trim()) {
    uni.showToast({ title: '请输入账号', icon: 'none' })
    return false
  }

  if (loginType.value === 'password' && !password.value.trim()) {
    uni.showToast({ title: '请输入密码', icon: 'none' })
    return false
  }

  if (loginType.value === 'code' && !code.value.trim()) {
    uni.showToast({ title: '请输入验证码', icon: 'none' })
    return false
  }

  return true
}

const handleLogin = async () => {
  if (!validateForm()) return

  if (loginType.value === 'code') {
    uni.showToast({ title: '限于平台能力暂不支持此功能', icon: 'none' })
    return
  }

  try {
    const res = await postLoginPwd({
      account: account.value.trim(),
      password: password.value,
    })

    if (res.code !== 0 || !res.result) {
      uni.showToast({ title: res.msg || '登录失败', icon: 'none' })
      return
    }

    authStore.setUserInfo(res.result)
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => {
      uni.switchTab({ url: '/pages/my/my' })
    }, 300)
  } catch {
    uni.showToast({ title: '登录失败，请稍后重试', icon: 'none' })
  }
}
</script>

<template>
  <view class="page">
    <view class="top-banner">
      <image class="banner-bg" src="/static/login/login-header.jpg" mode="aspectFill" />
      <view class="banner-overlay"></view>
      <text class="banner-title">灵镜智诊</text>
    </view>

    <view class="content">
      <image class="logo" src="/static/login/login-logo.png" mode="aspectFill" />
      <text class="main-title">灵镜智诊</text>
      <text class="sub-title">皮肤镜智能诊断系统</text>

      <view class="type-switch">
        <view class="switch-item" :class="{ active: loginType === 'password' }" @tap="switchLoginType('password')">密码登录</view>
        <view class="switch-item" :class="{ active: loginType === 'code' }" @tap="switchLoginType('code')">验证码登录</view>
      </view>

      <view class="form-block">
        <view class="form-row">
          <text class="label">账号</text>
          <input v-model="account" class="input" type="text" placeholder="请输入医生工号/手机号" placeholder-class="placeholder" />
        </view>

        <view v-if="loginType === 'password'" class="form-row">
          <text class="label">密码</text>
          <input v-model="password" class="input" type="password" password placeholder="请输入密码" placeholder-class="placeholder" />
        </view>

        <view v-else class="form-row code-row">
          <text class="label">验证码</text>
          <input v-model="code" class="input" type="number" placeholder="请输入验证码" placeholder-class="placeholder" />
          <button class="code-btn" @tap="handleGetCode">获取验证码</button>
        </view>
      </view>

      <text class="forgot" @tap="handleForgotPassword">忘记密码？</text>
      <button class="login-btn" @tap="handleLogin">登录</button>
    </view>

    <view class="agreement" @tap="agreed = !agreed">
      <view class="check-box" :class="{ checked: agreed }">
        <text v-if="agreed" class="check-mark">✓</text>
      </view>
      <text class="agreement-text">
        我已阅读并同意
        <text class="link" @tap.stop="openAgreement('user')">《用户协议》</text>
        和
        <text class="link" @tap.stop="openAgreement('privacy')">《隐私协议》</text>
      </text>
    </view>
  </view>
</template>

<style scoped lang="scss">
$page-width: 750rpx;
$theme: #8a2b31;
$theme-light: #f5e6e8;

.page {
  width: $page-width;
  min-height: 100vh;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  color: #1f1f1f;
}

.top-banner {
  position: relative;
  height: 220rpx;
  background: linear-gradient(135deg, #7e232a 0%, $theme 65%, #b3404a 100%);
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.banner-bg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 0.22;
}

.banner-overlay {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 22% 24%, rgba(255, 255, 255, 0.18), transparent 45%);
}

.banner-title {
  position: relative;
  z-index: 2;
  color: #fff;
  font-size: 40rpx;
  font-weight: 700;
  letter-spacing: 2rpx;
}

.content {
  padding: 22rpx 56rpx 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.logo { width: 150rpx; height: 150rpx; border-radius: 75rpx; border: 4rpx solid rgba(138, 43, 49, 0.2); background: #fff; }
.main-title { margin-top: 22rpx; font-size: 56rpx; font-weight: 800; color: #161616; }
.sub-title { margin-top: 10rpx; font-size: 34rpx; color: #4d4d4d; }

.type-switch { margin-top: 42rpx; display: flex; gap: 20rpx; }
.switch-item { min-width: 198rpx; height: 64rpx; border: 1px solid #d7d7d7; border-radius: 36rpx; font-size: 28rpx; color: #333; display: flex; align-items: center; justify-content: center; background: #fff; }
.switch-item.active { border-color: #d4a3a8; background: $theme-light; color: #222; font-weight: 600; }

.form-block { width: 100%; margin-top: 44rpx; }
.form-row { width: 100%; display: flex; align-items: center; gap: 20rpx; margin-bottom: 24rpx; }
.label { width: 100rpx; font-size: 34rpx; font-weight: 700; color: #111; flex-shrink: 0; }
.input { flex: 1; height: 82rpx; border: 1px solid #c8c8c8; border-radius: 14rpx; padding: 0 20rpx; font-size: 28rpx; box-sizing: border-box; }
.placeholder { color: #a0a0a0; font-size: 26rpx; }
.code-row .input { min-width: 0; }
.code-btn { width: 160rpx; height: 68rpx; line-height: 68rpx; border-radius: 16rpx; border: 1px solid $theme; color: $theme; background: #fff; font-size: 24rpx; padding: 0; }

.forgot { margin-top: 8rpx; font-size: 26rpx; color: $theme; align-self: center; }
.login-btn { margin-top: 40rpx; width: 450rpx; height: 88rpx; line-height: 88rpx; background: $theme; color: #fff; border-radius: 24rpx; font-size: 36rpx; font-weight: 700; border: none; }
.login-btn::after { border: none; }

.agreement { margin-top: auto; padding: 30rpx 56rpx 42rpx; display: flex; align-items: flex-start; gap: 14rpx; }
.check-box { margin-top: 4rpx; width: 30rpx; height: 30rpx; border: 1px solid #c7c7c7; border-radius: 6rpx; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.check-box.checked { border-color: $theme; background: $theme; }
.check-mark { color: #fff; font-size: 22rpx; line-height: 1; }
.agreement-text { font-size: 24rpx; color: #555; line-height: 1.6; }
.link { color: $theme; }
</style>
