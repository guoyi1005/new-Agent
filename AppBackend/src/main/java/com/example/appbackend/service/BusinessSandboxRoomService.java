package com.example.appbackend.service;

import com.example.appbackend.dto.BusinessSandboxRoomDTO;
import com.example.appbackend.entity.BusinessSandboxRoom;
import com.example.appbackend.entity.Result;
import com.example.appbackend.exception.BusinessException;
import com.example.appbackend.repository.BusinessSandboxRoomRepository;
import com.example.appbackend.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
public class BusinessSandboxRoomService {
    private static final List<String> ROLES = List.of("PRESIDENT", "MARKET", "OPERATIONS", "FINANCE", "PRODUCT");
    private static final Set<String> SCENARIOS = Set.of("campus-marketplace", "campus-coffee", "career-service", "campus-convenience", "campus-fitness", "campus-delivery", "campus-study-room", "campus-pet-service", "campus-digital-repair", "campus-travel");
    private final BusinessSandboxRoomRepository repository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public BusinessSandboxRoomService(BusinessSandboxRoomRepository repository, UserRepository userRepository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, Object> createRoom(Long userId, BusinessSandboxRoomDTO.CreateRoomRequest request) {
        String scenarioId = StringUtils.hasText(request == null ? null : request.scenarioId()) ? request.scenarioId() : "campus-marketplace";
        if (!SCENARIOS.contains(scenarioId)) throw new BusinessException(Result.BAD_REQUEST_CODE, "不支持的沙盘场景");
        BusinessSandboxRoom room = new BusinessSandboxRoom();
        room.setRoomCode(nextRoomCode());
        room.setHostUserId(userId);
        room.setScenarioId(scenarioId);
        String requestedName = request == null ? null : request.name();
        room.setName(StringUtils.hasText(requestedName) ? requestedName.trim() : "多人经营房间");
        room.setMaxCompanies(clamp(optional(request == null ? null : request.maxCompanies(), 4), 2, 4));
        room.setMaxMembers(clamp(optional(request == null ? null : request.maxMembers(), 5), 3, 5));
        room.setStateJson(write(initialState()));
        repository.save(room);
        return roomView(room, userId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listMyRooms(Long userId) {
        return repository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(room -> isParticipant(room, userId))
                .map(room -> roomView(room, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRoom(Long roomId, Long userId) {
        BusinessSandboxRoom room = requireRoom(roomId);
        return roomView(room, userId);
    }

    @Transactional
    public Map<String, Object> createCompany(Long roomId, Long userId, BusinessSandboxRoomDTO.CreateCompanyRequest request) {
        BusinessSandboxRoom room = requireRoom(roomId);
        requireWaiting(room);
        Map<String, Object> state = readState(room);
        List<Map<String, Object>> companies = companies(state);
        if (companies.size() >= room.getMaxCompanies()) throw new BusinessException(Result.BAD_REQUEST_CODE, "公司数量已达到房间上限");
        if (findCompanyByUser(state, userId) != null) throw new BusinessException(Result.BAD_REQUEST_CODE, "你已经加入了本房间的一家公司");
        if (!StringUtils.hasText(request == null ? null : request.companyName())) throw new BusinessException(Result.BAD_REQUEST_CODE, "公司名称不能为空");
        Map<String, Object> company = newCompany(request.companyName().trim(), userId);
        companies.add(company);
        saveState(room, state);
        return roomView(room, userId);
    }

    @Transactional
    public Map<String, Object> joinCompany(Long roomId, Long userId, BusinessSandboxRoomDTO.JoinCompanyRequest request) {
        BusinessSandboxRoom room = requireRoom(roomId);
        requireWaiting(room);
        Map<String, Object> state = readState(room);
        if (findCompanyByUser(state, userId) != null) throw new BusinessException(Result.BAD_REQUEST_CODE, "你已经加入了本房间的一家公司");
        Map<String, Object> company = requireCompany(state, request == null ? null : request.companyId());
        List<Map<String, Object>> members = asList(company.get("members"));
        if (members.size() >= room.getMaxMembers()) throw new BusinessException(Result.BAD_REQUEST_CODE, "该公司成员已满");
        String role = normalizeRole(request.role());
        if (members.stream().anyMatch(member -> role.equals(member.get("role")))) throw new BusinessException(Result.BAD_REQUEST_CODE, "该角色已有人担任");
        Map<String, Object> member = member(userId, role);
        members.add(member);
        saveState(room, state);
        return roomView(room, userId);
    }

    @Transactional
    public Map<String, Object> saveDraft(Long roomId, String companyId, Long userId, BusinessSandboxRoomDTO.DraftRequest request) {
        BusinessSandboxRoom room = requireRoom(roomId);
        if (!"IN_PROGRESS".equals(room.getStatus())) throw new BusinessException(Result.BAD_REQUEST_CODE, "房间尚未开始经营");
        Map<String, Object> state = readState(room);
        Map<String, Object> company = requireCompany(state, companyId);
        Map<String, Object> member = requireMember(company, userId);
        String role = normalizeRole(request == null ? null : request.role());
        if (!role.equals(member.get("role"))) throw new BusinessException(Result.FORBIDDEN_CODE, "只能提交自己负责的决策");
        if (request.decision() == null || request.decision().isEmpty()) throw new BusinessException(Result.BAD_REQUEST_CODE, "决策内容不能为空");
        Map<String, Object> drafts = asMap(company.computeIfAbsent("drafts", key -> new LinkedHashMap<>()));
        Map<String, Object> draft = new LinkedHashMap<>();
        draft.put("role", role);
        draft.put("decision", new LinkedHashMap<>(request.decision()));
        draft.put("submittedBy", userId);
        draft.put("submittedAt", new Date().toString());
        drafts.put(role, draft);
        company.put("confirmed", false);
        saveState(room, state);
        return roomView(room, userId);
    }

    @Transactional
    public Map<String, Object> confirmCompany(Long roomId, String companyId, Long userId, BusinessSandboxRoomDTO.ConfirmRequest request) {
        if (request != null && Boolean.FALSE.equals(request.confirmed())) throw new BusinessException(Result.BAD_REQUEST_CODE, "当前版本不支持撤回确认");
        BusinessSandboxRoom room = requireRoom(roomId);
        if (!"IN_PROGRESS".equals(room.getStatus())) throw new BusinessException(Result.BAD_REQUEST_CODE, "房间尚未开始经营");
        Map<String, Object> state = readState(room);
        Map<String, Object> company = requireCompany(state, companyId);
        if (!Objects.equals(asLong(company.get("captainUserId")), userId)) throw new BusinessException(Result.FORBIDDEN_CODE, "只有公司队长可以统一确认");
        Map<String, Object> drafts = asMap(company.get("drafts"));
        for (Object raw : asList(company.get("members"))) {
            Map<String, Object> member = asMap(raw);
            if (!drafts.containsKey(String.valueOf(member.get("role")))) {
                throw new BusinessException(Result.BAD_REQUEST_CODE, "仍有成员没有提交本轮决策");
            }
        }
        company.put("confirmed", true);
        company.put("confirmedAt", new Date().toString());
        saveState(room, state);
        return roomView(room, userId);
    }

    @Transactional
    public Map<String, Object> startRoom(Long roomId, Long userId) {
        BusinessSandboxRoom room = requireRoom(roomId);
        requireWaiting(room);
        if (!Objects.equals(room.getHostUserId(), userId)) throw new BusinessException(Result.FORBIDDEN_CODE, "只有房主可以开始经营");
        Map<String, Object> state = readState(room);
        List<Map<String, Object>> companies = companies(state);
        if (companies.size() < 2) throw new BusinessException(Result.BAD_REQUEST_CODE, "至少需要 2 家公司");
        for (Map<String, Object> company : companies) {
            int count = asList(company.get("members")).size();
            if (count < 3 || count > room.getMaxMembers()) throw new BusinessException(Result.BAD_REQUEST_CODE, "每家公司需要 3 至 " + room.getMaxMembers() + " 名成员");
            company.put("confirmed", false);
            company.put("drafts", new LinkedHashMap<>());
        }
        room.setStatus("IN_PROGRESS");
        room.setCurrentRound(1);
        state.put("lastRoundResult", null);
        saveState(room, state);
        return roomView(room, userId);
    }

    @Transactional
    public Map<String, Object> settleRoom(Long roomId, Long userId) {
        BusinessSandboxRoom room = requireRoom(roomId);
        if (!"IN_PROGRESS".equals(room.getStatus())) throw new BusinessException(Result.BAD_REQUEST_CODE, "房间当前不可结算");
        if (!Objects.equals(room.getHostUserId(), userId)) throw new BusinessException(Result.FORBIDDEN_CODE, "只有房主可以统一结算");
        Map<String, Object> state = readState(room);
        List<Map<String, Object>> companies = companies(state);
        if (companies.stream().anyMatch(company -> !Boolean.TRUE.equals(company.get("confirmed")))) throw new BusinessException(Result.BAD_REQUEST_CODE, "仍有公司没有确认本轮决策");
        settleRound(room, state, companies);
        if (room.getCurrentRound() >= room.getRoundCount()) {
            room.setStatus("COMPLETED");
        } else {
            room.setCurrentRound(room.getCurrentRound() + 1);
            for (Map<String, Object> company : companies) {
                company.put("confirmed", false);
                company.put("confirmedAt", null);
                company.put("drafts", new LinkedHashMap<>());
            }
        }
        saveState(room, state);
        return roomView(room, userId);
    }
    private void settleRound(BusinessSandboxRoom room, Map<String, Object> state, List<Map<String, Object>> companies) {
        double factor = 0.94 + ((Math.abs(Objects.hash(room.getId(), room.getCurrentRound())) % 13) / 100.0);
        Map<String, Object> event = roundEvent(room.getScenarioId(), room.getCurrentRound());
        List<Map<String, Object>> ranking = new ArrayList<>();
        for (Map<String, Object> company : companies) {
            Map<String, Object> decision = aggregateDecision(room.getScenarioId(), company);
            Map<String, Object> result = calculateCompany(company, decision, room.getCurrentRound(), factor, event, room.getScenarioId());
            result.put("companyId", company.get("id"));
            result.put("companyName", company.get("name"));
            result.put("isUser", false);
            company.put("lastResult", result);
            asList(company.computeIfAbsent("history", key -> new ArrayList<>())).add(result);
            ranking.add(result);
        }
        ranking.sort(Comparator.comparingDouble(item -> -asDouble(item.get("score"))));
        for (int i = 0; i < ranking.size(); i++) ranking.get(i).put("rank", i + 1);
        Map<String, Object> roundResult = new LinkedHashMap<>();
        roundResult.put("round", room.getCurrentRound());
        roundResult.put("event", event);
        roundResult.put("ranking", ranking);
        state.put("ranking", ranking);
        state.put("lastRoundResult", roundResult);
    }

    private Map<String, Object> calculateCompany(Map<String, Object> company, Map<String, Object> decision,
                                                  int round, double factor, Map<String, Object> event, String scenarioId) {
        double marketing = asDouble(decision.get("marketingBudget"));
        double service = asDouble(decision.get("serviceBudget"));
        double innovation = asDouble(decision.get("innovationBudget"));
        double rate = asDouble(decision.get("commissionRate"));
        double incentive = asDouble(decision.get("sellerIncentive"));
        String strategyId = String.valueOf(decision.getOrDefault("strategyId", "steady"));
        String expansion = String.valueOf(decision.getOrDefault("expansion", "none"));
        Map<String, Object> strategy = strategyEffects(strategyId);
        double expansionCost = expansionCost(expansion);
        double expansionRisk = "new".equals(expansion) ? 6 : "pilot".equals(expansion) ? 2 : 0;
        double eventGrowth = asDouble(event.get("growth"));
        double eventDemand = asDouble(event.get("demand"));
        double eventCost = asDouble(event.get("cost"));
        double eventSatisfaction = asDouble(event.get("satisfaction"));
        double eventRisk = asDouble(event.get("risk"));

        double previousCash = asDouble(company.get("cash"));
        double previousUsers = asDouble(company.get("users"));
        double previousSatisfaction = asDouble(company.get("satisfaction"));
        double previousRisk = asDouble(company.get("risk"));
        double users = Math.max(100, previousUsers + (marketing / 140.0 + innovation / 260.0 + incentive * 24 + factor * 18)
                * eventGrowth * asDouble(strategy.get("growth")));
        double activation = clamp(0.38 + service / 80000.0 + previousSatisfaction / 400.0 - rate / 100.0, 0.25, 0.82);
        double activeUsers = users * activation;
        double gmv = activeUsers * (80 + previousSatisfaction * 0.4) * factor * eventDemand * asDouble(strategy.get("demand"));
        double revenue = (gmv * rate / 100.0 + activeUsers * 9 + users * 2)
                * asDouble(strategy.get("revenue")) * asDouble(event.get("revenue"));
        double cost = (3500 + marketing + service + innovation + expansionCost + gmv * incentive / 100.0)
                * eventCost * asDouble(strategy.get("cost"));
        double profit = revenue - cost;
        double cash = previousCash + profit;
        double satisfaction = clamp(previousSatisfaction + 1.4 + service / 2500.0 + innovation / 3000.0
                + incentive * 0.7 - rate * 1.1 - expansionRisk * 0.4 + eventSatisfaction + asDouble(strategy.get("satisfaction")), 0, 100);
        double risk = clamp(previousRisk + rate * 1.2 + expansionRisk + eventRisk + asDouble(strategy.get("risk"))
                - service / 3500.0 - Math.max(0, satisfaction - previousSatisfaction) * 0.2, 0, 100);
        company.put("cash", round(cash));
        company.put("users", Math.round(users));
        company.put("activeUsers", Math.round(activeUsers));
        company.put("gmv", round(gmv));
        company.put("revenue", round(revenue));
        company.put("cost", round(cost));
        company.put("profit", round(profit));
        company.put("satisfaction", round1(satisfaction));
        company.put("risk", round1(risk));
        double score = cash + users * 30 + satisfaction * 700 - risk * 500 + Math.max(0, profit) * 1.5;
        company.put("score", Math.round(score));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("round", round);
        result.put("event", event);
        result.put("strategyId", strategyId);
        result.put("cash", company.get("cash"));
        result.put("users", company.get("users"));
        result.put("activeUsers", company.get("activeUsers"));
        result.put("gmv", company.get("gmv"));
        result.put("revenue", company.get("revenue"));
        result.put("cost", company.get("cost"));
        result.put("profit", company.get("profit"));
        result.put("satisfaction", company.get("satisfaction"));
        result.put("risk", company.get("risk"));
        result.put("score", company.get("score"));
        result.put("decision", decision);
        return result;
    }

    private Map<String, Object> aggregateDecision(String scenarioId, Map<String, Object> company) {
        Map<String, Object> decision = defaultDecision(scenarioId);
        Map<String, Object> drafts = asMap(company.get("drafts"));
        for (Map<String, Object> draft : drafts.values().stream().map(this::asMap).toList()) {
            decision.putAll(asMap(draft.get("decision")));
        }
        return decision;
    }

    private Map<String, Object> defaultDecision(String scenarioId) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("strategyId", "steady");
        value.put("marketingBudget", "campus-coffee".equals(scenarioId) ? 7000 : 9000);
        value.put("serviceBudget", 6000);
        value.put("innovationBudget", 5000);
        value.put("commissionRate", "career-service".equals(scenarioId) ? 10 : "campus-coffee".equals(scenarioId) ? 45 : 6);
        value.put("sellerIncentive", "career-service".equals(scenarioId) ? 3.5 : "campus-coffee".equals(scenarioId) ? 4 : 3);
        value.put("expansion", "none");
        return value;
    }

    private Map<String, Object> strategyEffects(String strategyId) {
        return switch (strategyId) {
            case "growth" -> Map.of("growth", 1.25, "demand", 1.06, "revenue", .98, "cost", 1.06, "satisfaction", -.6, "risk", 3);
            case "experience" -> Map.of("growth", 1.0, "demand", 1.04, "revenue", 1.01, "cost", 1.04, "satisfaction", 2.2, "risk", -1);
            case "innovation" -> Map.of("growth", 1.08, "demand", 1.1, "revenue", 1.05, "cost", 1.08, "satisfaction", .8, "risk", 1);
            default -> Map.of("growth", .9, "demand", 1.0, "revenue", 1.02, "cost", .92, "satisfaction", .5, "risk", -2);
        };
    }

    private double expansionCost(String expansion) {
        return "new".equals(expansion) ? 30000 : "pilot".equals(expansion) ? 12000 : 0;
    }

    private Map<String, Object> roundEvent(String scenarioId, int round) {
        List<Map<String, Object>> events = switch (scenarioId) {
            case "campus-coffee" -> List.of(
                    roundEvent("新学期开学", "新生返校带来额外客流", 1.12, 1.1, 1.02, 1.02, 0, 0),
                    roundEvent("周边咖啡店降价", "竞争门店推出低价套餐", .92, .96, .97, 1.0, -.5, 3),
                    roundEvent("原料和人工成本上涨", "门店利润空间被压缩", 1.0, 1.0, .98, 1.08, -1, 2),
                    roundEvent("校园文化节带来客流", "大型活动带来额外订单", 1.1, 1.15, 1.04, 1.04, 0, 1));
            case "career-service" -> List.of(
                    roundEvent("秋招季正式启动", "学生和企业需求集中释放", 1.1, 1.1, 1.02, 1.02, 0, 0),
                    roundEvent("企业岗位质量投诉增加", "平台审核能力受到质疑", .94, .96, .98, 1.02, -3, 5),
                    roundEvent("同类就业平台进入校园", "合作资源竞争加剧", .92, .96, .98, 1.02, -1, 3),
                    roundEvent("新高校提出合作意向", "平台获得新的扩张机会", 1.12, 1.08, 1.03, 1.05, 0, 2));
            default -> List.of(
                    roundEvent("开学季交易需求上涨", "教材和数码需求增加", 1.12, 1.1, 1.0, 1.0, 0, 0),
                    roundEvent("交易纠纷开始增加", "用户信任度受到挑战", .94, .96, .98, 1.02, -3, 5),
                    roundEvent("竞争平台补贴卖家", "优质卖家供给面临竞争", .9, .95, .97, 1.03, -1, 3),
                    roundEvent("学校开放新校区合作", "扩张机会与风险同时出现", 1.12, 1.08, 1.02, 1.04, 0, 2));
        };
        return events.get(Math.max(0, Math.min(events.size() - 1, round - 1)));
    }

    private Map<String, Object> roundEvent(String title, String description, double growth, double demand,
                                           double revenue, double cost, double satisfaction, double risk) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("title", title);
        value.put("description", description);
        value.put("growth", growth);
        value.put("demand", demand);
        value.put("revenue", revenue);
        value.put("cost", cost);
        value.put("satisfaction", satisfaction);
        value.put("risk", risk);
        return value;
    }

    private Map<String, Object> roomView(BusinessSandboxRoom room, Long userId) {
        Map<String, Object> state = readState(room);
        List<Map<String, Object>> companies = companies(state);
        Map<String, Object> membership = findMembership(state, userId);
        List<Map<String, Object>> companyViews = companies.stream()
                .map(company -> companyView(company, userId, membership))
                .toList();
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", room.getId());
        view.put("roomCode", room.getRoomCode());
        view.put("name", room.getName());
        view.put("scenarioId", room.getScenarioId());
        view.put("status", room.getStatus());
        view.put("currentRound", room.getCurrentRound());
        view.put("roundCount", room.getRoundCount());
        view.put("maxCompanies", room.getMaxCompanies());
        view.put("maxMembers", room.getMaxMembers());
        view.put("isHost", Objects.equals(room.getHostUserId(), userId));
        view.put("myCompanyId", membership == null ? null : membership.get("companyId"));
        view.put("myRole", membership == null ? null : membership.get("role"));
        view.put("companyCount", companies.size());
        view.put("memberCount", companies.stream().mapToInt(company -> asList(company.get("members")).size()).sum());
        view.put("companies", companyViews);
        view.put("lastRoundResult", state.get("lastRoundResult"));
        view.put("canStart", "WAITING".equals(room.getStatus()) && companies.size() >= 2
                && companies.stream().allMatch(company -> asList(company.get("members")).size() >= 3));
        view.put("canSettle", "IN_PROGRESS".equals(room.getStatus())
                && companies.stream().allMatch(company -> Boolean.TRUE.equals(company.get("confirmed"))));
        view.put("createdAt", room.getCreatedAt());
        view.put("updatedAt", room.getUpdatedAt());
        return view;
    }

    private Map<String, Object> companyView(Map<String, Object> company, Long userId, Map<String, Object> membership) {
        Map<String, Object> copy = deepCopy(company);
        Map<String, Object> drafts = asMap(copy.remove("drafts"));
        List<Map<String, Object>> statuses = new ArrayList<>();
        for (String role : ROLES) {
            Map<String, Object> status = new LinkedHashMap<>();
            status.put("role", role);
            status.put("submitted", drafts.containsKey(role));
            statuses.add(status);
        }
        boolean isMember = membership != null && Objects.equals(membership.get("companyId"), company.get("id"));
        boolean isCaptain = Objects.equals(asLong(company.get("captainUserId")), userId);
        copy.put("draftStatuses", statuses);
        copy.put("isMyCompany", isMember);
        copy.put("isCaptain", isCaptain);
        if (isMember) copy.put("myDraft", drafts.get(String.valueOf(membership.get("role"))));
        if (isCaptain) copy.put("drafts", drafts);
        List<Map<String, Object>> members = asList(copy.get("members")).stream().map(this::asMap).toList();
        for (Map<String, Object> member : members) member.put("isMe", Objects.equals(asLong(member.get("userId")), userId));
        copy.put("members", members);
        return copy;
    }

    private Map<String, Object> newCompany(String name, Long captainUserId) {
        Map<String, Object> company = new LinkedHashMap<>();
        company.put("id", "C" + UUID.randomUUID().toString().replace("-", "").substring(0, 10));
        company.put("name", name);
        company.put("captainUserId", captainUserId);
        company.put("members", new ArrayList<>(List.of(member(captainUserId, "PRESIDENT"))));
        company.put("drafts", new LinkedHashMap<>());
        company.put("confirmed", false);
        company.put("confirmedAt", null);
        company.put("cash", 100000);
        company.put("users", 3200);
        company.put("activeUsers", 1750);
        company.put("gmv", 198000);
        company.put("revenue", 34500);
        company.put("cost", 25000);
        company.put("profit", 9500);
        company.put("satisfaction", 72);
        company.put("risk", 18);
        company.put("score", 0);
        company.put("history", new ArrayList<>());
        return company;
    }

    private Map<String, Object> member(Long userId, String role) {
        Map<String, Object> member = new LinkedHashMap<>();
        member.put("userId", userId);
        member.put("username", displayName(userId));
        member.put("role", role);
        member.put("joinedAt", new Date().toString());
        return member;
    }

    private Map<String, Object> initialState() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("companies", new ArrayList<>());
        state.put("ranking", new ArrayList<>());
        state.put("lastRoundResult", null);
        return state;
    }

    private Map<String, Object> findMembership(Map<String, Object> state, Long userId) {
        for (Map<String, Object> company : companies(state)) {
            for (Object raw : asList(company.get("members"))) {
                Map<String, Object> member = asMap(raw);
                if (Objects.equals(asLong(member.get("userId")), userId)) {
                    Map<String, Object> membership = new LinkedHashMap<>();
                    membership.put("companyId", company.get("id"));
                    membership.put("role", member.get("role"));
                    return membership;
                }
            }
        }
        return null;
    }

    private Map<String, Object> findCompanyByUser(Map<String, Object> state, Long userId) {
        for (Map<String, Object> company : companies(state)) {
            for (Object raw : asList(company.get("members"))) {
                if (Objects.equals(asLong(asMap(raw).get("userId")), userId)) return company;
            }
        }
        return null;
    }

    private Map<String, Object> requireCompany(Map<String, Object> state, Object companyId) {
        String id = companyId == null ? "" : String.valueOf(companyId);
        return companies(state).stream().filter(company -> id.equals(String.valueOf(company.get("id")))).findFirst()
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND_CODE, "公司不存在"));
    }

    private Map<String, Object> requireMember(Map<String, Object> company, Long userId) {
        for (Object raw : asList(company.get("members"))) {
            Map<String, Object> member = asMap(raw);
            if (Objects.equals(asLong(member.get("userId")), userId)) return member;
        }
        throw new BusinessException(Result.FORBIDDEN_CODE, "你不属于这家公司");
    }

    private BusinessSandboxRoom requireRoom(Long roomId) {
        return repository.findById(roomId).orElseThrow(() -> new BusinessException(Result.NOT_FOUND_CODE, "经营房间不存在"));
    }

    private void requireParticipant(BusinessSandboxRoom room, Long userId) {
        if (!isParticipant(room, userId)) throw new BusinessException(Result.FORBIDDEN_CODE, "你尚未加入该房间");
    }

    private boolean isParticipant(BusinessSandboxRoom room, Long userId) {
        if (Objects.equals(room.getHostUserId(), userId)) return true;
        return findCompanyByUser(readState(room), userId) != null;
    }

    private void requireWaiting(BusinessSandboxRoom room) {
        if (!"WAITING".equals(room.getStatus())) throw new BusinessException(Result.BAD_REQUEST_CODE, "房间已经开始经营");
    }

    private String nextRoomCode() {
        for (int i = 0; i < 10; i++) {
            String code = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
            if (!repository.existsByRoomCode(code)) return code;
        }
        throw new BusinessException(Result.ERROR_CODE, "房间码生成失败");
    }

    private String normalizeRole(String role) {
        String value = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        if (!ROLES.contains(value)) throw new BusinessException(Result.BAD_REQUEST_CODE, "不支持的公司角色");
        return value;
    }

    private String displayName(Long userId) {
        return userRepository.findById(userId)
                .map(user -> StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername())
                .orElse("用户" + userId);
    }

    private Map<String, Object> readState(BusinessSandboxRoom room) {
        try {
            return objectMapper.readValue(room.getStateJson(), new TypeReference<>() { });
        } catch (JsonProcessingException error) {
            throw new BusinessException(Result.ERROR_CODE, "房间数据损坏");
        }
    }

    private void saveState(BusinessSandboxRoom room, Map<String, Object> state) {
        room.setStateJson(write(state));
        repository.save(room);
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException error) {
            throw new BusinessException(Result.ERROR_CODE, "房间数据序列化失败");
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> companies(Map<String, Object> state) {
        return (List<Map<String, Object>>) state.computeIfAbsent("companies", key -> new ArrayList<>());
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> asList(Object value) {
        return value instanceof List<?> list ? (List<T>) list : new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : new LinkedHashMap<>();
    }

    private Map<String, Object> deepCopy(Map<String, Object> value) {
        try {
            return objectMapper.readValue(objectMapper.writeValueAsString(value), new TypeReference<>() { });
        } catch (JsonProcessingException error) {
            throw new BusinessException(Result.ERROR_CODE, "房间数据复制失败");
        }
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) return number.longValue();
        try { return Long.parseLong(String.valueOf(value)); } catch (Exception ignored) { return null; }
    }

    private double asDouble(Object value) {
        if (value instanceof Number number) return number.doubleValue();
        try { return Double.parseDouble(String.valueOf(value)); } catch (Exception ignored) { return 0; }
    }

    private int optional(Integer value, int fallback) { return value == null ? fallback : value; }
    private int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
    private long round(double value) { return Math.round(value); }
    private double round1(double value) { return Math.round(value * 10) / 10.0; }
}