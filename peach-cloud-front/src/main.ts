import { createApp } from 'vue'
import Antd from 'ant-design-vue'
import App from './App.vue'
import router from './router'
import pinia from './stores'
import 'ant-design-vue/dist/reset.css'
import './style.css'

createApp(App).use(Antd).use(pinia).use(router).mount('#app')
