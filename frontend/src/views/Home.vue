<template>
  <AppLayout>
    <!-- 顶部导航 -->
    <header class="top-bar">
      <div class="top-bar-left">
        <button class="menu-btn md-hidden">
          <span class="material-symbols-outlined">menu</span>
        </button>
        <span class="chat-title">{{ currentChatTitle }}</span>
      </div>
      <div class="top-bar-right">
        <button class="profile-btn" @click="toggleProfile" title="我的偏好">
          <span class="material-symbols-outlined">person</span>
        </button>
      </div>
    </header>

    <!-- 用户画像弹窗 -->
    <div class="profile-modal" v-if="showProfile" @click.self="toggleProfile">
      <div class="profile-content">
        <div class="profile-header">
          <h3>我的美食偏好</h3>
          <button class="close-btn" @click="toggleProfile">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        <div class="profile-body" v-if="userProfile">
          <div class="profile-item" v-if="userProfile.taste?.preferred?.length">
            <span class="profile-label">口味偏好</span>
            <span class="profile-tags">
              <span class="tag" v-for="item in userProfile.taste.preferred" :key="item">{{ item }}</span>
            </span>
          </div>
          <div class="profile-item" v-if="userProfile.taste?.disliked?.length">
            <span class="profile-label">不喜欢</span>
            <span class="profile-tags">
              <span class="tag dislike" v-for="item in userProfile.taste.disliked" :key="item">{{ item }}</span>
            </span>
          </div>
          <div class="profile-item" v-if="userProfile.restrictions?.allergies?.length">
            <span class="profile-label">过敏食材</span>
            <span class="profile-tags">
              <span class="tag warning" v-for="item in userProfile.restrictions.allergies" :key="item">{{ item }}</span>
            </span>
          </div>
          <div class="profile-item" v-if="userProfile.restrictions?.avoidIngredients?.length">
            <span class="profile-label">忌口</span>
            <span class="profile-tags">
              <span class="tag" v-for="item in userProfile.restrictions.avoidIngredients" :key="item">{{ item }}</span>
            </span>
          </div>
          <div class="profile-item" v-if="userProfile.favoriteCuisines?.length">
            <span class="profile-label">喜欢菜系</span>
            <span class="profile-tags">
              <span class="tag primary" v-for="item in userProfile.favoriteCuisines" :key="item">{{ item }}</span>
            </span>
          </div>
          <div class="profile-item" v-if="userProfile.cookingLevel">
            <span class="profile-label">烹饪水平</span>
            <span class="profile-value">{{ userProfile.cookingLevel }}</span>
          </div>
          <div class="profile-item" v-if="userProfile.healthGoals?.length">
            <span class="profile-label">健康目标</span>
            <span class="profile-tags">
              <span class="tag" v-for="item in userProfile.healthGoals" :key="item">{{ item }}</span>
            </span>
          </div>
          <div class="profile-empty" v-if="!userProfile.taste?.preferred?.length && !userProfile.restrictions?.allergies?.length">
            <p>暂无偏好记录</p>
            <p class="hint">与我多聊几句，我会记住你的口味偏好哦~</p>
          </div>
        </div>
        <div class="profile-body" v-else>
          <div class="profile-loading">加载中...</div>
        </div>
      </div>
    </div>

    <!-- 对话区域 -->
    <section class="chat-canvas hide-scrollbar" ref="chatCanvas">
      <div class="chat-content">
        <!-- 欢迎消息 -->
        <div class="welcome-section" v-if="messages.length === 0">
          <div class="welcome-icon">
            <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 1;">restaurant_menu</span>
          </div>
          <h2>你好，我是馋嘴小迪</h2>
          <p>我可以帮你推荐食谱、分析营养、解答烹饪问题，或者帮你规划健康饮食。今天想吃什么？</p>

          <!-- 快捷操作 -->
          <div class="quick-actions">
            <button class="quick-btn" @click="sendQuickMessage('推荐一道家常菜')">
              <div class="quick-icon primary">
                <span class="material-symbols-outlined">lunch_dining</span>
              </div>
              <div class="quick-text">
                <div class="quick-title">推荐一道家常菜</div>
                <div class="quick-desc">简单易做的美味食谱</div>
              </div>
            </button>
            <button class="quick-btn" @click="sendQuickMessage('分析这道菜的营养成分')">
              <div class="quick-icon tertiary">
                <span class="material-symbols-outlined">nutrition</span>
              </div>
              <div class="quick-text">
                <div class="quick-title">分析营养成分</div>
                <div class="quick-desc">了解食物的营养价值</div>
              </div>
            </button>
          </div>
        </div>

        <!-- 聊天记录 -->
        <div class="messages-area" v-if="messages.length > 0 || isLoading">
          <div v-for="(msg, index) in messages" :key="index" class="message-row" :class="{ 'user-row': msg.isUser }">
            <!-- AI 消息 -->
            <template v-if="!msg.isUser">
              <div class="ai-avatar">
                <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 1;">restaurant</span>
              </div>
              <div class="ai-bubble">
                <div class="message-text" v-if="msg.content">{{ msg.content }}</div>
                <div class="typing-dots" v-else><span></span><span></span><span></span></div>
                <div class="bubble-actions">
                  <button class="action-btn">
                    <span class="material-symbols-outlined">content_copy</span> 复制
                  </button>
                  <button class="action-btn">
                    <span class="material-symbols-outlined">refresh</span> 重新生成
                  </button>
                  <div class="action-icons">
                    <span class="material-symbols-outlined like-btn">thumb_up</span>
                    <span class="material-symbols-outlined dislike-btn">thumb_down</span>
                  </div>
                </div>
              </div>
            </template>

            <!-- 用户消息 -->
            <template v-else>
              <div class="user-bubble">
                <div class="message-text">{{ msg.content }}</div>
              </div>
            </template>
          </div>
        </div>
      </div>
    </section>

    <!-- 底部输入区 -->
    <footer class="input-footer">
      <div class="input-wrapper">
        <div class="input-box">
          <button
            class="mode-switch-btn"
            :class="{ active: useRag }"
            :title="useRag ? '知识库问答模式' : 'AI 对话模式'"
            @click="useRag = !useRag"
          >
            <span class="material-symbols-outlined">{{ useRag ? 'menu_book' : 'restaurant' }}</span>
          </button>
          <textarea
            v-model="inputMessage"
            @keydown.enter.exact.prevent="sendMessage"
            :placeholder="useRag ? '小迪正在翻找知识库...' : '问问小迪关于美食的问题...'"
            rows="1"
            class="input-textarea hide-scrollbar"
          ></textarea>
          <button class="send-btn" @click="sendMessage" :disabled="!inputMessage.trim() || isLoading">
            <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 1;">send</span>
          </button>
        </div>
        <div class="feature-tags">
          <div class="tag">
            <span class="material-symbols-outlined">restaurant_menu</span>
            食谱推荐
          </div>
          <div class="tag">
            <span class="material-symbols-outlined">nutrition</span>
            营养分析
          </div>
          <div class="tag">
            <span class="material-symbols-outlined">local_dining</span>
            烹饪技巧
          </div>
        </div>
      </div>
    </footer>
  </AppLayout>
