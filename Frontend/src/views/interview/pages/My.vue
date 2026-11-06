<template>
	<div class="page-layout interview-profile">
		<Sidebar />
		<main class="main-content">
			<div class="page-container">
				<section class="hero-card" :class="{ loading: loading }">
					<div class="avatar-wrap">
						<img :src="profile.avatar" alt="avatar" class="avatar" />
					</div>
					<div class="hero-main">
						<div class="hero-head">
							<div>
								<h1 class="name">{{ profile.nickname }}</h1>
								<p class="sub">{{ profile.identity }}</p>
							</div>
							<div class="hero-actions">
								<button class="btn line" @click="openEdit">编辑资料</button>
								<button class="btn ghost" @click="openPasswordModal">修改密码</button>
								<button class="btn primary" @click="goInterview">开始面试</button>
								<button class="btn danger" @click="handleLogout">退出登录</button>
							</div>
						</div>
						<div class="tags">
							<span class="tag">目标岗位：{{ profile.targetPosition || '未设置' }}</span>
							<span class="tag">手机号：{{ profile.phone }}</span>
							<span class="tag">邮箱：{{ profile.email }}</span>
						</div>
					</div>
				</section>

				<section class="metric-grid">
					<article class="metric-card">
						<div class="metric-label">面试次数</div>
						<div class="metric-value">{{ profile.interviewCount }}</div>
					</article>
					<article class="metric-card">
						<div class="metric-label">AI 报告数</div>
						<div class="metric-value">{{ profile.aiReportCount }}</div>
					</article>
					<article class="metric-card">
						<div class="metric-label">平均分</div>
						<div class="metric-value">{{ profile.averageScore }}</div>
						<div class="metric-tip">{{ scoreLevelText }}</div>
					</article>
					<article class="metric-card">
						<div class="metric-label">资料完整度</div>
						<div class="metric-value">{{ profileCompletion }}%</div>
						<div class="meter"><span :style="{ width: `${profileCompletion}%` }"></span></div>
					</article>
				</section>

				<section class="content-grid">
					<div class="left-column">
						<article class="card">
							<h2>基础信息</h2>
							<div class="kv"><span>手机号</span><span>{{ profile.phone }}</span></div>
							<div class="kv"><span>邮箱</span><span>{{ profile.email }}</span></div>
							<div class="kv"><span>毕业年份</span><span>{{ profile.graduationYear }}</span></div>
						</article>

						<article class="card">
							<h2>技能标签</h2>
							<div class="skill-list" v-if="profile.skills.length">
								<span class="skill" v-for="(s, i) in profile.skills" :key="i">{{ s }}</span>
							</div>
							<p v-else class="empty">暂无技能标签</p>
						</article>
					</div>

					<article class="card work-experience-card">
						<h2>工作经历</h2>
						<div v-if="profile.workExperience" class="work-experience-content">{{ profile.workExperience }}</div>
						<p v-else class="empty">暂无工作经历</p>
					</article>
				</section>

				<section v-if="errorText" class="error-line">{{ errorText }}</section>
			</div>
		</main>
	</div>

	<Teleport to="body">
		<div v-if="editVisible" class="edit-overlay" @click.self="editVisible = false">
			<div class="edit-modal">
				<div class="edit-header">
					<h3>修改用户信息</h3>
					<button class="edit-close" @click="editVisible = false">✕</button>
				</div>
				<div class="edit-grid">
					<div class="form-item form-item-full avatar-upload-row">
						<div class="avatar-upload-preview">
							<img :src="editAvatarPreview" alt="avatar" />
						</div>
						<div class="avatar-upload-action">
							<button type="button" class="upload-avatar-btn" :disabled="avatarUploading" @click="triggerAvatarUpload">
								{{ avatarUploading ? '上传中...' : '上传头像' }}
							</button>
							<span class="upload-tip">建议上传 1:1 比例图片</span>
						</div>
						<input
							ref="avatarFileInput"
							type="file"
							accept="image/*"
							class="hidden-avatar-input"
							@change="handleAvatarFileChange"
						/>
					</div>
					<label class="form-item">
						<span>昵称</span>
						<input v-model="editForm.nickname" type="text" placeholder="请输入昵称" />
					</label>
					<label class="form-item">
						<span>手机号</span>
						<input v-model="editForm.phone" type="text" placeholder="请输入手机号" />
					</label>
					<label class="form-item">
						<span>邮箱</span>
						<input v-model="editForm.email" type="email" placeholder="请输入邮箱" />
					</label>
					<label class="form-item">
						<span>工作年限</span>
						<input v-model.number="editForm.work_experience_years" type="number" min="0" />
					</label>
					<label class="form-item">
						<span>毕业年份</span>
						<div class="year-picker" @click.stop>
							<button type="button" class="select-trigger" @click="toggleYearPicker">
								<span>{{ editForm.graduation_year || '请选择毕业年份' }}</span>
								<span class="caret">▾</span>
							</button>
							<div class="year-panel" v-if="yearPickerOpen">
								<div class="year-panel-head">
									<button type="button" class="year-nav-btn" :disabled="yearPageIndex === 0" @click="shiftYearPage(-1)">‹</button>
									<span class="year-range">{{ yearRangeLabel }}</span>
									<button type="button" class="year-nav-btn" :disabled="yearPageIndex >= yearPageCount - 1" @click="shiftYearPage(1)">›</button>
								</div>
								<div class="year-grid">
									<button
										type="button"
										class="year-cell"
										:class="{ active: editForm.graduation_year === year }"
										v-for="year in visibleGraduationYears"
										:key="year"
										@click="selectGraduationYear(year)"
									>
										{{ year }}
									</button>
								</div>
							</div>
						</div>
					</label>
					<label class="form-item">
						<span>目标岗位</span>
						<div class="multi-select" @click.stop>
							<button type="button" class="select-trigger" @click="toggleDropdown('position')">
								<span>{{ selectedPositionText }}</span>
								<span class="caret">▾</span>
							</button>
							<div class="select-menu" v-if="dropdownOpen.position">
								<button
									type="button"
									class="select-option"
									v-for="item in jobPositionOptions"
									:key="item.id"
									@click="toggleSelection('position', item.job_position)"
								>
									<span class="mark">{{ isSelected('position', item.job_position) ? '✅' : '⬜' }}</span>
									<span>{{ item.job_position }}</span>
								</button>
							</div>
						</div>
					</label>
					<label class="form-item">
						<span>技术栈</span>
						<div class="multi-select" @click.stop>
							<button type="button" class="select-trigger" @click="toggleDropdown('tech')">
								<span>{{ selectedTechStackText }}</span>
								<span class="caret">▾</span>
							</button>
							<div class="select-menu" v-if="dropdownOpen.tech">
								<button
									type="button"
									class="select-option"
									v-for="tech in filteredTechStackOptions"
									:key="tech"
									@click="toggleSelection('tech', tech)"
								>
									<span class="mark">{{ isSelected('tech', tech) ? '✅' : '⬜' }}</span>
									<span>{{ tech }}</span>
								</button>
							</div>
						</div>
					</label>
					<label class="form-item form-item-full">
						<span>技能标签（逗号分隔）</span>
						<input v-model="editForm.skill_tags" type="text" placeholder="例如：Python, Redis, MySQL" />
					</label>
					<label class="form-item form-item-full">
						<span>工作经历</span>
						<textarea v-model="editForm.work_experience" placeholder="请详细描述您的工作经历..." rows="4"></textarea>
					</label>
				</div>
				<p v-if="editError" class="edit-error">{{ editError }}</p>
				<div class="edit-footer">
					<button class="btn ghost" @click="editVisible = false">取消</button>
					<button class="btn primary" :disabled="editSaving" @click="saveProfile">
						{{ editSaving ? '保存中...' : '保存修改' }}
					</button>
				</div>
			</div>
		</div>
		<div v-if="passwordVisible" class="edit-overlay" @click.self="passwordVisible = false">
			<div class="edit-modal password-modal">
				<div class="edit-header">
					<h3>修改密码</h3>
					<button class="edit-close" @click="passwordVisible = false">✕</button>
				</div>
				<div class="edit-grid">
					<label class="form-item form-item-full">
						<span>手机号校验（请输入中间4位）</span>
						<div class="phone-check-row">
							<div class="phone-fixed-boxes">
								<span class="digit-box readonly" v-for="(digit, idx) in phonePrefixDigits" :key="`pre-${idx}`">{{ digit }}</span>
							</div>
							<div class="phone-middle-boxes">
								<input
									v-for="idx in 4"
									:key="idx"
									:ref="(el) => setPhoneDigitRef(el as HTMLInputElement | null, idx - 1)"
									class="digit-box"
									type="text"
									inputmode="numeric"
									maxlength="1"
									:value="phoneMiddleDigits[idx - 1]"
									@input="onPhoneDigitInput(idx - 1, $event)"
									@keydown="onPhoneDigitKeydown(idx - 1, $event)"
									@paste="onPhoneDigitPaste($event)"
								/>
							</div>
							<div class="phone-fixed-boxes">
								<span class="digit-box readonly" v-for="(digit, idx) in phoneSuffixDigits" :key="`suf-${idx}`">{{ digit }}</span>
							</div>
						</div>
					</label>
					<label class="form-item form-item-full" v-if="phoneVerificationPassed">
						<span>新密码</span>
						<input v-model="passwordForm.new_password" type="password" placeholder="请输入新密码" />
					</label>
					<label class="form-item form-item-full" v-if="phoneVerificationPassed">
						<span>确认新密码</span>
						<input v-model="passwordForm.confirm_password" type="password" placeholder="请再次输入新密码" />
					</label>
				</div>
				<p v-if="passwordError" class="edit-error">{{ passwordError }}</p>
				<div class="edit-footer">
					<button class="btn ghost" @click="passwordVisible = false">取消</button>
					<button class="btn primary" :disabled="passwordSaving" @click="submitPasswordChange">
						{{ passwordSaving ? '提交中...' : (phoneVerificationPassed ? '确认修改' : '验证手机号') }}
					</button>
				</div>
			</div>
		</div>
	</Teleport>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Sidebar from '../components/Sidebar.vue'
