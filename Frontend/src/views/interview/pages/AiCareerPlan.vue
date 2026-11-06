<template>
  <div class="page-layout">
    <Sidebar />
    <main class="main-content">
      <div class="content-area">
        <div class="main-body">
          <!-- 页面标题 -->
          <div class="page-header">
            <h1 class="page-title">职业规划师</h1>
            <span class="sync-badge">实时同步中</span>
          </div>

          <!-- 顶部状态卡片 -->
          <div class="status-cards">
            <div class="status-card">
              <div class="status-label">
                <span>岗位适配</span>
              </div>
              <div class="status-value">资深云架构师</div>
            </div>
            <div class="status-card">
              <div class="status-label">
                <span>市场热度</span>
              </div>
              <div class="status-value">
                需求缺口 <span class="trend-up">↑15%</span>
              </div>
            </div>
            <div class="status-card">
              <div class="status-label">
                <span>进阶方向</span>
              </div>
              <div class="status-value">AI 基础设施专家</div>
            </div>
          </div>

          <!-- 职场向导 - AI对话区域 -->
          <div class="chat-section">
            <div class="chat-header">
              <span class="chat-title">职场向导</span>
              <span class="online-indicator"></span>
            </div>
            <div class="chat-messages" ref="chatMessagesRef">
              <!-- 欢迎语 -->
              <div class="chat-welcome">
                <div class="welcome-avatar">
                  <img :src="robot2Img" alt="AI" class="robot-img" />
                </div>
                <div class="welcome-text">
                  你好！我是你的 AI 职业规划师，可以帮你分析职业发展方向、优化简历、制定学习计划。
                </div>
              </div>
              <!-- 消息列表 -->
              <div
                v-for="(msg, idx) in messages"
                :key="idx"
                class="message"
                :class="msg.role"
                v-show="!(thinking && msg.role === 'assistant' && !msg.content.trim())"
              >
                <div class="message-avatar">
                  <img v-if="msg.role === 'assistant'" :src="robot2Img" alt="AI" class="robot-img" />
                  <img
                    v-else-if="userProfile?.avatar_url"
                    :src="userProfile.avatar_url"
                    alt="用户头像"
                    class="user-message-avatar"
                  />
                  <span v-else>👤</span>
                </div>
                <div class="message-content">
                  <div class="message-bubble" v-html="formatMessage(msg.content)"></div>
                  <div class="message-time">{{ msg.role === 'assistant' ? 'AI 导师' : '你' }} · {{ msg.time }}</div>
                  <!-- AI消息的标签 -->
                  <div v-if="msg.role === 'assistant' && msg.tags" class="message-tags">
                    <span v-for="tag in msg.tags" :key="tag" class="msg-tag">{{ tag }}</span>
                  </div>
                </div>
              </div>
              <!-- 思考中 -->
              <div v-if="showThinkingIndicator" class="thinking-indicator">
                <div class="message-avatar thinking-avatar">
                  <img :src="robot2Img" alt="AI" class="robot-img" />
                </div>
                <div class="thinking-bubble">
                  <span class="thinking-text">正在思考</span>
                  <span class="thinking-dots">
                    <i></i>
                    <i></i>
                    <i></i>
                  </span>
                </div>
              </div>
            </div>
            <!-- 快捷问题 -->
            <div class="quick-questions">
              <button
                v-for="q in quickQuestions"
                :key="q"
                class="quick-btn"
                @click="sendQuickQuestion(q)"
                :disabled="thinking"
              >
                {{ q }}
              </button>
            </div>
            <!-- 输入框 -->
            <div class="chat-input-area">
              <div class="input-wrapper">
                <button class="attach-btn">📎</button>
                <input
                  v-model="inputText"
                  type="text"
                  placeholder="输入你的职业困惑或需求..."
                  @keyup.enter="sendMessage"
                  :disabled="thinking"
                />
                <button
                  class="send-btn"
                  @click="sendMessage"
                  :disabled="thinking || !inputText.trim()"
                >
                  <span class="send-icon">➤</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 背景遮罩层 -->
    <div class="drawer-backdrop" :class="{ visible: drawerExpanded }" @click="toggleDrawer"></div>

    <!-- 右侧抽屉栏 -->
    <aside class="right-drawer" :class="{ expanded: drawerExpanded }">
      <!-- 抽屉触发按钮 -->
      <div class="drawer-trigger" @click="toggleDrawer">
        <div class="trigger-icon">📄</div>
        <div class="trigger-text">智能简历</div>
        <div class="trigger-arrow" :class="{ rotated: drawerExpanded }">‹</div>
      </div>
      <!-- 抽屉内容 -->
      <div class="drawer-content" v-show="drawerExpanded">
        <div class="drawer-header">
          <h3>智能简历同步</h3>
          <button class="close-btn" @click="toggleDrawer">✕</button>
        </div>
        <div class="drawer-body">
          <!-- 简历预览卡片 -->
          <div class="resume-preview-card">
            <!-- 两栏布局：左侧20%，右侧80% -->
            <div class="resume-columns">
              <!-- 左侧栏：头像 + 个人信息 + 技术栈 -->
              <div class="resume-left">
                <!-- 头像 -->
                <div class="resume-avatar-section">
                  <img v-if="userProfile?.avatar_url" :src="userProfile.avatar_url" class="avatar-img-large" alt="头像" />
                  <div v-else class="avatar-placeholder-large">👤</div>
                </div>

                <!-- 个人信息 -->
                <div class="resume-section">
                  <h4 class="section-title">个人信息</h4>
                  <div class="personal-info">
                    <div class="info-item">
                      <span class="info-label">姓名</span>
                      <span class="info-value">{{ userProfile?.nickname || '未填写' }}</span>
                    </div>
                    <div class="info-item">
                      <span class="info-label">目标岗位</span>
                      <span class="info-value">{{ userProfile?.target_position || '未设置' }}</span>
                    </div>
                    <div class="info-item" v-if="userProfile?.email">
                      <span class="info-label">邮箱</span>
                      <span class="info-value">{{ userProfile.email }}</span>
                    </div>
                    <div class="info-item" v-if="userProfile?.phone">
                      <span class="info-label">电话</span>
                      <span class="info-value">{{ userProfile.phone }}</span>
                    </div>
                    <div class="info-item" v-if="userProfile?.work_experience_years">
                      <span class="info-label">工作年限</span>
                      <span class="info-value">{{ userProfile.work_experience_years }}年</span>
                    </div>
                  </div>
                </div>

                <!-- 技术栈 -->
                <div class="resume-section">
                  <h4 class="section-title">技术栈</h4>
                  <div class="skill-list-compact">
                    <div class="skill-item-compact" v-for="(tech, index) in techStackList.slice(0, 4)" :key="index">
                      <span class="skill-name-compact">{{ tech }}</span>
                      <div class="skill-bar-compact">
                        <div class="skill-progress-compact" :style="{ width: (90 - index * 5) + '%' }"></div>
                      </div>
                    </div>
                  </div>
                  <div class="skill-tags-compact">
                    <span class="skill-tag-compact" v-for="(tag, index) in skillTagsList.slice(0, 6)" :key="index">{{ tag }}</span>
                  </div>
                </div>
              </div>

              <!-- 右侧栏：教育背景 + 工作经历 -->
              <div class="resume-right">
                <!-- 教育背景 -->
                <div class="resume-section">
                  <h4 class="section-title">教育背景</h4>
                  <div class="education-row">
                    <div class="education-item-horizontal">
                      <div class="school-name">清华大学</div>
                      <div class="degree">计算机科学 · 硕士</div>
                      <div class="edu-date">2014 - 2016</div>
                    </div>
                    <div class="education-item-horizontal">
                      <div class="school-name">上海交通大学</div>
                      <div class="degree">软件工程 · 学士</div>
                      <div class="edu-date">2010 - 2014</div>
                    </div>
                  </div>
                </div>

                <!-- 工作经历 -->
                <div class="resume-section">
                  <h4 class="section-title">工作经历</h4>
                  <div class="work-list">
                    <div class="work-item">
                      <div class="work-header">
                        <span class="work-date">2018-09 ~ 至今</span>
                        <span class="work-company">全民简历科技有限公司</span>
                        <span class="work-position">行政专员</span>
                      </div>
                      <ul class="work-desc">
                        <li>拥负责本部的行政人事管理和日常事务，协助总监搞好各部门之间的综合协调，落实公司规章制度，沟通内外联系，保证上情下达和下情上报，负责对会议文件决定的事项进行催办、查办和落实，负责全公司组织系统研讨和修订。</li>
                        <li>编制公司人事管理制度，规避各项人事风险。</li>
                      </ul>
                    </div>
                    <div class="work-item">
                      <div class="work-header">
                        <span class="work-date">2016-09 ~ 2018-08</span>
                        <span class="work-company">上海斧掌网络科技有限公司</span>
                        <span class="work-position">行政专员</span>
                      </div>
                      <ul class="work-desc">
                        <li>负责中心简单财务管理，资产管控；</li>
                        <li>负责公司总部的来访客户接待工作，负责引导和介绍公司的分布情况；</li>
                        <li>负责中心的行政事务，公司班车管理、负责建立员工归属感及前台管理；</li>
                        <li>负责招聘工作，确保人才梯队发展和人才储备及培养。</li>
                        <li>督导公司各项行政、人事制度、员工福利、生日以及公司各种宴会活动的执行。</li>
                        <li>负责招聘工作，制定公司的人力资源发展计划，确保人才梯队发展和人才储备及培养。</li>
                      </ul>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="drawer-footer">
          <div class="sync-status">
            <span class="sync-dot"></span>
            <span class="sync-text">已通过折跃网同步实时更新</span>
          </div>
          <button class="edit-resume-btn">
            <span class="btn-icon">✎</span>
            <span>编辑</span>
          </button>
          <button class="export-pdf-btn">
            <span class="btn-icon">⬇</span>
            <span>导出高清 PDF</span>
          </button>
        </div>
      </div>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, computed } from 'vue'