</template>

<script setup>
import { ref, nextTick, onMounted, computed, watch, inject } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppLayout from '../components/AppLayout.vue'
import { chatWithFood, chatWithFoodRag, getUserProfile } from '../api'
import {
  getCurrentChatId,
  setCurrentChatId,
  createNewConversation,
  getConversation,
  updateConversationMessages
} from '../store/chatStore'

const route = useRoute()
const router = useRouter()

// 注入刷新对话列表的函数
const refreshConversations = inject('refreshConversations')

// 全局变量，用于在切换对话时保存正在流式传输的消息
let streamingMessages = []
let streamingAiMsgIndex = -1

const messages = ref([])
const inputMessage = ref('')
const isLoading = ref(false)
const chatCanvas = ref(null)
const currentChatId = ref(null)
const useRag = ref(false)
let eventSource = null

// 用户画像
const userProfile = ref(null)
const showProfile = ref(false)

const currentChatTitle = computed(() => {
  if (messages.value.length === 0) return '馋嘴小迪'
  const firstUserMsg = messages.value.find(m => m.isUser)
  if (firstUserMsg) {
    return firstUserMsg.content.length > 15
      ? firstUserMsg.content.slice(0, 15) + '...'
      : firstUserMsg.content
  }
  return '馋嘴小迪'
})

const scrollToBottom = () => {
  nextTick(() => {
    if (chatCanvas.value) {
      chatCanvas.value.scrollTop = chatCanvas.value.scrollHeight
    }
  })
}

const addMessage = (content, isUser = false) => {
  messages.value.push({
    id: Date.now(),
    content,
    isUser,
    time: new Date()
  })
  scrollToBottom()
}

