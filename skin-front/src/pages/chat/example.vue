<template>
  <view class="aichat-container">
    <!-- 顶部导航栏 -->
    <view class="top-nav">
      <!-- 菜单按钮 -->
      <view class="menu-icon" @click="toggleSidebar">
        <!-- 三条横杠图标 -->
        <view class="menu-bar"></view>
        <view class="menu-bar"></view>
        <view class="menu-bar"></view>
      </view>
      <text class="new-chat-text">{{ currentChatTitle || '新对话' }}</text>
      <!-- 显示当前对话标题或默认值 -->
      <!-- 新增的加号按钮 -->
      <view class="add-chat-icon" @click="startNewChat"> + </view>
    </view>

    <!-- 侧边栏 -->
    <view :class="['sidebar', { 'sidebar-open': isSidebarOpen }]">
      <view class="sidebar-header">
        <text class="sidebar-title">所有对话</text>
        <view class="close-sidebar-icon" @click="toggleSidebar">✖</view>
      </view>
      <view class="sidebar-content">
        <view v-if="chatSessions.length === 0 && !isLoadingSessions" class="no-sessions-message">
          暂无对话记录
        </view>
        <view v-else-if="isLoadingSessions" class="loading-sessions-message"> 加载中... </view>
        <view v-else>
          <view v-for="session in chatSessions" :key="session.chatId" class="session-item">
            <view class="session-info" @click="loadChatSession(session)">
              <text class="session-title">{{ session.title || '无标题对话' }}</text>
              <text class="session-date">{{ formatSessionDate(session.createdAt) }}</text>
            </view>
            <view class="session-actions">
              <view class="more-options-icon" @click.stop="toggleOptions(session)"> ... </view>
              <view v-if="activeSessionOptions === session.chatId" class="options-menu">
                <view class="option-item" @click="openRenameModal(session)">重命名</view>
                <view class="option-item delete-option" @click="deleteSession(session)">删除</view>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 侧边栏遮罩层 -->
    <view v-if="isSidebarOpen" class="sidebar-overlay" @click="toggleSidebar"></view>

    <!-- 重命名对话框 -->
    <view v-if="showRenameModal" class="rename-modal-overlay">
      <view class="rename-modal-content">
        <text class="modal-title">重命名对话</text>
        <input
          class="modal-input"
          type="text"
          v-model="newSessionTitle"
          placeholder="请输入新标题"
          @keyup.enter="confirmRename"
        />
        <view class="modal-actions">
          <button class="modal-button cancel-button" @click="cancelRename">取消</button>
          <button
            class="modal-button confirm-button"
            @click="confirmRename"
            :disabled="!newSessionTitle.trim()"
          >
            确定
          </button>
        </view>
      </view>
    </view>

    <!-- 主要内容区域 -->
    <view class="main-content">
      <template v-if="!hasChatStarted">
        <view class="whale-icon-placeholder"> 🐳 </view>
        <text class="greeting-text">嗨！我是 AI医疗助手</text>
        <text class="intro-text"> 请告诉我你的症状吧~ </text>
      </template>

      <view v-else class="chat-messages">
        <view
          v-for="msg in chatHistory"
          :key="msg.id"
          :class="['message-bubble', msg.role === 'user' ? 'user-message' : 'ai-message']"
        >
          <!-- 根据消息角色判断是否使用 uaMarkdownVue -->
          <uaMarkdownVue v-if="msg.role === 'assistant'" :source="msg.content" />
          <text v-else class="message-content">{{ msg.content }}</text>
        </view>
        <view ref="bottomOfChat"></view>
      </view>
    </view>

    <!-- 输入区域 -->
    <view class="input-area">
      <view class="message-input-container">
        <input
          class="message-input"
          type="text"
          placeholder="给AI医疗助手发送消息"
          v-model="messageContent"
          @keyup.enter="sendMessage"
          :disabled="isSending"
        />
        <view
          class="send-button-placeholder"
          :class="{
            'active-send-button': messageContent && !isSending,
            'disabled-send-button': isSending,
          }"
          @click="sendMessage"
        >
          ^
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import {
  getDeepSeekChatResponse,
  getUserChatSessions,
  getChatMessagesByChatId,
  renameChatSession,
  deleteChatSession,
} from '@/services/AIchatService'
import uaMarkdownVue from '@/components/ua-markdown/ua-markdown.vue' // 导入 uaMarkdownVue 组件

