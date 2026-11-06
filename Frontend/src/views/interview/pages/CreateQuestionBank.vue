<template>
  <div class="page-layout">
    <Sidebar />
    <main class="main-content">
      <div class="page-container">

        <!-- 页面标题 + Tab 切换 -->
        <div class="page-header">
          <h1>题库管理</h1>
          <div class="header-right">
            <button class="btn-ai-parse" @click="openAiRecognize">🤖 自动解析题目</button>
            <div class="tab-switch">
              <button
                class="tab-btn"
                :class="{ active: activeTab === 'list' }"
                @click="switchTab('list')"
              >题目列表</button>
              <button
                class="tab-btn"
                :class="{ active: activeTab === 'create' }"
                @click="switchTab('create')"
              >+ 新建题目</button>
            </div>
          </div>
        </div>

        <!-- ======================== Tab: 新建题目 ======================== -->
        <div v-show="activeTab === 'create'" class="form-card">
          <div class="section-title">基本信息</div>

          <div class="form-group">
            <label class="form-label required">岗位</label>
            <input v-model="form.job_position" type="text" class="form-input"
              placeholder="如：Java 开发、前端开发、产品经理" maxlength="100" />
          </div>

          <div class="form-group">
            <label class="form-label required">题目类型</label>
            <div class="tag-select-group">
              <span v-for="qt in questionTypeOptions" :key="qt.value"
                class="sel-tag" :class="{ active: form.question_type === qt.value }"
                @click="form.question_type = qt.value">{{ qt.label }}</span>
              <input
                v-if="!questionTypeOptions.find(q => q.value === form.question_type) && form.question_type !== ''"
                v-model="form.question_type" type="text" class="form-input inline-input"
                placeholder="自定义题型" maxlength="50" />
              <span class="sel-tag custom-tag"
                :class="{ active: !questionTypeOptions.find(q => q.value === form.question_type) && form.question_type !== '' }"
                @click="form.question_type = '__custom__'">自定义</span>
            </div>
            <input v-if="form.question_type === '__custom__'" v-model="form.question_type_custom"
              type="text" class="form-input" style="margin-top:8px"
              placeholder="请输入自定义题型" maxlength="50"
              @input="form.question_type = form.question_type_custom" />
          </div>

          <div class="form-group">
            <label class="form-label required">题干</label>
            <textarea v-model="form.question" class="form-textarea"
              placeholder="请输入题目内容..." rows="4"></textarea>
          </div>

          <div class="form-group">
            <label class="form-label required">难度</label>
            <div class="tag-select-group">
              <span v-for="d in difficultyOptions" :key="d.value"
                class="sel-tag" :class="{ active: form.difficulty === d.value }"
                @click="form.difficulty = d.value">{{ d.label }}</span>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">状态</label>
            <div class="tag-select-group">
              <span class="sel-tag" :class="{ active: form.status === 1 }" @click="form.status = 1">启用</span>
              <span class="sel-tag" :class="{ active: form.status === 0 }" @click="form.status = 0">停用</span>
            </div>
          </div>

          <div class="section-divider"></div>
          <div class="section-title">扩展信息（可选）</div>

          <div class="form-group">
            <label class="form-label">优秀答案示例</label>
            <textarea v-model="form.excellent_answer" class="form-textarea"
              placeholder="请输入优秀答案示例..." rows="4"></textarea>
          </div>

          <div class="form-group">
            <label class="form-label">答题要点</label>
            <textarea v-model="form.answer_points" class="form-textarea"
              placeholder="列举答题的核心要点..." rows="3"></textarea>
          </div>

          <div class="form-group">
            <label class="form-label">评分标准</label>
            <textarea v-model="form.score_standard" class="form-textarea"
              placeholder="描述打分维度与标准..." rows="3"></textarea>
          </div>

          <div class="form-group">
            <label class="form-label">考察意图</label>
            <input v-model="form.question_intent" type="text" class="form-input"
              placeholder="如：考察候选人对并发编程的掌握程度" maxlength="255" />
          </div>

          <div class="form-group">
            <label class="form-label">关键词</label>
            <input v-model="form.keywords" type="text" class="form-input"
              placeholder="多个关键词用逗号分隔，如：HashMap,线程安全,扩容" maxlength="255" />
            <p class="hint-text">关键词用于语义检索匹配</p>
          </div>

          <div class="form-group">
            <label class="form-label">适用层级</label>
            <input v-model="form.suitable_level" type="text" class="form-input"
              placeholder="如：P5、P6、初级、高级" maxlength="50" />
          </div>

          <div class="form-group">
            <label class="form-label">备注</label>
            <input v-model="form.remark" type="text" class="form-input"
              placeholder="其他补充说明..." maxlength="255" />
          </div>

          <div v-if="createErrorMsg" class="error-banner">⚠️ {{ createErrorMsg }}</div>

          <div class="form-footer">
            <button class="btn-outline" @click="resetForm">重置</button>
            <button class="btn-primary" :disabled="submitting" @click="handleSubmit">
              {{ submitting ? '提交中...' : '✓ 保存题目' }}
            </button>
          </div>
        </div>

        <!-- ======================== Tab: 题目列表 ======================== -->
        <div v-show="activeTab === 'list'" class="list-panel">

          <!-- 搜索 + 筛选栏 -->
          <div class="filter-card">
            <div class="filter-row">
              <div class="search-wrap">
                <input v-model="listQuery.q" type="text" class="search-input"
                  placeholder="搜索题干关键词..." @keyup.enter="doSearch" />
                <button class="search-btn" @click="doSearch">🔍 搜索</button>
              </div>
              <button class="btn-outline sm" @click="resetListQuery">重置筛选</button>
            </div>

            <div class="filter-row filter-tags-row">
              <span class="filter-label">岗位</span>
              <div class="tags-wrap">
                <span class="f-tag" :class="{ active: !listQuery.job_position }"
                  @click="listQuery.job_position = ''; doSearch()">全部</span>
                <span v-for="p in jobPositionOptions" :key="p"
                  class="f-tag" :class="{ active: listQuery.job_position === p }"
                  @click="listQuery.job_position = p; doSearch()">{{ p }}</span>
              </div>
            </div>

            <div class="filter-row filter-tags-row">
              <span class="filter-label">题型</span>
              <div class="tags-wrap">
                <span class="f-tag" :class="{ active: !listQuery.question_type }"
                  @click="listQuery.question_type = ''; doSearch()">全部</span>
                <span v-for="qt in questionTypeOptions" :key="qt.value"
                  class="f-tag" :class="{ active: listQuery.question_type === qt.value }"
                  @click="listQuery.question_type = qt.value; doSearch()">{{ qt.label }}</span>
              </div>
            </div>

            <div class="filter-row filter-tags-row">
              <span class="filter-label">状态</span>
              <div class="tags-wrap">
                <span class="f-tag" :class="{ active: listQuery.status === undefined }"
                  @click="listQuery.status = undefined; doSearch()">全部</span>
                <span class="f-tag" :class="{ active: listQuery.status === 1 }"
                  @click="listQuery.status = 1; doSearch()">启用</span>
                <span class="f-tag" :class="{ active: listQuery.status === 0 }"
                  @click="listQuery.status = 0; doSearch()">停用</span>
              </div>
            </div>
          </div>

          <!-- 列表区域 -->
          <div class="list-card">
            <div class="list-head">
              <span class="list-total">共 <b>{{ listTotal }}</b> 条题目</span>
              <div class="page-size-wrap">
                每页
                <select v-model="listQuery.page_size" class="page-size-select" @change="doSearch">
                  <option :value="10">10</option>
                  <option :value="20">20</option>
                  <option :value="50">50</option>
                </select>
                条
              </div>
            </div>

            <!-- 加载中 -->
            <div v-if="listLoading" class="loading-box">
              <span class="loading-spinner"></span> 加载中...
            </div>

            <!-- 空状态 -->
            <div v-else-if="listItems.length === 0" class="empty-box">
              <div class="empty-icon">📭</div>
              <p>暂无题目，快去新建一道吧！</p>
              <button class="btn-primary" @click="switchTab('create')">+ 新建题目</button>
            </div>

            <!-- 题目表格 -->
            <div v-else>
              <div class="q-table">
                <div class="q-thead">
                  <div class="q-th" style="width:48px">#</div>
                  <div class="q-th" style="flex:1.2">岗位</div>
                  <div class="q-th" style="flex:1">题型</div>
                  <div class="q-th" style="flex:3">题干</div>
                  <div class="q-th" style="width:72px">难度</div>
                  <div class="q-th" style="width:60px">状态</div>
                  <div class="q-th" style="width:140px">创建时间</div>
                  <div class="q-th" style="width:240px">操作</div>
                </div>
                <div
                  v-for="(item, idx) in listItems"
                  :key="item.id"
                  class="q-tr"
                  @click="openDetail(item.id)"
                >
                  <div class="q-td center" style="width:48px">
                    {{ (listQuery.page! - 1) * listQuery.page_size! + idx + 1 }}
                  </div>
                  <div class="q-td" style="flex:1.2">
                    <span class="tag-pos">{{ item.job_position }}</span>
                  </div>
                  <div class="q-td" style="flex:1">
                    <span class="tag-type">{{ item.question_type }}</span>
                  </div>
                  <div class="q-td ellipsis" style="flex:3" :title="item.question">
                    {{ item.question }}
                  </div>
                  <div class="q-td center" style="width:72px">
                    <span v-if="item.difficulty" class="diff-badge" :class="'diff-' + item.difficulty">
                      {{ diffLabel(item.difficulty) }}
                    </span>
                    <span v-else class="text-muted">—</span>
                  </div>
                  <div class="q-td center" style="width:60px">
                    <span class="status-dot" :class="item.status === 1 ? 'on' : 'off'">
                      {{ item.status === 1 ? '启用' : '停用' }}
                    </span>
                  </div>
                  <div class="q-td center text-muted" style="width:140px">
                    {{ formatDate(item.created_at) }}
                  </div>
                  <div class="q-td center ops-cell" style="width:240px" @click.stop>
                    <button class="op-btn view-btn" @click="openDetail(item.id)">查看</button>
                    <button
                      v-if="item.in_chroma === 1"
                      class="op-btn chroma-done-btn"
                      disabled
                      title="已写入向量库"
                    >已入向量库</button>
                    <button
                      v-else
                      class="op-btn chroma-btn"
                      :disabled="chromaSavingId === item.id"
                      @click="saveToChroma(item)"
                    >
                      {{ chromaSavingId === item.id ? '保存中…' : '保存到向量数据库' }}
                    </button>
                    <button class="op-btn del-btn" @click="confirmDelete(item)">删除</button>
                  </div>
                </div>
              </div>

              <!-- 分页 -->
              <div class="pagination">
                <button class="page-btn" :disabled="listQuery.page! <= 1"
                  @click="changePage(listQuery.page! - 1)">上一页</button>
                <div class="page-nums">
                  <span
                    v-for="p in pageNumbers"
                    :key="p"
                    class="page-num"
                    :class="{ active: p === listQuery.page, ellipsis: p === '...' }"
                    @click="typeof p === 'number' && changePage(p)"
                  >{{ p }}</span>
                </div>
                <button class="page-btn" :disabled="listQuery.page! >= totalPages"
                  @click="changePage(listQuery.page! + 1)">下一页</button>
                <span class="page-info">{{ listQuery.page }} / {{ totalPages }} 页</span>
              </div>
            </div>
          </div>
        </div>

      </div>
    </main>

    <!-- ======================== 详情弹窗 ======================== -->
    <div v-if="showDetail" class="modal-overlay" @click.self="showDetail = false">
      <div class="modal-box">
        <div class="modal-header">
          <h2>题目详情</h2>
          <button class="close-btn" @click="showDetail = false">×</button>
        </div>
        <div class="modal-body" v-if="detailItem">
          <div v-if="detailLoading" class="loading-box">
            <span class="loading-spinner"></span> 加载中...
          </div>
          <template v-else>
            <!-- ===== 基本信息 ===== -->
            <div class="detail-section-title">基本信息</div>
            <div class="detail-grid">
              <div class="detail-field col-half">
                <div class="detail-field-label">岗位</div>
                <div class="detail-field-value">{{ detailItem.job_position }}</div>
              </div>
              <div class="detail-field col-half">
                <div class="detail-field-label">题目类型</div>
                <div class="detail-field-value"><span class="tag-type">{{ detailItem.question_type }}</span></div>
              </div>
              <div class="detail-field col-half">
                <div class="detail-field-label">难度</div>
                <div class="detail-field-value">
                  <span v-if="detailItem.difficulty" class="diff-badge" :class="'diff-' + detailItem.difficulty">
                    {{ diffLabel(detailItem.difficulty) }}
                  </span>
                  <span v-else class="text-muted">—</span>
                </div>
              </div>
              <div class="detail-field col-half">
                <div class="detail-field-label">状态</div>
                <div class="detail-field-value">
                  <span class="status-dot" :class="detailItem.status === 1 ? 'on' : 'off'">
                    {{ detailItem.status === 1 ? '启用' : '停用' }}
                  </span>
                </div>
              </div>
              <div class="detail-field col-full">
                <div class="detail-field-label">题干</div>
                <div class="detail-field-value pre-wrap limit-height">{{ detailItem.question }}</div>
              </div>
            </div>

            <!-- ===== 扩展信息 ===== -->
            <div class="detail-divider"></div>
            <div class="detail-section-title">扩展信息</div>
            <div class="detail-grid">
              <div class="detail-field col-half">
                <div class="detail-field-label">优秀答案示例</div>
                <div class="detail-field-value pre-wrap limit-height">{{ detailItem.excellent_answer || '（未填写）' }}</div>
              </div>
              <div class="detail-field col-half">
                <div class="detail-field-label">答题要点</div>
                <div class="detail-field-value pre-wrap limit-height">{{ detailItem.answer_points || '（未填写）' }}</div>
              </div>
              <div class="detail-field col-half">
                <div class="detail-field-label">评分标准</div>
                <div class="detail-field-value pre-wrap limit-height">{{ detailItem.score_standard || '（未填写）' }}</div>
              </div>
              <div class="detail-field col-half">
                <div class="detail-field-label">考察意图</div>
                <div class="detail-field-value">{{ detailItem.question_intent || '—' }}</div>
              </div>
              <div class="detail-field col-half">
                <div class="detail-field-label">关键词</div>
                <div class="detail-field-value">{{ detailItem.keywords || '—' }}</div>
              </div>
              <div class="detail-field col-half">
                <div class="detail-field-label">适用层级</div>
                <div class="detail-field-value">{{ detailItem.suitable_level || '—' }}</div>
              </div>
              <div class="detail-field col-full">
                <div class="detail-field-label">备注</div>
                <div class="detail-field-value">{{ detailItem.remark || '—' }}</div>
              </div>
            </div>

            <!-- ===== 系统信息 ===== -->
            <div class="detail-divider"></div>
            <div class="detail-bottom-row">
              <div class="detail-time-row">
                <span class="text-muted">创建：{{ formatDate(detailItem.created_at) }}</span>
              </div>
              <div class="modal-footer">
                <button class="btn-outline" @click="showDetail = false">关闭</button>
                <button class="btn-danger" @click="confirmDelete(detailItem); showDetail = false">删除此题</button>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>

    <!-- ======================== 删除确认弹窗 ======================== -->
    <div v-if="showDeleteConfirm" class="modal-overlay" @click.self="showDeleteConfirm = false">
      <div class="modal-box confirm-box">
        <div class="confirm-icon">🗑️</div>
        <h3>确认删除</h3>
        <p class="confirm-text">确定要删除这道题目吗？此操作不可撤销。</p>
        <div class="confirm-q">{{ deleteTarget?.question?.slice(0, 60) }}{{ (deleteTarget?.question?.length ?? 0) > 60 ? '...' : '' }}</div>
        <div class="confirm-actions">
          <button class="btn-outline" @click="showDeleteConfirm = false">取消</button>
          <button class="btn-danger" :disabled="deleting" @click="doDelete">
            {{ deleting ? '删除中...' : '确认删除' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Toast 提示 -->
    <div v-if="showToast" class="success-toast" :class="toastType">{{ toastMsg }}</div>

    <!-- ======================== AI 自动解析弹窗 ======================== -->
    <div v-if="showAiRecognize" class="modal-overlay" @click.self="showAiRecognize = false">
      <div class="modal-box ai-modal-box">
        <div class="modal-header">
          <h2>🤖 AI 自动解析题目</h2>
          <button class="close-btn" @click="showAiRecognize = false">×</button>
        </div>
        <div class="modal-body">
          <p class="ai-modal-hint">将题目原文粘贴到下方，AI 将自动识别并填充题目各项字段。</p>

          <div class="form-group">
            <label class="form-label">题目原文 <span style="color:#f87171">*</span></label>
            <textarea
              v-model="aiSourceText"
              class="form-textarea"
              rows="8"
              placeholder="请将题目内容粘贴这里，可包含题干、参考答案、考察意图等..."
            />
          </div>

          <div class="ai-hints-row">
            <div class="form-group" style="flex:1">
              <label class="form-label">岗位提示（可空）</label>
              <input v-model="aiHintJob" type="text" class="form-input" placeholder="如：Java 开发" />
            </div>
            <div class="form-group" style="flex:1">
              <label class="form-label">题型提示（可空）</label>
              <input v-model="aiHintType" type="text" class="form-input" placeholder="如：技术问答" />
            </div>
          </div>

          <div v-if="aiRecognizeError" class="error-banner">⚠️ {{ aiRecognizeError }}</div>
        </div>
        <div class="ai-modal-footer">
          <button class="btn-outline" @click="showAiRecognize = false">取消</button>
          <button class="btn-primary" :disabled="aiRecognizing || !aiSourceText.trim()" @click="doAiRecognize">
            {{ aiRecognizing ? '解析中...' : '🚀 开始解析' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import Sidebar from '../components/Sidebar.vue'
import { knowledgeApi, chromaApi, type KnowledgeCreateParams, type KnowledgeItem, type KnowledgeListParams } from '../api/knowledge'

// ==================== 选项配置 ====================
const questionTypeOptions = [
  { label: '技术问答', value: '技术问答' },
  { label: '编程题', value: '编程题' },
  { label: '行为面试', value: '行为面试' },
  { label: '场景分析', value: '场景分析' },
  { label: '系统设计', value: '系统设计' },
  { label: '基础知识', value: '基础知识' },
]

const difficultyOptions = [
  { label: '入门', value: 1 },
  { label: '基础', value: 2 },
  { label: '中等', value: 3 },
  { label: '进阶', value: 4 },
  { label: '大厂真题', value: 5 },
]

const jobPositionOptions = [
  'Java 开发', '前端开发', '产品经理', '算法工程师', '测试开发', '数据分析', '后端开发', '全栈开发'
]

const diffLabel = (d: number) => {
  const map: Record<number, string> = { 1: '入门', 2: '基础', 3: '中等', 4: '进阶', 5: '大厂' }
  return map[d] ?? '—'
}

const formatDate = (s: string) => {
  if (!s) return '—'
  return s.replace('T', ' ').slice(0, 16)
}

// ==================== Tab ====================
const activeTab = ref<'create' | 'list'>('list')
const switchTab = (tab: 'create' | 'list') => {
  activeTab.value = tab
  if (tab === 'list') loadList()
}

// ==================== 新建表单 ====================
const defaultForm = () => ({
  job_position: '',
  question_type: '',
  question_type_custom: '',
  question: '',
  difficulty: 3,
  status: 1,
  excellent_answer: '',
  answer_points: '',
  score_standard: '',
  question_intent: '',
  keywords: '',
  suitable_level: '',
  remark: '',
})

const form = ref(defaultForm())
const submitting = ref(false)
const createErrorMsg = ref('')

const resetForm = () => {
  form.value = defaultForm()
  createErrorMsg.value = ''
}

const validate = (): boolean => {
  if (!form.value.job_position.trim()) { createErrorMsg.value = '岗位不能为空'; return false }
  if (!form.value.question_type.trim() || form.value.question_type === '__custom__') {
    createErrorMsg.value = '请选择或填写题目类型'; return false
  }
  if (!form.value.question.trim()) { createErrorMsg.value = '题干不能为空'; return false }
  createErrorMsg.value = ''
  return true
}

const handleSubmit = async () => {
  if (!validate()) return
  submitting.value = true
  try {
    const payload: KnowledgeCreateParams = {
      job_position: form.value.job_position.trim(),
      question_type: form.value.question_type.trim(),
      question: form.value.question.trim(),
      difficulty: form.value.difficulty,
      status: form.value.status,
    }
    if (form.value.excellent_answer.trim()) payload.excellent_answer = form.value.excellent_answer.trim()
    if (form.value.answer_points.trim()) payload.answer_points = form.value.answer_points.trim()
    if (form.value.score_standard.trim()) payload.score_standard = form.value.score_standard.trim()
    if (form.value.question_intent.trim()) payload.question_intent = form.value.question_intent.trim()
    if (form.value.keywords.trim()) payload.keywords = form.value.keywords.trim()
    if (form.value.suitable_level.trim()) payload.suitable_level = form.value.suitable_level.trim()
    if (form.value.remark.trim()) payload.remark = form.value.remark.trim()

    await knowledgeApi.create(payload)
    showToastMsg('题目保存成功！', 'success')
    resetForm()
    switchTab('list')
  } catch (err: any) {
    createErrorMsg.value = err?.message || '提交失败，请重试'
  } finally {
    submitting.value = false
  }
}

// ==================== 列表查询 ====================
const listItems = ref<KnowledgeItem[]>([])
const listTotal = ref(0)
const listLoading = ref(false)

const listQuery = ref<KnowledgeListParams & { page: number; page_size: number }>({
  page: 1,
  page_size: 10,
  q: '',
  job_position: '',
  question_type: '',
  status: undefined,
})

const totalPages = computed(() => Math.max(1, Math.ceil(listTotal.value / listQuery.value.page_size)))

const pageNumbers = computed(() => {
  const total = totalPages.value
  const cur = listQuery.value.page
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  const pages: (number | string)[] = [1]
  if (cur > 3) pages.push('...')
  for (let i = Math.max(2, cur - 1); i <= Math.min(total - 1, cur + 1); i++) pages.push(i)
  if (cur < total - 2) pages.push('...')
  pages.push(total)
  return pages
})

const loadList = async () => {
  listLoading.value = true
  try {
    const params: KnowledgeListParams = { page: listQuery.value.page, page_size: listQuery.value.page_size }
    if (listQuery.value.q) params.q = listQuery.value.q
    if (listQuery.value.job_position) params.job_position = listQuery.value.job_position
    if (listQuery.value.question_type) params.question_type = listQuery.value.question_type
    if (listQuery.value.status !== undefined) params.status = listQuery.value.status
    const res = await knowledgeApi.list(params)
    listItems.value = res.items
    listTotal.value = res.total
  } catch (err: any) {
    showToastMsg(err?.message || '加载失败', 'error')
  } finally {
    listLoading.value = false
  }
}

const doSearch = () => {
  listQuery.value.page = 1
  loadList()
}

const changePage = (p: number) => {
  if (p < 1 || p > totalPages.value) return
  listQuery.value.page = p
  loadList()
}

const resetListQuery = () => {
  listQuery.value = { page: 1, page_size: listQuery.value.page_size, q: '', job_position: '', question_type: '', status: undefined }
  loadList()
}

// ==================== 详情 ====================
const showDetail = ref(false)
const detailItem = ref<KnowledgeItem | null>(null)
const detailLoading = ref(false)

const openDetail = async (id: number) => {
  showDetail.value = true
  detailLoading.value = true
  detailItem.value = null
  try {
    detailItem.value = await knowledgeApi.detail(id)
  } catch (err: any) {
    showToastMsg(err?.message || '获取详情失败', 'error')
    showDetail.value = false
  } finally {
    detailLoading.value = false
  }
}

// ==================== 保存到向量数据库 ====================
const chromaSavingId = ref<number | null>(null)

const saveToChroma = async (item: KnowledgeItem) => {
  if (chromaSavingId.value != null) return
  chromaSavingId.value = item.id
  try {
    await chromaApi.create(item.id)
    item.in_chroma = 1
    showToastMsg('已保存到向量数据库', 'success')
  } catch (err: any) {
    showToastMsg(err?.message || '保存到向量库失败', 'error')
  } finally {
    chromaSavingId.value = null
  }
}

// ==================== 删除 ====================
const showDeleteConfirm = ref(false)
const deleteTarget = ref<KnowledgeItem | null>(null)
const deleting = ref(false)

const confirmDelete = (item: KnowledgeItem) => {
  deleteTarget.value = item
  showDeleteConfirm.value = true
}

const doDelete = async () => {
  if (!deleteTarget.value) return
  deleting.value = true
  try {
    await knowledgeApi.delete(deleteTarget.value.id)
    showToastMsg('题目已删除', 'success')
    showDeleteConfirm.value = false
    deleteTarget.value = null
    loadList()
  } catch (err: any) {
    showToastMsg(err?.message || '删除失败', 'error')
  } finally {
    deleting.value = false
  }
}

// ==================== Toast ====================
const showToast = ref(false)
const toastMsg = ref('')
const toastType = ref<'success' | 'error'>('success')

const showToastMsg = (msg: string, type: 'success' | 'error' = 'success') => {
  toastMsg.value = (type === 'success' ? '✅ ' : '❌ ') + msg
  toastType.value = type
  showToast.value = true
  setTimeout(() => { showToast.value = false }, 2500)
}

onMounted(() => {
  loadList()
})

// ==================== AI 自动解析 ====================
const showAiRecognize = ref(false)
const aiSourceText = ref('')
const aiHintJob = ref('')
const aiHintType = ref('')
const aiRecognizing = ref(false)
const aiRecognizeError = ref('')

const openAiRecognize = () => {
  aiSourceText.value = ''
  aiHintJob.value = ''
  aiHintType.value = ''
  aiRecognizeError.value = ''
  showAiRecognize.value = true
}

const doAiRecognize = async () => {
  if (!aiSourceText.value.trim()) return
  aiRecognizing.value = true
  aiRecognizeError.value = ''
  try {
    const result = await knowledgeApi.aiRecognize({
      source_text: aiSourceText.value.trim(),
      job_position: aiHintJob.value.trim() || undefined,
      question_type: aiHintType.value.trim() || undefined,
    })
    // 解析成功，填入表单
    form.value = {
      ...defaultForm(),
      job_position: result.job_position || '',
      question_type: result.question_type || '',
      question_type_custom: '',
      question: result.question || '',
      difficulty: result.difficulty ?? 3,
      status: 1,
      excellent_answer: result.excellent_answer || '',
      answer_points: result.answer_points || '',
      score_standard: result.score_standard || '',
      question_intent: result.question_intent || '',
      keywords: result.keywords || '',
      suitable_level: result.suitable_level || '',
      remark: result.remark || '',
    }
    showAiRecognize.value = false
    activeTab.value = 'create'
    showToastMsg('解析成功，已自动填入表单，请检查后提交', 'success')
  } catch (err: any) {
    aiRecognizeError.value = err?.message || 'AI 解析失败，请重试'
  } finally {
    aiRecognizing.value = false
  }
}
</script>

<style scoped>
/* ========== 布局 ========== */
.page-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  overflow-x: hidden;
  max-width: 100%;
  background: var(--hp-surface);
}

.main-content {
  flex: 1 1 0;
  min-width: 0;
  margin-left: clamp(200px, 25vw, 320px);
  overflow-y: auto;
  overflow-x: hidden;
  height: 100vh;
  background: var(--hp-surface);
}

.page-container {
  margin: 0 auto;
  padding: 32px 28px 64px;
  width: 100%;
  max-width: min(1350px, 100%);
  box-sizing: border-box;
  overflow-x: hidden;
}

/* ========== 页面标题 + Tab ========== */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 12px;
}

.page-header h1 {
  font-size: 22px;
  font-weight: 700;
  color: #e2e8f0;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.btn-ai-parse {
  height: 38px;
  padding: 0 16px;
  background: linear-gradient(135deg, #1f4f89 0%, #2d3f6b 100%);
  color: #7eb8f7;
  border: 1px solid #3a7bc8;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  outline: none;
}

.btn-ai-parse:hover {
  background: linear-gradient(135deg, #2d6bb3 0%, #3a5a9e 100%);
  color: #b8d4f7;
  box-shadow: 0 2px 10px rgba(58,123,200,0.3);
}

.tab-switch {
  display: flex;
  background: var(--hp-surface);
  border-radius: 10px;
  padding: 4px;
  border: 1px solid var(--hp-line);
  gap: 2px;
}

.tab-btn {
  padding: 7px 20px;
  border: none;
  border-radius: 7px;
  font-size: 14px;
  font-weight: 500;
  color: #6b7a99;
  background: transparent;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn.active {
  background: #3a7bc8;
  color: #fff;
  box-shadow: 0 2px 8px rgba(58,123,200,0.3);
}

.tab-btn:hover:not(.active) {
  background: rgba(58,123,200,0.1);
  color: #9daec8;
}

/* ========== 新建表单卡片 ========== */
.form-card {
  background: var(--hp-surface);
  border-radius: 16px;
  padding: 32px;
  border: 1px solid var(--hp-line);
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #9daec8;
  margin-bottom: 20px;
  padding-left: 8px;
  border-left: 3px solid #3a7bc8;
}

.section-divider {
  border: none;
  border-top: 1px solid var(--hp-line);
  margin: 28px 0;
}

.form-group {
  margin-bottom: 22px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #9daec8;
  margin-bottom: 8px;
}

.form-label.required::after {
  content: ' *';
  color: #f87171;
}

.form-input {
  width: 100%;
  height: 40px;
  padding: 0 12px;
  background: var(--hp-surface);
  border: 1px solid var(--hp-line);
  border-radius: 8px;
  font-size: 14px;
  color: var(--hp-ink);
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.form-input::placeholder { color: #4a5568; }

.form-input:focus {
  border-color: #3a7bc8;
  box-shadow: 0 0 0 3px rgba(58,123,200,0.15);
}

.inline-input {
  display: inline-block;
  width: auto;
  min-width: 140px;
  vertical-align: middle;
  margin-left: 8px;
}

.form-textarea {
  width: 100%;
  padding: 10px 12px;
  background: var(--hp-surface);
  border: 1px solid var(--hp-line);
  border-radius: 8px;
  font-size: 14px;
  color: var(--hp-ink);
  outline: none;
  resize: vertical;
  transition: border-color 0.2s;
  box-sizing: border-box;
  font-family: inherit;
  line-height: 1.6;
}

.form-textarea::placeholder { color: #4a5568; }

.form-textarea:focus {
  border-color: #3a7bc8;
  box-shadow: 0 0 0 3px rgba(58,123,200,0.15);
}

.hint-text {
  font-size: 12px;
  color: #4a5568;
  margin-top: 5px;
}

.tag-select-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.sel-tag {
  display: inline-flex;
  align-items: center;
  padding: 5px 14px;
  border: 1px solid var(--hp-line);
  border-radius: 20px;
  font-size: 13px;
  color: #6b7a99;
  background: var(--hp-surface);
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}

.sel-tag:hover { border-color: #3a7bc8; color: #3a7bc8; }
.sel-tag.active { background: #1f4f89; border-color: #3a7bc8; color: #7eb8f7; }
.sel-tag.custom-tag { border-style: dashed; }

.error-banner {
  padding: 12px 16px;
  background: rgba(248,113,113,0.1);
  border: 1px solid rgba(248,113,113,0.3);
  border-radius: 8px;
  color: #f87171;
  font-size: 14px;
  margin-bottom: 20px;
}

.form-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 8px;
}

/* ========== 列表面板 ========== */
.list-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 筛选卡片 */
.filter-card {
  background: var(--hp-surface);
  border-radius: 14px;
  padding: 20px 24px;
  border: 1px solid var(--hp-line);
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.search-wrap {
  display: flex;
  gap: 8px;
  flex: 1;
  min-width: 220px;
}

.search-input {
  flex: 1;
  height: 38px;
  padding: 0 12px;
  background: var(--hp-surface);
  border: 1px solid var(--hp-line);
  border-radius: 8px;
  font-size: 14px;
  color: var(--hp-ink);
  outline: none;
  transition: border-color 0.2s;
}

.search-input::placeholder { color: #4a5568; }
.search-input:focus { border-color: #3a7bc8; box-shadow: 0 0 0 3px rgba(58,123,200,0.15); }

.search-btn {
  height: 38px;
  padding: 0 16px;
  background: #3a7bc8;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.2s;
}

.search-btn:hover { background: #2d6bb3; }

.filter-tags-row {
  align-items: flex-start;
}

.filter-label {
  font-size: 13px;
  font-weight: 500;
  color: #6b7a99;
  min-width: 40px;
  padding-top: 5px;
}

.tags-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.f-tag {
  padding: 4px 12px;
  border: 1px solid var(--hp-line);
  border-radius: 16px;
  font-size: 12px;
  color: #6b7a99;
  background: var(--hp-surface);
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}

.f-tag:hover { border-color: #3a7bc8; color: #3a7bc8; }
.f-tag.active { background: #1f4f89; border-color: #3a7bc8; color: #7eb8f7; }

/* 列表卡片 */
.list-card {
  background: var(--hp-surface);
  border-radius: 14px;
  padding: 20px 24px;
  border: 1px solid var(--hp-line);
  overflow-x: hidden;
  max-width: 100%;
}

.list-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 8px;
}

.list-total { font-size: 14px; color: #6b7a99; }
.list-total b { color: #3a7bc8; font-weight: 700; }

.page-size-wrap {
  font-size: 13px;
  color: #6b7a99;
  display: flex;
  align-items: center;
  gap: 6px;
}

.page-size-select {
  background: var(--hp-surface);
  border: 1px solid var(--hp-line);
  border-radius: 6px;
  padding: 2px 6px;
  font-size: 13px;
  color: #9daec8;
  outline: none;
}

/* 表格 */
.q-table {
  display: flex;
  flex-direction: column;
  overflow-x: hidden;
  max-width: 100%;
}

.q-thead {
  display: flex;
  align-items: center;
  padding: 0 16px;
  height: 44px;
  background: var(--hp-surface);
  border-radius: 8px;
  margin-bottom: 4px;
  min-width: 0;
  max-width: 100%;
}

.q-th {
  font-size: 12px;
  font-weight: 600;
  color: #4a5568;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  flex-shrink: 0;
}

.q-tr {
  display: flex;
  align-items: center;
  padding: 0 16px;
  min-height: 56px;
  border-radius: 8px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.15s;
  margin-bottom: 2px;
  min-width: 0;
  max-width: 100%;
}

.q-tr:hover {
  background: rgba(58,123,200,0.08);
  border-color: var(--hp-line);
}

.q-td {
  font-size: 13px;
  color: #9daec8;
  flex-shrink: 0;
  padding: 0 6px;
}

.q-td.center { text-align: center; display: flex; justify-content: center; align-items: center; }
.q-td.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
  flex-shrink: 1;
}

/* 标签 */
.tag-pos {
  display: inline-block;
  padding: 2px 8px;
  background: rgba(58,123,200,0.15);
  color: #7eb8f7;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.tag-type {
  display: inline-block;
  padding: 2px 8px;
  background: rgba(52,211,153,0.12);
  color: #34d399;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.diff-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.diff-badge.diff-1, .diff-badge.diff-2 { background: rgba(52,211,153,0.12); color: #34d399; }
.diff-badge.diff-3 { background: rgba(251,191,36,0.12); color: #fbbf24; }
.diff-badge.diff-4, .diff-badge.diff-5 { background: rgba(248,113,113,0.12); color: #f87171; }

.status-dot {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
}

.status-dot.on { background: rgba(52,211,153,0.12); color: #34d399; }
.status-dot.off { background: rgba(255,255,255,0.05); color: #4a5568; }

.text-muted { color: #4a5568; }

/* 操作按钮 */
.op-btn {
  height: 28px;
  padding: 0 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  border: none;
  cursor: pointer;
  transition: all 0.15s;
  margin: 0 3px;
}

.view-btn { background: rgba(58,123,200,0.15); color: #7eb8f7; }
.view-btn:hover { background: rgba(58,123,200,0.25); }
.chroma-btn { background: rgba(34,197,94,0.15); color: #22c55e; }
.chroma-btn:hover:not(:disabled) { background: rgba(34,197,94,0.25); }
.chroma-btn:disabled { opacity: 0.7; cursor: wait; }
.chroma-done-btn { background: rgba(148,163,184,0.2); color: #94a3b8; cursor: default; }
.del-btn { background: rgba(248,113,113,0.12); color: #f87171; }
.del-btn:hover { background: rgba(248,113,113,0.22); }

.ops-cell {
  display: flex;
  flex-wrap: nowrap;
  gap: 6px;
  justify-content: center;
  align-items: center;
  min-width: 240px;
}
.ops-cell .op-btn { white-space: nowrap; flex-shrink: 0; }

/* 分页 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--hp-line);
}

.page-btn {
  height: 34px;
  padding: 0 16px;
  background: var(--hp-surface);
  border: 1px solid var(--hp-line);
  border-radius: 7px;
  font-size: 13px;
  color: #9daec8;
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) { border-color: #3a7bc8; color: #3a7bc8; }
.page-btn:disabled { opacity: 0.35; cursor: not-allowed; }

.page-nums { display: flex; gap: 4px; }

.page-num {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 7px;
  font-size: 13px;
  color: #9daec8;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.2s;
}

.page-num:hover:not(.ellipsis) { border-color: #3a7bc8; color: #3a7bc8; }
.page-num.active { background: #1f4f89; color: #7eb8f7; border-color: #3a7bc8; }
.page-num.ellipsis { cursor: default; color: #4a5568; }

.page-info { font-size: 13px; color: #4a5568; margin-left: 4px; }

/* 空状态 / 加载 */
.empty-box {
  text-align: center;
  padding: 60px 0;
  color: #4a5568;
}

.empty-icon { font-size: 48px; margin-bottom: 12px; }
.empty-box p { font-size: 14px; margin-bottom: 16px; }

.loading-box {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 60px 0;
  color: #4a5568;
  font-size: 14px;
}

.loading-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid var(--hp-line);
  border-top-color: #3a7bc8;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

/* ========== 弹窗 ========== */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.65);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.modal-box {
  background: var(--hp-surface);
  border: 1px solid var(--hp-line);
  border-radius: 16px;
  width: 100%;
  max-width: 860px;
  height: 88vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0,0,0,0.5);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  border-bottom: 1px solid var(--hp-line);
  flex-shrink: 0;
}

.modal-header h2 {
  font-size: 18px;
  font-weight: 700;
  color: #e2e8f0;
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  font-size: 26px;
  color: #4a5568;
  cursor: pointer;
  line-height: 1;
  transition: color 0.2s;
}

.close-btn:hover { color: #9daec8; }

.modal-body {
  padding: 16px 20px;
  overflow-y: auto;
  flex: 1;
  display: flex;
  flex-direction: column;
}

/* 详情样式 */
.detail-section-title {
  font-size: 12px;
  font-weight: 600;
  color: #3a7bc8;
  letter-spacing: 0.04em;
  margin-bottom: 10px;
  padding-left: 8px;
  border-left: 3px solid #3a7bc8;
}

.detail-divider {
  border: none;
  border-top: 1px solid var(--hp-line);
  margin: 10px 0;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 4px;
}

.detail-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.col-full {
  grid-column: 1 / -1;
}

.col-half {
  grid-column: span 1;
}

.detail-field-label {
  font-size: 11px;
  font-weight: 500;
  color: #4a5568;
  line-height: 1.4;
}

.detail-field-value {
  font-size: 13px;
  color: #c8d6e8;
  line-height: 1.6;
  background: var(--hp-surface);
  padding: 5px 10px;
  border-radius: 6px;
  min-height: 28px;
  border: 1px solid var(--hp-line);
}

.detail-field-value.pre-wrap {
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-field-value.limit-height {
  max-height: 80px;
  overflow-y: auto;
}

.detail-bottom-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: auto;
  padding-top: 10px;
  border-top: 1px solid var(--hp-line);
}

.detail-time-row {
  display: flex;
  gap: 16px;
  font-size: 11px;
  flex-wrap: wrap;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-shrink: 0;
}

/* 确认删除弹窗 */
.confirm-box {
  max-width: 420px;
  text-align: center;
  padding: 32px;
}

.confirm-icon { font-size: 40px; margin-bottom: 12px; }

.confirm-box h3 {
  font-size: 18px;
  font-weight: 700;
  color: #e2e8f0;
  margin: 0 0 8px;
}

.confirm-text {
  font-size: 14px;
  color: #6b7a99;
  margin-bottom: 12px;
}

.confirm-q {
  font-size: 13px;
  color: #9daec8;
  background: var(--hp-surface);
  border: 1px solid var(--hp-line);
  padding: 10px 14px;
  border-radius: 8px;
  margin-bottom: 20px;
  line-height: 1.5;
}

.confirm-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

/* ========== 通用按钮 ========== */
.btn-primary {
  height: 40px;
  padding: 0 24px;
  background: #3a7bc8;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary:hover:not(:disabled) { background: #2d6bb3; }
.btn-primary:disabled { opacity: 0.45; cursor: not-allowed; }

.btn-outline {
  height: 40px;
  padding: 0 20px;
  border: 1px solid var(--hp-line);
  border-radius: 8px;
  background: transparent;
  color: #6b7a99;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-outline:hover { border-color: #3a7bc8; color: #3a7bc8; }
.btn-outline.sm { height: 38px; padding: 0 14px; font-size: 13px; }

.btn-danger {
  height: 40px;
  padding: 0 20px;
  background: rgba(248,113,113,0.15);
  color: #f87171;
  border: 1px solid rgba(248,113,113,0.3);
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-danger:hover:not(:disabled) { background: rgba(248,113,113,0.25); }
.btn-danger:disabled { opacity: 0.45; cursor: not-allowed; }

/* ========== AI 解析弹窗样式 ========== */
.ai-modal-box {
  max-width: 640px;
  height: auto;
  max-height: 86vh;
}

.ai-modal-hint {
  font-size: 13px;
  color: #6b7a99;
  margin: 0 0 16px;
  line-height: 1.6;
}

.ai-hints-row {
  display: flex;
  gap: 16px;
}

.ai-modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 20px;
  border-top: 1px solid var(--hp-line);
  flex-shrink: 0;
}

/* ========== Toast ========== */
.success-toast {
  position: fixed;
  bottom: 32px;
  left: 50%;
  transform: translateX(-50%);
  background: var(--hp-surface);
  border: 1px solid var(--hp-line);
  color: var(--hp-ink);
  padding: 12px 24px;
  border-radius: 10px;
  font-size: 14px;
  z-index: 9999;
  animation: toast-in 0.3s ease;
  white-space: nowrap;
  box-shadow: 0 8px 24px rgba(0,0,0,0.4);
}

.success-toast.error { background: rgba(248,113,113,0.15); border-color: rgba(248,113,113,0.3); color: #f87171; }

@keyframes toast-in {
  from { opacity: 0; transform: translateX(-50%) translateY(10px); }
  to   { opacity: 1; transform: translateX(-50%) translateY(0); }
}
</style>