const saveCurrentChat = async () => {
  if (currentChatId.value) {
    await updateConversationMessages(currentChatId.value, messages.value)
  }
}

const loadChat = async (chatId) => {
  const chat = await getConversation(chatId)
  if (chat) {
    currentChatId.value = chatId
    messages.value = chat.messages
  } else {
    currentChatId.value = chatId
    messages.value = []
  }
  scrollToBottom()
  // 自动聚焦到输入框
  nextTick(() => {
    const textarea = document.querySelector('.input-textarea')
    if (textarea) textarea.focus()
  })
}

const sendQuickMessage = async (text) => {
  inputMessage.value = text
  await sendMessage()
}

const sendMessage = async () => {
  const content = inputMessage.value.trim()
  if (!content || isLoading.value) return

  // 如果是第一条消息，创建新对话
  if (messages.value.length === 0) {
    if (!currentChatId.value) {
      const newChat = await createNewConversation()
      currentChatId.value = newChat.id
      router.push(`/chat/${newChat.id}`)
    }
    // 创建用户消息和AI消息
    const userMsg = {
      id: Date.now(),
      content: content,
      isUser: true,
      time: new Date()
    }
    const aiMsg = {
      id: Date.now() + 1,
      content: '',
      isUser: false,
      time: new Date()
    }
    // 保存用户消息到对话
    await updateConversationMessages(currentChatId.value, [userMsg])
    messages.value = [userMsg, aiMsg]
    // 刷新对话列表
    if (refreshConversations) await refreshConversations()
  } else {
    addMessage(content, true)
    // 添加 AI 消息占位
    addMessage('', false)
  }

  inputMessage.value = ''
  isLoading.value = true

  const aiMsgIndex = messages.value.length - 1

  // 保存流式消息的引用
  streamingMessages = messages.value
  streamingAiMsgIndex = aiMsgIndex

  eventSource = useRag.value
    ? chatWithFoodRag(content, currentChatId.value)
    : chatWithFood(content, currentChatId.value)

  eventSource.onmessage = async (event) => {
    const data = event.data
    if (data && data !== '[DONE]') {
      if (messages.value[aiMsgIndex]) {
        messages.value[aiMsgIndex].content += data
      }
      scrollToBottom()
    }
    if (data === '[DONE]') {
      isLoading.value = false
      eventSource.close()
      eventSource = null
      await saveCurrentChat()
      if (refreshConversations) await refreshConversations()
    }
  }

  eventSource.onerror = async () => {
    if (messages.value[aiMsgIndex] && messages.value[aiMsgIndex].content === '') {
      messages.value[aiMsgIndex].content = '抱歉，连接出现错误，请重试。'
    }
    isLoading.value = false
    eventSource.close()
    eventSource = null
    await saveCurrentChat()
    if (refreshConversations) await refreshConversations()
  }
}

onMounted(() => {
  loadCurrentChat()
})

// 监听路由参数变化，切换对话
watch(() => route.params.id, async (newId, oldId) => {
  if (newId && newId !== currentChatId.value) {
    // 关闭之前的SSE连接
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
    isLoading.value = false
    streamingMessages = []
    streamingAiMsgIndex = -1

    // 切换到新对话
    currentChatId.value = newId
    messages.value = []
    await loadChat(newId)
  }
})

const loadCurrentChat = async () => {
  let chatId = route.params.id || await getCurrentChatId()
  if (!chatId) {
    const newChat = await createNewConversation()
    chatId = newChat.id
  }
  currentChatId.value = chatId
  await setCurrentChatId(chatId)
  await loadChat(chatId)
}

// 加载用户画像
const loadUserProfile = async () => {
  try {
    const userId = currentChatId.value || 'default'
    const response = await getUserProfile(userId)
    userProfile.value = response.data
  } catch (error) {
    console.error('Failed to load user profile:', error)
  }
}

// 切换画像显示
const toggleProfile = () => {
  showProfile.value = !showProfile.value
  if (showProfile.value && !userProfile.value) {
    loadUserProfile()
  }
}
</script>

<style scoped>
/* 顶部导航 */
.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  width: 100%;
  position: fixed;
  top: 0;
  z-index: 50;
  background: rgba(250, 248, 245, 0.8);
  backdrop-filter: blur(20px);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  font-family: var(--font-headline);
}

.menu-btn {
  padding: 8px;
  color: var(--on-surface);
}

.chat-title {
  margin-left: 16px;
  font-size: 16px;
  font-weight: 600;
  color: var(--on-surface);
}

