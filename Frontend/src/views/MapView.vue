<script setup>
/* ═══════════════════════════════════════════════════
   校园导航页 — 高德地图 JS API 2.0 + 纯前端扩展功能
   功能：高德地图/搜索/快捷按钮/聊天助手/暗色模式/
         打卡/留言板/随机漫步/失物招领
   ═══════════════════════════════════════════════════ */
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import '../assets/referenceDashboard.css'
import AppTabBar from '../components/AppTabBar.vue'
import {
  getFloorPlan,
  getFloorPlanPositions,
  getMapPlaceDetail,
  getMapPlaceList,
} from '../api/map'
import { getDishes } from '../api/campusServices'
import markerCanteen from '../assets/map/marker-canteen.svg'
import markerDormitory from '../assets/map/marker-dormitory.svg'
import markerOther from '../assets/map/marker-other.svg'
import markerSports from '../assets/map/marker-sports.svg'
import markerTeaching from '../assets/map/marker-teaching.svg'

/* ── 高德地图配置 ── */
const AMAP_KEY = 'e1790a70dfc91f2d3daf1895c0e9f87a'
const AMAP_SECURITY = 'f3eda2c1d4c4c76bdb907d570b69d8c'
const MAP_CENTER = [104.146867, 30.674820]
const MAP_ZOOM = 17

const mapPlaces = ref([])
const placeLoading = ref(false)

function fitCampusView() {
  if (!mapInstance) return
  const boundary = mapPlaces.value.find(isCampusBoundary)
  const boundaryOverlay = boundary ? fenceMap[boundary.id] : null
  if (boundaryOverlay) {
    mapInstance.setFitView([boundaryOverlay], false, [80, 80, 100, 80], 18)
    return
  }
  mapInstance.setZoomAndCenter(MAP_ZOOM, MAP_CENTER)
}

const SCENE_META = {
  CANTEEN: { label: '食堂', color: '#f97316', icon: markerCanteen },
  SPORTS: { label: '运动场', color: '#10b981', icon: markerSports },
  TEACHING: { label: '教学楼', color: '#3b82f6', icon: markerTeaching },
  DORMITORY: { label: '宿舍', color: '#8b5cf6', icon: markerDormitory },
}
const DEFAULT_SCENE_META = { label: '其他点位', color: '#64748b', icon: markerOther }
const sceneMeta = (sceneType) => SCENE_META[sceneType] || DEFAULT_SCENE_META
const isCampusBoundary = (place) => place?.placeType === 'CAMPUS_BOUNDARY'

const toCoordinate = (value) => {
  if (value === null || value === undefined || value === '') return null
  const coordinate = Number(value)
  return Number.isFinite(coordinate) ? coordinate : null
}

const parseFence = (fence) => {
  if (!fence?.geometryData) return null
  try {
    const geometry = typeof fence.geometryData === 'string'
      ? JSON.parse(fence.geometryData)
      : fence.geometryData
    const geometryType = String(fence.geometryType || geometry?.type || '').toUpperCase()
    const coordinates = geometryType === 'POLYGON'
      ? geometry?.coordinates?.[0]
      : geometry?.coordinates
    if (!Array.isArray(coordinates)) return null
    const path = coordinates
      .map(point => [toCoordinate(point?.[0]), toCoordinate(point?.[1])])
      .filter(point => point[0] !== null && point[1] !== null)
    if (geometryType === 'POLYGON' && path.length >= 3) return { geometryType, path }
    if (geometryType === 'LINESTRING' && path.length >= 2) return { geometryType, path }
    return null
  } catch {
    return null
  }
}

const toMapPoi = (place) => ({
  id: place.id,
  name: place.name || '未命名点位',
  lng: toCoordinate(place.longitude),
  lat: toCoordinate(place.latitude),
  desc: place.description || place.locationDesc || '暂无详细介绍',
  description: place.description || '',
  tag: place.placeType || sceneMeta(place.sceneType).label,
  sceneType: place.sceneType,
  placeType: place.placeType,
  locationDesc: place.locationDesc || '',
  status: place.status,
  images: Array.isArray(place.images) ? place.images : [],
  fence: parseFence(place.fence),
})

async function loadMapPlaces() {
  placeLoading.value = true
  try {
    const response = await getMapPlaceList({ status: 'ENABLED' })
    const places = (Array.isArray(response?.data) ? response.data : [])
      .filter(place => place.mapVisible !== false)
    const resolvedPlaces = places
      .map(toMapPoi)
      .filter(place => Number.isFinite(place.lng) && Number.isFinite(place.lat))
    mapPlaces.value = resolvedPlaces
  } catch (error) {
    mapError.value = error.message || '校园点位加载失败'
    mapPlaces.value = []
  } finally {
    placeLoading.value = false
  }
}

const activePoi = ref(null)          /* 当前选中点位 */
const activePoiScreen = ref({ x: 0, y: 0, panelX: 18, panelY: 18 })
const mapViewport = ref({ width: 0, height: 0 })
const selectedNotice = ref('')       /* 搜索选中提示 */
const categoryExpanded = ref(false)
const selectedCategories = ref(new Set(['canteen', 'dormitory', 'teaching', 'sports', 'other']))
const categoryItems = [
  { key: 'canteen', label: '食堂', icon: '餐', sceneType: 'CANTEEN' },
  { key: 'dormitory', label: '宿舍楼', icon: '宿', sceneType: 'DORMITORY' },
  { key: 'teaching', label: '教学楼', icon: '学', sceneType: 'TEACHING' },
  { key: 'sports', label: '运动场', icon: '场', sceneType: 'SPORTS' },
  { key: 'other', label: '其他', icon: '其', sceneType: 'OTHER' },
]
const filterCategoryKeys = categoryItems.map(item => item.key)
const allCategoriesSelected = computed(() =>
  filterCategoryKeys.every(key => selectedCategories.value.has(key)),
)
const isCategorySelected = key => selectedCategories.value.has(key)

function updateActivePoiScreen() {
  if (!mapInstance || !activePoi.value) return
  const pixel = mapInstance.lngLatToContainer([activePoi.value.lng, activePoi.value.lat])
  if (!pixel) return
  const size = mapInstance.getSize()
  const width = size?.getWidth?.() || size?.width || document.getElementById('container')?.clientWidth || window.innerWidth
  const height = size?.getHeight?.() || size?.height || document.getElementById('container')?.clientHeight || window.innerHeight
  const x = pixel.getX()
  const y = pixel.getY()
  const panelElement = document.querySelector('.canteen-panel')
  const cardWidth = Math.min(panelElement?.offsetWidth || 360, width - 36)
  const cardHeight = Math.min(panelElement?.offsetHeight || 430, height - 36)
  const gap = 96
  const margin = 18
  let panelX
  let panelY

  if (width - x - gap >= cardWidth + margin) {
    panelX = x + gap
    panelY = Math.max(margin, Math.min(y - cardHeight / 2, height - cardHeight - margin))
  } else if (x - gap >= cardWidth + margin) {
    panelX = x - gap - cardWidth
    panelY = Math.max(margin, Math.min(y - cardHeight / 2, height - cardHeight - margin))
  } else if (height - y - gap >= cardHeight + margin) {
    panelX = Math.max(margin, Math.min(x - cardWidth / 2, width - cardWidth - margin))
    panelY = y + gap
  } else if (y - gap >= cardHeight + margin) {
    panelX = Math.max(margin, Math.min(x - cardWidth / 2, width - cardWidth - margin))
    panelY = y - gap - cardHeight
  } else {
    panelX = Math.max(margin, Math.min(x - cardWidth / 2, width - cardWidth - margin))
    panelY = Math.max(margin, Math.min(y - cardHeight / 2, height - cardHeight - margin))
  }

  mapViewport.value = { width, height }
  activePoiScreen.value = { x, y, panelX, panelY }
}

function closeActivePoi() {
  activePoi.value = null
  syncFenceVisibility()
}

function applyCategoryFilters() {
  const visiblePlaces = mapPlaces.value.filter((place) => {
    if (isCampusBoundary(place)) return true
    const categoryKey = categoryItems.find(item => item.sceneType === place.sceneType)?.key
    if (!categoryKey || !selectedCategories.value.has(categoryKey)) return false
    return true
  })
  const visibleIds = new Set(visiblePlaces.map(place => String(place.id)))
  mapPlaces.value.forEach((place) => {
    const visible = visibleIds.has(String(place.id))
    markerMap[place.id]?.[visible ? 'show' : 'hide']()
  })
  if (activePoi.value && !visibleIds.has(String(activePoi.value.id))) closeActivePoi()
  fitCampusView()
}

function selectCategory(item) {
  const nextSelection = new Set(selectedCategories.value)
  if (allCategoriesSelected.value) nextSelection.clear()
  if (nextSelection.has(item.key)) nextSelection.delete(item.key)
  else nextSelection.add(item.key)
  selectedCategories.value = nextSelection
  applyCategoryFilters()
}

function selectAllCategories() {
  categoryExpanded.value = !categoryExpanded.value
  selectedCategories.value = new Set(filterCategoryKeys)
  applyCategoryFilters()
}

/* ═══════════════════════════════════════
   ② 搜索
   ═══════════════════════════════════════ */
const searchQuery = ref('')
const searchFocused = ref(false)
const normalizeSearchText = (value) => String(value || '')
  .trim()
  .toLowerCase()
  .replace(/\s+/g, '')
  .replace(/餐厅|饭堂/g, '食堂')
  .replace(/宿舍楼|寝室|公寓/g, '宿舍')
const filteredPois = computed(() => {
  const q = normalizeSearchText(searchQuery.value)
  if (!q) return []
  return mapPlaces.value.filter((poi) => {
    const searchText = normalizeSearchText([
      poi.name,
      poi.desc,
      poi.locationDesc,
      sceneMeta(poi.sceneType).label,
    ].join(' '))
    return searchText.includes(q)
  })
})
function selectSearchResult(poi) {
  searchQuery.value = poi.name
  searchFocused.value = false
  selectedNotice.value = `已定位到「${poi.name}」— ${poi.desc}`
  flyToPoi(poi) /* 地图自动移动到目标地点 */
  setTimeout(() => { selectedNotice.value = '' }, 4000)
}
function handleLocationSearch() {
  const poi = filteredPois.value[0]
  searchFocused.value = false
  if (!poi) {
    selectedNotice.value = searchQuery.value.trim()
      ? `未找到「${searchQuery.value.trim()}」，请尝试搜索食堂、餐厅、宿舍或教学楼。`
      : '请输入要搜索的校园地点。'
    setTimeout(() => { selectedNotice.value = '' }, 4000)
    return
  }
  selectSearchResult(poi)
}
function clearSearch() {
  searchQuery.value = ''
  selectedNotice.value = ''
}

