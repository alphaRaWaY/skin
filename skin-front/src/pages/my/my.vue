<script setup lang="ts">
import { useAuthorizationStore } from '@/stores';
const serviceTypes = [
  { type: 'history', text: '我的历史', icon: 'test' },
  // { type: 'report', text: '我的报告', icon: 'test1' },
  { type: 'family', text: '我的家人', icon: 'test2' },
]

const auth = useAuthorizationStore();
// 登出
const logout=()=>{
  setTimeout(()=>{
    auth.logout();
  },1000)
  uni.navigateTo({
    url:'/pages/login/login'
  })
}
// 测试登录状态
// const testAuth = () => {
//   console.log(auth.profile);
//   if (auth.token) {
//     uni.showToast({
//       title: '当前界面已登录',
//       icon: 'success'
//     });
//   } else {
//     uni.showToast({
//       title: '当前界面还未登录',
//       icon: 'success'
//     });
//   }
// }
// 获取屏幕边界到安全区域距离
const { safeAreaInsets } = uni.getSystemInfoSync();
</script>

<template>
  <scroll-view scroll-y class="viewport" enable-back-to-top>
    <!-- 个人资料 -->
    <view class="profile" :style="{ paddingTop: safeAreaInsets!.top + 'px' }">

      <!-- 已经登录的情况 -->
      <view class="profile" v-if="auth.token">
        <!-- 头像 -->
        <image
          class="avatar"
          mode="aspectFill"
          :src="auth.profile.avatar || '@/static/images/avatar.jpg'"
        ></image>
        <!-- 个人信息 -->
        <view class="meta">
          <view class="nickname">
            {{ auth.profile.nickname || auth.profile.mobile }}
          </view>
          <navigator class="extra" url="/pagesMember/profile/profile" hover-class="none">
            <text class="update">更新头像昵称</text>
          </navigator>
        </view>
      </view>

      <!-- 未登录的情况 -->
      <view class="overview" v-else>
        <navigator url="/pages/login/login" open-type="navigate" hover-class="navigator-hover">
          <image
            class="avatar gray"
            mode="aspectFill"
            src="@/static/images/avatar.jpg"
          />
        </navigator>
        <view class="meta">
          <navigator url="/pages/login/login" hover-class="none" class="nickname">
            未登录
          </navigator>
          <view class="extra">
            <text class="tips">点击登录账号</text>
          </view>
        </view>
      </view>

      <!-- 设置按钮 -->
      <navigator v-if="auth.token" class="settings" @click="logout" hover-class="none">
        退出登录
      </navigator>
    </view>

    <!-- 我的服务 -->
    <view class="orders">
      <view class="title">
        我的服务
        <navigator class="navigator" url="/pagesOrder/list/list?type=0" hover-class="none">
          智能皮肤检测系统<text class="icon-right"></text>
        </navigator>
      </view>
      <view class="section">
        <!-- 服务项目 -->
        <navigator
          v-if="auth.token"
          v-for="item in serviceTypes"
          :key="item.type"
          :class="`navigator iconfont icon-icon-${item.icon}`"
          :url="`/pages/my/sub/${item.type}`"
          hover-class="none"
        >
          {{ item.text }}
        </navigator>

        <navigator
          v-else
          v-for="item in serviceTypes"
          :key="item.type"
          :class="`navigator iconfont icon-icon-${item.icon}`"
          url="/pages/login/login"
          hover-class="none"
          class="gray"
        >
          {{ item.text }}
        </navigator>
      </view>
    </view>
    <!-- <button
      :disabled="false"
      :loading="false"
      hover-class="button-hover"
      @click="testAuth"
    >
      测试登录
    </button>

    <text class="iconfont icon-icon-test">测试图标是否有用</text> -->
    <!-- <button @click="testFamily">测试家人</button> -->
  </scroll-view>
</template>

<style lang="scss">
page {
  height: 100%;
  overflow: hidden;
  background-color: #f7f7f8;
}

.viewport {
  height: 100%;
  background-repeat: no-repeat;
  background-image: url(https://pcapi-xiaotuxian-front-devtest.itheima.net/miniapp/images/center_bg.png);
  background-size: 100% auto;
}

/* 用户信息 */
.profile {
  margin-top: 20rpx;
  position: relative;

  .overview {
    display: flex;
    height: 120rpx;
    padding: 0 36rpx;
    color: #fff;
  }

  .avatar {
    width: 120rpx;
    height: 120rpx;
    border-radius: 50%;
    background-color: #eee;
  }

  .gray {
    filter: grayscale(100%);
  }

  .meta {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: flex-start;
    line-height: 30rpx;
    padding: 16rpx 0;
    margin-left: 20rpx;
  }

  .nickname {
    max-width: 350rpx;
    margin-bottom: 16rpx;
    font-size: 30rpx;

    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .extra {
    display: flex;
    font-size: 20rpx;
  }

  .tips {
    font-size: 22rpx;
  }

  .update {
    padding: 3rpx 10rpx 1rpx;
    color: rgba(255, 255, 255, 0.8);
    border: 1rpx solid rgba(255, 255, 255, 0.8);
    margin-right: 10rpx;
    border-radius: 30rpx;
  }

  .settings {
    position: absolute;
    bottom: 0;
    right: 40rpx;
    font-size: 30rpx;
    color: #fff;
  }
}

/* 我的服务 */
.orders {
  position: relative;
  z-index: 99;
  padding: 30rpx;
  margin: 50rpx 20rpx 0;
  background-color: #fff;
  border-radius: 10rpx;
  box-shadow: 0 4rpx 6rpx rgba(240, 240, 240, 0.6);

  .title {
    height: 40rpx;
    line-height: 40rpx;
    font-size: 28rpx;
    color: #1e1e1e;

    .navigator {
      font-size: 24rpx;
      color: #939393;
      float: right;
    }
  }

  .gray {
    filter: grayscale(100%);
  }

  .section {
    width: 100%;
    display: flex;
    justify-content: space-between;
    padding: 40rpx 20rpx 10rpx;
    .navigator,
    .contact {
      text-align: center;
      font-size: 24rpx;
      color: #333;
      &::before {
        display: block;
        font-size: 60rpx;
        color: #ff9545;
      }
    }
    .contact {
      padding: 0;
      margin: 0;
      border: 0;
      background-color: transparent;
      line-height: inherit;
    }
  }
}

/* 猜你喜欢 */
.guess {
  background-color: #f7f7f8;
  margin-top: 20rpx;
}
</style>
