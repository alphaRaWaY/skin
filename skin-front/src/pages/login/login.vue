// src/pages/login/login.vue

<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { postLoginWxMin} from '@/services/login'
import { getFamily } from '@/services/familyService'
import { useAuthorizationStore } from '@/stores/modules/userStore'
import type { LoginResult } from '@/types/user'

const goToTest =  ()=>{
  uni.navigateTo({
    url: "/pages/test/test"
  })
}

let code = ''
// 获取code登录凭证
onLoad(async () => {
  try {
    const res = await uni.login({ provider: 'weixin' })
    code = res.code
  } catch (err) {
    uni.showToast({
      title: '登录失败',
      icon: 'error'
    })
  }
})

// 获取用户手机号码（企业写法）
// const onGetphonenumber: UniHelper.ButtonOnGetphonenumber = async (ev) => {
//   console.log('微信手机号授权事件', ev)
//   if (ev.detail?.errMsg !== 'getPhoneNumber:ok') {
//     uni.showToast({ title: '用户未授权手机号', icon: 'none' })
//     return
//   }
//   uni.showToast({ title: '用户授权手机号成功', icon: 'none' })
//   const encryptedData = ev.detail!.encryptedData!
//   const iv = ev.detail!.iv!
//   const params = {
//     code,
//     encryptedData,
//     iv
//   }
//   try {
//     const res = await postLoginWxMin(params)
//     if(res.code!=0)throw new Error(res.message)
//     loginSuccess(res.data)
//   } catch (error) {
//     console.log('登录失败', error)
//     uni.showToast({
//       title: '登录失败',
//       icon: 'error'
//     })
//   }
// }

// 模拟手机号码快捷登录
const onGetphonenumberSimple = async () => {
  try {
    const params = {
      code: 'mock_code',
      encryptedData: '123456',
      iv: '123456'
    }
    const res = await postLoginWxMin(params)
    if(res.code)throw new Error(res.msg)
    console.log('登录成功', res)
    uni.showToast({
      title: JSON.stringify(res),
      icon: 'success'
    })
    loginSuccess(res.result)
  } catch (error) {
    console.log('登录失败', error)
    uni.showToast({
      title: '登录失败',
      icon: 'error'
    })
  }
}

const loginSuccess = (result: LoginResult) => {
  // 保存用户信息
  try {
    const authStore = useAuthorizationStore()
    authStore.setUserInfo(result)
    // 提示登录成功
    uni.showToast({
      icon: 'success',
      title: '登录成功'
    })


    // 延迟跳转到我的页面
    setTimeout(() => {
      uni.switchTab({
        url: '/pages/my/my'
      })
    }, 1000)
  } catch (error) {
    console.log('存储用户信息失败', error)
    uni.showToast({
      title: '存储用户信息失败',
      icon: 'error'
    })
  }
}
</script>

<template>
  <view class="viewport">
    <view class="logo">
      <image src="/static/images/logo_icon.jpg"></image>
    </view>
    <view class="login">
      <!-- 企业写法 -->
     <!-- <button class="button phone" open-type="getPhoneNumber" @getphonenumber="onGetphonenumber"> -->
     <button class="button phone" open-type="getPhoneNumber" @getphonenumber="onGetphonenumberSimple">
       <text class="icon icon-phone"></text>
       手机号快捷登录
     </button>
      <view class="extra">
        <view class="caption">
          <text>其他登录方式</text>
        </view>
        <view class="options">
          <button @tap="onGetphonenumberSimple">
            <text class="icon icon-phone" >模拟快捷登录</text>
          </button>
        </view>
      </view>
      <view class="tips">登录/注册即视为你同意《服务条款》<u @click="goToTest">点我进入测试界面</u></view>
    </view>
  </view>
</template>

<style lang="scss">
page {
  height: 100%;
}

.viewport {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 20rpx 40rpx;
}

.logo {
  flex: 1;
  text-align: center;
  image {
    width: 220rpx;
    height: 220rpx;
    margin-top: 15vh;
  }
}

.login {
  display: flex;
  flex-direction: column;
  height: 60vh;
  padding: 40rpx 20rpx 20rpx;

  .input {
    width: 100%;
    height: 80rpx;
    font-size: 28rpx;
    border-radius: 72rpx;
    border: 1px solid #ddd;
    padding-left: 30rpx;
    margin-bottom: 20rpx;
  }

  .button {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 80rpx;
    font-size: 28rpx;
    border-radius: 72rpx;
    color: #fff;
    .icon {
      font-size: 40rpx;
      margin-right: 6rpx;
    }
  }

  .phone {
    background-color: #28bb9c;
  }

  .wechat {
    background-color: #06c05f;
  }

  .extra {
    flex: 1;
    padding: 70rpx 70rpx 0;
    .caption {
      width: 440rpx;
      line-height: 1;
      border-top: 1rpx solid #ddd;
      font-size: 26rpx;
      color: #999;
      position: relative;
      text {
        transform: translate(-40%);
        background-color: #fff;
        position: absolute;
        top: -12rpx;
        left: 50%;
      }
    }

    .options {
      display: flex;
      justify-content: center;
      align-items: center;
      margin-top: 70rpx;
      button {
        padding: 0;
        background-color: transparent;
      }
    }

    .icon {
      font-size: 24rpx;
      color: #444;
      display: flex;
      flex-direction: column;
      align-items: center;

      &::before {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 80rpx;
        height: 80rpx;
        margin-bottom: 6rpx;
        font-size: 40rpx;
        border: 1rpx solid #444;
        border-radius: 50%;
      }
    }
    .icon-weixin::before {
      border-color: #06c05f;
      color: #06c05f;
    }
  }
}

.tips {
  position: absolute;
  bottom: 80rpx;
  left: 20rpx;
  right: 20rpx;
  font-size: 22rpx;
  color: #999;
  text-align: center;
}
</style>