import { authApi, type InterviewJobPositionItem } from '../api/auth'
import { PATHS } from '../routes/paths'
import defaultAvatarImg from '@/assets/interview/1.png'
import { clearAuth } from '../../../utils/auth'

const router = useRouter()
const route = useRoute()

const handleLogout = () => {
  localStorage.removeItem('session_token')
  localStorage.removeItem('is_manager')
  localStorage.removeItem('user_id')
  localStorage.removeItem('nickname')
  clearAuth()
  router.replace(PATHS.LOGIN)
}

const profile = ref({
	nickname: '-',
	avatar: defaultAvatarImg,
	identity: '-',
	targetPosition: '-',
	interviewCount: 0,
	aiReportCount: 0,
	averageScore: 0,
	phone: '-',
	email: '-',
	graduationYear: '-',
	workYears: '-',
	workExperience: '',
	skills: [] as string[],
})
const loading = ref(false)
const errorText = ref('')
const editVisible = ref(false)
const editSaving = ref(false)
const editError = ref('')
const passwordVisible = ref(false)
const passwordSaving = ref(false)
const passwordError = ref('')
const passwordForm = reactive({
	new_password: '',
	confirm_password: '',
})
const accountPhoneRaw = ref('')
const phoneMiddleDigits = ref(['', '', '', ''])
const phoneDigitRefs = ref<Array<HTMLInputElement | null>>([])
const editForm = reactive({
	nickname: '',
	phone: '',
	email: '',
	work_experience_years: 0,
	work_experience: '',
	graduation_year: null as number | null,
	target_positions: [] as string[],
	tech_stacks: [] as string[],
	skill_tags: '',
	avatar_url: '',
})
const avatarFileInput = ref<HTMLInputElement | null>(null)
const editAvatarPreview = ref(defaultAvatarImg)
const localAvatarObjectUrl = ref('')
const avatarUploading = ref(false)
const currentUserId = ref<number>(0)
const jobPositionOptions = ref<InterviewJobPositionItem[]>([])
const dropdownOpen = reactive({
	position: false,
	tech: false,
})
const yearPickerOpen = ref(false)
const yearPageIndex = ref(0)
const YEAR_PAGE_SIZE = 20

