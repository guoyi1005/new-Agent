<template>
  <div class="page-layout">
    <Sidebar />
    <main class="main-content">
      <div class="page-container">
        <!-- 开始 AI 模拟面试区域 -->
        <section class="hero-section" v-if="status !== 'running'">
          <div class="hero-content">
            <div class="hero-text">
              <h1 class="hero-title">开始 AI 模拟面试</h1>
              <p class="hero-desc">基于企业真实岗位需求，生成沉浸式面试环境，助你斩获心仪 Offer。选择你的目标岗位，开启实战。</p>
            </div>
            <div class="hero-actions">
              <button class="btn-start" @click="handleStart" :disabled="starting">
                <span class="play-icon">▶</span>
                <span v-if="starting">开始中…</span><span v-else>立即开始</span>
              </button>
              <button class="btn-config" @click="goToInterviewConfig">
                配置面试参数
              </button>
            </div>
          </div>
        </section>

        <!-- 配置面试参数区域 -->
        <section class="config-section" v-if="status !== 'running' && showConfig">
          <div class="config-card">
            <div class="config-header">
              <span class="config-title">面试配置</span>
              <button class="btn-close" @click="showConfig = false">✕</button>
            </div>
            <div class="config-body">
              <label class="field">
                <span class="label">目标职位</span>
                <input v-model="jobRole" placeholder="例：后端工程师" />
              </label>

              <label class="field">
                <span class="label">面试模式</span>
                <select v-model="config.interview_mode">
                  <option value="full">全流程面试</option>
                  <option value="focused">专项强化</option>
                </select>
              </label>

              <label class="field">
                <span class="label">面试官风格</span>
                <select v-model="config.interviewer_persona">
                  <option value="neutral">中立理性</option>
                  <option value="friendly">亲和鼓励</option>
                  <option value="challenging">严格犀利</option>
                  <option value="pragmatic">务实追问</option>
                </select>
              </label>

              <label class="field" v-if="config.interview_mode === 'focused'">
                <span class="label">专项标签</span>
                <input v-model="config.focus_tags" placeholder="例如：系统设计, Redis, MySQL" />
              </label>

              <label class="field">
                <span class="label">面试难度（1-5）</span>
                <input v-model.number="config.difficulty" type="number" min="1" max="5" />
              </label>

              <label class="field field-full">
                <span class="label">职位/候选人要求</span>
                <textarea v-model="config.requirements" rows="3" placeholder="可填写岗位JD重点、候选人背景等"></textarea>
              </label>

              <label class="field">
                <span class="label">备注</span>
                <input v-model="config.remark" placeholder="可选" />
              </label>
              <div class="camera-config field-full">
                <span class="label">设备测试</span>
                <div class="media-test-grid">
                  <div class="device-panel">
                    <div class="device-panel-header">
                      <span class="label">摄像头测试</span>
                    </div>
                    <div class="device-panel-body camera-body">
                      <video ref="videoRef" autoplay playsinline muted v-show="cameraEnabled"></video>
                      <div class="camera-placeholder" v-show="!cameraEnabled">
                        <div class="placeholder-text">未开启摄像头，点击下方按钮以授权使用</div>
                        <div class="placeholder-sub" v-if="cameraError">{{ cameraError }}</div>
                      </div>
                    </div>
                    <div class="device-panel-footer">
                      <div class="cam-actions">
                        <button class="btn-sm" v-if="!cameraEnabled" @click="enableCamera">开启摄像头</button>
                        <button class="btn-sm" v-else @click="disableCamera">关闭摄像头</button>
                      </div>
                    </div>
                  </div>

                  <div class="device-panel">
                    <div class="device-panel-header">
                      <span class="label">麦克风测试</span>
                    </div>
                    <div class="device-panel-body camera-body">
                      <div class="camera-placeholder" v-if="!micEnabled">
                        <div class="placeholder-text">未开启麦克风，点击下方按钮以授权使用</div>
                        <div class="placeholder-sub" v-if="micError">{{ micError }}</div>
                      </div>
                      <div class="mic-test-wrap" v-else>
                        <div class="mic-test-tip">请对着麦克风说话，查看音量波动</div>
                        <div class="mic-level-track">
                          <div class="mic-level-fill" :style="{ width: `${micLevel}%` }"></div>
                        </div>
                        <div class="mic-level-text">当前音量：{{ Math.round(micLevel) }}%</div>
                      </div>
                    </div>
                    <div class="device-panel-footer">
                      <div class="cam-actions">
                        <button class="btn-sm" v-if="!micEnabled" @click="enableMicrophone">开启麦克风</button>
                        <button class="btn-sm" v-else @click="disableMicrophone">关闭麦克风</button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- 面试表现分析区域 -->
        <section class="analysis-section" v-if="status !== 'running'">
          <div class="section-header">
            <img class="section-icon" src="@/assets/interview/10.png" alt="分析" />
            <span class="section-title">面试表现分析</span>
          </div>
          <div class="analysis-cards">
            <div class="analysis-card">
              <div class="card-label">平均面试分</div>
              <div class="card-value">
                <span class="score">82</span>
                <span class="total">/100</span>
              </div>
              <div class="progress-bar">
                <div class="progress-fill" style="width: 82%"></div>
              </div>
            </div>
            <div class="analysis-card">
              <div class="card-label">关键竞争力</div>
              <div class="competency-item">
                <img class="competency-icon" src="@/assets/interview/Skills.png" alt="竞争力" />
                <div class="competency-info">
                  <div class="competency-name">专业技能</div>
                  <div class="competency-desc">优于 88% 的候选人</div>
                </div>
              </div>
            </div>
            <div class="analysis-card">
              <div class="card-label">
                <span>待提升项</span>
                <span class="badge-warning">建议加强</span>
              </div>
              <div class="improve-item">
                <div class="improve-name">系统设计能力</div>
                <div class="improve-desc">在分布式架构和性能优化方面有待提升，建议查阅相关专题。</div>
              </div>
            </div>
          </div>
        </section>
        
        <!-- 面试历史记录 -->
        <section class="history-section" v-if="status !== 'running'">
          <div class="section-header">
            <div class="section-title-wrap">
              <img class="section-icon" src="@/assets/interview/9.png" alt="历史" />
              <span class="section-title">面试历史记录</span>
            </div>
            <button class="btn-view-all" @click="loadHistory">查看全部</button>
          </div>
          <div class="history-card">
            <div v-if="loadingHistory" class="loading-tip">加载中...</div>
            <div v-else-if="historyError" class="history-error" role="alert">
              <span>{{ historyError }}</span>
              <button type="button" @click="loadHistory">重新加载</button>
            </div>
            <div v-else-if="historyList.length === 0" class="empty-tip">暂无面试记录</div>
            <div v-else class="table-container">
              <table class="history-table">
                <thead>
                  <tr>
                    <th>面试岗位 / 类型</th>
                    <th>面试日期</th>
                    <th>综合评分</th>
                    <th>状态</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in historyList" :key="item.id">
                    <td>
                      <div class="job-info">
                        <div class="job-icon"><img src="@/assets/interview/Position.png" alt="job" /></div> 
                        <div class="job-detail">
                          <div class="job-name">{{ item.job_role || '未知岗位' }}</div>
                          <div class="job-meta">AI 模拟面试</div>
                        </div>
                      </div>
                    </td>
                    <td>{{ formatTime(item.started_at || item.created_at) }}</td>
                    <td>
                      <span class="score-value">{{ getScoreDisplay(item) }}</span>
                    </td>
                    <td>
                      <span :class="{
                        'status-badge finished': item.status === 'finished',
                        'status-badge running': item.status === 'running',
                        'status-badge pending': item.status === 'created'
                      }">{{ formatStatus(item.status) }}</span>
                    </td>
                    <td>
                      <button
                        class="btn-detail"
                        @click="handleSummarize(item)"
                        :disabled="summarizingConversationId === item.conversation_id"
                      >
                        {{ getReportActionText(item) }}
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </section>

        <section class="panel" v-else>
          <header class="panel-header">
            <div class="panel-left">
              <button class="btn-back" @click="handleBack" :disabled="goingBack || ending || sending">
                <span v-if="goingBack">返回中…</span><span v-else>返回</span>
              </button>
              <div class="panel-title">对话</div>
            </div>
            <div class="actions">
              <button class="btn" @click="handleEnd" :disabled="ending || !conversationId">
                <span v-if="ending">结束中…</span><span v-else>结束面试</span>
              </button>
            </div>
          </header>
          <div class="chat">
            <div class="chat-messages" ref="chatListRef">
              <div v-for="(m, idx) in messages" :key="idx" class="chat-item" :class="m.role">
                <div class="bubble">{{ m.content }}</div>
              </div>
            </div>
            <div class="chat-input">
              <input
                v-model="newMessage"
                :placeholder="`对${jobRole || '面试官'} 说点什么…`"
                @keydown.enter.exact.prevent="sendMessage"
              />
              <button class="btn primary" @click="sendMessage" :disabled="sending || !newMessage.trim()">
                <span v-if="sending">发送中…</span><span v-else>发送</span>
              </button>
            </div>
          </div>
        </section>
      </div>
    </main>

    <InterviewDialog
      :visible="startDialog !== null"
      :title="startDialogTitle"
      :message="startDialogMessage"
      :primary-text="startDialogPrimaryText"
      secondary-text="取消"
      @primary="handleStartDialogPrimary"
      @secondary="closeStartDialog"
      @close="closeStartDialog"
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
import Sidebar from '../components/Sidebar.vue';
import { ref, computed, onBeforeUnmount, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router';
import { conversationApi } from '../api/conversation';
import { authApi } from '../api/auth';
import { redirectToLoginOnSessionExpired, resolveApiBase } from '../api/index';
import type { ConversationItem } from '../api/conversation';
import { PATHS } from '../routes/paths';
import InterviewDialog from '../components/InterviewDialog.vue';

const router = useRouter()
const noticeDialog = ref({ visible: false, title: '', message: '' });

function showNoticeDialog(title: string, message: string) {
  noticeDialog.value = { visible: true, title, message };
}

function closeNoticeDialog() {
  noticeDialog.value.visible = false;
}

const initialStatus = localStorage.getItem("conversation_status") || "";
const initialJobRole = localStorage.getItem("job_role") || "";
const initialConversationId = localStorage.getItem("conversation_id") || "";
const hasInitialRunningConversation = initialStatus === "running" && !!initialConversationId;
// 优先从 localStorage 取目标岗位（登录时或上次填写会写入），无会话时也预填
const jobRole = ref(initialJobRole);
const sessionToken = ref<string | null>(localStorage.getItem("session_token") || null);
const localUserId = Number(localStorage.getItem("user_id") || "0");
const userId = ref<number | null>(localUserId > 0 ? localUserId : null);
const conversationId = ref<string | null>(initialConversationId || null);
const status = ref<string | null>(hasInitialRunningConversation ? "running" : null);
const startedAt = ref<string | null>(localStorage.getItem("conversation_started_at") || null);
const endedAt = ref<string | null>(localStorage.getItem("conversation_ended_at") || null);

const CONFIG_STORAGE_KEY = "interview_config_draft";
const config = ref({
  interview_mode: "full" as 'full' | 'focused',
  interviewer_persona: "neutral" as 'neutral' | 'friendly' | 'challenging' | 'pragmatic',
  focus_tags: "",
  requirements: "",
  difficulty: 3,
  remark: "",
  status: 1,
});

const historyList = ref<ConversationItem[]>([]);
const loadingHistory = ref(false);
const historyError = ref('');
const showConfig = ref(false);
const pendingConversationInit = ref(false);
const summarizingConversationId = ref<string | null>(null);
const historyScoreMap = ref<Record<string, number>>({});

function getScoreDisplay(item: ConversationItem) {
  const convId = String(item.conversation_id || '');
  const listScore = item.evaluation_score;
  if (typeof listScore === 'number' && Number.isFinite(listScore)) return String(listScore);
  const v = historyScoreMap.value[convId];
  if (typeof v === 'number' && Number.isFinite(v)) return String(v);
  return '--';
}

function getReportActionText(item: ConversationItem) {
  const convId = String(item.conversation_id || '');
  if (summarizingConversationId.value === convId) return '生成中…';
  if (item.has_evaluation || typeof item.evaluation_score === 'number') return '查看AI报告';
  return '获取AI总结报告';
}

async function handleSummarize(item: ConversationItem) {
  const convId = String(item.conversation_id || '').trim();
  if (!convId) {
    showNoticeDialog('无法生成报告', '当前记录缺少会话编号，请重新加载后再试。');
    return;
  }
  if (summarizingConversationId.value) return;

  summarizingConversationId.value = convId;
  try {
    const res = await conversationApi.summarizeInterview({ conversation_id: convId });
    historyScoreMap.value = {
      ...historyScoreMap.value,
      [convId]: Number(res.score || 0),
    };
    const target = historyList.value.find((x) => String(x.conversation_id) === convId);
    if (target) {
      target.has_evaluation = true;
      target.evaluation_score = Number(res.score || 0);
    }

    router.push(`${PATHS.EVALUATION_REPORT}?conversation_id=${encodeURIComponent(convId)}`);
  } catch (e: any) {
    const msg = e?.message || '生成报告失败';
    showNoticeDialog('报告生成失败', msg);
  } finally {
    summarizingConversationId.value = null;
  }
}

async function loadHistory() {
  loadingHistory.value = true;
  historyError.value = '';
  try {
    // 不传 user_id，让后端直接用 session 中的用户 ID 过滤，避免因 localStorage 与 session 不同步导致 403
    const res = await conversationApi.list({
      page: 1,
      page_size: 20,
      order_by: '-created_at'
    });
    if (!res || !Array.isArray(res.items)) {
      throw new Error('历史记录接口返回格式异常');
    }
    console.log("[Mock] History list:", res.items);
    historyList.value = res.items;
    const scoreMap: Record<string, number> = {};
    for (const item of res.items) {
      if (typeof item.evaluation_score === 'number' && Number.isFinite(item.evaluation_score)) {
        scoreMap[String(item.conversation_id)] = Number(item.evaluation_score);
      }
    }
    historyScoreMap.value = scoreMap;
  } catch (e: any) {
    console.error("[Mock] Failed to load history:", e?.message || e);
    historyList.value = [];
    historyScoreMap.value = {};
    historyError.value = '面试记录加载失败，请稍后重试';
  } finally {
    loadingHistory.value = false;
  }
}

onMounted(() => {
  const raw = localStorage.getItem(CONFIG_STORAGE_KEY);
  if (raw) {
    try {
      const parsed = JSON.parse(raw);
      config.value = {
        ...config.value,
        ...parsed,
        difficulty: Math.min(5, Math.max(1, Number(parsed?.difficulty || 3))),
      };
    } catch (e) {
      console.warn('[Mock] parse config draft failed', e);
    }
  }
  loadHistory();
});

function goToInterviewConfig() {
  router.push(PATHS.AI_INTERVIEW_CONFIG);
}

function formatTime(t: string | null | undefined) {
  if (!t) return '-';
  const d = new Date(t);
  return isNaN(d.getTime()) ? '-' : d.toLocaleString();
}

function formatStatus(s: string) {
  if (s === 'finished') return '已结束';
  if (s === 'running') return '进行中';
  if (s === 'created') return '未开始';
  return s;
}

const starting = ref(false);
const startDialog = ref<null | 'missing-position' | 'login'>(null);
const startDialogTitle = computed(() => startDialog.value === 'login' ? '登录状态已失效' : '尚未设置面试岗位');
const startDialogMessage = computed(() => startDialog.value === 'login'
  ? '开始模拟面试前，需要先登录账号。'
  : '开始模拟面试前，需要先选择或设置目标岗位。');
const startDialogPrimaryText = computed(() => startDialog.value === 'login' ? '重新登录' : '去填写岗位');
const ending = ref(false);
const sending = ref(false);
const goingBack = ref(false);
const leavingByBack = ref(false);

const cameraEnabled = ref(false);
const videoRef = ref<HTMLVideoElement | null>(null);
let cameraStream: MediaStream | null = null;
const cameraError = ref("");
const micEnabled = ref(false);
const micError = ref("");
const micLevel = ref(0);
let micStream: MediaStream | null = null;
let micAudioContext: AudioContext | null = null;
let micAnalyser: AnalyserNode | null = null;
let micDataArray: Uint8Array | null = null;
let micRafId: number | null = null;

// Chat state
type ChatMsg = { role: 'user' | 'ai'; content: string };
const messages = ref<ChatMsg[]>([]);
const newMessage = ref("");
const chatListRef = ref<HTMLElement | null>(null);

async function scrollToBottom() {
  await nextTick();
  const el = chatListRef.value as any;
  if (el) el.scrollTop = el.scrollHeight;
}

async function enableCamera() {
  cameraError.value = "";
  try {
    cameraStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false });
    if (videoRef.value) {
      videoRef.value.srcObject = cameraStream;
    }
    cameraEnabled.value = true;
  } catch (e: any) {
    cameraError.value = e?.message || "无法获取摄像头权限";
    cameraEnabled.value = false;
  }
}

