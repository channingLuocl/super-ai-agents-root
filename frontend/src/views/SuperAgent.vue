<template>
  <div class="super-agent-container">
    <!-- 背景装饰元素 - 优化为科技感风格 -->
    <div class="background-effects">
      <div class="floating-icon">🤖</div>
      <div class="floating-icon">⚡</div>
      <div class="floating-icon">💡</div>
      <div class="gradient-blob blob-1"></div>
      <div class="gradient-blob blob-2"></div>
      <div class="noise-texture"></div>
    </div>
    
    <!-- 主容器 -->
    <div class="app-container">
      <!-- 现代化头部 - 采用LoverMaster的布局结构 -->
      <header class="modern-header">
        <div class="header-content">
          <button class="back-btn" @click="goBack">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
              <path d="M15 18L9 12L15 6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
            返回主页
          </button>
          
          <div class="header-center">
            <div class="logo">
              <div class="logo-icon">🤖</div>
              <h1 class="app-title">AI超级智能体</h1>
            </div>
            <div class="app-subtitle">全能AI助手 · 专业解决方案</div>
          </div>


        </div>
        
        <!-- 连接状态进度条 -->
        <div class="progress-bar" :class="{ active: connectionStatus === 'connecting' }"></div>
      </header>

      <!-- 内容区域 -->
      <main class="main-content">
        <!-- 欢迎卡片 -->
        <div v-if="showWelcome" class="welcome-card">
          <div class="welcome-content">
            <div class="welcome-icon">👋👋</div>
            <h2>欢迎使用AI超级智能体</h2>
            <p>我是您的全能AI助手，可以解答各类专业问题，提供精准建议和解决方案</p>
            <div class="quick-questions">
              <button 
                v-for="(question, index) in quickQuestions" 
                :key="index"
                class="quick-question-btn"
                @click="sendQuickQuestion(question)"
              >
                {{ question }}
              </button>
            </div>
          </div>
        </div>

        <!-- 聊天区域 -->
        <div class="chat-container" :class="{ 'with-welcome': showWelcome }">
          <ChatRoom 
            :messages="messages" 
            :connection-status="connectionStatus"
            ai-type="super"
            @send-message="sendMessage"
            class="modern-chat-room"
          />
        </div>
      </main>

      <!-- 浮动操作按钮 -->
      <div class="floating-actions">
        <button class="fab" @click="clearChat" title="清空对话">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M3 6H5H21" stroke="currentColor" stroke-width="2"/>
            <path d="M8 6V4C8 3.46957 8.21071 2.96086 8.58579 2.58579C8.96086 2.21071 9.46957 2 10 2H14C14.5304 2 15.0391 2.21071 15.4142 2.58579C15.7893 2.96086 16 3.46957 16 4V6M19 6V20C19 20.5304 18.7893 21.0391 18.4142 21.4142C18.0391 21.7893 17.5304 22 17 22H7C6.46957 22 5.96086 21.7893 5.58579 21.4142C5.21071 21.0391 5 20.5304 5 20V6H19Z" stroke="currentColor" stroke-width="2"/>
          </svg>
        </button>
        <button class="fab primary" @click="scrollToBottom" title="滚动到底部">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M12 5V19M12 19L19 12M12 19L5 12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useHead } from '@vueuse/head'
import ChatRoom from '../components/ChatRoom.vue'
import { chatWithManus } from '../api'

// 设置页面标题和元数据
useHead({
  title: 'AI超级智能体 - AI超级智能体应用平台',
  meta: [
    {
      name: 'description',
      content: 'AI超级智能体是AI超级智能体应用平台的全能助手，能解答各类专业问题，提供精准建议和解决方案'
    },
    {
      name: 'keywords',
      content: 'AI超级智能体,智能助手,专业问答,AI问答,专业建议,AI智能体'
    }
  ]
})

const router = useRouter()
const messages = ref([])
const chatId = ref('')
const connectionStatus = ref('disconnected')
let eventSource = null

// 生成随机会话ID
const generateChatId = () => {
  return 'super_' + Math.random().toString(36).substring(2, 15) + '_' + Date.now().toString(36)
}

// 快速问题列表
const quickQuestions = ref([
  '帮我写一份产品需求文档模板',
  '如何学习编程？给个学习路线',
  '解释一下人工智能的基本原理',
  '帮我分析这个商业模式的优缺点',
  '如何提高工作效率？',
  '推荐几个好用的时间管理工具',
  '如何准备技术面试？',
  '帮我制定一个健身计划'
])

// 计算属性
const showWelcome = computed(() => messages.value.length <= 1)

const statusText = computed(() => {
  const statusMap = {
    disconnected: '离线',
    connecting: '连接中...',
    connected: '在线',
    error: '连接错误'
  }
  return statusMap[connectionStatus.value] || '未知'
})

