<template>
  <view class="chat-page">
    <view class="topbar">
      <view class="topbar-btn" @click="toggleSidebar">菜单</view>
      <view class="topbar-title">{{ activeSessionTitle }}</view>
      <view class="topbar-btn" @click="createChat">+</view>
    </view>

    <view class="layout">
      <view class="sidebar" :class="{ open: sidebarOpen }">
        <view class="sidebar-head">
          <text class="sidebar-title">会话列表</text>
          <view class="new-btn" @click="createChat">新建</view>
        </view>

        <scroll-view class="session-scroll" scroll-y>
          <view
            v-for="session in sortedSessions"
            :key="session.chatId"
            class="session-item"
            :class="{ active: session.chatId === activeChatId }"
            @click="selectChat(session.chatId)"
          >
            <view class="session-main">
              <text class="session-name">{{ getSessionTitle(session.chatId) }}</text>
              <text class="session-time">{{ formatTime(session.updatedAt || session.createdAt) }}</text>
            </view>
            <view class="session-menu-wrap">
              <text class="session-menu-trigger" @click.stop="toggleSessionMenu(session.chatId)">...</text>
              <view v-if="activeMenuChatId === session.chatId" class="session-menu">
                <view class="menu-item" @click.stop="openRenameModal(session.chatId)">重命名</view>
                <view class="menu-item danger" @click.stop="removeChat(session.chatId)">删除</view>
              </view>
            </view>
          </view>

          <view v-if="sortedSessions.length === 0" class="empty-session">
            <text>暂无会话</text>
          </view>
        </scroll-view>
      </view>

      <view v-if="sidebarOpen" class="mask" @click="toggleSidebar"></view>

      <view class="chat-panel">
        <scroll-view
          class="message-scroll"
          scroll-y
          :scroll-top="messageScrollTop"
          :scroll-into-view="scrollAnchorId"
          :scroll-with-animation="true"
        >
          <view v-if="messages.length === 0" class="empty-chat">
            <text class="empty-title">开始新的对话</text>
            <text class="empty-desc">输入你的问题，AI 会在这里回复你。</text>
          </view>

          <view v-for="msg in messages" :key="msg.id" :id="`msg-${msg.id}`" class="msg-row" :class="msg.role">
            <view class="msg-bubble">
              <uaMarkdownVue v-if="msg.role === 'assistant'" :source="msg.content" />
              <text v-else user-select>{{ msg.content }}</text>
            </view>
          </view>
          <view id="bottom-anchor"></view>
        </scroll-view>

        <view class="composer">
          <textarea
            v-model="inputText"
            class="composer-input"
            :maxlength="2000"
            auto-height
            placeholder="请输入消息"
          />
          <button class="send-btn" :disabled="sending || !inputText.trim()" @click="sendMessage">
            {{ sending ? '发送中...' : '发送' }}
          </button>
        </view>
      </view>
    </view>

    <view v-if="renameModalVisible" class="rename-mask">
      <view class="rename-modal">
        <text class="rename-title">重命名会话</text>
        <input v-model="renameText" class="rename-input" maxlength="40" placeholder="请输入新标题" />
        <view class="rename-actions">
          <button class="ghost" @click="closeRenameModal">取消</button>
          <button class="primary" :disabled="!renameText.trim()" @click="confirmRename">保存</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import uaMarkdownVue from '@/components/ua-markdown/ua-markdown.vue'
import { deleteChatSession, getChatHistory, getUserChatSessions, postChatMessage } from '@/services/chatService'
import type { ChatMessage, UserChatSession } from '@/types/chat.d'

type SessionMeta = {
  title?: string
  updatedAt?: string
}

type UiSession = UserChatSession & {
  updatedAt?: string
}

const SESSION_META_KEY = 'CHAT_SESSION_META'
const MAX_TITLE_LENGTH = 40

const sidebarOpen = ref(false)
const sending = ref(false)
const inputText = ref('')
const activeChatId = ref('')
const activeMenuChatId = ref('')
const scrollAnchorId = ref('bottom-anchor')
const messageScrollTop = ref(0)

const renameModalVisible = ref(false)
const renameChatId = ref('')
const renameText = ref('')

const sessions = ref<UiSession[]>([])
const messages = ref<ChatMessage[]>([])
const sessionMeta = ref<Record<string, SessionMeta>>({})

const activeSessionTitle = computed(() => {
  if (!activeChatId.value) return 'AI问诊'
  return getSessionTitle(activeChatId.value)
})

const sortedSessions = computed(() => {
  return [...sessions.value].sort((a, b) => {
    const at = new Date(a.updatedAt || a.createdAt || 0).getTime()
    const bt = new Date(b.updatedAt || b.createdAt || 0).getTime()
    return bt - at
  })
})