const phonePrefix = computed(() => {
	if (!/^\d{11}$/.test(accountPhoneRaw.value)) return '***'
	return accountPhoneRaw.value.slice(0, 3)
})

const phoneSuffix = computed(() => {
	if (!/^\d{11}$/.test(accountPhoneRaw.value)) return '****'
	return accountPhoneRaw.value.slice(7)
})

const phonePrefixDigits = computed(() => phonePrefix.value.split(''))
const phoneSuffixDigits = computed(() => phoneSuffix.value.split(''))
const middleInput = computed(() => phoneMiddleDigits.value.join(''))
const phoneVerificationPassed = computed(() => {
	if (!/^\d{11}$/.test(accountPhoneRaw.value)) return false
	if (!/^\d{4}$/.test(middleInput.value)) return false
	return middleInput.value === accountPhoneRaw.value.slice(3, 7)
})

const profileCompletion = computed(() => {
	let total = 6
	let done = 0
	if (profile.value.targetPosition && profile.value.targetPosition !== '-') done += 1
	if (profile.value.phone && profile.value.phone !== '-') done += 1
	if (profile.value.email && profile.value.email !== '-') done += 1
	if (profile.value.graduationYear && profile.value.graduationYear !== '-') done += 1
	if (profile.value.workYears && profile.value.workYears !== '-') done += 1
	if (profile.value.skills.length > 0) done += 1
	return Math.round((done / total) * 100)
})

