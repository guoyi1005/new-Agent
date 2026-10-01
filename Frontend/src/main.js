import './assets/theme-tokens.css'
import './assets/main.css'
import './assets/home-theme.css'
import './assets/interview-home-theme.css'
import './utils/uniShim.js'

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

createApp(App).use(router).mount('#app')