// 响应式变量
const messageContent = ref('') // 输入框内容
const currentChatId = ref('') // 当前聊天的唯一标识
const currentChatTitle = ref('') // 当前对话的标题
const chatHistory = ref([]) // 聊天历史记录
const hasChatStarted = ref(false) // 控制初始界面和聊天界面的显示
const isSending = ref(false) // 控制发送按钮的禁用状态，防止重复发送
const bottomOfChat = ref(null) // 用于滚动到聊天底部

const isSidebarOpen = ref(false) // 控制侧边栏的显示/隐藏
const chatSessions = ref([]) // 存储用户的所有聊天会话记录
const isLoadingSessions = ref(false) // 加载聊天会话的状态

const activeSessionOptions = ref(null) // 存储当前显示选项菜单的 session.chatId
const showRenameModal = ref(false) // 控制重命名对话框的显示
const newSessionTitle = ref('') // 重命名输入框的值
const sessionToRename = ref(null) // 存储要重命名的会话对象

// 生成 6 位随机字母数字字符串
const generateRandomId = () => {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
  let result = ''
  for (let i = 0; i < 6; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return result
}

// 滚动到聊天底部
const scrollToBottom = async () => {
  await nextTick() // 确保DOM更新完成后再滚动
  if (bottomOfChat.value) {
    bottomOfChat.value.scrollIntoView({ behavior: 'smooth', block: 'end' })
  }
}

// 发送消息函数
const sendMessage = async () => {
  if (!messageContent.value.trim() || isSending.value) {
    return // 如果内容为空或正在发送，则不执行
  }

  isSending.value = true // 设置为正在发送状态
  // hasChatStarted.value = true; // 标记对话已开始，这行可以根据需要调整位置，确保在首次发送时触发

  const userMessage = messageContent.value
  const messageId = Date.now() // 简单ID，实际项目中可能需要更复杂的ID生成策略

  // 添加用户消息到聊天历史
  chatHistory.value.push({
    id: messageId,
    role: 'user',
    content: userMessage,
  })

  // 清空输入框
  messageContent.value = ''
  scrollToBottom() // 滚动到最新用户消息

  // 为 AI 响应添加一个占位符，内容为空或加载提示
  const aiResponsePlaceholder = {
    id: messageId + 1, // 确保ID唯一
    role: 'assistant',
    content: '正在思考中...', // 显示加载提示
  }
  chatHistory.value.push(aiResponsePlaceholder)
  scrollToBottom() // 滚动到AI消息占位符

  try {
    // 确保在发送第一条消息时切换到聊天界面
    if (!hasChatStarted.value) {
      hasChatStarted.value = true
    }

    // 直接 await 获取 AI 完整响应内容
    const aiResponseContent = await getDeepSeekChatResponse(userMessage, currentChatId.value)
    // 将获取到的完整内容赋值给 AI 消息
    aiResponsePlaceholder.content = aiResponseContent
    scrollToBottom() // 滚动到AI消息

    // 在发送第一条消息并收到回复时，修改对话标题
    // 判断条件：chatHistory 中有用户消息和 AI 消息各一条（总长度为2）
    // 并且当前对话标题是默认的“新对话”或者为空，意味着是首次对话
    if (
      chatHistory.value.length === 2 &&
      chatHistory.value[0].role === 'user' &&
      chatHistory.value[1].role === 'assistant' &&
      (currentChatTitle.value === '' || currentChatTitle.value === '新对话')
    ) {
      try {
        // 不传入 name 参数，让后端自动生成标题
        const newTitle = await renameChatSession(currentChatId.value)
        currentChatTitle.value = newTitle // 更新当前对话标题
        console.log('对话标题已更新为:', newTitle)
        // 刷新侧边栏会话列表，以便新标题显示出来
        await fetchChatSessions()
      } catch (renameError) {
        console.error('自动修改对话标题失败:', renameError)
        // 可以在这里给用户一个提示，但不是关键功能，可以静默处理
      }
    }
  } catch (error) {
    console.error('获取 AI 响应失败:', error)
    // getDeepSeekChatResponse 内部已经返回了错误信息字符串
    aiResponsePlaceholder.content = error.message || '抱歉，获取 AI 响应时发生错误。' // 显示错误信息
    scrollToBottom()
  } finally {
    isSending.value = false // 恢复发送状态
  }
}

// 切换侧边栏显示状态并加载会话
const toggleSidebar = async () => {
  isSidebarOpen.value = !isSidebarOpen.value
  if (isSidebarOpen.value) {
    await fetchChatSessions() // 侧边栏打开时加载会话
  }
}

// 获取用户聊天会话列表
const fetchChatSessions = async () => {
  isLoadingSessions.value = true
  try {
    const sessions = await getUserChatSessions()
    chatSessions.value = sessions
  } catch (error) {
    console.error('加载聊天会话失败:', error)
    uni.showToast({
      icon: 'error',
      title: error.message || '加载对话记录失败',
      duration: 2000,
    })
  } finally {
    isLoadingSessions.value = false
  }
}

// 加载特定聊天会话的历史消息
const loadChatSession = async (session) => {
  currentChatId.value = session.chatId // 更新当前聊天ID
  currentChatTitle.value = session.title || '无标题对话' // 更新当前对话标题
  hasChatStarted.value = true // 切换到聊天界面
  chatHistory.value = [] // 清空当前聊天历史

  uni.showToast({
    icon: 'loading',
    title: '加载对话中...',
    mask: true,
  })

  try {
    const messages = await getChatMessagesByChatId(session.chatId)
    // 确保消息按时间顺序排列，如果后端没有保证
    messages.sort((a, b) => new Date(a.createTime).getTime() - new Date(b.createTime).getTime())
    chatHistory.value = messages
    scrollToBottom()
    uni.hideToast()
    uni.showToast({
      icon: 'none',
      title: `已加载对话: ${session.title || session.chatId}`,
      duration: 1500,
    })
  } catch (error) {
    console.error('加载特定对话消息失败:', error)
    uni.hideToast()
    uni.showToast({
      icon: 'error',
      title: error.message || '加载对话消息失败',
      duration: 2000,
    })
    // 如果加载失败，可以考虑回到初始界面或显示错误
    hasChatStarted.value = false
  } finally {
    toggleSidebar() // 关闭侧边栏
  }
}

// 格式化会话创建日期
const formatSessionDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  // 简单格式化为YYYY-MM-DD HH:mm
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

// 切换会话操作选项菜单的显示
const toggleOptions = (session) => {
  if (activeSessionOptions.value === session.chatId) {
    activeSessionOptions.value = null // 如果点击的是同一个，则关闭
  } else {
    activeSessionOptions.value = session.chatId // 否则打开新的
  }
}

// 打开重命名对话框
const openRenameModal = (session) => {
  sessionToRename.value = session // 存储当前要重命名的会话
  newSessionTitle.value = session.title || '' // 预填充当前标题
  showRenameModal.value = true
  activeSessionOptions.value = null // 关闭操作菜单
}

// 取消重命名
const cancelRename = () => {
  showRenameModal.value = false
  newSessionTitle.value = ''
  sessionToRename.value = null
}

// 确认重命名
const confirmRename = async () => {
  if (!newSessionTitle.value.trim() || !sessionToRename.value) {
    uni.showToast({
      icon: 'none',
      title: '标题不能为空',
      duration: 1500,
    })
    return
  }

  uni.showLoading({ title: '重命名中...' })
  try {
    const updatedTitle = await renameChatSession(
      sessionToRename.value.chatId,
      newSessionTitle.value.trim(),
    )
    // 更新侧边栏中的会话标题
    const index = chatSessions.value.findIndex((s) => s.chatId === sessionToRename.value.chatId)
    if (index !== -1) {
      chatSessions.value[index].title = updatedTitle
    }
    // 如果当前正在查看的会话被重命名了，也更新顶部标题
    if (currentChatId.value === sessionToRename.value.chatId) {
      currentChatTitle.value = updatedTitle
    }
    uni.hideLoading()
    uni.showToast({
      icon: 'success',
      title: '重命名成功',
      duration: 1500,
    })
    cancelRename() // 关闭对话框
  } catch (error) {
    console.error('重命名失败:', error)
    uni.hideLoading()
    uni.showToast({
      icon: 'error',
      title: error.message || '重命名失败',
      duration: 2000,
    })
  }
}

// 删除会话
const deleteSession = async (session) => {
  activeSessionOptions.value = null // 关闭操作菜单

  // 弹出一个确认框，询问用户是否确定删除
  uni.showModal({
    title: '确认删除',
    content: `确定要删除对话“${session.title || '无标题对话'}”吗？`,
    confirmText: '删除',
    cancelText: '取消',
    confirmColor: '#e54d42', // 红色确认按钮
    success: async (res) => {
      if (res.confirm) {
        uni.showLoading({ title: '删除中...' })
        try {
          const deleteMessage = await deleteChatSession(session.chatId)
          uni.hideLoading()
          uni.showToast({
            icon: 'success',
            title: '删除成功',
            duration: 1500,
          })

          // 从会话列表中移除已删除的会话
          chatSessions.value = chatSessions.value.filter((s) => s.chatId !== session.chatId)

          // 如果删除的是当前正在查看的会话，则切换到新对话界面
          if (currentChatId.value === session.chatId) {
            currentChatId.value = generateRandomId() // 生成新的 chatId
            currentChatTitle.value = '' // 清空标题
            chatHistory.value = [] // 清空聊天历史
            hasChatStarted.value = false // 切换回初始欢迎界面
          }
          await fetchChatSessions() // 重新加载会话列表以确保最新状态
        } catch (error) {
          console.error('删除失败:', error)
          uni.hideLoading()
          uni.showToast({
            icon: 'error',
            title: error.message || '删除失败',
            duration: 2000,
          })
        }
      }
    },
  })
}

// 开始新对话（清空当前聊天，回到初始界面，生成新chatId）
const startNewChat = () => {
  // 检查是否已经在初始界面
  if (!hasChatStarted.value && chatHistory.value.length === 0 && currentChatTitle.value === '') {
    uni.showToast({
      icon: 'none',
      title: '您已经在新对话中',
      duration: 1500,
    })
    return
  }

  // 重置所有相关状态
  currentChatId.value = generateRandomId() // 生成新的 chatId
  currentChatTitle.value = '' // 清空标题
  chatHistory.value = [] // 清空聊天历史
  hasChatStarted.value = false // 切换回初始欢迎界面
  messageContent.value = '' // 清空输入框内容
  isSending.value = false // 确保发送状态重置

  uni.showToast({
    icon: 'none',
    title: '已开始新对话',
    duration: 1500,
  })
}

// 组件挂载时生成 chatId
onMounted(() => {
  currentChatId.value = generateRandomId()
  console.log('Generated Chat ID:', currentChatId.value)
  // 可以在这里预加载一次聊天会话，或者只在点击侧边栏时加载
  // fetchChatSessions();
})
</script>

<style>
.aichat-container {
  display: flex;
  flex-direction: column;
  height: 100vh; /* 占满整个视口高度 */
  font-family: sans-serif; /* 常用默认字体 */
}

/* 顶部导航栏 */
.top-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  border-bottom: 1px solid #eee; /* 浅色分隔线 */
  position: relative; /* 用于侧边栏定位 */
  z-index: 100; /* 确保在侧边栏之上 */
}

