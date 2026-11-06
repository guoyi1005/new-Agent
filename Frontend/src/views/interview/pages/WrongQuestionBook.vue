<template>
  <div class="page-layout">
    <Sidebar />
    <main class="main-content">
      <div class="page-container">

        <!-- 页面头部 -->
        <div class="page-header">
          <div class="header-left">
            <h1>错题本</h1>
            <p class="header-sub">回顾错题，查漏补缺，针对性提升</p>
          </div>
          <div class="header-stats">
            <div class="stat-card">
              <span class="stat-num">{{ stats.totalWrong }}</span>
              <span class="stat-lbl">总错题数</span>
            </div>
            <div class="stat-card warn">
              <span class="stat-num">{{ stats.highFreq }}</span>
              <span class="stat-lbl">高频错题</span>
            </div>
            <div class="stat-card success">
              <span class="stat-num">{{ stats.mastered }}</span>
              <span class="stat-lbl">已掌握</span>
            </div>
            <div class="stat-card info">
              <span class="stat-num">{{ stats.thisWeek }}</span>
              <span class="stat-lbl">本周新增</span>
            </div>
          </div>
        </div>

        <!-- 活跃错题 Tab -->
        <div v-if="activeTab === 'active'">
          <!-- 筛选区 -->
          <div class="filter-panel">
            <div class="search-row">
              <input v-model="searchQuery" type="text" placeholder="搜索错题关键词…" class="search-input" @input="currentPage = 1" />
              <button class="btn-primary" @click="showReviewModal = true; startReview('sequential')">▶ 顺序复习</button>
              <button class="btn-secondary" @click="startReview('random')">随机复习</button>
            </div>
            <div class="filter-rows">
              <div class="filter-row">
                <span class="filter-label">错误原因</span>
                <div class="filter-chips">
                  <span v-for="r in errorReasonOptions" :key="r.value" class="filter-chip"
                    :class="{ active: filterReason === r.value }" @click="toggleFilter('reason', r.value)">
                    {{ r.label }}
                  </span>
                </div>
              </div>
              <div class="filter-row">
                <span class="filter-label">时间范围</span>
                <div class="filter-chips">
                  <span v-for="t in timeRangeOptions" :key="t.value" class="filter-chip"
                    :class="{ active: filterTimeRange === t.value }" @click="toggleFilter('time', t.value)">
                    {{ t.label }}
                  </span>
                </div>
              </div>
              <div class="filter-row">
                <span class="filter-label">岗位</span>
                <div class="filter-chips">
                  <span v-for="k in positionOptions" :key="k.value" class="filter-chip"
                    :class="{ active: filterPosition === k.value }" @click="toggleFilter('position', k.value)">
                    {{ k.label }}
                  </span>
                </div>
              </div>
            </div>
            <div class="filter-summary" v-if="hasActiveFilters">
              <span class="summary-text">已筛选 {{ filteredActiveQuestions.length }} 题</span>
              <button class="clear-btn" @click="clearFilters">清除筛选</button>
            </div>
          </div>

          <!-- 错题列表 -->
          <div class="q-table">
            <div v-if="paginatedActiveQuestions.length === 0" class="empty-state">
              <p>暂无错题，继续加油练习！</p>
            </div>
            <template v-else>
              <!-- 列表头 -->
              <div class="q-table-head">
                <span class="col-status"></span>
                <span class="col-title">题目</span>
                <span class="col-knowledge">知识点</span>
                <span class="col-source">来源</span>
                <span class="col-count">答错次数</span>
                <span class="col-diff">难度</span>
                <span class="col-ops">操作</span>
              </div>
              <!-- 列表行 -->
              <div
                v-for="wq in paginatedActiveQuestions"
                :key="wq.id"
                class="q-row"
                :class="{ 'row-due': isReviewDue(wq) }"
              >
                <span class="col-status">
                  <span class="status-circle" :class="{ highlight: wq.isHighlight }"></span>
                </span>
                <span class="col-title">
                  <span class="row-title" @click="openReviewQuestion(wq)">{{ wq.title }}</span>
                  <span class="due-tag" v-if="isReviewDue(wq)">今日复习</span>
                  <span class="note-dot" v-if="wq.note" title="有笔记">💬</span>
                </span>
                <span class="col-knowledge">{{ wq.knowledgePoint }}</span>
                <span class="col-source row-muted">{{ wq.source }}</span>
                <span class="col-count">
                  <span class="metric-pill count-pill" :class="{ 'count-danger': wq.wrongCount >= 3 }">×{{ wq.wrongCount }}</span>
                </span>
                <span class="col-diff">
                  <span class="metric-pill diff-pill" :class="'diff-' + wq.difficulty">{{ getDifficultyLabel(wq.difficulty) }}</span>
                </span>
                <span class="col-ops">
                  <button class="row-btn" title="复习" @click="openReviewQuestion(wq)">复习</button>
                  <button class="row-btn" title="AI答疑" @click="openAIChat(wq)">AI</button>
                  <button class="row-btn" title="编辑" @click="openEditModal(wq)">编辑</button>
                  <button class="row-btn row-btn-ok" title="已掌握" @click="markMastered(wq)">已掌握</button>
                </span>
              </div>
            </template>
          </div>

          <!-- 分页 -->
          <div class="pagination" v-if="activeTotalPages > 1">
            <button class="page-btn" :disabled="currentPage === 1" @click="currentPage--">上一页</button>
            <span class="page-info">{{ currentPage }} / {{ activeTotalPages }}</span>
            <button class="page-btn" :disabled="currentPage === activeTotalPages" @click="currentPage++">下一页</button>
          </div>
        </div>

        <!-- 已掌握 Tab -->
        <div v-if="activeTab === 'mastered'">
          <div class="mastered-header">
            <span>已掌握题目（{{ masteredQuestions.length }}）</span>
            <span class="mastered-tip">可将题目重新移回活跃错题</span>
          </div>
          <div class="q-table">
            <div v-if="masteredQuestions.length === 0" class="empty-state">
              <p>还没有已掌握的题目，加油！</p>
            </div>
            <template v-else>
              <div class="q-table-head">
                <span class="col-status"></span>
                <span class="col-title">题目</span>
                <span class="col-knowledge">知识点</span>
                <span class="col-source">来源</span>
                <span class="col-count">答错次数</span>
                <span class="col-diff">难度</span>
                <span class="col-ops">操作</span>
              </div>
              <div v-for="wq in masteredQuestions" :key="wq.id" class="q-row q-row-mastered">
                <span class="col-status">
                  <span class="status-circle status-done">✓</span>
                </span>
                <span class="col-title">
                  <span class="row-title row-title-muted">{{ wq.title }}</span>
                </span>
                <span class="col-knowledge row-muted">{{ wq.knowledgePoint }}</span>
                <span class="col-source row-muted">{{ wq.source }}</span>
                <span class="col-count">
                  <span class="metric-pill count-pill">×{{ wq.wrongCount }}</span>
                </span>
                <span class="col-diff">
                  <span class="metric-pill diff-pill" :class="'diff-' + wq.difficulty">{{ getDifficultyLabel(wq.difficulty) }}</span>
                </span>
                <span class="col-ops">
                  <button class="row-btn row-btn-warn" @click="unmarkMastered(wq)">移回</button>
                </span>
              </div>
            </template>
          </div>
        </div>

        <!-- 数据分析 Tab -->
        <div v-if="activeTab === 'analysis'" class="analysis-tab">
          <!-- 错误原因分布 -->
          <div class="analysis-section">
            <h3 class="section-title">错误原因分布</h3>
            <div class="reason-chart">
              <div v-for="item in reasonStats" :key="item.reason" class="reason-bar-row">
                <span class="reason-name">{{ getReasonLabel(item.reason) }}</span>
                <div class="bar-track">
                  <div class="bar-fill" :style="{ width: item.pct + '%', background: item.color }"></div>
                </div>
                <span class="bar-num">{{ item.count }}题 ({{ item.pct }}%)</span>
              </div>
            </div>
          </div>

          <!-- 知识点错误率 -->
          <div class="analysis-section">
            <h3 class="section-title">知识点薄弱分析</h3>
            <div class="knowledge-grid">
              <div v-for="kp in knowledgeStats" :key="kp.name" class="knowledge-card"
                :class="{ 'weakness': kp.errorRate >= 60 }">
                <div class="kp-name">{{ kp.name }}</div>
                <div class="kp-rate-bar">
                  <div class="kp-fill" :style="{ width: kp.errorRate + '%', background: getErrorRateColor(kp.errorRate) }"></div>
                </div>
                <div class="kp-rate-text" :style="{ color: getErrorRateColor(kp.errorRate) }">错误率 {{ kp.errorRate }}%</div>
                <div class="kp-count">{{ kp.wrongCount }}/{{ kp.total }} 题答错</div>
              </div>
            </div>
          </div>

          <!-- 高频错题 TOP5 -->
          <div class="analysis-section">
            <h3 class="section-title">高频错题 TOP 5</h3>
            <div class="top-list">
              <div v-for="(wq, idx) in topWrongQuestions" :key="wq.id" class="top-item">
                <span class="top-rank" :class="'rank-' + (idx + 1)">{{ idx + 1 }}</span>
                <span class="top-title">{{ wq.title }}</span>
                <span class="top-count danger">答错 {{ wq.wrongCount }} 次</span>
                <button class="mini-btn" @click="openReviewQuestion(wq)">复习</button>
              </div>
            </div>
          </div>

          <!-- 月度报告 -->
          <div class="analysis-section">
            <h3 class="section-title">月度错题报告</h3>
            <div class="monthly-report">
              <div class="report-card">
                <div class="report-icon">📊</div>
                <div class="report-content">
                  <div class="report-title">本月新增错题</div>
                  <div class="report-num">{{ stats.thisMonth }} 题</div>
                </div>
              </div>
              <div class="report-card">
                <div class="report-icon">🎯</div>
                <div class="report-content">
                  <div class="report-title">本月掌握错题</div>
                  <div class="report-num">{{ stats.masteredThisMonth }} 题</div>
                </div>
              </div>
              <div class="report-card">
                <div class="report-icon">🔥</div>
                <div class="report-content">
                  <div class="report-title">最薄弱知识点</div>
                  <div class="report-num">{{ weakestKnowledge }}</div>
                </div>
              </div>
              <div class="report-card">
                <div class="report-icon">💡</div>
                <div class="report-content">
                  <div class="report-title">主要错误原因</div>
                  <div class="report-num">{{ topErrorReason }}</div>
                </div>
              </div>
            </div>
            <div class="report-suggestion">
              <h4>💡 提升建议</h4>
              <ul>
                <li v-for="(s, i) in improvementSuggestions" :key="i">{{ s }}</li>
              </ul>
            </div>
          </div>
        </div>

        <!-- 复习计划 Tab -->
        <div v-if="activeTab === 'plan'" class="plan-tab">
          <!-- 艾宾浩斯复习提醒 -->
          <div class="plan-section">
            <div class="plan-header-row">
              <h3 class="section-title">今日复习任务</h3>
              <span class="plan-toggle">
                <input type="checkbox" id="ebbinghaus-toggle" v-model="ebbinghausEnabled" />
                <label for="ebbinghaus-toggle">启用艾宾浩斯遗忘曲线</label>
              </span>
            </div>
            <div v-if="ebbinghausEnabled" class="ebbinghaus-info">
              <div class="ebbinghaus-card" v-for="item in todayReviewPlan" :key="item.id">
                <div class="eb-left">
                  <span class="eb-day-badge">第{{ item.reviewDay }}次</span>
                </div>
                <div class="eb-body">
                  <div class="eb-title">{{ item.title }}</div>
                  <div class="eb-meta">答错于 {{ item.lastWrongTime }} · 建议复习间隔：{{ item.interval }}天</div>
                </div>
                <button class="btn-primary btn-sm" @click="openReviewQuestion(item)">立即复习</button>
              </div>
              <div v-if="todayReviewPlan.length === 0" class="empty-state small">
                <span>🎉 今日暂无复习任务，保持学习节奏！</span>
              </div>
            </div>
          </div>

          <!-- AI个性化复习计划 -->
          <div class="plan-section">
            <h3 class="section-title">AI 个性化复习计划</h3>
            <div class="ai-plan-area">
              <div class="ai-plan-intro">
                <span class="ai-avatar">🤖</span>
                <div class="ai-intro-text">
                  <p>基于你的错题数据，AI已为你生成专属复习计划：</p>
                </div>
              </div>
              <div class="ai-plan-content">
                <div class="plan-item" v-for="(item, idx) in aiReviewPlan" :key="idx">
                  <div class="plan-day">第{{ idx + 1 }}天</div>
                  <div class="plan-tasks">
                    <span class="plan-task" v-for="(t, ti) in item.tasks" :key="ti">{{ t }}</span>
                  </div>
                  <div class="plan-goal">目标：{{ item.goal }}</div>
                </div>
              </div>
              <button class="btn-primary" @click="refreshAIPlan">🔄 重新生成计划</button>
            </div>
          </div>
        </div>

      </div>
    </main>

    <!-- 复习答题弹窗 -->
    <div v-if="showReviewModal && reviewQuestion" class="modal-overlay" @click.self="closeReviewModal">
      <div class="modal-content review-modal">
        <div class="modal-header">
          <div class="modal-header-left">
            <h2>错题复习</h2>
            <span class="review-progress">
              {{ reviewIndex + 1 }} / {{ reviewQueue.length }}
            </span>
          </div>
          <button class="close-btn" @click="closeReviewModal">×</button>
        </div>
        <div class="modal-body">
          <template>
            <div class="review-question-header">
              <div class="review-tags">
                <span class="meta-tag difficulty" :class="'diff-' + reviewQuestion.difficulty">{{ getDifficultyLabel(reviewQuestion.difficulty) }}</span>
                <span class="meta-tag type">{{ reviewQuestion.questionType }}</span>
                <span class="meta-tag knowledge">{{ reviewQuestion.knowledgePoint }}</span>
              </div>
              <div class="review-wrong-info">
                <span class="info-item">来源：{{ reviewQuestion.source }}</span>
                <span class="info-item warn">答错 {{ reviewQuestion.wrongCount }} 次</span>
              </div>
            </div>
            <div class="review-title">{{ reviewQuestion.title }}</div>
            <div class="review-content" v-if="reviewQuestion.content">{{ reviewQuestion.content }}</div>

            <div class="review-core-blocks">
              <div class="compare-block question-full">
                <div class="compare-label">完整题目</div>
                <div class="compare-content preserve-line">{{ reviewQuestion.title || '暂无题目内容' }}<template v-if="reviewQuestion.content">\n{{ reviewQuestion.content }}</template></div>
              </div>
              <div class="compare-block correct-ans">
                <div class="compare-label">参考答案</div>
                <div class="compare-content preserve-line">{{ reviewQuestion.correctAnswer || '暂无参考答案' }}</div>
              </div>
              <div class="compare-block focus-block">
                <div class="compare-label">考察重点</div>
                <div class="compare-content preserve-line">{{ reviewQuestion.assessmentFocus || reviewQuestion.knowledgePoint || '暂无考察重点' }}</div>
              </div>
            </div>

            <!-- 历史错误信息 -->
            <div class="history-wrong" v-if="!showReviewAnswer">
              <div class="history-title">上次错误原因：</div>
              <div class="error-reason-tags">
                <span class="reason-tag" v-for="r in reviewQuestion.errorReasons" :key="r">{{ getReasonLabel(r) }}</span>
              </div>
            </div>

            <!-- 答题区 -->
            <div class="answer-area" v-if="!showReviewAnswer">
              <h4>重新作答：</h4>
              <textarea v-model="reviewUserAnswer" class="answer-textarea" placeholder="请重新输入你的答案…" rows="5"></textarea>
              <div class="answer-btns">
                <button class="btn-primary" @click="submitReviewAnswer">提交对比</button>
              </div>
            </div>

            <!-- 答案对比区（提交后显示） -->
            <div class="answer-compare" v-if="showReviewAnswer">
              <div class="compare-block user-ans">
                <div class="compare-label">我的原答案</div>
                <div class="compare-content">{{ reviewQuestion.userAnswer }}</div>
              </div>
              <div class="compare-block note-block" v-if="reviewQuestion.note">
                <div class="compare-label">我的笔记</div>
                <div class="compare-content">{{ reviewQuestion.note }}</div>
              </div>
              <div class="review-result-btns">
                <button class="btn-danger" @click="confirmWrong">❌ 还是不会</button>
                <button class="btn-success" @click="confirmRight">✅ 本次掌握</button>
              </div>
            </div>

            <!-- 导航 -->
            <div class="review-nav" v-if="reviewQueue.length > 1">
              <button class="btn-secondary" @click="prevReviewQuestion" :disabled="reviewIndex === 0">← 上一题</button>
              <button class="btn-secondary" @click="nextReviewQuestion" :disabled="reviewIndex === reviewQueue.length - 1">下一题 →</button>
            </div>
          </template>
        </div>
      </div>
    </div>

    <!-- AI答疑弹窗 -->
    <div v-if="showAIChat && aiChatQuestion" class="modal-overlay" @click.self="closeAIChat">
      <div class="modal-content ai-modal">
        <div class="modal-header">
          <h2>🤖 AI答疑</h2>
          <button class="close-btn" @click="closeAIChat">×</button>
        </div>
        <div class="modal-body ai-chat-body">
          <div class="ai-question-context">
            <div class="context-label">错题：</div>
            <div class="context-text">{{ aiChatQuestion.title }}</div>
          </div>
          <div class="ai-messages" ref="aiMessagesRef">
            <div v-for="(msg, idx) in aiMessages" :key="idx" class="ai-message" :class="msg.role">
              <div class="msg-avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</div>
              <div class="msg-bubble">{{ msg.content }}</div>
            </div>
            <div v-if="aiThinking" class="ai-message assistant">
              <div class="msg-avatar">🤖</div>
              <div class="msg-bubble thinking">AI思考中…</div>
            </div>
          </div>
          <div class="ai-quick-questions">
            <span class="quick-label">快捷提问：</span>
            <button v-for="q in quickQuestions" :key="q" class="quick-btn" @click="sendQuickQuestion(q)">{{ q }}</button>
          </div>
          <div class="ai-input-row">
            <input v-model="aiInputText" type="text" placeholder="向AI提问…" class="ai-input"
              @keyup.enter="sendAIMessage" />
            <button class="btn-primary" @click="sendAIMessage" :disabled="aiThinking">发送</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑错题弹窗 -->
    <div v-if="showEditModal && editingQuestion" class="modal-overlay" @click.self="closeEditModal">
      <div class="modal-content edit-modal">
        <div class="modal-header">
          <h2>编辑错题信息</h2>
          <button class="close-btn" @click="closeEditModal">×</button>
        </div>
        <div class="modal-body">
          <div class="edit-field">
            <label>错误原因标签</label>
            <div class="reason-selector">
              <span v-for="r in errorReasonOptions.filter(x => x.value !== '')" :key="r.value"
                class="reason-select-chip"
                :class="{ active: editingQuestion.errorReasons.includes(r.value) }"
                @click="toggleEditReason(r.value)">
                {{ r.label }}
              </span>
            </div>
          </div>
          <div class="edit-field">
            <label>个人笔记</label>
            <textarea v-model="editingQuestion.note" class="edit-textarea" placeholder="记录你的学习心得、易错点…" rows="4"></textarea>
          </div>
          <div class="edit-field">
            <label>星标</label>
            <label class="toggle-label">
              <input type="checkbox" v-model="editingQuestion.isHighlight" />
              <span>{{ editingQuestion.isHighlight ? '已星标' : '点击星标' }}</span>
            </label>
          </div>
          <div class="modal-footer-btns">
            <button class="btn-secondary" @click="closeEditModal">取消</button>
            <button class="btn-primary" @click="saveEdit">保存</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 手动添加错题弹窗 -->
    <div v-if="showAddModal" class="modal-overlay" @click.self="showAddModal = false">
      <div class="modal-content edit-modal">
        <div class="modal-header">
          <h2>手动添加错题</h2>
          <button class="close-btn" @click="showAddModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="edit-field">
            <label>题目标题 *</label>
            <input v-model="newQuestion.title" type="text" class="edit-input" placeholder="输入题目…" />
          </div>
          <div class="edit-field">
            <label>题目内容</label>
            <textarea v-model="newQuestion.content" class="edit-textarea" placeholder="题目详细描述…" rows="3"></textarea>
          </div>
          <div class="edit-field">
            <label>正确答案</label>
            <textarea v-model="newQuestion.correctAnswer" class="edit-textarea" placeholder="参考答案…" rows="3"></textarea>
          </div>
          <div class="edit-field">
            <label>知识点</label>
            <input v-model="newQuestion.knowledgePoint" type="text" class="edit-input" placeholder="如：JVM、Spring…" />
          </div>
          <div class="edit-field">
            <label>错误原因</label>
            <div class="reason-selector">
              <span v-for="r in errorReasonOptions.filter(x => x.value !== '')" :key="r.value"
                class="reason-select-chip"
                :class="{ active: newQuestion.errorReasons.includes(r.value) }"
                @click="toggleNewReason(r.value)">
                {{ r.label }}
              </span>
            </div>
          </div>
          <div class="modal-footer-btns">
            <button class="btn-secondary" @click="showAddModal = false">取消</button>
            <button class="btn-primary" @click="addManualQuestion">添加错题</button>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import Sidebar from '../components/Sidebar.vue';