// 添加消息到列表
const addMessage = (content, isUser, type = '') => {
  const message = {
    id: Date.now() + Math.random(),
    content,
    isUser,
    type,
    time: new Date().getTime(),
    streaming: !isUser && content === ''
  }
  messages.value.push(message)
  return message
}

// 发送快速问题
const sendQuickQuestion = (question) => {
  sendMessage(question)
}

// 发送消息
const sendMessage = (messageContent) => {
  if (!messageContent.trim()) return

  // 添加用户消息
  addMessage(messageContent, true, 'user-question')
  
  // 添加AI消息占位
  const aiMessage = addMessage('', false)
  
  // 关闭现有连接
  if (eventSource) {
    eventSource.close()
  }

  connectionStatus.value = 'connecting'
  
  try {
    eventSource = chatWithManus(messageContent, chatId.value)
    
    eventSource.onmessage = (event) => {
      const data = event.data
      if (data && data !== '[DONE]') {
        connectionStatus.value = 'connected'
        // 更新AI消息内容
        const currentAiMessage = messages.value.find(msg => msg.id === aiMessage.id)
        if (currentAiMessage) {
          currentAiMessage.content += data
          currentAiMessage.streaming = true
        }
      }
      
      if (data === '[DONE]') {
        connectionStatus.value = 'disconnected'
        const currentAiMessage = messages.value.find(msg => msg.id === aiMessage.id)
        if (currentAiMessage) {
          currentAiMessage.streaming = false
        }
        eventSource.close()
      }
    }
    
    eventSource.onerror = (error) => {
      console.error('SSE Error:', error)
      connectionStatus.value = 'error'
      const currentAiMessage = messages.value.find(msg => msg.id === aiMessage.id)
      if (currentAiMessage) {
        currentAiMessage.content = '抱歉，连接出现错误，请重试。'
        currentAiMessage.streaming = false
      }
      eventSource.close()
    }
    
  } catch (error) {
    console.error('发送消息错误:', error)
    connectionStatus.value = 'error'
  }
}

// 返回主页
const goBack = () => {
  if (eventSource) {
    eventSource.close()
  }
  router.push('/')
}

// 清空对话
const clearChat = () => {
  if (confirm('确定要清空当前对话吗？')) {
    messages.value = []
    chatId.value = generateChatId()
    addMessage('你好，我是AI超级智能体。我可以解答各类问题，提供专业建议，请问有什么可以帮助你的吗？', false)
  }
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    const chatContainer = document.querySelector('.chat-container')
    if (chatContainer) {
      chatContainer.scrollTop = chatContainer.scrollHeight
    }
  })
}

// 页面加载时添加欢迎消息
onMounted(() => {
  chatId.value = generateChatId()
  addMessage('你好，我是AI超级智能体。我可以解答各类问题，提供专业建议，请问有什么可以帮助你的吗？', false)
})

// 组件销毁前关闭SSE连接
onBeforeUnmount(() => {
  if (eventSource) {
    eventSource.close()
  }
})
</script>

<style scoped>
.super-agent-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f0f4ff 0%, #f8faff 50%, #f0f4ff 100%);
  position: relative;
  overflow-x: hidden;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* 背景效果 - 优化为科技感风格 */
.background-effects {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}

.floating-icon {
  position: absolute;
  font-size: 24px;
  opacity: 0.15;
  animation: float 6s ease-in-out infinite;
  filter: drop-shadow(0 2px 4px rgba(99, 102, 241, 0.2));
}

.floating-icon:nth-child(1) {
  top: 10%;
  left: 10%;
  animation-delay: 0s;
}

.floating-icon:nth-child(2) {
  top: 20%;
  right: 15%;
  animation-delay: 2s;
}

.floating-icon:nth-child(3) {
  bottom: 15%;
  left: 20%;
  animation-delay: 4s;
}

.gradient-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(50px);
  opacity: 0.12;
}

