<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AppTabBar from '../components/AppTabBar.vue'
import {
  createComment,
  getCommentList,
  getHotTopics,
  getMyFavoritePosts,
  getMyForumPosts,
  getPostDetail,
  getPostList,
  getTopicList,
  parsePostImages,
  publishPost as publishForumPost,
  togglePostFavorite,
  togglePostLike,
} from '../api/forum'
import { getToken } from '../utils/auth'

const route = useRoute()
const router = useRouter()

const CATEGORIES = [
  { id: 'recommended', label: '推荐', keywords: [] },
  { id: 'experience', label: '经验分享', keywords: ['经验', '分享', '学习', '求职', '面试'] },
  { id: 'cases', label: '就业案例', keywords: ['就业', '案例', '上岸', '校招', 'offer'] },
  { id: 'referrals', label: '内推招聘', keywords: ['内推', '招聘', '实习', '岗位'] },
  { id: 'qa', label: '问答交流', keywords: ['问答', '提问', '求助', '讨论'] },
  { id: 'following', label: '我的关注', keywords: [] },
]
const SORTS = [
  { id: 'latest', label: '最新' },
  { id: 'hot', label: '热门' },
  { id: 'featured', label: '精华' },
]

const posts = ref([])
const topics = ref([])
const hotTopics = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const searchDraft = ref('')
const activeKeyword = ref('')
const loading = ref(true)
const loadError = ref('')
const feedScope = ref('category')

const detailOpen = ref(false)
const detailLoading = ref(false)
const selectedPost = ref(null)
const comments = ref([])
const commentDraft = ref('')
const commentSaving = ref(false)
const actionBusy = ref('')

const publishOpen = ref(false)
const publishSaving = ref(false)
const publishError = ref('')
const publishForm = reactive({ title: '', content: '', topicId: '' })

const category = computed(() => {
  const value = String(route.query.category || 'recommended')
  return CATEGORIES.some((item) => item.id === value) ? value : 'recommended'
})
const sort = computed(() => {
  const value = String(route.query.sort || 'latest')
  return SORTS.some((item) => item.id === value) ? value : 'latest'
})
const currentCategory = computed(() => CATEGORIES.find((item) => item.id === category.value) || CATEGORIES[0])
const selectedTopicId = computed(() => {
  const keywords = currentCategory.value.keywords
  if (!keywords.length) return null
  const matched = topics.value.find((topic) => keywords.some((keyword) => String(topic.topicName || '').includes(keyword)))
  return matched?.id || null
})
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

async function loadTopics() {
  const [allResult, hotResult] = await Promise.allSettled([getTopicList(), getHotTopics({ limit: 8 })])
  if (allResult.status === 'fulfilled') topics.value = normalizeList(allResult.value)
  if (hotResult.status === 'fulfilled') hotTopics.value = normalizeList(hotResult.value)
}

function normalizeList(value) {
  if (Array.isArray(value)) return value
  if (Array.isArray(value?.records)) return value.records
  if (Array.isArray(value?.data)) return value.data
  return []
}

function friendlyError(message) {
  const text = String(message || '')
  if (/failed to fetch|networkerror|load failed|connection/i.test(text)) return '网络连接失败，请确认服务已启动后重试'
  return text || '加载失败'
}

async function loadFeed() {
  loading.value = true
  loadError.value = ''
  try {
    if (!topics.value.length) await loadTopics()
    let pageData
    if (feedScope.value === 'myPosts') {
      pageData = await getMyForumPosts({ pageNum: pageNum.value, pageSize })
    } else if (category.value === 'following') {
      pageData = await getMyFavoritePosts({ pageNum: pageNum.value, pageSize })
    } else {
      if (category.value !== 'recommended' && !selectedTopicId.value) {
        posts.value = []
        total.value = 0
        return
      }
      pageData = await getPostList({
        pageNum: pageNum.value,
        pageSize: sort.value === 'featured' ? 50 : pageSize,
        topicId: selectedTopicId.value || undefined,
        keyword: activeKeyword.value || undefined,
        sortBy: sort.value === 'hot' ? 'likeCount' : undefined,
      })
    }
    let rows = normalizeList(pageData)
    if (sort.value === 'featured' && feedScope.value === 'category') {
      rows = rows.filter((item) => item.highlighted)
    }
    posts.value = rows
    total.value = Number(pageData?.total ?? rows.length)
  } catch (error) {
    posts.value = []
    total.value = 0
    loadError.value = friendlyError(error.message || '社区内容加载失败')
  } finally {
    loading.value = false
  }
}