import Sidebar from '../components/Sidebar.vue'
import { aiCoachApi } from '../api/index'
import { authApi, type UserProfileDetail } from '../api/auth'
import robot2Img from '@/assets/interview/mianshiguan.png'

// 消息类型
interface Message {
  role: 'user' | 'assistant'
  content: string
  time: string
  tags?: string[]
}

// 用户档案数据
const userProfile = ref<UserProfileDetail | null>(null)

// 计算属性：解析技能标签
const skillTagsList = computed(() => {
  if (!userProfile.value?.skill_tags) return []
  return userProfile.value.skill_tags.split(',').map(s => s.trim()).filter(Boolean)
})

// 计算属性：解析技术栈
const techStackList = computed(() => {
  if (!userProfile.value?.tech_stack) return []
  return userProfile.value.tech_stack.split(/[,\n]/).map(s => s.trim()).filter(Boolean)
})


// 对话状态
const messages = ref<Message[]>([])
const inputText = ref('')
const thinking = ref(false)
const chatMessagesRef = ref<HTMLElement | null>(null)

const showThinkingIndicator = computed(() => {
  if (!thinking.value) return false
  const lastMsg = messages.value[messages.value.length - 1]
  return !!lastMsg && lastMsg.role === 'assistant' && !lastMsg.content.trim()
})