.md-hidden {
  display: block;
}

@media (min-width: 768px) {
  .md-hidden {
    display: none;
  }
}

/* 对话区域 */
.chat-canvas {
  flex: 1;
  overflow-y: auto;
  padding: 80px 16px 160px;
}

@media (min-width: 768px) {
  .chat-canvas {
    padding: 80px 0 160px;
  }
}

.chat-content {
  max-width: 768px;
  margin: 0 auto;
}

/* 欢迎区域 */
.welcome-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 120px 24px 48px;
  min-height: calc(100vh - 240px);
}

.welcome-icon {
  width: 64px;
  height: 64px;
  border-radius: 24px;
  background: var(--surface-container-lowest);
  box-shadow: 0 4px 12px rgba(232, 90, 79, 0.05);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
}

.welcome-icon .material-symbols-outlined {
  font-size: 36px;
  color: var(--primary);
}

.welcome-section h2 {
  font-family: var(--font-headline);
  font-size: 30px;
  font-weight: 700;
  color: var(--on-surface);
  letter-spacing: -0.02em;
  margin-bottom: 12px;
}

.welcome-section > p {
  color: var(--on-surface-variant);
  max-width: 400px;
  line-height: 1.6;
}

/* 快捷操作 */
.quick-actions {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
  margin-top: 48px;
  width: 100%;
}

@media (min-width: 768px) {
  .quick-actions {
    grid-template-columns: 1fr 1fr;
  }
}

.quick-btn {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px;
  background: var(--surface-container-lowest);
  border-radius: 16px;
  border: 1px solid rgba(171, 173, 175, 0.1);
  text-align: left;
  transition: all 0.3s ease;
}

.quick-btn:hover {
  box-shadow: 0 8px 16px rgba(232, 90, 79, 0.05);
}

.quick-icon {
  padding: 12px;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.quick-icon.primary {
  background: rgba(0, 98, 140, 0.1);
  color: var(--primary);
}

.quick-icon.tertiary {
  background: rgba(136, 60, 147, 0.1);
  color: var(--tertiary);
}

.quick-btn:hover .quick-icon.primary {
  background: var(--primary);
  color: white;
}

.quick-btn:hover .quick-icon.tertiary {
  background: var(--tertiary);
  color: white;
}

.quick-title {
  font-weight: 700;
  font-size: 14px;
  color: var(--on-surface);
}

.quick-desc {
  font-size: 12px;
  color: var(--on-surface-variant);
  margin-top: 4px;
}

/* 消息区域 */
.messages-area {
  padding: 0 16px;
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.message-row.user-row {
  justify-content: flex-end;
}

/* AI 头像 */
.ai-avatar {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  background: var(--primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(232, 90, 79, 0.2);
}

.ai-avatar .material-symbols-outlined {
  color: white;
  font-size: 20px;
}

/* AI 消息气泡 */
.ai-bubble {
  max-width: 85%;
  background: rgba(255, 138, 101, 0.1);
  border: 1px solid rgba(232, 90, 79, 0.2);
  padding: 16px 20px;
  border-radius: 20px;
  border-top-left-radius: 4px;
}

.message-text {
  font-size: 14px;
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-word;
}

/* 气泡操作 */
.bubble-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(232, 90, 79, 0.1);
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--on-surface-variant);
  transition: color 0.2s ease;
}

.action-btn:hover {
  color: var(--primary);
}

.action-btn .material-symbols-outlined {
  font-size: 16px;
}

.action-icons {
  margin-left: auto;
  display: flex;
  gap: 8px;
}

.like-btn, .dislike-btn {
  font-size: 18px;
  color: var(--on-surface-variant);
  cursor: pointer;
  transition: color 0.2s ease;
}

.like-btn:hover {
  color: #16a34a;
}

.dislike-btn:hover {
  color: #dc2626;
}

/* 用户消息气泡 */
.user-bubble {
  max-width: 80%;
  background: var(--surface-container-lowest);
  padding: 14px 20px;
  border-radius: 20px;
  border-top-right-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(171, 173, 175, 0.05);
}

/* 打字动画 */
.typing-dots {
  display: flex;
  gap: 4px;
  padding: 4px 0;
}

.typing-dots span {
  width: 8px;
  height: 8px;
  background: var(--outline);
  border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-dots span:nth-child(2) { animation-delay: 0.2s; }
.typing-dots span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-6px); opacity: 1; }
}

