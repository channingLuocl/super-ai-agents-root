import {
  createConversationApi,
  deleteConversationApi,
  getConversationApi,
  getConversationsApi,
  getCurrentChatIdApi,
  setCurrentChatIdApi,
  updateConversationMessagesApi
} from '../api'

const DEFAULT_USER_ID = 'default'

export const getConversations = async (userId = DEFAULT_USER_ID) => {
  const response = await getConversationsApi(userId)
  return response.data || []
}

export const getVisibleConversations = async (userId = DEFAULT_USER_ID) => {
  const conversations = await getConversations(userId)
  return conversations.filter(c => c.messages.length > 0)
}

export const getCurrentChatId = async (userId = DEFAULT_USER_ID) => {
  const response = await getCurrentChatIdApi(userId)
  return response.data?.chatId || null
}

export const setCurrentChatId = async (chatId, userId = DEFAULT_USER_ID) => {
  await setCurrentChatIdApi(chatId, userId)
}

export const createNewConversation = async (userId = DEFAULT_USER_ID) => {
  const response = await createConversationApi(userId)
  return response.data
}

export const getConversation = async (chatId, userId = DEFAULT_USER_ID) => {
  const response = await getConversationApi(chatId, userId)
  return response.data || null
}

export const updateConversationMessages = async (chatId, messages, userId = DEFAULT_USER_ID) => {
  const response = await updateConversationMessagesApi(chatId, messages, userId)
  return response.data
}

export const deleteConversation = async (chatId, userId = DEFAULT_USER_ID) => {
  const response = await deleteConversationApi(chatId, userId)
  return response.data
}

export default {
  getConversations,
  getVisibleConversations,
  getCurrentChatId,
  setCurrentChatId,
  createNewConversation,
  getConversation,
  updateConversationMessages,
  deleteConversation
}
