import { http } from '@/utils/http'
import type { ChatMessage, UserChatSession } from '../types/chat.d'

/**
 * 请求聊天结果
 * @param prompt 提示词
 * @param chatid 会话ID
 */
export const postChatMessage = (prompt: string, chatId: string) => {
  console.log("发送promt给后端：",prompt);
  return http<string>({
    method: 'POST',
    url: '/deepseek/chat',
    data: {
      prompt,
      chatId
    }
  })
}

/**
 * 获取当前用户所有聊天会话
 */
export const getUserChatSessions = () => {
  return http<UserChatSession[]>({
    method: 'GET',
    url: '/deepseek/chat'
  })
}

/**
 * 根据 chatId 获取该会话的历史消息
 */
export const getChatHistory = (chatid: string) => {
  return http<ChatMessage[]>({
    method: 'GET',
    url: `/deepseek/chat/${chatid}`
  })
}

/**
 * 删除某个聊天会话
 */
export const deleteChatSession = (chatid: string) => {
  return http({
    method: 'DELETE',
    url: `/deepseek/chat/${chatid}`
  })
}