import { wrongBookApi, type WrongBookItem } from '../api/wrongBook';
import { knowledgeApi } from '../api/knowledge';

// ==================== 类型定义 ====================
interface WrongQuestion {
  id: number;
  title: string;
  content?: string;
  position: string;
  source: string;
  questionType: string;
  difficulty: number;
  knowledgePoint: string;
  errorReasons: string[];
  userAnswer: string;
  correctAnswer: string;
  assessmentFocus?: string;
  wrongCount: number;
  lastWrongTime: string;
  nextReviewTime?: string;
  masteredTime?: string;
  isMastered: boolean;
  isHighlight: boolean;
  note: string;
  reviewDay?: number;
  interval?: number;
  reviewHistory: { date: string; result: 'wrong' | 'right' }[];
}

// ==================== 响应式数据 ====================
const activeTab = ref<'active' | 'mastered' | 'analysis' | 'plan'>('active');
const searchQuery = ref('');
const filterReason = ref('');
const filterTimeRange = ref('');
const filterPosition = ref('');
const currentPage = ref(1);
const pageSize = 8;

const showReviewModal = ref(false);
const showAIChat = ref(false);
const showEditModal = ref(false);
const showAddModal = ref(false);

const reviewMode = ref<'sequential' | 'random'>('sequential');
const reviewQueue = ref<WrongQuestion[]>([]);
const reviewIndex = ref(0);
const reviewQuestion = computed(() => reviewQueue.value[reviewIndex.value] || null);
const reviewUserAnswer = ref('');
const showReviewAnswer = ref(false);