function disableCamera() {
  if (cameraStream) {
    cameraStream.getTracks().forEach(t => t.stop());
    cameraStream = null;
  }
  if (videoRef.value) videoRef.value.srcObject = null;
  cameraEnabled.value = false;
}

function tickMicLevel() {
  if (!micAnalyser || !micDataArray) return;
  micAnalyser.getByteTimeDomainData(micDataArray);
  let sum = 0;
  for (let i = 0; i < micDataArray.length; i++) {
    const normalized = (micDataArray[i] - 128) / 128;
    sum += normalized * normalized;
  }
  const rms = Math.sqrt(sum / micDataArray.length);
  const value = Math.min(100, rms * 240);
  micLevel.value = value;
  micRafId = window.requestAnimationFrame(tickMicLevel);
}

async function enableMicrophone() {
  micError.value = "";
  try {
    micStream = await navigator.mediaDevices.getUserMedia({ audio: true, video: false });
    const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
    if (!AudioCtx) throw new Error('当前浏览器不支持音频分析');
    micAudioContext = new AudioCtx();
    const source = micAudioContext.createMediaStreamSource(micStream);
    micAnalyser = micAudioContext.createAnalyser();
    micAnalyser.fftSize = 1024;
    micDataArray = new Uint8Array(micAnalyser.frequencyBinCount);
    source.connect(micAnalyser);
    micEnabled.value = true;
    tickMicLevel();
  } catch (e: any) {
    micError.value = e?.message || '无法获取麦克风权限';
    micEnabled.value = false;
    disableMicrophone();
  }
}