const techStackOptions = computed(() => {
	const set = new Set<string>()
	for (const item of jobPositionOptions.value) {
		const raw = String(item.tech_stack || '').trim()
		if (!raw) continue
		const parts = raw
			.replace(/\r\n/g, '\n')
			.split(/,|，|\n|\//)
			.map((x) => x.trim())
			.filter(Boolean)
		for (const p of parts) set.add(p)
	}
	return Array.from(set)
})

const filteredTechStackOptions = computed(() => {
	if (editForm.target_positions.length === 0) return [] as string[]
	const selectedPositions = new Set(editForm.target_positions)
	const set = new Set<string>()
	for (const item of jobPositionOptions.value) {
		if (!selectedPositions.has(item.job_position)) continue
		const raw = String(item.tech_stack || '').trim()
		if (!raw) continue
		const parts = raw
			.replace(/\r\n/g, '\n')
			.split(/,|，|\n|\//)
			.map((x) => x.trim())
			.filter(Boolean)
		for (const p of parts) set.add(p)
	}
	return Array.from(set)
})

const selectedPositionText = computed(() => {
	if (editForm.target_positions.length === 0) return '请选择岗位'
	return editForm.target_positions.join('、')
})

const selectedTechStackText = computed(() => {
	if (editForm.target_positions.length === 0) return '请先选择目标岗位'
	if (editForm.tech_stacks.length === 0) return '请选择技术栈'
	return editForm.tech_stacks.join('、')
})

watch(
	() => editForm.target_positions,
	() => {
		const allowed = new Set(filteredTechStackOptions.value)
		editForm.tech_stacks = editForm.tech_stacks.filter((x) => allowed.has(x))
	},
	{ deep: true }
)

function toggleDropdown(kind: 'position' | 'tech') {
	yearPickerOpen.value = false
	if (kind === 'position') {
		dropdownOpen.position = !dropdownOpen.position
		dropdownOpen.tech = false
		return
	}
	dropdownOpen.tech = !dropdownOpen.tech
	dropdownOpen.position = false
}

function isSelected(kind: 'position' | 'tech', value: string) {
	if (kind === 'position') {
		return editForm.target_positions.includes(value)
	}
	return editForm.tech_stacks.includes(value)
}

function toggleSelection(kind: 'position' | 'tech', value: string) {
	if (kind === 'position') {
		if (editForm.target_positions.includes(value)) {
			editForm.target_positions = editForm.target_positions.filter((x) => x !== value)
		} else {
			editForm.target_positions = [...editForm.target_positions, value]
		}
		return
	}
	if (editForm.tech_stacks.includes(value)) {
		editForm.tech_stacks = editForm.tech_stacks.filter((x) => x !== value)
	} else {
		editForm.tech_stacks = [...editForm.tech_stacks, value]
	}
}

const scoreLevelText = computed(() => {
	const s = Number(profile.value.averageScore || 0)
	if (s >= 85) return '状态优秀'
	if (s >= 70) return '持续提升中'
	if (s > 0) return '建议继续训练'
	return '暂无评分记录'
})

const graduationYearOptions = computed(() => {
	const current = new Date().getFullYear()
	const years: number[] = []
	for (let y = current + 2; y >= 1990; y -= 1) {
		years.push(y)
	}
	return years
})

const yearPageCount = computed(() => Math.max(1, Math.ceil(graduationYearOptions.value.length / YEAR_PAGE_SIZE)))

const visibleGraduationYears = computed(() => {
	const start = yearPageIndex.value * YEAR_PAGE_SIZE
	return graduationYearOptions.value.slice(start, start + YEAR_PAGE_SIZE)
})

const yearRangeLabel = computed(() => {
	const arr = visibleGraduationYears.value
	if (arr.length === 0) return ''
	return `${arr[0]} - ${arr[arr.length - 1]}`
})

function syncYearPageWithSelected() {
	const selected = editForm.graduation_year
	if (!selected) {
		yearPageIndex.value = 0
		return
	}
	const idx = graduationYearOptions.value.findIndex((x) => x === selected)
	yearPageIndex.value = idx >= 0 ? Math.floor(idx / YEAR_PAGE_SIZE) : 0
}

function toggleYearPicker() {
	yearPickerOpen.value = !yearPickerOpen.value
	if (yearPickerOpen.value) {
		dropdownOpen.position = false
		dropdownOpen.tech = false
		syncYearPageWithSelected()
	}
}

function shiftYearPage(delta: number) {
	const next = yearPageIndex.value + delta
	if (next < 0 || next >= yearPageCount.value) return
	yearPageIndex.value = next
}

function selectGraduationYear(year: number) {
	editForm.graduation_year = year
	yearPickerOpen.value = false
}

function maskPhone(phone: string): string {
	if (!phone || phone.length < 8) return phone || '-'
	return phone.slice(0, 3) + '****' + phone.slice(-4)
}

function goInterview() {
	router.push(PATHS.AI_MOCK_INTERVIEW)
}

function revokeLocalAvatarUrl() {
	if (!localAvatarObjectUrl.value) return
	URL.revokeObjectURL(localAvatarObjectUrl.value)
	localAvatarObjectUrl.value = ''
}

function triggerAvatarUpload() {
	avatarFileInput.value?.click()
}

async function handleAvatarFileChange(event: Event) {
	const input = event.target as HTMLInputElement
	const file = input.files?.[0]
	if (!file) return
	if (!file.type.startsWith('image/')) {
		editError.value = '请上传图片格式文件'
		input.value = ''
		return
	}
	if (file.size > 5 * 1024 * 1024) {
		editError.value = '头像图片大小不能超过 5MB'
		input.value = ''
		return
	}
	revokeLocalAvatarUrl()
	const localUrl = URL.createObjectURL(file)
	localAvatarObjectUrl.value = localUrl
	editAvatarPreview.value = localUrl
	editError.value = ''
	avatarUploading.value = true
	try {
		const res = await authApi.uploadAvatar(file)
		const url = String(res.url || '').trim()
		if (!url) throw new Error('头像上传成功但未返回地址')
		editForm.avatar_url = url
		revokeLocalAvatarUrl()
		editAvatarPreview.value = url
	} catch (e: any) {
		editError.value = e?.message || '头像上传失败，请稍后重试'
	} finally {
		avatarUploading.value = false
		input.value = ''
	}
}

function setPhoneDigitRef(el: HTMLInputElement | null, index: number) {
	phoneDigitRefs.value[index] = el
}

function onPhoneDigitInput(index: number, event: Event) {
	const input = event.target as HTMLInputElement
	const digit = input.value.replace(/\D/g, '').slice(-1)
	phoneMiddleDigits.value[index] = digit
	passwordError.value = ''
	input.value = digit
	if (digit && index < 3) {
		phoneDigitRefs.value[index + 1]?.focus()
	}
}

function onPhoneDigitKeydown(index: number, event: KeyboardEvent) {
	if (event.key !== 'Backspace') return
	if (phoneMiddleDigits.value[index]) return
	if (index <= 0) return
	phoneDigitRefs.value[index - 1]?.focus()
}

function onPhoneDigitPaste(event: ClipboardEvent) {
	event.preventDefault()
	const pasted = (event.clipboardData?.getData('text') || '').replace(/\D/g, '').slice(0, 4)
	for (let i = 0; i < 4; i += 1) {
		phoneMiddleDigits.value[i] = pasted[i] || ''
	}
	const focusIndex = Math.min(Math.max(pasted.length - 1, 0), 3)
	phoneDigitRefs.value[focusIndex]?.focus()
}

async function openPasswordModal() {
	passwordError.value = ''
	try {
		const p = await loadProfile()
		accountPhoneRaw.value = String(p.phone || '').trim()
		phoneMiddleDigits.value = ['', '', '', '']
		passwordForm.new_password = ''
		passwordForm.confirm_password = ''
		passwordVisible.value = true
		setTimeout(() => {
			phoneDigitRefs.value[0]?.focus()
		}, 0)
	} catch (e: any) {
		passwordError.value = e?.message || '加载手机号失败'
	}
}

async function submitPasswordChange() {
	passwordError.value = ''
	if (!currentUserId.value) {
		passwordError.value = '用户ID缺失，无法修改密码'
		return
	}
	if (!/^\d{11}$/.test(accountPhoneRaw.value)) {
		passwordError.value = '当前账号手机号异常，无法完成校验'
		return
	}
	const firstEmptyIndex = phoneMiddleDigits.value.findIndex((x) => !x)
	if (firstEmptyIndex >= 0) {
		passwordError.value = '手机号中间4位需要全部输入'
		phoneDigitRefs.value[firstEmptyIndex]?.focus()
		return
	}
	if (!/^\d{4}$/.test(middleInput.value)) {
		passwordError.value = '请输入4位数字'
		return
	}
	if (!phoneVerificationPassed.value) {
		passwordError.value = '手机号中间4位校验失败'
		return
	}
	if (!passwordForm.new_password.trim() || !passwordForm.confirm_password.trim()) {
		passwordError.value = '手机号校验通过后，请先填写新密码和确认密码'
		return
	}
	if (!passwordForm.new_password.trim()) {
		passwordError.value = '请输入新密码'
		return
	}
	if (passwordForm.new_password !== passwordForm.confirm_password) {
		passwordError.value = '两次输入的新密码不一致'
		return
	}
	passwordSaving.value = true
	try {
		await authApi.resetPassword({
			id: currentUserId.value,
			phone: accountPhoneRaw.value,
			new_password: passwordForm.new_password,
		})
		passwordVisible.value = false
		alert('密码修改成功')
	} catch (e: any) {
		passwordError.value = e?.message || '密码修改失败'
	} finally {
		passwordSaving.value = false
	}
}

function applyProfile(p: any) {
	const expYears = p.work_experience_years ?? 0
	currentUserId.value = Number(p.id || 0)
	accountPhoneRaw.value = String(p.phone || '').trim()
	const avatarUrl = String((p as any).avatar_url || '').trim()
	profile.value = {
		nickname: p.nickname || '-',
		avatar: avatarUrl || defaultAvatarImg,
		identity: expYears === 0 ? '应届生' : `${expYears} 年经验`,
		targetPosition: (p.target_position || '').trim() || '-',
		interviewCount: Number(p.interview_count || 0),
		aiReportCount: Number((p as any).ai_report_count || 0),
		averageScore: Number(p.average_score || 0),
		phone: maskPhone(p.phone || ''),
		email: p.email || '-',
		graduationYear: p.graduation_year ? String(p.graduation_year) : '-',
		workYears: expYears === 0 ? '在校实习' : `${expYears} 年工作经验`,
		workExperience: (p as any).work_experience || '',
		skills: p.skill_tags
			? String(p.skill_tags).split(',').map((x) => x.trim()).filter(Boolean)
			: [],
	}
}

async function loadProfile() {
	const p = await authApi.getCurrentProfile()
	applyProfile(p)
	// 同步头像和昵称到 localStorage，供 Sidebar 读取
	const avatarUrl = String((p as any).avatar_url || '').trim()
	if (avatarUrl) {
		localStorage.setItem('avatar_url', avatarUrl)
	}
	if (p.nickname) {
		localStorage.setItem('nickname', p.nickname)
	}
	return p
}

async function openEdit() {
	editError.value = ''
	try {
		if (jobPositionOptions.value.length === 0) {
			const optionsRes = await authApi.getInterviewJobPositions()
			jobPositionOptions.value = optionsRes.items || []
		}
		const p = await loadProfile()
		editForm.nickname = p.nickname || ''
		editForm.phone = p.phone || ''
		editForm.email = p.email || ''
		editForm.work_experience_years = p.work_experience_years ?? 0
		editForm.graduation_year = p.graduation_year ?? null
		editForm.target_positions = String(p.target_position || '')
			.split(',')
			.map((x) => x.trim())
			.filter(Boolean)
			.filter((x) => jobPositionOptions.value.some((jp) => jp.job_position === x))
		editForm.tech_stacks = String((p as any).tech_stack || '')
			.split(',')
			.map((x) => x.trim())
			.filter(Boolean)
			.filter((x) => filteredTechStackOptions.value.includes(x))
		editForm.skill_tags = p.skill_tags || ''
		editForm.work_experience = (p as any).work_experience || ''
		editForm.avatar_url = String((p as any).avatar_url || '').trim()
		revokeLocalAvatarUrl()
		editAvatarPreview.value = editForm.avatar_url || defaultAvatarImg
		dropdownOpen.position = false
		dropdownOpen.tech = false
		yearPickerOpen.value = false
		editVisible.value = true
	} catch (e: any) {
		editError.value = e?.message || '加载可编辑信息失败'
	}
}

async function saveProfile() {
	if (!currentUserId.value) {
		editError.value = '用户ID缺失，无法保存'
		return
	}
	editSaving.value = true
	editError.value = ''
	if (editForm.target_positions.length === 0) {
		editError.value = '请选择目标岗位'
		editSaving.value = false
		return
	}
	if (editForm.tech_stacks.length === 0) {
		editError.value = '请选择技术栈'
		editSaving.value = false
		return
	}
	try {
		const selectedTargetPosition = editForm.target_positions.join(',')
		await authApi.updateProfile({
			id: currentUserId.value,
			nickname: editForm.nickname || undefined,
			phone: editForm.phone || undefined,
			email: editForm.email || undefined,
			work_experience_years: Number(editForm.work_experience_years || 0),
			work_experience: editForm.work_experience || null,
			graduation_year: editForm.graduation_year,
			target_position: selectedTargetPosition || null,
			tech_stack: editForm.tech_stacks.join(',') || null,
			skill_tags: editForm.skill_tags || null,
			avatar_url: editForm.avatar_url || undefined,
		})
		localStorage.setItem('job_role', selectedTargetPosition)
		await loadProfile()
		editVisible.value = false
		const returnTo = typeof route.query.returnTo === 'string' ? route.query.returnTo : ''
		if (route.query.edit === 'target-position' && returnTo.startsWith('/interview/')) {
			await router.replace(returnTo)
		}
	} catch (e: any) {
		editError.value = e?.message || '保存失败，请稍后重试'
	} finally {
		editSaving.value = false
	}
}

onMounted(async () => {
	loading.value = true
	errorText.value = ''
	try {
		await loadProfile()
		if (route.query.edit === 'target-position') {
			await openEdit()
		}
	} catch (e) {
		console.error('[My] load profile failed', e)
		errorText.value = '个人信息加载失败，请稍后刷新重试'
	} finally {
		loading.value = false
	}
})

onBeforeUnmount(() => {
	revokeLocalAvatarUrl()
})
</script>

<style scoped>
.page-layout {
	display: flex;
	min-height: 100vh;
	background: #121820;
}

.main-content {
	flex: 1;
	margin-left: clamp(200px, 25vw, 320px);
	padding: 24px 28px;
}

.page-container {
	max-width: 1400px;
	margin: 0 auto;
	display: grid;
	gap: 20px;
}

.hero-card,
.card,
.metric-card {
	border: 1px solid #2a3441;
	border-radius: 12px;
	background: #1b2431;
}

.hero-card {
	padding: 20px;
	display: grid;
	grid-template-columns: 88px 1fr;
	gap: 16px;
	align-items: center;
}

.hero-card.loading {
	opacity: 0.75;
}

.avatar-wrap {
	width: 88px;
	height: 88px;
	border-radius: 50%;
	overflow: hidden;
	border: 2px solid #2e69c7;
}

.avatar {
	width: 100%;
	height: 100%;
	object-fit: cover;
	object-position: center;
}

.hero-main {
	display: grid;
	gap: 10px;
}

.hero-head {
	display: flex;
	justify-content: space-between;
	gap: 12px;
	align-items: center;
}

.hero-actions {
	display: flex;
	flex-wrap: wrap;
	gap: 8px;
}

.hero-actions .btn:hover { filter: brightness(.94); }
.hero-actions .btn:focus-visible {
	outline: 2px solid var(--hp-pink-ink);
	outline-offset: 2px;
}

.hero-actions .btn:is(.line, .ghost) {
	background: var(--hp-yellow);
	border: 1px solid var(--hp-line-strong);
	color: var(--hp-ink);
}

.hero-actions .btn.ghost { background: var(--hp-pink); }

.hero-actions .btn.danger {
	background: var(--hp-surface);
	border: 1px solid #a85a50;
	color: #8f342b;
}

.btn {
	padding: 8px 14px;
	border-radius: 8px;
	font-size: 13px;
	cursor: pointer;
}

.btn.line {
	border: 1px solid #3d5f99;
	background: transparent;
	color: #c3d9ff;
}

.btn.ghost {
	border: 1px solid #35558f;
	color: #b4ceff;
	background: #12203d;
}

.btn.primary {
	border: 1px solid #3a8af8;
	color: #e8f4ff;
	background: linear-gradient(90deg, #1e6bd6, #3a8af8);
}

.btn.danger {
	border: 1px solid #dc2626;
	color: #fee2e2;
	background: linear-gradient(90deg, #991b1b, #dc2626);
}

.name {
	margin: 0;
	color: #ffffff;
	font-size: 24px;
	font-weight: 600;
}

.sub {
	margin: 4px 0 0;
	color: #8b9aae;
	font-size: 13px;
}

.tags {
	display: flex;
	flex-wrap: wrap;
	gap: 8px;
}

.tag {
	border: 1px solid #2a3441;
	color: #9ca3af;
	background: #0f121d;
	border-radius: 999px;
	padding: 5px 10px;
	font-size: 12px;
}

.metric-grid {
	display: grid;
	grid-template-columns: repeat(4, minmax(0, 1fr));
	gap: 20px;
}

.metric-card {
	padding: 20px;
	min-height: 96px;
	display: grid;
	align-content: start;
	gap: 8px;
}

.metric-label {
	font-size: 13px;
	color: #8b9aae;
}

.metric-value {
	font-size: 28px;
	line-height: 1;
	font-weight: 700;
	color: var(--hp-ink);
	font-variant-numeric: tabular-nums;
}

.metric-tip {
	font-size: 12px;
	color: #3e603b;
}

.meter {
	width: 100%;
	height: 4px;
	background: #2a3441;
	border-radius: 2px;
	overflow: hidden;
}

.meter span {
	display: block;
	height: 100%;
	background: linear-gradient(90deg, #3a7bc8, #60a5fa);
}

.content-grid {
	display: flex;
	gap: 20px;
	align-items: flex-start;
}

.left-column {
	display: flex;
	flex-direction: column;
	gap: 20px;
	flex: 0 0 auto;
	max-width: 360px;
	min-width: 280px;
}

.card {
	padding: 20px;
}

.work-experience-card {
	flex: 1;
	min-width: 0;
}

.card h2 {
	margin: 0 0 16px;
	color: #ffffff;
	font-size: 16px;
	font-weight: 600;
}

.kv {
	display: flex;
	justify-content: space-between;
	color: var(--hp-muted);
	border-bottom: 1px solid var(--hp-line);
	padding: 10px 0;
	gap: 12px;
	font-size: 13px;
}

.skill-list {
	display: flex;
	flex-wrap: wrap;
	gap: 8px;
}

.skill {
	padding: 5px 10px;
	font-size: 12px;
	color: #9ca3af;
	border: 1px solid #2a3441;
	background: #0f121d;
	border-radius: 999px;
}

.empty {
	color: #5a6a7d;
	font-size: 13px;
}

.error-line {
	color: #ff9d9d;
	font-size: 13px;
	padding: 2px 2px 0;
}

.edit-overlay {
	position: fixed;
	inset: 0;
	background: rgba(3, 8, 20, 0.72);
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 16px;
	z-index: 1000;
}

.edit-modal {
	width: min(560px, 100%);
	border: 1px solid #2a3441;
	border-radius: 12px;
	background: #1b2431;
	padding: 20px;
}

.password-modal {
	width: min(480px, 100%);
}

.edit-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 12px;
}

.edit-header h3 {
	margin: 0;
	font-size: 16px;
	font-weight: 600;
	color: #ffffff;
}

.edit-close {
	border: 1px solid #2a3441;
	background: #1b2431;
	color: #9ca3af;
	border-radius: 8px;
	width: 28px;
	height: 28px;
	cursor: pointer;
	font-size: 14px;
}

.edit-grid {
	display: grid;
	grid-template-columns: repeat(2, minmax(0, 1fr));
	gap: 14px 16px;
}

.form-item {
	display: grid;
	gap: 6px;
}

.form-item span {
	font-size: 12px;
	color: #9ca3af;
}

.form-item input {
	border: 1px solid #2a3441;
	background: #0f121d;
	color: #ffffff;
	border-radius: 8px;
	padding: 8px 12px;
	outline: none;
	font-size: 13px;
}

.form-item textarea {
	border: 1px solid #2a3441;
	background: #0f121d;
	color: #ffffff;
	border-radius: 8px;
	padding: 8px 12px;
	outline: none;
	font-size: 13px;
	resize: vertical;
	min-height: 80px;
	font-family: inherit;
	line-height: 1.5;
}

.work-experience-content {
	color: #b4c0d0;
	font-size: 13px;
	line-height: 1.6;
	white-space: pre-wrap;
	word-break: break-word;
}

.avatar-upload-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 16px;
	padding: 2px 0 8px;
}

.avatar-upload-preview {
	width: 60px;
	height: 60px;
	border-radius: 50%;
	overflow: hidden;
	border: 2px solid #3a7bc8;
	background: #1b2431;
	flex-shrink: 0;
}

.avatar-upload-preview img {
	width: 100%;
	height: 100%;
	object-fit: cover;
}

.avatar-upload-action {
	flex: 1;
	display: grid;
	gap: 6px;
	justify-items: start;
}

.upload-avatar-btn {
	border: 1px dashed #3a7bc8;
	background: #1b2431;
	color: #9ca3af;
	border-radius: 8px;
	padding: 8px 12px;
	cursor: pointer;
	font-size: 13px;
}

.upload-avatar-btn:hover {
	background: #2a3441;
}

.upload-tip {
	font-size: 11px;
	color: #5a6a7d;
}

.hidden-avatar-input {
	display: none;
}

.phone-check-row {
	display: flex;
	align-items: center;
	justify-content: center;
	gap: 8px;
	flex-wrap: nowrap;
	width: 100%;
}

.phone-fixed-boxes {
	display: grid;
	grid-auto-flow: column;
	grid-auto-columns: 28px;
	gap: 5px;
}

.phone-middle-boxes {
	display: grid;
	grid-template-columns: repeat(4, 28px);
	gap: 5px;
	padding: 0;
	margin: 0;
}

.digit-box {
	width: 28px;
	height: 32px;
	box-sizing: border-box;
	border-radius: 6px;
	border: 1px solid #2a3441;
	background: #0f121d;
	color: #ffffff;
	text-align: center;
	font-size: 14px;
	font-weight: 600;
	outline: none;
	padding: 0;
	margin: 0;
	appearance: none;
	-webkit-appearance: none;
}

.digit-box.readonly {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	background: #1b2431;
	color: #9ca3af;
	border-color: #2a3441;
}

.digit-box:focus {
	border-color: #3a7bc8;
	box-shadow: none;
}

.multi-select {
	position: relative;
}

.year-picker {
	position: relative;
}

.select-trigger {
	width: 100%;
	border: 1px solid #2a3441;
	background: #0f121d;
	color: #ffffff;
	border-radius: 8px;
	padding: 8px 12px;
	outline: none;
	display: flex;
	justify-content: space-between;
	align-items: center;
	cursor: pointer;
	text-align: left;
	font-size: 13px;
}

.caret {
	color: #6b7280;
	margin-left: 8px;
}

.select-menu {
	position: absolute;
	left: 0;
	right: 0;
	top: calc(100% + 6px);
	background: #1b2431;
	border: 1px solid #2a3441;
	border-radius: 8px;
	max-height: 210px;
	overflow-y: auto;
	-ms-overflow-style: none;
	scrollbar-width: none;
	padding: 6px;
	z-index: 20;
}

.select-menu::-webkit-scrollbar {
	width: 0;
	height: 0;
	display: none;
}

.select-option {
	width: 100%;
	border: 0;
	background: transparent;
	color: #ffffff;
	display: flex;
	align-items: center;
	gap: 8px;
	padding: 7px 8px;
	border-radius: 6px;
	text-align: left;
	cursor: pointer;
	font-size: 13px;
}

.select-option:hover {
	background: #2a3441;
}

.mark {
	width: 20px;
	text-align: center;
}

.year-panel {
	position: absolute;
	left: 0;
	right: 0;
	top: calc(100% + 6px);
	background: #1b2431;
	border: 1px solid #2a3441;
	border-radius: 8px;
	padding: 8px;
	z-index: 25;
}

.year-panel-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 8px;
}

.year-range {
	font-size: 12px;
	color: #9ca3af;
}

.year-nav-btn {
	width: 26px;
	height: 26px;
	border-radius: 6px;
	border: 1px solid #2a3441;
	background: #0f121d;
	color: #9ca3af;
	cursor: pointer;
}

.year-nav-btn:disabled {
	opacity: 0.45;
	cursor: not-allowed;
}

.year-grid {
	display: grid;
	grid-template-columns: repeat(5, minmax(0, 1fr));
	gap: 6px;
}

.year-cell {
	height: 30px;
	border-radius: 6px;
	border: 1px solid #2a3441;
	background: #0f121d;
	color: #9ca3af;
	font-size: 12px;
	cursor: pointer;
}

.year-cell:hover {
	background: #2a3441;
}

.year-cell.active {
	border-color: #3a7bc8;
	background: #3a7bc8;
	color: #ffffff;
}

.multi-tip {
	font-size: 11px;
	color: #7f9bc8;
}

.form-item-full {
	grid-column: 1 / -1;
}

.edit-error {
	margin: 12px 0 0;
	color: #ef4444;
	font-size: 12px;
}

.edit-footer {
	display: flex;
	justify-content: flex-end;
	gap: 10px;
	margin-top: 14px;
}

@media (max-width: 980px) {
	.main-content {
		margin-left: 0;
	}

	.hero-head {
		flex-direction: column;
		align-items: flex-start;
	}

	.metric-grid {
		grid-template-columns: repeat(2, minmax(0, 1fr));
	}

	.content-grid {
		flex-direction: column;
	}

	.left-column {
		max-width: none;
		min-width: 0;
	}

	.work-experience-card {
		flex: none;
	}

	.edit-grid {
		grid-template-columns: 1fr;
	}

	.year-grid {
		grid-template-columns: repeat(4, minmax(0, 1fr));
	}
}
</style>
