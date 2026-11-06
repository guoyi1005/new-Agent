<template>
	<div class="page-layout">
		<Sidebar />
		<main class="main-content">
			<div class="page-container">
				<section class="hero-card">
					<div>
						<h1>个人题库收藏</h1>
						<p>收藏高价值题目，沉淀你的个人笔记与复盘要点。</p>
					</div>
					<div class="hero-metrics">
						<div class="metric-box">
							<span class="metric-label">收藏题目</span>
							<span class="metric-value">{{ filteredItems.length }}</span>
						</div>
						<div class="metric-box">
							<span class="metric-label">已写备注</span>
							<span class="metric-value">{{ noteCount }}</span>
						</div>
					</div>
				</section>

				<section class="toolbar-card">
					<div class="search-row">
						<input v-model="keyword" type="text" placeholder="搜索题目标题、岗位、备注关键词..." />
						<select v-model="positionFilter">
							<option value="">全部岗位</option>
							<option v-for="p in positionOptions" :key="p" :value="p">{{ p }}</option>
						</select>
					</div>
					<div class="chip-row" v-if="positionChipOptions.length > 0">
						<button class="chip" :class="{ active: positionChipFilter === '' }" @click="selectPositionChip('')">全部岗位</button>
						<button class="chip" v-for="p in positionChipOptions" :key="p" :class="{ active: positionChipFilter === p }" @click="selectPositionChip(p)">
							{{ p }}
						</button>
					</div>
				</section>

				<section class="list-card">
					<header class="list-head">
						<span>题目</span>
						<span>备注</span>
						<span>操作</span>
					</header>

					<div class="list-body" v-if="filteredItems.length > 0">
						<article class="note-row" v-for="item in filteredItems" :key="item.id">
							<div class="question-col">
								<h3>
									<button class="question-link" @click="openQuestionDetail(item)">{{ item.title }}</button>
								</h3>
								<div class="meta">
									<span>{{ item.position }}</span>
									<span>{{ item.questionType }}</span>
									<span>收藏于 {{ item.createdAt }}</span>
									<span>更新于 {{ item.updatedAt }}</span>
								</div>
							</div>
							<div class="remark-col">
								<p v-if="item.remark">{{ item.remark }}</p>
								<p v-else class="muted">暂无备注</p>
							</div>
							<div class="action-col">
								<button class="line" @click="openRemarkEditor(item)">{{ item.remark ? '编辑备注' : '添加备注' }}</button>
								<button class="danger" @click="removeItem(item.id)">取消收藏</button>
							</div>
						</article>
					</div>

					<div class="empty" v-else>
						{{ loading ? '收藏题目加载中...' : '当前筛选条件下没有收藏题目' }}
					</div>
				</section>
			</div>
		</main>
	</div>

	<Teleport to="body">
		<div v-if="remarkVisible" class="overlay" @click.self="remarkVisible = false">
			<div class="modal">
				<h3>编辑备注</h3>
				<p class="modal-title">{{ editingItem?.title }}</p>
				<textarea v-model="remarkDraft" rows="6" placeholder="记录这道题的关键思路、易错点、复习提醒..." />
				<div class="modal-actions">
					<button class="line" @click="remarkVisible = false">取消</button>
					<button class="primary" @click="saveRemark">保存备注</button>
				</div>
			</div>
		</div>
		<div v-if="detailVisible" class="overlay" @click.self="detailVisible = false">
			<div class="modal detail-modal">
				<div class="detail-head">
					<h3>题目详情</h3>
					<button class="close-btn" @click="detailVisible = false">✕</button>
				</div>
				<div v-if="detailLoading" class="detail-empty">详情加载中...</div>
				<div v-else-if="detailError" class="detail-empty">{{ detailError }}</div>
				<div v-else-if="detailData && detailItem" class="detail-content">
					<div class="detail-tags">
						<span>{{ detailItem.position }}</span>
						<span>{{ detailItem.questionType }}</span>
						<span>{{ difficultyText(detailData.difficulty || 3) }}</span>
						<span>{{ detailData.status === 1 ? '启用' : '停用' }}</span>
					</div>
					<section class="detail-block">
						<h4>题目内容</h4>
						<p>{{ detailData.question || '-' }}</p>
					</section>
					<section class="detail-block">
						<h4>优秀答案示例</h4>
						<p>{{ detailData.excellent_answer || '-' }}</p>
					</section>
					<section class="detail-block">
						<h4>答题要点</h4>
						<p>{{ detailData.answer_points || '-' }}</p>
					</section>
					<section class="detail-block two-col">
						<div>
							<h4>评分标准</h4>
							<p>{{ detailData.score_standard || '-' }}</p>
						</div>
						<div>
							<h4>考察意图</h4>
							<p>{{ detailData.question_intent || '-' }}</p>
						</div>
					</section>
					<section class="detail-block two-col">
						<div>
							<h4>关键词</h4>
							<p>{{ detailData.keywords || '-' }}</p>
						</div>
						<div>
							<h4>适用层级</h4>
							<p>{{ detailData.suitable_level || '-' }}</p>
						</div>
					</section>
					<section class="detail-block">
						<h4>我的收藏备注</h4>
						<p>{{ detailItem.remark || '暂无备注' }}</p>
						<p class="detail-time">收藏于 {{ detailItem.createdAt }} · 更新于 {{ detailItem.updatedAt }}</p>
					</section>
				</div>
			</div>
		</div>
	</Teleport>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import Sidebar from '../components/Sidebar.vue'