function disableMicrophone() {
  if (micRafId !== null) {
    window.cancelAnimationFrame(micRafId);
    micRafId = null;
  }
  if (micStream) {
    micStream.getTracks().forEach(t => t.stop());
    micStream = null;
  }
  if (micAudioContext) {
    micAudioContext.close().catch(() => undefined);
    micAudioContext = null;
  }
  micAnalyser = null;
  micDataArray = null;
  micLevel.value = 0;
  micEnabled.value = false;
}

onBeforeUnmount(() => {
  disableCamera();
  disableMicrophone();
  if (!leavingByBack.value && status.value === 'running' && conversationId.value) {
    // 页面卸载时如果还在进行中，尝试提交结束
    // 注意：sendBeacon 更适合页面卸载时的请求，但这里复用 fetch 逻辑
    // 为保证成功率，这里仅做尽力而为的尝试
    handleEnd({ silent: true }).catch(console.error);
  }
});

const statusDisplay = computed(() => {
  if (!status.value) return "未开始";
  if (status.value === "running") return "进行中";
  if (status.value === "finished") return "已结束";
  return status.value;
});

const statusClass = computed(() => {
  if (status.value === "running") return "badge-running";
  if (status.value === "finished") return "badge-finished";
  return "badge-idle";
});

const formattedStartedAt = computed(() => {
  if (!startedAt.value) return "-";
  const d = new Date(startedAt.value);
  return isNaN(d.getTime()) ? startedAt.value : d.toLocaleString();
});