.menu-icon {
  width: 24px;
  height: 24px;
  display: flex;
  flex-direction: column;
  justify-content: space-around;
  cursor: pointer;
  padding: 5px; /* 增加点击区域 */
}

.menu-bar {
  width: 100%;
  height: 3px;
  background-color: #333;
  border-radius: 2px;
}

.add-chat-icon {
  width: 24px; /* 占位符大小 */
  height: 24px;
  background-color: #ccc; /* 占位符颜色 */
  border-radius: 50%; /* 使其呈圆形 */
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 20px;
  font-weight: bold;
  color: #333;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.add-chat-icon:hover {
  background-color: #e0e0e0;
}

.new-chat-text {
  font-size: 18px;
  font-weight: bold;
}

/* 侧边栏样式 */
.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  width: 70%; /* 侧边栏宽度 */
  max-width: 300px; /* 最大宽度 */
  height: 100vh;
  background-color: #fff;
  box-shadow: 2px 0 6px rgba(0, 0, 0, 0.1);
  transform: translateX(-100%); /* 初始状态：完全隐藏在左侧 */
  transition: transform 0.3s ease-in-out;
  z-index: 200; /* 确保在内容之上 */
  display: flex;
  flex-direction: column;
}

.sidebar-open {
  transform: translateX(0); /* 打开状态：完全显示 */
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  border-bottom: 1px solid #eee;
}

