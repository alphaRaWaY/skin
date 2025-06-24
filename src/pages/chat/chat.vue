<template>
  <view class="chat-container">
    <!-- 侧边栏 -->
    <view class="sidebar" :class="{ collapsed: !sidebarVisible }">
      <ChatList
        :sessions="sessions"
        :activeId="activeChatId"
        @select="setActiveChat"
        @create="createChat"
      />
    </view>

    <!-- 切换按钮 -->
    <view
      class="toggle-btn"
      :class="{ collapsed: !sidebarVisible }"
      @click="toggleSidebar"
    >
      <text>{{ sidebarVisible ? '⮜' : '⮞' }}</text>
    </view>

    <!-- 主聊天区域 -->
    <view class="chat-main" :class="{ shifted: !sidebarVisible }">
      <ChatBox :chatId="activeChatId" @create="createChat" />
    </view>
  </view>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import ChatList from '@/components/ChatList.vue'
import ChatBox from '@/components/ChatBox.vue'
import { getUserChatSessions } from '@/services/chatService'
import type { UserChatSession } from '@/types/chat'

const sessions = ref<UserChatSession[]>([])
const activeChatId = ref('')
const sidebarVisible = ref(true)

onMounted(async () => {
  const res = await getUserChatSessions()
  sessions.value = res.result
  if (sessions.value.length > 0) {
    activeChatId.value = sessions.value[0].chatId
  }
})

function setActiveChat(id: string) {
  activeChatId.value = id
}

function createChat() {
  const newId = Date.now().toString()
  sessions.value.unshift({ chatId: newId, userId: 0 })
  activeChatId.value = newId
}

function toggleSidebar() {
  sidebarVisible.value = !sidebarVisible.value
}
</script>

<style scoped lang="scss">
.chat-container {
  display: flex;
  height: 100vh;
  background-color: #f9f9f9;
  overflow: hidden;
  position: relative;
}

.sidebar {
  width: 260rpx;
  background-color: #f5f5f5;
  border-right: 1px solid #ddd;
  transition: transform 0.3s ease-in-out;
  z-index: 1;
  flex-shrink: 0;

  &.collapsed {
    transform: translateX(-100%);
  }
}

.toggle-btn {
  position: absolute;
  top: 20rpx;
  left: 260rpx;
  width: 40rpx;
  height: 40rpx;
  background-color: #ffffff;
  border: 1px solid #ddd;
  border-radius: 0 8rpx 8rpx 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #333;
  font-size: 24rpx;
  z-index: 2;
  transition: left 0.3s ease;

  &.collapsed {
    left: 0;
  }
}



.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #ffffff;
  overflow: hidden;
  transition: margin-left 0.3s ease-in-out;

  &.shifted {
    margin-left: -260rpx;
  }
}
</style>
