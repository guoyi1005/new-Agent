import './assets/theme-tokens.css'
import './assets/main.css'
import './assets/home-theme.css'
import './assets/interview-home-theme.css'
import './assets/android-app.css'
import './utils/uniShim.js'

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { isAndroidApp } from './config/androidApi'

if (isAndroidApp) document.body.classList.add('zh-android-app')

createApp(App).use(router).mount('#app')