.blob-1 {
  width: 300px;
  height: 300px;
  background: linear-gradient(45deg, #c7d2fe, #a5b4fc);
  top: -100px;
  right: -100px;
}

.blob-2 {
  width: 400px;
  height: 400px;
  background: linear-gradient(45deg, #ddd6fe, #c4b5fd);
  bottom: -150px;
  left: -150px;
}

.noise-texture {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.65' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)' opacity='0.02'/%3E%3C/svg%3E");
}

/* 主容器 */
.app-container {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  backdrop-filter: blur(20px);
}

/* 现代化头部 - 采用LoverMaster样式 */
.modern-header {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(224, 231, 255, 0.5);
  box-shadow: 0 8px 32px rgba(224, 231, 255, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 2rem;
  max-width: 1400px;
  margin: 0 auto;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(224, 231, 255, 0.6);
  border-radius: 12px;
  color: #6366f1;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.9);
  border-color: #a5b4fc;
  transform: translateX(-4px);
}

.header-center {
  text-align: center;
  flex: 1;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  margin-bottom: 0.25rem;
}

.logo-icon {
  font-size: 2rem;
  filter: drop-shadow(0 2px 4px rgba(224, 231, 255, 0.3));
}

.app-title {
  font-size: 1.75rem;
  font-weight: 700;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0;
}

.app-subtitle {
  font-size: 0.875rem;
  color: #8b5cf6;
  font-weight: 400;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 20px;
  font-size: 0.8rem;
  color: #6366f1;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  transition: all 0.3s ease;
}

.status-indicator.connected .status-dot {
  background: #a5b4fc;
  box-shadow: 0 0 10px #a5b4fc;
}

.status-indicator.connecting .status-dot {
  background: #818cf8;
  animation: pulse 1.5s infinite;
}

.status-indicator.error .status-dot {
  background: #dc3545;
}

.status-indicator.disconnected .status-dot {
  background: #6b7280;
}

.chat-info {
  background: rgba(255, 255, 255, 0.6);
  padding: 0.5rem 1rem;
  border-radius: 12px;
  font-size: 0.8rem;
  color: #8b5cf6;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-id {
  font-family: 'Monaco', 'Consolas', monospace;
  font-size: 0.75rem;
}

.progress-bar {
  height: 2px;
  background: linear-gradient(90deg, #a5b4fc, #818cf8);
  width: 0%;
  transition: width 0.3s ease;
}

.progress-bar.active {
  width: 100%;
  animation: progress 2s ease-in-out infinite;
}

/* 主内容区域 */
.main-content {
  flex: 1;
  padding: 2rem;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* 欢迎卡片 - 优化样式 */
.welcome-card {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(224, 231, 255, 0.4);
  border-radius: 20px;
  padding: 2rem;
  text-align: center;
  box-shadow: 0 20px 40px rgba(224, 231, 255, 0.08);
}

.welcome-content .welcome-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
  color: #6366f1;
}

.welcome-content h2 {
  color: #4c1d95;
  margin-bottom: 1rem;
  font-size: 1.5rem;
}

.welcome-content p {
  color: #6b7280;
  margin-bottom: 2rem;
  line-height: 1.6;
}

.quick-questions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  justify-content: center;
  max-width: 800px;
  margin: 0 auto;
}

.quick-question-btn {
  padding: 0.75rem 1.5rem;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(224, 231, 255, 0.5);
  border-radius: 12px;
  color: #6366f1;
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
  flex: 1;
  min-width: 200px;
  max-width: calc(50% - 0.75rem);
}

.quick-question-btn:hover {
  background: rgba(255, 255, 255, 0.8);
  border-color: #a5b4fc;
  transform: translateY(-2px);
}

/* 聊天容器 */
.chat-container {
  flex: 1;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(224, 231, 255, 0.3);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(224, 231, 255, 0.08);
}

.chat-container.with-welcome {
  max-height: 600px;
}

.modern-chat-room {
  height: 100%;
}

/* 浮动操作按钮 */
.floating-actions {
  position: fixed;
  bottom: 2rem;
  right: 2rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  z-index: 90;
}

.fab {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(20px);
  color: #6366f1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  box-shadow: 0 8px 32px rgba(224, 231, 255, 0.15);
  border: 1px solid rgba(224, 231, 255, 0.4);
}

.fab:hover {
  background: rgba(255, 255, 255, 0.9);
  border-color: #a5b4fc;
  transform: translateY(-2px) scale(1.05);
}

.fab.primary {
  background: linear-gradient(135deg, #a5b4fc, #818cf8);
  color: white;
  border: none;
}

.fab.primary:hover {
  background: linear-gradient(135deg, #818cf8, #a5b4fc);
  border: none;
}

/* 动画 */
@keyframes float {
  0%, 100% { transform: translateY(0px) rotate(0deg); }
  50% { transform: translateY(-20px) rotate(5deg); }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

@keyframes progress {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(400%); }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-content {
    padding: 1rem;
    flex-wrap: wrap;
    gap: 1rem;
  }
  
  .header-center {
    order: -1;
    width: 100%;
  }
  
  .main-content {
    padding: 1rem;
  }
  
  .welcome-card {
    padding: 1.5rem;
  }
  
  .quick-questions {
    flex-direction: column;
  }
  
  .quick-question-btn {
    min-width: 100%;
    max-width: 100%;
  }
  
  .floating-actions {
    bottom: 1rem;
    right: 1rem;
  }
  
  .chat-info {
    max-width: 120px;
  }
}

@media (max-width: 480px) {
  .app-title {
    font-size: 1.5rem;
  }
  
  .logo-icon {
    font-size: 1.5rem;
  }
  
  .welcome-content .welcome-icon {
    font-size: 3rem;
  }
  
  .fab {
    width: 48px;
    height: 48px;
  }
  
  .chat-info {
    display: none;
  }
}
</style>