// 快捷问题
const quickQuestions = [
  '如何准备技术面试？',
  '我的职业发展方向是什么？',
  '如何提升竞争力？',
  '简历如何优化？'
]

// 抽屉状态
const drawerExpanded = ref(false)

// 切换抽屉
function toggleDrawer() {
  drawerExpanded.value = !drawerExpanded.value
}

// 格式化消息内容（处理换行）
function formatMessage(content: string): string {
  return content.replace(/\n/g, '<br>')
}

// 滚动到底部
async function scrollToBottom() {
  await nextTick()
  const el = chatMessagesRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

// 获取当前时间
function getCurrentTime(): string {
  const now = new Date()
  return now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// 发送消息
async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || thinking.value) return

  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: text,
    time: getCurrentTime()
  })
  inputText.value = ''
  thinking.value = true
  await scrollToBottom()

  // 添加助手消息占位
  const assistantIndex = messages.value.length
  messages.value.push({
    role: 'assistant',
    content: '',
    time: getCurrentTime(),
    tags: []
  })

  try {
    await new Promise<void>((resolve, reject) => {
      aiCoachApi.chatStream(
        text,
        (chunk) => {
          const msg = messages.value[assistantIndex]
          if (msg) {
            msg.content += chunk
          }
          scrollToBottom()
        },
        () => {
          thinking.value = false
          // 模拟添加标签
          const msg = messages.value[assistantIndex]
          if (msg && msg.content.includes('优化')) {
            msg.tags = ['简历优化', 'AI建议']
          }
          scrollToBottom()
          resolve()
        },
        (error) => {
          const msg = messages.value[assistantIndex]
          if (msg) {
            msg.content = '抱歉，网络异常，请稍后重试。'
          }
          thinking.value = false
          scrollToBottom()
          reject(error)
        }
      )
    })
  } catch (e) {
    const msg = messages.value[assistantIndex]
    if (msg) {
      msg.content = '抱歉，网络异常，请稍后重试。'
    }
    thinking.value = false
    scrollToBottom()
  }
}

