<template>
  <div class="page-layout">
    <main class="main-content">
      <div class="page-container">
        <div class="cockpit">
          <header class="cockpit-header">
            <div class="brand-left">
              <div class="brand-title">智能面试舱</div>
              <div class="brand-sub">PRO v2.4</div>
            </div>
            <div class="header-actions">
              <div class="timer-pill">面试进行中 {{ timerText }}</div>
            </div>
          </header>

          <div class="cockpit-grid">
            <aside class="left-col">
              <div class="left-cards-wrapper">
                <!-- 面试概览卡片 -->
                <section class="left-card overview-card">
                  <div class="overview-header">
                    <span class="overview-title">面试概览</span>
                    <span class="overview-badge">{{ interviewProgressPercent }}%</span>
                  </div>
                  <div class="overview-grid">
                    <div class="overview-item">
                      <div class="overview-info">
                        <div class="overview-label">面试岗位</div>
                        <div class="overview-value">{{ jobRole || '未设置' }}</div>
                      </div>
                    </div>
                    <div class="overview-item">
                      <div class="overview-info">
                        <div class="overview-label">已进行时长</div>
                        <div class="overview-value">{{ timerText }}</div>
                      </div>
                    </div>
                    <div class="overview-item">
                      <div class="overview-info">
                        <div class="overview-label">已回答问题</div>
                        <div class="overview-value highlight">{{ answeredQuestionsCount }} 题</div>
                      </div>
                    </div>
                    <div class="overview-item">
                      <div class="overview-info">
                        <div class="overview-label">面试模式</div>
                        <div class="overview-value">{{ interviewModeLabel }}</div>
                      </div>
                    </div>
                  </div>
                  <div class="overview-progress">
                    <div class="overview-progress-bar">
                      <div class="overview-progress-fill" :style="{ width: interviewProgressPercent + '%' }"></div>
                    </div>
                    <div class="overview-progress-label">面试进度</div>
                  </div>
                </section>

                <!-- 候选人视频卡片 -->
                <section class="left-card face-card">
                  <div class="face-preview">
                    <video ref="cameraVideoRef" class="camera-video" autoplay playsinline muted v-show="cameraEnabled && !showCenterCameraPreview"></video>
                    <div class="camera-placeholder" v-show="!cameraEnabled || showCenterCameraPreview">
                      <button type="button" class="camera-logo" @click="handleCameraToggle" title="点击开启摄像头" aria-label="开启摄像头">
                        <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                          <rect x="3" y="6" width="12" height="12" rx="2" />
                          <path d="m15 10 6-3v10l-6-3" />
                        </svg>
                      </button>
                      <div v-if="showCenterCameraPreview" class="camera-wait-tip">等待接通中，画面已切换到主屏</div>
                      <div class="camera-error" v-if="cameraError">{{ cameraError }}</div>
                    </div>
                    <button class="camera-switch-btn" @click="handleCameraToggle" v-if="cameraEnabled">关闭摄像头</button>
                    <div class="face-tag" v-if="cameraEnabled">
                      <span class="rec-dot"></span>
                      REC 候选人
                    </div>
                  </div>
                  <div class="face-metrics" v-if="cameraEnabled">
                    <div class="metric-item">
                      <span class="metric-label">情绪</span>
                      <span class="metric-value emotion">{{ emotionCurrentLabel }}</span>
                    </div>
                    <div class="metric-divider"></div>
                    <div class="metric-item">
                      <span class="metric-label">心率</span>
                      <span class="metric-value heart-rate">78 bpm</span>
                    </div>
                  </div>
                </section>

                <!-- 环境监测卡片 -->
                <section class="left-card env-card">
                  <div class="env-header">
                    <span class="env-title">环境监测</span>
                    <span class="env-status-icon" :class="{ good: networkLatency < 50 }">✓</span>
                  </div>
                  <div class="env-item">
                    <div class="env-item-header">
                      <span class="env-icon">📶</span>
                      <span class="env-label">网络延迟</span>
                      <span class="env-value" :class="{ good: networkLatency < 50, warn: networkLatency >= 100 }">{{ networkLatency }} ms</span>
                    </div>
                    <div class="env-progress">
                      <div class="env-progress-bar">
                        <div class="env-progress-fill" :style="{ width: networkQualityPercent + '%' }"></div>
                      </div>
                    </div>
                  </div>
                  <div class="env-item">
                    <div class="env-item-header">
                      <span class="env-icon">☀</span>
                      <span class="env-label">光线强度</span>
                      <span class="env-value" :class="lightLevelClass">{{ lightLevelText }}</span>
                    </div>
                    <div class="env-progress">
                      <div class="env-progress-bar">
                        <div class="env-progress-fill" :style="{ width: lightLevelPercent + '%' }"></div>
                      </div>
                    </div>
                  </div>
                </section>

                <!-- 专注状态卡片 -->
                <section class="left-card focus-card">
                  <div class="focus-header">专注状态</div>
                  <div class="focus-content">
                    <div class="focus-circle">
                      <svg class="focus-svg" viewBox="0 0 100 100">
                        <circle class="focus-track" cx="50" cy="50" r="42"></circle>
                        <circle class="focus-fill" cx="50" cy="50" r="42" stroke-dasharray="264" :stroke-dashoffset="focusStrokeOffset"></circle>
                      </svg>
                      <div class="focus-percent">{{ focusPercent }}%</div>
                    </div>
                    <div class="focus-text">
                      <div class="focus-subtitle">当前目光留存率</div>
                      <div class="focus-desc">{{ focusDesc }}</div>
                    </div>
                  </div>
                </section>
              </div>
            </aside>

            <section class="center-col">
              <div class="center-main-card">
                <div class="center-content">
                  <!-- AI 面试官头像区域 -->
                  <div class="ai-avatar-wrap ready">
                    <div class="ai-avatar ready-face">
                      <video
                        v-if="!avatarConnected"
                        class="ai-avatar-img interviewer-direct-video"
                        :src="listeningInterviewerVideo"
                        autoplay
                        muted
                        loop
                        playsinline
                        preload="auto"
                        aria-label="正在认真聆听的专业女性面试官"
                      ></video>
                      <video
                        ref="centerCameraVideoRef"
                        class="ai-avatar-img center-camera-preview"
                        autoplay
                        playsinline
                        muted
                        v-show="showCenterCameraPreview"
                      ></video>
                      <video
                        ref="avatarVideoRef"
                        class="avatar-video avatar-video-source"
                        :poster="femaleInterviewerImg"
                        autoplay
                        playsinline
                        @loadeddata="handleAvatarVideoLoaded"
                        v-show="avatarConnected"
                      ></video>
                      <canvas
                        ref="avatarCanvasRef"
                        class="avatar-video avatar-video-keyed"
                        v-show="avatarConnected"
                      ></canvas>
                    </div>
                  </div>

                  <!-- AI 名称和状态 -->
                  <div class="ai-meta">
                    <div class="ai-avatar-status">{{ interviewerStatusText }}</div>
                    <button
                      v-if="interviewerState === 'error' || interviewerState === 'unconfigured'"
                      class="interviewer-retry-btn"
                      type="button"
                      @click="reconnectInterviewer"
                    >重新连接</button>
                  </div>

                </div>
              </div>

              <div class="center-caption" :class="{ user: isCenterCaptionUser }">
                <span class="center-caption-label">{{ centerCaptionLabel }}：</span>
                <span>{{ centerCaptionText }}</span>
                <span v-if="sending" class="live-cursor" aria-hidden="true"></span>
              </div>

              <div class="center-chat-input">
                <input
                  v-model="newMessage"
                  :placeholder="chatInputPlaceholder"
                  @keydown.enter.exact.prevent="sendMessage"
                />
                <div class="chat-actions">
                  <button
                    class="chat-mic-btn"
                    @click="handleVoicePanelToggle"
                    :class="{ active: textInputMode }"
                    :title="micModeToggleTitle"
                  >
                    <svg v-if="!textInputMode" class="chat-mic-icon" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                      <path d="M12 15a4 4 0 0 0 4-4V7a4 4 0 1 0-8 0v4a4 4 0 0 0 4 4Zm-1 3.93A7 7 0 0 1 5 12h2a5 5 0 0 0 10 0h2a7 7 0 0 1-6 6.93V22h-2v-3.07Z" />
                    </svg>
                    <svg v-else class="chat-mic-icon" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                      <path d="M3 6a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2h-5v2h3v2H7v-2h3v-2H5a2 2 0 0 1-2-2V6Zm3 2h2v2H6V8Zm3 0h2v2H9V8Zm3 0h2v2h-2V8Zm3 0h2v2h-2V8ZM6 11h2v2H6v-2Zm3 0h8v2H9v-2Z" />
                    </svg>
                  </button>
                  <button class="btn primary" @click="sendMessage" :disabled="sending || !newMessage.trim()">
                    <span v-if="sending">发送中...</span><span v-else>发送</span>
                  </button>
                  <button class="btn end-btn" @click="handleEndAndReturn" :disabled="ending">
                    <img src="@/assets/interview/chat4.png" alt="结束面试" />
                    <span v-if="ending">结束中...</span><span v-else>结束面试</span>
                  </button>
                </div>
              </div>

              <!-- AI 实时引导提示 -->
              <div class="ai-tips-section">
                <div class="ai-tips-header">
                  <svg class="sparkle-icon" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12 2L14.4 9.6L22 12L14.4 14.4L12 22L9.6 14.4L2 12L9.6 9.6L12 2Z"/>
                  </svg>
                  <span>AI 实时引导提示</span>
                </div>
                <transition name="tip-fade" mode="out-in">
                  <div class="ai-tips-cards" :key="aiTipsPageKey">
                    <div v-for="(tip, idx) in visibleAiTips" :key="`${tip.id}-${idx}`" class="tip-card">
                    <div class="tip-card-icon" :class="{ orange: tip.tone === 'warn' }">
                      <svg viewBox="0 0 24 24" fill="currentColor">
                        <path v-if="tip.tone === 'warn'" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
                        <path v-else d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
                      </svg>
                    </div>
                    <div class="tip-card-content">
                      <div class="tip-card-title">{{ tip.title }}</div>
                      <div class="tip-card-text">{{ tip.text }}</div>
                    </div>
                    </div>
                  </div>
                </transition>
              </div>
            </section>

            <section class="right-col">
              <div class="transcript-card">
                <div class="transcript-header">
                  <div class="transcript-title">实时语音转译</div>
                </div>

                <div class="chat-messages" ref="chatListRef">
                  <div v-for="(m, idx) in messages" :key="idx" class="transcript-item" :class="m.role">
                    <div class="transcript-badge">{{ m.role === 'user' ? '我' : 'AI' }}</div>
                    <div class="transcript-content">
                      <div class="transcript-text">{{ m.content }}</div>
                      <div class="transcript-time">{{ m.ts || '--:--:--' }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </section>
          </div>
        </div>
      </div>
    </main>

    <InterviewDialog
      :visible="initStatus === 'initializing' || initStatus === 'error'"
      :title="initDialogTitle"
      :message="initDialogMessage"
      :busy="initStatus === 'initializing'"
      :dismissible="false"
      :primary-text="initStatus === 'error' ? initDialogPrimaryText : undefined"
      :secondary-text="initStatus === 'error' ? '返回模拟面试首页' : undefined"
      @primary="handleInitDialogPrimary"
      @secondary="returnToMockInterview"
    />
    <InterviewDialog
      :visible="noticeDialog.visible"
      :title="noticeDialog.title"
      :message="noticeDialog.message"
      primary-text="知道了"
      @primary="closeNoticeDialog"
      @close="closeNoticeDialog"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onBeforeUnmount, nextTick, onMounted, onActivated, onDeactivated, computed, watch } from 'vue'
import { useRouter } from 'vue-router';
import { conversationApi } from '../api/conversation';
import { authApi } from '../api/auth';
import { avatarApi, type AvatarActionInput } from '../api/avatar';
import {
  interviewerApi,
  resolveApiBase,
  redirectToLoginOnSessionExpired,
} from '../api/index';
import { PATHS } from '../routes/paths';
import { AvatarWebApiDriver } from '../services/avatarWebApiDriver';
import femaleInterviewerImg from '@/assets/interview/female-interviewer.png';
import listeningInterviewerVideo from '@/assets/interview/female-interviewer-final.mp4';
import InterviewDialog from '../components/InterviewDialog.vue';

const router = useRouter()
const CONFIG_STORAGE_KEY = 'interview_config_draft';
const noticeDialog = ref({ visible: false, title: '', message: '' });

function showNoticeDialog(title: string, message: string) {
  noticeDialog.value = { visible: true, title, message };
}

function closeNoticeDialog() {
  noticeDialog.value.visible = false;
}

const sessionToken = ref<string | null>(localStorage.getItem('token') || localStorage.getItem('session_token') || null);
const localUserId = Number(localStorage.getItem('user_id') || '0');
const userId = ref<number | null>(localUserId > 0 ? localUserId : null);
const jobRole = ref(localStorage.getItem('job_role') || '');
// 每次进入页面都强制新建会话，不恢复旧会话
const conversationId = ref<string | null>(null);
const startedAt = ref<string | null>(null);
const endedAt = ref<string | null>(null);
const status = ref<string | null>(null);

const config = ref({
  interview_mode: 'full' as 'full' | 'focused',
  interviewer_persona: 'neutral' as 'neutral' | 'friendly' | 'challenging' | 'pragmatic',
  focus_tags: '',
  requirements: '',
  difficulty: 3,
  remark: '',
  status: 1,
});

const ending = ref(false);
const sending = ref(false);

/**
 * 面试官（AI）连接状态机。
 * idle → connecting → ready → asking → waiting_answer → thinking → asking …，异常进入 error，
 * 没有可用模型时进入 unconfigured（页面明确提示“AI 面试官服务尚未配置”，不再无限 loading）。
 */
type InterviewerState =
  | 'idle'
  | 'connecting'
  | 'ready'
  | 'asking'
  | 'waiting_answer'
  | 'thinking'
  | 'error'
  | 'unconfigured';
const interviewerState = ref<InterviewerState>('idle');
const interviewerModelLabel = ref('');
const interviewerErrorText = ref('');
let interviewerProbeTask: Promise<InterviewerState> | null = null;

const interviewerStatusText = computed(() => {
  switch (interviewerState.value) {
    case 'connecting':
      return '正在连接 AI 面试官…';
    case 'ready':
      return interviewerModelLabel.value
        ? `AI 面试官已连接（${interviewerModelLabel.value}）`
        : 'AI 面试官已连接';
    case 'asking':
      return '面试官正在提问…';
    case 'thinking':
      return '面试官正在分析你的回答…';
    case 'waiting_answer':
      return '等待你的回答…';
    case 'error':
      return `AI 面试官连接失败${interviewerErrorText.value ? `：${interviewerErrorText.value}` : ''}`;
    case 'unconfigured':
      return 'AI 面试官服务尚未配置';
    default:
      return 'AI 面试官待连接';
  }
});

const isInterviewerTalking = computed(() =>
  ['ready', 'asking', 'thinking', 'waiting_answer'].includes(interviewerState.value),
);

function classifyInterviewerFailure(message: string): InterviewerState {
  const text = String(message || '');
  if (/尚未配置|未配置|没有可用|模型配置|模型未|请先配置|not configured|no model/i.test(text)) {
    return 'unconfigured';
  }
  return 'error';
}

