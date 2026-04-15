<script setup>
import { onMounted, ref, provide } from 'vue'
import { getVisibleConversations } from './store/chatStore'

// 在顶层提供响应式对话列表
const conversationsRef = ref([])

// 提供更新函数
const refreshConversations = async () => {
  conversationsRef.value = await getVisibleConversations()
}

provide('conversations', conversationsRef)
provide('refreshConversations', refreshConversations)

onMounted(() => {
  refreshConversations()
})
</script>

<template>
  <router-view />
</template>

<style>
/* 全局样式已在 style.css 中定义 */
</style>
