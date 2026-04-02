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
      <button class="new-chat-btn" @click="goHome">
        <span class="material-symbols-outlined">add_circle</span>
        <span>New Chat</span>
      </button>

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
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

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

const goHome = () => {
  router.push('/')
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

/* 导航 */
.sidebar-nav {
  margin-top: auto;
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