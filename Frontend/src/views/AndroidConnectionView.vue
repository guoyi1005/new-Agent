<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { clearAuth } from '../utils/auth'
import { getStoredApiOrigin, normalizeApiOrigin, saveApiOrigin } from '../config/androidApi'

defineProps({ firstRun: { type: Boolean, default: false } })

const router = useRouter()
const address = ref(getStoredApiOrigin())
const checking = ref(false)
const status = ref('')
const error = ref('')

async function checkConnection() {
  error.value = ''
  status.value = ''
  let origin
  try {
    origin = normalizeApiOrigin(address.value)
  } catch (cause) {
    error.value = cause.message
    return
  }
  checking.value = true
  try {
    const controller = new AbortController()
    const timer = window.setTimeout(() => controller.abort(), 8000)
    try {
      // 任何 HTTP 响应都说明地址可达；登录页面会再验证接口和账号。
      await fetch(`${origin}/api`, { signal: controller.signal, cache: 'no-store' })
      status.value = '服务器地址可达，可以保存。'
    } finally {
      window.clearTimeout(timer)
    }
  } catch {
    error.value = '暂时无法连接。请确认手机和服务器网络互通、后端已启动，并允许跨域访问。'
  } finally {
    checking.value = false
  }
}

function save() {
  error.value = ''
  try {
    saveApiOrigin(address.value)
    clearAuth()
    window.location.replace('/login')
  } catch (cause) {
    error.value = cause.message
  }
}
</script>

<template>
  <main class="android-connection">
    <div class="android-connection__content">
      <p class="android-connection__brand">知航 · 安卓版</p>
      <h1>{{ firstRun ? '连接你的学习空间' : '服务器连接' }}</h1>
      <p class="android-connection__intro">App 沿用现有账号和数据。先填写可以从这部手机访问的后端地址。</p>
      <form @submit.prevent="save">
        <label for="android-api-origin">后端地址</label>
        <input id="android-api-origin" v-model.trim="address" type="url" inputmode="url" autocomplete="url" placeholder="https://example.com" required />
        <p class="android-connection__hint">本机调试可填电脑的局域网地址，例如 http://192.168.1.5:8080；安卓模拟器可用 http://10.0.2.2:8080。不要填 localhost。</p>
        <p v-if="error" class="android-connection__error" role="alert">{{ error }}</p>
        <p v-if="status" class="android-connection__success" role="status">{{ status }}</p>
        <div class="android-connection__actions">
          <button type="button" class="android-connection__test" :disabled="checking" @click="checkConnection">{{ checking ? '正在检查…' : '测试连接' }}</button>
          <button type="submit" class="android-connection__save">保存并登录</button>
        </div>
      </form>
      <button v-if="!firstRun" type="button" class="android-connection__back" @click="router.back()">返回设置</button>
      <p class="android-connection__notice">局域网 HTTP 仅供开发调试。正式部署请配置 HTTPS 地址，避免登录信息在网络中明文传输。</p>
    </div>
  </main>
</template>

<style scoped>
.android-connection{display:grid;min-height:100dvh;place-items:center;padding:calc(32px + env(safe-area-inset-top)) 22px calc(32px + env(safe-area-inset-bottom));background:#f8f2e8;color:#302821;font-family:Inter,'PingFang SC','Microsoft YaHei',sans-serif}
.android-connection__content{width:min(100%,460px);padding:32px 28px;border:1px solid #e1d5c5;border-radius:28px;background:#fffaf3;box-shadow:0 18px 44px rgba(82,60,41,.09)}
.android-connection__brand{margin:0 0 28px;color:#9a704e;font-size:13px;font-weight:800;letter-spacing:.12em}
.android-connection h1{margin:0 0 10px;font-size:clamp(29px,8vw,38px);line-height:1.2}
.android-connection__intro{margin:0 0 30px;color:#66584a;line-height:1.65}
.android-connection label{display:block;margin-bottom:9px;font-size:14px;font-weight:750}
.android-connection input{box-sizing:border-box;width:100%;min-height:50px;padding:0 15px;border:1px solid #bda991;border-radius:13px;background:#fff;color:#302821;font:inherit}
.android-connection input:focus-visible,.android-connection button:focus-visible{outline:2px solid #9a704e;outline-offset:3px}
.android-connection__hint,.android-connection__notice{color:#6d6054;font-size:12px;line-height:1.6}
.android-connection__hint{margin:11px 0 24px}
.android-connection__notice{margin:30px 0 0;padding-top:18px;border-top:1px solid #e4d9ca}
.android-connection__error,.android-connection__success{margin:0 0 14px;font-size:13px;line-height:1.6}
.android-connection__error{color:#9b3939}.android-connection__success{color:#386b43}
.android-connection__actions{display:grid;grid-template-columns:1fr 1.15fr;gap:10px}
.android-connection button{min-height:46px;border-radius:12px;font:inherit;font-weight:700;cursor:pointer}
.android-connection__test{border:1px solid #bda991;background:#fffaf3;color:#503c2f}
.android-connection__save{border:1px solid #5f4838;background:#5f4838;color:#fffaf3}
.android-connection__back{margin-top:18px;padding:0;border:0;background:transparent;color:#6f5542}
@media(max-width:380px){.android-connection__content{padding:26px 20px}.android-connection__actions{grid-template-columns:1fr}}
</style>
