import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createHead } from '@vueuse/head'
import './style.css'
import '@fontsource/material-symbols-outlined'

const app = createApp(App)
const head = createHead()

app.use(router)
app.use(head)
app.mount('#app')