.sidebar-title {
  font-size: 18px;
  font-weight: bold;
}

.close-sidebar-icon {
  font-size: 24px;
  cursor: pointer;
  padding: 5px;
}

.sidebar-content {
  flex-grow: 1;
  overflow-y: auto; /* 允许内容滚动 */
  padding: 10px 0;
}

.no-sessions-message,
.loading-sessions-message {
  padding: 20px;
  text-align: center;
  color: #666;
}

.session-item {
  display: flex;
  align-items: center; /* 垂直居中对齐子项 */
  justify-content: space-between; /* 标题和操作按钮两端对齐 */
  padding: 10px 15px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.2s ease;
  position: relative; /* 用于定位选项菜单 */
}

.session-item:hover {
  background-color: #f5f5f5;
}

.session-info {
  flex-grow: 1; /* 占据可用空间 */
  display: flex;
  flex-direction: column;
  overflow: hidden; /* 隐藏溢出内容 */
}

.session-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 5px;
  white-space: nowrap; /* 不换行 */
  overflow: hidden; /* 隐藏超出部分 */
  text-overflow: ellipsis; /* 显示省略号 */
}

.session-date {
  font-size: 12px;
  color: #999;
}

.session-actions {
  position: relative; /* 用于定位 options-menu */
  margin-left: 10px; /* 与标题的间距 */
}

