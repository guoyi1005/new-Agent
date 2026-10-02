import { calculateSandboxRound, getSandboxScenario, SANDBOX_STRATEGIES } from './businessSandbox.js'

const CANDIDATE_PROFILES = [
  { id: 'balanced', name: '稳健平衡方案', strategyId: 'steady', shares: [35, 30, 20], expansion: 'none', rate: 'mid', incentive: 'mid' },
  { id: 'growth', name: '增长优先方案', strategyId: 'growth', shares: [50, 20, 15], expansion: 'pilot', rate: 'low', incentive: 'high' },
  { id: 'experience', name: '体验优先方案', strategyId: 'experience', shares: [20, 45, 20], expansion: 'none', rate: 'low', incentive: 'high' },
  { id: 'profit', name: '利润优先方案', strategyId: 'steady', shares: [15, 10, 5], expansion: 'none', rate: 'high', incentive: 'low' },
  { id: 'innovation', name: '产品创新方案', strategyId: 'innovation', shares: [20, 20, 40], expansion: 'pilot', rate: 'mid', incentive: 'mid' },
  { id: 'safe', name: '风险控制方案', strategyId: 'experience', shares: [10, 45, 20], expansion: 'none', rate: 'low', incentive: 'low' },
]

const clamp = (value, min, max) => Math.min(max, Math.max(min, value))

function roundToStep(value, min, max, step) {
  const safe = clamp(value, min, max)
  return clamp(min + Math.round((safe - min) / step) * step, min, max)
}

function rangeValue(config, type) {
  if (type === 'low') return config.min
  if (type === 'high') return config.max
  return config.default
}

function buildCandidateDecision(scenario, profile) {
  const config = scenario.decisionConfig
  const budget = Number(scenario.roundBudget || 30000)
  const [marketingShare, serviceShare, innovationShare] = profile.shares
  return {
    strategyId: profile.strategyId,
    marketingBudget: roundToStep(budget * marketingShare / 100, config.marketing.min, config.marketing.max, config.marketing.step),
    serviceBudget: roundToStep(budget * serviceShare / 100, config.service.min, config.service.max, config.service.step),
    innovationBudget: roundToStep(budget * innovationShare / 100, config.innovation.min, config.innovation.max, config.innovation.step),
    sellerIncentive: roundToStep(rangeValue(config.incentive, profile.incentive), config.incentive.min, config.incentive.max, config.incentive.step),
    commissionRate: roundToStep(rangeValue(config.rate, profile.rate), config.rate.min, config.rate.max, config.rate.step),
    expansion: profile.expansion,
  }
}

function baseSessionForRound(session, roundItem) {
  return {
    ...session,
    ...roundItem.before,
    status: 'in_progress',
    round: roundItem.round,
    history: [],
    competitors: [],
    ranking: [],
    report: null,
  }
}

function objectiveScore(result) {
  const after = result.after || {}
  const delta = result.delta || {}
  return Number(after.profit || 0)
    + Number(delta.users || 0) * 45
    + Number(after.satisfaction || 0) * 220
    - Number(after.risk || 0) * 180
}

function buildReasons(actual, best, roundItem, scenario) {
  const current = actual.decision || {}
  const target = best.decision || {}
  const budget = Number(scenario.roundBudget || 30000)
  const reasons = []

  if (Number(current.marketingBudget || 0) - Number(target.marketingBudget || 0) > budget * 0.08) {
    reasons.push('推广投入高于更优方案，容易带来增长，但会挤压利润和服务预算。')
  }
  if (Number(target.serviceBudget || 0) - Number(current.serviceBudget || 0) > budget * 0.08) {
    reasons.push('服务与治理投入不足，用户满意度下降后会影响后续活跃和复购。')
  }
  if (Number(target.innovationBudget || 0) - Number(current.innovationBudget || 0) > budget * 0.08) {
    reasons.push('产品研发投入不足，当期看似节省成本，但长期竞争力会受到影响。')
  }
  if (current.expansion && current.expansion !== 'none' && target.expansion === 'none') {
    reasons.push('本轮扩张时机偏早，扩张成本和新区域风险抵消了部分增长收益。')
  }
  if (Number(current.commissionRate || 0) > Number(target.commissionRate || 0) + 1) {
    reasons.push('费率或价格策略高于更优方案，短期收入增加，但会抑制用户体验和活跃。')
  }
  if (Number(current.sellerIncentive || 0) > Number(target.sellerIncentive || 0) + 1) {
    reasons.push('合作激励投入偏高，交易规模增加的同时，单笔收益被明显压缩。')
  }
  if (Number(actual.after?.risk || 0) > Number(best.after?.risk || 0) + 5) {
    reasons.push('本轮风险上升速度快于更优方案，后续需要更多治理成本来恢复稳定。')
  }
  if (!reasons.length) {
    reasons.push(`在“${roundItem.event?.title || '本轮事件'}”影响下，你的组合没有出现明显结构性错误，主要差距来自预算优先级。`)
  }
  const profitGap = Number(best.after?.profit || 0) - Number(actual.after?.profit || 0)
  const userGap = Number(best.delta?.users || 0) - Number(actual.delta?.users || 0)
  reasons.push(`候选回放显示：利润相差 ${profitGap >= 0 ? '+' : ''}${Math.round(profitGap)}，用户增长相差 ${userGap >= 0 ? '+' : ''}${Math.round(userGap)}。`)
  return reasons
}

function buildRoundSummary(actual, best, roundItem) {
  const actualStrategy = actual.strategy?.name || actual.decision?.strategyId || '实际策略'
  const bestStrategy = best.strategy?.name || best.name
  const profitGap = Number(best.after?.profit || 0) - Number(actual.after?.profit || 0)
  return `第 ${roundItem.round} 轮你采用“${actualStrategy}”，候选回放中“${bestStrategy}”综合表现更好，预计可多获得 ${Math.round(profitGap)} 的当期利润。`
}

export function buildSandboxProcessAnalysis(session) {
  if (!session?.scenarioId || !Array.isArray(session.history)) return []
  const scenario = getSandboxScenario(session.scenarioId)

  return session.history.map((roundItem) => {
    const base = baseSessionForRound(session, roundItem)
    const candidates = CANDIDATE_PROFILES.map((profile) => {
      const decision = buildCandidateDecision(scenario, profile)
      const simulated = calculateSandboxRound(base, decision)
      const result = simulated.roundResult
      return {
        id: profile.id,
        name: profile.name,
        strategyName: result.strategy.name,
        decision: result.decision,
        after: result.after,
        delta: result.delta,
        objective: objectiveScore(result),
      }
    })
    const bestOverall = [...candidates].sort((a, b) => b.objective - a.objective)[0]
    const profitBest = [...candidates].sort((a, b) => Number(b.after.profit) - Number(a.after.profit))[0]
    const growthBest = [...candidates].sort((a, b) => Number(b.delta.users) - Number(a.delta.users))[0]
    const riskBest = [...candidates].sort((a, b) => Number(a.after.risk) - Number(b.after.risk))[0]
    const actual = {
      strategy: roundItem.strategy,
      decision: roundItem.decision,
      after: roundItem.after,
      delta: roundItem.delta,
      objective: objectiveScore(roundItem),
    }
    return {
      round: roundItem.round,
      event: roundItem.event,
      actual,
      bestOverall,
      profitBest,
      growthBest,
      riskBest,
      scoreGap: Math.round(bestOverall.objective - actual.objective),
      summary: buildRoundSummary(actual, bestOverall, roundItem),
      reasons: buildReasons(actual, bestOverall, roundItem, scenario),
    }
  })
}