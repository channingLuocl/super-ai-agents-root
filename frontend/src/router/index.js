import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: {
      title: '馋嘴小迪 - 美食助手'
    }
  },
  {
    path: '/chat/:id',
    name: 'Chat',
    component: () => import('../views/Home.vue'),
    meta: {
      title: '馋嘴小迪 - 美食助手'
    }
  },
  {
    path: '/statistics',
    name: 'Statistics',
    component: () => import('../views/Statistics.vue'),
    meta: {
      title: '配置与统计中心 - 馋嘴小迪'
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.meta.title) {
    document.title = to.meta.title
  }
  next()
})

export default router