const createChatId = () => `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`

const normalizeMessages = (list: any[]): ChatMessage[] => {
  return list.map((item, idx) => ({
    id: Number(item.id ?? idx + 1),
    chatId: String(item.chatId ?? ''),
    role: item.role === 'assistant' ? 'assistant' : 'user',
    content: String(item.content ?? ''),
    createdAt: String(item.createdAt ?? item.createTime ?? '')
  }))
}

const saveSessionMeta = () => {
  uni.setStorageSync(SESSION_META_KEY, sessionMeta.value)
}

const loadSessionMeta = () => {
  const raw = uni.getStorageSync(SESSION_META_KEY)
  sessionMeta.value = raw && typeof raw === 'object' ? raw : {}
}

const touchSession = (chatId: string) => {
  const prev = sessionMeta.value[chatId] || {}
  sessionMeta.value[chatId] = {
    ...prev,
    updatedAt: new Date().toISOString()
  }
  saveSessionMeta()
}

const getSessionTitle = (chatId: string) => {
  const metaTitle = sessionMeta.value[chatId]?.title
  if (metaTitle?.trim()) return metaTitle.trim()
  return `会话 ${chatId.slice(0, 8)}`
}

const generateTitleFromText = (text: string) => {
  const normalized = text.replace(/\s+/g, ' ').trim()
  if (!normalized) return '新会话'
  return normalized.slice(0, MAX_TITLE_LENGTH)
}

const ensureSessionExists = (chatId: string) => {
  if (sessions.value.some((s) => s.chatId === chatId)) return
  sessions.value.unshift({
    id: 0,
    userId: 0,
    chatId,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  })
}

const refreshSessions = async () => {
  const res = await getUserChatSessions()
  const serverSessions = (res.result || []).map((raw: any) => {
    const createdAt = String(raw.createdAt || raw.createTime || '')
    return {
      id: Number(raw.id || 0),
      userId: Number(raw.userId || raw.userid || 0),
      chatId: String(raw.chatId || ''),
      createdAt,
      updatedAt: String(raw.updatedAt || raw.lastActivityTime || createdAt)
    } as UiSession
  })

  const localOnly = sessions.value.filter((s) => !serverSessions.some((x) => x.chatId === s.chatId))
  sessions.value = [...serverSessions, ...localOnly].filter(
    (item, idx, arr) => arr.findIndex((x) => x.chatId === item.chatId) === idx
  )

  if (!activeChatId.value && sessions.value.length > 0) {
    activeChatId.value = sessions.value[0].chatId
    await loadMessages(activeChatId.value)
  }
}

const loadMessages = async (chatId: string) => {
  if (!chatId) return
  const res = await getChatHistory(chatId)
  messages.value = normalizeMessages(res.result || [])
  scrollToBottom()
}

const selectChat = async (chatId: string) => {
  activeChatId.value = chatId
  activeMenuChatId.value = ''
  sidebarOpen.value = false
  await loadMessages(chatId)
}

const createChat = () => {
  const id = createChatId()
  activeChatId.value = id
  messages.value = []
  ensureSessionExists(id)
  touchSession(id)
  sidebarOpen.value = false
}

const toggleSidebar = () => {
  sidebarOpen.value = !sidebarOpen.value
  if (!sidebarOpen.value) activeMenuChatId.value = ''
}

const toggleSessionMenu = (chatId: string) => {
  activeMenuChatId.value = activeMenuChatId.value === chatId ? '' : chatId
}

const openRenameModal = (chatId: string) => {
  renameChatId.value = chatId
  renameText.value = sessionMeta.value[chatId]?.title || ''
  renameModalVisible.value = true
  activeMenuChatId.value = ''
}

const closeRenameModal = () => {
  renameModalVisible.value = false
  renameChatId.value = ''
  renameText.value = ''
}

const confirmRename = () => {
  const chatId = renameChatId.value
  if (!chatId || !renameText.value.trim()) return
  const prev = sessionMeta.value[chatId] || {}
  sessionMeta.value[chatId] = {
    ...prev,
    title: renameText.value.trim().slice(0, MAX_TITLE_LENGTH),
    updatedAt: new Date().toISOString()
  }
  saveSessionMeta()
  sessions.value = sessions.value.map((s) => (s.chatId === chatId ? { ...s, updatedAt: new Date().toISOString() } : s))
  closeRenameModal()
}