/* 底部输入区 */
.input-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  padding: 24px;
  background: linear-gradient(to top, var(--surface), rgba(250, 248, 245, 0.95), transparent);
}

.input-wrapper {
  max-width: 768px;
  margin: 0 auto;
  position: relative;
}

.input-box {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  background: var(--surface-container-lowest);
  padding: 8px 12px 8px 16px;
  border-radius: 24px;
  box-shadow: 0 8px 24px rgba(44, 47, 49, 0.05);
  border: 1px solid rgba(171, 173, 175, 0.1);
  transition: all 0.2s ease;
}

.input-box:focus-within {
  border-color: var(--primary-fixed);
  box-shadow: 0 8px 24px rgba(44, 47, 49, 0.05), 0 0 0 4px rgba(52, 181, 250, 0.2);
}

.attach-btn {
  padding: 8px;
  color: var(--on-surface-variant);
  margin-bottom: 6px;
}

.mode-switch-btn {
  padding: 6px;
  color: var(--on-surface-variant);
  margin-bottom: 6px;
  transition: all 0.2s ease;
  border-radius: 50%;
  opacity: 0.5;
}

.mode-switch-btn:hover {
  opacity: 0.8;
}

.mode-switch-btn.active {
  color: var(--primary);
  opacity: 1;
}

.mode-switch-btn .material-symbols-outlined {
  font-size: 20px;
}

.input-textarea {
  flex: 1;
  padding: 12px 8px;
  background: transparent;
  border: none;
  font-size: 14px;
  resize: none;
  max-height: 192px;
  color: var(--on-surface);
}

.input-textarea::placeholder {
  color: var(--on-surface-variant);
  opacity: 0.5;
}

.send-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--primary), var(--primary-container));
  color: white;
  border-radius: 50%;
  margin-bottom: 6px;
  box-shadow: 0 4px 12px rgba(232, 90, 79, 0.2);
  transition: all 0.2s ease;
}

.send-btn:hover:not(:disabled) {
  transform: scale(1.05);
}

.send-btn:active:not(:disabled) {
  transform: scale(0.95);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 功能标签 */
.feature-tags {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-top: 12px;
  padding: 0 16px;
}

.tag {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: var(--on-surface-variant);
  opacity: 0.6;
}

.tag .material-symbols-outlined {
  font-size: 14px;
}

/* 顶部右侧按钮 */
.top-bar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.profile-btn {
  padding: 8px;
  color: var(--on-surface-variant);
  border-radius: 50%;
  transition: all 0.2s ease;
}

.profile-btn:hover {
  background: rgba(0, 0, 0, 0.05);
  color: var(--primary);
}

.profile-btn .material-symbols-outlined {
  font-size: 20px;
}

/* 用户画像弹窗 */
.profile-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
}

.profile-content {
  background: var(--surface);
  border-radius: 20px;
  width: 90%;
  max-width: 400px;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
}

.profile-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.profile-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--on-surface);
  margin: 0;
}

.close-btn {
  padding: 4px;
  color: var(--on-surface-variant);
  border-radius: 50%;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background: rgba(0, 0, 0, 0.05);
  color: var(--on-surface);
}

.profile-body {
  padding: 24px;
}

.profile-item {
  margin-bottom: 20px;
}

.profile-item:last-child {
  margin-bottom: 0;
}

.profile-label {
  display: block;
  font-size: 12px;
  color: var(--on-surface-variant);
  margin-bottom: 8px;
  font-weight: 500;
}

.profile-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.profile-tags .tag {
  padding: 6px 12px;
  background: var(--surface-container-lowest);
  border-radius: 16px;
  font-size: 13px;
  color: var(--on-surface);
  opacity: 1;
}

.profile-tags .tag.primary {
  background: rgba(232, 90, 79, 0.1);
  color: var(--primary);
}

.profile-tags .tag.dislike {
  background: rgba(220, 38, 38, 0.1);
  color: #dc2626;
}

.profile-tags .tag.warning {
  background: rgba(234, 179, 8, 0.1);
  color: #ca8a04;
}

.profile-value {
  font-size: 14px;
  color: var(--on-surface);
}

.profile-empty {
  text-align: center;
  padding: 24px 0;
  color: var(--on-surface-variant);
}

.profile-empty p {
  margin: 0 0 8px 0;
}

.profile-empty .hint {
  font-size: 12px;
  opacity: 0.7;
}

.profile-loading {
  text-align: center;
  padding: 24px 0;
  color: var(--on-surface-variant);
}
</style>