/* ═══════════════════════════════════════
   ⑩ 高德地图加载与初始化
   ═══════════════════════════════════════ */
const mapReady = ref(false)
const mapError = ref('')
let mapInstance = null        /* AMap.Map 实例 */
const markerMap = {}          /* poi.id → AMap.Marker 映射 */
const fenceMap = {}           /* poi.id → 围栏覆盖物映射 */
const mapOverlays = []
let infoWindow = null         /* 全局信息窗 */

function syncFenceVisibility(activeId = null) {
  const normalizedActiveId = activeId == null ? null : String(activeId)
  Object.entries(fenceMap).forEach(([id, overlay]) => {
    overlay[String(id) === normalizedActiveId ? 'show' : 'hide']()
  })
}

const indoorOpen = ref(false)
const indoorLoading = ref(false)
const indoorCanteen = ref(null)
const indoorFloors = ref([])
const indoorFloorId = ref(null)
const indoorFloorPlan = ref(null)
const indoorStalls = ref([])
const indoorDishes = ref([])
const indoorPositions = ref([])
const indoorZoom = ref(1.12)
const indoorSearch = ref('')
const selectedIndoorStallId = ref(null)
const introPreview = ref(null)
const indoorHistory = ref([])
const indoorView = ref('map')
const indoorPlanViewportRef = ref(null)
const indoorDragging = ref(false)
let indoorDragStartX = 0
let indoorDragStartY = 0
let indoorDragScrollLeft = 0
let indoorDragScrollTop = 0
let indoorDragViewport = null

const indoorCategoryLabels = {
  noodle: '面食类', soup: '汤羹类', rice: '炒饭盖饭', local: '地方小吃', drink: '饮品甜点', light: '轻食类',
}
const stallStatusLabels = { 1: '营业中', 2: '休息中', 3: '已关闭' }
const indoorCategoryKeys = Object.keys(indoorCategoryLabels)
const selectedIndoorCategories = ref(new Set(indoorCategoryKeys))
const allIndoorCategoriesSelected = computed(() =>
  indoorCategoryKeys.every(key => selectedIndoorCategories.value.has(key)),
)
const isIndoorCategorySelected = category => selectedIndoorCategories.value.has(category)

const categoryKeyByLabel = Object.fromEntries(
  Object.entries(indoorCategoryLabels).map(([key, label]) => [label, key]),
)

function resolveIndoorCategory(value) {
  const normalized = String(value || '').trim()
  return indoorCategoryLabels[normalized] ? normalized : categoryKeyByLabel[normalized] || 'local'
}

const apiIndoorMenu = computed(() => indoorDishes.value
  .map(dish => ({
    ...dish,
    stall: dish.stallName || '未命名档口',
    displayStallId: dish.stallPlaceId,
    category: resolveIndoorCategory(dish.category),
    image: dish.imageUrl,
  }))
  .filter(dish => indoorStalls.value.some(stall => String(stall.id) === String(dish.displayStallId))))

const demoFacilities = []

const indoorStallColors = ['#ff7a35', '#f97316', '#eab308', '#e11d48', '#f59e0b', '#0ea5e9', '#ef4444', '#8b5cf6', '#f59e0b', '#06b6d4', '#14b8a6', '#dc2626', '#22c55e', '#ec4899', '#0ea5e9', '#22c55e']
const resolvedIndoorStalls = computed(() => indoorStalls.value.map((stall, index) => {
  const position = indoorPositionMap.value.get(String(stall.id))
  const dishCategories = apiIndoorMenu.value
    .filter(dish => String(dish.displayStallId) === String(stall.id))
    .map(dish => dish.category)
  return {
    ...stall,
    category: dishCategories[0] || resolveIndoorCategory(stall.description),
    x: Number(position?.xRatio),
    y: Number(position?.yRatio),
    color: indoorStallColors[index % indoorStallColors.length],
  }
}).filter(stall => Number.isFinite(stall.x) && Number.isFinite(stall.y)))

const filteredDemoStalls = computed(() => {
  const keyword = indoorSearch.value.trim().toLowerCase()
  return resolvedIndoorStalls.value.filter(stall =>
    selectedIndoorCategories.value.has(stall.category)
    && (!selectedIndoorStallId.value || String(stall.id) === String(selectedIndoorStallId.value))
    && (!keyword || stall.name.toLowerCase().includes(keyword)))
})

const filteredDemoFacilities = computed(() => demoFacilities.filter(item => selectedIndoorCategories.value.has(item.category)))
const filteredDemoMenu = computed(() => {
  const keyword = indoorSearch.value.trim().toLowerCase()
  return apiIndoorMenu.value.filter(item =>
    selectedIndoorCategories.value.has(item.category)
    && (!selectedIndoorStallId.value || String(item.displayStallId) === String(selectedIndoorStallId.value))
    && (!keyword || `${item.name}${item.stall}`.toLowerCase().includes(keyword)))
})

function selectIndoorCategory(category) {
  if (category === 'all') {
    selectedIndoorCategories.value = new Set(indoorCategoryKeys)
    return
  }
  const nextSelection = new Set(selectedIndoorCategories.value)
  if (allIndoorCategoriesSelected.value) nextSelection.clear()
  if (nextSelection.has(category)) nextSelection.delete(category)
  else nextSelection.add(category)
  selectedIndoorCategories.value = nextSelection
}

function toggleIntroPreview(event, title, text) {
  const content = event.currentTarget.querySelector('span')
  if (!text || !content || content.scrollWidth <= content.clientWidth) return
  if (introPreview.value?.text === text && introPreview.value?.title === title) {
    introPreview.value = null
    return
  }
  const triggerRect = event.currentTarget.getBoundingClientRect()
  const dialog = event.currentTarget.closest('.indoor-guide-dialog')
  const dialogRect = dialog?.getBoundingClientRect()
  if (!dialogRect) return
  const previewWidth = 340
  const previewHeight = 220
  const gap = 10
  const margin = 16
  const rightSideLeft = triggerRect.right - dialogRect.left + gap
  const hasRightSpace = rightSideLeft + previewWidth <= dialogRect.width - margin
  const left = hasRightSpace
    ? rightSideLeft
    : Math.max(margin, triggerRect.left - dialogRect.left - previewWidth - gap)
  const top = Math.max(margin, Math.min(
    triggerRect.top - dialogRect.top - 24,
    dialogRect.height - previewHeight - margin,
  ))
  introPreview.value = { title, text, style: { left: `${left}px`, top: `${top}px` } }
}

function indoorSnapshot() {
  const viewport = indoorPlanViewportRef.value
  return {
    view: indoorView.value,
    search: indoorSearch.value,
    categories: [...selectedIndoorCategories.value],
    stallId: selectedIndoorStallId.value,
    zoom: indoorZoom.value,
    scrollLeft: viewport?.scrollLeft || 0,
    scrollTop: viewport?.scrollTop || 0,
  }
}

function pushIndoorHistory() {
  indoorHistory.value.push(indoorSnapshot())
}

function openIndoorView(view) {
  if (indoorView.value === view) return
  pushIndoorHistory()
  indoorView.value = view
}

async function focusIndoorStall(stall) {
  await nextTick()
  const viewport = indoorPlanViewportRef.value
  const canvas = viewport?.querySelector('.indoor-plan-canvas')
  if (!viewport || !canvas) return
  const targetLeft = canvas.scrollWidth * stall.x / 100 - viewport.clientWidth / 2
  const targetTop = canvas.scrollHeight * stall.y / 100 - viewport.clientHeight / 2
  viewport.scrollTo({
    left: Math.max(0, targetLeft),
    top: Math.max(0, targetTop),
    behavior: 'smooth',
  })
}

async function showStallOnMap(stall) {
  pushIndoorHistory()
  selectedIndoorStallId.value = null
  selectedIndoorCategories.value = new Set([stall.category])
  indoorSearch.value = stall.name
  indoorZoom.value = 1.7
  indoorView.value = 'map'
  await focusIndoorStall(stall)
}

function openStallDetail(stall) {
  pushIndoorHistory()
  selectedIndoorStallId.value = stall.id
  indoorSearch.value = ''
  selectedIndoorCategories.value = new Set(indoorCategoryKeys)
  indoorView.value = 'list'
}

function showStallMenu(stall) {
  pushIndoorHistory()
  selectedIndoorStallId.value = stall.id
  const stallCategories = apiIndoorMenu.value
    .filter(dish => String(dish.displayStallId) === String(stall.id))
    .map(dish => dish.category)
  selectedIndoorCategories.value = new Set(stallCategories.length ? stallCategories : [stall.category])
  indoorSearch.value = ''
  indoorView.value = 'menu'
}

function openIndoorList() {
  if (indoorView.value !== 'list') pushIndoorHistory()
  selectedIndoorStallId.value = null
  indoorView.value = 'list'
}

async function goBackIndoor() {
  const previous = indoorHistory.value.pop()
  if (!previous) {
    indoorOpen.value = false
    return
  }
  indoorView.value = previous.view
  indoorSearch.value = previous.search
  selectedIndoorCategories.value = new Set(previous.categories)
  selectedIndoorStallId.value = previous.stallId
  indoorZoom.value = previous.zoom
  await nextTick()
  if (previous.view === 'map' && indoorPlanViewportRef.value) {
    indoorPlanViewportRef.value.scrollTo({
      left: previous.scrollLeft,
      top: previous.scrollTop,
      behavior: 'auto',
    })
  }
}

async function openFloorDetail(floor) {
  indoorOpen.value = true
  indoorCanteen.value = activePoi.value
  indoorFloorId.value = floor.id
  indoorZoom.value = 1.12
  indoorSearch.value = ''
  selectedIndoorCategories.value = new Set(indoorCategoryKeys)
  selectedIndoorStallId.value = null
  indoorHistory.value = []
  indoorView.value = 'map'
  await selectIndoorFloor(floor.id)
}

function adjustIndoorZoom(delta) {
  indoorZoom.value = Math.max(1.08, Math.min(1.8, Number((indoorZoom.value + delta).toFixed(2))))
}

function onIndoorWheel(event) {
  adjustIndoorZoom(event.deltaY < 0 ? 0.1 : -0.1)
}