.more-options-icon {
  font-size: 20px;
  font-weight: bold;
  cursor: pointer;
  padding: 5px;
  line-height: 1; /* 确保垂直居中 */
}

.options-menu {
  position: absolute;
  top: 100%; /* 位于图标下方 */
  right: 0;
  background-color: #fff;
  border: 1px solid #ddd;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  min-width: 120px;
  z-index: 210; /* 确保在侧边栏内容之上 */
  overflow: hidden; /* 确保圆角有效 */
}

.option-item {
  padding: 10px 15px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  transition: background-color 0.2s ease;
  white-space: nowrap; /* 防止选项文字换行 */
}

.option-item:hover {
  background-color: #f0f0f0;
}

.delete-option {
  color: #e54d42; /* 红色表示删除 */
}

/* 侧边栏遮罩层 */
.sidebar-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.5); /* 半透明黑色 */
  z-index: 150; /* 在内容和侧边栏之间 */
}

/* 重命名对话框样式 */
.rename-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 300; /* 确保在所有内容之上 */
}

.rename-modal-content {
  background-color: #fff;
  border-radius: 12px;
  padding: 25px;
  width: 80%;
  max-width: 400px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.modal-title {
  font-size: 20px;
  font-weight: bold;
  text-align: center;
  color: #333;
}

.modal-input {
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 10px 15px;
  font-size: 16px;
  outline: none;
  transition: border-color 0.2s ease;
}

.modal-input:focus {
  border-color: #007bff;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 10px;
}

.modal-button {
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 16px;
  cursor: pointer;
  border: none;
  transition: background-color 0.2s ease;
}

.cancel-button {
  background-color: #e0e0e0;
  color: #333;
}

.cancel-button:hover {
  background-color: #d0d0d0;
}

.confirm-button {
  background-color: #007bff;
  color: white;
}

.confirm-button:hover:not(:disabled) {
  background-color: #0056b3;
}

.confirm-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 主要内容区域 */
.main-content {
  flex-grow: 1; /* 占据可用空间 */
  display: flex;
  flex-direction: column;
  justify-content: center; /* 垂直居中内容 */
  align-items: center; /* 水平居中内容 */
  text-align: center;
  padding: 20px;
  overflow-y: auto; /* 允许内容滚动 */
}

.whale-icon-placeholder {
  font-size: 80px; /* 调整表情符号图标大小 */
  margin-bottom: 20px;
}

.greeting-text {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 10px;
}

.intro-text {
  font-size: 16px;
  color: #666;
  max-width: 80%; /* 限制文本宽度以提高可读性 */
  line-height: 1.5;
}

/* 聊天消息显示区域 */
.chat-messages {
  width: 100%;
  height: 100%; /* 填充父容器 */
  display: flex;
  flex-direction: column;
  padding: 0 15px; /* 左右内边距 */
  box-sizing: border-box; /* 包含内边距在宽度内 */
}

.message-bubble {
  max-width: 70%; /* 限制气泡宽度 */
  padding: 10px 15px;
  border-radius: 15px;
  margin-bottom: 10px;
  word-wrap: break-word; /* 自动换行 */
}

.user-message {
  align-self: flex-end; /* 右对齐 */
  background-color: #007bff; /* 蓝色背景 */
  color: white;
  border-bottom-right-radius: 2px; /* 右下角稍微变直，模拟聊天气泡 */
}

.ai-message {
  align-self: flex-start; /* 左对齐 */
  background-color: #e0e0e0; /* 灰色背景 */
  color: #333;
  border-bottom-left-radius: 2px; /* 左下角稍微变直 */
}

/* 输入区域 */
.input-area {
  padding: 15px;
  border-top: 1px solid #eee; /* 浅色分隔线 */
  background-color: #f8f8f8; /* 输入区域的浅色背景 */
}

.message-input-container {
  background-color: #fff;
  border: 1px solid #ddd;
  border-radius: 25px; /* 输入框的圆角 */
  padding: 8px 15px;
  display: flex; /* 使用 flexbox 对齐输入框和发送按钮 */
  align-items: center;
}

.message-input {
  flex-grow: 1; /* 允许输入框占据大部分空间 */
  border: none;
  outline: none;
  font-size: 16px;
}

.send-button-placeholder {
  font-size: 24px;
  font-weight: bold;
  color: #333; /* 无内容时的默认颜色 */
  margin-left: 10px;
  width: 36px;
  height: 36px;
  display: flex;
  justify-content: center;
  align-items: center;
  border-radius: 50%; /* 使其呈圆形 */
  background-color: #e0e0e0; /* 默认背景颜色 */
  transition: all 0.2s ease-in-out; /* 颜色变化的平滑过渡 */
  cursor: pointer; /* 添加手型光标 */
}

/* 活动（蓝色）状态的新类 */
.active-send-button {
  color: #fff; /* 激活时箭头的颜色为白色 */
  background-color: #007bff; /* 激活时背景颜色为蓝色 */
}

/* 禁用状态的按钮样式 */
.disabled-send-button {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
