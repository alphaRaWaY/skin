<template>
  <view class="container">
    <!-- 自定义导航栏 -->
    <view class="custom-nav" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <text class="nav-title">健康问诊</text>
      </view>
    </view>

    <!-- 页面内容 -->
    <view class="content" :style="{ paddingTop: navHeight + 'px' }">
      <image class="logo" src="/static/logo.png" />
      <view class="text-area">
        <text class="title">{{ title }}</text>
      </view>
      <button
        :disabled="false"
        :loading="false"
        open-type=""
        hover-class="button-hover"
        @click="goToLogin"
      >
        去登录
      </button>  
      <uni-card
        title="测试"
      >
        用来测试一个uni-ui组件是否能够正常显示
      </uni-card>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const title = ref('你好')
const statusBarHeight = ref(0)
const navHeight = ref(0)

onMounted(() => {
  const systemInfo = uni.getSystemInfoSync()
  statusBarHeight.value = systemInfo.statusBarHeight || 0
  // 导航栏高度：状态栏高度 + 44px（导航内容高度）
  navHeight.value = statusBarHeight.value + 44
})

const goToLogin = () => {
  uni.navigateTo({
    url: '/pages/login/login'
  })
}
</script>

<style lang="scss">
.container {
  min-height: 100vh;
  background-color: #f7f7f8;
}

.custom-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 999;
  background-color: #ffffff;
  
  .nav-content {
    height: 44px;
    display: flex;
    align-items: center;
    justify-content: center;
    
    .nav-title {
      font-size: 16px;
      font-weight: 500;
      color: #333;
    }
  }
}

.content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.logo {
  height: 200rpx;
  width: 200rpx;
  margin-top: 200rpx;
  margin-left: auto;
  margin-right: auto;
  margin-bottom: 50rpx;
}

.text-area {
  display: flex;
  justify-content: center;
}

.title {
  font-size: 36rpx;
  color: #8f8f94;
}

.login-btn {
  margin-top: 40rpx;
  width: 80%;
  background-color: #28bb9c;
}
</style>
