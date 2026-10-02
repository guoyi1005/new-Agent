const STORAGE_KEY = 'campus-business-sandbox-v3'
const TOTAL_ROUNDS = 4

export const SANDBOX_STRATEGIES = [
  {
    id: 'growth',
    name: '激进增长',
    summary: '扩大曝光和优惠，优先抢占用户规模。',
    effects: { growth: 1.35, demand: 1.06, revenue: .98, cost: 1.06, satisfaction: -.6, risk: 3 },
  },
  {
    id: 'steady',
    name: '稳健运营',
    summary: '控制成本和风险，追求稳定利润。',
    effects: { growth: .9, demand: 1, revenue: 1.02, cost: .92, satisfaction: .5, risk: -2 },
  },
  {
    id: 'experience',
    name: '体验优先',
    summary: '增加服务投入，提升满意度与复购。',
    effects: { growth: 1, demand: 1.04, revenue: 1.01, cost: 1.04, satisfaction: 2.2, risk: -1 },
  },
  {
    id: 'innovation',
    name: '产品创新',
    summary: '投入研发和新功能，换取长期增长。',
    effects: { growth: 1.08, demand: 1.1, revenue: 1.05, cost: 1.08, satisfaction: .8, risk: 1 },
  },
]

export const BUSINESS_SANDBOX_SCENARIOS = [
  {
    id: 'campus-marketplace',
    name: '校园二手交易平台经营沙盘',
    title: '校园二手交易平台',
    companyName: '云集二手',
    description: '经营校园二手交易平台，在增长、信任、服务和利润之间连续做出四轮经营决策。',
    rounds: TOTAL_ROUNDS,
    roundBudget: 30000,
    initial: {
      cash: 100000,
      users: 3200,
      activeUsers: 1750,
      gmv: 198000,
      revenue: 34500,
      cost: 25000,
      profit: 9500,
      satisfaction: 72,
      risk: 18,
    },
    labels: {
      cash: '平台现金',
      users: '注册用户',
      gmv: '本轮成交额',
      satisfaction: '用户满意度',
      risk: '经营风险',
      cashNote: '可支配经营资金',
      usersNote: '当前平台规模',
      gmvNote: '商品交易总额',
      satisfactionNote: '体验与服务质量',
      riskNote: '数值越低越稳定',
      revenueNote: '四轮平台收入',
      costNote: '推广、服务和扩张',
      profitNote: '收入减去成本',
      finalUsersNote: '平台注册规模',
    },
    decisionConfig: {
      marketing: { label: '平台推广', hint: '带来新用户和商品曝光，直接增加当期成本。', min: 0, max: 16000, step: 1000, default: 9000, format: 'money' },
      service: { label: '服务治理', hint: '提升审核、客服和纠纷处理能力，改善信任与风险。', min: 0, max: 14000, step: 1000, default: 6000, format: 'money' },
      innovation: { label: '产品研发', hint: '优化搜索、推荐和交易工具，提升长期体验。', min: 0, max: 12000, step: 1000, default: 5000, format: 'money' },
      incentive: { label: '卖家激励力度', hint: '激励卖家增加商品供给和成交，按成交额计入成本。', min: 0, max: 8, step: 0.5, default: 3, format: 'percent' },
      rate: { label: '平台手续费率', hint: '手续费越高，收入越高，但可能降低活跃和满意度。', min: 3, max: 12, step: 0.5, default: 6, format: 'percent' },
      expansionLabel: '校区扩张',
      expansionHint: '扩张会消耗资金并增加经营风险。',
    },
    expansions: {
      none: { label: '暂不扩张', cost: 0, userBonus: 0, gmvMultiplier: 1, riskDelta: 0, activationBonus: 0 },
      pilot: { label: '试点新校区', cost: 12000, userBonus: 180, gmvMultiplier: 1.08, riskDelta: 2, activationBonus: 0.02 },
      new: { label: '新增一个校区', cost: 30000, userBonus: 420, gmvMultiplier: 1.16, riskDelta: 6, activationBonus: 0.04 },
    },
    roundEvents: [
      { title: '开学季交易需求上涨', description: '教材、数码和生活用品需求增加，平台流量高于平时。', growth: 1.12, demand: 1.1, revenue: 1, cost: 1, satisfaction: 0, risk: 0 },
      { title: '交易纠纷开始增加', description: '部分商品描述不实，用户对平台信任度下降。', growth: .94, demand: .96, revenue: .98, cost: 1.02, satisfaction: -3, risk: 5 },
      { title: '竞争平台补贴卖家', description: '同类平台用补贴争夺优质卖家，商品供给面临压力。', growth: .9, demand: .95, revenue: .97, cost: 1.03, satisfaction: -1, risk: 3 },
      { title: '学校开放新校区合作', description: '新校区希望引入校园平台，扩张机会与经营风险同时出现。', growth: 1.12, demand: 1.08, revenue: 1.02, cost: 1.04, satisfaction: 0, risk: 2 },
    ],
    competitors: [
      { id: 'ai-yihuan', name: '易换校园', persona: 'steady', cash: 96000, users: 3000, activeUsers: 1600, gmv: 180000, satisfaction: 74, risk: 16 },
      { id: 'ai-kuaizhuan', name: '快转二手', persona: 'growth', cash: 88000, users: 3500, activeUsers: 1900, gmv: 220000, satisfaction: 68, risk: 28 },
    ],
    economics: {
      userGrowthDivisor: 140,
      incentiveUserFactor: 25,
      marketUserFactor: 20,
      activationBase: 0.38,
      serviceActivation: 80000,
      satisfactionActivation: 400,
      commissionActivation: 100,
      spendBase: 80,
      satisfactionSpend: 0.4,
      roundSpend: 2,
      serviceRevenuePerUser: 9,
      adRevenuePerUser: 2,
      fixedCost: 3500,
      satisfactionBase: 1.5,
      serviceSatisfaction: 2500,
      incentiveSatisfaction: 0.7,
      innovationSatisfaction: 3000,
      rateSatisfactionPenalty: 1.1,
      rateRiskFactor: 1.2,
      serviceRiskDivisor: 3500,
      innovationGrowthDivisor: 260,
    },
    evaluations: [
      { code: 'business_analysis', label: '商业分析', metric: 'profit' },
      { code: 'user_growth', label: '用户增长', metric: 'growth' },
      { code: 'operations_decision', label: '运营决策', metric: 'operations' },
      { code: 'risk_control', label: '风险控制', metric: 'risk' },
    ],
  },
  {
    id: 'campus-coffee',
    name: '校园咖啡店经营沙盘',
    title: '校园咖啡店',
    companyName: '拾光咖啡',
    description: '经营校园咖啡店，在客流、定价、服务和产品研发之间完成四轮门店经营。',
    rounds: TOTAL_ROUNDS,
    roundBudget: 28000,
    initial: {
      cash: 80000,
      users: 2600,
      activeUsers: 1500,
      gmv: 120000,
      revenue: 48000,
      cost: 39000,
      profit: 9000,
      satisfaction: 76,
      risk: 16,
    },
    labels: {
      cash: '门店现金',
      users: '到店用户',
      gmv: '本轮营业额',
      satisfaction: '顾客满意度',
      risk: '经营风险',
      cashNote: '门店可支配资金',
      usersNote: '本周期到店规模',
      gmvNote: '饮品与轻食营业额',
      satisfactionNote: '口味与服务体验',
      riskNote: '数值越低越稳定',
      revenueNote: '四轮营业收入',
      costNote: '推广、人工和原料',
      profitNote: '收入减去成本',
      finalUsersNote: '最终到店用户规模',
    },
    decisionConfig: {
      marketing: { label: '门店推广', hint: '提高曝光可以带来更多到店用户，但会增加宣传成本。', min: 0, max: 14000, step: 1000, default: 7000, format: 'money' },
      service: { label: '服务与出餐', hint: '增加人力和出餐保障可以改善体验并降低投诉风险。', min: 0, max: 14000, step: 1000, default: 6000, format: 'money' },
      innovation: { label: '菜单研发', hint: '研发新品和会员产品，提升长期吸引力。', min: 0, max: 12000, step: 1000, default: 4000, format: 'money' },
      incentive: { label: '会员优惠力度', hint: '优惠可以提高复购和满意度，但会降低单杯收入。', min: 0, max: 12, step: 0.5, default: 4, format: 'percent' },
      rate: { label: '产品毛利率', hint: '提高毛利率会增加当期收益，但可能影响顾客体验。', min: 30, max: 60, step: 1, default: 45, format: 'percent' },
      expansionLabel: '门店扩张',
      expansionHint: '增加门店会扩大服务范围，同时提高固定成本。',
    },
    expansions: {
      none: { label: '保持单店经营', cost: 0, userBonus: 0, gmvMultiplier: 1, riskDelta: 0, activationBonus: 0 },
      pilot: { label: '开设店中店试点', cost: 10000, userBonus: 160, gmvMultiplier: 1.08, riskDelta: 2, activationBonus: 0.02 },
      new: { label: '新增一家校园店', cost: 28000, userBonus: 400, gmvMultiplier: 1.18, riskDelta: 6, activationBonus: 0.05 },
    },
    roundEvents: [
      { title: '新学期开学', description: '新生和老生集中返校，门店客流明显增加。', growth: 1.12, demand: 1.1, revenue: 1.02, cost: 1.02, satisfaction: 0, risk: 0 },
      { title: '周边咖啡店降价', description: '竞争门店推出低价套餐，部分顾客开始比价。', growth: .92, demand: .96, revenue: .97, cost: 1, satisfaction: -.5, risk: 3 },
      { title: '原料和人工成本上涨', description: '供应链价格上涨，门店利润空间被压缩。', growth: 1, demand: 1, revenue: .98, cost: 1.08, satisfaction: -1, risk: 2 },
      { title: '校园文化节带来客流', description: '学校举办大型活动，门店获得额外曝光和订单。', growth: 1.1, demand: 1.15, revenue: 1.04, cost: 1.04, satisfaction: 0, risk: 1 },
    ],
    competitors: [
      { id: 'ai-chenguang', name: '晨光咖啡', persona: 'experience', cash: 82000, users: 2500, activeUsers: 1500, gmv: 128000, satisfaction: 79, risk: 14 },
      { id: 'ai-shiguang', name: '巷口咖啡', persona: 'growth', cash: 70000, users: 2900, activeUsers: 1700, gmv: 140000, satisfaction: 70, risk: 26 },
    ],
    economics: {
      userGrowthDivisor: 130,
      incentiveUserFactor: 28,
      marketUserFactor: 18,
      activationBase: 0.44,
      serviceActivation: 70000,
      satisfactionActivation: 350,
      commissionActivation: 5,
      spendBase: 52,
      satisfactionSpend: 0.32,
      roundSpend: 1.8,
      serviceRevenuePerUser: 3,
      adRevenuePerUser: 1.5,
      fixedCost: 4200,
      satisfactionBase: 1.8,
      serviceSatisfaction: 2200,
      incentiveSatisfaction: 0.8,
      innovationSatisfaction: 2800,
      rateSatisfactionPenalty: 0.015,
      rateRiskFactor: 0.015,
      serviceRiskDivisor: 3000,
      innovationGrowthDivisor: 260,
    },
    evaluations: [
      { code: 'cost_control', label: '成本控制', metric: 'profit' },
      { code: 'store_operations', label: '门店运营', metric: 'growth' },
      { code: 'customer_experience', label: '顾客体验', metric: 'satisfaction' },
      { code: 'risk_control', label: '风险控制', metric: 'risk' },
    ],
  },
  {
    id: 'career-service',
    name: '校园就业服务平台经营沙盘',
    title: '校园就业服务平台',
    companyName: '职达校园',
    description: '经营面向学生和企业的就业服务平台，在客户规模、服务质量、合作成本和经营风险之间决策。',
    rounds: TOTAL_ROUNDS,
    roundBudget: 32000,
    initial: {
      cash: 120000,
      users: 1800,
      activeUsers: 960,
      gmv: 90000,
      revenue: 38000,
      cost: 28000,
      profit: 10000,
      satisfaction: 71,
      risk: 20,
    },
    labels: {
      cash: '项目现金',
      users: '服务用户',
      gmv: '服务成交额',
      satisfaction: '用户满意度',
      risk: '经营风险',
      cashNote: '项目可支配资金',
      usersNote: '学生与企业用户规模',
      gmvNote: '岗位与服务交易额',
      satisfactionNote: '学生与企业体验',
      riskNote: '数值越低越稳定',
      revenueNote: '四轮服务收入',
      costNote: '推广、服务和合作',
      profitNote: '收入减去成本',
      finalUsersNote: '最终服务用户规模',
    },
    decisionConfig: {
      marketing: { label: '平台推广', hint: '提高推广可以带来更多学生和企业用户。', min: 0, max: 16000, step: 1000, default: 10000, format: 'money' },
      service: { label: '服务保障', hint: '增加服务保障可以提升岗位质量与用户满意度。', min: 0, max: 16000, step: 1000, default: 7000, format: 'money' },
      innovation: { label: '产品与岗位研发', hint: '开发新工具和岗位服务，提升长期竞争力。', min: 0, max: 14000, step: 1000, default: 5000, format: 'money' },
      incentive: { label: '企业合作补贴', hint: '补贴企业可以增加岗位和合作，但会增加服务成本。', min: 0, max: 10, step: 0.5, default: 3.5, format: 'percent' },
      rate: { label: '服务费率', hint: '提高服务费率会增加平台收入，但可能降低企业合作意愿。', min: 5, max: 20, step: 0.5, default: 10, format: 'percent' },
      expansionLabel: '服务区域扩张',
      expansionHint: '扩展高校或城市会增加投入与合作风险。',
    },
    expansions: {
      none: { label: '聚焦当前区域', cost: 0, userBonus: 0, gmvMultiplier: 1, riskDelta: 0, activationBonus: 0 },
      pilot: { label: '试点新高校', cost: 15000, userBonus: 220, gmvMultiplier: 1.08, riskDelta: 2, activationBonus: 0.02 },
      new: { label: '新增城市服务点', cost: 36000, userBonus: 520, gmvMultiplier: 1.16, riskDelta: 6, activationBonus: 0.04 },
    },
    roundEvents: [
      { title: '秋招季正式启动', description: '学生求职和企业招聘需求集中释放，平台访问量增加。', growth: 1.1, demand: 1.1, revenue: 1.02, cost: 1.02, satisfaction: 0, risk: 0 },
      { title: '企业岗位质量投诉增加', description: '部分岗位信息不准确，学生用户开始质疑平台审核能力。', growth: .94, demand: .96, revenue: .98, cost: 1.02, satisfaction: -3, risk: 5 },
      { title: '同类就业平台进入校园', description: '新的就业平台开始争夺学生和企业合作资源。', growth: .92, demand: .96, revenue: .98, cost: 1.02, satisfaction: -1, risk: 3 },
      { title: '新高校提出合作意向', description: '平台有机会进入新高校，但需要提前投入运营和团队资源。', growth: 1.12, demand: 1.08, revenue: 1.03, cost: 1.05, satisfaction: 0, risk: 2 },
    ],
    competitors: [
      { id: 'ai-zhichetong', name: '职通车', persona: 'growth', cash: 108000, users: 2100, activeUsers: 1100, gmv: 105000, satisfaction: 69, risk: 30 },
      { id: 'ai-qingning', name: '青柠就业', persona: 'experience', cash: 115000, users: 1700, activeUsers: 920, gmv: 98000, satisfaction: 78, risk: 17 },
    ],
    economics: {
      userGrowthDivisor: 145,
      incentiveUserFactor: 28,
      marketUserFactor: 18,
      activationBase: 0.36,
      serviceActivation: 75000,
      satisfactionActivation: 380,
      commissionActivation: 9,
      spendBase: 72,
      satisfactionSpend: 0.38,
      roundSpend: 2,
      serviceRevenuePerUser: 11,
      adRevenuePerUser: 2.5,
      fixedCost: 4000,
      satisfactionBase: 1.4,
      serviceSatisfaction: 2400,
      incentiveSatisfaction: 0.75,
      innovationSatisfaction: 3000,
      rateSatisfactionPenalty: 0.08,
      rateRiskFactor: 0.08,
      serviceRiskDivisor: 3200,
      innovationGrowthDivisor: 250,
    },
    evaluations: [
      { code: 'business_analysis', label: '商业分析', metric: 'profit' },
      { code: 'client_growth', label: '客户增长', metric: 'growth' },
      { code: 'service_operations', label: '服务运营', metric: 'operations' },
      { code: 'risk_control', label: '风险控制', metric: 'risk' },
    ],
  },
]

