<template>
  <AppLayout>
    <!-- 顶部导航 -->
    <header class="top-bar">
      <div class="top-bar-left">
        <button class="menu-btn md-hidden">
          <span class="material-symbols-outlined">menu</span>
        </button>
      </div>
    </header>

    <!-- 主内容区 -->
    <section class="config-canvas hide-scrollbar">
      <div class="config-content">
        <!-- 页面标题 -->
        <header class="page-header">
          <h1>配置与统计中心</h1>
          <p>管理您的模型连接参数并实时监控资源消耗趋势</p>
        </header>

        <!-- 统计卡片 -->
        <div class="metric-cards">
          <div class="metric-card">
            <div class="metric-header">
              <div class="metric-icon primary">
                <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 1;">generating_tokens</span>
              </div>
              <span class="metric-label">今日 Token 消耗</span>
            </div>
            <div class="metric-value">42,850</div>
            <div class="metric-footer">
              预计成本: <span class="metric-cost">¥1.28</span>
            </div>
          </div>

          <div class="metric-card">
            <div class="metric-header">
              <div class="metric-icon secondary">
                <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 1;">analytics</span>
              </div>
              <span class="metric-label">本月 Token 消耗</span>
            </div>
            <div class="metric-value">856,204</div>
            <div class="metric-footer">
              较上月同期增长 <span class="metric-growth">15.4%</span>
            </div>
          </div>
        </div>

        <!-- 连接设置 -->
        <section class="config-section">
          <h2 class="section-title">
            <span class="material-symbols-outlined">link</span>
            连接设置
          </h2>
          <div class="config-form">
            <div class="form-row">
              <div class="form-group">
                <label>模型 Base URL</label>
                <input type="text" v-model="config.baseUrl" placeholder="请输入 API 基础地址" />
              </div>
              <div class="form-group">
                <label>API Token</label>
                <input type="password" v-model="config.apiToken" placeholder="请输入您的密钥" />
              </div>
            </div>
            <div class="form-actions">
              <button class="save-btn">保存配置</button>
            </div>
          </div>
        </section>

        <!-- 图表区域 -->
        <div class="charts-grid">
          <!-- 每日趋势 -->
          <section class="chart-section">
            <h2 class="section-title">
              <span class="material-symbols-outlined">show_chart</span>
              每日 Token 消耗趋势 (近 14 天)
            </h2>
            <div class="chart-bars daily">
              <div v-for="(value, index) in dailyData" :key="index" class="bar" :class="{ active: index === dailyData.length - 1 }" :style="{ height: value + '%' }">
                <span v-if="index === dailyData.length - 1" class="bar-label">今日</span>
              </div>
            </div>
            <div class="chart-labels">
              <span>14天前</span>
              <span>7天前</span>
              <span>今天</span>
            </div>
          </section>

          <!-- 每月趋势 -->
          <section class="chart-section">
            <h2 class="section-title">
              <span class="material-symbols-outlined">bar_chart</span>
              每月 Token 消耗趋势 (近 6 个月)
            </h2>
            <div class="chart-bars monthly">
              <div v-for="(value, index) in monthlyData" :key="index" class="bar" :class="{ active: index === monthlyData.length - 1 }" :style="{ height: value + '%' }">
                <span v-if="index === monthlyData.length - 1" class="bar-label">本月</span>
              </div>
            </div>
            <div class="chart-labels">
              <span>12月</span>
              <span>1月</span>
              <span>2月</span>
              <span>3月</span>
              <span>4月</span>
              <span>5月</span>
            </div>
          </section>
        </div>
      </div>
    </section>
  </AppLayout>
</template>

<script setup>
import { ref } from 'vue'
import AppLayout from '../components/AppLayout.vue'

const config = ref({
  baseUrl: 'https://api.openai.com/v1',
  apiToken: 'sk-••••••••••••••••••••••••'
})

const dailyData = [30, 45, 25, 60, 55, 80, 70, 40, 50, 35, 65, 90, 75, 85]
const monthlyData = [40, 60, 55, 85, 70, 95]
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
  background: rgba(245, 247, 249, 0.8);
  backdrop-filter: blur(20px);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  font-family: var(--font-headline);
}

