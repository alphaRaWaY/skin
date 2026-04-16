<template>
  <view class="chat-list">
    <scroll-view scroll-y class="chat-scroll">
      <view
        v-for="chat in sessions"
        :key="chat.chatId"
        :class="['chat-item', chat.chatId === activeId ? 'active' : '']"
        @click="$emit('select', chat.chatId)"
      >
        <text class="chat-title">会话 {{ chat.chatId }}</text>
      </view>
    </scroll-view>
    <button class="new-btn" @click="$emit('create')">新建会话</button>
  </view>
</template>

<script lang="ts" setup>
defineProps<{
  sessions: { chatId: string; userId: number }[]
  activeId: string
}>()
defineEmits(['select', 'create'])
</script>

<style scoped lang="scss">
.chat-list {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f7f7f7;
  padding: 20rpx 10rpx;
  box-sizing: border-box;
  overflow: hidden;
}

.chat-scroll {
  flex: 1;
  overflow-y: auto;
  padding-right: 10rpx;
}

.chat-item {
  padding: 20rpx;
  margin-bottom: 10rpx;
  border-radius: 12rpx;
  background-color: #ffffff;
  box-shadow: 0 4rpx 8rpx rgba(0, 0, 0, 0.04);
  transition: background-color 0.3s;
}

.chat-item.active {
  background-color: #e0f0ff;
  font-weight: bold;
  color: #005500;
}

.chat-title {
  font-size: 28rpx;
  word-break: break-all;
}

.new-btn {
  margin-top: 20rpx;
  padding: 20rpx;
  background-color: #00aa00;
  color: white;
  border: none;
  border-radius: 12rpx;
  font-size: 28rpx;
}
</style>