// 发送快捷问题
function sendQuickQuestion(q: string) {
  inputText.value = q
  sendMessage()
}

// 加载用户档案
async function loadUserProfile() {
  try {
    const profile = await authApi.getCurrentProfile()
    userProfile.value = profile
  } catch (e) {
    console.error('加载用户档案失败:', e)
  }
}

onMounted(() => {
  // 加载用户档案数据
  loadUserProfile()
})
</script>

<style scoped>
/* ========== 基础布局 ========== */
.page-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: var(--hp-surface);
}

.main-content {
  flex: 1;
  margin-left: clamp(200px, 25vw, 320px);
  margin-right: 48px; /* 为抽屉按钮预留空间 */
  height: 100vh;
  background: var(--hp-surface);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: margin-right 0.3s ease;
}

.content-area {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.main-body {
  flex: 1;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow: hidden;
  min-height: 0;
}

/* ========== 页面头部 ========== */
.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #ffffff;
  margin: 0;
}

.sync-badge {
  font-size: 11px;
  padding: 4px 10px;
  background: rgba(16, 185, 129, 0.15);
  color: #10b981;
  border-radius: 12px;
  border: 1px solid rgba(16, 185, 129, 0.3);
}

/* ========== 状态卡片 ========== */
.status-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  flex-shrink: 0;
}

.status-card {
  background: var(--hp-surface);
  border: 1px solid var(--hp-line);
  border-radius: 20px;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.status-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #6b7280;
}

.status-icon {
  font-size: 14px;
}

.status-value {
  font-size: 15px;
  font-weight: 600;
  color: var(--hp-ink);
}

.trend-up {
  color: #10b981;
}

/* ========== 对话区域 ========== */
.chat-section {
  flex: 1;
  background: var(--hp-surface);
  border: 1px solid var(--hp-line);
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--hp-line);
  flex-shrink: 0;
}

.chat-title {
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
}

.online-indicator {
  width: 8px;
  height: 8px;
  background: #10b981;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
  background: var(--hp-surface);
}

/* 欢迎语 */
.chat-welcome {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.welcome-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--hp-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
  overflow: hidden;
  border: 1px solid var(--hp-line);
}

.robot-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-message-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.welcome-text {
  padding: 12px 16px;
  background: var(--hp-surface);
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--hp-ink);
  width: fit-content;
  max-width: min(70%, 760px);
  word-break: break-word;
}

/* 消息气泡 */
.message {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--hp-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
  border: 1px solid var(--hp-line);
}

.message.user .message-avatar {
  background: #3a7bc8;
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: fit-content;
  max-width: min(70%, 760px);
}

.message.user .message-content {
  align-items: flex-end;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--hp-ink);
  background: var(--hp-surface);
  word-break: break-word;
  max-width: 100%;
}

.message.user .message-bubble {
  background: #3a7bc8;
  color: #ffffff;
  border-radius: 12px;
}

.thinking-indicator {
  display: flex;
  align-items: center;
  gap: 12px;
}

.thinking-avatar {
  background: var(--hp-surface);
}

.thinking-bubble {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 14px;
  color: #c5d4ee;
  font-size: 13px;
  background: var(--hp-surface-2);
  border: 1px solid rgba(76, 125, 199, 0.18);
  box-shadow: 0 10px 24px rgba(6, 13, 28, 0.16);
}

.thinking-text {
  font-size: 13px;
  font-weight: 500;
}