/** 探测面试官是否真的可用（复用后端已配置且测试通过的文本模型列表）。 */
async function probeInterviewer(): Promise<InterviewerState> {
  if (interviewerProbeTask) return interviewerProbeTask;
  interviewerProbeTask = (async () => {
    interviewerState.value = 'connecting';
    interviewerErrorText.value = '';
    const controller = new AbortController();
    const timer = window.setTimeout(() => controller.abort(), 10000);
    try {
      const status = await interviewerApi.status(controller.signal);
      if (!status.configured) {
        interviewerModelLabel.value = '';
        interviewerState.value = 'unconfigured';
        return 'unconfigured' as InterviewerState;
      }
      interviewerModelLabel.value = String(status.model || '').trim();
      interviewerState.value = 'ready';
      return 'ready' as InterviewerState;
    } catch (e: any) {
      const aborted = e?.name === 'AbortError';
      interviewerErrorText.value = aborted ? '连接超时' : (e?.message || '无法访问 AI 面试官服务');
      interviewerState.value = 'error';
      console.warn('[AiChat] 面试官可用性探测失败:', e?.message || e);
      return 'error' as InterviewerState;
    } finally {
      window.clearTimeout(timer);
    }
  })();
  try {
    return await interviewerProbeTask;
  } finally {
    interviewerProbeTask = null;
  }
}

async function ensureInterviewerReady(): Promise<InterviewerState> {
  if (['ready', 'asking', 'thinking', 'waiting_answer'].includes(interviewerState.value)) return 'ready';
  return probeInterviewer();
}

const pendingConversationInit = ref(true);
type InitStatus = 'idle' | 'initializing' | 'success' | 'error';
type InitErrorKind = 'missing-position' | 'auth' | 'request';
const initStatus = ref<InitStatus>('idle');
const initErrorKind = ref<InitErrorKind>('request');
const initErrorMessage = ref('');
let initializationTask: Promise<boolean> | null = null;
type InitFlowState = 'idle' | 'loading_profile' | 'waiting_job' | 'initializing' | 'ready' | 'error';
const initFlowState = ref<InitFlowState>('idle');
let profileLoadTask: Promise<boolean> | null = null;

async function loadTargetPosition(): Promise<boolean> {
  if (profileLoadTask) return profileLoadTask;
  profileLoadTask = (async () => {
    initFlowState.value = 'loading_profile';
    try {
      const profile = await authApi.getCurrentProfile();
      const profileUserId = Number(profile?.id || 0);
      if (profileUserId > 0) {
        userId.value = profileUserId;
        localStorage.setItem('user_id', String(profileUserId));
      }
      const targetPosition = String(profile?.target_position || '').trim();
      if (!targetPosition) {
        jobRole.value = '';
        localStorage.removeItem('job_role');
        initFlowState.value = 'waiting_job';
        return false;
      }
      jobRole.value = targetPosition;
      localStorage.setItem('job_role', targetPosition);
      return true;
    } catch (error) {
      console.error('[AiChat] 读取目标岗位失败:', error);
      const cachedPosition = String(localStorage.getItem('job_role') || '').trim();
      if (cachedPosition) {
        jobRole.value = cachedPosition;
        return true;
      }
      initFlowState.value = 'error';
      throw error;
    }
  })();
  try {
    return await profileLoadTask;
  } finally {
    profileLoadTask = null;
  }
}
const initDialogTitle = computed(() => {
  if (initStatus.value === 'initializing') return '正在初始化模拟面试';
  if (initErrorKind.value === 'missing-position') return '尚未设置面试岗位';
  if (initErrorKind.value === 'auth') return '登录状态已失效';
  return '模拟面试初始化失败';
});
const initDialogMessage = computed(() => {
  if (initStatus.value === 'initializing') return '正在创建面试会话并加载面试环境，请稍候。';
  if (initErrorKind.value === 'missing-position') return '开始模拟面试前，需要先选择或设置目标岗位。';
  if (initErrorKind.value === 'auth') return '当前登录状态不可用，请重新登录后再开始模拟面试。';
  return initErrorMessage.value || '暂时无法创建面试会话，请重新尝试。';
});
const initDialogPrimaryText = computed(() => {
  if (initErrorKind.value === 'missing-position') return '去填写岗位';
  if (initErrorKind.value === 'auth') return '重新登录';
  return '重新尝试';
});
const timerText = ref('00:00:00');
const pageEnteredAtMs = Date.now();
const cameraEnabled = ref(false);
const cameraStarting = ref(false);
const cameraError = ref('');
const cameraVideoRef = ref<HTMLVideoElement | null>(null);
const centerCameraVideoRef = ref<HTMLVideoElement | null>(null);
let cameraStream: MediaStream | null = null;
const faceRecordConversationId = ref<string | null>(null);
let faceFrameUploadTimerId: number | null = null;
let faceFrameUploading = false;
const FACE_UPLOAD_FPS = 2;
let emotionPollTimerId: number | null = null;
const emotionLinePath = ref('');
const emotionAreaPath = ref('');
const emotionPositiveLinePath = ref('');
const emotionCurrentCode = ref('idle');
const emotionCurrentLabel = ref('等待数据');
const emotionCurrentTensionPercent = ref(0);
const emotionCurrentPositivityPercent = ref(0);
const micEnabled = ref(false);
const micError = ref('');
const micLevel = ref(0);
const textInputMode = ref(true);
const recordingAudio = ref(false);
const transcribingAudio = ref(false);
const voicePanelTip = ref('点击大麦克风按钮进行录音');
const spacePressed = ref(false);
let micStream: MediaStream | null = null;
let micAudioContext: AudioContext | null = null;
let micAnalyser: AnalyserNode | null = null;
let micDataArray: Uint8Array | null = null;
let micRafId: number | null = null;
let timerIntervalId: number | null = null;
let aiTipsRotateTimerId: number | null = null;
let voiceRecordStream: MediaStream | null = null;
let mediaRecorder: MediaRecorder | null = null;
let audioChunks: Blob[] = [];
let ttsAudioEl: HTMLAudioElement | null = null;
let ttsObjectUrl: string | null = null;
let activeChatRequestController: AbortController | null = null;
const avatarVideoRef = ref<HTMLVideoElement | null>(null);
const avatarCanvasRef = ref<HTMLCanvasElement | null>(null);
const avatarConnected = ref(false);
const avatarConnecting = ref(false);
const avatarError = ref('');
const avatarFrameMs = ref(40);
const avatarDriver = new AvatarWebApiDriver();
const avatarOpeningText = ref('');
const avatarOpeningAutoSpeak = ref(true);
const avatarRuntimeId = ref('');
const avatarOpeningActionEnabled = ref(false);
const openingSpokenConversationId = ref<string | null>(null);
let avatarChromaRafId: number | null = null;
let avatarSourceCanvas: HTMLCanvasElement | null = null;
let avatarSourceCtx: CanvasRenderingContext2D | null = null;

type ChatMsg = { role: 'user' | 'ai'; content: string; ts?: string };
type AiTip = { id: string; title: string; text: string; tone: 'ok' | 'warn' };
const messages = ref<ChatMsg[]>([]);
const newMessage = ref('');
const chatListRef = ref<HTMLElement | null>(null);
const latestUserTranscript = ref('');
const centerCaptionSpeaker = ref<'ai' | 'user'>('ai');
const aiTipsMockData: AiTip[] = [
  {
    id: 'tip-1',
    title: '建议重点描述',
    text: '检测到当前话题涉及“架构设计”，建议补充你在高并发场景下的拆分思路与结果指标。',
    tone: 'ok',
  },
  {
    id: 'tip-2',
    title: '语速提醒',
    text: '当前语速偏快（约 238 字/分），建议句尾稍停顿，给关键信息留出呼吸感。',
    tone: 'warn',
  },
  {
    id: 'tip-3',
    title: '结构优化',
    text: '可尝试按“背景-挑战-行动-结果”来回答，会更容易让面试官抓住重点。',
    tone: 'ok',
  },
  {
    id: 'tip-4',
    title: '细节补充',
    text: '建议补一句“你的个人贡献占比”，比如方案设计、性能优化或故障处理责任。',
    tone: 'ok',
  },
  {
    id: 'tip-5',
    title: '表达提醒',
    text: '连续重复“然后”较多，可用“接着”“最终”“因此”替换，表达会更专业。',
    tone: 'warn',
  },
  {
    id: 'tip-6',
    title: '亮点加分',
    text: '如果有线上事故复盘经验，建议简短提及一次止损与复盘闭环，会很加分。',
    tone: 'ok',
  },
];
const aiTipsPageIndex = ref(0);
const aiTipsRenderTick = ref(0);
const INITIAL_INTERVIEWER_GREETING = '您好，欢迎参加今天的面试。请你做一个简单的自我介绍，可以重点突出一下你过往经历中与本职位相关项目。我们开始吧。';
const OPENING_HELLO_ACTION_ID = 'A_U_hello_O';
const OPENING_HELLO_ACTION_AVATAR_ID = '111204004';
const OPENING_HELLO_TENCENT_VOICE_TYPE = 502001;
const TEXT_STREAM_DELAY_AFTER_TTS_MS = 1020;
const TEXT_STREAM_ESTIMATED_MS_PER_CHAR = 200;

const liveAiSpeech = computed(() => {
  if (recordingAudio.value) return '正在聆听您的回答...';
  for (let i = messages.value.length - 1; i >= 0; i -= 1) {
    const item = messages.value[i];
    if (item?.role === 'ai') {
      const text = String(item.content || '').trim();
      if (text) return text;
      return sending.value ? 'AI 正在实时发言中' : '等待 AI 发言';
    }
  }
  return sending.value ? 'AI 正在实时发言中' : '等待 AI 发言';
});

const centerCaptionText = computed(() => {
  if (!avatarConnected.value) {
    const userText = centerCaptionSpeaker.value === 'user'
      ? String(latestUserTranscript.value || '').trim()
      : '';
    if (userText) return userText;
    const aiText = String(liveAiSpeech.value || '').trim();
    return aiText && aiText !== '等待 AI 发言' ? aiText : interviewerStatusText.value;
  }
  if (centerCaptionSpeaker.value === 'user') {
    const userText = String(latestUserTranscript.value || '').trim();
    if (userText) return userText;
  }
  return liveAiSpeech.value;
});

const centerCaptionLabel = computed(() => {
  if (!avatarConnected.value) return isInterviewerTalking.value ? '面试官' : '系统';
  if (centerCaptionSpeaker.value === 'user' && String(latestUserTranscript.value || '').trim()) return '我';
  return '面试官';
});

const isCenterCaptionUser = computed(() => centerCaptionLabel.value === '我');
const showCenterCameraPreview = computed(() => cameraEnabled.value && !avatarConnected.value);

const chatInputPlaceholder = computed(() => {
  if (textInputMode.value) return `对${jobRole.value || '面试官'}说点什么...`;
  if (recordingAudio.value || transcribingAudio.value) return voicePanelTip.value;
  return recordingAudio.value ? '松开空格结束录音，随后自动转文字' : '按住空格开始语音输入，松开自动转文字';
});
const micModeToggleTitle = computed(() => (textInputMode.value ? '切换到语音输入' : '切换到文本输入'));
const visibleAiTips = computed(() => {
  const pageSize = 2;
  const total = aiTipsMockData.length;
  if (total === 0) return [];
  const pageCount = Math.max(1, Math.ceil(total / pageSize));
  const normalizedPageIndex = aiTipsPageIndex.value % pageCount;
  const start = normalizedPageIndex * pageSize;
  const pageTips = [...aiTipsMockData.slice(start, start + pageSize)];
  if (total > 1 && pageTips.length < pageSize) {
    const deficit = pageSize - pageTips.length;
    pageTips.push(...aiTipsMockData.slice(0, deficit));
  }
  return pageTips;
});
const aiTipsPageKey = computed(() => `${aiTipsPageIndex.value}-${aiTipsRenderTick.value}`);

// 面试概览计算属性
const answeredQuestionsCount = computed(() => {
  return messages.value.filter((m) => m.role === 'user').length;
});

const interviewProgressPercent = computed(() => {
  const count = answeredQuestionsCount.value;
  // 假设面试通常有 8-12 个问题，进度按回答数量计算
  const targetQuestions = 10;
  return Math.min(100, Math.round((count / targetQuestions) * 100));
});

const interviewModeLabel = computed(() => {
  const mode = config.value.interview_mode;
  if (mode === 'full') return '全面面试';
  if (mode === 'focused') return '专项面试';
  return '标准面试';
});

// 能力评估模拟数据（基于面试进度动态变化）
const abilityScores = computed(() => {
  const progress = interviewProgressPercent.value;
  const baseScores = [
    { name: '沟通表达', score: Math.min(95, 65 + progress * 0.3), level: 'high' },
    { name: '技术深度', score: Math.min(90, 55 + progress * 0.35), level: 'medium' },
    { name: '问题分析', score: Math.min(88, 60 + progress * 0.28), level: 'high' },
    { name: '应变能力', score: Math.min(85, 50 + progress * 0.35), level: 'medium' },
    { name: '逻辑思维', score: Math.min(92, 70 + progress * 0.22), level: 'high' },
  ];
  return baseScores.map((item) => ({
    ...item,
    score: Math.round(item.score),
    level: item.score >= 80 ? 'high' : item.score >= 60 ? 'medium' : 'low',
  }));
});

const overallAbilityScore = computed(() => {
  const scores = abilityScores.value;
  if (scores.length === 0) return '--';
  const avg = scores.reduce((sum, item) => sum + item.score, 0) / scores.length;
  return Math.round(avg);
});

// 环境监测动态数据
const networkLatency = ref(12);
const networkQualityPercent = computed(() => {
  // 延迟越低，质量百分比越高（0-50ms 为 100%，>200ms 为 0%）
  const latency = networkLatency.value;
  if (latency <= 20) return 95;
  if (latency <= 50) return 85;
  if (latency <= 100) return 70;
  if (latency <= 150) return 50;
  return Math.max(10, 100 - latency * 0.5);
});

const lightLevelPercent = ref(75);
const lightLevelText = computed(() => {
  const level = lightLevelPercent.value;
  if (level >= 80) return '优秀';
  if (level >= 60) return '良好';
  if (level >= 40) return '一般';
  return '偏暗';
});
const lightLevelClass = computed(() => {
  const level = lightLevelPercent.value;
  if (level >= 80) return 'excellent';
  if (level >= 60) return 'good';
  if (level >= 40) return 'normal';
  return 'poor';
});

// 专注状态动态数据
const focusPercent = ref(88);
const focusStrokeOffset = computed(() => {
  // 264 是圆周长，根据百分比计算偏移量
  const circumference = 264;
  return circumference - (focusPercent.value / 100) * circumference;
});
const focusDesc = computed(() => {
  const percent = focusPercent.value;
  if (percent >= 90) return '表现极其稳定';
  if (percent >= 75) return '注意力集中';
  if (percent >= 60) return '略有分心';
  return '请保持专注';
});

let envMonitorTimerId: number | null = null;

function startEnvMonitorSimulation() {
  if (envMonitorTimerId !== null) return;
  envMonitorTimerId = window.setInterval(() => {
    // 网络延迟在 8-25ms 之间波动
    networkLatency.value = Math.round(8 + Math.random() * 17);
    // 光线强度在 70-90% 之间波动
    lightLevelPercent.value = Math.round(70 + Math.random() * 20);
    // 专注度在 82-95% 之间波动
    focusPercent.value = Math.round(82 + Math.random() * 13);
  }, 2000);
}

function stopEnvMonitorSimulation() {
  if (envMonitorTimerId !== null) {
    window.clearInterval(envMonitorTimerId);
    envMonitorTimerId = null;
  }
}

// 清空对话处理函数
function handleClearChat() {
  if (messages.value.length === 0) return;
  messages.value = [];
  latestUserTranscript.value = '';
  centerCaptionSpeaker.value = 'ai';
  ensureInitialInterviewerGreeting();
  scrollToBottom();
}

function rotateAiTips() {
  const pageSize = 2;
  const pageCount = Math.max(1, Math.ceil(aiTipsMockData.length / pageSize));
  if (pageCount <= 1) return;
  aiTipsPageIndex.value = (aiTipsPageIndex.value + 1) % pageCount;
  aiTipsRenderTick.value += 1;
}

function startAiTipsRotation() {
  stopAiTipsRotation();
  aiTipsRotateTimerId = window.setInterval(rotateAiTips, 3600);
}