const aiChatQuestion = ref<WrongQuestion | null>(null);
const aiMessages = ref<{ role: 'user' | 'assistant'; content: string }[]>([]);
const aiInputText = ref('');
const aiThinking = ref(false);

const editingQuestion = ref<WrongQuestion | null>(null);
const editingOriginalIndex = ref(-1);

const ebbinghausEnabled = ref(true);

const newQuestion = ref({
  title: '',
  content: '',
  correctAnswer: '',
  knowledgePoint: '',
  errorReasons: [] as string[]
});

// ==================== 常量配置 ====================
const tabs = [
  { id: 'active', label: '活跃错题', icon: '📚' },
  { id: 'mastered', label: '已掌握', icon: '✅' },
  { id: 'analysis', label: '数据分析', icon: '📊' },
  { id: 'plan', label: '复习计划', icon: '📅' }
];

const errorReasonOptions = [
  { label: '全部原因', value: '' },
  { label: '知识点遗忘', value: 'forget' },
  { label: '审题失误', value: 'misread' },
  { label: '思路错误', value: 'wrong_approach' },
  { label: '计算/代码错误', value: 'calc_error' },
  { label: '表达不清', value: 'expression' },
  { label: '知识盲区', value: 'blind_spot' }
];

const timeRangeOptions = [
  { label: '全部时间', value: '' },
  { label: '今天', value: 'today' },
  { label: '近一周', value: 'week' },
  { label: '近一月', value: 'month' }
];

const positionOptions = computed(() => {
  const set = new Set<string>();
  wrongQuestions.value.forEach((q) => {
    const p = String(q.position || '').trim();
    if (p) {
      set.add(p);
    }
  });
  return [
    { label: '全部岗位', value: '' },
    ...Array.from(set).map((p) => ({ label: p, value: p })),
  ];
});

const quickQuestions = [
  '为什么我答错了？',
  '这道题考察什么知识点？',
  '有没有同类型的题目？',
  '如何避免此类错误？'
];

const wrongQuestions = ref<WrongQuestion[]>([]);

