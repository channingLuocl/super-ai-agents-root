<template>
  <div class="home-container">
    <!-- 顶部横幅 -->
    <div class="banner-section">
      <div class="banner-content">
        <div class="banner-text">
          <h1 class="banner-title">AI超级智能体</h1>
          <p class="banner-subtitle">探索AI的无限可能，让智能体为你服务</p>
        </div>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="main-content">
      <div class="agents-grid">
        <div 
          v-for="(agent, index) in agents" 
          :key="index" 
          class="agent-card"
          @click="navigateTo(agent.path)"
        >
          <div class="card-icon">{{ agent.icon }}</div>
          <div class="card-content">
            <h3 class="card-title">{{ agent.title }}</h3>
            <p class="card-desc">{{ agent.description }}</p>
            <div class="card-footer">
              <span class="card-tag">{{ agent.tag }}</span>
              <span class="card-users">{{ formatUsers(agent.users) }}人使用</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useHead } from '@vueuse/head'

// 设置页面标题和元数据
useHead({
  title: 'AI超级智能体应用平台 - 首页',
  meta: [
    {
      name: 'description',
      content: 'AI超级智能体应用平台提供AI恋爱大师和AI超级智能体服务，满足您的各种AI对话需求'
    },
    {
      name: 'keywords',
      content: 'AI智能体,AI应用,AI恋爱大师,AI助手,智能对话,AI超级智能体,首页'
    }
  ]
})

const router = useRouter()

const agents = [
  {
    icon: '❤️',
    title: 'AI恋爱大师',
    description: '智能情感顾问，帮你解答恋爱烦恼，提供专业的情感建议',
    tag: '工作流',
    users: 127000,
    path: '/love-master'
  },
  {
    icon: '🤖',
    title: 'AI超级智能体',
    description: '全能型AI助手，解决各类专业问题，提供精准建议和解决方案',
    tag: '工作流',
    users: 414000,
    path: '/super-agent'
  }
]

const navigateTo = (path) => {
  router.push(path)
}

const formatUsers = (num) => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + '万'
  }
  return num.toString()
}
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 40px;
}

/* 顶部横幅 */
.banner-section {
  background: linear-gradient(135deg, #e0f2fe 0%, #bae6fd 50%, #93c5fd 100%);
  position: relative;
  overflow: hidden;
  padding: 60px 0 80px;
}

.banner-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    radial-gradient(circle at 20% 30%, rgba(59, 130, 246, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 80% 70%, rgba(147, 197, 253, 0.1) 0%, transparent 50%);
  pointer-events: none;
}

.banner-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 32px;
  text-align: center;
  position: relative;
  z-index: 1;
}

.banner-text {
  max-width: 800px;
  margin: 0 auto;
}

.banner-title {
  font-size: 3.5rem;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 20px;
  line-height: 1.2;
  letter-spacing: -1px;
}

.banner-subtitle {
  font-size: 1.25rem;
  color: #475569;
  line-height: 1.6;
}


/* 主内容区 */
.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 60px 32px;
}

.agents-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 24px;
  max-width: 800px;
  margin: 0 auto;
}

.agent-card {
  background: white;
  border-radius: 20px;
  padding: 32px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: relative;
  overflow: hidden;
  border: 1px solid #f1f5f9;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.agent-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  border-color: #e2e8f0;
}


.card-icon {
  font-size: 4.5rem;
  margin-bottom: 24px;
  width: 100px;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-radius: 20px;
  transition: transform 0.3s ease;
}

.agent-card:hover .card-icon {
  transform: scale(1.1) rotate(5deg);
}

.card-content {
  flex: 1;
}

.card-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 12px;
  line-height: 1.4;
}

.card-desc {
  font-size: 1rem;
  color: #64748b;
  line-height: 1.6;
  margin-bottom: 24px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;
}

.card-tag {
  font-size: 0.75rem;
  color: #3b82f6;
  background: #eff6ff;
  padding: 4px 10px;
  border-radius: 6px;
  font-weight: 500;
}

.card-users {
  font-size: 0.75rem;
  color: #94a3b8;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .banner-content {
    padding: 0 20px;
  }
  
  .banner-title {
    font-size: 2.5rem;
  }
  
  .banner-subtitle {
    font-size: 1.125rem;
  }
  
  .main-content {
    padding: 40px 20px;
  }
  
  .agents-grid {
    grid-template-columns: 1fr;
    max-width: 100%;
  }
  
  .agent-card {
    padding: 24px;
  }
  
  .card-icon {
    width: 80px;
    height: 80px;
    font-size: 3.5rem;
  }
  
  .card-title {
    font-size: 1.25rem;
  }
}

@media (max-width: 480px) {
  .banner-section {
    padding: 40px 0 60px;
  }
  
  .banner-title {
    font-size: 2rem;
  }
  
  .banner-subtitle {
    font-size: 1rem;
  }
  
  .main-content {
    padding: 30px 16px;
  }
}
</style>

