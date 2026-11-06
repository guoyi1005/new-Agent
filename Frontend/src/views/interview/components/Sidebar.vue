<template>
  <div class="sidebar-wrapper">
    <aside class="sidebar">
      <!-- Logo 区域 -->
      <div class="sidebar-header">
        <div class="logo-text">
          <div class="logo">折跃同步</div>
          <div class="logo-sub">智能面试助手</div>
        </div>
      </div>

      <!-- 用户信息与功能区域 -->
      <div class="user-section">
        <div class="user-card" @click="navigateTo(PATHS.MY)">
          <div class="user-avatar">
            <img v-if="userAvatar" :src="userAvatar" alt="" />
          </div>
          <div class="user-info">
            <div class="user-name">{{ userNickname || '\u00a0' }}</div>
            <div class="user-status">点击进入个人中心</div>
          </div>
          <div class="user-arrow">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M9 18l6-6-6-6"/>
            </svg>
          </div>
        </div>
      </div>

      <!-- 导航菜单 -->
      <nav class="sidebar-nav">
        <div class="nav-section">
          <a
            v-for="item in menuItems"
            :key="item.path"
            :href="item.path"
            class="nav-item"
            :class="{ active: isActive(item.path) }"
            @click.prevent="navigateTo(item.path)"
          >
            <span class="nav-icon" v-html="item.icon"></span>
            <span class="nav-text">{{ item.name }}</span>
            <span class="nav-indicator" v-if="isActive(item.path)"></span>
          </a>
        </div>

        <!-- 底部操作区 -->
        <div class="nav-bottom">
          <!-- AI 职业导师卡片 -->
          <div class="mentor-card">
            <div class="mentor-avatar">
              <img src="@/assets/interview/Robot.png" alt="bot" style="width:100%;height:100%;object-fit:contain;border-radius:10px;" />
            </div>
            <div class="mentor-title">AI 职业导师</div>
            <div class="mentor-desc">基于你的能力模型，为你量身定制面试策略。</div>
            <button class="mentor-btn" @click="goToCareerPlan">开始对话</button>
          </div>

          <!-- 管理员功能 -->
          <a
            v-if="isManager"
            :href="PATHS.CREATE_QUESTION_BANK"
            class="nav-item nav-item-admin"
            :class="{ active: isActive(PATHS.CREATE_QUESTION_BANK) }"
            @click.prevent="navigateTo(PATHS.CREATE_QUESTION_BANK)"
          >
            <span class="nav-icon" v-html="icons.createQuestionBank"></span>
            <span class="nav-text">新建题库</span>
            <span class="nav-badge">管理员</span>
          </a>
          <a
            v-if="isManager"
            :href="PATHS.MOCK_QUESTION_FILTER"
            class="nav-item nav-item-admin"
            :class="{ active: isActive(PATHS.MOCK_QUESTION_FILTER) }"
            @click.prevent="navigateTo(PATHS.MOCK_QUESTION_FILTER)"
          >
            <span class="nav-icon" v-html="icons.mockQuestionFilter"></span>
            <span class="nav-text">模拟筛选题目</span>
            <span class="nav-badge">管理员</span>
          </a>
        </div>
      </nav>
    </aside>
  </div>
</template>

<script>
let cachedSidebarProfile = null;
</script>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { PATHS } from '../routes/paths';
import { authApi } from '../api/auth';
import { clearAuth, getToken, getUserInfo } from '../../../utils/auth';

const emit = defineEmits(['customPath']);

const route = useRoute();
const router = useRouter();
const currentPath = ref(route.path);

watch(
  () => route.path,
  (path) => {
    currentPath.value = path;
  },
);

const session = getToken() || localStorage.getItem('session_token');
const account = getUserInfo();
const initialProfile = {
  nickname: account?.nickname || account?.realName || account?.username || '',
  avatar: account?.avatar || account?.avatar_url || '',
};
const userInfo = ref(
  session && cachedSidebarProfile?.session === session
    ? cachedSidebarProfile.profile
    : initialProfile,
);

const userNickname = computed(() => userInfo.value.nickname);
const userAvatar = computed(() => {
  if (userInfo.value.avatar) return userInfo.value.avatar;
  return userInfo.value.nickname
    ? `https://api.dicebear.com/7.x/adventurer/svg?seed=${encodeURIComponent(userInfo.value.nickname)}`
    : '';
});

// 加载用户信息
async function loadUserInfo() {
  try {
    const profile = await authApi.getCurrentProfile();
    if (session !== (getToken() || localStorage.getItem('session_token'))) return;
    userInfo.value = {
      nickname: profile.nickname || initialProfile.nickname,
      avatar: profile.avatar_url || initialProfile.avatar,
    };
    if (session) cachedSidebarProfile = { session, profile: userInfo.value };
  } catch {
    // Keep the signed-in account visible if refreshing its profile fails.
  }
}