.thinking-dots {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.thinking-dots i {
  display: block;
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: #6aa8ff;
  opacity: 0.35;
  animation: thinkingPulse 1.2s infinite ease-in-out;
}

.thinking-dots i:nth-child(2) {
  animation-delay: 0.15s;
}

.thinking-dots i:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes thinkingPulse {
  0%, 80%, 100% {
    opacity: 0.35;
    transform: translateY(0);
  }
  40% {
    opacity: 1;
    transform: translateY(-2px);
  }
}

.message-time {
  font-size: 11px;
  color: #6b7280;
}

.message-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.msg-tag {
  font-size: 11px;
  padding: 4px 10px;
  background: rgba(58, 123, 200, 0.15);
  color: #3a7bc8;
  border-radius: 12px;
  border: 1px solid rgba(58, 123, 200, 0.3);
}

/* 快捷问题 */
.quick-questions {
  display: flex;
  gap: 10px;
  padding: 12px 20px;
  border-top: 1px solid var(--hp-line);
  flex-wrap: wrap;
  flex-shrink: 0;
}

.quick-btn {
  padding: 8px 14px;
  background: var(--hp-surface);
  border: 1px solid var(--hp-line);
  border-radius: 16px;
  color: #9ca3af;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-btn:hover:not(:disabled) {
  background: var(--hp-surface);
  color: var(--hp-ink);
  border-color: #3a7bc8;
}

.quick-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 输入区域 */
.chat-input-area {
  padding: 12px 20px 16px;
  border-top: 1px solid var(--hp-line);
  flex-shrink: 0;
}

.input-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--hp-surface);
  border: 1px solid var(--hp-line);
  border-radius: 24px;
  padding: 4px 4px 4px 12px;
}

.attach-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: #6b7280;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s;
}

.attach-btn:hover {
  color: #9ca3af;
}

.input-wrapper input {
  flex: 1;
  height: 40px;
  border: none;
  background: transparent;
  color: #ffffff;
  font-size: 13px;
  outline: none;
}

.input-wrapper input::placeholder {
  color: #6b7280;
}

.input-wrapper input:disabled {
  opacity: 0.6;
}

.send-btn {
  width: 40px;
  height: 40px;
  border: none;
  background: #3a7bc8;
  border-radius: 50%;
  color: #ffffff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.send-btn:hover:not(:disabled) {
  background: #2d6bb3;
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.send-icon {
  font-size: 14px;
  margin-left: 2px;
}

/* ========== 背景遮罩层 ========== */
.drawer-backdrop {
  position: fixed;
  left: 0;
  top: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  z-index: 99;
  opacity: 0;
  visibility: hidden;
  transition: opacity 0.3s ease, visibility 0.3s ease;
}

.drawer-backdrop.visible {
  opacity: 1;
  visibility: visible;
}

/* ========== 右侧抽屉 ========== */
.right-drawer {
  position: fixed;
  right: 0;
  top: 0;
  height: 100vh;
  display: flex;
  z-index: 100;
}

.drawer-trigger {
  width: 48px;
  height: 100vh;
  background: var(--hp-surface);
  border-left: 1px solid var(--hp-line);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;
  cursor: pointer;
  transition: background 0.3s ease;
  user-select: none;
}

.drawer-trigger:hover {
  background: var(--hp-surface);
}

.trigger-icon {
  font-size: 20px;
  margin-bottom: 8px;
}

.trigger-text {
  writing-mode: vertical-rl;
  text-orientation: mixed;
  font-size: 12px;
  color: #9ca3af;
  letter-spacing: 2px;
  margin-bottom: 12px;
}

.trigger-arrow {
  font-size: 16px;
  color: #6b7280;
  transition: transform 0.3s ease;
}

.trigger-arrow.rotated {
  transform: rotate(180deg);
}

.drawer-content {
  width: 70vw;
  max-width: 1200px;
  height: 100vh;
  background: var(--hp-surface);
  border-left: 1px solid var(--hp-line);
  display: flex;
  flex-direction: column;
  transform: translateX(100%);
  opacity: 0;
  transition: transform 0.3s ease, opacity 0.3s ease;
}

.right-drawer.expanded .drawer-content {
  transform: translateX(0);
  opacity: 1;
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--hp-line);
  flex-shrink: 0;
}

.drawer-header h3 {
  font-size: 15px;
  font-weight: 600;
  color: #ffffff;
  margin: 0;
}

.close-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: var(--hp-surface);
  border-radius: 6px;
  color: #6b7280;
  font-size: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.close-btn:hover {
  background: var(--hp-surface);
  color: var(--hp-ink);
}

.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: var(--hp-surface);
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

/* 简历预览卡片 */
.resume-preview-card {
  background: var(--hp-surface);
  border: 1px solid var(--hp-line);
  overflow: hidden;
  width: 100%;
  max-width: 1100px;
  height: fit-content;
  transform: scale(1.03);
  transform-origin: top center;
}

/* 简历头部 */
.resume-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 24px;
  background: var(--hp-surface-2);
  border-bottom: 1px solid var(--hp-line);
}

.header-main {
  flex: 1;
}