function selectCategory(id) {
  feedScope.value = 'category'
  pageNum.value = 1
  router.replace({ path: '/community', query: { ...route.query, category: id, sort: sort.value } })
}

function selectSort(id) {
  pageNum.value = 1
  router.replace({ path: '/community', query: { ...route.query, category: category.value, sort: id } })
}

function submitSearch() {
  activeKeyword.value = searchDraft.value.trim()
  pageNum.value = 1
  loadFeed()
}

function clearSearch() {
  searchDraft.value = ''
  activeKeyword.value = ''
  pageNum.value = 1
  loadFeed()
}

function selectTopic(topic) {
  searchDraft.value = topic.topicName || ''
  activeKeyword.value = searchDraft.value
  feedScope.value = 'category'
  pageNum.value = 1
  router.replace({ path: '/community', query: { category: 'recommended', sort: 'latest' } }).then(loadFeed)
}

function changePage(nextPage) {
  if (nextPage < 1 || nextPage > pageCount.value) return
  pageNum.value = nextPage
  loadFeed()
}

function openMyPosts() {
  feedScope.value = 'myPosts'
  pageNum.value = 1
  selectedPost.value = null
  loadFeed()
}

function openMyFavorites() {
  feedScope.value = 'category'
  router.replace({ path: '/community', query: { category: 'following', sort: sort.value } })
}

function ensureLogin() {
  if (getToken()) return true
  router.push({ path: '/login', query: { redirect: route.fullPath } })
  return false
}