const formattedEndedAt = computed(() => {
  if (!endedAt.value) return "-";
  const d = new Date(endedAt.value);
  return isNaN(d.getTime()) ? endedAt.value : d.toLocaleString();
});

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

  // 不传 user_id，让后端统一用 session_token 解析用户 ID，确保与 list 接口过滤条件一致
  const res = await conversationApi.newId({
    job_role: jobRole.value.trim(),
    session_token: sessionToken.value || undefined,
  });
  conversationId.value = res.conversation_id;

  const startRes = await conversationApi.start({
    conversation_id: conversationId.value,
    config: configPayload,
  });

  startedAt.value = startRes.started_at || res.created_at || new Date().toISOString();
  endedAt.value = null;
  status.value = "running";
  pendingConversationInit.value = false;

  localStorage.setItem("conversation_id", conversationId.value);
  localStorage.setItem("job_role", jobRole.value);
  localStorage.setItem(CONFIG_STORAGE_KEY, JSON.stringify(config.value));
  localStorage.setItem("conversation_status", status.value || "");
  localStorage.setItem("conversation_started_at", startedAt.value || "");
  localStorage.setItem("conversation_ended_at", endedAt.value || "");

  console.groupCollapsed("[Mock] 首条消息触发会话初始化");
  console.log("conversation_id:", conversationId.value);
  console.log("job_role:", jobRole.value);
  console.log("started_at:", startedAt.value);
  console.groupEnd();
}

