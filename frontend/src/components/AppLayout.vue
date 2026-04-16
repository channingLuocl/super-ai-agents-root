<template>
  <div class="app-shell">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <!-- Logo -->
      <div class="sidebar-header">
        <div class="logo-icon">
          <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 1;">restaurant</span>
        </div>
        <div class="logo-text">
          <h1>馋嘴小迪</h1>
          <p>美食小助手</p>
        </div>
      </div>

      <!-- New Chat 按钮 -->
      <button class="new-chat-btn" @click="createChat">
        <span class="material-symbols-outlined">add_circle</span>
        <span>New Chat</span>
      </button>

      <!-- 对话列表 -->
      <div class="chat-list">
        <div
          v-for="chat in conversationsRef"
          :key="chat.id"
          class="chat-item"
          :class="{ active: chat.id === currentChatId }"
          @click="selectChat(chat.id)"
        >
          <span class="chat-item-icon">
            <span class="material-symbols-outlined">chat_bubble</span>
          </span>
          <span class="chat-item-title">{{ chat.title }}</span>
          <button class="chat-item-delete" @click.stop="deleteChat(chat.id)">
            <span class="material-symbols-outlined">delete</span>
          </button>
        </div>
      </div>

      <!-- 导航 -->
      <nav class="sidebar-nav">
        <button
          v-for="item in menuItems"
          :key="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
          @click="navigateTo(item.path)"
        >
          <span class="material-symbols-outlined">{{ item.icon }}</span>
          <span>{{ item.name }}</span>
          <span class="material-symbols-outlined arrow">chevron_right</span>
        </button>
      </nav>
    </aside>

    <!-- 主内容区 -->
    <main class="main-area">
      <slot></slot>
    </main>
  </div>
</template>

<script setup>
import { computed, inject } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  createNewConversation,
  deleteConversation,
  getVisibleConversations,
  setCurrentChatId
} from '../store/chatStore'

const router = useRouter()
const route = useRoute()

// 从App.vue注入响应式对话列表
const conversationsRef = inject('conversations')
const refreshConversations = inject('refreshConversations')

// 当前对话ID直接来自路由参数
const currentChatId = computed(() => route.params.id || null)

const menuItems = [
  {
    name: '配置与统计中心',
    path: '/statistics',
    icon: 'settings'
  }
]

const isActive = (path) => {
  return route.path === path
}

const navigateTo = (path) => {
  router.push(path)
}

const createChat = async () => {
  const newChat = await createNewConversation()
  router.push(`/chat/${newChat.id}`)
}

const selectChat = async (chatId) => {
  await setCurrentChatId(chatId)
  router.push(`/chat/${chatId}`)
}

const deleteChat = async (chatId) => {
  await deleteConversation(chatId)
  const wasCurrentChat = currentChatId.value === chatId
  const conversations = await getVisibleConversations()
  if (conversationsRef) {
    conversationsRef.value = conversations
  }
  if (refreshConversations) await refreshConversations()

  if (wasCurrentChat) {
    const newChat = await createNewConversation()
    router.push(`/chat/${newChat.id}`)
  }
}
</script>

<style scoped>
.app-shell {
  display: flex;
  height: 100vh;
  width: 100%;
  overflow: hidden;
}

/* 侧边栏 */
.sidebar {
  display: none;
  flex-direction: column;
  padding: 16px;
  gap: 8px;
  height: 100%;
  width: 256px;
  background: var(--surface-container-low);
  font-family: var(--font-headline);
  font-weight: 500;
}

@media (min-width: 768px) {
  .sidebar {
    display: flex;
  }
}

/* Logo区域 */
.sidebar-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 8px;
  margin-bottom: 24px;
}

.logo-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--primary), var(--primary-container));
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.logo-icon .material-symbols-outlined {
  color: white;
  font-size: 24px;
}

.logo-text h1 {
  font-size: 18px;
  font-weight: 800;
  color: var(--on-surface);
  line-height: 1.2;
}

.logo-text p {
  font-size: 10px;
  color: var(--on-surface-variant);
  opacity: 0.7;
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

/* New Chat 按钮 */
.new-chat-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 12px;
  margin-bottom: 16px;
  background: var(--surface-container-lowest);
  color: var(--primary);
  font-weight: 700;
  font-size: 14px;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.new-chat-btn:hover {
  transform: translateX(4px);
}

.new-chat-btn:active {
  transform: scale(0.95);
}

/* 对话列表 */
.chat-list {
  flex: 1;
  overflow-y: auto;
  margin-bottom: 16px;
}

.chat-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 4px;
}

.chat-item:hover {
  background: rgba(255, 255, 255, 0.5);
}

.chat-item.active {
  background: rgba(232, 90, 79, 0.1);
}

.chat-item-icon {
  color: var(--on-surface-variant);
  font-size: 18px;
}

.chat-item-title {
  flex: 1;
  font-size: 13px;
  font-weight: 500;
  color: var(--on-surface);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-item-delete {
  opacity: 0;
  padding: 4px;
  color: var(--on-surface-variant);
  transition: all 0.2s ease;
}

.chat-item:hover .chat-item-delete {
  opacity: 1;
}

.chat-item-delete:hover {
  color: var(--error);
}

/* 导航 */
.sidebar-nav {
  padding-top: 16px;
  border-top: 1px solid rgba(89, 92, 94, 0.1);
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 8px 16px;
  color: var(--on-surface-variant);
  font-size: 14px;
  font-weight: 700;
  border-radius: 12px;
  transition: all 0.2s ease;
}

.nav-item:hover {
  color: var(--primary);
  background: rgba(255, 255, 255, 0.5);
}

.nav-item.active {
  background: rgba(232, 90, 79, 0.05);
  color: var(--primary);
}

.nav-item .arrow {
  margin-left: auto;
  opacity: 0;
  font-size: 16px;
  transition: opacity 0.2s ease;
}

.nav-item:hover .arrow {
  opacity: 1;
}

/* 主内容区 */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  background: var(--surface);
  overflow: hidden;
}
</style>