const mapWrongBookItem = (item: WrongBookItem): WrongQuestion => {
  const dateOnly = String(item.last_wrong_time || '').split('T')[0] || TODAY();
  const interval = item.wrong_count >= 3 ? 1 : item.wrong_count === 2 ? 3 : 7;
  return {
    id: item.id,
    title: item.title,
    content: item.content || '',
    position: item.job_position || '未知岗位',
    source: item.source || '题库',
    questionType: item.question_type || '简答题',
    difficulty: Number(item.difficulty || 3),
    knowledgePoint: item.knowledge_point || item.question_type || '其他',
    assessmentFocus: item.assessment_focus || '',
    errorReasons: ['wrong_approach'],
    userAnswer: item.user_answer || '',
    correctAnswer: item.correct_answer || '',
    wrongCount: Number(item.wrong_count || 1),
    lastWrongTime: dateOnly,
    nextReviewTime: dateOnly,
    isMastered: false,
    isHighlight: false,
    note: '',
    reviewDay: 1,
    interval,
    reviewHistory: [],
  };
};

const loadWrongBook = async () => {
  try {
    const res = await wrongBookApi.list();
    wrongQuestions.value = (res.items || []).map(mapWrongBookItem);
    currentPage.value = 1;
  } catch (e) {
    console.error('[WrongQuestionBook] load wrong book failed', e);
    alert('加载错题本失败，请稍后重试');
  }
};

// ==================== 计算属性 ====================
const activeQuestions = computed(() => wrongQuestions.value.filter(q => !q.isMastered));
const masteredQuestions = computed(() => wrongQuestions.value.filter(q => q.isMastered));

const hasActiveFilters = computed(() =>
  !!(searchQuery.value || filterReason.value || filterTimeRange.value || filterPosition.value)
);

const filteredActiveQuestions = computed(() => {
  let list = activeQuestions.value;
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase();
    list = list.filter(item => item.title.toLowerCase().includes(q) || item.knowledgePoint.toLowerCase().includes(q));
  }
  if (filterReason.value) {
    list = list.filter(item => item.errorReasons.includes(filterReason.value));
  }
  if (filterPosition.value) {
    list = list.filter(item => item.position === filterPosition.value);
  }
  if (filterTimeRange.value) {
    const now = new Date();
    list = list.filter(item => {
      const d = new Date(item.lastWrongTime);
      const diff = (now.getTime() - d.getTime()) / (1000 * 60 * 60 * 24);
      if (filterTimeRange.value === 'today') return diff < 1;
      if (filterTimeRange.value === 'week') return diff <= 7;
      if (filterTimeRange.value === 'month') return diff <= 30;
      return true;
    });
  }
  return list;
});

const activeTotalPages = computed(() => Math.ceil(filteredActiveQuestions.value.length / pageSize));
const paginatedActiveQuestions = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filteredActiveQuestions.value.slice(start, start + pageSize);
});

const stats = computed(() => ({
  totalWrong: activeQuestions.value.length,
  highFreq: activeQuestions.value.filter(q => q.wrongCount >= 3).length,
  mastered: masteredQuestions.value.length,
  thisWeek: activeQuestions.value.filter(q => {
    const d = new Date(q.lastWrongTime);
    const diff = (Date.now() - d.getTime()) / (1000 * 60 * 60 * 24);
    return diff <= 7;
  }).length,
  thisMonth: activeQuestions.value.filter(q => {
    const d = new Date(q.lastWrongTime);
    const diff = (Date.now() - d.getTime()) / (1000 * 60 * 60 * 24);
    return diff <= 30;
  }).length,
  masteredThisMonth: masteredQuestions.value.filter(q => {
    if (!q.masteredTime) return false;
    const d = new Date(q.masteredTime);
    const diff = (Date.now() - d.getTime()) / (1000 * 60 * 60 * 24);
    return diff <= 30;
  }).length
}));

const topWrongQuestions = computed(() =>
  [...activeQuestions.value].sort((a, b) => b.wrongCount - a.wrongCount).slice(0, 5)
);

const reasonStats = computed(() => {
  const total = activeQuestions.value.length;
  const counts: Record<string, number> = {};
  const colors: Record<string, string> = {
    forget: '#ff7875',
    misread: '#ffa940',
    wrong_approach: '#ff4d4f',
    calc_error: '#faad14',
    expression: '#36cfc9',
    blind_spot: '#9254de'
  };
  activeQuestions.value.forEach(q => {
    q.errorReasons.forEach(r => {
      counts[r] = (counts[r] || 0) + 1;
    });
  });
  return Object.entries(counts)
    .map(([reason, count]) => ({
      reason,
      count,
      pct: total > 0 ? Math.round((count / total) * 100) : 0,
      color: colors[reason] || '#666'
    }))
    .sort((a, b) => b.count - a.count);
});

const knowledgeStats = computed(() => {
  const map: Record<string, { wrong: number; total: number }> = {};
  activeQuestions.value.forEach(q => {
    const kp = q.knowledgePoint;
    if (!map[kp]) map[kp] = { wrong: 0, total: 0 };
    map[kp].wrong += 1;
    map[kp].total += 1;
  });
  return Object.entries(map).map(([name, data]) => ({
    name,
    wrongCount: data.wrong,
    total: data.total,
    errorRate: Math.round((data.wrong / Math.max(data.total, 1)) * 100)
  })).sort((a, b) => b.errorRate - a.errorRate);
});

const weakestKnowledge = computed(() => {
  if (knowledgeStats.value.length === 0) return '暂无数据';
  return knowledgeStats.value[0]?.name ?? '暂无数据';
});

const topErrorReason = computed(() => {
  if (reasonStats.value.length === 0) return '暂无数据';
  return getReasonLabel(reasonStats.value[0]?.reason ?? '');
});

const improvementSuggestions = computed(() => {
  const suggestions: string[] = [];
  const topKp = knowledgeStats.value[0];
  if (topKp) suggestions.push(`${topKp.name} 知识点错误率 ${topKp.errorRate}%，建议每天复习 5 道相关错题 + 10 道同类题`);
  const topReason = reasonStats.value[0];
  if (topReason?.reason === 'forget') suggestions.push('主要错误原因为"知识点遗忘"，建议开启艾宾浩斯遗忘曲线复习计划');
  if (topReason?.reason === 'wrong_approach') suggestions.push('主要错误原因为"思路错误"，建议在复习时先口述解题框架再作答');
  if (activeQuestions.value.filter(q => q.wrongCount >= 3).length > 0) suggestions.push(`有 ${stats.value.highFreq} 道高频错题（答错≥3次），建议优先集中攻克`);
  suggestions.push('建议每天安排 30 分钟专项错题复习，坚持 2 周可显著提升正确率');
  return suggestions;
});

const todayReviewPlan = computed(() => {
  const today = new Date().toISOString().split('T')[0] ?? '';
  return activeQuestions.value.filter(q => q.nextReviewTime && today && q.nextReviewTime <= today);
});

const aiReviewPlan = ref([
  {
    tasks: ['复习JVM内存模型（5道错题）', '刷10道JVM同类题'],
    goal: '攻克JVM薄弱知识点'
  },
  {
    tasks: ['复习Redis分布式锁（4道错题）', '对比Redisson实现原理'],
    goal: '掌握Redis高频面试题'
  },
  {
    tasks: ['复习MySQL索引（3道错题）', '手写explain分析流程'],
    goal: '强化数据库优化能力'
  },
  {
    tasks: ['全面回顾本周所有错题', '针对高频错题强化记忆'],
    goal: '巩固本周学习成果'
  },
  {
    tasks: ['模拟面试 1 次，重点考察薄弱知识点', '整理错题笔记'],
    goal: '检验学习效果'
  }
]);

const TODAY = () => new Date().toISOString().split('T')[0] as string;
const getDifficultyLabel = (d: number) => ({ 1: '入门', 2: '基础', 3: '中等', 4: '进阶', 5: '大厂真题' }[d] || '未知');
const getReasonLabel = (r: string) => errorReasonOptions.find(o => o.value === r)?.label || r;
const getErrorRateColor = (rate: number) => rate >= 70 ? '#ff4d4f' : rate >= 50 ? '#faad14' : '#52c41a';

const isReviewDue = (wq: WrongQuestion) => {
  if (!wq.nextReviewTime || !ebbinghausEnabled.value) return false;
  const today = new Date().toISOString().split('T')[0] ?? '';
  return !!today && wq.nextReviewTime <= today;
};