async function handleStart() {
  if (!sessionToken.value) {
    startDialog.value = 'login';
    return;
  }
  // 未填写目标岗位时，尝试从当前用户档案（session）拉取
  if (!jobRole.value.trim()) {
    try {
      const profile = await authApi.getCurrentProfile();
      if (profile?.target_position?.trim()) {
        jobRole.value = profile.target_position.trim();
        localStorage.setItem("job_role", jobRole.value);
      }
    } catch (_) {
      // 忽略接口失败，下面统一提示
    }
  }
  if (!jobRole.value.trim()) {
    startDialog.value = 'missing-position';
    return;
  }
  starting.value = true;
  try {
    pendingConversationInit.value = true;
    conversationId.value = null;
    status.value = null;
    startedAt.value = null;
    endedAt.value = null;
    showConfig.value = false;
    localStorage.setItem("job_role", jobRole.value.trim());
    localStorage.setItem(CONFIG_STORAGE_KEY, JSON.stringify(config.value));
    localStorage.setItem("conversation_status", "");
    localStorage.removeItem("conversation_id");
    localStorage.removeItem("conversation_started_at");
    localStorage.removeItem("conversation_ended_at");

    router.push(PATHS.AI_CHAT);
  } catch (e: any) {
    console.error("[Mock] 开始面试失败:", e);
    startDialog.value = 'missing-position';
  } finally {
    starting.value = false;
  }
}

function closeStartDialog() {
  startDialog.value = null;
}

function handleStartDialogPrimary() {
  const dialog = startDialog.value;
  startDialog.value = null;
  if (dialog === 'login') {
    router.push(PATHS.LOGIN);
    return;
  }
  router.push({
    path: PATHS.MY,
    query: {
      edit: 'target-position',
      returnTo: PATHS.AI_MOCK_INTERVIEW,
    },
  });
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
    console.groupCollapsed("[Mock] 结束面试提交");
    console.log("request:", {
      conversation_id: conversationId.value,
      user_id: userId.value,
      job_role: jobRole.value.trim(),
      started_at: startedAt.value || endedIso,
      ended_at: endedIso,
    });
    console.log("response:", res);
    console.groupEnd();
    status.value = res.status || null;
    startedAt.value = res.started_at || null;
    endedAt.value = res.ended_at || null;
    localStorage.setItem("conversation_status", status.value || "");
    localStorage.setItem("conversation_started_at", startedAt.value || "");
    localStorage.setItem("conversation_ended_at", endedAt.value || "");
    if (!silent) showNoticeDialog('面试已结束', '本次面试记录已保存。');
    returnToConfigPanel();
    await loadHistory();
    return true;
  } catch (e: any) {
    console.error("[Mock] 结束面试失败:", e);
    if (!silent) showNoticeDialog('结束面试失败', e?.message || '请稍后重试。');
    return false;
  } finally {
    ending.value = false;
  }
}

