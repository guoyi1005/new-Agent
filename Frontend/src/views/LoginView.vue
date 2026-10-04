<script setup>

import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { login, register } from '../api/user'
import { setToken, setUserInfo } from '../utils/auth'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const showPassword = ref(false)
const mode = ref('login')
const form = reactive({
  username: '',
  password: '',
})
const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
})

function togglePassword() {
  showPassword.value = !showPassword.value
}

function switchMode(target) {
  mode.value = target
  errorMessage.value = ''
  successMessage.value = ''
  showPassword.value = false
}

async function handleLogin() {
  errorMessage.value = ''

  if (!form.username.trim()) {
    errorMessage.value = '请输入用户名'
    return
  }

  if (!form.password || form.password.length < 6) {
    errorMessage.value = '密码长度至少6位'
    return
  }

  loading.value = true
  try {
    const result = await login({
      username: form.username.trim(),
      password: form.password,
    })
    const user = result.data || {}

    setToken(user.token)
    setUserInfo({
      id: user.id,
      userId: user.id,
      username: user.username,
      role: user.role,
      phone: user.phone,
      realName: user.realName,
      college: user.college,
      major: user.major,
      className: user.className,
      personalNumber: user.personalNumber,
      studentId: user.personalNumber,
      avatar: user.avatar,
    })

    router.replace(String(route.query.redirect || '/home'))
  } catch (error) {
    errorMessage.value = error.message || '登录失败'
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  errorMessage.value = ''
  successMessage.value = ''

  if (registerForm.username.trim().length < 3) {
    errorMessage.value = '用户名长度至少3位'
    return
  }

  if (!registerForm.password || registerForm.password.length < 6) {
    errorMessage.value = '密码长度至少6位'
    return
  }

  if (registerForm.password !== registerForm.confirmPassword) {
    errorMessage.value = '两次输入的密码不一致'
    return
  }

  loading.value = true
  try {
    await register({
      username: registerForm.username.trim(),
      password: registerForm.password,
    })

    form.username = registerForm.username.trim()
    form.password = ''
    registerForm.password = ''
    registerForm.confirmPassword = ''
    mode.value = 'login'
    showPassword.value = false
    successMessage.value = '注册成功，请使用新账号登录'
  } catch (error) {
    errorMessage.value = error.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <div class="login-card">
      <div class="brand-area">
        <div class="brand-logo">
          <svg viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
            <text class="logo-text" x="50%" y="50%" dominant-baseline="central" text-anchor="middle"
                  font-family="'Segoe UI', -apple-system, BlinkMacSystemFont, sans-serif"
                  font-size="26" font-weight="900" fill="#171717"
                  stroke="#171717" stroke-width="2"
                  style="paint-order: stroke fill;"
                  letter-spacing="1.5">ZH</text>
          </svg>
        </div>
        <div class="brand-text-group">
          <h1 class="brand-title">知航</h1>
        </div>
      </div>

      <form v-if="mode === 'login'" class="login-form" @submit.prevent="handleLogin">
        <div class="form-group">
          <label class="form-label">账号</label>
          <div class="input-wrapper">
            <input
              v-model="form.username"
              class="form-input"
              autocomplete="username"
              placeholder="请输入账号"
            />
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">密码</label>
          <div class="input-wrapper">
            <input
              v-model="form.password"
              class="form-input"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="current-password"
              placeholder="请输入密码"
            />
            <button
              type="button"
              class="toggle-password"
              @click="togglePassword"
            >
              <svg v-if="!showPassword" class="eye-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                <circle cx="12" cy="12" r="3"></circle>
              </svg>
              <svg v-else class="eye-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                <line x1="1" y1="1" x2="23" y2="23"></line>
              </svg>
              <span class="toggle-text">{{ showPassword ? '隐藏' : '显示' }}</span>
            </button>
          </div>
        </div>

        <p v-if="successMessage" class="form-success">{{ successMessage }}</p>
        <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

        <div class="button-group">
          <button
            class="submit-btn"
            :disabled="loading"
            type="submit"
          >
            <span v-if="loading" class="btn-loading"></span>
            <span class="btn-text">{{ loading ? '登录中...' : '进入校园助手' }}</span>
          </button>

          <button
            class="register-btn"
            type="button"
            @click="switchMode('register')"
          >
            注册新账号
          </button>
        </div>
      </form>

      <form v-else class="login-form" @submit.prevent="handleRegister">
        <div class="form-group">
          <label class="form-label">账号</label>
          <div class="input-wrapper">
            <input
              v-model="registerForm.username"
              class="form-input"
              autocomplete="username"
              placeholder="请设置账号（3-50位）"
            />
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">密码</label>
          <div class="input-wrapper">
            <input
              v-model="registerForm.password"
              class="form-input"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="new-password"
              placeholder="请设置密码（至少6位）"
            />
            <button
              type="button"
              class="toggle-password"
              @click="togglePassword"
            >
              <svg v-if="!showPassword" class="eye-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                <circle cx="12" cy="12" r="3"></circle>
              </svg>
              <svg v-else class="eye-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                <line x1="1" y1="1" x2="23" y2="23"></line>
              </svg>
              <span class="toggle-text">{{ showPassword ? '隐藏' : '显示' }}</span>
            </button>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">确认密码</label>
          <div class="input-wrapper">
            <input
              v-model="registerForm.confirmPassword"
              class="form-input"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="new-password"
              placeholder="请再次输入密码"
            />
          </div>
        </div>

        <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

        <div class="button-group">
          <button
            class="submit-btn"
            :disabled="loading"
            type="submit"
          >
            <span v-if="loading" class="btn-loading"></span>
            <span class="btn-text">{{ loading ? '注册中...' : '注册账号' }}</span>
          </button>

          <button
            class="register-btn"
            type="button"
            @click="switchMode('login')"
          >
            返回登录
          </button>
        </div>
      </form>
    </div>
  </main>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  position: relative;
  overflow: hidden;
  color: var(--hp-ink);
  background: var(--hp-bg);
  font-family: Inter, 'Segoe UI', system-ui, -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.login-card {
  width: 100%;
  max-width: 420px;
  background: var(--hp-cream);
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-lg);
  padding: 44px 40px 36px;
  position: relative;
  z-index: 1;
  overflow: hidden;
  animation: card-reveal 0.7s cubic-bezier(0.22, 1, 0.36, 1) both;
}

@keyframes card-reveal {
  0% {
    opacity: 0;
    transform: translateY(30px) scale(0.96);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.brand-area {
  text-align: center;
  margin-bottom: 40px;
  animation: fade-slide-up 0.6s ease-out 0.1s both;
}

@keyframes fade-slide-up {
  0% {
    opacity: 0;
    transform: translateY(16px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

.brand-logo {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 60px;
  height: 60px;
  background: var(--hp-yellow);
  border: 1px solid var(--hp-line);
  border-radius: 16px;
  color: var(--hp-ink);
  margin-bottom: 16px;
  animation: logo-appear 0.5s cubic-bezier(0.22, 1, 0.36, 1) 0.2s both;
}

.brand-logo svg {
  width: 34px;
  height: 34px;
}

@keyframes logo-appear {
  0% { opacity: 0; transform: scale(0.9); }
  100% { opacity: 1; transform: scale(1); }
}

.brand-text-group {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.brand-title {
  font-size: 34px;
  font-weight: 700;
  color: var(--hp-ink);
  margin: 0;
  letter-spacing: -0.02em;
  line-height: 1.2;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  animation: field-enter 0.5s ease-out both;
}

.form-group:nth-child(1) { animation-delay: 0.3s; }
.form-group:nth-child(2) { animation-delay: 0.4s; }

@keyframes field-enter {
  0% {
    opacity: 0;
    transform: translateY(12px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

.form-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--hp-ink);
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  border: 1px solid rgba(23, 23, 23, 0.24);
  border-radius: 999px;
  background: transparent;
  transition: border-color 0.2s ease;
}

.input-wrapper:hover {
  border-color: rgba(23, 23, 23, 0.4);
}

.input-wrapper:focus-within {
  border-color: var(--hp-ink);
}

.form-input {
  flex: 1;
  height: 46px;
  padding: 0 16px;
  border: none;
  outline: none;
  background: transparent;
  font-size: 14px;
  color: var(--hp-ink);
  font-family: inherit;
}

.form-input::placeholder {
  color: #a8a196;
}

.toggle-password {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  margin-right: 6px;
  background: transparent;
  border: none;
  border-radius: 999px;
  cursor: pointer;
  color: var(--hp-muted);
  font-size: 12px;
  font-weight: 600;
  transition: color 0.18s ease, background 0.18s ease;
}

.toggle-password:hover {
  color: var(--hp-ink);
  background: rgba(23, 23, 23, 0.06);
}

.toggle-password:active {
  transform: scale(0.95);
}

.eye-icon {
  width: 16px;
  height: 16px;
  transition: transform 0.2s ease;
}

.toggle-password:hover .eye-icon {
  transform: scale(1.1);
}

.form-error {
  margin: 0;
  padding: 10px 14px;
  background: #faf0ee;
  border: 1px solid #d9b0ab;
  border-radius: var(--hp-r-md);
  color: #a54239;
  font-size: 13px;
  animation: error-shake 0.5s cubic-bezier(0.36, 0.07, 0.19, 0.97);
}

.form-success {
  margin: 0;
  padding: 10px 14px;
  background: #e7ead9;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-md);
  color: var(--hp-ink);
  font-size: 13px;
}

@keyframes error-shake {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-8px); }
  40% { transform: translateX(8px); }
  60% { transform: translateX(-5px); }
  80% { transform: translateX(5px); }
}

.button-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 6px;
  animation: field-enter 0.5s ease-out 0.5s both;
}

.submit-btn {
  height: 48px;
  border: 1px solid var(--hp-ink);
  border-radius: 999px;
  background: var(--hp-ink);
  color: var(--hp-cream);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: background 0.2s ease;
}

.submit-btn::before {
  content: none;
}

.submit-btn:hover:not(:disabled) {
  background: #2f2f2f;
}

.submit-btn:active:not(:disabled) {
  transform: scale(0.99);
}

.submit-btn:disabled {
  background: #d8d1c4;
  border-color: #d8d1c4;
  color: #fbf8f2;
  cursor: not-allowed;
}

.btn-loading {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(251, 248, 242, 0.35);
  border-top-color: #fbf8f2;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.btn-text {
  position: relative;
  z-index: 1;
}

.register-btn {
  height: 44px;
  border: 1px solid var(--hp-line);
  border-radius: 999px;
  background: transparent;
  color: var(--hp-ink);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
  position: relative;
}

.register-btn::before {
  content: none;
}

.register-btn:hover {
  color: var(--hp-cream);
  background: var(--hp-ink);
}

.register-btn:active {
  transform: scale(0.99);
}
</style>