.resume-name {
  font-size: 24px;
  font-weight: 700;
  color: #ffffff;
  margin: 0 0 8px 0;
}

.resume-title {
  font-size: 14px;
  color: #3a7bc8;
  margin: 0 0 16px 0;
  font-weight: 500;
}

.contact-info {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 20px;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #9ca3af;
}

.contact-icon {
  font-size: 12px;
  color: #3a7bc8;
}

.header-avatar {
  margin-left: 20px;
}

.avatar-placeholder {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  background: var(--hp-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  color: #6b7280;
  border: 2px solid var(--hp-line);
}

.avatar-img {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  object-fit: cover;
  border: 2px solid var(--hp-line);
}

/* 空状态占位符 */
.empty-placeholder {
  padding: 16px;
  text-align: center;
  color: #6b7280;
  font-size: 12px;
}

.empty-text {
  color: #6b7280;
  font-size: 12px;
}

/* 工作经历文本 */
.work-experience-content {
  padding: 12px 0;
}

.work-experience-text {
  font-size: 12px;
  color: #9ca3af;
  line-height: 1.8;
  white-space: pre-wrap;
  margin: 0;
}

/* 工作经历列表新样式 */
.work-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.work-item {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.work-header {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.work-date {
  font-size: 13px;
  color: #6b7280;
  white-space: nowrap;
  width: 120px;
  flex-shrink: 0;
}

.work-company {
  font-size: 14px;
  font-weight: 700;
  color: #3a7bc8;
  flex: 1;
  text-align: center;
}

.work-position {
  font-size: 13px;
  color: #9ca3af;
  white-space: nowrap;
  width: 80px;
  flex-shrink: 0;
  text-align: right;
}

.work-desc {
  margin: 0;
  padding-left: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.work-desc li {
  font-size: 12px;
  color: #9ca3af;
  line-height: 1.8;
  position: relative;
  padding-left: 16px;
}

.work-desc li::before {
  content: '•';
  position: absolute;
  left: 0;
  color: #3a7bc8;
}

/* 两栏布局：左侧20%，右侧80% */
.resume-columns {
  display: flex;
  gap: 0;
  min-height: 100%;
}

.resume-left {
  width: 20%;
  min-width: 180px;
  padding: 24px 20px;
  border-right: 1px solid var(--hp-line);
  background: var(--hp-surface);
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.resume-right {
  flex: 1;
  padding: 24px;
  background: var(--hp-surface);
}

/* 左侧头像区域 */
.resume-avatar-section {
  display: flex;
  justify-content: center;
  padding-bottom: 8px;
}

.avatar-img-large {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--hp-line);
}

.avatar-placeholder-large {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: var(--hp-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  color: #6b7280;
  border: 1px solid var(--hp-line);
}

/* 左侧个人信息 */
.personal-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 11px;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.info-value {
  font-size: 13px;
  color: #e5e7eb;
  font-weight: 500;
  word-break: break-word;
}

/* 左侧技术栈紧凑样式 */
.skill-list-compact {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 12px;
}

.skill-item-compact {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.skill-name-compact {
  font-size: 11px;
  color: #e5e7eb;
}

.skill-bar-compact {
  height: 3px;
  background: var(--hp-surface);
  border-radius: 2px;
  overflow: hidden;
}

.skill-progress-compact {
  height: 100%;
  background: linear-gradient(90deg, #3a7bc8 0%, #60a5fa 100%);
  border-radius: 2px;
}

.skill-tags-compact {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.skill-tag-compact {
  font-size: 10px;
  padding: 4px 8px;
  background: rgba(58, 123, 200, 0.1);
  color: #3a7bc8;
  border-radius: 4px;
  border: 1px solid rgba(58, 123, 200, 0.2);
}

/* 区块标题 */
.resume-section {
  margin-bottom: 28px;
}

.resume-section:last-child {
  margin-bottom: 0;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #ffffff;
  margin: 0 0 16px 0;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--hp-line);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* 时间线样式 */
.timeline {
  position: relative;
}

.timeline-item {
  position: relative;
  padding-left: 20px;
  padding-bottom: 24px;
}

.timeline-item:last-child {
  padding-bottom: 0;
}

.timeline-item::before {
  content: '';
  position: absolute;
  left: 5px;
  top: 8px;
  bottom: 0;
  width: 1px;
  background: var(--hp-surface);
}

.timeline-item:last-child::before {
  display: none;
}

.timeline-dot {
  position: absolute;
  left: 0;
  top: 6px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #3a7bc8;
  border: 2px solid var(--hp-line);
}

.timeline-content {
  margin-top: -4px;
}

.timeline-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
}

.company-name {
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  margin-bottom: 4px;
}

.job-title {
  font-size: 12px;
  color: #3a7bc8;
}

.timeline-date {
  font-size: 11px;
  color: #6b7280;
  white-space: nowrap;
}

.timeline-desc {
  margin: 0;
  padding-left: 0;
  list-style: none;
}

.timeline-desc li {
  font-size: 12px;
  color: #9ca3af;
  line-height: 1.8;
  margin-bottom: 6px;
  position: relative;
  padding-left: 12px;
}

.timeline-desc li::before {
  content: '•';
  position: absolute;
  left: 0;
  color: #3a7bc8;
}

.timeline-desc li:last-child {
  margin-bottom: 0;
}

.timeline-desc strong {
  color: #10b981;
  font-weight: 600;
}

.project-link {
  font-size: 12px;
  color: #3a7bc8;
  text-decoration: none;
  transition: color 0.2s;
}

.project-link:hover {
  color: #60a5fa;
}

/* 教育背景 */
.education-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.education-item {
  padding-bottom: 16px;
  border-bottom: 1px solid var(--hp-line);
}

.education-item:last-child {
  padding-bottom: 0;
  border-bottom: none;
}

/* 横向排列的教育背景 */
.education-row {
  display: flex;
  flex-direction: row;
  gap: 24px;
  flex-wrap: wrap;
}

.education-item-horizontal {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--hp-surface);
  border: 1px solid var(--hp-line);
  border-radius: 8px;
  flex: 1;
  min-width: 280px;
}

.school-name {
  font-size: 13px;
  font-weight: 600;
  color: #ffffff;
  margin-bottom: 4px;
}

.education-item-horizontal .school-name {
  margin-bottom: 0;
  white-space: nowrap;
}

.degree {
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 4px;
}

.education-item-horizontal .degree {
  margin-bottom: 0;
  color: #6b7280;
}

.edu-date {
  font-size: 11px;
  color: #6b7280;
}

.education-item-horizontal .edu-date {
  margin-left: auto;
  padding-left: 12px;
  border-left: 1px solid var(--hp-line);
  white-space: nowrap;
}

/* 技术栈 */
.skill-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 16px;
}

.skill-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.skill-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.skill-name {
  font-size: 12px;
  color: #e5e7eb;
}

.skill-percent {
  font-size: 11px;
  color: #3a7bc8;
  font-weight: 600;
}

.skill-bar {
  height: 4px;
  background: var(--hp-surface);
  border-radius: 2px;
  overflow: hidden;
}

.skill-progress {
  height: 100%;
  background: linear-gradient(90deg, #3a7bc8 0%, #60a5fa 100%);
  border-radius: 2px;
  transition: width 0.5s ease;
}

.skill-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.skill-tag {
  font-size: 10px;
  padding: 5px 10px;
  background: rgba(58, 123, 200, 0.1);
  color: #3a7bc8;
  border-radius: 4px;
  border: 1px solid rgba(58, 123, 200, 0.2);
}

.drawer-footer {
  padding: 16px 24px;
  border-top: 1px solid var(--hp-line);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 16px;
  background: var(--hp-surface);
}

.sync-status {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.sync-dot {
  width: 8px;
  height: 8px;
  background: #10b981;
  border-radius: 50%;
}

.sync-text {
  font-size: 12px;
  color: #6b7280;
}

.edit-resume-btn {
  padding: 10px 24px;
  background: var(--hp-surface);
  border: 1px solid var(--hp-line);
  border-radius: 20px;
  color: var(--hp-ink);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.edit-resume-btn:hover {
  background: var(--hp-surface);
  border-color: #3a7bc8;
  color: var(--hp-ink);
}

.export-pdf-btn {
  padding: 10px 24px;
  background: #3a7bc8;
  border: none;
  border-radius: 20px;
  color: #ffffff;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.export-pdf-btn:hover {
  background: #2d6bb3;
}

.btn-icon {
  font-size: 14px;
}

/* ========== 响应式 ========== */
@media (max-width: 1200px) {
  .status-cards {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .main-content {
    margin-right: 48px;
  }
  .drawer-content {
    width: 75vw;
  }
}
</style>