.top-bar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.menu-btn {
  padding: 8px;
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

.status-badge {
  display: none;
  align-items: center;
  gap: 8px;
  padding: 4px 12px;
  background: var(--surface-container-low);
  border-radius: 100px;
}

@media (min-width: 768px) {
  .status-badge {
    display: flex;
  }
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--primary-container);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.status-badge span:last-child {
  font-size: 12px;
  font-weight: 600;
  color: var(--on-surface-variant);
  font-family: var(--font-body);
}

/* 主内容区 */
.config-canvas {
  flex: 1;
  overflow-y: auto;
  padding: 96px 24px 48px;
}

@media (min-width: 768px) {
  .config-canvas {
    padding: 96px 48px 48px;
  }
}

.config-content {
  max-width: 1280px;
  margin: 0 auto;
}

/* 页面标题 */
.page-header {
  margin-bottom: 40px;
}

.page-header h1 {
  font-family: var(--font-headline);
  font-size: 36px;
  font-weight: 700;
  color: var(--on-surface);
  letter-spacing: -0.02em;
  margin-bottom: 8px;
}

.page-header p {
  font-family: var(--font-body);
  color: var(--on-surface-variant);
}

/* 统计卡片 */
.metric-cards {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
  margin-bottom: 32px;
}

@media (min-width: 768px) {
  .metric-cards {
    grid-template-columns: 1fr 1fr;
  }
}

.metric-card {
  background: var(--surface-container-lowest);
  padding: 32px;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(171, 173, 175, 0.1);
}

.metric-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.metric-icon {
  padding: 8px;
  border-radius: 8px;
}

.metric-icon.primary {
  background: rgba(0, 98, 140, 0.1);
  color: var(--primary);
}

.metric-icon.secondary {
  background: rgba(77, 93, 115, 0.1);
  color: var(--secondary);
}

.metric-label {
  font-size: 14px;
  font-weight: 700;
  color: var(--on-surface-variant);
}

.metric-value {
  font-family: var(--font-headline);
  font-size: 36px;
  font-weight: 700;
  color: var(--on-surface);
}

.metric-footer {
  font-size: 12px;
  color: var(--on-surface-variant);
  opacity: 0.7;
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.metric-cost {
  color: var(--on-surface);
  font-weight: 700;
}

.metric-growth {
  color: #16a34a;
  font-weight: 700;
}

/* 配置区域 */
.config-section {
  background: var(--surface-container-lowest);
  padding: 32px;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(171, 173, 175, 0.1);
  margin-bottom: 32px;
}

.section-title {
  font-family: var(--font-headline);
  font-size: 20px;
  font-weight: 700;
  color: var(--on-surface);
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 32px;
}

.section-title .material-symbols-outlined {
  color: var(--primary);
}

.config-form {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr;
  gap: 32px;
}

@media (min-width: 768px) {
  .form-row {
    grid-template-columns: 1fr 1fr;
  }
}

.form-group label {
  display: block;
  font-size: 12px;
  font-weight: 700;
  color: var(--on-surface-variant);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 8px;
}

.form-group input {
  width: 100%;
  background: var(--surface-container-low);
  border: none;
  border-radius: 12px;
  padding: 16px;
  font-size: 14px;
  color: var(--on-surface);
  transition: all 0.2s ease;
}

.form-group input::placeholder {
  color: var(--on-surface-variant);
  opacity: 0.4;
}

.form-group input:focus {
  box-shadow: 0 0 0 4px rgba(0, 98, 140, 0.2);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
}

.save-btn {
  padding: 12px 40px;
  background: linear-gradient(135deg, var(--primary), var(--primary-container));
  color: white;
  border-radius: 12px;
  font-weight: 700;
  font-size: 14px;
  box-shadow: 0 4px 12px rgba(0, 98, 140, 0.2);
  transition: all 0.2s ease;
}

.save-btn:hover {
  transform: scale(1.02);
}

.save-btn:active {
  transform: scale(0.95);
}

/* 图表区域 */
.charts-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 32px;
}

@media (min-width: 1024px) {
  .charts-grid {
    grid-template-columns: 1fr 1fr;
  }
}

.chart-section {
  background: var(--surface-container-lowest);
  padding: 32px;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(171, 173, 175, 0.1);
}

.chart-section .section-title {
  font-size: 18px;
  margin-bottom: 32px;
}

.chart-bars {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  height: 192px;
  padding: 0 8px;
}

.chart-bars.daily {
  gap: 8px;
}

.chart-bars.monthly {
  gap: 24px;
  padding: 0 16px;
}

.bar {
  flex: 1;
  background: rgba(0, 98, 140, 0.1);
  border-radius: 4px 4px 0 0;
  transition: background 0.2s ease;
  position: relative;
}

.bar:hover {
  background: rgba(0, 98, 140, 0.2);
}

.bar.active {
  background: var(--primary);
  box-shadow: 0 4px 12px rgba(0, 98, 140, 0.2);
}

.bar-label {
  position: absolute;
  top: -24px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 10px;
  font-weight: 700;
  color: var(--primary);
}

.chart-labels {
  display: flex;
  justify-content: space-between;
  margin-top: 24px;
  font-size: 10px;
  font-weight: 700;
  color: var(--on-surface-variant);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 0 4px;
}
</style>