const toggleFilter = (type: string, value: string) => {
  currentPage.value = 1;
  if (type === 'reason') filterReason.value = filterReason.value === value ? '' : value;
  else if (type === 'time') filterTimeRange.value = filterTimeRange.value === value ? '' : value;
  else if (type === 'position') filterPosition.value = filterPosition.value === value ? '' : value;
};

const clearFilters = () => {
  searchQuery.value = '';
  filterReason.value = '';
  filterTimeRange.value = '';
  filterPosition.value = '';
  currentPage.value = 1;
};

const markMastered = (wq: WrongQuestion) => {
  const item = wrongQuestions.value.find(q => q.id === wq.id);
  if (item) {
    item.isMastered = true;
    item.masteredTime = new Date().toISOString().split('T')[0];
  }
};

const unmarkMastered = (wq: WrongQuestion) => {
  const item = wrongQuestions.value.find(q => q.id === wq.id);
  if (item) {
    item.isMastered = false;
    item.masteredTime = undefined;
  }
};

// 复习模式
const startReview = (mode: 'sequential' | 'random') => {
  reviewMode.value = mode;
  const queue = mode === 'random'
    ? [...filteredActiveQuestions.value].sort(() => Math.random() - 0.5)
    : [...filteredActiveQuestions.value];
  reviewQueue.value = queue;
  reviewIndex.value = 0;
  reviewUserAnswer.value = '';
  showReviewAnswer.value = false;
  showReviewModal.value = true;
};

const openReviewQuestion = async (wq: WrongQuestion) => {
  let next = { ...wq };
  try {
    const detail = await knowledgeApi.detail(wq.id);
    next = {
      ...next,
      title: String(detail.question || next.title || '').trim() || next.title,
      content: String(detail.remark || next.content || '').trim(),
      questionType: String(detail.question_type || next.questionType || '').trim() || next.questionType,
      difficulty: Number(detail.difficulty || next.difficulty || 3),
      knowledgePoint: String(detail.question_type || next.knowledgePoint || '').trim() || next.knowledgePoint,
      correctAnswer: String(detail.excellent_answer || next.correctAnswer || '').trim() || next.correctAnswer,
      assessmentFocus: String(detail.answer_points || detail.question_intent || next.assessmentFocus || '').trim() || next.assessmentFocus,
      source: String(detail.job_position || next.position || '').trim() ? `${String(detail.job_position || next.position).trim()}题库` : next.source,
      position: String(detail.job_position || next.position || '').trim() || next.position,
    };
  } catch (e) {
    console.error('[WrongQuestionBook] load detail for review failed', e);
  }

  reviewMode.value = 'sequential';
  reviewQueue.value = [next];
  reviewIndex.value = 0;
  reviewUserAnswer.value = '';
  showReviewAnswer.value = false;
  showReviewModal.value = true;
};

const closeReviewModal = () => {
  showReviewModal.value = false;
};

const prevReviewQuestion = () => {
  if (reviewIndex.value > 0) {
    reviewIndex.value--;
    reviewUserAnswer.value = '';
    showReviewAnswer.value = false;
  }
};

const nextReviewQuestion = () => {
  if (reviewIndex.value < reviewQueue.value.length - 1) {
    reviewIndex.value++;
    reviewUserAnswer.value = '';
    showReviewAnswer.value = false;
  }
};

const submitReviewAnswer = () => {
  showReviewAnswer.value = true;
};

const confirmRight = () => {
  const wq = reviewQuestion.value;
  if (wq) {
    const item = wrongQuestions.value.find(q => q.id === wq.id);
    if (item) item.reviewHistory.push({ date: TODAY(), result: 'right' });
  }
  if (reviewIndex.value < reviewQueue.value.length - 1) {
    nextReviewQuestion();
  } else {
    closeReviewModal();
  }
};

const confirmWrong = () => {
  const wq = reviewQuestion.value;
  if (wq) {
    const item = wrongQuestions.value.find(q => q.id === wq.id);
    if (item) {
      item.wrongCount++;
      item.reviewHistory.push({ date: TODAY(), result: 'wrong' });
    }
  }
  if (reviewIndex.value < reviewQueue.value.length - 1) {
    nextReviewQuestion();
  } else {
    closeReviewModal();
  }
};

// AI答疑
const openAIChat = (wq: WrongQuestion) => {
  aiChatQuestion.value = wq;
  aiMessages.value = [
    {
      role: 'assistant',
      content: `我注意到你在"${wq.title}"这道题上已经答错了 ${wq.wrongCount} 次，主要错误原因是${wq.errorReasons.map(getReasonLabel).join('、')}。你可以直接问我哪里没理解，我来帮你分析！`
    }
  ];
  showAIChat.value = true;
};

const closeAIChat = () => {
  showAIChat.value = false;
  aiChatQuestion.value = null;
  aiMessages.value = [];
};

const sendAIMessage = () => {
  if (!aiInputText.value.trim() || aiThinking.value) return;
  aiMessages.value.push({ role: 'user', content: aiInputText.value });
  const userMsg = aiInputText.value;
  aiInputText.value = '';
  aiThinking.value = true;
  setTimeout(() => {
    aiMessages.value.push({
      role: 'assistant',
      content: generateAIResponse(userMsg, aiChatQuestion.value)
    });
    aiThinking.value = false;
  }, 1200);
};

const sendQuickQuestion = (q: string) => {
  aiInputText.value = q;
  sendAIMessage();
};

const generateAIResponse = (question: string, wq: WrongQuestion | null): string => {
  if (!wq) return '请先选择一道错题再进行提问。';
  if (question.includes('为什么') && question.includes('答错')) {
    return `分析你对"${wq.title}"的错误：你的答案"${wq.userAnswer.slice(0, 30)}…"，主要问题是${wq.errorReasons.map(getReasonLabel).join('和')}。建议：先梳理知识框架，再结合正确答案中的关键点逐条理解。`;
  }
  if (question.includes('知识点')) {
    return `这道题考察的核心知识点是【${wq.knowledgePoint}】。在面试中，面试官通常会从以下角度深入追问：1. 基础概念定义 2. 底层实现原理 3. 与相关技术的对比 4. 实际应用场景和踩坑经验。`;
  }
  if (question.includes('同类') || question.includes('类似')) {
    return `与"${wq.title}"相关的同类题型有：\n1. 深入追问类：如原理、源码分析\n2. 对比类：如同类技术对比\n3. 场景题：如实际工作中如何应用\n建议你在掌握本题后，主动扩展这些相关问题。`;
  }
  if (question.includes('避免') || question.includes('如何')) {
    return `针对你的错误原因"${wq.errorReasons.map(getReasonLabel).join('、')}"，建议：\n1. 建立知识体系思维导图，避免孤立记忆\n2. 用"费曼学习法"：假设向他人解释这个知识点\n3. 多做相关题目巩固\n4. 建立错题本（你已经在做了！）定期复习`;
  }
  return `关于"${wq.title}"：${wq.correctAnswer.slice(0, 100)}… 记住核心要点是${wq.knowledgePoint}的基础知识，建议结合官方文档或源码加深理解。`;
};

// 编辑
const openEditModal = (wq: WrongQuestion) => {
  editingOriginalIndex.value = wrongQuestions.value.findIndex(q => q.id === wq.id);
  editingQuestion.value = JSON.parse(JSON.stringify(wq));
  showEditModal.value = true;
};

const closeEditModal = () => {
  showEditModal.value = false;
  editingQuestion.value = null;
};

const toggleEditReason = (reason: string) => {
  if (!editingQuestion.value) return;
  const idx = editingQuestion.value.errorReasons.indexOf(reason);
  if (idx === -1) editingQuestion.value.errorReasons.push(reason);
  else editingQuestion.value.errorReasons.splice(idx, 1);
};

const saveEdit = () => {
  if (editingOriginalIndex.value !== -1 && editingQuestion.value) {
    wrongQuestions.value[editingOriginalIndex.value] = editingQuestion.value;
  }
  closeEditModal();
};

// 手动添加
const toggleNewReason = (reason: string) => {
  const idx = newQuestion.value.errorReasons.indexOf(reason);
  if (idx === -1) newQuestion.value.errorReasons.push(reason);
  else newQuestion.value.errorReasons.splice(idx, 1);
};