// 读取管理员标识
const isManager = computed(() => localStorage.getItem('is_manager') === '1');

// 线性图标 SVG 定义
const icons = {
  dashboard: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
    <rect x="3" y="3" width="7" height="7" rx="1"/>
    <rect x="14" y="3" width="7" height="7" rx="1"/>
    <rect x="3" y="14" width="7" height="7" rx="1"/>
    <rect x="14" y="14" width="7" height="7" rx="1"/>
  </svg>`,
  
  mockInterview: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
    <path d="M15.6 11.6L22 7v10l-6.4-4.6"/>
    <rect x="2" y="6" width="14" height="12" rx="2"/>
  </svg>`,

  questionBank: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
    <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
    <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
    <path d="M8 7h8"/>
    <path d="M8 11h8"/>
    <path d="M8 15h4"/>
  </svg>`,
  
  wrongQuestion: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
    <path d="M12 9v4"/>
    <path d="M12 17h.01"/>
    <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
  </svg>`,
  
  myNote: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
    <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/>
    <polyline points="14,2 14,8 20,8"/>
    <path d="M12 18v-6"/>
    <path d="M9 15l3-3 3 3"/>
  </svg>`,
  
  learning: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
    <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/>
    <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/>
  </svg>`,
  
  aiCareer: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
    <rect x="3" y="11" width="18" height="10" rx="2"/>
    <circle cx="12" cy="5" r="3"/>
    <path d="M8 15h.01"/>
    <path d="M16 15h.01"/>
    <path d="M9 18h6"/>
  </svg>`,
  
  profile: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
    <circle cx="12" cy="7" r="4"/>
  </svg>`,
  
  createQuestionBank: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
    <path d="M12 5v14"/>
    <path d="M5 12h14"/>
    <circle cx="12" cy="12" r="10"/>
  </svg>`,

  mockQuestionFilter: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
    <path d="M3 5h18"/>
    <path d="M6 12h12"/>
    <path d="M10 19h4"/>
    <circle cx="17" cy="17" r="3"/>
    <path d="M19.5 19.5 22 22"/>
  </svg>`,
  
  logout: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
    <polyline points="16,17 21,12 16,7"/>
    <line x1="21" y1="12" x2="9" y2="12"/>
  </svg>`
};

const menuItems = [
  { name: '能力面板', path: PATHS.INDEX, icon: icons.dashboard },
  { name: '模拟面试', path: PATHS.AI_MOCK_INTERVIEW, icon: icons.mockInterview },
  { name: '题库练习', path: PATHS.QUESTION_BANK, icon: icons.questionBank },
  { name: '错题本', path: PATHS.WRONG_QUESTION_BOOK, icon: icons.wrongQuestion },
  { name: '个人题库', path: PATHS.MY_NOTE, icon: icons.myNote },
  { name: '学习资源', path: PATHS.ABILITY_IMPROVEMENT, icon: icons.learning },
  { name: 'AI职业规划', path: PATHS.AI_CAREER_PLAN, icon: icons.aiCareer },
];

const isActive = (path) => {
  return currentPath.value === path || currentPath.value.startsWith(`${path}/`);
};

const navigateTo = (path) => {
  router.push(path);
};

const handleLogout = () => {
  cachedSidebarProfile = null;
  localStorage.removeItem('session_token')
  localStorage.removeItem('is_manager')
  localStorage.removeItem('user_id')
  localStorage.removeItem('nickname')
  clearAuth()
  router.replace(PATHS.LOGIN)
}

const goToCareerPlan = () => {
  router.push(PATHS.AI_CAREER_PLAN);
};

onMounted(() => {
  loadUserInfo();
});
</script>

<style scoped>
/* CSS 变量定义 */
.sidebar-wrapper {
  --sidebar-bg: #0f121d;
  --sidebar-bg-secondary: #141827;
  --sidebar-bg-hover: rgba(58, 123, 200, 0.08);
  --sidebar-bg-active: rgba(58, 123, 200, 0.12);
  --sidebar-border: rgba(255, 255, 255, 0.06);
  --sidebar-text: #8b95a5;
  --sidebar-text-hover: #e4e8ed;
  --sidebar-text-active: #3a7bc8;
  --sidebar-primary: #3a7bc8;
  --sidebar-primary-glow: rgba(58, 123, 200, 0.25);
  --sidebar-danger: #e05555;
  --sidebar-card-bg: linear-gradient(135deg, #1a2235 0%, #151b2b 100%);
  
  position: fixed;
  left: 0;
  top: 0;
  width: clamp(220px, 22vw, 280px);
  height: 100vh;
  box-sizing: border-box;
  z-index: 100;
}

.sidebar {
  width: 100%;
  height: 100%;
  background: var(--sidebar-bg);
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  overflow: hidden;
  border-right: 1px solid var(--sidebar-border);
}

/* Logo 区域 */
.sidebar-header {
  padding: clamp(20px, 3vh, 28px) clamp(16px, 2vw, 24px);
  border-bottom: 1px solid var(--sidebar-border);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.logo-container {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--sidebar-primary) 0%, #2d5fa8 100%);
  border-radius: 12px;
  color: #ffffff;
  box-shadow: 0 4px 12px var(--sidebar-primary-glow);
}

.logo-icon svg {
  width: 22px;
  height: 22px;
}

.logo-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.logo {
  font-size: clamp(18px, 2.2vw, 22px);
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 0.5px;
  line-height: 1.2;
}

.logo-sub {
  font-size: clamp(11px, 1.3vw, 13px);
  color: var(--sidebar-text);
  font-weight: 400;
  letter-spacing: 0.3px;
}

/* 用户功能区域 */
.user-section {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  border-bottom: 1px solid var(--sidebar-border);
}

.user-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: var(--sidebar-bg-secondary);
  border-radius: 12px;
  border: 1px solid var(--sidebar-border);
  cursor: pointer;
  transition: all 0.2s ease;
}

.user-card:hover {
  background: var(--sidebar-bg-hover);
  border-color: rgba(58, 123, 200, 0.3);
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid var(--sidebar-primary);
  flex-shrink: 0;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-status {
  font-size: 12px;
  color: var(--sidebar-text);
  margin-top: 2px;
}

.user-arrow {
  width: 20px;
  height: 20px;
  color: var(--sidebar-text);
  flex-shrink: 0;
}

.user-arrow svg {
  width: 100%;
  height: 100%;
}

.custom-path-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 14px;
  background: var(--sidebar-bg-secondary);
  border: 1px dashed var(--sidebar-border);
  border-radius: 10px;
  color: var(--sidebar-text);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.custom-path-btn:hover {
  background: var(--sidebar-bg-hover);
  border-color: var(--sidebar-primary);
  color: var(--sidebar-primary);
}

.custom-path-btn svg {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

/* 导航区域 */
.sidebar-nav {
  flex: 1;
  padding: clamp(12px, 1.5vh, 16px) clamp(10px, 1.2vw, 14px);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  overflow-x: hidden;
}

.sidebar-nav::-webkit-scrollbar {
  width: 4px;
}

.sidebar-nav::-webkit-scrollbar-track {
  background: transparent;
}

.sidebar-nav::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
}

.sidebar-nav::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.2);
}

.nav-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-section-title {
  font-size: 11px;
  font-weight: 600;
  color: #4a5568;
  text-transform: uppercase;
  letter-spacing: 1px;
  padding: 8px 12px 6px;
  margin-bottom: 4px;
}

/* 导航项 */
.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 10px;
  text-decoration: none;
  color: var(--sidebar-text);
  font-size: clamp(13px, 1.5vw, 14px);
  font-weight: 500;
  cursor: pointer;
  border: none;
  background: transparent;
  width: 100%;
  text-align: left;
  outline: none;
}

.nav-item.active {
  background: var(--sidebar-bg-active);
  color: var(--sidebar-text-active);
}

.nav-item.active .nav-icon {
  color: var(--sidebar-primary);
}

.nav-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 24px;
  background: var(--sidebar-primary);
  border-radius: 0 3px 3px 0;
  box-shadow: 0 0 12px var(--sidebar-primary-glow);
}

.nav-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: inherit;
  transition: color 0.2s ease;
}

.nav-icon :deep(svg) {
  width: 100%;
  height: 100%;
}

.nav-text {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* .nav-indicator {
  width: 6px;
  height: 6px;
  background: var(--sidebar-primary);
  border-radius: 50%;
  box-shadow: 0 0 8px var(--sidebar-primary-glow);
} */

.nav-badge {
  font-size: 10px;
  font-weight: 600;
  color: var(--sidebar-primary);
  background: rgba(58, 123, 200, 0.15);
  padding: 2px 8px;
  border-radius: 10px;
  letter-spacing: 0.3px;
}

/* 底部区域 */
.nav-bottom {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--sidebar-border);
}

/* AI 职业导师卡片 */
.mentor-card {
  margin: 4px 0;
  padding: 16px;
  background: var(--sidebar-card-bg);
  border-radius: 14px;
  border: 1px solid var(--sidebar-border);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 10px;
  transition: all 0.3s ease;
}

.mentor-card:hover {
  border-color: rgba(58, 123, 200, 0.3);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
}

.mentor-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(58, 123, 200, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  border: 1px solid var(--sidebar-border);
  overflow: hidden;
}

.mentor-title {
  font-size: clamp(13px, 1.6vw, 15px);
  font-weight: 600;
  color: #ffffff;
}

.mentor-desc {
  font-size: clamp(10px, 1.2vw, 11px);
  color: var(--sidebar-text);
  line-height: 1.4;
}

.mentor-btn {
  width: 100%;
  padding: 10px;
  background: var(--sidebar-primary);
  color: #ffffff;
  font-size: clamp(11px, 1.3vw, 13px);
  font-weight: 500;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.mentor-btn:hover {
  background: #2d6bb3;
}

/* 管理员项 */
.nav-item-admin {
  margin-top: 4px;
  border-radius: 10px;
}

/* 退出登录 */
.nav-item-logout {
  margin-top: 4px;
}

.nav-item-logout:hover {
  background: rgba(224, 85, 85, 0.08);
  color: var(--sidebar-danger);
}

.nav-item-logout:hover .nav-icon {
  color: var(--sidebar-danger);
}

/* 响应式调整 */
@media (max-width: 1200px) {
  .sidebar-wrapper {
    width: clamp(200px, 20vw, 240px);
  }
  
  .nav-item {
    padding: 10px 12px;
    gap: 10px;
  }
  
  .nav-icon {
    width: 18px;
    height: 18px;
  }
}

@media (max-width: 900px) {
  .sidebar-wrapper {
    width: 72px;
  }

  .logo-text,
  .user-section,
  .nav-text,
  .nav-section-title,
  .nav-badge,
  .nav-indicator {
    display: none;
  }

  .logo-container {
    justify-content: center;
  }

  .nav-item {
    justify-content: center;
    padding: 12px;
  }

  .nav-item.active::before {
    width: 100%;
    height: 3px;
    top: auto;
    bottom: 0;
    left: 50%;
    transform: translateX(-50%);
    border-radius: 3px 3px 0 0;
  }

  .nav-bottom {
    padding-top: 8px;
  }

  .nav-item-admin,
  .nav-item-logout {
    justify-content: center;
  }

  .mentor-card {
    padding: 8px;
  }

  .mentor-avatar {
    display: none;
  }

  .mentor-desc {
    display: none;
  }

  .mentor-btn {
    padding: 8px;
    font-size: 0;
  }

  .mentor-btn::before {
    content: "→";
    font-size: 16px;
  }
}
/* Soft brutalist navigation treatment */
.sidebar-wrapper {
  --sidebar-bg: #171715;
  --sidebar-bg-secondary: #24231f;
  --sidebar-bg-hover: rgba(255, 255, 255, .08);
  --sidebar-bg-active: #f2e8d7;
  --sidebar-border: rgba(255, 255, 255, .14);
  --sidebar-text: #a7a49d;
  --sidebar-text-hover: #ffffff;
  --sidebar-text-active: #201f1c;
  --sidebar-primary: #d6c28a;
  --sidebar-primary-glow: transparent;
  --sidebar-card-bg: #24231f;
  top: 72px;
  left: 14px;
  width: clamp(220px, 20vw, 268px);
  height: calc(100vh - 86px);
}
.sidebar { border: 1px solid #0d0d0c; border-radius: 26px; }
.logo { font-size: 24px; letter-spacing: -.04em; }
.logo-sub { color: #aaa69d; letter-spacing: .1em; }
.user-card { border-radius: 16px; }
.user-avatar { border-width: 1px; border-color: #e9d8a7; }
.nav-item { border-radius: 999px; transition: color .18s ease, background .18s ease, transform .18s ease; }
.nav-item:hover { color: #fff; background: rgba(255,255,255,.07); transform: translateX(2px); }
.nav-item.active { color: var(--sidebar-text-active); background: var(--sidebar-bg-active); }
.nav-item.active .nav-icon { color: #201f1c; }
.nav-item.active::before { display: none; }
.mentor-card { border-radius: 18px; box-shadow: none; }
.mentor-card:hover { border-color: rgba(255,255,255,.28); box-shadow: none; }
.mentor-btn { border: 1px solid #f7f0e4; border-radius: 999px; color: #1d1c19; background: #f7f0e4; }
.mentor-btn:hover { color: #1d1c19; background: #ddcfae; }
@media (max-width: 900px) { .sidebar-wrapper { left: 8px; width: 68px; } }
</style>
