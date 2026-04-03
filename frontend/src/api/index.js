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
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
    .join('&')

  const fullUrl = `${API_BASE_URL}${url}?${queryString}`
  const eventSource = new EventSource(fullUrl)

  return eventSource
}

export const chatWithFood = (message, chatId = 'default') => {
  return connectSSE('/ai/food/chat/stream', { message, chatId })
}

export const chatWithFoodRag = (message, chatId = 'default') => {
  return connectSSE('/ai/food/rag/stream', { message, chatId })
}

export default {
  chatWithFood,
  chatWithFoodRag
}