const addManualQuestion = () => {
  if (!newQuestion.value.title.trim()) {
    alert('请填写题目标题');
    return;
  }
  const maxId = Math.max(...wrongQuestions.value.map(q => q.id), 0);
  wrongQuestions.value.push({
    id: maxId + 1,
    title: newQuestion.value.title,
    content: newQuestion.value.content,
    position: '手动添加',
    source: '手动添加',
    questionType: '简答题',
    difficulty: 3,
    knowledgePoint: newQuestion.value.knowledgePoint || '其他',
    errorReasons: newQuestion.value.errorReasons.length ? newQuestion.value.errorReasons : ['forget'],
    userAnswer: '',
    correctAnswer: newQuestion.value.correctAnswer,
    wrongCount: 1,
    lastWrongTime: TODAY(),
    isMastered: false,
    isHighlight: false,
    note: '',
    reviewHistory: []
  });
  newQuestion.value = { title: '', content: '', correctAnswer: '', knowledgePoint: '', errorReasons: [] };
  showAddModal.value = false;
};

const exportWrongQuestions = () => {
  const data = filteredActiveQuestions.value.map(q => ({
    题目: q.title,
    来源: q.source,
    知识点: q.knowledgePoint,
    错误原因: q.errorReasons.map(getReasonLabel).join('、'),
    答错次数: q.wrongCount,
    正确答案: q.correctAnswer,
    我的笔记: q.note,
    最近答错时间: q.lastWrongTime
  }));
  const jsonStr = JSON.stringify(data, null, 2);
  const blob = new Blob(['\uFEFF' + jsonStr], { type: 'application/json;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `错题本_${new Date().toISOString().split('T')[0]}.json`;
  a.click();
  URL.revokeObjectURL(url);
};

const refreshAIPlan = () => {
  aiReviewPlan.value = aiReviewPlan.value.map(item => ({ ...item }));
};

onMounted(() => {
  void loadWrongBook();
});
</script>

<style scoped>
/* ==================== 基础布局 ==================== */
.page-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: var(--hp-surface);
}

.main-content {
  flex: 1;
  margin-left: clamp(200px, 25vw, 320px);
  overflow-y: auto;
  height: 100vh;
  background:
    radial-gradient(55% 60% at 50% -10%, rgba(41, 99, 220, 0.2), transparent 70%),
    #080d19 !important;
}

.page-container {
  padding: clamp(20px, 4vh, 32px) clamp(16px, 3vw, 24px);
  max-width: 1400px;
  margin: 0 auto;
  min-height: 100%;
  background: transparent;
}

/* ==================== 页面头部 ==================== */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 24px;
}

.header-left h1 {
  font-size: clamp(24px, 4vw, 32px);
  font-weight: 700;
  color: #333;
  margin: 0 0 6px;
}

.header-sub {
  font-size: 14px;
  color: #888;
  margin: 0;
}

.header-stats {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.stat-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  min-width: 80px;
}

.stat-card.warn .stat-num { color: var(--hp-yellow-ink); }
.stat-card.success .stat-num { color: #3e603b; }
.stat-card.info .stat-num { color: var(--hp-pink-ink); }

.stat-num {
  font-size: 22px;
  font-weight: 700;
  color: #ff4d4f;
}

.stat-lbl {
  font-size: 12px;
  color: #888;
}

/* ==================== Tab导航 ==================== */
.tab-nav {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e8e8e8;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.25s;
}

.tab-btn:hover {
  border-color: #3a7bc8;
  color: #3a7bc8;
}

.tab-btn.active {
  background: #3a7bc8;
  border-color: #3a7bc8;
  color: white;
  font-weight: 600;
}

.tab-icon { font-size: 16px; }

.tab-right-actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
}

.action-btn-outline {
  padding: 8px 16px;
  background: white;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 13px;
  color: #555;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn-outline:hover {
  border-color: #3a7bc8;
  color: #3a7bc8;
}