const removeChat = async (chatId: string) => {
  activeMenuChatId.value = ''
  const ok = await new Promise<boolean>((resolve) => {
    uni.showModal({
      title: '删除会话',
      content: '确定删除该会话及其全部消息吗？',
      success: (res) => resolve(!!res.confirm)
    })
  })
  if (!ok) return

  try {
    await deleteChatSession(chatId)
  } catch (_error) {
    // keep local cleanup
  }

  sessions.value = sessions.value.filter((s) => s.chatId !== chatId)
  const nextMeta = { ...sessionMeta.value }
  delete nextMeta[chatId]
  sessionMeta.value = nextMeta
  saveSessionMeta()

  if (activeChatId.value === chatId) {
    if (sessions.value.length > 0) {
      activeChatId.value = sessions.value[0].chatId
      await loadMessages(activeChatId.value)
    } else {
      activeChatId.value = ''
      messages.value = []
    }
  }
}

const scrollToBottom = () => {
  scrollAnchorId.value = ''
  nextTick(() => {
    messageScrollTop.value += 100000
    setTimeout(() => {
      scrollAnchorId.value = 'bottom-anchor'
      messageScrollTop.value += 100000
    }, 30)
  })
}

const sendMessage = async () => {
  const content = inputText.value.trim()
  if (!content || sending.value) return

  if (!activeChatId.value) {
    activeChatId.value = createChatId()
  }
  const chatId = activeChatId.value
  ensureSessionExists(chatId)

  const baseId = Date.now()
  messages.value.push({
    id: baseId,
    chatId,
    role: 'user',
    content,
    createdAt: new Date().toISOString()
  })
  inputText.value = ''
  scrollToBottom()

  const aiPlaceholder: ChatMessage = {
    id: baseId + 1,
    chatId,
    role: 'assistant',
    content: '思考中...',
    createdAt: new Date().toISOString()
  }
  messages.value.push(aiPlaceholder)
  scrollToBottom()

  sending.value = true
  try {
    const res = await postChatMessage(content, chatId)
    aiPlaceholder.content = res.result || '暂无回复'

    if (!sessionMeta.value[chatId]?.title && messages.value.filter((m) => m.role === 'user').length === 1) {
      sessionMeta.value[chatId] = {
        ...sessionMeta.value[chatId],
        title: generateTitleFromText(content),
        updatedAt: new Date().toISOString()
      }
      saveSessionMeta()
    }

    touchSession(chatId)
    sessions.value = sessions.value.map((s) =>
      s.chatId === chatId ? { ...s, updatedAt: new Date().toISOString() } : s
    )
    await refreshSessions()
    scrollToBottom()
  } catch (error: any) {
    aiPlaceholder.content = error?.message || '发送失败，请稍后重试'
    scrollToBottom()
  } finally {
    sending.value = false
  }
}

const formatTime = (value?: string) => {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return ''
  const mm = `${d.getMonth() + 1}`.padStart(2, '0')
  const dd = `${d.getDate()}`.padStart(2, '0')
  const hh = `${d.getHours()}`.padStart(2, '0')
  const mi = `${d.getMinutes()}`.padStart(2, '0')
  return `${mm}-${dd} ${hh}:${mi}`
}

onMounted(async () => {
  loadSessionMeta()
  await refreshSessions()
  if (!activeChatId.value) createChat()
})
</script>

<style scoped lang="scss">
.chat-page {
  height: 100vh;
  width: 100%;
  max-width: 100vw;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #f8fafc 0%, #eef3f8 100%);
  overflow: hidden;
  box-sizing: border-box;
}

.topbar {
  height: 96rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24rpx;
  background: rgba(255, 255, 255, 0.92);
  border-bottom: 1px solid #e7edf5;
}

.topbar-btn {
  width: 64rpx;
  height: 64rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  border: 1px solid #e4eaf3;
}

.topbar-title {
  flex: 1;
  text-align: center;
  padding: 0 20rpx;
  font-size: 30rpx;
  font-weight: 600;
  color: #1f2937;
}

.layout {
  position: relative;
  flex: 1;
  width: 100%;
  max-width: 100vw;
  display: flex;
  overflow: hidden;
  min-height: 0;
  box-sizing: border-box;
}

.sidebar {
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  width: 520rpx;
  max-width: 78vw;
  background: #ffffff;
  border-right: 1px solid #e8edf4;
  transform: translateX(-100%);
  transition: transform 0.24s ease;
  z-index: 20;
  display: flex;
  flex-direction: column;
}

.sidebar.open {
  transform: translateX(0);
}

.sidebar-head {
  height: 100rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24rpx;
  border-bottom: 1px solid #eff3f8;
}

.sidebar-title {
  font-size: 30rpx;
  color: #0f172a;
  font-weight: 600;
}

.new-btn {
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background: #e9f2ff;
  color: #1d4ed8;
  font-size: 24rpx;
}

.session-scroll {
  flex: 1;
}

.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22rpx 20rpx;
  border-bottom: 1px solid #f2f5fa;
}

.session-item.active {
  background: #f4f8ff;
}