function returnToConfigPanel() {
  status.value = null;
  showConfig.value = false;
  pendingConversationInit.value = false;
  conversationId.value = null;
  startedAt.value = null;
  endedAt.value = null;
  messages.value = [];
  newMessage.value = "";

  localStorage.removeItem("conversation_id");
  localStorage.removeItem("conversation_started_at");
  localStorage.removeItem("conversation_ended_at");
  localStorage.setItem("conversation_status", "");
}

async function handleBack() {
  if (goingBack.value) return;
  goingBack.value = true;
  try {
    const hasUserDialogue = messages.value.some(m => m.role === 'user' && !!m.content.trim());
    const shouldEndBeforeLeave = hasUserDialogue && status.value === 'running' && !!conversationId.value;
    if (shouldEndBeforeLeave) {
      const ok = await handleEnd({ silent: true });
      if (!ok) {
        showNoticeDialog('结束面试失败', '请重试后再返回。');
        return;
      }
    } else {
      returnToConfigPanel();
    }
  } finally {
    goingBack.value = false;
  }
}

async function sendMessage() {
  const text = newMessage.value.trim();
  if (!text) return;
  messages.value.push({ role: 'user', content: text });
  newMessage.value = "";
  sending.value = true;
  try {
    await ensureConversationInitialized();
    messages.value.push({ role: 'ai', content: "" });
    const ai = messages.value[messages.value.length - 1];
    if (!ai) return;
    await scrollToBottom();
    // 与项目其它接口保持一致：开发环境直连 8080，生产环境走同源 /api
    const url = `${resolveApiBase()}/langgraph/chat`;
    const authToken = localStorage.getItem("session_token") || localStorage.getItem("token") || "";
    const resp = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(authToken ? { Authorization: `Bearer ${authToken}`, 'X-Session-Token': authToken } : {}),
      },
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
            try {
              const payload = JSON.parse(dataText);
              if (payload?.content) {
                ai.content += String(payload.content);
              } else if (payload?.error) {
                throw new Error(String(payload.error));
              }
            } catch {
              ai.content += dataText;
            }
            await scrollToBottom();
          }
        }

        sep = buffer.indexOf('\n\n');
      }
    }

    if (!ai.content) {
      messages.value.splice(messages.value.indexOf(ai), 1);
      showNoticeDialog('AI 面试官连接失败', '面试官没有返回内容，请检查 AI 服务与模型配置后重试。');
      return;
    }
  } catch (e: any) {
    console.error("[Mock] 发送失败:", e);
    const placeholder = messages.value[messages.value.length - 1];
    if (placeholder?.role === 'ai' && !String(placeholder.content || '').trim()) {
      messages.value.pop();
    }
    const failureText = String(e?.message || '');
    if (/尚未配置|未配置|没有可用|模型配置|模型未|not configured|no model/i.test(failureText)) {
      showNoticeDialog(
        'AI 面试官服务尚未配置',
        '当前没有可用的面试官模型，请先在系统配置中维护文本模型（ai.service.text.*）后再开始面试。',
      );
    } else {
      showNoticeDialog('AI 面试官连接失败', failureText || '无法访问 AI 面试官服务，请稍后重试。');
    }
  } finally {
    sending.value = false;
  }
}
</script>

<style scoped>
/* 深色主题配色 */
.page-layout {
  --bg-page: var(--hp-surface);
  --bg-hero: var(--hp-yellow);
  --bg-card: var(--hp-surface);
  --bg-table-header: var(--hp-surface-2);
  --border-color: var(--hp-line);
  --text-primary: var(--hp-ink);
  --text-secondary: var(--hp-muted);
  --text-muted: var(--hp-muted);
  --accent-primary: var(--hp-pink-ink);
  --accent-success: #10b981;
  --accent-warning: #f59e0b;
  --accent-danger: #ef4444;
}

.page-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: var(--bg-page);
}

.main-content {
  flex: 1;
  margin-left: clamp(200px, 25vw, 320px);
  overflow-y: auto;
  height: 100vh;
  background: var(--bg-page);
}

.page-container {
  padding: 24px 6px;
  max-width: 1200px;
  margin: 0 auto;
  background: var(--bg-page);
  min-height: 100%;
  box-sizing: border-box;
}

/* Hero Section - 开始 AI 模拟面试 */
.hero-section {
  width: 100%;
  box-sizing: border-box;
  background: var(--bg-hero);
  border-radius: 16px;
  padding: 40px;
  margin-bottom: 32px;
  border: 1px solid var(--hp-line-strong);
}

.hero-content {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: end;
  gap: 24px;
}

.hero-text {
  flex: 1;
}

.hero-title {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 16px 0;
}

.hero-desc {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin: 0 0 24px 0;
  max-width: 480px;
}