export const BUSINESS_SANDBOX_SCENARIO = BUSINESS_SANDBOX_SCENARIOS[0]

function clamp(value, min, max) {
  return Math.min(max, Math.max(min, Number(value) || 0))
}

function roundMoney(value) {
  return Math.round(Number(value) || 0)
}

function roundPercent(value) {
  return Math.round((Number(value) || 0) * 10) / 10
}

function hashText(text) {
  let hash = 0
  for (let index = 0; index < text.length; index += 1) {
    hash = (hash * 31 + text.charCodeAt(index)) >>> 0
  }
  return hash
}

function marketFactor(sessionId, round) {
  const seed = hashText(`${sessionId}-${round}`) % 21
  return 0.9 + seed / 100
}

function createSessionId() {
  return `sandbox-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

function clone(value) {
  return JSON.parse(JSON.stringify(value))
}

function readStore() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    const parsed = raw ? JSON.parse(raw) : {}
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

function writeStore(store) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(store))
    return true
  } catch {
    return false
  }
}

export function getSandboxScenario(scenarioId) {
  return BUSINESS_SANDBOX_SCENARIOS.find((item) => item.id === scenarioId) || BUSINESS_SANDBOX_SCENARIO
}

export function getSandboxStrategies() {
  return clone(SANDBOX_STRATEGIES)
}

export function getSandboxRoundEvent(session) {
  if (!session) return null
  const scenario = getSandboxScenario(session.scenarioId)
  const index = clamp(Number(session.round || 1) - 1, 0, scenario.roundEvents.length - 1)
  return { round: Number(session.round || 1), ...scenario.roundEvents[index] }
}

function createCompetitors(scenario) {
  return scenario.competitors.map((item) => ({
    ...item,
    personaLabel: {
      growth: '激进增长型',
      steady: '稳健运营型',
      experience: '体验优先型',
      innovation: '产品创新型',
    }[item.persona] || '稳健运营型',
    revenue: item.gmv * (scenario.id === 'campus-coffee' ? .42 : .12),
    cost: item.gmv * (scenario.id === 'campus-coffee' ? .35 : .1),
    profit: item.gmv * (scenario.id === 'campus-coffee' ? .07 : .02),
    latestProfit: 0,
    score: 0,
  }))
}

export function createSandboxSession(scenarioId = BUSINESS_SANDBOX_SCENARIO.id) {
  const scenario = getSandboxScenario(scenarioId)
  const now = new Date().toISOString()
  return {
    id: createSessionId(),
    scenarioId: scenario.id,
    name: scenario.title,
    companyName: scenario.companyName,
    status: 'in_progress',
    round: 1,
    createdAt: now,
    updatedAt: now,
    completedAt: '',
    ...clone(scenario.initial),
    competitors: createCompetitors(scenario),
    ranking: [],
    history: [],
    report: null,
  }
}

export function listSandboxSessions() {
  const store = readStore()
  return Object.values(store)
    .filter((item) => item && BUSINESS_SANDBOX_SCENARIOS.some((scenario) => scenario.id === item.scenarioId))
    .sort((left, right) => String(right.updatedAt || '').localeCompare(String(left.updatedAt || '')))
}

export function getSandboxSession(sessionId) {
  if (!sessionId) return null
  const session = readStore()[sessionId]
  return session ? clone(session) : null
}

export function saveSandboxSession(session) {
  if (!session?.id) return false
  const store = readStore()
  store[session.id] = clone({
    ...session,
    updatedAt: new Date().toISOString(),
  })
  return writeStore(store)
}

export function deleteSandboxSession(sessionId) {
  const store = readStore()
  if (!Object.prototype.hasOwnProperty.call(store, sessionId)) return false
  delete store[sessionId]
  return writeStore(store)
}

export function getExpansionOptions(scenarioId) {
  const scenario = getSandboxScenario(scenarioId)
  return Object.entries(scenario.expansions).map(([id, option]) => ({ id, ...option }))
}

function companyScore(company) {
  return Number(company.cash || 0)
    + Number(company.users || 0) * 30
    + Number(company.satisfaction || 0) * 700
    - Number(company.risk || 0) * 500
    + Math.max(0, Number(company.latestProfit ?? company.profit ?? 0)) * 1.5
}

function rankCompanies(userCompany, competitors) {
  return [userCompany, ...competitors]
    .map((item) => ({ ...item, score: Math.round(companyScore(item)) }))
    .sort((left, right) => right.score - left.score)
    .map((item, index) => ({ ...item, rank: index + 1 }))
}

function userCompanySnapshot(session, scenario, ranking = []) {
  return {
    id: 'user-company',
    name: session.companyName || scenario.companyName,
    personaLabel: '你的公司',
    isUser: true,
    cash: session.cash,
    users: session.users,
    activeUsers: session.activeUsers,
    gmv: session.gmv,
    revenue: session.revenue,
    cost: session.cost,
    profit: session.profit,
    latestProfit: session.profit,
    satisfaction: session.satisfaction,
    risk: session.risk,
    rank: ranking.find((item) => item.isUser)?.rank || 1,
  }
}

function updateCompetitors(session, scenario, event, factor, strategy) {
  return (session.competitors || createCompetitors(scenario)).map((competitor) => {
    const person = {
      growth: { growth: 1.25, cost: 1.05, satisfaction: -.8, risk: 3, demand: 1.04 },
      steady: { growth: .92, cost: .92, satisfaction: .5, risk: -2, demand: 1 },
      experience: { growth: 1, cost: 1.02, satisfaction: 2, risk: -1, demand: 1.03 },
      innovation: { growth: 1.08, cost: 1.06, satisfaction: .8, risk: 1, demand: 1.06 },
    }[competitor.persona] || { growth: 1, cost: 1, satisfaction: 0, risk: 0, demand: 1 }
    const economics = scenario.economics
    const users = clamp(Number(competitor.users || 0) + (70 + Number(session.round || 1) * 12) * person.growth * event.growth, 100, 300000)
    const satisfactionBase = clamp(Number(competitor.satisfaction || 0) + person.satisfaction + event.satisfaction, 0, 100)
    const gmv = users * (Number(economics.spendBase || 60) * .8 + satisfactionBase * .25)
      * factor * event.demand * person.demand
    const rate = scenario.decisionConfig.rate.default
    const revenue = gmv * rate / 100 + users * Number(economics.serviceRevenuePerUser || 5) * .7
    const cost = (Number(economics.fixedCost || 3000) * .9 + 9000 * Number(person.cost || 1)
      + gmv * .018) * event.cost
    const latestProfit = revenue - cost
    const cash = Number(competitor.cash || 0) + latestProfit
    const risk = clamp(Number(competitor.risk || 0) + person.risk + event.risk - 1, 0, 100)
    const next = {
      ...competitor,
      users: Math.round(users),
      activeUsers: Math.round(users * clamp(0.42 + satisfactionBase / 400, .25, .8)),
      gmv: roundMoney(gmv),
      revenue: roundMoney(revenue),
      cost: roundMoney(cost),
      profit: roundMoney(latestProfit),
      latestProfit: roundMoney(latestProfit),
      cash: roundMoney(cash),
      satisfaction: roundPercent(satisfactionBase),
      risk: roundPercent(risk),
    }
    next.score = Math.round(companyScore(next))
    return next
  })
}

function buildInsight(previous, next, decision, scenario, event, strategy, ranking) {
  const insights = [`本轮市场事件：${event.title}`, `经营策略：${strategy.name}`]
  if (decision.marketingBudget >= scenario.roundBudget * .4) insights.push('推广预算较高，用户增长明显，但现金压力同步上升')
  if (decision.serviceBudget >= scenario.roundBudget * .3) insights.push('服务投入充足，用户满意度和经营风险得到改善')
  if (decision.innovationBudget >= scenario.roundBudget * .25) insights.push('研发投入开始形成长期竞争力，对本期利润有一定压力')
  if (next.profit < 0) insights.push('本轮出现经营亏损，需要重新平衡预算和收入')
  if (next.satisfaction < previous.satisfaction) insights.push('用户满意度下降，下一轮应增加服务投入或降低费率')
  if (next.risk >= 60) insights.push('经营风险偏高，建议控制扩张速度并增加治理投入')
  const userRank = ranking.find((item) => item.isUser)?.rank || 1
  insights.push(userRank === 1 ? '本轮综合排名第 1，保持当前优势' : `本轮综合排名第 ${userRank}，需要关注领先公司的增长策略`)
  return insights
}

export function calculateSandboxRound(currentSession, decisionInput) {
  if (!currentSession || currentSession.status !== 'in_progress') {
    throw new Error('当前经营已经结束，无法继续结算')
  }
  const scenario = getSandboxScenario(currentSession.scenarioId)
  const config = scenario.decisionConfig
  const economics = scenario.economics
  const strategy = SANDBOX_STRATEGIES.find((item) => item.id === decisionInput?.strategyId) || SANDBOX_STRATEGIES[1]
  const event = getSandboxRoundEvent(currentSession)
  const decision = {
    strategyId: strategy.id,
    marketingBudget: clamp(decisionInput?.marketingBudget, config.marketing.min, config.marketing.max),
    serviceBudget: clamp(decisionInput?.serviceBudget, config.service.min, config.service.max),
    innovationBudget: clamp(decisionInput?.innovationBudget, config.innovation.min, config.innovation.max),
    sellerIncentive: clamp(decisionInput?.sellerIncentive, config.incentive.min, config.incentive.max),
    commissionRate: clamp(decisionInput?.commissionRate, config.rate.min, config.rate.max),
    expansion: scenario.expansions[decisionInput?.expansion] ? decisionInput.expansion : 'none',
  }
  const expansion = scenario.expansions[decision.expansion]
  const previous = {
    cash: Number(currentSession.cash) || 0,
    users: Number(currentSession.users) || 0,
    activeUsers: Number(currentSession.activeUsers) || 0,
    gmv: Number(currentSession.gmv) || 0,
    revenue: Number(currentSession.revenue) || 0,
    cost: Number(currentSession.cost) || 0,
    profit: Number(currentSession.profit) || 0,
    satisfaction: Number(currentSession.satisfaction) || 0,
    risk: Number(currentSession.risk) || 0,
  }
  const factor = marketFactor(currentSession.id, currentSession.round)
  const userGrowth = (decision.marketingBudget / economics.userGrowthDivisor
    + decision.innovationBudget / economics.innovationGrowthDivisor
    + decision.sellerIncentive * economics.incentiveUserFactor
    + expansion.userBonus + factor * economics.marketUserFactor)
    * event.growth * strategy.effects.growth
  const users = clamp(previous.users + userGrowth, 100, 300000)
  const activationRate = clamp(
    economics.activationBase
      + decision.serviceBudget / economics.serviceActivation
      + previous.satisfaction / economics.satisfactionActivation
      - decision.commissionRate / economics.commissionActivation
      + expansion.activationBonus,
    0.25,
    0.85,
  )
  const activeUsers = clamp(users * activationRate, 100, users)
  const spendPerUser = economics.spendBase
    + previous.satisfaction * economics.satisfactionSpend
    + (currentSession.round - 1) * economics.roundSpend
  const gmv = activeUsers * spendPerUser * factor * expansion.gmvMultiplier * event.demand * strategy.effects.demand
  const revenue = (gmv * decision.commissionRate / 100
    + activeUsers * economics.serviceRevenuePerUser
    + users * economics.adRevenuePerUser) * event.revenue * strategy.effects.revenue
  const sellerCost = gmv * decision.sellerIncentive / 100
  const cost = (economics.fixedCost + expansion.cost + decision.marketingBudget
    + decision.serviceBudget + decision.innovationBudget + sellerCost) * event.cost * strategy.effects.cost
  const profit = revenue - cost
  const cash = previous.cash + profit
  const satisfactionChange = economics.satisfactionBase
    + decision.serviceBudget / economics.serviceSatisfaction
    + decision.innovationBudget / economics.innovationSatisfaction
    + decision.sellerIncentive * economics.incentiveSatisfaction
    - decision.commissionRate * economics.rateSatisfactionPenalty
    - expansion.riskDelta * 0.45
    + event.satisfaction
    + strategy.effects.satisfaction
    - (profit < -20000 ? 3 : 0)
  const satisfaction = clamp(previous.satisfaction + satisfactionChange, 0, 100)
  const risk = clamp(
    previous.risk
      + decision.commissionRate * economics.rateRiskFactor
      + expansion.riskDelta
      + event.risk
      + strategy.effects.risk
      - decision.serviceBudget / economics.serviceRiskDivisor
      - Math.max(0, satisfaction - previous.satisfaction) * 0.2,
    0,
    100,
  )
  const next = {
    cash: roundMoney(cash),
    users: Math.round(users),
    activeUsers: Math.round(activeUsers),
    gmv: roundMoney(gmv),
    revenue: roundMoney(revenue),
    cost: roundMoney(cost),
    profit: roundMoney(profit),
    satisfaction: roundPercent(satisfaction),
    risk: roundPercent(risk),
  }
  const competitors = updateCompetitors(currentSession, scenario, event, factor, strategy)
  const userCompany = userCompanySnapshot({ ...currentSession, ...next, companyName: currentSession.companyName }, scenario)
  const ranking = rankCompanies(userCompany, competitors)
  const roundResult = {
    round: currentSession.round,
    marketFactor: Math.round(factor * 100) / 100,
    event,
    strategy,
    decision,
    before: previous,
    after: next,
    delta: {
      cash: next.cash - previous.cash,
      users: next.users - previous.users,
      activeUsers: next.activeUsers - previous.activeUsers,
      gmv: next.gmv - previous.gmv,
      profit: next.profit - previous.profit,
      satisfaction: roundPercent(next.satisfaction - previous.satisfaction),
      risk: roundPercent(next.risk - previous.risk),
    },
    ranking,
    insight: buildInsight(previous, next, decision, scenario, event, strategy, ranking),
  }

  return {
    ...currentSession,
    ...next,
    competitors,
    ranking,
    round: currentSession.round + 1,
    history: [...(currentSession.history || []), roundResult],
    roundResult,
  }
}

function buildReport(session) {
  const scenario = getSandboxScenario(session.scenarioId)
  const history = Array.isArray(session.history) ? session.history : []
  const totalProfit = roundMoney(session.cash - scenario.initial.cash)
  const userGrowth = Math.max(0, Number(session.users) - scenario.initial.users)
  const profitScore = clamp(52 + totalProfit / 1500, 0, 100)
  const growthScore = clamp(userGrowth / 9, 0, 100)
  const satisfactionScore = clamp(session.satisfaction, 0, 100)
  const riskScore = clamp(100 - session.risk, 0, 100)
  const score = Math.round(profitScore * 0.35 + growthScore * 0.25 + satisfactionScore * 0.2 + riskScore * 0.2)
  const grade = score >= 85 ? '优秀' : score >= 70 ? '良好' : score >= 55 ? '合格' : '待改进'
  const scoreMap = {
    profit: profitScore,
    growth: growthScore,
    operations: (growthScore + satisfactionScore) / 2,
    satisfaction: satisfactionScore,
    risk: riskScore,
  }
  const summaryMap = {
    business_analysis: totalProfit >= 0 ? '能够关注成本与收益变化' : '需要加强盈亏平衡判断',
    user_growth: userGrowth > 400 ? '增长策略产生了明显效果' : '用户增长仍有提升空间',
    operations_decision: '综合了增长表现和服务体验',
    risk_control: session.risk < 45 ? '经营风险总体可控' : '需要降低扩张和治理风险',
    cost_control: totalProfit >= 0 ? '成本与收入保持了较好平衡' : '需要进一步压缩固定成本',
    store_operations: userGrowth > 300 ? '门店运营带来稳定客流增长' : '门店运营仍有优化空间',
    customer_experience: satisfactionScore >= 75 ? '顾客体验保持在良好水平' : '需要关注口味、服务和等待时间',
    client_growth: userGrowth > 300 ? '学生与企业客户规模持续增长' : '客户增长还需要加强渠道运营',
    service_operations: '综合了服务规模、合作质量和用户体验',
  }
  const evaluations = scenario.evaluations.map((item) => ({
    code: item.code,
    label: item.label,
    value: Math.round(scoreMap[item.metric] || 0),
    summary: summaryMap[item.code] || '已生成项目实训证据',
  }))
  const totalRevenue = history.reduce((sum, item) => sum + Number(item.after?.revenue || 0), 0)
  const totalCost = history.reduce((sum, item) => sum + Number(item.after?.cost || 0), 0)
  const userCompany = userCompanySnapshot(session, scenario)
  const ranking = rankCompanies(userCompany, session.competitors || [])
  return {
    scenarioName: scenario.name,
    rounds: history.length,
    totalRevenue,
    totalCost,
    totalProfit,
    finalCash: session.cash,
    finalUsers: session.users,
    finalSatisfaction: session.satisfaction,
    finalRisk: session.risk,
    score,
    grade,
    ranking,
    evaluations,
    history: clone(history),
    generatedAt: new Date().toISOString(),
  }
}

export function completeSandboxSession(session) {
  if (!session || session.status !== 'in_progress') {
    throw new Error('当前经营无法生成报告')
  }
  const completed = {
    ...session,
    status: 'completed',
    completedAt: new Date().toISOString(),
    report: null,
  }
  completed.report = buildReport(completed)
  return completed
}

export function formatSandboxMoney(value) {
  const number = Number(value) || 0
  const sign = number < 0 ? '-' : ''
  return `${sign}¥${Math.abs(Math.round(number)).toLocaleString('zh-CN')}`
}

export function formatSandboxPercent(value, digits = 1) {
  return `${(Number(value) || 0).toFixed(digits)}%`
}
