const CONVERSATIONS_KEY = 'food_chat_conversations'
const CURRENT_CHAT_KEY = 'food_chat_current'

// 生成唯一ID
const generateId = () => Math.random().toString(36).substring(2, 12)

// 获取所有对话
export const getConversations = () => {
  const data = localStorage.getItem(CONVERSATIONS_KEY)
  return data ? JSON.parse(data) : []
}

// 保存对话列表
export const saveConversations = (conversations) => {
  localStorage.setItem(CONVERSATIONS_KEY, JSON.stringify(conversations))
}

// 获取当前对话ID
export const getCurrentChatId = () => {
  return localStorage.getItem(CURRENT_CHAT_KEY) || null
}

// 设置当前对话ID
export const setCurrentChatId = (chatId) => {
  localStorage.setItem(CURRENT_CHAT_KEY, chatId)
}

// 创建新对话
export const createNewConversation = () => {
  const conversations = getConversations()
  const newChat = {
    id: generateId(),
    title: '新对话',
    messages: [],
    createdAt: Date.now(),
    updatedAt: Date.now()
  }
  conversations.unshift(newChat)
  saveConversations(conversations)
  setCurrentChatId(newChat.id)
  return newChat
}

// 获取指定对话
export const getConversation = (chatId) => {
  const conversations = getConversations()
  return conversations.find(c => c.id === chatId)
}

// 更新对话消息
export const updateConversationMessages = (chatId, messages) => {
  const conversations = getConversations()
  const index = conversations.findIndex(c => c.id === chatId)
  if (index !== -1) {
    const firstUserMsg = messages.find(m => m.isUser)
    const title = firstUserMsg
      ? firstUserMsg.content.slice(0, 20) + (firstUserMsg.content.length > 20 ? '...' : '')
      : '新对话'

    conversations[index].messages = messages
    conversations[index].title = title
    conversations[index].updatedAt = Date.now()

    const current = conversations.splice(index, 1)[0]
    conversations.unshift(current)

    saveConversations(conversations)
  }
}

// 删除对话
export const deleteConversation = (chatId) => {
  const conversations = getConversations()
  const filtered = conversations.filter(c => c.id !== chatId)
  saveConversations(filtered)

  if (getCurrentChatId() === chatId) {
    if (filtered.length > 0) {
      setCurrentChatId(filtered[0].id)
    } else {
      createNewConversation()
    }
  }
}

export default {
  generateId,
  getConversations,
  getCurrentChatId,
  setCurrentChatId,
  createNewConversation,
  getConversation,
  updateConversationMessages,
  deleteConversation
}