import { collectionApi } from '../api/collection'
import { knowledgeApi, type KnowledgeItem } from '../api/knowledge'

interface NoteItem {
	id: number
	knowledgeId: number
	title: string
	position: string
	questionType: string
	remark: string
	createdAt: string
	updatedAt: string
}

const keyword = ref('')
const positionFilter = ref('')
const positionChipFilter = ref('')

const items = ref<NoteItem[]>([])
const loading = ref(false)

// 岗位选项列表（从全量数据提取，不受过滤影响）
const allPositionOptions = ref<string[]>([])
const positionOptions = computed(() => allPositionOptions.value)
const positionChipOptions = computed(() => allPositionOptions.value)

const selectPositionChip = (position: string) => {
	positionChipFilter.value = position
	positionFilter.value = position
}

function mapCollectionItem(it: any): NoteItem {
	const createdAt = String(it.created_at || '').slice(0, 10)
	const updatedAt = String(it.updated_at || '').slice(0, 10)
	return {
		id: Number(it.id || 0),
		knowledgeId: Number(it.knowledge_id || 0),
		title: String(it.question || ''),
		position: String(it.job_position || ''),
		questionType: String(it.question_type || ''),
		remark: String(it.remark || ''),
		createdAt: createdAt || '-',
		updatedAt: updatedAt || '-',
	}
}

async function loadCollections() {
	loading.value = true
	try {
		const params: { q?: string } = {}
		if (keyword.value.trim()) params.q = keyword.value.trim()
		const res = await collectionApi.list(params)
		items.value = (res.items || []).map(mapCollectionItem)
		// 仅在加载全量数据时（无关键词）更新岗位选项列表
		if (!params.q) {
			allPositionOptions.value = Array.from(new Set(items.value.map((x) => x.position)))
		}
	} catch (e: any) {
		console.error('[MyNote] load collections failed', e)
		items.value = []
	} finally {
		loading.value = false
	}
}

let searchTimer: number | null = null
// 仅关键词变化时请求后端，岗位筛选在前端进行
watch(keyword, () => {
	if (searchTimer) {
		window.clearTimeout(searchTimer)
	}
	searchTimer = window.setTimeout(() => {
		loadCollections()
	}, 250)
})

const filteredItems = computed(() => {
	const q = keyword.value.trim().toLowerCase()
	return items.value.filter((item) => {
		if (positionFilter.value && item.position !== positionFilter.value) return false
		if (!q) return true
		return (
			item.title.toLowerCase().includes(q)
			|| item.position.toLowerCase().includes(q)
			|| item.questionType.toLowerCase().includes(q)
			|| item.remark.toLowerCase().includes(q)
		)
	})
})

const noteCount = computed(() => filteredItems.value.filter((x) => x.remark.trim()).length)

const remarkVisible = ref(false)
const editingItem = ref<NoteItem | null>(null)
const remarkDraft = ref('')
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detailData = ref<KnowledgeItem | null>(null)
const detailItem = ref<NoteItem | null>(null)