.hero-actions {
  display: flex;
  gap: 16px;
  justify-content: flex-end;
  align-self: center;
}

.btn-start {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #ffffff;
  color: #1e1e3f;
  border: none;
  padding: 12px 24px;
  border-radius: 24px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-start:hover {
  background: #f0f0f0;
  transform: translateY(-1px);
}

.btn-start:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.play-icon {
  font-size: 12px;
}

.btn-config {
  background: transparent;
  color: var(--text-secondary);
  border: 1px solid rgba(255, 255, 255, 0.2);
  padding: 12px 24px;
  border-radius: 24px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-config:hover {
  background: rgba(255, 255, 255, 0.1);
  color: var(--text-primary);
}

/* Config Section - 配置面试参数 */
.config-section {
  margin-bottom: 32px;
}

.config-card {
  box-sizing: border-box;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 18px;
  width: 100%;
}

.config-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.config-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.btn-close {
  background: none;
  border: none;
  color: var(--text-secondary);
  font-size: 18px;
  cursor: pointer;
  padding: 4px;
}

.btn-close:hover {
  color: var(--text-primary);
}

.config-body {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 16px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-full {
  grid-column: 1 / -1;
}

.label {
  color: var(--text-secondary);
  font-size: 14px;
}

input {
  background: var(--bg-page);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 12px 16px;
  font-size: 14px;
  color: var(--text-primary);
  outline: none;
  transition: border-color 0.2s;
}

select,
textarea {
  background: var(--bg-page);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 12px 16px;
  font-size: 14px;
  color: var(--text-primary);
  outline: none;
  transition: border-color 0.2s;
}

select:focus,
textarea:focus {
  border-color: var(--accent-primary);
}

textarea::placeholder {
  color: var(--text-muted);
}

input:focus {
  border-color: var(--accent-primary);
}

input::placeholder {
  color: var(--text-muted);
}

.camera-config {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.media-test-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.device-panel {
  --test-box-width: min(100%, 380px);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.device-panel-header {
  display: flex;
  justify-content: flex-start;
  align-items: center;
}

.device-panel-footer {
  display: flex;
  justify-content: center;
  width: 100%;
}

.device-panel-footer .cam-actions {
  width: var(--test-box-width);
}

.device-panel-footer .btn-sm {
  width: 100%;
  height: 44px;
  padding: 10px 16px;
  border-radius: 10px;
}

.cam-actions {
  display: flex;
  gap: 8px;
}

.btn-sm {
  background: var(--accent-primary);
  border: none;
  color: var(--text-primary);
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn-sm:hover {
  opacity: 0.9;
}

.camera-body {
  border: 1px dashed var(--border-color);
  border-radius: 12px;
  padding: 12px;
  display: grid;
  place-items: center;
  background: var(--bg-page);
}

.device-panel-body {
  width: var(--test-box-width);
  aspect-ratio: 1 / 1;
  min-height: 0;
  margin: 0 auto;
  overflow: hidden;
}

.mic-test-wrap {
  width: 100%;
  max-width: 420px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center;
}

.mic-test-tip {
  color: var(--text-secondary);
  font-size: 13px;
}

.mic-level-track {
  width: 100%;
  height: 10px;
  border-radius: 999px;
  background: var(--hp-surface);
  overflow: hidden;
}

.mic-level-fill {
  height: 100%;
  background: linear-gradient(90deg, #22c55e, #3b82f6);
  transition: width 0.1s linear;
}

.mic-level-text {
  color: var(--text-secondary);
  font-size: 12px;
}

@media (max-width: 900px) {
  .config-body {
    grid-template-columns: 1fr;
  }

  .field-full {
    grid-column: auto;
  }

  .media-test-grid {
    grid-template-columns: 1fr;
  }
}

video {
  width: 100%;
  height: 100%;
  max-width: none;
  border-radius: 10px;
  background: #000;
  object-fit: cover;
}

.camera-placeholder {
  text-align: center;
  color: var(--text-secondary);
}

.placeholder-sub {
  color: var(--accent-danger);
  font-size: 12px;
  margin-top: 8px;
}

/* Analysis Section - 面试表现分析 */
.analysis-section {
  margin-bottom: 32px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
}

.section-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid var(--hp-line);
}

.section-icon {
  width: 25px;
  height: 25px;
  object-fit: contain;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.btn-view-all {
  margin-left: auto;
  background: none;
  border: none;
  color: var(--accent-primary);
  font-size: 13px;
  cursor: pointer;
}

.btn-view-all:hover {
  text-decoration: underline;
}

.analysis-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.analysis-card {
  background: var(--bg-card);
  border: 1px solid var(--hp-line);
  border-radius: 12px;
  padding: 20px;
}

.card-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: var(--text-secondary);
  font-size: 13px;
  margin-bottom: 12px;
}

.badge-warning {
  background: rgba(245, 158, 11, 0.15);
  color: var(--accent-warning);
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 11px;
}

.card-value {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 12px;
}

.score {
  font-size: 36px;
  font-weight: 700;
  color: var(--text-primary);
}

.total {
  font-size: 14px;
  color: var(--text-muted);
}

.progress-bar {
  height: 4px;
  background: var(--border-color);
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--accent-primary), #6366f1);
  border-radius: 2px;
}

.competency-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.competency-icon {
  width: 30px;
  height: 30px;
  object-fit: contain;
  font-size: 24px;
}

.competency-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.competency-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.competency-desc {
  font-size: 12px;
  color: var(--text-secondary);
}

.improve-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.improve-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.improve-desc {
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.5;
}

/* History Section - 面试历史记录 */
.history-section {
  margin-bottom: 32px;
}

.history-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 0;
  overflow: hidden;
}

.table-container {
  overflow-x: auto;
}

.history-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.history-table th {
  background: var(--bg-table-header);
  color: var(--text-secondary);
  font-weight: 500;
  padding: 16px;
  text-align: left;
  border-bottom: 1px solid var(--border-color);
}

.history-table td {
  padding: 16px;
  text-align: left;
  border-bottom: 1px solid var(--border-color);
  color: var(--text-secondary);
}

.history-table tbody tr:hover {
  background: rgba(255, 255, 255, 0.02);
}

.job-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.job-icon {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
}

.job-icon img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  border-radius: 6px;
}

.job-detail {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.job-name {
  font-weight: 500;
  color: var(--text-primary);
}

.job-meta {
  font-size: 12px;
  color: var(--text-muted);
}

.score-value {
  font-size: 18px;
  font-weight: 600;
  color: var(--accent-primary);
}

.status-badge {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.finished {
  background: rgba(16, 185, 129, 0.15);
  color: var(--accent-success);
}

.status-badge.running {
  background: rgba(79, 70, 229, 0.15);
  color: var(--accent-primary);
}

.status-badge.pending {
  background: rgba(139, 146, 168, 0.15);
  color: var(--text-secondary);
}

.btn-detail {
  background: none;
  border: none;
  color: var(--accent-primary);
  font-size: 13px;
  cursor: pointer;
}

.btn-detail:hover {
  text-decoration: underline;
}

.loading-tip, .empty-tip {
  color: var(--text-secondary);
  text-align: center;
  padding: 40px;
}

.history-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding: 36px 20px;
  color: #c7cfdf;
  text-align: center;
}

.history-error button {
  height: 36px;
  padding: 0 16px;
  color: #dfe6ff;
  font: inherit;
  font-size: 13px;
  cursor: pointer;
  background: rgba(91, 108, 214, 0.14);
  border: 1px solid rgba(110, 130, 255, 0.34);
  border-radius: 8px;
  transition: background 0.18s ease, border-color 0.18s ease;
}

.history-error button:hover {
  background: rgba(91, 108, 214, 0.22);
  border-color: rgba(126, 145, 255, 0.52);
}

.history-error button:focus-visible {
  outline: 2px solid rgba(128, 154, 255, 0.75);
  outline-offset: 2px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.panel-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.btn-back {
  background: transparent;
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  height: 36px;
  padding: 0 12px;
  cursor: pointer;
}

.btn-back:hover {
  color: var(--text-primary);
  border-color: var(--hp-line);
}

.btn-back:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Chat */
.chat { display: flex; flex-direction: column; height: calc(100vh - 160px); background: var(--bg-page); }
.chat-messages { flex: 1; overflow-y: auto; padding: 8px; display: flex; flex-direction: column; gap: 10px; background: var(--bg-page); }
.chat-item { display: flex; }
.chat-item.user { justify-content: flex-end; }
.chat-item.ai { justify-content: flex-start; }
.bubble { max-width: 70%; padding: 10px 12px; border-radius: 12px; line-height: 1.4; background: var(--bg-card); border: 1px solid var(--border-color); color: var(--text-primary); }
.chat-item.user .bubble { background: var(--accent-primary); color: var(--text-primary); border-color: var(--accent-primary); }
.chat-item.ai .bubble { background: var(--bg-card); color: var(--text-primary); }
.chat-input { display: grid; grid-template-columns: 1fr auto; gap: 12px; padding: 8px; border-top: 1px solid var(--border-color); background: var(--bg-page); }

/* Responsive */
@media (max-width: 1024px) {
  .analysis-cards {
    grid-template-columns: 1fr;
  }
  
  .hero-content {
    grid-template-columns: 1fr;
    text-align: left;
  }
}

@media (max-width: 720px) {
  .page-container {
    padding: 24px 6px;
  }
  
  .hero-section {
    padding: 24px;
  }
  
  .hero-title {
    font-size: 24px;
  }
  
  .hero-actions {
    flex-direction: column;
    width: 100%;
  }
  
  .btn-start, .btn-config {
    width: 100%;
    justify-content: center;
  }
  
  .history-table th,
  .history-table td {
    padding: 12px 8px;
    font-size: 13px;
  }
}
</style>