function stopAiTipsRotation() {
  if (aiTipsRotateTimerId !== null) {
    window.clearInterval(aiTipsRotateTimerId);
    aiTipsRotateTimerId = null;
  }
}

function resetAiTipsRotation() {
  aiTipsPageIndex.value = 0;
  aiTipsRenderTick.value += 1;
}

function resolveOpeningGreetingText(): string {
  const text = String(avatarOpeningText.value || '').trim();
  return text || INITIAL_INTERVIEWER_GREETING;
}

function ensureInitialInterviewerGreeting() {
  // 面试官服务不可用时不做任何“假装已连接”的发言
  if (interviewerState.value !== 'ready') return;
  const hasAiSpeech = messages.value.some((m) => m.role === 'ai' && String(m.content || '').trim().length > 0);
  if (hasAiSpeech) return;
  messages.value.push({
    role: 'ai',
    content: resolveOpeningGreetingText(),
    ts: new Date().toLocaleTimeString(),
  });
}

async function requestInitialQuestion() {
  if (!conversationId.value) throw new Error('面试会话尚未创建');
  interviewerState.value = 'asking';
  const authToken = localStorage.getItem('session_token') || localStorage.getItem('token') || '';
  const response = await fetch(`${resolveApiBase()}/langgraph/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(authToken ? { Authorization: `Bearer ${authToken}`, 'X-Session-Token': authToken } : {}),
    },
    body: JSON.stringify({
      conversation_id: conversationId.value,
      initial_question: true,
      job_role: jobRole.value,
    }),
  });
  if (!response.ok || !response.body) {
    const text = await response.text().catch(() => '');
    throw new Error(text || `HTTP ${response.status}`);
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';
  let answer = '';
  let finished = false;
  while (!finished) {
    const chunk = await reader.read();
    finished = !!chunk.done;
    if (chunk.value) buffer += decoder.decode(chunk.value, { stream: !finished });
    const events = buffer.split(/\r?\n\r?\n/);
    buffer = events.pop() || '';
    for (const event of events) {
      const data = event.split(/\r?\n/)
        .filter((line) => line.startsWith('data:'))
        .map((line) => line.slice(5).trim())
        .join('\n');
      if (!data || data === '[DONE]') continue;
      let payload: any;
      try {
        payload = JSON.parse(data);
      } catch {
        continue;
      }
      if (payload?.error) throw new Error(String(payload.error));
      if (payload?.content) answer += String(payload.content);
    }
  }
  if (!answer.trim()) throw new Error('面试官未返回第一题');
  messages.value.push({ role: 'ai', content: answer.trim(), ts: new Date().toLocaleTimeString() });
  interviewerState.value = 'waiting_answer';
  centerCaptionSpeaker.value = 'ai';
  await scrollToBottom();
}

async function reconnectInterviewer() {
  const readiness = await probeInterviewer();
  if (readiness !== 'ready') return;
  const hasQuestion = messages.value.some((item) => item.role === 'ai' && item.content.trim());
  if (!hasQuestion && conversationId.value) {
    try {
      await requestInitialQuestion();
    } catch (error: any) {
      interviewerState.value = classifyInterviewerFailure(error?.message || '');
      interviewerErrorText.value = error?.message || '连接失败';
    }
  }
}

async function scrollToBottom() {
  await nextTick();
  const el = chatListRef.value as any;
  if (el) el.scrollTop = el.scrollHeight;
}

async function ensureAvatarConnected() {
  if (avatarDriver.isConnected()) {
    avatarConnected.value = true;
    avatarError.value = '';
    if (avatarVideoRef.value?.readyState && avatarVideoRef.value.readyState >= 2) {
      startAvatarChromaKey();
    }
    return;
  }
  if (avatarConnecting.value) return;
  if (!avatarVideoRef.value) return;

  avatarConnecting.value = true;
  avatarError.value = '';
  try {
    const runtime = await avatarApi.getRuntimeConfig();
    avatarRuntimeId.value = String(runtime?.avatar?.avatar_id || '').trim();
    avatarOpeningActionEnabled.value = avatarRuntimeId.value === OPENING_HELLO_ACTION_AVATAR_ID;
    console.info('[AiChat] avatar runtime', {
      avatar_id: avatarRuntimeId.value,
      opening_action_enabled: avatarOpeningActionEnabled.value,
    });
    avatarOpeningText.value = String(runtime?.opening?.text || '').trim();
    avatarOpeningAutoSpeak.value = runtime?.opening?.auto_speak !== false;
    const runtimeFrame = Number(runtime?.audio?.default_frame_ms || 40);
    avatarFrameMs.value = [20, 40, 60, 80, 100].includes(runtimeFrame) ? runtimeFrame : 40;
    const preferredProtocol = runtime?.stream?.protocol === 'flv' ? 'flv' : 'webrtc';
    const allowFallback = runtime?.stream?.allow_fallback !== false;
    avatarDriver.attachVideo(avatarVideoRef.value);
    await avatarDriver.startWithFallback(conversationId.value, preferredProtocol, allowFallback);
    avatarConnected.value = true;
    if (avatarVideoRef.value.readyState >= 2) {
      startAvatarChromaKey();
    }
  } catch (e: any) {
    avatarConnected.value = false;
    avatarOpeningActionEnabled.value = false;
    stopAvatarChromaKey();
    const attempts = e?.payload?.attempts;
    if (Array.isArray(attempts) && attempts.length > 0) {
      avatarError.value = attempts
        .map((a: any) => `${a.protocol || '-'}: ${a.message || a.code || 'start_failed'}`)
        .join(' | ');
    } else {
      avatarError.value = e?.payload?.message || e?.message || '初始化失败';
    }
    console.error('[AiChat] 数字人连接失败:', e);
  } finally {
    avatarConnecting.value = false;
  }
}

async function playOpeningGreetingByAvatarOnce() {
  const cid = String(conversationId.value || '').trim();
  if (!cid) return;
  if (openingSpokenConversationId.value === cid) return;
  if (avatarOpeningAutoSpeak.value === false) return;

  const greetingText = resolveOpeningGreetingText();
  if (!greetingText) return;

  try {
    const openingVoiceType = avatarOpeningActionEnabled.value ? OPENING_HELLO_TENCENT_VOICE_TYPE : undefined;
    const tts = await conversationApi.ttsSynthesize(greetingText, {
      codec: 'pcm',
      sample_rate: 16000,
      voice_type: openingVoiceType,
    });
    const filename = String(tts?.filename || '').trim();
    if (!filename) return;
    const openingActions = avatarOpeningActionEnabled.value
      ? [
          {
            type: 'action',
            value: OPENING_HELLO_ACTION_ID,
            tb: 0,
          },
        ]
      : undefined;
    await driveAvatarByTtsFile(
      filename,
      tts?.audio_base64 ? String(tts.audio_base64) : undefined,
      tts?.codec ? String(tts.codec) : undefined,
      {
        strictLipSync: true,
        actions: openingActions,
      },
    );
    openingSpokenConversationId.value = cid;
  } catch (e) {
    console.warn('[AiChat] 开场欢迎语数字人播报失败:', e);
  }
}

function rgbToHsv(r: number, g: number, b: number): { h: number; s: number; v: number } {
  const rn = r / 255;
  const gn = g / 255;
  const bn = b / 255;
  const max = Math.max(rn, gn, bn);
  const min = Math.min(rn, gn, bn);
  const delta = max - min;

  let h = 0;
  if (delta > 0) {
    if (max === rn) {
      h = ((gn - bn) / delta) % 6;
    } else if (max === gn) {
      h = (bn - rn) / delta + 2;
    } else {
      h = (rn - gn) / delta + 4;
    }
    h *= 60;
    if (h < 0) h += 360;
  }

  const s = max === 0 ? 0 : delta / max;
  const v = max;
  return { h, s, v };
}

function stopAvatarChromaKey() {
  if (avatarChromaRafId !== null) {
    window.cancelAnimationFrame(avatarChromaRafId);
    avatarChromaRafId = null;
  }
  const canvas = avatarCanvasRef.value;
  if (canvas) {
    const ctx = canvas.getContext('2d');
    if (ctx) {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
    }
  }
}

function ensureAvatarSourceContext(width: number, height: number): CanvasRenderingContext2D | null {
  if (!avatarSourceCanvas) {
    avatarSourceCanvas = document.createElement('canvas');
  }
  if (avatarSourceCanvas.width !== width || avatarSourceCanvas.height !== height) {
    avatarSourceCanvas.width = width;
    avatarSourceCanvas.height = height;
    avatarSourceCtx = null;
  }
  if (!avatarSourceCtx) {
    avatarSourceCtx = avatarSourceCanvas.getContext('2d', { willReadFrequently: true });
  }
  return avatarSourceCtx;
}

function runAvatarChromaFrame() {
  const video = avatarVideoRef.value;
  const canvas = avatarCanvasRef.value;
  if (!video || !canvas || !avatarConnected.value) {
    stopAvatarChromaKey();
    return;
  }
  if (video.readyState < 2 || video.videoWidth <= 0 || video.videoHeight <= 0) {
    avatarChromaRafId = window.requestAnimationFrame(runAvatarChromaFrame);
    return;
  }

  const width = video.videoWidth;
  const height = video.videoHeight;
  if (canvas.width !== width || canvas.height !== height) {
    canvas.width = width;
    canvas.height = height;
  }

  const sourceCtx = ensureAvatarSourceContext(width, height);
  const targetCtx = canvas.getContext('2d');
  if (!sourceCtx || !targetCtx) {
    avatarChromaRafId = window.requestAnimationFrame(runAvatarChromaFrame);
    return;
  }

  sourceCtx.drawImage(video, 0, 0, width, height);
  const frame = sourceCtx.getImageData(0, 0, width, height);
  const data = frame.data;

  for (let i = 0; i < data.length; i += 4) {
    const r = data[i] ?? 0;
    const g = data[i + 1] ?? 0;
    const b = data[i + 2] ?? 0;
    const hsv = rgbToHsv(r, g, b);

    const inGreenHue = hsv.h >= 72 && hsv.h <= 170;
    const neutralBackdrop =
      r >= 42 &&
      r <= 92 &&
      g >= 48 &&
      g <= 98 &&
      b >= 42 &&
      b <= 92 &&
      Math.max(r, g, b) - Math.min(r, g, b) <= 24;
    if ((!inGreenHue || hsv.s < 0.16 || hsv.v < 0.18) && !neutralBackdrop) continue;

    const hueWeight = inGreenHue ? 1 - Math.min(1, Math.abs(hsv.h - 118) / 52) : 0.55;
    const satWeight = Math.min(1, Math.max(0, (hsv.s - 0.12) / 0.5));
    const valWeight = Math.min(1, Math.max(0, (hsv.v - 0.18) / 0.45));
    const neutralWeight = neutralBackdrop ? 0.82 : 0;
    const keyStrength = Math.max(neutralWeight, hueWeight * 0.56 + satWeight * 0.28 + valWeight * 0.16);

    const alpha = Math.round(255 * (1 - keyStrength));
    data[i + 3] = Math.min(data[i + 3] ?? 255, Math.max(0, alpha));

    if ((data[i + 3] ?? 0) > 0 && keyStrength > 0.35 && g > r && g > b) {
      const spill = Math.min(0.34, keyStrength * 0.38);
      data[i + 1] = Math.round(g * (1 - spill));
    }
  }

  targetCtx.clearRect(0, 0, width, height);
  targetCtx.putImageData(frame, 0, 0);
  avatarChromaRafId = window.requestAnimationFrame(runAvatarChromaFrame);
}

function startAvatarChromaKey() {
  stopAvatarChromaKey();
  if (!avatarConnected.value || !avatarVideoRef.value || !avatarCanvasRef.value) return;
  avatarChromaRafId = window.requestAnimationFrame(runAvatarChromaFrame);
}

function handleAvatarVideoLoaded() {
  if (!avatarConnected.value) return;
  startAvatarChromaKey();
}

function stopTtsPlayback() {
  if (ttsAudioEl) {
    try {
      ttsAudioEl.pause();
      ttsAudioEl.src = '';
    } catch {
      // ignore
    }
    ttsAudioEl = null;
  }
  if (ttsObjectUrl) {
    URL.revokeObjectURL(ttsObjectUrl);
    ttsObjectUrl = null;
  }
}

async function playTtsBase64AudioFallback(audioBase64: string, codec?: string) {
  const b64 = (audioBase64 || '').trim();
  if (!b64) return;

  stopTtsPlayback();

  const raw = atob(b64);
  const bytes = new Uint8Array(raw.length);
  for (let i = 0; i < raw.length; i += 1) {
    bytes[i] = raw.charCodeAt(i);
  }

  const normalized = String(codec || 'mp3').toLowerCase();
  const mime = normalized === 'wav' ? 'audio/wav' : normalized === 'pcm' ? 'audio/L16' : 'audio/mpeg';
  const blob = new Blob([bytes], { type: mime });
  ttsObjectUrl = URL.createObjectURL(blob);
  ttsAudioEl = new Audio(ttsObjectUrl);
  await ttsAudioEl.play();
}

async function driveAvatarByTtsFile(
  ttsFilename: string,
  audioBase64?: string,
  codec?: string,
  options?: { strictLipSync?: boolean; actions?: AvatarActionInput[] },
) {
  const strictLipSync = options?.strictLipSync !== false;
  const actions = options?.actions;
  const filename = (ttsFilename || '').trim();
  if (!filename) {
    if (audioBase64) {
      await playTtsBase64AudioFallback(audioBase64, codec);
    }
    return;
  }

  await ensureAvatarConnected();
  if (!avatarDriver.isConnected()) {
    if (audioBase64) {
      await playTtsBase64AudioFallback(audioBase64, codec);
    }
    return;
  }

  const frameMs = avatarFrameMs.value || 40;
  try {
    await avatarDriver.speak(filename, frameMs, actions);
  } catch (e) {
    if (avatarDriver.getProtocol() === 'webrtc') {
      try {
        await avatarDriver.stop();
        avatarConnected.value = false;
        await avatarDriver.startAndPlay(conversationId.value, 'flv');
        avatarConnected.value = true;
        await avatarDriver.speak(filename, frameMs, actions);
        return;
      } catch {
        // ignore fallback failure and use local audio playback fallback below
      }
    }
    if (!strictLipSync && audioBase64) {
      await playTtsBase64AudioFallback(audioBase64, codec);
      return;
    }
    avatarError.value = '数字人播报失败，请重试';
    throw e;
  }
}

function markInterviewEndedLocally(endedIso?: string) {
  const finalEndedAt = endedIso || endedAt.value || new Date().toISOString();
  status.value = 'ended';
  endedAt.value = finalEndedAt;
  pendingConversationInit.value = true;
  stopTimer();
  updateTimerText();
  localStorage.setItem('conversation_status', 'ended');
  localStorage.setItem('conversation_ended_at', finalEndedAt);
  localStorage.removeItem('conversation_id');
  conversationId.value = null;
}

function formatDuration(totalSeconds: number) {
  const safe = Math.max(0, totalSeconds);
  const h = Math.floor(safe / 3600);
  const m = Math.floor((safe % 3600) / 60);
  const s = safe % 60;
  const hh = String(h).padStart(2, '0');
  const mm = String(m).padStart(2, '0');
  const ss = String(s).padStart(2, '0');
  return `${hh}:${mm}:${ss}`;
}

function resolveBaseStartMs() {
  if (startedAt.value) {
    const parsed = Date.parse(startedAt.value);
    if (Number.isFinite(parsed)) return parsed;
  }
  return pageEnteredAtMs;
}

function updateTimerText() {
  const startMs = resolveBaseStartMs();
  let endMs = Date.now();
  if (status.value !== 'running' && endedAt.value) {
    const endedMs = Date.parse(endedAt.value);
    if (Number.isFinite(endedMs)) {
      endMs = endedMs;
    }
  }
  const seconds = Math.floor((endMs - startMs) / 1000);
  timerText.value = formatDuration(seconds);
}

function startTimer() {
  updateTimerText();
  if (timerIntervalId !== null) return;
  timerIntervalId = window.setInterval(updateTimerText, 1000);
}

function stopTimer() {
  if (timerIntervalId !== null) {
    window.clearInterval(timerIntervalId);
    timerIntervalId = null;
  }
}

async function ensureConversationInitialized() {
  if (!pendingConversationInit.value && conversationId.value) return;

  const difficulty = Math.min(5, Math.max(1, Number(config.value.difficulty || 3)));
  config.value.difficulty = difficulty;
  const configPayload = {
    target_position: jobRole.value.trim(),
    interview_mode: config.value.interview_mode,
    interviewer_persona: config.value.interviewer_persona,
    focus_tags: config.value.focus_tags?.trim() || null,
    requirements: config.value.requirements?.trim() || null,
    difficulty,
    remark: config.value.remark?.trim() || null,
    status: 1,
  };

  let createdAt: string | undefined;
  if (!conversationId.value) {
    // 不传 user_id，让后端统一用 Authorization 中的登录用户创建会话。
    const res = await conversationApi.newId({
      job_role: jobRole.value.trim(),
    });
    if (!res?.conversation_id) {
      throw new Error('创建面试会话失败：接口未返回会话编号');
    }
    conversationId.value = res.conversation_id;
    createdAt = res.created_at;
  }

  const startRes = await conversationApi.start({
    conversation_id: conversationId.value,
    config: configPayload,
  });
  if (!startRes || !startRes.status) {
    throw new Error('启动面试会话失败：接口响应不完整');
  }

  startedAt.value = startRes.started_at || createdAt || new Date().toISOString();
  endedAt.value = null;
  status.value = 'running';
  pendingConversationInit.value = false;

  localStorage.setItem('conversation_id', conversationId.value);
  localStorage.setItem('conversation_status', status.value || '');
  localStorage.setItem('conversation_started_at', startedAt.value || '');
  localStorage.setItem('conversation_ended_at', endedAt.value || '');
}

async function handleEnd(options?: { silent?: boolean }): Promise<boolean> {
  const silent = options?.silent ?? false;
  if (!conversationId.value) {
    if (!silent) showNoticeDialog('无法结束面试', '当前没有可结束的面试会话。');
    return false;
  }
  ending.value = true;
  try {
    const endedIso = new Date().toISOString();
    const res = await conversationApi.commit({
      conversation_id: conversationId.value,
      user_id: userId.value || 0,
      job_role: jobRole.value.trim(),
      started_at: startedAt.value || endedIso,
      ended_at: endedIso,
    });
    status.value = res.status || null;
    startedAt.value = res.started_at || null;
    endedAt.value = res.ended_at || null;
    localStorage.setItem('conversation_status', status.value || '');
    localStorage.setItem('conversation_started_at', startedAt.value || '');
    localStorage.setItem('conversation_ended_at', endedAt.value || '');
    markInterviewEndedLocally(endedAt.value || endedIso);
    if (!silent) showNoticeDialog('面试已结束', '本次面试记录已保存。');
    return true;
  } catch (e: any) {
    console.error('[AiChat] 结束面试失败:', e);
    if (!silent) showNoticeDialog('结束面试失败', e?.message || '请稍后重试。');
    return false;
  } finally {
    ending.value = false;
  }
}

async function handleEndAndReturn() {
  if (ending.value) return;
  const convIdForReport = String(conversationId.value || '').trim();
  if (activeChatRequestController) {
    activeChatRequestController.abort();
    activeChatRequestController = null;
  }
  sending.value = false;
  stopTtsPlayback();
  stopAvatarChromaKey();
  await avatarDriver.destroy().catch((e) => {
    console.warn('[AiChat] 数字人销毁失败:', e);
  });
  avatarConnected.value = false;
  // 如果有录制中的面部记录，先结束
  if (faceRecordConversationId.value) {
    try {
      await conversationApi.faceRecordEnd({ conversation_id: faceRecordConversationId.value });
    } catch {
      // ignore
    }
    faceRecordConversationId.value = null;
  }
  // 无论是否发过消息，只要会话在进行中就提交，确保历史记录存在
  if (status.value === 'running' && conversationId.value) {
    const ok = await handleEnd({ silent: true });
    if (!ok) {
      console.warn('[AiChat] 结束提交失败，已执行本地结束兜底。');
    }
  }
  markInterviewEndedLocally();
  if (convIdForReport) {
    router.push(`${PATHS.EVALUATION_REPORT}?conversation_id=${encodeURIComponent(convIdForReport)}`);
  } else {
    router.push(PATHS.AI_MOCK_INTERVIEW)
  }
}

async function sendMessage() {
  const text = newMessage.value.trim();
  if (!text) return;
  // 面试官未就绪时先做一次真实探测；确认无可用模型则明确提示，不伪造回答
  const readiness = await ensureInterviewerReady();
  if (readiness === 'unconfigured') {
    showNoticeDialog(
      'AI 面试官服务尚未配置',
      '当前没有可用的面试官模型，请先在系统配置中维护文本模型（ai.service.text.*，包含 provider / base-url / api-key / model 并通过连通性测试）后再开始面试。',
    );
    return;
  }
  if (readiness === 'error') {
    showNoticeDialog('AI 面试官连接失败', interviewerErrorText.value || '无法访问 AI 面试官服务，请稍后重试。');
    return;
  }
  latestUserTranscript.value = text;
  centerCaptionSpeaker.value = 'user';
  stopTtsPlayback();
  if (avatarDriver.isConnected()) {
    await avatarDriver.interrupt().catch((e) => {
      console.warn('[AiChat] 数字人打断失败，继续发送文本:', e);
    });
  }
  messages.value.push({ role: 'user', content: text, ts: new Date().toLocaleTimeString() });
  newMessage.value = '';
  sending.value = true;
  interviewerState.value = 'thinking';
  try {
    await ensureConversationInitialized();
    messages.value.push({ role: 'ai', content: '', ts: new Date().toLocaleTimeString() });
    const ai = messages.value[messages.value.length - 1];
    if (!ai) return;
    let allowAiTextStreaming = !avatarConnected.value;
    let fullAiText = '';
    let shownAiTextLen = 0;
    let audioDrivenStartAtMs = 0;
    let audioDrivenDurationMs = 0;
    let textSyncTimerId: number | null = null;

    const renderAiTextTo = async (targetLen: number) => {
      const safeTarget = Math.max(0, Math.min(fullAiText.length, targetLen));
      if (safeTarget <= shownAiTextLen) return;
      shownAiTextLen = safeTarget;
      centerCaptionSpeaker.value = 'ai';
      ai.content = fullAiText.slice(0, shownAiTextLen);
      await scrollToBottom();
    };

    const syncAiTextByAudioProgress = async () => {
      if (!allowAiTextStreaming) return;
      if (!fullAiText.length) return;

      if (audioDrivenStartAtMs <= 0 || audioDrivenDurationMs <= 0) {
        await renderAiTextTo(fullAiText.length);
        return;
      }

      const elapsed = Math.max(0, Date.now() - audioDrivenStartAtMs);
      const progress = Math.max(0, Math.min(1, elapsed / audioDrivenDurationMs));
      const targetLen = Math.floor(progress * fullAiText.length);
      await renderAiTextTo(targetLen);
    };

    const ensureAudioTextSyncLoop = () => {
      if (textSyncTimerId !== null) return;
      textSyncTimerId = window.setInterval(() => {
        void syncAiTextByAudioProgress();
      }, 30);
    };
    await scrollToBottom();

    // 与项目其它接口保持一致：开发环境直连 8080，生产环境走同源 /api
    const url = `${resolveApiBase()}/langgraph/chat`;
    const authToken = localStorage.getItem('session_token') || localStorage.getItem('token') || '';
    if (activeChatRequestController) {
      activeChatRequestController.abort();
      activeChatRequestController = null;
    }
    const requestController = new AbortController();
    activeChatRequestController = requestController;
    const resp = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(authToken ? { Authorization: `Bearer ${authToken}`, 'X-Session-Token': authToken } : {}),
      },
      signal: requestController.signal,
      body: JSON.stringify({
        conversation_id: conversationId.value,
        content: text,
        job_role: jobRole.value || undefined,
      }),
    });

    if (!resp.ok || !resp.body) {
      const errText = await resp.text().catch(() => '');
      let payload: any = null;
      try {
        payload = errText ? JSON.parse(errText) : null;
      } catch {
        payload = null;
      }
      if (redirectToLoginOnSessionExpired(resp.status, payload)) {
        return;
      }
      throw new Error(errText || `HTTP ${resp.status}`);
    }

    const reader = resp.body.getReader();
    const decoder = new TextDecoder('utf-8');
    let buffer = '';
    let done = false;

    while (!done) {
      const chunk = await reader.read();
      done = !!chunk.done;
      if (chunk.value) {
        buffer += decoder.decode(chunk.value, { stream: !done });
      }

      let sep = buffer.indexOf('\n\n');
      while (sep !== -1) {
        const rawEvent = buffer.slice(0, sep).trim();
        buffer = buffer.slice(sep + 2);

        if (rawEvent) {
          const dataLines = rawEvent
            .split('\n')
            .filter((l) => l.startsWith('data:'))
            .map((l) => l.slice(5).trim());
          const dataText = dataLines.join('\n');

          if (dataText === '[DONE]') {
            done = true;
            break;
          }

          if (dataText) {
            let payload: any = null;
            try {
              payload = JSON.parse(dataText);
            } catch {
              payload = null;
            }
            if (payload?.error) {
              throw new Error(String(payload.error));
            }
            if (payload) {
              if (interviewerState.value === 'thinking') interviewerState.value = 'asking';
              if (payload?.type === 'tts') {
                // 以“触发播报”为起点，不等待整段播完，避免文字被阻塞到音频结束
                void driveAvatarByTtsFile(
                  payload?.filename ? String(payload.filename) : '',
                  payload?.audio_base64 ? String(payload.audio_base64) : undefined,
                  payload?.codec ? String(payload.codec) : undefined,
                  { strictLipSync: true },
                ).catch((e) => {
                  console.warn('[AiChat] 数字人播报触发失败:', e);
                });
                const payloadDurationMs = Number(payload?.duration_ms || payload?.audio_duration_ms || 0);
                const estimatedByAudio = payload?.audio_base64
                  ? await estimateAudioDurationMsFromBase64(String(payload.audio_base64)).catch(() => 0)
                  : 0;
                audioDrivenDurationMs = Math.max(
                  1,
                  Math.round(
                    payloadDurationMs > 0
                      ? payloadDurationMs
                      : estimatedByAudio > 0
                        ? estimatedByAudio
                        : Math.max(1400, fullAiText.length * TEXT_STREAM_ESTIMATED_MS_PER_CHAR),
                  ),
                );
                audioDrivenStartAtMs = Date.now() + TEXT_STREAM_DELAY_AFTER_TTS_MS;
                await new Promise((resolve) => window.setTimeout(resolve, TEXT_STREAM_DELAY_AFTER_TTS_MS));
                allowAiTextStreaming = true;
                ensureAudioTextSyncLoop();
              } else if (payload?.content) {
                fullAiText += String(payload.content);
                if (allowAiTextStreaming) {
                  ensureAudioTextSyncLoop();
                  await syncAiTextByAudioProgress();
                }
              }
            } else {
              fullAiText += dataText;
              if (allowAiTextStreaming) {
                ensureAudioTextSyncLoop();
                await syncAiTextByAudioProgress();
              }
            }
            await scrollToBottom();
          }
        }

        sep = buffer.indexOf('\n\n');
      }
    }

    // 收尾策略：
    // 1) 有音频进度锚点时，等待到预计播放结束再补全，避免“一段直接显示”
    // 2) 无音频锚点时，直接补全兜底
    if (allowAiTextStreaming && audioDrivenStartAtMs > 0 && audioDrivenDurationMs > 0) {
      const finishAtMs = audioDrivenStartAtMs + audioDrivenDurationMs;
      while (Date.now() < finishAtMs && shownAiTextLen < fullAiText.length) {
        await syncAiTextByAudioProgress();
        await new Promise((resolve) => window.setTimeout(resolve, 24));
      }
    }
    await renderAiTextTo(fullAiText.length);
    if (textSyncTimerId !== null) {
      window.clearInterval(textSyncTimerId);
      textSyncTimerId = null;
    }

    if (!ai.content) {
      // 面试官没有返回任何内容：如实反馈，不伪造面试官发言
      messages.value.splice(messages.value.indexOf(ai), 1);
      interviewerState.value = 'error';
      interviewerErrorText.value = '面试官未返回内容';
      showNoticeDialog('AI 面试官连接失败', '面试官没有返回内容，请检查 AI 服务与模型配置后重试。');
      return;
    }
    interviewerState.value = 'waiting_answer';
  } catch (e: any) {
    if (e?.name === 'AbortError') return;
    console.error('[AiChat] 发送失败:', e);
    const placeholder = messages.value[messages.value.length - 1];
    if (placeholder?.role === 'ai' && !String(placeholder.content || '').trim()) {
      messages.value.pop();
    }
    const failure = classifyInterviewerFailure(e?.message || '');
    interviewerState.value = failure;
    interviewerErrorText.value = e?.message || '请求失败';
    if (failure === 'unconfigured') {
      showNoticeDialog(
        'AI 面试官服务尚未配置',
        '当前没有可用的面试官模型，请先在系统配置中维护文本模型（ai.service.text.*）后再开始面试。',
      );
    } else {
      showNoticeDialog('AI 面试官连接失败', e?.message || '无法访问 AI 面试官服务，请稍后重试。');
    }
  } finally {
    activeChatRequestController = null;
    sending.value = false;
  }
}

async function estimateAudioDurationMsFromBase64(audioBase64: string): Promise<number> {
  const b64 = String(audioBase64 || '').trim();
  if (!b64) return 0;
  try {
    const raw = atob(b64);
    const bytes = new Uint8Array(raw.length);
    for (let i = 0; i < raw.length; i += 1) {
      bytes[i] = raw.charCodeAt(i);
    }
    const ctx = new AudioContext();
    try {
      const audioBuffer = await ctx.decodeAudioData(bytes.buffer.slice(0));
      return Math.max(0, Math.round(audioBuffer.duration * 1000));
    } finally {
      await ctx.close().catch(() => {});
    }
  } catch {
    return 0;
  }
}

function clamp01(v: number): number {
  if (v < 0) return 0;
  if (v > 1) return 1;
  return v;
}

function mapEmotionCodeToText(code: string): string {
  const mapping: Record<string, string> = {
    stable: '稳定',
    tense: '紧张',
    positive: '积极',
    high_variation: '波动较大',
    low_mood_pattern: '状态低落',
    no_face: '未检测到人脸',
    idle: '等待数据',
  };
  return mapping[code] || '稳定';
}

function rebuildEmotionPaths(tensionSeries: number[], positivitySeries: number[]) {
  const width = 240;
  const height = 72;
  if (!tensionSeries.length && !positivitySeries.length) {
    emotionLinePath.value = '';
    emotionAreaPath.value = '';
    emotionPositiveLinePath.value = '';
    return;
  }

  const buildPath = (series: number[]) => {
    const pts = series.map((v, idx) => {
      const x = series.length === 1 ? width : (idx / (series.length - 1)) * width;
      const y = height - clamp01(v) * height;
      return { x, y };
    });
    const line = pts.map((p, idx) => `${idx === 0 ? 'M' : 'L'} ${p.x.toFixed(2)} ${p.y.toFixed(2)}`).join(' ');
    const area = `${line} L ${pts[pts.length - 1].x.toFixed(2)} ${height} L ${pts[0].x.toFixed(2)} ${height} Z`;
    return { line, area };
  };

  if (tensionSeries.length) {
    const tensionPath = buildPath(tensionSeries);
    emotionLinePath.value = tensionPath.line;
    emotionAreaPath.value = tensionPath.area;
  } else {
    emotionLinePath.value = '';
    emotionAreaPath.value = '';
  }

  if (positivitySeries.length) {
    emotionPositiveLinePath.value = buildPath(positivitySeries).line;
  } else {
    emotionPositiveLinePath.value = '';
  }
}

async function pullEmotionSummary() {
  if (!cameraEnabled.value || !faceRecordConversationId.value) return;

  try {
    const response = await conversationApi.faceRecordEmotionSummary({
      conversation_id: faceRecordConversationId.value,
      smooth_window: 3,
    });
    const summary = (response as any)?.data && (response as any)?.data?.per_second ? (response as any).data : response as any;
    const perSecond = Array.isArray(summary?.per_second) ? summary.per_second : [];

    if (!perSecond.length) {
      emotionCurrentCode.value = 'idle';
      emotionCurrentLabel.value = '等待数据';
      emotionCurrentTensionPercent.value = 0;
      emotionCurrentPositivityPercent.value = 0;
      rebuildEmotionPaths([], []);
      return;
    }

    const latest = perSecond[perSecond.length - 1] || {};
    const latestCode = String(latest?.emotion || 'stable');
    const latestTension = Number(latest?.avg_tension_score || 0);
    const latestPositivity = Number(latest?.avg_positivity_score || 0);

    emotionCurrentCode.value = latestCode;
    emotionCurrentLabel.value = mapEmotionCodeToText(latestCode);
    emotionCurrentTensionPercent.value = Math.round(clamp01(latestTension) * 100);
    emotionCurrentPositivityPercent.value = Math.round(clamp01(latestPositivity) * 100);

    const recentTensionSeries = perSecond
      .slice(-16)
      .map((x: any) => Number(x?.avg_tension_score || 0));
    const recentPositivitySeries = perSecond
      .slice(-16)
      .map((x: any) => Number(x?.avg_positivity_score || 0));
    rebuildEmotionPaths(recentTensionSeries, recentPositivitySeries);
  } catch (e) {
    console.warn('[AiChat] 获取情绪总结失败:', e);
  }
}

function startEmotionPolling() {
  if (emotionPollTimerId !== null) return;
  void pullEmotionSummary();
  emotionPollTimerId = window.setInterval(() => {
    void pullEmotionSummary();
  }, 1000);
}

function stopEmotionPolling() {
  if (emotionPollTimerId !== null) {
    window.clearInterval(emotionPollTimerId);
    emotionPollTimerId = null;
  }
  emotionCurrentCode.value = 'idle';
  emotionCurrentLabel.value = '等待数据';
  emotionCurrentTensionPercent.value = 0;
  emotionCurrentPositivityPercent.value = 0;
  rebuildEmotionPaths([], []);
}

function getCameraErrorMessage(error: any): string {
  const name = String(error?.name || '');
  if (name === 'NotAllowedError' || name === 'PermissionDeniedError') {
    return '摄像头权限被拒绝，请在浏览器地址栏允许摄像头权限。';
  }
  if (name === 'NotFoundError' || name === 'DevicesNotFoundError') {
    return '未检测到可用摄像头，请检查设备连接。';
  }
  if (name === 'NotReadableError' || name === 'TrackStartError') {
    return '摄像头正在被其他应用占用，请关闭后重试。';
  }
  if (!navigator.mediaDevices?.getUserMedia) {
    return '当前浏览器不支持摄像头访问，请使用新版 Chrome 或 Edge。';
  }
  return '摄像头开启失败，请检查浏览器权限或设备状态。';
}

async function handleCameraToggle() {
  if (cameraStarting.value) return;
  if (cameraEnabled.value) {
    // 先结束录制
    if (faceRecordConversationId.value) {
      try {
        await conversationApi.faceRecordEnd({ conversation_id: faceRecordConversationId.value });
        console.log('[AiChat] 面部录制已结束');
      } catch (e) {
        console.error('[AiChat] 结束录制失败:', e);
      }
      faceRecordConversationId.value = null;
    }
    stopCamera();
    return;
  }

  cameraError.value = '';
  cameraStarting.value = true;
  try {
    cameraStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false });
    const bindStream = async (videoEl: HTMLVideoElement | null) => {
      if (!videoEl) return;
      videoEl.srcObject = cameraStream;
      await videoEl.play().catch(() => {
        // muted + playsinline normally autoplays; ignore if browser starts it itself.
      });
    };
    await Promise.all([bindStream(cameraVideoRef.value), bindStream(centerCameraVideoRef.value)]);
    cameraEnabled.value = true;
  } catch (e: any) {
    console.error('[AiChat] 开启摄像头失败:', e);
    cameraError.value = getCameraErrorMessage(e);
    cameraEnabled.value = false;
    stopCamera();
    cameraStarting.value = false;
    return;
  }

  try {
    // 摄像头预览优先；后端会话和面部录制失败时不影响本地摄像头打开。
    await ensureConversationInitialized();

    const recordRes = await conversationApi.faceRecordStart({
      conversation_id: conversationId.value || undefined,
      auto_capture: false,
      detect_fps: FACE_UPLOAD_FPS,
    });
    const recordConversationId = (recordRes as any)?.conversation_id || (recordRes as any)?.data?.conversation_id;
    if (!recordConversationId) {
      throw new Error('未拿到 conversation_id');
    }
    faceRecordConversationId.value = String(recordConversationId);
    console.log('[AiChat] 面部录制已开始, conversation_id:', faceRecordConversationId.value);
    startFaceFrameUpload();
    startEmotionPolling();
  } catch (e) {
    console.warn('[AiChat] 面部录制初始化失败，摄像头预览继续可用:', e);
    cameraError.value = '';
    if (faceRecordConversationId.value) {
      try {
        await conversationApi.faceRecordEnd({ conversation_id: faceRecordConversationId.value });
      } catch {
        // ignore
      }
      faceRecordConversationId.value = null;
    }
  } finally {
    cameraStarting.value = false;
  }
}

function stopCamera() {
  // 停止截图上传
  stopFaceFrameUpload();
  stopEmotionPolling();
  if (cameraStream) {
    cameraStream.getTracks().forEach((t) => t.stop());
    cameraStream = null;
  }
  if (cameraVideoRef.value) {
    cameraVideoRef.value.srcObject = null;
  }
  if (centerCameraVideoRef.value) {
    centerCameraVideoRef.value.srcObject = null;
  }
  cameraEnabled.value = false;
}

watch([cameraEnabled, avatarConnected, centerCameraVideoRef], async () => {
  if (!cameraEnabled.value || avatarConnected.value) return;
  if (!cameraStream || !centerCameraVideoRef.value) return;
  centerCameraVideoRef.value.srcObject = cameraStream;
  await centerCameraVideoRef.value.play().catch(() => {});
});

function captureCameraFrameDataUrl(video: HTMLVideoElement): string | null {
  if (video.videoWidth <= 0 || video.videoHeight <= 0) return null;

  const canvas = document.createElement('canvas');
  const targetWidth = 360;
  const targetHeight = Math.max(1, Math.round((video.videoHeight / video.videoWidth) * targetWidth));
  canvas.width = targetWidth;
  canvas.height = targetHeight;

  const ctx = canvas.getContext('2d');
  if (!ctx) return null;
  ctx.drawImage(video, 0, 0, targetWidth, targetHeight);

  return canvas.toDataURL('image/jpeg', 0.75);
}

function startFaceFrameUpload() {
  if (faceFrameUploadTimerId !== null) return;

  const intervalMs = Math.max(200, Math.round(1000 / FACE_UPLOAD_FPS));
  faceFrameUploadTimerId = window.setInterval(async () => {
    if (faceFrameUploading) return;
    if (!cameraEnabled.value || !faceRecordConversationId.value) return;

    const video = cameraVideoRef.value;
    if (!video || video.readyState < 2) return;

    const frameImage = captureCameraFrameDataUrl(video);
    if (!frameImage) return;

    faceFrameUploading = true;
    try {
      await conversationApi.faceRecordAppend({
        conversation_id: faceRecordConversationId.value,
        frame_image: frameImage,
        capture_time: new Date().toISOString(),
      });
    } catch (e) {
      console.error('[AiChat] 上传截图失败:', e);
    } finally {
      faceFrameUploading = false;
    }
  }, intervalMs);
}

function stopFaceFrameUpload() {
  if (faceFrameUploadTimerId !== null) {
    window.clearInterval(faceFrameUploadTimerId);
    faceFrameUploadTimerId = null;
  }
  faceFrameUploading = false;
}

function startMicMonitor() {
  if (!micAnalyser || !micDataArray) return;

  const update = () => {
    if (!micAnalyser || !micDataArray || !micEnabled.value) return;
    micAnalyser.getByteTimeDomainData(micDataArray);
    let sum = 0;
    for (let i = 0; i < micDataArray.length; i += 1) {
      const v = (micDataArray[i] - 128) / 128;
      sum += v * v;
    }
    const rms = Math.sqrt(sum / micDataArray.length);
    micLevel.value = Math.min(100, Math.max(0, Math.round(rms * 300)));
    micRafId = window.requestAnimationFrame(update);
  };

  micRafId = window.requestAnimationFrame(update);
}

function stopMicrophone() {
  if (micRafId !== null) {
    window.cancelAnimationFrame(micRafId);
    micRafId = null;
  }

  if (micStream) {
    micStream.getTracks().forEach((t) => t.stop());
    micStream = null;
  }

  if (micAudioContext) {
    micAudioContext.close().catch(() => {});
    micAudioContext = null;
  }

  micAnalyser = null;
  micDataArray = null;
  micLevel.value = 0;
  micEnabled.value = false;
}

function pickRecorderMimeType(): { mimeType: string; ext: 'mp3' | 'm4a' | 'wav' } | null {
  const candidates: Array<{ mimeType: string; ext: 'mp3' | 'm4a' | 'wav' }> = [
    { mimeType: 'audio/mpeg', ext: 'mp3' },
    { mimeType: 'audio/mp3', ext: 'mp3' },
    { mimeType: 'audio/mp4;codecs=mp4a.40.2', ext: 'm4a' },
    { mimeType: 'audio/mp4', ext: 'm4a' },
    { mimeType: 'audio/aac', ext: 'm4a' },
    { mimeType: 'audio/wav', ext: 'wav' },
  ];

  for (const item of candidates) {
    if (MediaRecorder.isTypeSupported(item.mimeType)) {
      return item;
    }
  }
  return null;
}

async function startVoiceRecording() {
  if (recordingAudio.value || transcribingAudio.value) return;
  voicePanelTip.value = '录音中...再次点击可停止并上传';
  try {
    const recorderPreset = pickRecorderMimeType();
    if (!recorderPreset) {
      voicePanelTip.value = '当前浏览器不支持 mp3/m4a/wav 录音，请更换浏览器后重试';
      return;
    }

    voiceRecordStream = await navigator.mediaDevices.getUserMedia({ audio: true, video: false });
    mediaRecorder = new MediaRecorder(voiceRecordStream, { mimeType: recorderPreset.mimeType });
    audioChunks = [];

    mediaRecorder.ondataavailable = (ev: BlobEvent) => {
      if (ev.data && ev.data.size > 0) {
        audioChunks.push(ev.data);
      }
    };

    mediaRecorder.onstop = async () => {
      const blobType = mediaRecorder?.mimeType || recorderPreset.mimeType;
      const audioBlob = new Blob(audioChunks, { type: blobType });
      audioChunks = [];
      if (audioBlob.size <= 0) {
        voicePanelTip.value = '未捕获到有效音频，请重试';
        cleanupVoiceRecorder();
        return;
      }

      transcribingAudio.value = true;
      voicePanelTip.value = '正在上传录音文件...';
      try {
        const uploadExt = recorderPreset.ext;

        const uploaded = await conversationApi.uploadAudio(audioBlob, `recording.${uploadExt}`, uploadExt);
        const savedFileName = uploaded?.saved_file?.filename || '';
        if (!savedFileName) {
          voicePanelTip.value = '上传成功，但未拿到文件名';
          return;
        }

        voicePanelTip.value = `上传成功，开始转写：${savedFileName}`;
        const asrRes = await conversationApi.transcribeByFilename(savedFileName, uploadExt);
        const text = String(asrRes?.text || '').trim();
        if (text) {
          latestUserTranscript.value = text;
          centerCaptionSpeaker.value = 'user';
          newMessage.value = text;
          voicePanelTip.value = '转写完成，正在自动发送...';
          await sendMessage();
          voicePanelTip.value = '已自动发送，可继续按住空格录音';
        } else {
          voicePanelTip.value = '转写完成，但未识别到文本';
        }
      } catch (e: any) {
        console.error('[AiChat] 录音转写流程失败:', e);
        voicePanelTip.value = e?.message || '录音处理失败，请重试';
      } finally {
        transcribingAudio.value = false;
        cleanupVoiceRecorder();
      }
    };

    mediaRecorder.start();
    recordingAudio.value = true;
  } catch (e: any) {
    console.error('[AiChat] 开始录音失败:', e);
    voicePanelTip.value = e?.message || '无法获取麦克风权限';
    cleanupVoiceRecorder();
  }
}

function stopVoiceRecording() {
  if (!mediaRecorder || mediaRecorder.state === 'inactive') {
    cleanupVoiceRecorder();
    return;
  }
  recordingAudio.value = false;
  mediaRecorder.stop();
}

function cleanupVoiceRecorder() {
  if (voiceRecordStream) {
    voiceRecordStream.getTracks().forEach((t) => t.stop());
    voiceRecordStream = null;
  }
  mediaRecorder = null;
  recordingAudio.value = false;
}

function handleVoicePanelToggle() {
  textInputMode.value = !textInputMode.value;
  if (textInputMode.value) {
    if (recordingAudio.value) stopVoiceRecording();
    if (micEnabled.value) stopMicrophone();
  }
}

function handleSpaceHoldKeyDown(event: KeyboardEvent) {
  if (event.code !== 'Space') return;
  if (textInputMode.value) return;
  if (event.repeat) {
    event.preventDefault();
    return;
  }
  if (transcribingAudio.value || ending.value) {
    event.preventDefault();
    return;
  }
  event.preventDefault();
  spacePressed.value = true;
  if (!recordingAudio.value) {
    void startVoiceRecording().finally(() => {
      if (!spacePressed.value && recordingAudio.value) {
        stopVoiceRecording();
      }
    });
  }
}

function handleSpaceHoldKeyUp(event: KeyboardEvent) {
  if (event.code !== 'Space') return;
  if (textInputMode.value) return;
  event.preventDefault();
  spacePressed.value = false;
  if (recordingAudio.value) stopVoiceRecording();
}

async function initializeFreshInterviewSession(): Promise<boolean> {
  if (initializationTask) return initializationTask;

  initializationTask = (async () => {
    if (!sessionToken.value) {
      initFlowState.value = 'error';
      initErrorKind.value = 'auth';
      initErrorMessage.value = '';
      initStatus.value = 'error';
      return false;
    }
    if (!jobRole.value.trim()) {
      initFlowState.value = 'waiting_job';
      initErrorKind.value = 'missing-position';
      initErrorMessage.value = '';
      initStatus.value = 'error';
      return false;
    }

    initStatus.value = 'initializing';
    initFlowState.value = 'initializing';
    initErrorMessage.value = '';
    messages.value = [];
    latestUserTranscript.value = '';
    centerCaptionSpeaker.value = 'ai';
    conversationId.value = null;
    startedAt.value = null;
    endedAt.value = null;
    status.value = null;
    pendingConversationInit.value = true;
    localStorage.removeItem('conversation_id');
    localStorage.removeItem('conversation_status');
    localStorage.removeItem('conversation_started_at');
    localStorage.removeItem('conversation_ended_at');

    try {
      await ensureConversationInitialized();
      initStatus.value = 'success';
      initFlowState.value = 'ready';
    } catch (e: any) {
      console.error('[AiChat] 初始化会话失败:', e?.message || e);
      const statusCode = Number(e?.status || 0);
      const errorCode = String(e?.code || e?.payload?.error || e?.payload?.msg || '');
      const errorField = String(e?.payload?.field || '');
      const missingPosition = errorCode === 'missing_or_empty_field'
        && ['target_position', 'job_role', 'positionName'].includes(errorField);
      initErrorKind.value = statusCode === 401 ? 'auth' : missingPosition ? 'missing-position' : 'request';
      initErrorMessage.value = statusCode === 401 || missingPosition
        ? ''
        : '初始化过程中出现异常，请重新尝试。';
      initStatus.value = 'error';
      initFlowState.value = missingPosition ? 'waiting_job' : 'error';
      return false;
    }

    // 本阶段只初始化文字面试官；数字人、摄像头和语音不参与文字会话链路。
    const interviewerReadiness = await ensureInterviewerReady();
    if (interviewerReadiness === 'unconfigured') {
      showNoticeDialog(
        'AI 面试官服务尚未配置',
        '当前没有可用的面试官模型。请在系统配置中维护文本模型（ai.service.text.*，含 provider / base-url / api-key / model 并通过连通性测试）后再开始面试。',
      );
    } else if (interviewerReadiness === 'error') {
      showNoticeDialog(
        'AI 面试官连接失败',
        interviewerErrorText.value || '无法访问 AI 面试官服务，请检查 AI 服务是否已启动后重试。',
      );
    } else {
      try {
        await requestInitialQuestion();
      } catch (error: any) {
        const failure = classifyInterviewerFailure(error?.message || '');
        interviewerState.value = failure;
        interviewerErrorText.value = error?.message || '第一题生成失败';
        showNoticeDialog(
          failure === 'unconfigured' ? 'AI 面试官服务尚未配置' : 'AI 面试官连接失败',
          failure === 'unconfigured'
            ? '后端尚未配置可用的 DeepSeek API Key。'
            : '第一题生成失败，请使用“重新连接”再次尝试。',
        );
      }
    }
    await scrollToBottom();
    return true;
  })();

  try {
    return await initializationTask;
  } finally {
    initializationTask = null;
  }
}

async function prepareAndInitializeInterview(): Promise<boolean> {
  sessionToken.value = localStorage.getItem('token') || localStorage.getItem('session_token') || null;
  if (!sessionToken.value) {
    initErrorKind.value = 'auth';
    initErrorMessage.value = '';
    initStatus.value = 'error';
    initFlowState.value = 'error';
    return false;
  }
  try {
    const hasPosition = await loadTargetPosition();
    if (!hasPosition) {
      initErrorKind.value = 'missing-position';
      initErrorMessage.value = '';
      initStatus.value = 'error';
      return false;
    }
  } catch (error: any) {
    const statusCode = Number(error?.response?.status || error?.status || 0);
    initErrorKind.value = statusCode === 401 ? 'auth' : 'request';
    initErrorMessage.value = statusCode === 401
      ? ''
      : '暂时无法读取目标岗位，请重新尝试。';
    initStatus.value = 'error';
    return false;
  }
  return initializeFreshInterviewSession();
}

onMounted(async () => {
  startTimer();
  resetAiTipsRotation();
  startAiTipsRotation();
  startEnvMonitorSimulation();

  const raw = localStorage.getItem(CONFIG_STORAGE_KEY);
  if (raw) {
    try {
      const parsed = JSON.parse(raw);
      config.value = {
        ...config.value,
        ...parsed,
        difficulty: Math.min(5, Math.max(1, Number(parsed?.difficulty || 3))),
      };
    } catch {
      // ignore invalid draft
    }
  }

  await prepareAndInitializeInterview();
});

let activatedOnce = false;
onActivated(() => {
  if (!activatedOnce) {
    activatedOnce = true;
    window.addEventListener('keydown', handleSpaceHoldKeyDown);
    window.addEventListener('keyup', handleSpaceHoldKeyUp);
    return;
  }
  window.addEventListener('keydown', handleSpaceHoldKeyDown);
  window.addEventListener('keyup', handleSpaceHoldKeyUp);
  resetAiTipsRotation();
  startAiTipsRotation();
  if (initStatus.value !== 'success' || !conversationId.value) {
    void prepareAndInitializeInterview();
  }
});

function returnToMockInterview() {
  router.push(PATHS.AI_MOCK_INTERVIEW);
}

function handleInitDialogPrimary() {
  if (initErrorKind.value === 'missing-position') {
    router.push({
      path: PATHS.MY,
      query: {
        edit: 'target-position',
        returnTo: PATHS.AI_MOCK_INTERVIEW,
      },
    });
    return;
  }
  if (initErrorKind.value === 'auth') {
    router.push(PATHS.LOGIN);
    return;
  }
  void prepareAndInitializeInterview();
}

onDeactivated(() => {
  window.removeEventListener('keydown', handleSpaceHoldKeyDown);
  window.removeEventListener('keyup', handleSpaceHoldKeyUp);
  spacePressed.value = false;
  stopAiTipsRotation();
  stopAvatarChromaKey();
  avatarDriver.destroy().catch((e) => {
    console.warn('[AiChat] 页面切出时数字人销毁失败:', e);
  });
  avatarConnected.value = false;
});

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleSpaceHoldKeyDown);
  window.removeEventListener('keyup', handleSpaceHoldKeyUp);
  stopAiTipsRotation();
  stopEnvMonitorSimulation();
  if (activeChatRequestController) {
    activeChatRequestController.abort();
    activeChatRequestController = null;
  }
  stopTimer();
  stopTtsPlayback();
  stopAvatarChromaKey();
  // 如果摄像头开启中，先结束录制
  if (cameraEnabled.value && faceRecordConversationId.value) {
    conversationApi.faceRecordEnd({ conversation_id: faceRecordConversationId.value }).catch(console.error);
    faceRecordConversationId.value = null;
  }
  stopCamera();
  stopEmotionPolling();
  stopMicrophone();
  if (recordingAudio.value) {
    stopVoiceRecording();
  }
  cleanupVoiceRecorder();
  avatarDriver.destroy().catch((e) => {
    console.warn('[AiChat] 组件卸载时数字人销毁失败:', e);
  });
  avatarConnected.value = false;
  if (status.value === 'running' && conversationId.value) {
    handleEnd({ silent: true }).catch(console.error);
  }
});
</script>

<style scoped>
.page-layout {
  --bg-page: #070c14;
  --bg-card: rgba(18, 28, 45, 0.65);
  --bg-card-soft: rgba(22, 38, 58, 0.5);
  --border-color: rgba(60, 100, 140, 0.25);
  --text-primary: #e8eef9;
  --text-secondary: #8fa2bc;
  --accent-primary: #00d47e;
  --accent-orange: #ff6b3d;

  display: flex;
  height: 100vh;
  overflow: hidden;
  background: var(--bg-page);
}

.main-content {
  flex: 1;
  margin-left: 0;
  overflow: hidden;
  height: 100vh;
  background:
    radial-gradient(ellipse 120% 80% at 50% 0%, rgba(16, 52, 94, 0.36) 0%, transparent 58%),
    radial-gradient(ellipse 130% 95% at 50% 115%, rgba(4, 20, 44, 0.55) 0%, transparent 68%),
    var(--bg-page);
}

.page-container {
  padding: 0;
  max-width: none;
  margin: 0;
  min-height: 100vh;
  box-sizing: border-box;
}

.cockpit {
  border: none;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  overflow: hidden;
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.cockpit-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  border-bottom: 1px solid var(--border-color);
  background: rgba(10, 18, 30, 0.6);
  backdrop-filter: blur(12px);
}

.brand-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.brand-title {
  color: var(--text-primary);
  font-size: 24px;
  font-weight: 700;
}

.brand-sub {
  color: #6f839e;
  font-size: 12px;
  margin-top: 6px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.timer-pill {
  color: var(--accent-primary);
  background: rgba(0, 212, 126, 0.1);
  border: 1px solid rgba(0, 212, 126, 0.25);
  border-radius: 999px;
  font-size: 11px;
  padding: 6px 12px;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.icon-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid #31435f;
  background: #18253a;
  color: #b9c8dc;
  cursor: pointer;
}

/* 整体容器外间距：12px（上下左右内边距），gap: 12px（中间与左右容器间距） */
.cockpit-grid {
  display: grid;
  grid-template-columns: minmax(260px, 20%) 1fr minmax(300px, 22%);
  gap: 16px;
  padding: 16px;
  height: calc(100vh - 56px);
  box-sizing: border-box;
  min-height: 0;
}

.left-col,
.center-col,
.right-col {
  min-height: 0;
  height: 100%;
}

.left-col {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.left-cards-wrapper {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
  flex-shrink: 0;
  padding: 12px 0 0;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(74, 130, 186, 0.3) transparent;
}

.left-cards-wrapper::-webkit-scrollbar {
  width: 4px;
}

.left-cards-wrapper::-webkit-scrollbar-track {
  background: transparent;
}

.left-cards-wrapper::-webkit-scrollbar-thumb {
  background: rgba(74, 130, 186, 0.3);
  border-radius: 999px;
}

.left-cards-wrapper .face-card {
  flex: 0 0 auto;
  min-height: 180px;
}

.left-cards-wrapper .env-card {
  flex: 0 0 auto;
}

.left-cards-wrapper .focus-card {
  flex: 0 0 auto;
}

.left-cards-wrapper .overview-card {
  flex: 0 0 auto;
}

.left-cards-wrapper .ability-card {
  flex: 0 0 auto;
}

.left-cards-wrapper .actions-card {
  flex: 0 0 auto;
}

.left-cards-wrapper .shortcuts-card {
  flex: 0 0 auto;
}

.left-card {
  background:
    linear-gradient(180deg, rgba(22, 39, 62, 0.78), rgba(13, 25, 42, 0.86));
  border: 1px solid rgba(99, 151, 218, 0.18);
  border-radius: 18px;
  padding: 14px;
  box-shadow: 0 18px 44px rgba(0, 0, 0, 0.22), inset 0 1px 0 rgba(190, 220, 255, 0.04);
}

.face-card {
  padding: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background:
    linear-gradient(180deg, rgba(22, 39, 62, 0.78), rgba(13, 25, 42, 0.86));
}

.face-preview {
  flex: 1;
  height: auto;
  min-height: 160px;
  background:
    radial-gradient(circle at 50% 36%, rgba(89, 151, 255, 0.16), transparent 36%),
    linear-gradient(165deg, rgba(34, 59, 87, 0.64) 0%, rgba(10, 24, 39, 0.88) 100%);
  position: relative;
  overflow: hidden;
  border-radius: 18px;
}

.camera-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transform: scaleX(-1);
  transform-origin: center;
}

.camera-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
}

.camera-logo {
  width: 112px;
  height: 112px;
  border-radius: 24px;
  display: grid;
  place-items: center;
  font-size: 40px;
  color: #c8d9ec;
  background: rgba(18, 44, 68, 0.38);
  border: 1px solid rgba(128, 182, 255, 0.24);
  cursor: pointer;
  padding: 0;
  box-shadow: 0 16px 36px rgba(0, 0, 0, 0.22), inset 0 1px 0 rgba(255, 255, 255, 0.05);
  transition: background 0.2s ease, border-color 0.2s ease, transform 0.15s ease, box-shadow 0.2s ease;
}

.camera-logo:hover {
  background: rgba(32, 67, 102, 0.62);
  border-color: rgba(117, 178, 255, 0.56);
  transform: translateY(-1px) scale(1.02);
  box-shadow: 0 18px 42px rgba(37, 113, 217, 0.18), inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.camera-logo:active {
  transform: scale(0.98);
}

.camera-logo:focus {
  outline: none;
  border-color: rgba(0, 212, 126, 0.5);
  box-shadow: 0 0 0 2px rgba(0, 212, 126, 0.15);
}

.camera-switch-btn {
  border: 1px solid rgba(129, 179, 246, 0.36);
  background: rgba(18, 39, 64, 0.9);
  color: #d7e6f7;
  border-radius: 999px;
  font-size: 12px;
  padding: 7px 12px;
  cursor: pointer;
}

.camera-switch-btn:hover {
  background: rgba(34, 60, 88, 0.98);
}

.camera-switch-btn {
  position: absolute;
  top: 8px;
  right: 8px;
}

.camera-error {
  color: #ffd3d3;
  font-size: 11px;
  text-align: center;
  line-height: 1.5;
  max-width: 82%;
  max-height: 48px;
  padding: 0 10px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.camera-wait-tip {
  margin-top: 8px;
  padding: 6px 10px;
  border-radius: 10px;
  font-size: 12px;
  color: #9fdefe;
  background: rgba(9, 29, 49, 0.62);
  border: 1px solid rgba(118, 181, 240, 0.35);
}

.face-tag {
  position: absolute;
  left: 10px;
  bottom: 10px;
  font-size: 11px;
  color: #d7e7fb;
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(0, 0, 0, 0.4);
  padding: 4px 10px;
  border-radius: 999px;
}

.rec-dot {
  width: 6px;
  height: 6px;
  background: #ff4444;
  border-radius: 50%;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.face-metrics {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-top: 1px solid rgba(99, 151, 218, 0.16);
  background:
    linear-gradient(90deg, rgba(10, 23, 39, 0.92), rgba(16, 34, 56, 0.86));
}

.metric-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
  flex: 1;
  font-size: 12px;
}

.metric-label {
  color: rgba(180, 200, 220, 0.8);
}

.metric-value {
  font-weight: 600;
  white-space: nowrap;
}

.metric-divider {
  width: 1px;
  height: 22px;
  background: rgba(99, 151, 218, 0.16);
}

.metric-value.emotion {
  color: #00d47e;
}

.metric-value.heart-rate {
  color: #ff6b6b;
}

/* 环境监测卡片 */
.env-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  background:
    linear-gradient(180deg, rgba(22, 39, 62, 0.78), rgba(13, 25, 42, 0.86));
  overflow: hidden;
}

.env-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 2px;
}

.env-title {
  color: rgba(230, 245, 255, 0.9);
  font-size: 13px;
  font-weight: 600;
}

.env-status-icon {
  width: 20px;
  height: 20px;
  background: linear-gradient(135deg, #18d99a, #37a7ff);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #fff;
  box-shadow: 0 0 18px rgba(24, 217, 154, 0.25);
}

.env-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px 0;
}

.env-item-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.env-icon {
  font-size: 14px;
  opacity: 0.8;
}

.env-label {
  flex: 1;
  color: rgba(180, 200, 220, 0.9);
  font-size: 12px;
}

.env-value {
  font-size: 12px;
  font-weight: 600;
  color: rgba(200, 220, 240, 0.9);
}

.env-value.good {
  color: #00d47e;
}

.env-value.warn {
  color: #ff6b3d;
}

.env-value.excellent {
  color: #00d47e;
}

.env-value.normal {
  color: #4a89ff;
}

.env-value.poor {
  color: #ff9a4a;
}

.env-progress {
  padding-left: 22px;
}

.env-progress-bar {
  height: 5px;
  background: rgba(43, 71, 106, 0.6);
  border-radius: 999px;
  overflow: hidden;
}

.env-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #28d99c, #4a89ff);
  border-radius: inherit;
  box-shadow: 0 0 14px rgba(40, 217, 156, 0.35);
}

.focus-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  background:
    linear-gradient(180deg, rgba(22, 39, 62, 0.78), rgba(13, 25, 42, 0.86));
  overflow: hidden;
}

.focus-header {
  color: rgba(230, 245, 255, 0.9);
  font-size: 13px;
  font-weight: 600;
}

.focus-content {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-height: 0;
}

.focus-circle {
  position: relative;
  width: 70px;
  height: 70px;
  flex-shrink: 0;
}

.focus-svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.focus-track {
  fill: none;
  stroke: rgba(60, 80, 100, 0.5);
  stroke-width: 8;
  stroke-linecap: round;
}

.focus-fill {
  fill: none;
  stroke: #28d99c;
  stroke-width: 8;
  stroke-linecap: round;
  transition: stroke-dashoffset 0.5s ease;
  filter: drop-shadow(0 0 6px rgba(40, 217, 156, 0.35));
}

.focus-percent {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 16px;
  font-weight: 700;
  color: #28d99c;
}

.focus-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.focus-subtitle {
  color: rgba(160, 180, 200, 0.8);
  font-size: 11px;
}

.focus-desc {
  color: rgba(230, 245, 255, 0.95);
  font-size: 13px;
  font-weight: 600;
}

/* 面试概览卡片 */
.overview-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  background:
    linear-gradient(180deg, rgba(22, 39, 62, 0.78), rgba(13, 25, 42, 0.86));
  overflow: hidden;
}

.overview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.overview-title {
  color: rgba(230, 245, 255, 0.9);
  font-size: 13px;
  font-weight: 600;
}

.overview-badge {
  background: linear-gradient(135deg, rgba(0, 212, 126, 0.25), rgba(40, 217, 156, 0.15));
  border: 1px solid rgba(0, 212, 126, 0.35);
  color: #00d47e;
  font-size: 11px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 999px;
}

.overview-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.overview-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  background: rgba(15, 28, 48, 0.5);
  border-radius: 10px;
  border: 1px solid rgba(74, 130, 186, 0.15);
}

.overview-icon {
  font-size: 16px;
  opacity: 0.9;
}

.overview-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.overview-label {
  color: rgba(160, 180, 200, 0.7);
  font-size: 10px;
}

.overview-value {
  color: rgba(230, 245, 255, 0.95);
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.overview-value.highlight {
  color: #00d47e;
}

.overview-progress {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-top: 8px;
  border-top: 1px solid rgba(74, 130, 186, 0.15);
}

.overview-progress-bar {
  height: 6px;
  background: rgba(43, 71, 106, 0.6);
  border-radius: 999px;
  overflow: hidden;
}

.overview-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #00d47e, #28d99c);
  border-radius: inherit;
  box-shadow: 0 0 12px rgba(0, 212, 126, 0.35);
  transition: width 0.5s ease;
}

.overview-progress-label {
  color: rgba(160, 180, 200, 0.7);
  font-size: 10px;
}

/* 能力雷达卡片 */
.ability-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  background:
    linear-gradient(180deg, rgba(22, 39, 62, 0.78), rgba(13, 25, 42, 0.86));
  overflow: hidden;
}

.ability-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ability-title {
  color: rgba(230, 245, 255, 0.9);
  font-size: 13px;
  font-weight: 600;
}

.ability-live {
  color: #00d47e;
  font-size: 10px;
  padding: 2px 6px;
  background: rgba(0, 212, 126, 0.12);
  border-radius: 999px;
  border: 1px solid rgba(0, 212, 126, 0.25);
}

.ability-bars {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ability-bar-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ability-bar-label {
  color: rgba(180, 200, 220, 0.85);
  font-size: 11px;
  min-width: 60px;
}

.ability-bar-track {
  flex: 1;
  height: 6px;
  background: rgba(43, 71, 106, 0.5);
  border-radius: 999px;
  overflow: hidden;
}

.ability-bar-fill {
  height: 100%;
  border-radius: inherit;
  transition: width 0.5s ease;
}

.ability-bar-fill.high {
  background: linear-gradient(90deg, #00d47e, #28d99c);
  box-shadow: 0 0 8px rgba(0, 212, 126, 0.3);
}

.ability-bar-fill.medium {
  background: linear-gradient(90deg, #4a89ff, #6ba3ff);
  box-shadow: 0 0 8px rgba(74, 137, 255, 0.3);
}

.ability-bar-fill.low {
  background: linear-gradient(90deg, #ff6b3d, #ff8a5c);
  box-shadow: 0 0 8px rgba(255, 107, 61, 0.3);
}

.ability-bar-score {
  font-size: 11px;
  font-weight: 700;
  min-width: 28px;
  text-align: right;
}

.ability-bar-score.high {
  color: #00d47e;
}

.ability-bar-score.medium {
  color: #4a89ff;
}

.ability-bar-score.low {
  color: #ff6b3d;
}

.ability-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 8px;
  border-top: 1px solid rgba(74, 130, 186, 0.15);
}

.ability-summary-label {
  color: rgba(160, 180, 200, 0.7);
  font-size: 11px;
}

.ability-summary-value {
  color: #00d47e;
  font-size: 18px;
  font-weight: 700;
}

/* 快速操作卡片 */
.actions-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  background:
    linear-gradient(180deg, rgba(22, 39, 62, 0.78), rgba(13, 25, 42, 0.86));
  overflow: hidden;
}

.actions-header {
  color: rgba(230, 245, 255, 0.9);
  font-size: 13px;
  font-weight: 600;
}

.actions-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 8px;
  background: rgba(18, 34, 55, 0.72);
  border: 1px solid rgba(74, 130, 186, 0.2);
  border-radius: 12px;
  color: rgba(200, 220, 240, 0.9);
  font-size: 11px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-btn:hover {
  background: rgba(28, 50, 78, 0.85);
  border-color: rgba(99, 151, 218, 0.35);
  transform: translateY(-1px);
}

.action-btn:active {
  transform: scale(0.98);
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.action-btn.active {
  background: rgba(0, 212, 126, 0.15);
  border-color: rgba(0, 212, 126, 0.4);
  color: #00d47e;
}

.action-btn.primary {
  background: linear-gradient(135deg, rgba(0, 212, 126, 0.2), rgba(40, 217, 156, 0.15));
  border-color: rgba(0, 212, 126, 0.35);
  color: #00d47e;
}

.action-btn.warn {
  background: rgba(255, 107, 61, 0.12);
  border-color: rgba(255, 107, 61, 0.25);
  color: #ff6b3d;
}

.action-icon {
  width: 18px;
  height: 18px;
}

/* 键盘快捷键卡片 */
.shortcuts-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  background:
    linear-gradient(180deg, rgba(22, 39, 62, 0.78), rgba(13, 25, 42, 0.86));
  overflow: hidden;
}

.shortcuts-header {
  color: rgba(230, 245, 255, 0.9);
  font-size: 13px;
  font-weight: 600;
}

.shortcuts-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.shortcut-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.shortcut-key {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 36px;
  height: 22px;
  padding: 0 8px;
  background: rgba(18, 34, 55, 0.72);
  border: 1px solid rgba(74, 130, 186, 0.25);
  border-radius: 6px;
  color: rgba(200, 220, 240, 0.95);
  font-size: 10px;
  font-weight: 600;
  font-family: 'SF Mono', 'Consolas', monospace;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.shortcut-desc {
  color: rgba(160, 180, 200, 0.8);
  font-size: 11px;
}

.mic-error {
  color: #f0a0a0;
  font-size: 10px;
  margin-top: 2px;
}

/* 中间容器外间距：与左右容器间距 16px（由 cockpit-grid 的 gap 控制），内部元素间距 12px */
.center-col {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 0;
}

.center-main-card {
  flex: 1;
  background:
    radial-gradient(64% 58% at 50% 32%, rgba(58, 102, 144, 0.14) 0%, rgba(12, 31, 51, 0.08) 48%, transparent 78%);
  border: 0;
  border-radius: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  padding: 20px 24px 10px;
  box-shadow: none;
  min-height: 0;
  overflow: hidden;
}

.center-content {
  width: min(100%, 760px);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  padding: 18px 20px 8px;
}

/* AI 面试官头像区域 */
.ai-avatar-wrap {
  position: relative;
  width: 360px;
  height: 360px;
  display: grid;
  place-items: center;
  isolation: isolate;
}

.ai-avatar-wrap.idle {
  width: 360px;
  height: 360px;
}

.ai-avatar-wrap.ready {
  width: min(94%, 760px);
  height: min(60vh, 580px);
  border-radius: 44px;
  padding: 0;
  background:
    linear-gradient(90deg, rgba(3, 11, 22, 0) 0%, rgba(10, 24, 38, 0.42) 18%, rgba(13, 28, 43, 0.24) 50%, rgba(10, 24, 38, 0.42) 82%, rgba(3, 11, 22, 0) 100%),
    radial-gradient(52% 62% at 50% 58%, rgba(63, 76, 76, 0.32) 0%, rgba(30, 47, 56, 0.22) 42%, rgba(6, 16, 29, 0) 78%);
  border: none;
  box-shadow:
    0 26px 72px rgba(1, 8, 20, 0.2);
  backdrop-filter: blur(14px) saturate(106%);
  -webkit-backdrop-filter: blur(14px) saturate(106%);
}

.ai-avatar-wrap.ready::before {
  content: '';
  position: absolute;
  inset: 4% 6%;
  border-radius: inherit;
  pointer-events: none;
  background:
    radial-gradient(42% 56% at 50% 46%, rgba(92, 108, 104, 0.2) 0%, rgba(43, 60, 67, 0.1) 52%, transparent 74%),
    linear-gradient(90deg, rgba(5, 14, 27, 0.72), transparent 20%, transparent 80%, rgba(5, 14, 27, 0.72));
  filter: blur(18px);
  z-index: 1;
}

.ai-avatar-ring {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(91, 164, 255, 0.22);
  pointer-events: none;
}

.ai-avatar-ring.outer {
  width: 302px;
  height: 302px;
  box-shadow:
    0 0 86px rgba(74, 137, 255, 0.18),
    inset 0 0 54px rgba(74, 137, 255, 0.07);
}

.ai-avatar-ring.inner {
  width: 218px;
  height: 218px;
  background: radial-gradient(circle, rgba(74, 137, 255, 0.15) 0%, rgba(74, 137, 255, 0.05) 62%, transparent 74%);
}

.listening-ring {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(105, 178, 255, 0.34);
  background:
    radial-gradient(circle, transparent 58%, rgba(88, 160, 255, 0.08) 59%, transparent 64%),
    conic-gradient(from 90deg, transparent 0 18%, rgba(115, 197, 255, 0.45) 22%, transparent 30% 52%, rgba(65, 137, 255, 0.4) 58%, transparent 66% 100%);
  filter: drop-shadow(0 0 18px rgba(74, 137, 255, 0.18));
  opacity: 0;
  pointer-events: none;
  animation: listeningPulse 2.7s ease-out infinite;
  z-index: 0;
}

.listening-ring.ring-a {
  width: 178px;
  height: 178px;
  animation-delay: 0s;
}

.listening-ring.ring-b {
  width: 226px;
  height: 226px;
  animation-delay: 0.45s;
}

.listening-ring.ring-c {
  width: 274px;
  height: 274px;
  animation-delay: 0.9s;
}

.listening-ring.ring-d {
  width: 326px;
  height: 326px;
  animation-delay: 1.35s;
}

@keyframes listeningPulse {
  0% {
    transform: scale(0.86) rotate(0deg);
    opacity: 0;
  }
  18% {
    opacity: 0.95;
  }
  72% {
    opacity: 0.38;
  }
  100% {
    transform: scale(1.08) rotate(38deg);
    opacity: 0;
  }
}

.ai-avatar {
  position: relative;
  width: 176px;
  height: 176px;
  z-index: 2;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid rgba(125, 190, 255, 0.44);
  background: rgba(7, 16, 31, 0.74);
  box-shadow:
    0 22px 48px rgba(3, 10, 24, 0.34),
    0 0 42px rgba(74, 137, 255, 0.24);
}

.ai-avatar.ready-face {
  width: 100%;
  height: auto;
  max-height: 100%;
  aspect-ratio: 3 / 2;
  border-radius: 42px;
  border: none;
  overflow: hidden;
  background:
    linear-gradient(90deg, rgba(2, 10, 20, 0) 0%, rgba(13, 29, 43, 0.36) 22%, rgba(16, 31, 42, 0.18) 50%, rgba(13, 29, 43, 0.36) 78%, rgba(2, 10, 20, 0) 100%),
    radial-gradient(48% 58% at 50% 46%, rgba(74, 84, 82, 0.28) 0%, rgba(29, 44, 51, 0.18) 48%, rgba(5, 14, 27, 0) 82%);
  box-shadow: none;
}

.ai-avatar.ready-face::before {
  content: '';
  position: absolute;
  left: 8%;
  right: 8%;
  bottom: 12%;
  height: 12%;
  border-radius: 50%;
  background: radial-gradient(ellipse at center, rgba(5, 17, 33, 0.72) 0%, rgba(5, 17, 33, 0) 70%);
  filter: blur(4px);
  pointer-events: none;
  z-index: 2;
}

.ai-avatar.ready-face::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  pointer-events: none;
  z-index: 4;
  box-shadow:
    inset 0 0 76px rgba(4, 13, 24, 0.34),
    inset 0 -18px 30px rgba(4, 13, 24, 0.28);
}

.ai-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center 18%;
}

.avatar-video {
  position: absolute;
  inset: 0;
  z-index: 1;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
}

.avatar-video-source {
  opacity: 0;
  pointer-events: none;
}

.avatar-video-keyed {
  z-index: 3;
  object-fit: contain;
  object-position: center bottom;
  transform: scale(1.16);
  transform-origin: center bottom;
  border-radius: 44px;
  clip-path: inset(0 round 44px);
  filter: drop-shadow(0 20px 30px rgba(3, 11, 22, 0.48)) saturate(0.98) contrast(1.02);
  -webkit-mask-image:
    linear-gradient(to right, transparent 0%, rgba(0, 0, 0, 0.4) 18%, #000 30%, #000 70%, rgba(0, 0, 0, 0.4) 82%, transparent 100%),
    linear-gradient(to bottom, transparent 0%, #000 12%, #000 82%, transparent 100%),
    radial-gradient(112% 102% at 50% 50%, #000 54%, rgba(0, 0, 0, 0.72) 76%, transparent 100%);
  mask-image:
    linear-gradient(to right, transparent 0%, rgba(0, 0, 0, 0.4) 18%, #000 30%, #000 70%, rgba(0, 0, 0, 0.4) 82%, transparent 100%),
    linear-gradient(to bottom, transparent 0%, #000 12%, #000 82%, transparent 100%),
    radial-gradient(112% 102% at 50% 50%, #000 54%, rgba(0, 0, 0, 0.72) 76%, transparent 100%);
  -webkit-mask-composite: source-in;
  mask-composite: intersect;
}

.interviewer-direct-video {
  position: absolute;
  inset: 0;
  z-index: 5;
  width: 100%;
  height: 100%;
  object-fit: contain;
  filter: saturate(1.02) contrast(1.02) brightness(0.98);
}

.center-camera-preview {
  position: absolute;
  inset: 0;
  z-index: 2;
  object-fit: cover;
  object-position: center;
  transform: scaleX(-1);
  transform-origin: center;
}

.center-waiting-text {
  position: absolute;
  inset: 0;
  z-index: 2;
  display: grid;
  place-items: center;
  color: #9ec8ef;
  font-size: 16px;
  letter-spacing: 0.4px;
}

.ai-avatar.ready-face .ai-avatar-img {
  object-position: center 10%;
}

.ai-avatar.ready-face .avatar-video {
  object-fit: contain;
  object-position: center bottom;
}

.ai-meta {
  text-align: center;
  margin-top: 18px;
  width: min(92%, 780px);
  padding-bottom: 10px;
  position: relative;
  z-index: 8;
}

.ai-avatar-status {
  margin-top: 0;
  color: #8fe6bc;
  font-size: 13px;
  line-height: 1.4;
  min-height: 18px;
}

.interviewer-retry-btn {
  margin-top: 8px;
  padding: 6px 14px;
  border: 1px solid rgba(112, 159, 255, 0.38);
  border-radius: 8px;
  background: rgba(22, 36, 66, 0.7);
  color: #c8dbff;
  font: inherit;
  cursor: pointer;
}

.interviewer-retry-btn:hover {
  background: rgba(38, 57, 96, 0.82);
  border-color: rgba(132, 178, 255, 0.62);
}

.ai-status {
  color: #00d47e;
  margin-top: 8px;
  font-size: 14px;
}

.ai-live-text {
  color: #cfe0f6;
  max-width: 690px;
  line-height: 1.62;
  font-size: 17px;
  min-height: 0;
  max-height: calc(1.62em * 2);
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  text-overflow: ellipsis;
  margin-top: 12px;
  opacity: 0.95;
}

.center-caption {
  width: min(860px, 100%);
  align-self: center;
  margin: 0 auto 2px;
  min-height: 42px;
  max-height: 82px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  text-align: center;
  color: #d7e8fb;
  font-size: 17px;
  line-height: 1.65;
  padding: 8px 18px;
  border-radius: 14px;
  background: rgba(8, 20, 36, 0.72);
  border: 1px solid rgba(104, 162, 228, 0.26);
  box-shadow:
    0 10px 28px rgba(0, 0, 0, 0.18),
    inset 0 1px 0 rgba(220, 239, 255, 0.05);
  position: relative;
  z-index: 8;
}

.center-caption.user {
  color: #dff8ff;
  border-color: rgba(103, 232, 249, 0.22);
  background: rgba(5, 22, 34, 0.5);
}

.center-caption-label {
  color: #67e8f9;
  font-weight: 700;
  margin-right: 6px;
}

.live-cursor {
  display: inline-block;
  width: 8px;
  height: 1em;
  margin-left: 4px;
  border-radius: 2px;
  background: #00d47e;
  vertical-align: -1px;
  animation: liveCursorBlink 0.9s step-end infinite;
}

@keyframes liveCursorBlink {
  0%, 45% { opacity: 1; }
  46%, 100% { opacity: 0.2; }
}

/* AI 实时引导提示区域 */
.ai-tips-section {
  position: relative;
  overflow: hidden;
  background:
    linear-gradient(135deg, rgba(18, 48, 78, 0.94) 0%, rgba(14, 31, 52, 0.96) 58%, rgba(10, 22, 38, 0.98) 100%);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  border: 1px solid rgba(92, 157, 244, 0.42);
  padding: 16px;
  opacity: 1;
  transform: none;
  z-index: 7;
  box-shadow:
    0 14px 36px rgba(0, 0, 0, 0.26),
    inset 0 1px 0 rgba(220, 239, 255, 0.08);
}

.ai-tips-section::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(110deg, rgba(0, 212, 126, 0) 20%, rgba(0, 212, 126, 0.08) 50%, rgba(0, 212, 126, 0) 80%);
  transform: translateX(-140%);
  animation: aiTipsShimmer 1.2s ease-out 180ms 1 both;
  pointer-events: none;
}

.ai-tips-header {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  color: #00d47e;
  font-size: 14px;
  font-weight: 600;
  opacity: 1;
  transform: none;
}

.sparkle-icon {
  width: 18px;
  height: 18px;
  color: #00d47e;
}

.ai-tips-cards {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.tip-card {
  background: linear-gradient(165deg, rgba(24, 49, 77, 0.9), rgba(17, 35, 58, 0.94));
  border: 1px solid rgba(100, 166, 244, 0.34);
  border-radius: 12px;
  padding: 14px;
  display: flex;
  gap: 12px;
  align-items: flex-start;
  opacity: 1;
  transform: none;
  box-shadow: inset 0 1px 0 rgba(220, 239, 255, 0.06);
}

.tip-card:nth-child(1) {
  animation-delay: 0ms;
}

.tip-card:nth-child(2) {
  animation-delay: 0ms;
}

.tip-card-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: rgba(0, 212, 126, 0.15);
  color: #00d47e;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.tip-card-icon svg {
  width: 20px;
  height: 20px;
}

.tip-card-icon.orange {
  background: rgba(255, 107, 61, 0.15);
  color: #ff6b3d;
}

.tip-card-content {
  flex: 1;
  min-width: 0;
}

.tip-card-title {
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 6px;
}

.tip-card-text {
  color: rgba(218, 232, 248, 0.9);
  font-size: 12px;
  line-height: 1.58;
}

.tip-fade-enter-active,
.tip-fade-leave-active {
  transition: opacity 360ms ease, transform 360ms ease;
}

.tip-fade-enter-from,
.tip-fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

@keyframes aiTipsSectionReveal {
  0% {
    opacity: 0;
    transform: translateY(10px) scale(0.99);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes aiTipsHeaderReveal {
  0% {
    opacity: 0;
    transform: translateY(6px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes aiTipCardReveal {
  0% {
    opacity: 0;
    transform: translateY(12px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes aiTipsShimmer {
  0% {
    transform: translateX(-140%);
  }
  100% {
    transform: translateX(140%);
  }
}

.right-col {
  min-height: 0;
}

.transcript-card {
  height: 100%;
  background:
    linear-gradient(180deg, rgba(17, 30, 49, 0.82), rgba(9, 19, 34, 0.94));
  border: 1px solid rgba(99, 151, 218, 0.2);
  border-radius: 18px;
  display: grid;
  grid-template-rows: auto 1fr;
  overflow: hidden;
  box-shadow: 0 18px 44px rgba(0, 0, 0, 0.24), inset 0 1px 0 rgba(190, 220, 255, 0.04);
}

.transcript-header {
  padding: 16px 18px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(74, 107, 151, 0.24);
  background: rgba(15, 28, 47, 0.48);
}

.transcript-title {
  color: #e7f1ff;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.chat-messages {
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.transcript-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  width: 100%;
}

.transcript-item.user {
  flex-direction: row-reverse;
  justify-content: flex-start;
}

.transcript-badge {
  width: 28px;
  height: 28px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  font-size: 11px;
  color: #e9f2ff;
  background: rgba(74, 137, 255, 0.22);
  border: 1px solid rgba(128, 182, 255, 0.18);
  flex-shrink: 0;
}

.transcript-item.user .transcript-badge {
  background: rgba(40, 217, 156, 0.2);
  border-color: rgba(40, 217, 156, 0.2);
}

.transcript-item.welcome {
  padding: 12px;
  border: 1px solid rgba(74, 137, 255, 0.18);
  border-radius: 14px;
  background:
    linear-gradient(135deg, rgba(42, 78, 123, 0.28), rgba(12, 25, 43, 0.66)),
    radial-gradient(circle at 12% 0%, rgba(74, 137, 255, 0.2), transparent 46%);
}

.transcript-item.welcome .transcript-badge {
  background: linear-gradient(135deg, rgba(74, 137, 255, 0.48), rgba(40, 217, 156, 0.22));
  border-color: rgba(126, 190, 255, 0.28);
}

.transcript-content {
  min-width: 0;
  max-width: 78%;
}

.transcript-text {
  color: #d3e3f8;
  font-size: 13px;
  line-height: 1.65;
  word-break: break-word;
  padding: 10px 12px;
  border: 1px solid rgba(74, 137, 255, 0.16);
  border-radius: 4px 14px 14px 14px;
  background: rgba(18, 34, 55, 0.72);
  box-shadow: inset 0 1px 0 rgba(220, 239, 255, 0.04);
}

.transcript-time {
  color: #6f839f;
  font-size: 11px;
  margin-top: 4px;
}

.transcript-item.user .transcript-content {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.transcript-item.user .transcript-text {
  color: #e8fff7;
  border-color: rgba(40, 217, 156, 0.18);
  border-radius: 14px 4px 14px 14px;
  background: linear-gradient(135deg, rgba(30, 118, 92, 0.7), rgba(21, 82, 111, 0.72));
}

.transcript-item.user .transcript-time {
  text-align: right;
}

.center-chat-input {
  width: min(860px, 100%);
  align-self: center;
  margin-top: 8px;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  padding: 12px;
  border: 1px solid rgba(92, 157, 244, 0.28);
  border-radius: 18px;
  background:
    linear-gradient(135deg, rgba(16, 34, 58, 0.9), rgba(9, 20, 36, 0.92)),
    radial-gradient(circle at 20% 0%, rgba(74, 137, 255, 0.18), transparent 42%);
  box-shadow:
    0 18px 44px rgba(0, 0, 0, 0.25),
    0 0 28px rgba(74, 137, 255, 0.08),
    inset 0 1px 0 rgba(220, 239, 255, 0.06);
  position: relative;
  z-index: 6;
}

.chat-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat-mic-btn {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  border: 1px solid rgba(93, 137, 194, 0.34);
  background: rgba(21, 40, 63, 0.84);
  color: #d5e5f8;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.chat-mic-icon {
  width: 18px;
  height: 18px;
  display: block;
}

.chat-mic-btn.active {
  border-color: rgba(74, 137, 255, 0.72);
  background: rgba(42, 78, 123, 0.88);
  box-shadow: 0 0 0 4px rgba(74, 137, 255, 0.12);
}

.center-chat-input input {
  background: rgba(8, 18, 32, 0.76);
  border: 1px solid rgba(93, 137, 194, 0.26);
  border-radius: 12px;
  padding: 11px 13px;
  font-size: 13px;
  color: var(--text-primary);
  outline: none;
}

.center-chat-input input:focus {
  border-color: rgba(74, 137, 255, 0.68);
  box-shadow: 0 0 0 3px rgba(74, 137, 255, 0.12);
}

.center-chat-input input::placeholder {
  color: #7f94b0;
}

.btn {
  border: none;
  border-radius: 10px;
  height: 34px;
  padding: 0 14px;
  cursor: pointer;
  font-weight: 700;
  font-size: 12px;
  white-space: nowrap;
}

.end-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: linear-gradient(135deg, #ff6b3d, #ff4d32);
  color: #fff;
  min-width: 112px;
  height: 38px;
  box-shadow: 0 10px 22px rgba(255, 90, 57, 0.22);
}

.end-btn img {
  width: 15px;
  height: 15px;
  object-fit: contain;
}

.btn.primary {
  background: linear-gradient(135deg, #2f8f66, #28d99c);
  color: #fff;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.voice-panel {
  width: min(760px, 100%);
  align-self: center;
  border: 1px solid rgba(92, 157, 244, 0.22);
  border-radius: 18px;
  padding: 14px 14px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  background:
    linear-gradient(180deg, rgba(17, 34, 57, 0.92), rgba(10, 21, 37, 0.9));
  box-shadow: 0 16px 36px rgba(0, 0, 0, 0.24), inset 0 1px 0 rgba(220, 239, 255, 0.05);
}

.voice-record-btn {
  width: 86px;
  height: 86px;
  border-radius: 50%;
  border: 2px solid #6f93b9;
  background: radial-gradient(circle, #365d86 0%, #1f3f61 70%);
  color: #e6f0fb;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.voice-record-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.voice-record-btn.recording {
  border-color: #f79a8f;
  background: radial-gradient(circle, #b15554 0%, #7a343a 70%);
}

.voice-panel-tip {
  color: #90a7c3;
  font-size: 11px;
}

@media (max-width: 1280px) {
  .cockpit-grid {
    grid-template-columns: 20% 60% 20%;
  }

  .ai-orb-wrap {
    width: 170px;
    height: 170px;
  }

  .orb.outer {
    width: 152px;
    height: 152px;
  }

  .orb.inner {
    width: 104px;
    height: 104px;
  }
}

@media (max-width: 1100px) {
  .cockpit-grid {
    grid-template-columns: 1fr;
  }

  .left-col,
  .right-col,
  .center-col {
    min-height: auto;
  }

  .transcript-card {
    min-height: 420px;
  }
}

/* 面试概览卡片 */
.overview-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.overview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.overview-title {
  color: rgba(230, 245, 255, 0.9);
  font-size: 13px;
  font-weight: 600;
}

.overview-badge {
  background: linear-gradient(135deg, #00d47e, #28d99c);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 999px;
}

.overview-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.overview-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.overview-icon {
  font-size: 14px;
  opacity: 0.8;
}

.overview-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.overview-label {
  color: rgba(160, 180, 200, 0.7);
  font-size: 10px;
}

.overview-value {
  color: rgba(200, 220, 240, 0.95);
  font-size: 12px;
  font-weight: 600;
}

.overview-value.highlight {
  color: #00d47e;
}

.overview-progress {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.overview-progress-bar {
  height: 5px;
  background: rgba(43, 71, 106, 0.5);
  border-radius: 999px;
  overflow: hidden;
}

.overview-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #00d47e, #4a89ff);
  border-radius: inherit;
}

.overview-progress-label {
  color: rgba(160, 180, 200, 0.7);
  font-size: 10px;
  text-align: right;
}

/* 能力评估卡片 */
.ability-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ability-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ability-title {
  color: rgba(230, 245, 255, 0.9);
  font-size: 13px;
  font-weight: 600;
}

.ability-live {
  color: #00d47e;
  font-size: 10px;
  font-weight: 600;
}

.ability-bars {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ability-bar-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ability-bar-label {
  color: rgba(180, 200, 220, 0.8);
  font-size: 10px;
  width: 52px;
  flex-shrink: 0;
}

.ability-bar-track {
  flex: 1;
  height: 6px;
  background: rgba(43, 71, 106, 0.5);
  border-radius: 999px;
  overflow: hidden;
}

.ability-bar-fill {
  height: 100%;
  border-radius: inherit;
  transition: width 0.4s ease;
}

.ability-bar-fill.high {
  background: linear-gradient(90deg, #00d47e, #28d99c);
}

.ability-bar-fill.medium {
  background: linear-gradient(90deg, #4a89ff, #28d9ff);
}

.ability-bar-fill.low {
  background: linear-gradient(90deg, #ff6b3d, #ff9a4a);
}

.ability-bar-score {
  font-size: 11px;
  font-weight: 600;
  width: 24px;
  text-align: right;
}

.ability-bar-score.high {
  color: #00d47e;
}

.ability-bar-score.medium {
  color: #4a89ff;
}

.ability-bar-score.low {
  color: #ff6b3d;
}

.ability-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 8px;
  border-top: 1px solid rgba(99, 151, 218, 0.15);
}

.ability-summary-label {
  color: rgba(180, 200, 220, 0.8);
  font-size: 11px;
}

.ability-summary-value {
  color: #00d47e;
  font-size: 16px;
  font-weight: 700;
}

/* 快速操作卡片 */
.actions-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.actions-header {
  color: rgba(230, 245, 255, 0.9);
  font-size: 13px;
  font-weight: 600;
}

.actions-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 8px;
  border: 1px solid rgba(93, 137, 194, 0.3);
  border-radius: 10px;
  background: rgba(22, 42, 65, 0.7);
  color: rgba(200, 220, 240, 0.9);
  font-size: 10px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-btn:hover {
  background: rgba(32, 60, 90, 0.85);
  border-color: rgba(74, 137, 255, 0.5);
  transform: translateY(-1px);
}

.action-btn.active {
  background: rgba(0, 212, 126, 0.15);
  border-color: rgba(0, 212, 126, 0.4);
  color: #00d47e;
}

.action-btn.warn {
  color: #ff6b3d;
  border-color: rgba(255, 107, 61, 0.3);
}

.action-btn.warn:hover {
  background: rgba(255, 107, 61, 0.15);
  border-color: rgba(255, 107, 61, 0.5);
}

.action-btn.primary {
  background: rgba(0, 212, 126, 0.12);
  border-color: rgba(0, 212, 126, 0.3);
  color: #00d47e;
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.action-icon {
  width: 14px;
  height: 14px;
}

/* 键盘快捷键卡片 */
.shortcuts-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.shortcuts-header {
  color: rgba(230, 245, 255, 0.9);
  font-size: 13px;
  font-weight: 600;
}

.shortcuts-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.shortcut-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.shortcut-key {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 48px;
  height: 24px;
  padding: 0 8px;
  background: rgba(10, 22, 38, 0.8);
  border: 1px solid rgba(93, 137, 194, 0.35);
  border-radius: 6px;
  color: rgba(200, 220, 240, 0.95);
  font-size: 10px;
  font-weight: 600;
  font-family: inherit;
}

.shortcut-desc {
  color: rgba(160, 180, 200, 0.8);
  font-size: 11px;
}
</style>
