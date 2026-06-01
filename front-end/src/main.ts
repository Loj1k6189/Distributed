import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'  
import httpPlugin from './lib/httpPlugin'

const app = createApp(App)

app.use(router) 
app.use(httpPlugin)

app.mount('#app')