function startIndoorDrag(event) {
  if (event.button !== 0) return
  const viewport = event.currentTarget
  indoorDragging.value = true
  indoorDragViewport = viewport
  indoorDragStartX = event.clientX
  indoorDragStartY = event.clientY
  indoorDragScrollLeft = viewport.scrollLeft
  indoorDragScrollTop = viewport.scrollTop
  window.addEventListener('mousemove', moveIndoorDrag)
  window.addEventListener('mouseup', stopIndoorDrag, { once: true })
  event.preventDefault()
}

function moveIndoorDrag(event) {
  if (!indoorDragging.value || !indoorDragViewport) return
  const viewport = indoorDragViewport
  viewport.scrollLeft = indoorDragScrollLeft - (event.clientX - indoorDragStartX)
  viewport.scrollTop = indoorDragScrollTop - (event.clientY - indoorDragStartY)
  event.preventDefault()
}

function stopIndoorDrag() {
  indoorDragging.value = false
  indoorDragViewport = null
  window.removeEventListener('mousemove', moveIndoorDrag)
}

const indoorPositionMap = computed(
  () => new Map(indoorPositions.value.map(position => [String(position.placeId), position])),
)
const selectedIndoorFloor = computed(() =>
  indoorFloors.value.find(floor => String(floor.id) === String(indoorFloorId.value))
  || null,
)
const indoorClusters = computed(() => {
  const groups = new Map()
  resolvedIndoorStalls.value.forEach((stall) => {
    const column = Math.floor(stall.x / 25)
    const row = Math.floor(stall.y / 25)
    const key = `${column}-${row}`
    const group = groups.get(key) || { id: key, count: 0, xTotal: 0, yTotal: 0, color: stall.color }
    group.count += 1
    group.xTotal += stall.x
    group.yTotal += stall.y
    groups.set(key, group)
  })
  return [...groups.values()].map(group => ({
    id: group.id,
    count: group.count,
    x: group.xTotal / group.count,
    y: group.yTotal / group.count,
    color: group.color,
  }))
})

async function loadIndoorDishes() {
  try {
    const dishes = await getDishes({ floorPlaceId: indoorFloorId.value })
    indoorDishes.value = (Array.isArray(dishes) ? dishes : [])
      .filter(dish => dish.isAvailable !== false)
  } catch (error) {
    indoorDishes.value = []
    mapError.value = error.message || '菜品信息加载失败'
  }
}

async function selectIndoorFloor(floorId) {
  indoorFloorId.value = floorId
  introPreview.value = null
  indoorFloorPlan.value = null
  indoorStalls.value = []
  indoorDishes.value = []
  indoorPositions.value = []
  if (!floorId) return

  indoorLoading.value = true
  try {
    const [planResponse, stallResponse] = await Promise.all([
      getFloorPlan(floorId),
      getMapPlaceList({ sceneType: 'CANTEEN', parentId: floorId, status: 'ENABLED' }),
    ])
    const plan = planResponse?.data || null
    indoorFloorPlan.value = plan
    indoorStalls.value = (Array.isArray(stallResponse?.data) ? stallResponse.data : [])
      .filter(place => place.placeType === 'CANTEEN_STALL')
      .sort((left, right) => (left.sortOrder || 0) - (right.sortOrder || 0))
    await loadIndoorDishes()
    if (plan) {
      const positionResponse = await getFloorPlanPositions(plan.id)
      indoorPositions.value = Array.isArray(positionResponse?.data) ? positionResponse.data : []
    }
  } catch (error) {
    mapError.value = error.message || '楼层档口信息加载失败'
  } finally {
    indoorLoading.value = false
  }
}

async function openIndoorGuide(poi) {
  indoorOpen.value = true
  indoorHistory.value = []
  indoorCanteen.value = poi
  indoorFloors.value = []
  indoorFloorId.value = null
  indoorFloorPlan.value = null
  indoorStalls.value = []
  indoorDishes.value = []
  indoorPositions.value = []
  indoorLoading.value = true
  try {
    const response = await getMapPlaceList({
      sceneType: 'CANTEEN',
      parentId: poi.id,
      status: 'ENABLED',
    })
    indoorFloors.value = (Array.isArray(response?.data) ? response.data : [])
      .filter(place => place.placeType === 'FLOOR')
      .sort((left, right) => (left.sortOrder || 0) - (right.sortOrder || 0))
    if (indoorFloors.value.length) {
      await selectIndoorFloor(indoorFloors.value[0].id)
    }
  } catch (error) {
    mapError.value = error.message || '食堂楼层加载失败'
  } finally {
    indoorLoading.value = false
  }
}

function closeIndoorGuide() {
  indoorOpen.value = false
  introPreview.value = null
}

function createInfoWindowContent(poi) {
  const root = document.createElement('div')
  root.className = 'map-info-window'

  const heading = document.createElement('div')
  heading.className = 'map-info-window__heading'
  const pin = document.createElement('img')
  pin.className = 'map-info-window__pin'
  pin.src = sceneMeta(poi.sceneType).icon
  pin.alt = ''
  const name = document.createElement('strong')
  name.textContent = poi.name
  heading.append(pin, name)

  const type = document.createElement('span')
  type.className = 'map-info-window__type'
  type.textContent = poi.tag
  const description = document.createElement('p')
  description.textContent = poi.desc

  root.append(heading, type, description)
  if (poi.sceneType === 'CANTEEN') {
    const indoorButton = document.createElement('button')
    indoorButton.type = 'button'
    indoorButton.className = 'map-info-window__indoor-button'
    indoorButton.textContent = '查看楼层与档口'
    indoorButton.addEventListener('click', () => openIndoorGuide(poi))
    root.append(indoorButton)
  }
  return root
}

async function selectPoi(poi, marker) {
  try {
    const response = await getMapPlaceDetail(poi.id)
    const detail = response?.data ? toMapPoi(response.data) : poi
    activePoi.value = detail
    syncFenceVisibility(detail.id)
    /* 使用页面右侧的统一详情卡，避免同时出现高德默认信息窗。 */
    infoWindow?.close()
    await nextTick()
    updateActivePoiScreen()
  } catch (error) {
    mapError.value = error.message || '点位详情加载失败'
  }
}

async function togglePoi(poi, marker) {
  if (String(activePoi.value?.id) === String(poi.id)) {
    closeActivePoi()
    return
  }
  await selectPoi(poi, marker)
}

/* 动态加载高德 JS API 脚本 */
function loadAMapScript() {
  return new Promise((resolve, reject) => {
    /* 安全配置必须在脚本加载前设置 */
    window._AMapSecurityConfig = { securityJsCode: AMAP_SECURITY }
    if (window.AMap) { resolve(); return }
    const s = document.createElement('script')
    s.src = `https://webapi.amap.com/maps?v=2.0&key=${AMAP_KEY}`
    s.onload = resolve
    s.onerror = () => reject(new Error('高德地图脚本加载失败'))
    document.head.appendChild(s)
  })
}

/* 初始化地图、添加标记点、绑定事件 */
async function initMap() {
  try {
    await loadAMapScript()
    const AMap = window.AMap
    if (!AMap) throw new Error('AMap 未定义')

    const campusBoundary = mapPlaces.value.find(isCampusBoundary)
    mapInstance = new AMap.Map('container', {
      zoom: MAP_ZOOM,
      center: campusBoundary ? [campusBoundary.lng, campusBoundary.lat] : MAP_CENTER,
      viewMode: '2D',
      dragEnable: false,
      rotateEnable: false,
      pitchEnable: false,
      resizeEnable: true,
    })
    mapInstance.on('mapmove', updateActivePoiScreen)
    mapInstance.on('zoomchange', updateActivePoiScreen)

    /* 全局信息窗实例 */
    infoWindow = new AMap.InfoWindow({
      offset: new AMap.Pixel(0, -43),
      closeWhenClickMap: true,
    })

    /* 为接口返回的真实点位添加标记 */
    mapPlaces.value.forEach(poi => {
      const boundary = isCampusBoundary(poi)
      const meta = boundary
        ? { color: '#2563eb', icon: markerOther }
        : sceneMeta(poi.sceneType)
      let fenceOverlay = null
      if (poi.fence?.geometryType === 'POLYGON') {
        fenceOverlay = new AMap.Polygon({
          path: poi.fence.path,
          strokeColor: meta.color,
          strokeWeight: boundary ? 4 : 3,
          strokeOpacity: boundary ? 0.9 : 0.95,
          strokeStyle: boundary ? 'dashed' : 'solid',
          fillColor: meta.color,
          fillOpacity: boundary ? 0.06 : 0.2,
          zIndex: boundary ? 20 : 80,
          bubble: false,
        })
      } else if (poi.fence?.geometryType === 'LINESTRING') {
        fenceOverlay = new AMap.Polyline({
          path: poi.fence.path,
          strokeColor: meta.color,
          strokeWeight: 4,
          strokeOpacity: 0.95,
          zIndex: 80,
          bubble: false,
        })
      }
      if (fenceOverlay) {
        mapInstance.add(fenceOverlay)
        fenceOverlay.hide()
        mapOverlays.push(fenceOverlay)
        fenceMap[poi.id] = fenceOverlay
      }

      if (boundary) return

      const markerContent = document.createElement('div')
      markerContent.className = 'real-map-marker'
      const markerTitle = document.createElement('span')
      markerTitle.className = 'real-map-marker__title'
      markerTitle.textContent = poi.name
      const markerIcon = document.createElement('img')
      markerIcon.className = 'real-map-marker__icon'
      markerIcon.src = meta.icon
      markerIcon.alt = ''
      markerContent.append(markerTitle, markerIcon)
      const marker = new AMap.Marker({
        position: [poi.lng, poi.lat],
        title: poi.name,
        content: markerContent,
        offset: new AMap.Pixel(-70, -64),
      })

      marker.on('click', (event) => {
        event?.originEvent?.stopPropagation?.()
        togglePoi(poi, marker)
      })
      fenceOverlay?.on('click', () => {
        mapInstance.setZoomAndCenter(Math.max(mapInstance.getZoom() || MAP_ZOOM, MAP_ZOOM), [poi.lng, poi.lat])
        selectPoi(poi, marker)
      })

      markerMap[poi.id] = marker
      mapInstance.add(marker)
    })

    mapReady.value = true
    fitCampusView()
  } catch (err) {
    mapError.value = err.message || '地图初始化失败'
    console.error('[MapInit]', err)
  }
}