const difficultyText = (difficulty: number) => {
	const map: Record<number, string> = {
		1: '入门',
		2: '基础',
		3: '中等',
		4: '进阶',
		5: '大厂真题',
	}
	return map[difficulty] || `难度 ${difficulty}`
}

const openQuestionDetail = async (item: NoteItem) => {
	detailVisible.value = true
	detailLoading.value = true
	detailError.value = ''
	detailItem.value = item
	detailData.value = null
	try {
		detailData.value = await knowledgeApi.detail(item.knowledgeId)
	} catch (e: any) {
		detailError.value = e?.message || '题目详情加载失败'
	} finally {
		detailLoading.value = false
	}
}

const openRemarkEditor = (item: NoteItem) => {
	editingItem.value = item
	remarkDraft.value = item.remark
	remarkVisible.value = true
}

const saveRemark = async () => {
	if (!editingItem.value) return
	const id = editingItem.value.id
	try {
		const updated = await collectionApi.updateRemark({
			id,
			remark: remarkDraft.value.trim() || null,
		})
		const target = items.value.find((x) => x.id === id)
		if (target) {
			target.remark = String(updated.remark || '')
		}
		remarkVisible.value = false
	} catch (e) {
		console.error('[MyNote] save remark failed', e)
	}
}

const removeItem = async (id: number) => {
	try {
		await collectionApi.delete(id)
		items.value = items.value.filter((x) => x.id !== id)
	} catch (e) {
		console.error('[MyNote] remove collection failed', e)
	}
}

onMounted(() => {
	loadCollections()
})
</script>

<style scoped>
.page-layout {
	display: flex;
	min-height: 100vh;
	background: var(--hp-surface);
}

.main-content {
	flex: 1;
	margin-left: clamp(200px, 25vw, 320px);
	padding: 24px;
}

.page-container {
	max-width: 1180px;
	margin: 0 auto;
	display: grid;
	gap: 16px;
}

