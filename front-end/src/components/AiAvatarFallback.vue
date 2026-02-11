<template>
  <div class="ai-avatar-fallback" :class="[type, { hoverable: isHoverable }]">
    <span v-if="type === 'love'">❤️</span>
    <span v-else-if="type === 'tech'">⚙️</span>
    <span v-else>🤖</span>
  </div>
</template>

<script setup>
defineProps({
  type: {
    type: String,
    default: 'default'
  },
  isHoverable: {
    type: Boolean,
    default: true
  }
})
</script>

<style scoped>
.ai-avatar-fallback {
  --primary-hue: 220; /* 主色调基色（蓝色系） */
  --love-hue: 340;    /* Love类型基色（粉色系） */
  --tech-hue: 280;    /* Tech类型基色（紫色系） */

  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  border-radius: 50%;
  
  /* 关键升级1：玻璃拟态背景与边框 */
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  
  /* 关键升级2：新拟物柔和阴影 */
  box-shadow: 
    inset 1px 1px 2px rgba(255, 255, 255, 0.8),
    inset -1px -1px 2px rgba(0, 0, 0, 0.05),
    4px 4px 12px rgba(0, 0, 0, 0.08);
  
  transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  position: relative;
  overflow: hidden;
}

/* 关键升级3：细腻的光泽动画（替代原较生硬的闪光） */
.ai-avatar-fallback::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 60%;
  height: 100%;
  background: linear-gradient(90deg, 
            transparent, 
            rgba(255, 255, 255, 0.4), 
            transparent);
  transform: skewX(-15deg);
  transition: left 0.8s ease;
}

.ai-avatar-fallback:hover::before {
  left: 140%;
}

/* 类型特定的渐变叠加（淡雅色调） */
.ai-avatar-fallback::after {
  content: '';
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%;
  border-radius: 50%;
  opacity: 0.7;
  background: radial-gradient(circle at 30% 30%, 
            hsl(var(--primary-hue), 60%, 85%) 0%, 
            hsl(var(--primary-hue), 40%, 70%) 100%);
}

.ai-avatar-fallback.love::after {
  background: radial-gradient(circle at 30% 30%, 
            hsl(var(--love-hue), 70%, 85%) 0%, 
            hsl(var(--love-hue), 50%, 75%) 100%);
}

.ai-avatar-fallback.tech::after {
  background: radial-gradient(circle at 30% 30%, 
            hsl(var(--tech-hue), 50%, 85%) 0%, 
            hsl(var(--tech-hue), 30%, 75%) 100%);
}

/* 交互反馈 */
.ai-avatar-fallback.hoverable:hover {
  transform: scale(1.08);
  box-shadow: 
    inset 1px 1px 3px rgba(255, 255, 255, 0.9),
    inset -1px -1px 3px rgba(0, 0, 0, 0.07),
    6px 6px 18px rgba(0, 0, 0, 0.12);
}

/* 确保表情符号在最上层 */
.ai-avatar-fallback > span {
  z-index: 1;
  filter: drop-shadow(0 1px 1px rgba(0,0,0,0.1));
}
</style>