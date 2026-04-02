<script setup>
import { ref, provide } from 'vue'
import { getConversations } from './store/chatStore'

// 在顶层提供响应式对话列表
const conversationsRef = ref(getConversations().filter(c => c.messages.length > 0))

// 提供更新函数
const refreshConversations = () => {
  conversationsRef.value = getConversations().filter(c => c.messages.length > 0)
}

provide('conversations', conversationsRef)
provide('refreshConversations', refreshConversations)
</script>

<template>
  <router-view />
</template>

<style>
/* 全局样式已在 style.css 中定义 */
</style>
