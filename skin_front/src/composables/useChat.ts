import { ref } from 'vue'
import type { ChatMessage } from '../types/chat'


export function useChat(chatId: string) {
  const messages = ref<ChatMessage[]>([])
  const loading = ref(false)

  const loadHistory = async () => {
    const res = await getChatMessages(chatId)
    messages.value = res
  }

  const sendMessage = async (content: string) => {
    loading.value = true
    messages.value.push({ role: 'user', content })
    const reply = await postSendMessage(content, chatId)
    messages.value.push({ role: 'assistant', content: reply })
    loading.value = false
  }

  return {
    messages,
    loading,
    sendMessage,
    loadHistory
  }
}
