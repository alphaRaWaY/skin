export type ChatMessage = {
  id: number
  chatId: string
  role: 'user' | 'assistant'
  content: string
  createdAt: string
}

export type UserChatSession = {
  id: number
  userId: number
  chatId: string
  createdAt: string
}