/* ==================== 筛选面板 ==================== */
.filter-panel {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.search-row {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.search-input {
  flex: 1;
  min-width: 200px;
  padding: 10px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.search-input:focus { border-color: #3a7bc8; }

.filter-rows {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.filter-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-label {
  font-size: 13px;
  color: #888;
  min-width: 60px;
  padding-top: 6px;
  flex-shrink: 0;
}

.filter-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  flex: 1;
  align-items: center;
}

.filter-chip {
  padding: 5px 14px;
  background: #f5f5f5;
  border: 1px solid #e0e0e0;
  border-radius: 20px;
  font-size: 12px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}

.filter-chip:hover {
  background: #e6f0fe;
  border-color: #3a7bc8;
  color: #3a7bc8;
}

.filter-chip.active {
  background: #3a7bc8;
  border-color: #3a7bc8;
  color: white;
}

.filter-divider {
  color: #d9d9d9;
  padding: 0 4px;
}

.filter-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.summary-text { font-size: 13px; color: #3a7bc8; }

.clear-btn {
  padding: 4px 12px;
  background: transparent;
  border: 1px solid #ddd;
  border-radius: 16px;
  font-size: 12px;
  color: #888;
  cursor: pointer;
  transition: all 0.2s;
}

.clear-btn:hover { border-color: #ff4d4f; color: #ff4d4f; }

/* ==================== 错题列表（表格式） ==================== */
.q-table {
  background: white;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
  overflow: hidden;
  margin-bottom: 20px;
}

.q-table-head {
  display: flex;
  align-items: center;
  padding: 0 14px;
  height: 44px;
  background: #f7f8fa;
  border-bottom: 1px solid #e8e8e8;
  font-size: 13px;
  color: #999;
  font-weight: 500;
}

.q-row {
  display: flex;
  align-items: center;
  padding: 0 14px;
  height: 56px;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
  color: #333;
}

.q-row:last-child { border-bottom: none; }
.q-row:hover { background: #fafafa; }
.q-row.row-due { background: #fffbe6; }
.q-row.q-row-mastered { opacity: 0.65; }

/* 列宽度 */
.col-status { width: 36px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; }
.col-title  { flex: 0 1 38%; min-width: 0; display: flex; align-items: center; gap: 8px; overflow: hidden; }
.col-knowledge { width: 110px; flex-shrink: 0; font-size: 13px; color: #555; }
.col-source  { width: 170px; flex-shrink: 0; font-size: 12px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.col-count  { width: 96px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; }
.col-diff   { width: 92px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; }
.col-ops    { width: 228px; flex-shrink: 0; display: flex; align-items: center; gap: 6px; justify-content: flex-end; }

/* 题目标题 */
.row-title {
  font-size: 14px;
  color: #333;
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}
.row-title:hover { color: #3a7bc8; }
.row-title-muted { color: #999; cursor: default; }
.row-title-muted:hover { color: #999; }

.row-muted { color: #bbb; }

.due-tag {
  padding: 1px 7px;
  background: #fa8c16;
  color: white;
  border-radius: 8px;
  font-size: 11px;
  flex-shrink: 0;
}

.note-dot { font-size: 13px; flex-shrink: 0; opacity: 0.7; }

/* 状态圆圈 */
.status-circle {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 1.5px solid #d0d0d0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: transparent;
  background: white;
}
.status-circle.highlight {
  border-color: #fa8c16;
  background: #fff7e6;
}
.status-circle.status-done {
  border-color: #52c41a;
  background: #f6ffed;
  color: #52c41a;
}

.metric-pill {
  min-width: 58px;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid #d9e0ec;
  background: #f5f8fe;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
}

.count-pill { color: #5b6f8f; }
.count-pill.count-danger {
  color: #ff4d4f;
  border-color: #ffc1c1;
  background: #fff1f0;
}

.diff-pill.diff-1 { color: #389e0d; border-color: #b7eb8f; background: #f6ffed; }
.diff-pill.diff-2 { color: #52c41a; border-color: #d9f7be; background: #f6ffed; }
.diff-pill.diff-3 { color: #d48806; border-color: #ffe58f; background: #fffbe6; }
.diff-pill.diff-4 { color: #d46b08; border-color: #ffd8bf; background: #fff2e8; }
.diff-pill.diff-5 { color: #cf1322; border-color: #ffa39e; background: #fff1f0; }

/* 行操作按鈕 */
.row-btn {
  padding: 4px 10px;
  background: transparent;
  border: 1px solid #e0e0e0;
  border-radius: 5px;
  font-size: 12px;
  color: #666;
  cursor: pointer;
  white-space: nowrap;
}
.row-btn:hover { background: #f0f5ff; border-color: #3a7bc8; color: #3a7bc8; }
.row-btn-ok   { color: #52c41a; border-color: #b7eb8f; }
.row-btn-ok:hover { background: #f6ffed; border-color: #52c41a; color: #52c41a; }
.row-btn-warn { color: #fa8c16; border-color: #ffd591; }
.row-btn-warn:hover { background: #fff7e6; }

/* ==================== 空状态 ==================== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  background: white;
  border-radius: 12px;
  color: #bbb;
}

.empty-state.small { padding: 24px; border-radius: 8px; }

/* ==================== 已掌握 ==================== */
.mastered-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  font-size: 14px;
  color: #666;
}

.mastered-tip { color: #aaa; font-size: 12px; }

/* ==================== 分页 ==================== */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 16px;
}

.page-btn {
  padding: 8px 20px;
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) { border-color: #3a7bc8; color: #3a7bc8; }
.page-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.page-info { font-size: 14px; color: #666; }

/* ==================== 数据分析 ==================== */
.analysis-tab, .plan-tab {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.analysis-section, .plan-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 16px;
}

/* 错误原因柱状图 */
.reason-chart { display: flex; flex-direction: column; gap: 12px; }

.reason-bar-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.reason-name { font-size: 13px; color: #555; min-width: 90px; }

.bar-track {
  flex: 1;
  height: 14px;
  background: #f5f5f5;
  border-radius: 7px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 7px;
  transition: width 0.6s ease;
}

.bar-num { font-size: 12px; color: #888; min-width: 80px; text-align: right; }

/* 知识点卡片 */
.knowledge-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}

.knowledge-card {
  padding: 16px;
  background: #fafafa;
  border-radius: 10px;
  border: 1px solid #f0f0f0;
  transition: all 0.2s;
}

.knowledge-card.weakness { border-color: #ffccc7; background: #fff9f9; }

.kp-name { font-size: 14px; font-weight: 600; color: #333; margin-bottom: 10px; }

.kp-rate-bar {
  height: 8px;
  background: #f5f5f5;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 8px;
}

.kp-fill { height: 100%; border-radius: 4px; }
.kp-rate-text { font-size: 14px; font-weight: 700; margin-bottom: 4px; }
.kp-count { font-size: 12px; color: #aaa; }

/* Top错题 */
.top-list { display: flex; flex-direction: column; gap: 10px; }

.top-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #fafafa;
  border-radius: 8px;
}

.top-rank {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 700;
  background: #f0f0f0;
  color: #888;
  flex-shrink: 0;
}

.top-rank.rank-1 { background: #ff4d4f; color: white; }
.top-rank.rank-2 { background: #fa8c16; color: white; }
.top-rank.rank-3 { background: #faad14; color: white; }

.top-title { flex: 1; font-size: 14px; color: #333; }
.top-count { font-size: 13px; font-weight: 600; }
.top-count.danger { color: #ff4d4f; }

.mini-btn {
  padding: 4px 12px;
  background: #3a7bc8;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.2s;
}

.mini-btn:hover { background: #2e6ab5; }

/* 月度报告 */
.monthly-report {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}

.report-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 10px;
}

.report-icon { font-size: 28px; }
.report-title { font-size: 12px; color: #888; margin-bottom: 4px; }
.report-num { font-size: 18px; font-weight: 700; color: #333; }

.report-suggestion {
  padding: 16px 20px;
  background: #f0f9ff;
  border-radius: 10px;
  border-left: 4px solid #1890ff;
}

.report-suggestion h4 { font-size: 14px; color: #1890ff; margin: 0 0 10px; }
.report-suggestion ul { margin: 0; padding: 0 0 0 20px; }
.report-suggestion li { font-size: 13px; color: #555; line-height: 2; }

/* ==================== 复习计划 ==================== */
.plan-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.plan-header-row .section-title { margin: 0; }

.plan-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
}

.plan-toggle input { cursor: pointer; }

.ebbinghaus-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 18px;
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 10px;
  margin-bottom: 10px;
}

.eb-left { flex-shrink: 0; }

.eb-day-badge {
  padding: 4px 10px;
  background: #fa8c16;
  color: white;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
}

.eb-body { flex: 1; }
.eb-title { font-size: 14px; font-weight: 600; color: #333; margin-bottom: 4px; }
.eb-meta { font-size: 12px; color: #aaa; }

/* AI计划 */
.ai-plan-area { display: flex; flex-direction: column; }

.ai-plan-intro {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
  padding: 14px 18px;
  background: #f0f9ff;
  border-radius: 10px;
}

.ai-avatar { font-size: 28px; flex-shrink: 0; }
.ai-intro-text p { margin: 0; font-size: 14px; color: #555; }

.ai-plan-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}

.plan-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 14px 18px;
  background: #fafafa;
  border-radius: 10px;
  border: 1px solid #f0f0f0;
}

.plan-day {
  font-size: 13px;
  font-weight: 700;
  color: #3a7bc8;
  min-width: 45px;
  padding-top: 4px;
  flex-shrink: 0;
}

.plan-tasks {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.plan-task {
  padding: 4px 12px;
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 20px;
  font-size: 12px;
  color: #555;
}

.plan-goal {
  font-size: 12px;
  color: #888;
  min-width: 120px;
  padding-top: 6px;
  flex-shrink: 0;
}

.ebbinghaus-info { display: flex; flex-direction: column; }

/* ==================== 弹窗公用 ==================== */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.modal-content {
  background: white;
  border-radius: 16px;
  width: 100%;
  max-width: 760px;
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.modal-header-left { display: flex; align-items: center; gap: 12px; }
.modal-header h2 { font-size: 18px; font-weight: 600; color: #333; margin: 0; }

.review-progress {
  font-size: 13px;
  color: #888;
  padding: 3px 10px;
  background: #f0f0f0;
  border-radius: 20px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 26px;
  color: #aaa;
  cursor: pointer;
  line-height: 1;
  transition: color 0.2s;
}

.close-btn:hover { color: #333; }

.modal-body {
  padding: 24px;
  overflow-y: auto;
  max-height: calc(90vh - 72px);
}

/* ==================== 复习弹窗 ==================== */
.review-modal { max-width: 800px; }

.review-question-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.review-tags { display: flex; flex-wrap: wrap; gap: 6px; }

.review-wrong-info { display: flex; gap: 12px; }

.info-item {
  font-size: 12px;
  color: #888;
  padding: 3px 10px;
  background: #f5f5f5;
  border-radius: 6px;
}

.info-item.warn { background: #fff2f0; color: #ff4d4f; }

.review-title {
  font-size: 17px;
  font-weight: 600;
  color: #333;
  line-height: 1.5;
  margin-bottom: 12px;
}

.review-content {
  font-size: 14px;
  color: #555;
  line-height: 1.8;
  padding: 14px 16px;
  background: #fafafa;
  border-radius: 8px;
  margin-bottom: 16px;
}

.review-core-blocks {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.history-wrong {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  padding: 10px 14px;
  background: #fff9f0;
  border-radius: 8px;
  flex-wrap: wrap;
}

.history-title { font-size: 12px; color: #888; flex-shrink: 0; }

.error-reason-tags { display: flex; flex-wrap: wrap; gap: 6px; }

.reason-tag {
  padding: 2px 10px;
  background: #fff0f0;
  color: #ff4d4f;
  border-radius: 10px;
  font-size: 12px;
}

.answer-area h4 { font-size: 14px; font-weight: 600; color: #333; margin: 0 0 10px; }

.answer-textarea {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.6;
  resize: vertical;
  box-sizing: border-box;
  outline: none;
  transition: border-color 0.2s;
}

.answer-textarea:focus { border-color: #3a7bc8; }

.answer-btns { display: flex; gap: 10px; margin-top: 12px; }

.answer-compare { display: flex; flex-direction: column; gap: 14px; }

.compare-block {
  padding: 14px 16px;
  border-radius: 8px;
}

.compare-block.user-ans { background: #fff9f0; border: 1px solid #ffe58f; }
.compare-block.correct-ans { background: #f6ffed; border: 1px solid #b7eb8f; }
.compare-block.note-block { background: #f0f9ff; border: 1px solid #91d5ff; }
.compare-block.question-full { background: #f6f8ff; border: 1px solid #c9d8ff; }
.compare-block.focus-block { background: #f5fff8; border: 1px solid #b7ebc6; }

.compare-label { font-size: 12px; font-weight: 600; color: #888; margin-bottom: 6px; }
.compare-content { font-size: 14px; color: #333; line-height: 1.7; }
.preserve-line { white-space: pre-wrap; }

.review-result-btns { display: flex; gap: 10px; justify-content: center; margin-top: 4px; }

.review-nav {
  display: flex;
  justify-content: space-between;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

/* ==================== AI弹窗 ==================== */
.ai-modal { max-width: 680px; }

.ai-chat-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: calc(90vh - 80px);
  overflow: hidden;
}

.ai-question-context {
  padding: 12px 16px;
  background: #f0f9ff;
  border-radius: 8px;
  font-size: 13px;
}

.context-label { color: #888; margin-bottom: 4px; }
.context-text { color: #333; font-weight: 500; }

.ai-messages {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 200px;
  max-height: 320px;
  padding: 4px 0;
}

.ai-message {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.ai-message.user { flex-direction: row-reverse; }

.msg-avatar { font-size: 20px; flex-shrink: 0; }

.msg-bubble {
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  max-width: 75%;
}

.ai-message.assistant .msg-bubble {
  background: #f5f5f5;
  color: #333;
  border-radius: 4px 12px 12px 12px;
}

.ai-message.user .msg-bubble {
  background: #3a7bc8;
  color: white;
  border-radius: 12px 4px 12px 12px;
}

.msg-bubble.thinking {
  color: #aaa;
  font-style: italic;
}

.ai-quick-questions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.quick-label { font-size: 12px; color: #aaa; flex-shrink: 0; }

.quick-btn {
  padding: 5px 12px;
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 16px;
  font-size: 12px;
  color: #555;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-btn:hover { border-color: #3a7bc8; color: #3a7bc8; }

.ai-input-row {
  display: flex;
  gap: 10px;
}

.ai-input {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.ai-input:focus { border-color: #3a7bc8; }

/* ==================== 编辑弹窗 ==================== */
.edit-modal { max-width: 560px; }

.edit-field { margin-bottom: 20px; }

.edit-field label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #555;
  margin-bottom: 8px;
}

.edit-input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.2s;
}

.edit-input:focus { border-color: #3a7bc8; }

.edit-textarea {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  resize: vertical;
  box-sizing: border-box;
  line-height: 1.6;
  transition: border-color 0.2s;
}

.edit-textarea:focus { border-color: #3a7bc8; }

.reason-selector { display: flex; flex-wrap: wrap; gap: 8px; }

.reason-select-chip {
  padding: 6px 14px;
  background: #f5f5f5;
  border: 1px solid #e0e0e0;
  border-radius: 20px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.reason-select-chip:hover { border-color: #3a7bc8; color: #3a7bc8; }

.reason-select-chip.active {
  background: #ff4d4f;
  border-color: #ff4d4f;
  color: white;
}

.toggle-label {
  display: flex !important;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #555;
  font-weight: 400 !important;
}

.modal-footer-btns {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 8px;
}

/* ==================== 通用按钮 ==================== */
.btn-primary {
  padding: 10px 22px;
  background: #3a7bc8;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-primary:hover:not(:disabled) { background: #2e6ab5; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-primary.btn-sm { padding: 7px 16px; font-size: 13px; }

.btn-secondary {
  padding: 10px 22px;
  background: white;
  color: #555;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-secondary:hover:not(:disabled) { border-color: #3a7bc8; color: #3a7bc8; }
.btn-secondary:disabled { opacity: 0.5; cursor: not-allowed; }

.btn-danger {
  padding: 10px 22px;
  background: #ff4d4f;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-danger:hover { background: #e03333; }

.btn-success {
  padding: 10px 22px;
  background: #52c41a;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-success:hover { background: #3da812; }

/* ==================== 错题本主题 ==================== */
.main-content {
  background: var(--hp-bg);
}

.header-left h1 { color: var(--hp-ink); }
.header-sub,
.stat-lbl,
.mastered-tip,
.page-info,
.quick-label,
.context-label,
.history-title,
.compare-label,
.plan-goal,
.report-title,
.bar-num,
.kp-count,
.row-muted,
.filter-label,
.top-rank {
  color: var(--hp-muted);
}

.stat-card,
.tab-btn,
.action-btn-outline,
.filter-panel,
.q-table,
.analysis-section,
.plan-section,
.empty-state,
.modal-content,
.plan-item,
.top-item,
.knowledge-card,
.report-card,
.ai-plan-intro,
.ai-question-context {
  background: var(--hp-surface);
  border: 1px solid var(--hp-line);
  box-shadow: none;
}

.empty-state { color: var(--hp-muted); }
.stat-card .stat-num { color: var(--hp-pink-ink); }

.tab-nav,
.filter-summary,
.modal-header,
.review-nav {
  border-color: var(--hp-line);
}

.tab-btn,
.action-btn-outline,
.page-btn,
.btn-secondary,
.quick-btn,
.row-btn,
.filter-chip,
.reason-select-chip,
.plan-task,
.metric-pill,
.review-progress,
.info-item,
.bar-track,
.kp-rate-bar {
  background: var(--hp-surface);
  border-color: var(--hp-line);
  color: var(--hp-ink-2);
}

.tab-btn:hover,
.action-btn-outline:hover,
.btn-secondary:hover:not(:disabled),
.row-btn:hover,
.quick-btn:hover,
.page-btn:hover:not(:disabled),
.filter-chip:hover,
.reason-select-chip:hover {
  background: var(--hp-surface);
  border-color: var(--hp-line-strong);
  color: var(--hp-ink);
}

.tab-btn.active,
.filter-chip.active {
  background: var(--hp-yellow);
  border-color: var(--hp-line-strong);
  color: var(--hp-pink-ink);
}

.search-input,
.answer-textarea,
.ai-input,
.edit-input,
.edit-textarea {
  background: var(--hp-surface);
  border-color: #2a436b;
  color: var(--hp-ink);
}

.search-input::placeholder,
.ai-input::placeholder,
.answer-textarea::placeholder,
.edit-input::placeholder,
.edit-textarea::placeholder {
  color: #6e88b0;
}

.q-table-head {
  background: var(--hp-surface);
  border-bottom-color: var(--hp-line);
  color: #8fa8cd;
}

.q-row {
  border-bottom-color: var(--hp-line);
  color: #dce9ff;
}

.q-row:hover { background: var(--hp-surface); }
.q-row.row-due { background: var(--hp-surface); }

.row-title,
.top-title,
.section-title,
.modal-header h2,
.review-title,
.kp-name,
.compare-content,
.context-text,
.edit-field label,
.toggle-label,
.ai-intro-text p,
.report-num,
.eb-title,
.summary-text {
  color: #e8f2ff;
}

.row-title-muted,
.close-btn {
  color: #8fa8cd;
}

.status-circle {
  border-color: #35598a;
  background: var(--hp-surface);
}

.metric-pill {
  background: var(--hp-surface);
  border-color: #2a4a76;
}

.count-pill { color: #bad2f4; }
.count-pill.count-danger {
  color: var(--hp-ink);
  background: var(--hp-surface);
  border-color: #7d3246;
}

.report-suggestion {
  background: var(--hp-surface);
  border-left-color: #3a8af8;
}

.report-suggestion h4 { color: #8ac7ff; }
.report-suggestion li { color: #b8cff0; }

.compare-block.user-ans { background: var(--hp-surface); border-color: #6b5430; }
.compare-block.correct-ans { background: var(--hp-surface); border-color: #2f6a44; }
.compare-block.note-block { background: var(--hp-surface); border-color: #2b5a91; }
.compare-block.question-full { background: var(--hp-surface); border-color: #365b90; }
.compare-block.focus-block { background: var(--hp-surface); border-color: #2f6a4c; }

.reason-tag { background: var(--hp-surface); color: var(--hp-ink); }

.btn-primary {
  border: 1px solid #3a8af8;
  background: linear-gradient(90deg, #1f69d3, #3a8af8);
  color: #eef7ff;
}

.btn-danger { background: #a0263c; }
.btn-danger:hover { background: #8b1f33; }
.btn-success { background: #267a34; }
.btn-success:hover { background: #1f662c; }

/* ==================== 响应式 ==================== */
@media (max-width: 768px) {
  .page-header { flex-direction: column; }
  .header-stats { justify-content: flex-start; }
  .tab-nav { gap: 6px; }
  .tab-right-actions { width: 100%; margin-left: 0; }
  .search-row { flex-direction: column; }
  .q-table-head { display: none; }
  .q-row { height: auto; padding: 12px 14px; flex-wrap: wrap; gap: 6px; }
  .col-knowledge, .col-source { width: auto; }
  .col-count, .col-diff { width: auto; }
  .col-ops { width: 100%; justify-content: flex-start; margin-top: 4px; }
  .modal-content { max-width: 100%; max-height: 100vh; border-radius: 0; }
}
</style>
