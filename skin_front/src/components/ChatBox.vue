<template>
  <view class="chat-container">
    <view v-if="messages.length === 0" class="welcome-box">
      <text class="welcome-title">你好，我是 AI 助手 🤖</text>
      <text class="welcome-desc">点击下方按钮，开始新对话。</text>
      <button class="new-chat-btn" @click="$emit('create')">➕ 新建对话</button>
    </view>

    <scroll-view
      v-else
      class="messages"
      scroll-y
      :scroll-top="scrollTop"
      @scrolltoupper="onScrollTop"
    >
<!--      <view v-for="(msg, i) in messages" :key="i" :class="['msg', msg.role]">
        {{ msg.content }}
      </view>
      -->
  <view v-for="(msg, i) in messages" :key="i" :class="['msg', msg.role]">
    <uaMarkdownVue v-if="msg.role === 'assistant'" :source="msg.content" />
    <view v-else>{{ msg.content }}</view>
  </view>

    </scroll-view>

    <view class="input-bar" :class="{ loading: stylizingLoading }">
      <input
        v-model="inputText"
        class="input"
        placeholder="请输入..."
        confirm-type="send"
        @confirm="send"
        :disabled="stylizingLoading"
      />
      <button
        class="send-btn"
        :disabled="stylizingLoading || !inputText.trim()"
        @click="send"
      >
        <text v-if="stylizingLoading">发送中...</text>
        <text v-else>发送</text>
      </button>
    </view>
  </view>
</template>

<script lang="ts" setup>
// pages/chat.vue
import uaMarkdownVue from './ua-markdown/ua-markdown.vue'
import { ref, watch, nextTick } from 'vue'
import { getChatHistory, postChatMessage } from '../services/chatService'

const props = defineProps<{ chatId: string }>()
const emit = defineEmits(['create'])

const inputText = ref('')
const messages = ref<{ role: 'user' | 'assistant'; content: string }[]>([])
const stylizingLoading = ref(false)
const scrollTop = ref(0)

watch(
  () => props.chatId,
  async (newId) => {
    messages.value = []
    inputText.value = ''
    stylizingLoading.value = false

    if (newId) {
      const history = await getChatHistory(newId)
      if (history?.result) {
        messages.value = history.result.map((msg: any) => ({
          role: msg.role,
          content: msg.content
        }))
        await nextTick()
        scrollToBottom()
      }
    }
  },
  { immediate: true }
)


type Msg = {
  role: 'user' | 'assistant'
  content: string
}

async function send() {
  if (stylizingLoading.value) return
  if (!inputText.value.trim()) return

  stylizingLoading.value = true
  const userMsg = <Msg>{ role: 'user', content: inputText.value }
  messages.value.push(userMsg)

  // const history = await getChatHistory(props.chatId);
  // console.log(history);
  uni.showLoading({
    title:'消息生成中'
  })
  const res = await postChatMessage(userMsg.content,props.chatId);
  uni.hideLoading();

  messages.value.push({ role: 'assistant', content: res.result })

  inputText.value = ''
  stylizingLoading.value = false

  await nextTick()
  scrollToBottom()
}

function scrollToBottom() {
  // 设置scrollTop为大数确保滚到底部
  scrollTop.value = 99999
}

function onScrollTop() {
  // 如果需要处理上拉加载历史，可以写这里
}

</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f0f0f0;
  position: relative;
}

.welcome-box {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 40rpx;
  background: #f7faff;
  text-align: center;
}

.welcome-title {
  font-size: 36rpx;
  font-weight: bold;
  margin-bottom: 20rpx;
}

.welcome-desc {
  font-size: 28rpx;
  color: #666;
  margin-bottom: 40rpx;
}

.new-chat-btn {
  background-color: #409eff;
  color: white;
  padding: 20rpx 40rpx;
  border-radius: 12rpx;
  font-size: 30rpx;
}

.messages {
  flex: 1;
  padding: 20rpx;
  background: #fff;
  overflow-y: auto;
  max-height: 100%;
  /* 预留底部输入框高度，防止遮挡 */
  margin-bottom: 80rpx;
}

.msg {
  margin: 10rpx 0;
  padding: 20rpx;
  border-radius: 10rpx;
  max-width: 80%;
  word-break: break-word;
}

.msg.user {
  align-self: flex-end;
  background-color: #a0d8ef;
}

.msg.assistant {
  align-self: flex-start;
  background-color: #e0e0e0;
}

/* .input-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  border-top: 1px solid #ccc;
  display: flex;
  align-items: center;
  padding: 10rpx 20rpx;
  z-index: 100;
} */

.input-bar {
  /* 删除 position: fixed; bottom: 0; left: 0; right: 0; */
  /* 改成flex布局内的正常块元素 */
  background: #fff;
  border-top: 1px solid #ccc;
  display: flex;
  align-items: center;
  padding: 10rpx 20rpx;
  z-index: 100;
}


.input-bar.loading {
  opacity: 0.7;
}

.input {
  flex: 1;
  height: 48rpx;
  border: 1px solid #ccc;
  border-radius: 24rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
}

.send-btn {
  margin-left: 15rpx;
  background-color: #409eff;
  color: #fff;
  border-radius: 24rpx;
  padding: 10rpx 30rpx;
  font-size: 28rpx;
}

.send-btn:disabled {
  background-color: #a0cfff;
}
</style>