/* 地图飞行到指定点位并打开信息窗 */
async function flyToPoi(poi) {
  if (!mapInstance) return
  mapInstance.setZoomAndCenter(17, [poi.lng, poi.lat], false, 500)
  const marker = markerMap[poi.id]
  await selectPoi(poi, marker)
}

/* ═══════════════════════════════════════
   ③ 快捷按钮 / 导航
   ═══════════════════════════════════════ */
function flyTo(poi) {
  if (poi.sceneType !== 'CANTEEN') return
  selectedNotice.value = `正在查看「${poi.name}」`
  flyToPoi(poi)
  setTimeout(() => { selectedNotice.value = '' }, 3000)
}

/* ═══════════════════════════════════════
   ④ 校园打卡系统
   ═══════════════════════════════════════ */
const checkins = reactive(JSON.parse(localStorage.getItem('campus-checkins') || '{}'))
function doCheckin(poi) {
  if (checkins[poi.id]) return
  checkins[poi.id] = { time: Date.now() }
  localStorage.setItem('campus-checkins', JSON.stringify({ ...checkins }))
}

/* ═══════════════════════════════════════
   ⑥ 点位留言板
   ═══════════════════════════════════════ */
const boardData = reactive(JSON.parse(localStorage.getItem('campus-board') || '{}'))
const boardInput = ref('')
const boardAuthor = ref('')
function boardMessages(poiId) { return boardData[poiId] || [] }
function postBoard(poi) {
  const t = boardInput.value.trim(); if (!t) return
  if (!boardData[poi.id]) boardData[poi.id] = []
  boardData[poi.id].push({ text: t, author: boardAuthor.value.trim() || '匿名', time: Date.now() })
  localStorage.setItem('campus-board', JSON.stringify({ ...boardData }))
  boardInput.value = ''
}
function fmtTime(ts) {
  const d = new Date(ts)
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

/* ═══════════════════════════════════════
   ⑦ 失物招领
   ═══════════════════════════════════════ */
const lostItems = reactive(JSON.parse(localStorage.getItem('campus-lost') || '[]'))
const lostForm = reactive({ title: '', desc: '', type: 'lost', contact: '' })
const showLostForm = ref(false)
function postLost() {
  if (!lostForm.title.trim()) return
  lostItems.unshift({ ...lostForm, time: Date.now() })
  localStorage.setItem('campus-lost', JSON.stringify([...lostItems]))
  Object.assign(lostForm, { title: '', desc: '', type: 'lost', contact: '' })
  showLostForm.value = false
}

/* ═══════════════════════════════════════
   ⑧ 随机漫步
   ═══════════════════════════════════════ */
const randomPoi = ref(null)
function randomWalk() {
  if (!mapPlaces.value.length) return
  const pick = mapPlaces.value[Math.floor(Math.random() * mapPlaces.value.length)]
  randomPoi.value = pick
  flyToPoi(pick) /* 随机漫步也移动地图 */
}

/* ═══════════════════════════════════════
   全局事件
   ═══════════════════════════════════════ */
function onDocClick(e) {
  if (!e.target.closest('.search-wrapper')) searchFocused.value = false
  if (!e.target.closest('.random-modal') && !e.target.closest('.random-btn')) randomPoi.value = null
}

function onMapContainerClick(e) {
  if (e.target.closest('.amap-marker')) return
  closeActivePoi()
}

function setMapDragEnabled(enabled) {
  mapInstance?.setStatus({ dragEnable: enabled })
}

function onMapPointerDown(event) {
  if (event.button === 0) setMapDragEnabled(true)
}

function onMapPointerMove(event) {
  if (event.buttons === 0) setMapDragEnabled(false)
}

function stopMapDrag() {
  setMapDragEnabled(false)
}

onMounted(async () => {
  document.addEventListener('click', onDocClick)
  const mapContainer = document.getElementById('container')
  mapContainer?.addEventListener('click', onMapContainerClick)
  mapContainer?.addEventListener('pointerdown', onMapPointerDown, true)
  mapContainer?.addEventListener('pointermove', onMapPointerMove, true)
  window.addEventListener('pointerup', stopMapDrag)
  window.addEventListener('pointercancel', stopMapDrag)
  await loadMapPlaces()
  await nextTick()
  initMap()
})
onUnmounted(() => {
  document.removeEventListener('click', onDocClick)
  const mapContainer = document.getElementById('container')
  mapContainer?.removeEventListener('click', onMapContainerClick)
  mapContainer?.removeEventListener('pointerdown', onMapPointerDown, true)
  mapContainer?.removeEventListener('pointermove', onMapPointerMove, true)
  window.removeEventListener('pointerup', stopMapDrag)
  window.removeEventListener('pointercancel', stopMapDrag)
  stopIndoorDrag()
  mapOverlays.splice(0, mapOverlays.length)
  if (mapInstance) { mapInstance.destroy(); mapInstance = null }
})
</script>

<template>
  <div class="map-page reference-theme map-reference">
    <AppTabBar />

    <!-- ═══ 主区域 ═══ -->
    <div class="map-main">

      <!-- 高德地图容器（id="container" 固定） -->
      <div id="container" class="map-canvas">
        <!-- 加载中状态 -->
        <div v-if="!mapReady && !mapError" class="map-loading">
          <div class="spinner"></div>
          <span>地图加载中...</span>
        </div>
        <!-- 加载失败回退 -->
        <div v-if="mapError" class="map-fallback">
          <span>{{ mapError }}</span>
          <small>请检查高德地图 Key 配置和网络连接</small>
        </div>
      </div>

      <!-- ② 搜索栏 -->
      <div class="search-wrapper">
        <div class="search-box">
          <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
          </svg>
          <input v-model="searchQuery" class="search-input" placeholder="搜索校区、食堂、宿舍楼..."
            @input="searchFocused = true" @focus="searchFocused = true"
            @keyup.enter="handleLocationSearch"/>
          <button v-if="searchQuery" class="search-clear" @click="clearSearch">✕</button>
          <button class="search-submit" @click="handleLocationSearch">搜索</button>
        </div>
        <div v-if="searchFocused && filteredPois.length" class="search-dropdown">
          <div v-for="p in filteredPois" :key="p.id" class="search-item" @click="selectSearchResult(p)">
            <img class="si-icon" :src="sceneMeta(p.sceneType).icon" alt="" />
            <div><div class="si-name">{{ p.name }}</div><div class="si-desc">{{ p.desc }}</div></div>
          </div>
        </div>
      </div>

      <!-- 左侧一级分类：点击“全部”展开或收起 -->
      <aside class="category-rail" :class="{ expanded: categoryExpanded }">
        <button
          class="category-all"
          :class="{ active: allCategoriesSelected }"
          type="button"
          :aria-pressed="allCategoriesSelected"
          @click="selectAllCategories"
        >
          <span class="category-all-icon">⠿</span>
          <span>全部</span>
        </button>
        <div class="category-list">
          <button
            v-for="item in categoryItems"
            :key="item.key"
            type="button"
            class="category-item"
            :class="[{ active: isCategorySelected(item.key) }, `category-item--${item.key}`]"
            :aria-pressed="isCategorySelected(item.key)"
            @click="selectCategory(item)"
          >
            <span class="category-item-icon">{{ item.icon }}</span>
            <span>{{ item.label }}</span>
          </button>
        </div>
      </aside>
      <!-- 选中提示条 -->
      <Transition name="fade">
        <div v-if="selectedNotice" class="notice-bar">{{ selectedNotice }}</div>
      </Transition>

      <!-- 顶部工具栏 -->
      <div class="top-tools">
        <button class="tool-btn random-btn" @click="randomWalk" title="随机漫步" aria-label="随机漫步">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M16 3h5v5M4 20l5.5-5.5M21 3l-7.5 7.5M16 21h5v-5M4 4l5.5 5.5" />
            <circle cx="12" cy="12" r="1.6" />
          </svg>
        </button>
      </div>

      <!-- 随机漫步弹窗 -->
      <Transition name="fade">
        <div v-if="randomPoi" class="random-modal">
          <div class="random-name">{{ randomPoi.name }}</div>
          <div class="random-desc">{{ randomPoi.desc }}</div>
          <div class="random-hint">去这里走走吧！</div>
          <button class="random-close" @click="randomPoi = null">好的</button>
        </div>
      </Transition>

      <!-- 选中点位详情面板 -->
      <Transition name="slide-up">
        <div
          v-if="activePoi?.images?.[0]?.imageUrl"
          class="poi-panel canteen-panel"
          :style="{ left: `${activePoiScreen.panelX}px`, top: `${activePoiScreen.panelY}px` }"
        >
          <div
            class="canteen-photo-slot canteen-photo-slot--interactive"
            role="button"
            tabindex="0"
            aria-label="收起餐厅外表图"
            title="点击收起餐厅外表图"
            @click.stop="closeActivePoi"
            @keydown.enter.prevent="closeActivePoi"
            @keydown.space.prevent="closeActivePoi"
          >
            <img
              class="canteen-photo"
              :src="activePoi.images[0].imageUrl"
              :alt="activePoi.name"
              :style="{ objectPosition: `${activePoi.images[0].focusX ?? 50}% ${activePoi.images[0].focusY ?? 50}%` }"
            />
          </div>
        </div>
      </Transition>

      <Transition name="fade">
        <div v-if="indoorOpen" class="indoor-guide-mask" @click.self="closeIndoorGuide">
          <section class="indoor-guide-dialog">
            <div class="indoor-breadcrumb">智慧校园 <b>›</b> {{ indoorCanteen?.name || '食堂' }} <b>›</b> <strong>{{ selectedIndoorFloor?.name || '楼层' }}</strong></div>
            <button class="indoor-guide-close" type="button" @click="closeIndoorGuide">×</button>

            <header class="indoor-toolbar">
              <h2>{{ indoorCanteen?.name || '食堂' }} · {{ selectedIndoorFloor?.name || '楼层' }}</h2>
              <nav class="indoor-view-tabs">
                <button :class="{ active: indoorView === 'map' }" @click="openIndoorView('map')">▱ 平面地图</button>
                <button :class="{ active: indoorView === 'list' }" @click="openIndoorList">☷ 档口列表</button>
                <button :class="{ active: indoorView === 'menu' }" @click="openIndoorView('menu')">▤ 菜品菜单</button>
              </nav>
              <label class="indoor-search"><span>⌕</span><input v-model="indoorSearch" :placeholder="indoorView === 'menu' ? `搜索${selectedIndoorFloor?.name || '本层'}菜品` : `搜索${selectedIndoorFloor?.name || '本层'}档口`" /></label>
            </header>

            <nav class="indoor-categories">
              <button :class="{ active: allIndoorCategoriesSelected }" :aria-pressed="allIndoorCategoriesSelected" @click="selectIndoorCategory('all')">全部</button>
              <button v-for="(label, category) in indoorCategoryLabels" :key="category" :class="{ active: isIndoorCategorySelected(category) }" :aria-pressed="isIndoorCategorySelected(category)" @click="selectIndoorCategory(category)">{{ label }}</button>
              <button class="indoor-back-map" type="button" @click="goBackIndoor">返回</button>
            </nav>

            <main v-if="indoorView === 'map'" class="indoor-map-stage">
              <div ref="indoorPlanViewportRef" class="indoor-plan-viewport" :class="{ dragging: indoorDragging }" @wheel.prevent="onIndoorWheel" @mousedown="startIndoorDrag">
                <div class="indoor-plan-canvas" :style="{ width: `${indoorZoom * 100}%` }">
                  <img v-if="indoorFloorPlan?.imageUrl" :src="indoorFloorPlan.imageUrl" :alt="`${indoorCanteen?.name || '食堂'}${selectedIndoorFloor?.name || '楼层'}平面图`" draggable="false" />
                  <p v-else class="indoor-empty">该楼层暂未上传平面图</p>
                  <template v-if="filteredDemoFacilities.length">
                    <span v-for="facility in filteredDemoFacilities" :key="facility.id" class="facility-map-marker" :style="{ left: `${facility.x}%`, top: `${facility.y}%`, '--facility-color': facility.color }"><i>{{ facility.symbol }}</i><b>{{ facility.name }}</b></span>
                  </template>
                  <template v-else-if="indoorZoom < 1.35 && allIndoorCategoriesSelected && !indoorSearch">
                    <span v-for="cluster in indoorClusters" :key="cluster.id" class="stall-cluster" :style="{ left: `${cluster.x}%`, top: `${cluster.y}%`, background: cluster.color }">{{ cluster.count }}</span>
                  </template>
                  <template v-else-if="indoorZoom < 1.65 && allIndoorCategoriesSelected && !indoorSearch">
                    <span v-for="stall in filteredDemoStalls" :key="stall.id" class="demo-stall-marker" :style="{ left: `${stall.x}%`, top: `${stall.y}%`, '--stall-color': stall.color }" @click.stop="openStallDetail(stall)"><i></i><b>{{ stall.name }}</b></span>
                  </template>
                  <template v-else>
                    <span v-for="stall in filteredDemoStalls" :key="stall.id" class="demo-stall-marker" :style="{ left: `${stall.x}%`, top: `${stall.y}%`, '--stall-color': stall.color }" @click.stop="openStallDetail(stall)">
                      <i></i><b>{{ stall.name }}</b>
                    </span>
                  </template>
                </div>
              </div>
              <div class="indoor-zoom-controls">
                <button @click="adjustIndoorZoom(.2)">＋</button>
                <button @click="adjustIndoorZoom(-.2)">−</button>
                <button @click="indoorZoom = 1.12">◎</button>
              </div>
              <span class="indoor-zoom-hint">{{ indoorZoom < 1.35 ? '滚轮向上：逐步拆分聚合档口' : indoorZoom < 1.65 ? '继续放大：查看全部档口名称' : `当前显示 ${filteredDemoStalls.length} 个档口` }}</span>
            </main>

            <main v-else-if="indoorView === 'list'" class="indoor-content-view">
              <div class="indoor-content-heading"><strong>{{ selectedIndoorFloor?.name || '本层' }}档口</strong><span>共{{ filteredDemoStalls.length }}个档口</span></div>
              <div class="indoor-stall-grid">
                <article v-for="stall in filteredDemoStalls" :key="stall.id" class="indoor-stall-card">
                  <img v-if="stall.imageUrl" class="stall-card-image" :src="stall.imageUrl" :alt="stall.name" loading="lazy" />
                  <span v-else class="stall-card-image-placeholder">▱</span>
                  <div class="stall-card-title">
                    <span><strong>{{ stall.name }}</strong><small>{{ indoorCategoryLabels[stall.category] }}</small></span>
                    <em v-if="stallStatusLabels[stall.stallStatus]" :class="`status-${stall.stallStatus}`">{{ stallStatusLabels[stall.stallStatus] }}</em>
                  </div>
                  <div class="stall-card-meta-row">
                    <small v-if="stall.businessHours">营业时间 {{ stall.businessHours }}</small>
                  </div>
                  <button v-if="stall.description" class="stall-card-intro" type="button" @click="toggleIntroPreview($event, '档口介绍', stall.description)">
                    <strong>档口介绍</strong><span>{{ stall.description }}</span><i>›</i>
                  </button>
                  <div class="stall-card-actions"><button @click="showStallOnMap(stall)">查看位置</button><button @click="showStallMenu(stall)">查看菜品</button></div>
                </article>
                <p v-if="!filteredDemoStalls.length" class="indoor-empty">当前筛选下暂无档口</p>
              </div>
            </main>

            <main v-else class="indoor-content-view">
              <div class="indoor-content-heading"><strong>{{ selectedIndoorFloor?.name || '本层' }}菜品</strong><span>共{{ filteredDemoMenu.length }}道菜品</span></div>
              <div class="indoor-menu-grid">
                <article v-for="dish in filteredDemoMenu" :key="dish.id" class="indoor-menu-card">
                  <img v-if="dish.image" :src="dish.image" :alt="dish.name" loading="lazy" />
                  <span v-else class="indoor-menu-card__placeholder">餐</span>
                  <div class="indoor-menu-card__body">
                    <strong>{{ dish.name }}</strong>
                    <span class="dish-category">{{ indoorCategoryLabels[dish.category] }}</span>
                    <button v-if="dish.description" class="dish-description" type="button" @click="toggleIntroPreview($event, '菜品介绍', dish.description)"><b>菜品介绍</b><span>{{ dish.description }}</span><i>›</i></button>
                  </div>
                  <div class="dish-card-footer"><span v-if="dish.taste">口味：{{ dish.taste }}</span><b>¥{{ dish.price }}</b></div>
                </article>
                <p v-if="!filteredDemoMenu.length" class="indoor-empty">当前筛选下暂无菜品</p>
              </div>
            </main>

            <Transition name="intro-preview">
              <aside v-if="introPreview" class="intro-preview-popover" :style="introPreview.style">
                <strong>{{ introPreview.title }}</strong>
                <button type="button" aria-label="关闭介绍" @click="introPreview = null">×</button>
                <p>{{ introPreview.text }}</p>
              </aside>
            </Transition>

          </section>
        </div>
      </Transition>
    </div>

  </div>
</template>

<style scoped>
/* ═══ 页面主题变量 ═══ */
.map-page {
  --bg: var(--hp-bg, #f5f0e7); --surface: var(--hp-cream, #fbf8f2); --text: var(--hp-ink, #171717); --text2: #6f6a60;
  --border: rgba(23, 23, 23, 0.16); --primary: var(--hp-ink, #171717); --primary-light: #f7f2e8;
  --canvas: #e3ebf2; --canvas-text: var(--hp-ink, #171717); --shadow: rgba(0,0,0,0);
  position: relative; width: 100%; height: 100vh; overflow: hidden;
  background: var(--bg); color: var(--text); transition: background .3s, color .3s;
  font-family: Inter, 'Segoe UI', system-ui, -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
.map-main { position: absolute; inset: 60px 0 0 0; }

/* ═══ 高德地图容器 ═══ */
.map-canvas {
  width: 100%; height: 100%;
  position: relative; overflow: hidden;
}
.map-loading, .map-fallback {
  position: absolute; inset: 0;
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px;
  background: linear-gradient(135deg, #e0f2fe, #dbeafe);
  color: var(--primary); font-size: 16px; font-weight: 600; z-index: 5;
}
.map-fallback { color: #ef4444; }
.map-fallback small { font-size: 13px; font-weight: 400; color: var(--text2); }
.map-canvas :global(.real-map-marker) {
  display: flex; width: 140px; height: 64px; align-items: center;
  flex-direction: column; justify-content: flex-end; cursor: pointer;
}
.map-canvas :global(.real-map-marker:hover .real-map-marker__title) {
  border-color: rgba(37,99,235,.35);
}
.map-canvas :global(.real-map-marker__title) {
  overflow: hidden; max-width: 136px; margin-bottom: 3px; padding: 4px 9px;
  border: 1px solid rgba(15,23,42,.1); border-radius: 5px;
  color: #172033; background: rgba(255,255,255,.97);
  box-shadow: 0 2px 8px rgba(15,23,42,.16); font-size: 13px;
  font-weight: 600; line-height: 18px; text-overflow: ellipsis; white-space: nowrap;
}
.map-canvas :global(.real-map-marker__icon) {
  display: block; width: 28px; height: 36px; filter: drop-shadow(0 3px 4px rgba(15,23,42,.22));
}
.map-canvas :global(.map-info-window) {
  min-width: 190px; max-width: 260px; padding: 10px 14px;
  color: #334155; font-family: system-ui, sans-serif;
}
.map-canvas :global(.map-info-window__heading) {
  display: flex; align-items: center; gap: 8px; margin-bottom: 5px;
}
.map-canvas :global(.map-info-window__heading strong) { color: #111827; font-size: 16px; }
.map-canvas :global(.map-info-window__pin) {
  width: 16px; height: 21px; flex: 0 0 auto;
}
.map-canvas :global(.map-info-window__type) {
  display: block; margin-bottom: 5px; color: #3b82f6; font-size: 12px;
}
.map-canvas :global(.map-info-window p) {
  margin: 0; color: #64748b; font-size: 13px; line-height: 1.5;
}
.map-canvas :global(.map-info-window__indoor-button) {
  width: 100%; margin-top: 10px; padding: 8px 12px;
  border: 0; border-radius: 7px; background: #f97316;
  color: #fff; font-size: 13px; font-weight: 600; cursor: pointer;
}
.map-canvas :global(.map-info-window__indoor-button:hover) { background: #ea580c; }
.spinner {
  width: 32px; height: 32px;
  border: 3px solid #bfdbfe; border-top-color: var(--primary);
  border-radius: 50%; animation: spin .8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ═══ 搜索栏 ═══ */
.search-wrapper {
  position: absolute; top: 24px; left: 28px;
  width: min(470px, calc(100% - 120px)); z-index: 12;
}
.search-box {
  display: flex; align-items: center; gap: 10px; height: 58px;
  padding: 0 10px 0 18px; background: var(--surface); border-radius: 16px;
  border: 1px solid var(--border); box-shadow: 0 6px 22px rgba(15,23,42,.12);
}
.search-icon { flex: 0 0 18px; width: 18px; height: 18px; color: var(--text2); }
.search-input {
  flex: 1; border: none; outline: none; font-size: 14px;
  color: var(--text); background: transparent;
}
.search-input::placeholder { color: var(--text2); }
.search-clear {
  width: 22px; height: 22px; display: grid; place-items: center;
  border-radius: 50%; background: var(--primary-light); border: none;
  color: var(--text2); font-size: 12px; cursor: pointer;
}
.search-submit {
  height: 38px; padding: 0 20px; border: 0; border-radius: 10px;
  color: #fff; background: #0758cf; font-weight: 700; cursor: pointer;
}
.search-dropdown {
  margin-top: 8px; background: var(--surface); border-radius: 14px;
  box-shadow: 0 8px 30px var(--shadow); overflow: hidden;
}
.search-item {
  display: flex; align-items: center; gap: 10px; padding: 12px 16px;
  cursor: pointer; transition: background .12s;
}
.search-item:hover { background: var(--primary-light); }
.search-item + .search-item { border-top: 1px solid var(--border); }
.si-icon { width: 16px; height: 21px; flex: 0 0 auto; }
.si-name { font-size: 14px; font-weight: 600; color: var(--text); }
.si-desc { font-size: 12px; color: var(--text2); margin-top: 2px; }
.notice-bar {
  position: absolute; top: 70px; left: 50%; transform: translateX(-50%);
  padding: 8px 20px; border-radius: 20px; z-index: 11;
  background: var(--primary); color: #fff; font-size: 13px; font-weight: 600;
  box-shadow: 0 4px 16px rgba(59,130,246,.3); white-space: nowrap;
}

/* ═══ 顶部工具栏 ═══ */
.top-tools { position: absolute; top: 16px; right: 16px; z-index: 12; display: flex; gap: 6px; }
.tool-btn {
  width: 38px; height: 38px; display: grid; place-items: center;
  border-radius: 50%; border: none; font-size: 18px;
  background: var(--surface); box-shadow: 0 2px 10px var(--shadow);
  cursor: pointer; transition: transform .15s;
}
.tool-btn:hover { transform: scale(1.1); }
.tool-btn svg {
  width: 19px;
  height: 19px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.7;
  stroke-linecap: round;
  stroke-linejoin: round;
}

/* ═══ 左侧校园一级分类 ═══ */
.category-rail {
  position: absolute; top: 168px; left: 28px; z-index: 12;
  width: 96px; height: 94px; overflow: hidden;
  border: 1px solid rgba(148,163,184,.22); border-radius: 24px;
  background: rgba(255,255,255,.96); box-shadow: 0 12px 32px rgba(15,23,42,.14);
  transition: height .28s cubic-bezier(.4,0,.2,1); backdrop-filter: blur(14px);
}
.category-rail.expanded { height: 430px; }
.category-all, .category-item {
  width: 100%; border: 0; background: transparent; color: #4c5b70;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 7px; font-size: 13px; font-weight: 650; cursor: default;
}
.category-all { height: 94px; color: #fff; background: #075fd7; cursor: pointer; }
.category-all-icon { font-size: 28px; line-height: 22px; }
.category-list { opacity: 0; transform: translateY(-10px); transition: opacity .18s, transform .24s; }
.category-rail.expanded .category-list { opacity: 1; transform: translateY(0); }
.category-item { height: 84px; border-bottom: 1px solid #edf1f5; cursor: pointer; }
.category-item-icon {
  width: 34px; height: 34px; display: grid; place-items: center;
  border-radius: 11px; color: #2e75d2; background: #eef5ff; font-weight: 850;
}
.category-item.active { color: #0758cf; background: #f4f8ff; }

/* ═══ 随机漫步 ═══ */
.random-modal {
  position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);
  z-index: 15; background: var(--surface); border-radius: 20px;
  padding: 28px 32px; text-align: center;
  box-shadow: 0 12px 40px var(--shadow); min-width: 220px;
}
.random-icon { font-size: 48px; margin-bottom: 8px; }
.random-name { font-size: 20px; font-weight: 700; color: var(--text); }
.random-desc { font-size: 13px; color: var(--text2); margin: 6px 0 12px; }
.random-hint { font-size: 14px; color: var(--primary); font-weight: 600; margin-bottom: 14px; }
.random-close {
  padding: 8px 28px; border-radius: 20px; border: none;
  background: var(--primary); color: #fff; font-weight: 700; cursor: pointer; font-size: 14px;
}

/* ═══ 底部快捷按钮 ═══ */
.quick-bar {
  position: absolute; bottom: 16px; left: 50%; transform: translateX(-50%);
  display: flex; gap: 6px; padding: 8px 12px; z-index: 10;
  background: var(--surface); border-radius: 20px;
  box-shadow: 0 4px 24px var(--shadow); overflow-x: auto;
  max-width: calc(100% - 32px); scrollbar-width: none;
}
.quick-bar::-webkit-scrollbar { display: none; }
.quick-btn {
  display: flex; flex-direction: column; align-items: center; gap: 3px;
  padding: 8px 12px; border: none; border-radius: 14px;
  background: transparent; cursor: pointer; transition: all .15s;
  white-space: nowrap; min-width: 58px;
}
.quick-btn:hover { background: var(--primary-light); }
.quick-btn.active { background: var(--primary-light); }
.quick-icon { width: 16px; height: 21px; }
.quick-label { font-size: 11px; font-weight: 600; color: var(--text2); }
.quick-btn.active .quick-label { color: var(--primary); }
.quick-empty { padding: 12px 18px; color: var(--text2); font-size: 13px; white-space: nowrap; }

/* ═══ 点位详情面板 ═══ */
.poi-panel {
  position: absolute; bottom: 80px; left: 16px; right: 16px;
  max-width: 440px; margin: 0 auto; z-index: 11;
  background: var(--surface); border-radius: 16px;
  box-shadow: 0 8px 32px var(--shadow); padding: 16px;
  max-height: 50vh; overflow-y: auto;
}

.canteen-panel {
  right: auto; bottom: auto; width: 280px; max-height: none;
  margin: 0; padding: 8px; overflow: hidden;
  border: 1px solid rgba(148,163,184,.24); border-radius: 12px;
  background: rgba(255,255,255,.97); box-shadow: 0 12px 34px rgba(15,23,42,.2);
}
.canteen-photo-slot {
  width: 100%; aspect-ratio: 4 / 3; overflow: hidden;
  border-radius: 8px; background: #edf1f5;
}
.canteen-photo-slot--interactive { cursor: zoom-out; }
.canteen-photo-slot--interactive:focus-visible {
  outline: 2px solid var(--hp-ink);
  outline-offset: 2px;
}
.canteen-photo { width: 100%; height: 100%; display: block; object-fit: cover; }
.canteen-photo-placeholder {
  width: 100%; height: 100%; display: grid; place-items: center; color: #9aa7b7;
}
.canteen-photo-placeholder svg {
  width: 46px; height: 46px; fill: none; stroke: currentColor;
  stroke-width: 1.5; stroke-linecap: round; stroke-linejoin: round;
}

@media (max-width: 900px) {
  .canteen-panel { right: auto; bottom: auto; width: min(280px, calc(100% - 36px)); }
  .category-rail { left: 18px; }
  .search-wrapper { left: 18px; width: calc(100% - 126px); }
}
.poi-header { display: flex; justify-content: space-between; align-items: center; }
.poi-name { font-size: 18px; font-weight: 700; color: var(--text); }
.poi-type { margin-top: 5px; color: var(--primary); font-size: 12px; }
.poi-cover {
  width: 100%; height: 150px; margin-top: 12px; border-radius: 10px;
  object-fit: cover;
}
.poi-close {
  width: 28px; height: 28px; display: grid; place-items: center;
  border-radius: 50%; border: none; background: var(--primary-light);
  color: var(--text2); cursor: pointer; font-size: 14px;
}
.poi-desc { font-size: 13px; color: var(--text2); margin: 8px 0 12px; line-height: 1.5; }
.poi-location {
  margin-top: -4px; color: var(--text2); font-size: 12px; line-height: 1.5;
}
.poi-section { margin-top: 12px; padding-top: 12px; border-top: 1px solid var(--border); }
.section-label { font-size: 13px; font-weight: 700; color: var(--text); margin-bottom: 8px; }
.checkin-btn {
  padding: 6px 20px; border-radius: 16px; border: none;
  background: var(--primary); color: #fff; font-size: 13px; font-weight: 600; cursor: pointer;
}
.checkin-btn:hover { opacity: .85; }
.checkin-done { font-size: 13px; color: #16a34a; font-weight: 600; }
.board-list { max-height: 120px; overflow-y: auto; margin-bottom: 8px; }
.board-msg {
  padding: 6px 0; font-size: 13px; border-bottom: 1px solid var(--border);
  display: flex; flex-wrap: wrap; gap: 4px; align-items: baseline;
}
.board-author { font-weight: 700; color: var(--primary); margin-right: 6px; }
.board-text { color: var(--text); flex: 1; }
.board-time { font-size: 11px; color: var(--text2); }
.board-empty { font-size: 13px; color: var(--text2); padding: 8px 0; }
.board-form { display: flex; gap: 6px; }
.board-author-input, .board-input {
  padding: 6px 10px; border-radius: 14px; border: 1px solid var(--border);
  font-size: 13px; outline: none; background: var(--bg); color: var(--text);
}
.board-author-input { width: 70px; }
.board-input { flex: 1; }
.board-send {
  padding: 6px 14px; border-radius: 14px; border: none;
  background: var(--primary); color: #fff; font-size: 13px; font-weight: 600; cursor: pointer;
}

/* ═══ 失物招领 ═══ */
.lost-body { flex: 1; overflow-y: auto; padding: 12px; max-height: 400px; background: var(--bg); }
.lost-new-btn {
  width: 100%; padding: 8px; border-radius: 14px; border: 1px dashed var(--border);
  background: transparent; color: var(--primary); font-weight: 600;
  cursor: pointer; margin-bottom: 10px; font-size: 13px;
}
.lost-form { display: flex; flex-direction: column; gap: 6px; margin-bottom: 10px; }
.lost-form select, .lost-form input {
  padding: 8px 12px; border-radius: 10px; border: 1px solid var(--border);
  font-size: 13px; outline: none; background: var(--surface); color: var(--text);
}
.lost-submit {
  padding: 8px; border-radius: 12px; border: none;
  background: var(--primary); color: #fff; font-weight: 700; cursor: pointer; font-size: 14px;
}
.lost-empty { text-align: center; color: var(--text2); font-size: 13px; padding: 24px 0; }
.lost-card {
  padding: 10px 12px; background: var(--surface); border-radius: 12px;
  margin-bottom: 8px; border: 1px solid var(--border);
}
.lost-tag {
  display: inline-block; padding: 2px 8px; border-radius: 8px;
  font-size: 11px; font-weight: 700; margin-bottom: 4px;
}
.lost-tag.lost { background: #fef2f2; color: #dc2626; }
.lost-tag.found { background: #f0fdf4; color: #16a34a; }
.lost-title { font-size: 14px; font-weight: 600; color: var(--text); }
.lost-desc { font-size: 13px; color: var(--text2); margin-top: 2px; }
.lost-meta { font-size: 11px; color: var(--text2); margin-top: 4px; }

/* ═══ 动画 ═══ */
.slide-up-enter-active, .slide-up-leave-active { transition: all .25s ease; }
.slide-up-enter-from, .slide-up-leave-to { opacity: 0; transform: translateY(20px); }
.fade-enter-active, .fade-leave-active { transition: opacity .2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.indoor-guide-mask {
  position: fixed; inset: 0; z-index: 1200;
  display: grid; place-items: center; padding: 32px;
  background: rgba(15, 23, 42, .58); backdrop-filter: blur(4px);
}
.indoor-guide-dialog {
  display: flex; width: min(1180px, 94vw); height: min(820px, 88vh);
  flex-direction: column; overflow: hidden;
  border: 1px solid var(--border); border-radius: 18px;
  background: var(--surface); box-shadow: 0 24px 80px rgba(15, 23, 42, .34);
}
.indoor-guide-header {
  display: flex; align-items: flex-start; justify-content: space-between; gap: 24px;
  padding: 22px 26px 18px; border-bottom: 1px solid var(--border);
}
.indoor-guide-eyebrow {
  color: #f97316; font-size: 12px; font-weight: 700; letter-spacing: .08em;
}
.indoor-guide-header h2 { margin: 5px 0 4px; color: var(--text); font-size: 23px; }
.indoor-guide-header p { margin: 0; color: var(--text2); font-size: 13px; }
.indoor-guide-close {
  display: grid; width: 34px; height: 34px; place-items: center;
  border: 0; border-radius: 50%; background: var(--primary-light);
  color: var(--text2); font-size: 24px; line-height: 1; cursor: pointer;
}
.indoor-guide-tabs {
  display: flex; gap: 8px; padding: 14px 26px;
  overflow-x: auto; border-bottom: 1px solid var(--border);
}
.indoor-guide-tabs button {
  min-width: 82px; padding: 8px 16px;
  border: 1px solid var(--border); border-radius: 8px;
  background: transparent; color: var(--text2); font-weight: 600; cursor: pointer;
}
.indoor-guide-tabs button.active {
  border-color: #f97316; background: #fff7ed; color: #c2410c;
}
.indoor-guide-state {
  display: flex; flex: 1; align-items: center; justify-content: center; gap: 12px;
  color: var(--text2); font-size: 14px;
}
.indoor-guide-body {
  display: grid; min-height: 0; flex: 1;
  grid-template-columns: minmax(0, 1fr) 280px; gap: 18px; padding: 20px 26px 26px;
}
.indoor-guide-plan {
  position: relative; align-self: start; overflow: hidden;
  border: 1px solid var(--border); border-radius: 10px; background: var(--bg);
}
.indoor-guide-plan > img { display: block; width: 100%; height: auto; }
.indoor-guide-marker {
  position: absolute; z-index: 2; transform: translate(-50%, -50%);
  pointer-events: none;
}
.indoor-guide-marker i {
  display: block; width: 15px; height: 15px; margin: auto;
  border: 3px solid #fff; border-radius: 50%; background: #f97316;
  box-shadow: 0 2px 7px rgba(15, 23, 42, .38);
}
.indoor-guide-marker b {
  display: block; max-width: 150px; margin-top: 4px; padding: 4px 7px;
  overflow: hidden; border-radius: 5px; background: rgba(31, 41, 55, .9);
  color: #fff; font-size: 12px; font-weight: 600;
  text-overflow: ellipsis; white-space: nowrap;
}
.indoor-guide-stalls {
  min-height: 0; overflow-y: auto;
  border: 1px solid var(--border); border-radius: 10px; background: var(--bg);
}
.indoor-guide-stalls__title {
  position: sticky; top: 0; z-index: 1;
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 15px; border-bottom: 1px solid var(--border);
  background: var(--surface); color: var(--text); font-weight: 700;
}
.indoor-guide-stalls__title span {
  min-width: 24px; padding: 2px 7px; border-radius: 12px;
  background: var(--primary-light); color: var(--primary); text-align: center; font-size: 12px;
}
.indoor-guide-stall {
  display: flex; align-items: center; justify-content: space-between; gap: 10px;
  padding: 12px 14px; border-bottom: 1px solid var(--border);
}
.indoor-guide-stall > span { min-width: 0; }
.indoor-guide-stall strong,
.indoor-guide-stall small {
  display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.indoor-guide-stall strong { color: var(--text); font-size: 13px; }
.indoor-guide-stall small { margin-top: 3px; color: var(--text2); font-size: 11px; }
.indoor-guide-stall em {
  flex: 0 0 auto; padding: 3px 7px; border-radius: 10px;
  background: var(--border); color: var(--text2); font-size: 11px; font-style: normal;
}
.indoor-guide-stall em.ready { background: #dcfce7; color: #15803d; }
.indoor-guide-stalls__empty { padding: 40px 16px; color: var(--text2); text-align: center; }

/* ═══ 食堂楼层平面导览 ═══ */
.indoor-guide-dialog {
  position: relative; width: min(1080px, 72vw); height: min(790px, 78vh);
  min-width: 780px; background: #fff;
}
.indoor-breadcrumb {
  padding: 18px 28px 10px; color: #627086; font-size: 12px;
}
.indoor-breadcrumb b { margin: 0 8px; color: #98a4b4; }
.indoor-breadcrumb strong { color: #0758cf; }
.indoor-guide-dialog > .indoor-guide-close { position: absolute; top: 15px; right: 22px; z-index: 4; background: transparent; }
.indoor-toolbar {
  display: grid; grid-template-columns: minmax(190px, 1fr) auto minmax(220px, .9fr);
  align-items: center; gap: 22px; padding: 14px 28px 18px; border-bottom: 1px solid #dce4ef;
}
.indoor-toolbar h2 { margin: 0; color: #182335; font-size: 24px; }
.indoor-view-tabs { display: flex; padding: 5px; border-radius: 10px; background: #eaf1ff; }
.indoor-view-tabs button {
  height: 42px; padding: 0 18px; border: 0; border-radius: 8px;
  color: #4c5a70; background: transparent; font-weight: 700; cursor: pointer;
}
.indoor-view-tabs button.active { color: #0758cf; background: #fff; box-shadow: 0 2px 8px rgba(33,72,128,.1); }
.indoor-search {
  display: flex; height: 44px; align-items: center; gap: 9px; padding: 0 14px;
  border: 1px solid #b9c8df; border-radius: 9px; background: #f6f9ff;
}
.indoor-search span { color: #53647d; font-size: 20px; }
.indoor-search input { width: 100%; border: 0; outline: 0; color: #27364a; background: transparent; }
.indoor-categories {
  display: flex; align-items: center; gap: 9px; padding: 13px 28px;
  overflow-x: auto; border-bottom: 1px solid #dce4ef; background: #f3f7ff;
}
.indoor-categories button, .indoor-facility {
  flex: 0 0 auto; min-width: 62px; height: 44px; padding: 0 14px;
  border: 1px solid #bdc9db; border-radius: 999px; display: grid; place-items: center;
  color: #435168; background: #fff; font-size: 12px; font-weight: 700;
}
.indoor-categories button { cursor: pointer; }
.indoor-categories button.active { border-color: #0758cf; color: #fff; background: #0758cf; }
.indoor-facility { min-width: 54px; color: #46536a; cursor: pointer; }
.indoor-facility--first { margin-left: clamp(28px, 4vw, 72px); }
.indoor-map-stage {
  position: relative; min-height: 0; flex: 1; padding: 26px 48px;
  background-color: #edf4ff;
  background-image: linear-gradient(#dce8fa 1px, transparent 1px), linear-gradient(90deg, #dce8fa 1px, transparent 1px);
  background-size: 24px 24px;
}
.indoor-plan-viewport {
  width: 100%; height: 100%; overflow: auto; border: 1px solid #bbc8da;
  border-radius: 8px; background: #fff; box-shadow: 0 5px 18px rgba(31,55,88,.12);
  cursor: grab; touch-action: none; user-select: none;
}
.indoor-plan-viewport.dragging { cursor: grabbing; }
.indoor-plan-canvas {
  position: relative; width: 100%; height: auto; min-height: 0; aspect-ratio: 1680 / 1185;
  transition: width .2s ease, height .2s ease;
}
.indoor-plan-canvas > img { width: 100%; height: 100%; display: block; object-fit: contain; pointer-events: none; }
.stall-cluster {
  position: absolute; z-index: 3; width: 38px; height: 38px; display: grid; place-items: center;
  border: 4px solid #fff; border-radius: 50%; color: #fff; font-size: 15px; font-weight: 850;
  box-shadow: 0 4px 12px rgba(15,23,42,.3); transform: translate(-50%, -50%);
}
.demo-stall-marker { position: absolute; z-index: 3; transform: translate(-50%, -50%); }
.demo-stall-marker i {
  display: block; width: 31px; height: 31px; margin: auto; border: 4px solid #fff;
  border-radius: 50%; background: var(--stall-color); box-shadow: 0 3px 10px rgba(15,23,42,.28);
}
.demo-stall-marker b {
  display: block; margin-top: 2px; padding: 4px 7px; border: 1px solid #d4deea;
  border-radius: 5px; color: #253349; background: rgba(255,255,255,.96);
  box-shadow: 0 2px 7px rgba(15,23,42,.14); font-size: 11px; white-space: nowrap;
}
.facility-map-marker { position: absolute; z-index: 4; transform: translate(-50%, -50%); text-align: center; }
.facility-map-marker i { display: grid; width: 34px; height: 34px; margin: auto; place-items: center; border: 4px solid #fff; border-radius: 50%; color: #fff; background: var(--facility-color); box-shadow: 0 3px 10px rgba(15,23,42,.28); font-size: 10px; font-style: normal; font-weight: 800; }
.facility-map-marker b { display: block; margin-top: 3px; padding: 4px 7px; border: 1px solid #d4deea; border-radius: 5px; color: #253349; background: #fff; box-shadow: 0 2px 7px rgba(15,23,42,.14); font-size: 11px; white-space: nowrap; }
.indoor-zoom-controls {
  position: absolute; right: 59px; bottom: 37px; display: flex; flex-direction: column;
  overflow: hidden; border: 1px solid #c5d0df; border-radius: 9px; box-shadow: 0 4px 12px rgba(15,23,42,.14);
}
.indoor-zoom-controls button { width: 40px; height: 40px; border: 0; border-bottom: 1px solid #dce4ef; color: #25354a; background: #fff; font-size: 22px; cursor: pointer; }
.indoor-zoom-controls button:last-child { border-bottom: 0; color: #0758cf; }
.indoor-zoom-hint { position: absolute; left: 59px; bottom: 37px; padding: 7px 11px; border-radius: 8px; color: #53647c; background: rgba(255,255,255,.92); font-size: 11px; }
.indoor-content-view { min-height: 0; flex: 1; margin: 22px 28px; overflow: hidden; border: 1px solid #cbd6e5; border-radius: 12px; background: #fff; }
.indoor-content-heading { display: flex; height: 58px; align-items: center; justify-content: space-between; padding: 0 24px; border-bottom: 1px solid #dce4ef; color: #263449; }
.indoor-content-heading span { color: #74849a; font-size: 12px; }
.indoor-stall-grid, .indoor-menu-grid { height: calc(100% - 58px); padding: 22px; overflow-y: auto; background: #f8faff; }
.indoor-stall-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); grid-auto-rows: minmax(330px, auto); align-content: start; gap: 18px; }
.indoor-stall-card { min-height: 330px; overflow: hidden; border: 1px solid #cbd6e5; border-radius: 11px; background: #fff; box-shadow: 0 2px 6px rgba(31,55,88,.04); }
.stall-card-image, .stall-card-image-placeholder { width: 100%; height: 126px; display: block; background: #eef2f7; }
.stall-card-image { object-fit: cover; }
.stall-card-image-placeholder { display: grid; place-items: center; color: #9aa9bb; font-size: 48px; }
.stall-card-title { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 15px 16px 0; }
.stall-card-title > span { min-width: 0; display: flex; align-items: center; gap: 8px; }
.stall-card-title strong { min-width: 0; overflow: hidden; color: #1d2a3d; font-size: 15px; text-overflow: ellipsis; white-space: nowrap; }
.stall-card-title small { flex: 0 0 auto; padding: 2px 7px; border-radius: 5px; color: #f97316; background: #fff0e7; font-size: 10px; }
.stall-card-title em { flex: 0 0 auto; padding: 3px 8px; border: 1px solid currentColor; border-radius: 6px; font-size: 11px; font-style: normal; }
.stall-card-title em.status-1 { color: #16a34a; background: #f0fdf4; }
.stall-card-title em.status-2 { color: #d97706; background: #fffbeb; }
.stall-card-title em.status-3 { color: #64748b; background: #f8fafc; }
.stall-card-meta-row { display: flex; min-height: 24px; align-items: center; justify-content: space-between; gap: 12px; padding: 9px 16px 0; }
.stall-card-meta-row small { min-width: 0; overflow: hidden; color: #64748b; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.stall-card-meta-row b { flex: 0 0 auto; color: #e5484d; font-size: 13px; }
.stall-card-intro { width: calc(100% - 32px); margin: 13px 16px 0; padding: 0; border: 0; display: grid; grid-template-columns: auto minmax(0, 1fr) auto; align-items: center; gap: 7px; color: #64748b; background: transparent; text-align: left; cursor: pointer; }
.stall-card-intro strong { color: #334155; font-size: 12px; }
.stall-card-intro span { overflow: hidden; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.stall-card-intro i { color: #0758cf; font-size: 18px; font-style: normal; }
.stall-card-actions { display: grid; grid-template-columns: 1fr 1fr; gap: 9px; margin: 16px; }
.stall-card-actions button { height: 36px; border: 1px solid #0758cf; border-radius: 7px; color: #0758cf; background: #fff; font-weight: 700; cursor: pointer; }
.stall-card-actions button:last-child { color: #fff; background: #0758cf; }
.intro-preview-popover { position: absolute; z-index: 8; width: min(340px, calc(100% - 32px)); max-height: 60%; padding: 20px; overflow-y: auto; border: 1px solid #cbd6e5; border-radius: 12px; background: #fff; box-shadow: 0 16px 42px rgba(15,23,42,.2); }
.intro-preview-popover strong { color: #1d2a3d; font-size: 16px; }
.intro-preview-popover > button { position: absolute; top: 10px; right: 12px; width: 30px; height: 30px; border: 0; border-radius: 50%; color: #64748b; background: #f1f5f9; font-size: 20px; cursor: pointer; }
.intro-preview-popover p { margin: 12px 0 0; color: #566579; font-size: 14px; line-height: 1.8; white-space: pre-wrap; overflow-wrap: anywhere; }
.intro-preview-enter-active, .intro-preview-leave-active { transition: opacity .18s ease, transform .18s ease; }
.intro-preview-enter-from, .intro-preview-leave-to { opacity: 0; transform: translateX(8px); }
.indoor-menu-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); grid-auto-rows: 290px; align-content: start; gap: 18px; }
.indoor-menu-card { position: relative; min-height: 290px; overflow: hidden; border: 1px solid #cbd6e5; border-radius: 11px; background: #fff; box-shadow: 0 2px 7px rgba(31,55,88,.06); }
.indoor-menu-card > img { display: block; width: 100%; height: 142px; object-fit: cover; background: #e9f0fb; }
.indoor-menu-card__placeholder { width: 100%; height: 142px; display: grid; place-items: center; color: #9aa9bb; background: #eef2f7; font-size: 32px; }
.indoor-menu-card__body { display: flex; padding: 13px 14px 42px; flex-direction: column; gap: 5px; }
.indoor-menu-card strong { color: #1d2a3d; font-size: 15px; }
.dish-category { width: fit-content; padding: 2px 7px; border-radius: 5px; color: #f97316; background: #fff0e7; font-size: 10px; }
.dish-description { width: 100%; min-width: 0; margin: 4px 0 0; padding: 0; border: 0; display: grid; grid-template-columns: auto minmax(0, 1fr) auto; gap: 7px; color: #64748b; background: transparent; font-size: 11px; text-align: left; cursor: pointer; }
.dish-description b { color: #334155; }
.dish-description span { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dish-description i { color: #0758cf; font-size: 15px; font-style: normal; }
.dish-card-footer { position: absolute; right: 13px; bottom: 16px; left: 13px; display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.dish-card-footer span { min-width: 0; overflow: hidden; color: #64748b; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.dish-card-footer b { flex: 0 0 auto; color: #ef4444; font-size: 15px; }
.indoor-empty { grid-column: 1 / -1; padding: 70px 0; color: #7a889b; text-align: center; }

/* ═══ 响应式 ═══ */
@media (max-width: 480px) {
  .quick-bar { bottom: 10px; gap: 4px; padding: 6px 8px; }
  .quick-btn { padding: 6px 8px; min-width: 50px; }
  .quick-icon { font-size: 18px; } .quick-label { font-size: 10px; }
  .poi-panel { left: 8px; right: 8px; bottom: 70px; }
  .search-wrapper { width: calc(100% - 80px); }
  .random-modal { width: calc(100% - 48px); padding: 20px; }
}
@media (max-width: 760px) {
  .indoor-guide-mask { padding: 12px; }
  .indoor-guide-dialog { width: 100%; height: 92vh; min-width: 0; }
  .indoor-toolbar { grid-template-columns: 1fr; gap: 10px; overflow-y: auto; }
  .indoor-map-stage { padding: 16px; }
  .indoor-content-view { margin: 12px; }
  .indoor-stall-grid, .indoor-menu-grid { grid-template-columns: 1fr; padding: 14px; }
  .indoor-menu-grid { grid-auto-rows: 310px; }
  .indoor-menu-card { min-height: 310px; }
  .indoor-menu-card > img { height: 150px; }
  .indoor-guide-header { padding: 18px; }
  .indoor-guide-body {
    display: flex; flex-direction: column; overflow-y: auto; padding: 14px 18px 18px;
  }
  .indoor-guide-stalls { flex: 0 0 auto; max-height: 230px; }
}
/* ===== 本站配色覆盖（奶油底 + 黑色描边 + 低饱和马卡龙色，去掉蓝色与投影） ===== */
.search-box,
.search-dropdown,
.tool-btn,
.category-rail,
.random-modal,
.quick-bar,
.poi-panel,
  .canteen-panel,
.indoor-guide-dialog,
.indoor-plan-viewport,
.indoor-stall-card,
.indoor-menu-card,
.intro-preview-popover,
.facility-map-marker b,
.real-map-marker__title {
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-md);
  background: var(--hp-cream);
  box-shadow: none;
}
.tool-btn { border-radius: 50%; color: var(--hp-ink); }
.tool-btn:hover { color: var(--hp-cream); background: var(--hp-ink); }
.category-rail,
.quick-bar { border-radius: 999px; }
.category-item.active { color: var(--hp-ink); background: var(--hp-yellow); }
.indoor-breadcrumb strong { color: var(--hp-ink); }
.indoor-view-tabs button.active {
  color: var(--hp-ink);
  background: var(--hp-yellow);
  box-shadow: none;
}
.indoor-categories button.active {
  border-color: var(--hp-ink);
  color: var(--hp-cream);
  background: var(--hp-ink);
}
.indoor-map-stage { background-color: #f7f2e8; }
.stall-card-actions button:last-child {
  border-radius: 999px;
  color: var(--hp-cream);
  background: var(--hp-ink);
}
.real-map-marker > i,
.real-map-marker > b,
.stall-marker,
.facility-map-marker i,
.facility-map-marker b { box-shadow: none; }
.intro-preview-popover > button { border-radius: 50%; color: var(--hp-muted); background: rgba(23, 23, 23, 0.06); }
</style>
