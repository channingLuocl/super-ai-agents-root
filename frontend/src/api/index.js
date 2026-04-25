import axios from 'axios'

const API_BASE_URL = process.env.NODE_ENV === 'production'
  ? '/api'
  : 'http://localhost:8123/api'

const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000
})

export const connectSSE = (url, params) => {
  const queryString = Object.keys(params)
    .filter(key => params[key] !== undefined && params[key] !== null && params[key] !== '')
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
    .join('&')

  const fullUrl = `${API_BASE_URL}${url}?${queryString}`
  const eventSource = new EventSource(fullUrl)

  return eventSource
}

export const chatWithFoodAgent = (message, chatId = 'default', location = null) => {
  return connectSSE('/ai/food/agent/stream', {
    message,
    chatId,
    longitude: location?.longitude,
    latitude: location?.latitude
  })
}

// 获取用户画像
export const getUserProfile = (userId) => {
  return request.get(`/memory/profile/${userId}`)
}

export const getConversationsApi = (userId = 'default') => {
  return request.get('/chat/conversations', { params: { userId } })
}

export const createConversationApi = (userId = 'default') => {
  return request.post('/chat/conversations', null, { params: { userId } })
}

export const getConversationApi = (chatId, userId = 'default') => {
  return request.get(`/chat/conversations/${chatId}`, { params: { userId } })
}

export const updateConversationMessagesApi = (chatId, messages, userId = 'default') => {
  return request.put(`/chat/conversations/${chatId}/messages`, messages, { params: { userId } })
}

export const deleteConversationApi = (chatId, userId = 'default') => {
  return request.delete(`/chat/conversations/${chatId}`, { params: { userId } })
}

export const getCurrentChatIdApi = (userId = 'default') => {
  return request.get('/chat/current', { params: { userId } })
}

export const setCurrentChatIdApi = (chatId, userId = 'default') => {
  return request.put(`/chat/current/${chatId}`, null, { params: { userId } })
}

export default {
  chatWithFoodAgent,
  getUserProfile,
  getConversationsApi,
  createConversationApi,
  getConversationApi,
  updateConversationMessagesApi,
  deleteConversationApi,
  getCurrentChatIdApi,
  setCurrentChatIdApi
}