.hero-card,
.toolbar-card,
.list-card {
	border: 1px solid var(--hp-line);
	border-radius: 14px;
	background: linear-gradient(180deg, #1a263a, #142033);
}

.hero-card {
	padding: 16px 20px;
	display: flex;
	justify-content: space-between;
	align-items: center;
	gap: 16px;
}

.hero-card h1 {
	margin: 0;
	color: #e6f1ff;
	font-size: 20px;
}

.hero-card p {
	margin: 6px 0 0;
	color: #8fa5c7;
	font-size: 13px;
}

.hero-metrics {
	display: flex;
	gap: 12px;
}

.metric-box {
	min-width: 100px;
	border: 1px solid #2a4064;
	border-radius: 10px;
	padding: 12px 16px;
	background: var(--hp-surface);
	display: grid;
	gap: 4px;
}

.metric-label {
	font-size: 11px;
	color: #7f96bb;
}

.metric-value {
	font-size: 24px;
	color: #42b2ff;
	font-weight: 700;
}

.toolbar-card {
	padding: 12px 16px;
	display: grid;
	gap: 10px;
}

.search-row {
	display: grid;
	grid-template-columns: 1fr 200px;
	gap: 12px;
}

.search-row input,
.search-row select,
textarea {
	border: 1px solid #2a436b;
	border-radius: 8px;
	background: var(--hp-surface);
	color: var(--hp-ink);
	padding: 8px 12px;
	font-size: 13px;
	outline: none;
}

.chip-row {
	display: flex;
	gap: 8px;
	flex-wrap: wrap;
}

.chip {
	border: 1px solid #31517f;
	border-radius: 999px;
	background: var(--hp-surface);
	color: #9db6df;
	padding: 4px 10px;
	font-size: 12px;
	cursor: pointer;
}

.chip.active {
	border-color: #3a8af8;
	color: #dff0ff;
	background: #17386a;
}

.list-card {
	padding: 16px;
}

.list-head {
	display: grid;
	grid-template-columns: 1.2fr 1fr 160px;
	padding: 8px 12px;
	color: #8ea3c6;
	font-size: 12px;
	border-bottom: 1px solid var(--hp-line);
}

.note-row {
	display: grid;
	grid-template-columns: 1.2fr 1fr 160px;
	gap: 12px;
	padding: 12px;
	border-bottom: 1px dashed var(--hp-line);
	align-items: start;
}

.note-row h3 {
	margin: 0;
	font-size: 14px;
	color: #e8f2ff;
}

.question-link {
	border: none;
	background: transparent;
	padding: 0;
	margin: 0;
	font: inherit;
	color: #e8f2ff;
	cursor: pointer;
	text-align: left;
}

.question-link:hover {
	color: #58b0ff;
	text-decoration: underline;
}

.meta {
	margin-top: 6px;
	display: flex;
	flex-wrap: wrap;
	gap: 6px;
}

.meta span {
	font-size: 11px;
	color: #7f96bb;
	border: 1px solid #2a436b;
	border-radius: 999px;
	padding: 2px 6px;
}

.remark-col p {
	margin: 0;
	color: #d9e6fb;
	font-size: 13px;
	line-height: 1.5;
}

.remark-col .muted {
	color: #7088ae;
}

.action-col {
	display: flex;
	flex-direction: column;
	gap: 6px;
}

.action-col button {
	border-radius: 6px;
	padding: 6px 10px;
	font-size: 12px;
	cursor: pointer;
}

.line {
	border: 1px solid #3a8af8;
	background: transparent;
	color: #b9dcff;
}

.danger {
	border: 1px solid #7d3246;
	background: var(--hp-surface);
	color: var(--hp-ink);
}

.empty {
	padding: 36px 12px;
	text-align: center;
	color: #7a91b7;
}

.overlay {
	position: fixed;
	inset: 0;
	background: rgba(6, 10, 20, 0.7);
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 16px;
	z-index: 1200;
}

.modal {
	width: min(520px, 100%);
	border: 1px solid #2d4671;
	border-radius: 12px;
	background: var(--hp-surface);
	padding: 20px;
	display: grid;
	gap: 12px;
}

.modal h3 {
	margin: 0;
	color: #e8f2ff;
	font-size: 16px;
}

.modal-title {
	margin: 0;
	color: #8fa6c9;
	font-size: 13px;
}

.modal textarea {
	font-size: 13px;
	line-height: 1.6;
	resize: vertical;
}

.modal-actions {
	display: flex;
	justify-content: flex-end;
	gap: 10px;
	margin-top: 4px;
}

.modal-actions button {
	padding: 8px 16px;
	font-size: 13px;
	border-radius: 6px;
	cursor: pointer;
}

.primary {
	border: 1px solid #2e86f7;
	background: linear-gradient(90deg, #1f69d3, #3a8af8);
	color: #eef7ff;
}

.detail-modal {
	width: min(980px, 100%);
	max-height: min(84vh, 920px);
	overflow: auto;
	gap: 14px;
}

.detail-head {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding-bottom: 10px;
	border-bottom: 1px solid var(--hp-line);
}

.close-btn {
	border: none;
	background: transparent;
	color: #9eb5d8;
	cursor: pointer;
	font-size: 18px;
}

.detail-empty {
	padding: 20px 0;
	color: #89a2c9;
}

.detail-content {
	display: grid;
	gap: 12px;
}

.detail-tags {
	display: flex;
	flex-wrap: wrap;
	gap: 8px;
}

.detail-tags span {
	font-size: 12px;
	color: #89a8d3;
	border: 1px solid #2a436b;
	border-radius: 999px;
	padding: 4px 10px;
}

.detail-block {
	border: 1px solid var(--hp-line);
	border-radius: 10px;
	background: var(--hp-surface);
	padding: 12px;
	display: grid;
	gap: 8px;
}

.detail-block h4 {
	margin: 0;
	font-size: 14px;
	color: #c8defd;
}

.detail-block p {
	margin: 0;
	color: #dce8fc;
	line-height: 1.6;
	white-space: pre-wrap;
}

.detail-block.two-col {
	grid-template-columns: repeat(2, minmax(0, 1fr));
	gap: 10px;
}

.detail-time {
	font-size: 12px;
	color: #7e97bd;
}

@media (max-width: 980px) {
	.main-content {
		margin-left: 0;
		padding: 14px;
	}

	.hero-card {
		flex-direction: column;
		align-items: flex-start;
	}

	.search-row {
		grid-template-columns: 1fr;
	}

	.list-head,
	.note-row {
		grid-template-columns: 1fr;
	}

	.list-head {
		display: none;
	}

	.detail-block.two-col {
		grid-template-columns: 1fr;
	}
}
</style>
