<template>
  <view class="chat-page">
    <view class="chat-header">
      <text class="chat-title">AI问诊</text>
    </view>

    <scroll-view
      class="chat-scroll"
      scroll-y
      :scroll-into-view="scrollIntoViewId"
      :scroll-with-animation="true"
    >
      <view class="chat-list">
        <view
          v-for="item in messages"
          :key="item.id"
          :id="`msg-${item.id}`"
          :class="['msg-row', item.role === 'user' ? 'is-user' : 'is-ai']"
        >
          <view class="msg-bubble">
            <text class="msg-text">{{ item.content }}</text>
          </view>
        </view>
      </view>
    </scroll-view>

    <view class="chat-input-wrap">
      <input
        v-model="inputText"
        class="chat-input"
        type="text"
        placeholder="请输入您的问题"
        :maxlength="500"
        @confirm="sendMessage"
      />
      <button class="send-btn" @tap="sendMessage">发送</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue'

type ChatMessage = {
  id: number
  role: 'user' | 'assistant'
  content: string
}

const inputText = ref('')
const messages = ref<ChatMessage[]>([
  { id: 1, role: 'assistant', content: '您好，我是灵镜智诊AI助手，请描述您的症状。' },
])
const scrollIntoViewId = ref('msg-1')

const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text) return

  const uid = Date.now()
  messages.value.push({ id: uid, role: 'user', content: text })
  inputText.value = ''

  const reply = '已收到您的描述。当前页面仅展示会话界面，后续将接入完整问诊流程。'
  messages.value.push({ id: uid + 1, role: 'assistant', content: reply })

  await nextTick()
  scrollIntoViewId.value = `msg-${uid + 1}`
}
</script>

<style scoped lang="scss">
$theme: #8a2b31;

.chat-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f6f7f9;
}

.chat-header {
  height: 96rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #7b1f26, $theme);
}

.chat-title {
  color: #fff;
  font-size: 34rpx;
  font-weight: 700;
}

.chat-scroll {
  flex: 1;
  min-height: 0;
}

.chat-list {
  padding: 24rpx;
}

.msg-row {
  display: flex;
  margin-bottom: 18rpx;
}

.msg-row.is-user {
  justify-content: flex-end;
}

.msg-row.is-ai {
  justify-content: flex-start;
}

.msg-bubble {
  max-width: 76%;
  border-radius: 18rpx;
  padding: 16rpx 18rpx;
  background: #fff;
}

.is-user .msg-bubble {
  background: $theme;
}

.msg-text {
  color: #2a2a2a;
  font-size: 28rpx;
  line-height: 1.5;
  word-break: break-word;
}

.is-user .msg-text {
  color: #fff;
}

.chat-input-wrap {
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding: 16rpx 20rpx calc(16rpx + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1rpx solid #ebebeb;
}

.chat-input {
  flex: 1;
  height: 74rpx;
  border: 1rpx solid #d8d8d8;
  border-radius: 14rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
}

.send-btn {
  width: 128rpx;
  height: 74rpx;
  line-height: 74rpx;
  margin: 0;
  padding: 0;
  border-radius: 14rpx;
  border: none;
  background: $theme;
  color: #fff;
  font-size: 28rpx;
  font-weight: 600;
}
</style>