async function openPostDetail(post) {
  if (!post?.id) return
  detailOpen.value = true
  selectedPost.value = post
  comments.value = []
  detailLoading.value = true
  try {
    const [detail, commentPage] = await Promise.all([
      getPostDetail(post.id),
      getCommentList({ postId: post.id, pageNum: 1, pageSize: 50 }),
    ])
    selectedPost.value = detail || post
    comments.value = normalizeList(commentPage)
  } catch (error) {
    loadError.value = friendlyError(error.message || '帖子详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

function closePostDetail() {
  detailOpen.value = false
  selectedPost.value = null
  comments.value = []
  commentDraft.value = ''
}

async function switchLike(post) {
  if (!post?.id || !ensureLogin() || actionBusy.value) return
  actionBusy.value = `like-${post.id}`
  try {
    await togglePostLike(post.id)
    post.isLiked = !post.isLiked
    post.likeCount = Math.max(0, Number(post.likeCount || 0) + (post.isLiked ? 1 : -1))
  } catch (error) {
    loadError.value = friendlyError(error.message || '点赞失败')
  } finally {
    actionBusy.value = ''
  }
}

async function switchFavorite(post) {
  if (!post?.id || !ensureLogin() || actionBusy.value) return
  actionBusy.value = `favorite-${post.id}`
  try {
    await togglePostFavorite(post.id)
    post.isFavorited = !post.isFavorited
  } catch (error) {
    loadError.value = friendlyError(error.message || '收藏失败')
  } finally {
    actionBusy.value = ''
  }
}

async function submitComment() {
  if (!selectedPost.value?.id || !ensureLogin()) return
  if (!commentDraft.value.trim()) return
  commentSaving.value = true
  try {
    await createComment({ postId: selectedPost.value.id, content: commentDraft.value.trim() })
    commentDraft.value = ''
    const commentPage = await getCommentList({ postId: selectedPost.value.id, pageNum: 1, pageSize: 50 })
    comments.value = normalizeList(commentPage)
    selectedPost.value.commentCount = Number(selectedPost.value.commentCount || 0) + 1
  } catch (error) {
    loadError.value = friendlyError(error.message || '评论发布失败')
  } finally {
    commentSaving.value = false
  }
}

function openPublish() {
  if (!ensureLogin()) return
  publishForm.title = ''
  publishForm.content = ''
  publishForm.topicId = topics.value[0]?.id || ''
  publishError.value = ''
  publishOpen.value = true
}

function closePublish() {
  if (!publishSaving.value) publishOpen.value = false
}

async function submitPost() {
  if (!publishForm.title.trim() || !publishForm.content.trim()) {
    publishError.value = '标题和正文不能为空'
    return
  }
  publishSaving.value = true
  publishError.value = ''
  try {
    await publishForumPost({
      title: publishForm.title.trim(),
      content: publishForm.content.trim(),
      topicId: publishForm.topicId ? Number(publishForm.topicId) : null,
      images: [],
    })
    publishOpen.value = false
    pageNum.value = 1
    await loadFeed()
  } catch (error) {
    publishError.value = error.message || '发布失败'
  } finally {
    publishSaving.value = false
  }
}

function authorInitial(name) {
  return String(name || '校').slice(0, 1).toUpperCase()
}

function excerpt(value) {
  const text = String(value || '').replace(/\s+/g, ' ').trim()
  return text.length > 150 ? `${text.slice(0, 150)}…` : text
}

function formatTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

function postImages(post) {
  return parsePostImages(post?.images)
}

watch(
  () => [category.value, sort.value],
  () => {
    if (!loading.value) loadFeed()
  },
)

onMounted(loadFeed)
</script>

<template>
  <div class="community-page">
    <AppTabBar />
    <main class="community-shell">
      <header class="community-hero">
        <div class="hero-info">
          <h1>校友社区</h1>
          <p class="hero-line">经验分享、就业案例与问答交流都在这里，向学长学姐提问，也把你的经历分享给学弟学妹。</p>
        </div>
        <div class="hero-actions">
          <form class="community-search" @submit.prevent="submitSearch">
            <input v-model="searchDraft" type="search" placeholder="搜索帖子、话题或经验" />
            <button type="submit">搜索</button>
          </form>
          <button type="button" class="community-btn community-btn--primary" @click="openPublish">发布帖子</button>
        </div>
      </header>

      <nav class="community-tabs" aria-label="社区分类">
        <button
          v-for="item in CATEGORIES"
          :key="item.id"
          type="button"
          :class="{ active: feedScope === 'category' && category === item.id }"
          @click="selectCategory(item.id)"
        >
          {{ item.label }}
        </button>
      </nav>

      <section class="community-layout">
        <div class="feed-column">
          <div class="feed-toolbar">
            <div class="sort-tabs">
              <button v-for="item in SORTS" :key="item.id" type="button" :class="{ active: sort === item.id }" @click="selectSort(item.id)">{{ item.label }}</button>
            </div>
            <div class="feed-meta">
              <span v-if="activeKeyword" class="search-result">搜索“{{ activeKeyword }}” <button type="button" @click="clearSearch">清除</button></span>
              <span>{{ total > 0 ? `${total} 条内容` : '暂无内容' }}</span>
            </div>
          </div>

          <p v-if="loadError" class="community-error"><span>{{ loadError }}</span><button type="button" @click="loadFeed">重新加载</button></p>
          <div v-if="loading" class="community-state">正在加载社区内容…</div>
          <div v-else-if="!posts.length" class="community-empty">
            <strong>{{ activeKeyword ? '没有找到匹配的内容' : '暂无匹配内容' }}</strong>
            <p>{{ activeKeyword ? '换个关键词试试，或者发布一条新经验。' : (category === 'recommended' ? '社区还没有发布真实内容，第一条经验从这里开始。' : '当前分类还没有真实帖子，发布第一条经验吧。') }}</p>
            <button type="button" class="community-btn community-btn--primary" @click="openPublish">发布帖子</button>
            <div class="empty-links">
              <button type="button" @click="selectSort('hot')">看热门内容</button>
              <button type="button" @click="selectCategory('experience')">逛逛经验分享</button>
              <button type="button" @click="openMyFavorites">查看我的收藏</button>
            </div>
          </div>

          <div v-else class="post-list">
            <article v-for="post in posts" :key="post.id" class="post-card" tabindex="0" @click="openPostDetail(post)" @keydown.enter="openPostDetail(post)">
              <header>
                <span class="avatar">
                  <img v-if="post.avatar" :src="post.avatar" alt="" />
                  <i v-else>{{ authorInitial(post.username) }}</i>
                </span>
                <div class="author">
                  <strong>{{ post.username || '校园用户' }}</strong>
                  <span>{{ formatTime(post.createTime) }}</span>
                </div>
                <span v-if="post.pinOrder > 0" class="soft-tag">置顶</span>
                <span v-else-if="post.highlighted" class="soft-tag soft-tag--gold">精华</span>
                <span v-if="post.topicName" class="topic-tag">{{ post.topicName }}</span>
              </header>
              <h2>{{ post.title }}</h2>
              <p>{{ excerpt(post.content) }}</p>
              <img v-if="postImages(post)[0]" class="post-image" :src="postImages(post)[0]" alt="" />
              <footer>
                <span>{{ post.viewCount || 0 }} 浏览</span>
                <button type="button" :disabled="actionBusy === `like-${post.id}`" @click.stop="switchLike(post)">{{ post.isLiked ? '已赞' : '点赞' }} {{ post.likeCount || 0 }}</button>
                <button type="button" @click.stop="openPostDetail(post)">评论 {{ post.commentCount || 0 }}</button>
                <button type="button" :disabled="actionBusy === `favorite-${post.id}`" @click.stop="switchFavorite(post)">{{ post.isFavorited ? '已收藏' : '收藏' }}</button>
              </footer>
            </article>
          </div>

          <div v-if="posts.length && pageCount > 1" class="pagination">
            <button type="button" :disabled="pageNum <= 1" @click="changePage(pageNum - 1)">上一页</button>
            <span>{{ pageNum }} / {{ pageCount }}</span>
            <button type="button" :disabled="pageNum >= pageCount" @click="changePage(pageNum + 1)">下一页</button>
          </div>
        </div>

        <aside class="community-sidebar">
          <section v-if="hotTopics.length" class="sidebar-panel">
            <div class="sidebar-head"><h2>热门话题</h2><span>{{ hotTopics.length }}</span></div>
            <div class="hot-topics">
              <button v-for="(topic, index) in hotTopics" :key="topic.id" type="button" @click="selectTopic(topic)">
                <span><i class="rank">{{ String(index + 1).padStart(2, '0') }}</i>{{ topic.topicName }}</span>
                <em>{{ topic.postCount || 0 }} 帖</em>
              </button>
            </div>
          </section>
          <p v-else class="sidebar-empty sidebar-empty--bare">暂未生成热门话题，发布内容后自动汇总。</p>

          <section class="sidebar-panel">
            <div class="sidebar-head"><h2>社区规则</h2></div>
            <ul class="community-rules">
              <li>尊重原创，转载和引用请注明来源。</li>
              <li>分享真实经历，不伪造校友或企业身份。</li>
              <li>招聘和内推信息请明确岗位、地点和截止时间。</li>
            </ul>
          </section>

          <section class="sidebar-panel sidebar-panel--slim">
            <strong>我的内容</strong>
            <div class="my-content-links">
              <button type="button" @click="openMyPosts">我的帖子</button>
              <i>·</i>
              <button type="button" @click="openMyFavorites">我的收藏</button>
            </div>
          </section>
        </aside>
      </section>
    </main>

    <Teleport to="body">
      <div v-if="detailOpen" class="community-mask" @click.self="closePostDetail">
        <article class="post-detail">
          <header class="detail-head">
            <div>
              <span v-if="selectedPost?.topicName" class="topic-tag">{{ selectedPost.topicName }}</span>
              <h2>{{ selectedPost?.title }}</h2>
              <p>{{ selectedPost?.username || '校园用户' }} · {{ formatTime(selectedPost?.createTime) }}</p>
            </div>
            <button type="button" aria-label="关闭帖子详情" @click="closePostDetail">×</button>
          </header>

          <div v-if="detailLoading" class="community-state">正在加载帖子详情…</div>
          <template v-else>
            <div class="detail-body">
              <p>{{ selectedPost?.content || '暂无正文内容' }}</p>
              <img v-for="image in postImages(selectedPost)" :key="image" :src="image" alt="" />
            </div>

            <div class="detail-actions">
              <button type="button" @click="switchLike(selectedPost)">{{ selectedPost?.isLiked ? '已赞' : '点赞' }} {{ selectedPost?.likeCount || 0 }}</button>
              <button type="button" @click="switchFavorite(selectedPost)">{{ selectedPost?.isFavorited ? '已收藏' : '收藏' }}</button>
              <span>{{ selectedPost?.viewCount || 0 }} 浏览</span>
            </div>

            <section class="comment-section">
              <div class="sidebar-head"><h2>评论</h2><span>{{ comments.length }}</span></div>
              <form class="comment-editor" @submit.prevent="submitComment">
                <textarea v-model="commentDraft" rows="3" placeholder="写下你的想法或经验补充" />
                <button type="submit" class="community-btn community-btn--primary" :disabled="commentSaving || !commentDraft.trim()">{{ commentSaving ? '发布中…' : '发表评论' }}</button>
              </form>
              <div v-if="comments.length" class="comment-list">
                <article v-for="comment in comments" :key="comment.id">
                  <span class="avatar avatar--small"><img v-if="comment.avatar" :src="comment.avatar" alt="" /><i v-else>{{ authorInitial(comment.username) }}</i></span>
                  <div><strong>{{ comment.username || '校园用户' }}</strong><p>{{ comment.content }}</p><small>{{ formatTime(comment.createTime) }}</small></div>
                </article>
              </div>
              <p v-else class="sidebar-empty">暂无评论，欢迎补充经验。</p>
            </section>
          </template>
        </article>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="publishOpen" class="community-mask" @click.self="closePublish">
        <form class="publish-dialog" @submit.prevent="submitPost">
          <header class="detail-head">
            <div><h2>发布帖子</h2><p>分享真实经历、经验或问题。</p></div>
            <button type="button" aria-label="关闭发布窗口" @click="closePublish">×</button>
          </header>
          <label><span>标题</span><input v-model="publishForm.title" maxlength="200" type="text" placeholder="用一句话概括你的分享" /></label>
          <label><span>话题</span><select v-model="publishForm.topicId"><option value="">不选择话题</option><option v-for="topic in topics" :key="topic.id" :value="topic.id">{{ topic.topicName }}</option></select></label>
          <label><span>正文</span><textarea v-model="publishForm.content" rows="9" placeholder="分享你的学习、求职、面试或就业经历" /></label>
          <p v-if="publishError" class="community-error">{{ publishError }}</p>
          <footer><button type="button" class="community-btn" :disabled="publishSaving" @click="closePublish">取消</button><button type="submit" class="community-btn community-btn--primary" :disabled="publishSaving">{{ publishSaving ? '发布中…' : '确认发布' }}</button></footer>
        </form>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.community-page{min-height:100vh;color:var(--hp-ink);background:var(--hp-bg)}.community-shell{width:min(1280px,calc(100% - 40px));margin:0 auto;padding:96px 0 80px}.community-hero{display:flex;align-items:center;justify-content:space-between;gap:18px;padding:24px 28px;border:1px solid #e4ebf2;border-radius:var(--hp-r-lg);background:var(--hp-tint);box-shadow:var(--hp-shadow-sm);overflow:hidden;animation:cm-fade-up .5s cubic-bezier(.22,.61,.36,1) both}.hero-info{display:grid;gap:7px;min-width:0}.community-hero h1{margin:0;color:var(--hp-ink);font-size:25px;line-height:1.2;letter-spacing:-.02em}.hero-line{margin:0;color:var(--hp-ink-2);font-size:13.5px;line-height:1.6}.community-hero p{margin:0;color:var(--hp-ink-2);font-size:15px}.hero-actions{display:flex;flex-wrap:wrap;align-items:center;gap:12px}.community-search{display:flex;align-items:center;width:min(38vw,440px);padding:5px 5px 5px 16px;border:1px solid var(--hp-line-strong);border-radius:999px;background:var(--hp-surface);box-shadow:var(--hp-shadow-sm)}.community-search input{flex:1;min-width:0;height:40px;border:0;outline:0;color:var(--hp-ink);background:transparent;font:inherit}.community-search button{height:40px;padding:0 14px;border:0;border-radius:999px;color:var(--hp-blue-ink);background:transparent;font-weight:700;cursor:pointer;transition:background .2s ease}.community-search button:hover{background:var(--hp-surface-2)}.community-btn{min-height:46px;padding:0 24px;border:1px solid var(--hp-line-strong);border-radius:999px;color:var(--hp-ink);background:var(--hp-surface);font-size:14px;font-weight:600;cursor:pointer}.community-btn:hover{transform:translateY(-1px);border-color:var(--hp-ink)}.community-btn:disabled{opacity:.55;cursor:default;transform:none}.community-btn--primary{border-color:var(--hp-ink);color:#fff;background:var(--hp-ink);box-shadow:var(--hp-shadow-sm)}.community-tabs{position:sticky;top:60px;z-index:20;display:flex;gap:4px;margin:16px 0;padding:5px;overflow-x:auto;border:1px solid var(--hp-line);border-radius:999px;background:rgba(255,255,255,.96);box-shadow:var(--hp-shadow-sm);backdrop-filter:blur(6px);animation:cm-fade-up .5s cubic-bezier(.22,.61,.36,1) .08s both}.community-tabs button{flex:0 0 auto;min-height:40px;padding:0 18px;border:0;border-radius:999px;color:var(--hp-ink-2);background:transparent;font-size:14px;font-weight:600;cursor:pointer}.community-tabs button{transition:background .22s ease,color .22s ease}.community-tabs button:hover{color:var(--hp-ink);background:var(--hp-surface-2)}.community-tabs button.active{color:#fff;background:var(--hp-ink)}.community-layout{display:grid;grid-template-columns:minmax(0,1fr) 300px;gap:18px;align-items:start}.feed-column{min-width:0}.feed-toolbar{display:flex;align-items:center;justify-content:space-between;gap:14px;margin-bottom:12px;color:var(--hp-muted);font-size:12px}.feed-meta{display:flex;align-items:center;gap:12px}.sort-tabs{display:flex;gap:16px}.sort-tabs button{padding:4px 2px;border:0;border-bottom:2px solid transparent;border-radius:0;color:var(--hp-ink-2);background:transparent;font-size:13.5px;font-weight:600;cursor:pointer;transition:color .2s ease,border-color .2s ease}.sort-tabs button:hover{color:var(--hp-ink)}.sort-tabs button.active{color:var(--hp-ink);border-bottom-color:var(--hp-ink)}.search-result button{margin-left:6px;padding:0;border:0;color:var(--hp-blue-ink);background:transparent;font-weight:700;cursor:pointer}.community-state,.community-error,.community-empty{padding:18px;border:1px solid var(--hp-line);border-radius:var(--hp-r-md);color:var(--hp-ink-2);background:var(--hp-surface);box-shadow:var(--hp-shadow-sm)}.community-error{display:flex;align-items:center;justify-content:space-between;gap:12px;color:#8b4c49;background:#f8eeee}.community-error button{flex:none;padding:0;border:0;color:#8b4c49;background:transparent;font-size:12px;font-weight:700;text-decoration:underline;cursor:pointer}.community-empty{text-align:center;padding:34px 24px 30px}.community-empty strong{color:var(--hp-ink);font-size:15px}.community-empty p{margin:7px auto 16px;max-width:430px;font-size:13px;line-height:1.7}.empty-links{display:flex;flex-wrap:wrap;justify-content:center;gap:6px 18px;margin-top:13px}.empty-links button{padding:0;border:0;color:var(--hp-blue-ink);background:transparent;font-size:13px;font-weight:600;cursor:pointer}.empty-links button:hover{text-decoration:underline}.post-list{display:grid;gap:12px}.post-card{animation:cm-fade-up .45s cubic-bezier(.22,.61,.36,1) both}.post-list .post-card:nth-child(2){animation-delay:.05s}.post-list .post-card:nth-child(3){animation-delay:.1s}.post-list .post-card:nth-child(4){animation-delay:.15s}.post-list .post-card:nth-child(5){animation-delay:.2s}.post-list .post-card:nth-child(6){animation-delay:.25s}.post-card{padding:22px 24px;border:1px solid var(--hp-line);border-radius:var(--hp-r-lg);background:var(--hp-surface);box-shadow:var(--hp-shadow-sm);cursor:pointer;transition:transform .18s ease,border-color .18s ease,box-shadow .18s ease}.post-card:hover{transform:translateY(-2px);border-color:var(--hp-line-strong);box-shadow:var(--hp-shadow-md)}.post-card header{display:flex;align-items:center;gap:10px}.avatar{display:grid;width:38px;height:38px;flex:none;place-items:center;overflow:hidden;border-radius:50%;color:var(--hp-blue-ink);background:var(--hp-blue);font-style:normal;font-weight:700}.avatar img{width:100%;height:100%;object-fit:cover}.author{display:grid;gap:3px;flex:1;min-width:0}.author strong{color:var(--hp-ink);font-size:14px}.author span{color:var(--hp-muted);font-size:12px}.soft-tag,.topic-tag{display:inline-flex;padding:5px 9px;border-radius:999px;font-size:11px;font-weight:700;white-space:nowrap}.soft-tag{color:var(--hp-blue-ink);background:var(--hp-blue)}.soft-tag--gold{color:#806d35;background:#f5edc9}.topic-tag{color:var(--hp-ink-2);background:var(--hp-surface-2);border:1px solid var(--hp-line)}.post-card h2{margin:17px 0 9px;color:var(--hp-ink);font-size:20px;line-height:1.35;letter-spacing:-.02em}.post-card>p{margin:0;color:var(--hp-ink-2);font-size:14px;line-height:1.75}.post-image{display:block;width:100%;max-height:280px;margin-top:14px;border-radius:var(--hp-r-md);object-fit:cover}.post-card footer{display:flex;align-items:center;gap:18px;margin-top:17px;color:var(--hp-muted);font-size:12px}.post-card footer button{padding:0;border:0;color:var(--hp-ink-2);background:transparent;font-size:12px;font-weight:600;cursor:pointer}.pagination{display:flex;align-items:center;justify-content:center;gap:14px;margin-top:18px}.pagination button{min-height:36px;padding:0 14px;border:1px solid var(--hp-line-strong);border-radius:999px;color:var(--hp-ink);background:var(--hp-surface);cursor:pointer}.pagination button:disabled{opacity:.45;cursor:default}.community-sidebar{display:grid;gap:12px;position:sticky;top:128px}.sidebar-panel{padding:20px;border:1px solid var(--hp-line);border-radius:var(--hp-r-lg);background:var(--hp-surface);box-shadow:var(--hp-shadow-sm)}.sidebar-head{display:flex;align-items:center;justify-content:space-between;gap:12px}.sidebar-head h2{margin:0;font-size:16px}.sidebar-head span{color:var(--hp-muted);font-size:12px}.hot-topics{display:grid;gap:6px;margin-top:14px}.hot-topics button{display:flex;align-items:center;justify-content:space-between;gap:10px;padding:8px 0;border:0;color:var(--hp-ink-2);background:transparent;text-align:left;cursor:pointer}.hot-topics button:hover{color:var(--hp-blue-ink)}.hot-topics em{color:var(--hp-muted);font-size:11px;font-style:normal}.hot-topics .rank{margin-right:8px;color:var(--hp-blue-ink);font-size:11px;font-style:normal;font-weight:700;font-variant-numeric:tabular-nums}.community-rules{margin:14px 0 0;padding-left:18px;color:var(--hp-ink-2);font-size:13px;line-height:1.8}.sidebar-panel--slim{display:flex;align-items:center;justify-content:space-between;padding:14px 20px}.sidebar-panel--slim strong{font-size:14px}.my-content-links{display:flex;align-items:center;gap:8px}.my-content-links i{color:var(--hp-muted);font-style:normal}.my-content-links button{padding:0;border:0;color:var(--hp-blue-ink);background:transparent;font-size:13px;font-weight:600;cursor:pointer}.my-content-links button:hover{text-decoration:underline}.sidebar-empty{color:var(--hp-muted);font-size:13px;line-height:1.7}.sidebar-empty--bare{padding:2px 4px;font-size:12.5px}.community-mask{position:fixed;inset:0;z-index:2000;display:grid;place-items:center;padding:20px;background:rgba(27,35,41,.36)}.post-detail,.publish-dialog{width:min(760px,100%);max-height:calc(100vh - 40px);overflow:auto;padding:26px;border:1px solid var(--hp-line);border-radius:var(--hp-r-lg);background:var(--hp-surface);box-shadow:var(--hp-shadow-lg)}.detail-head{display:flex;align-items:flex-start;justify-content:space-between;gap:18px}.detail-head h2{margin:10px 0 7px;color:var(--hp-ink);font-size:26px;line-height:1.3}.detail-head p{margin:0;color:var(--hp-muted);font-size:13px}.detail-head>button{width:36px;height:36px;flex:none;border:0;border-radius:50%;color:var(--hp-ink-2);background:var(--hp-surface-2);font-size:22px;cursor:pointer}.detail-body{margin-top:22px;color:var(--hp-ink-2);font-size:15px;line-height:1.9;white-space:pre-wrap}.detail-body img{display:block;width:100%;margin-top:12px;border-radius:var(--hp-r-md);object-fit:cover}.detail-actions{display:flex;align-items:center;gap:10px;margin-top:20px;padding-top:16px;border-top:1px solid var(--hp-line)}.detail-actions button,.detail-actions span{padding:7px 11px;border:1px solid var(--hp-line);border-radius:999px;color:var(--hp-ink-2);background:var(--hp-surface);font-size:12px}.detail-actions button{cursor:pointer}.comment-section{margin-top:24px}.comment-editor{display:grid;gap:10px;margin-top:14px}.comment-editor textarea,.publish-dialog input,.publish-dialog select,.publish-dialog textarea{width:100%;box-sizing:border-box;padding:11px 12px;border:1px solid var(--hp-line-strong);border-radius:10px;outline:0;color:var(--hp-ink);background:var(--hp-surface-2);font:inherit}.comment-editor textarea:focus,.publish-dialog input:focus,.publish-dialog select:focus,.publish-dialog textarea:focus{border-color:var(--hp-blue-ink);background:var(--hp-surface)}.comment-editor button{justify-self:end}.comment-list{display:grid;gap:0;margin-top:12px}.comment-list article{display:grid;grid-template-columns:34px minmax(0,1fr);gap:10px;padding:14px 0;border-top:1px solid var(--hp-line)}.avatar--small{width:32px;height:32px}.comment-list strong{font-size:13px}.comment-list p{margin:4px 0;color:var(--hp-ink-2);font-size:13px;line-height:1.65}.comment-list small{color:var(--hp-muted);font-size:11px}.publish-dialog{display:grid;gap:15px}.publish-dialog label{display:grid;gap:7px;color:var(--hp-ink-2);font-size:12px;font-weight:600}.publish-dialog footer{display:flex;justify-content:flex-end;gap:10px}@keyframes cm-fade-up{from{opacity:0;transform:translateY(14px)}to{opacity:1;transform:none}}@keyframes cm-pop{from{opacity:0;transform:translateY(12px) scale(.97)}to{opacity:1;transform:none}}@keyframes cm-fade{from{opacity:0}to{opacity:1}}.community-mask{animation:cm-fade .2s ease both}.post-detail,.publish-dialog{animation:cm-pop .28s cubic-bezier(.22,.61,.36,1) both}@media(max-width:980px){.community-layout{grid-template-columns:1fr}.community-sidebar{position:static;grid-template-columns:repeat(3,minmax(0,1fr))}}@media(max-width:760px){.community-shell{width:min(100% - 24px,1280px);padding-top:82px}.community-hero{display:grid;gap:14px;padding:20px 20px}.hero-actions{display:grid}.community-search{width:100%}.community-tabs{top:52px}.community-sidebar{grid-template-columns:1fr}.post-card{padding:18px}.post-card footer{flex-wrap:wrap}.community-search button{padding:0 14px}}@media(prefers-reduced-motion:reduce){.community-page *,.community-page *::before,.community-page *::after{animation-duration:.01ms!important;animation-iteration-count:1!important;transition-duration:.01ms!important}}
</style>
