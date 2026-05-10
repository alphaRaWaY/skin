<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

type PolicyType = 'user' | 'privacy'

const title = ref('协议内容')
const content = ref('')

const USER_POLICY_TEXT = `用户协议

欢迎使用灵镜智诊皮肤镜智能诊断系统（以下简称“本系统”）。在使用本系统前，请您仔细阅读并理解本协议。

一、服务说明
1. 本系统为皮肤健康管理与辅助分析工具，提供信息化记录、辅助分析及健康建议展示功能。
2. 本系统输出结果仅供健康参考，不构成医疗诊断结论或处方建议。

二、用户义务
1. 您应保证提交信息真实、合法、完整，不得冒用他人身份。
2. 您不得利用本系统从事违法违规活动，不得干扰系统正常运行。

三、免责声明
1. 因网络、设备、第三方服务波动导致的服务中断，本系统将尽力恢复，但不承担由此造成的间接损失。
2. 对于基于不完整信息形成的分析结果偏差，本系统不承担医疗责任。

四、协议更新
1. 本协议可能根据业务发展进行更新，更新后在系统内公示并生效。
2. 您继续使用本系统即视为接受更新后的协议内容。`

const PRIVACY_POLICY_TEXT = `隐私协议

灵镜智诊重视您的个人信息与隐私安全。为向您提供服务，我们将依据本协议处理相关数据。

一、信息收集范围
1. 账号信息：如用户名、手机号、医工号等登录识别信息。
2. 业务信息：如检测记录、病史描述、上传图片及相关分析结果。
3. 设备与日志信息：用于保障系统稳定和安全审计。

二、信息使用目的
1. 用于身份认证、服务提供、结果展示与问题排查。
2. 用于改进产品体验和服务质量。

三、信息保护措施
1. 我们采取访问控制、传输保护、日志审计等措施保护数据安全。
2. 未经授权，不会向无关第三方披露您的个人信息。

四、用户权利
1. 您有权查询、更正、删除您的相关信息。
2. 您可通过管理员渠道申请账号注销与数据处理。

五、协议说明
1. 本协议仅用于当前系统服务范围。
2. 正式上线后如有法务版本更新，将以最新公示文本为准。`

onLoad((query) => {
  const type = (query?.type as PolicyType) || 'user'
  if (type === 'privacy') {
    title.value = '隐私协议'
    content.value = PRIVACY_POLICY_TEXT
  } else {
    title.value = '用户协议'
    content.value = USER_POLICY_TEXT
  }
})
</script>

<template>
  <view class="policy-page">
    <view class="policy-card">
      <text class="policy-title">{{ title }}</text>
      <scroll-view scroll-y class="policy-content">
        <text class="policy-text" user-select>{{ content }}</text>
      </scroll-view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.policy-page {
  min-height: 100vh;
  background: #f6f7f9;
  padding: 24rpx;
  box-sizing: border-box;
}

.policy-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
}

.policy-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #1f1f1f;
  display: block;
  margin-bottom: 18rpx;
}

.policy-content {
  height: calc(100vh - 180rpx);
}

.policy-text {
  color: #333;
  font-size: 28rpx;
  line-height: 1.8;
  white-space: pre-wrap;
}
</style>