.session-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.session-name {
  font-size: 27rpx;
  color: #111827;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 360rpx;
}

.session-time {
  font-size: 22rpx;
  color: #6b7280;
}

.session-menu-wrap {
  position: relative;
}

.session-menu-trigger {
  width: 44rpx;
  height: 44rpx;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
}

.session-menu {
  position: absolute;
  top: 48rpx;
  right: 0;
  background: #fff;
  border: 1px solid #e6ecf4;
  border-radius: 14rpx;
  box-shadow: 0 12rpx 28rpx rgba(22, 34, 56, 0.14);
  overflow: hidden;
  min-width: 160rpx;
  z-index: 21;
}

.menu-item {
  padding: 16rpx 18rpx;
  font-size: 24rpx;
  color: #1f2937;
}

.menu-item.danger {
  color: #dc2626;
}

.empty-session {
  padding: 30rpx;
  text-align: center;
  color: #94a3b8;
  font-size: 24rpx;
}

.mask {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.28);
  z-index: 19;
}

.chat-panel {
  flex: 1;
  width: 100%;
  max-width: 100%;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  box-sizing: border-box;
}

.message-scroll {
  flex: 1;
  height: 0;
  width: 100%;
  max-width: 100%;
  min-height: 0;
  padding: 20rpx 24rpx 0;
  box-sizing: border-box;
}

.empty-chat {
  padding-top: 120rpx;
  text-align: center;
}

.empty-title {
  font-size: 38rpx;
  color: #0f172a;
  font-weight: 700;
}

.empty-desc {
  margin-top: 18rpx;
  font-size: 26rpx;
  color: #64748b;
  line-height: 1.6;
}

.msg-row {
  display: flex;
  width: 100%;
  max-width: 100%;
  margin-bottom: 20rpx;
  box-sizing: border-box;
}

.msg-row.user {
  justify-content: flex-end;
}

.msg-row.assistant {
  justify-content: flex-start;
}

.msg-bubble {
  max-width: 82%;
  width: fit-content;
  border-radius: 22rpx;
  padding: 16rpx 20rpx;
  font-size: 28rpx;
  line-height: 1.6;
  word-break: break-word;
  overflow-wrap: anywhere;
  box-sizing: border-box;
}

.msg-row.user .msg-bubble {
  background: linear-gradient(120deg, #2b7fff, #2b6bff);
  color: #ffffff;
  border-bottom-right-radius: 8rpx;
}

.msg-row.assistant .msg-bubble {
  background: #ffffff;
  color: #111827;
  border: 1px solid #e9edf4;
  border-bottom-left-radius: 8rpx;
}

.composer {
  padding: 16rpx 20rpx calc(16rpx + env(safe-area-inset-bottom));
  width: 100%;
  max-width: 100%;
  background: rgba(255, 255, 255, 0.95);
  border-top: 1px solid #e6ecf5;
  display: flex;
  align-items: flex-end;
  gap: 14rpx;
  box-sizing: border-box;
}

.composer-input {
  flex: 1;
  min-width: 0;
  min-height: 76rpx;
  max-height: 220rpx;
  background: #f8fafc;
  border: 1px solid #e4ebf4;
  border-radius: 20rpx;
  padding: 16rpx 18rpx;
  font-size: 28rpx;
}

.send-btn {
  width: 150rpx;
  height: 76rpx;
  border-radius: 20rpx;
  background: #2563eb;
  color: #fff;
  font-size: 27rpx;
  border: none;
}

.send-btn[disabled] {
  background: #9fbaf1;
}

.rename-mask {
  position: fixed;
  inset: 0;
  background: rgba(2, 6, 23, 0.4);
  z-index: 40;
  display: flex;
  align-items: center;
  justify-content: center;
}

.rename-modal {
  width: 78vw;
  background: #fff;
  border-radius: 18rpx;
  padding: 24rpx;
}

.rename-title {
  font-size: 32rpx;
  color: #0f172a;
  font-weight: 600;
}

.rename-input {
  margin-top: 16rpx;
  height: 72rpx;
  border-radius: 14rpx;
  border: 1px solid #dfe7f2;
  padding: 0 16rpx;
  font-size: 27rpx;
}

.rename-actions {
  margin-top: 20rpx;
  display: flex;
  justify-content: flex-end;
  gap: 12rpx;
}

.rename-actions .ghost,
.rename-actions .primary {
  min-width: 120rpx;
  height: 64rpx;
  border-radius: 14rpx;
  border: none;
  font-size: 25rpx;
}

.rename-actions .ghost {
  background: #eef2f7;
  color: #334155;
}

.rename-actions .primary {
  background: #2563eb;
  color: #fff;
}

.rename-actions .primary[disabled] {
  background: #9db8ef;
}
</style>
