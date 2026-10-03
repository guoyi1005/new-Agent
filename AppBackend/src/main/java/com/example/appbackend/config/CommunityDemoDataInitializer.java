package com.example.appbackend.config;

import com.example.appbackend.entity.ForumPost;
import com.example.appbackend.entity.ForumTopic;
import com.example.appbackend.entity.LocalJobPosting;
import com.example.appbackend.entity.Role;
import com.example.appbackend.entity.User;
import com.example.appbackend.repository.ForumPostRepository;
import com.example.appbackend.repository.ForumTopicRepository;
import com.example.appbackend.repository.LocalJobPostingRepository;
import com.example.appbackend.repository.RoleRepository;
import com.example.appbackend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 校友社区初始内容。
 *
 * 说明：
 * 1. 只在 forum_post 完全为空时写入，社区一旦有真实用户内容就再也不介入；
 * 2. 「内推招聘」板块的内容直接来自平台已抓取的公开岗位数据，属于真实信息；
 * 3. 其余板块是平台整理的示例帖（成长路径、经验复盘、常见问答），用于让新用户理解社区该写什么；
 * 4. 发布者使用两个平台账号，不伪造「已认证校友」等身份标识。
 */
@Component
@Order(240)
public class CommunityDemoDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CommunityDemoDataInitializer.class);

    private static final String TOPIC_PATH = "成长路径";
    private static final String TOPIC_EXPERIENCE = "经验分享";
    private static final String TOPIC_CASE = "就业案例";
    private static final String TOPIC_QA = "问答交流";
    private static final String TOPIC_REFERRAL = "内推招聘";

    private static final int REFERRAL_LIMIT = 6;

    private final ForumTopicRepository topicRepository;
    private final ForumPostRepository postRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final LocalJobPostingRepository jobRepository;

    public CommunityDemoDataInitializer(ForumTopicRepository topicRepository,
                                        ForumPostRepository postRepository,
                                        UserRepository userRepository,
                                        RoleRepository roleRepository,
                                        LocalJobPostingRepository jobRepository) {
        this.topicRepository = topicRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.jobRepository = jobRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (postRepository.count() > 0) {
            return;
        }
        Map<String, ForumTopic> topics = ensureTopics();
        User helper = ensureUser("campus_helper", "校园助手", "社区运营");
        User alumni = ensureUser("alumni_voice", "学长学姐说", "经验分享");

        List<ForumPost> curated = new ArrayList<>();
        for (Seed seed : curatedSeeds()) {
            ForumTopic topic = topics.get(seed.topicName());
            if (topic == null) continue;
            curated.add(newPost(resolveAuthor(seed.author(), helper, alumni), topic, seed.title(), seed.content(), 0, false));
        }
        postRepository.saveAll(curated);

        List<ForumPost> referrals = referralPosts(topics.get(TOPIC_REFERRAL), helper);
        postRepository.saveAll(referrals);

        // ForumPost 的 @PrePersist 会强制写入当前时间，所以插入后再回写一次时间，
        // 让「推荐」首屏以经验内容为主，而不是被招聘信息占满。
        LocalDateTime referralBase = LocalDateTime.now().minusDays(1);
        for (int i = 0; i < referrals.size(); i++) {
            referrals.get(i).setCreateTime(referralBase.minusMinutes(i * 10L));
        }
        postRepository.saveAll(referrals);

        refreshTopicCounts(topics);
        log.info("校友社区初始内容写入完成，共 {} 条帖子", curated.size() + referrals.size());
    }

    private Map<String, ForumTopic> ensureTopics() {
        Map<String, ForumTopic> existing = new LinkedHashMap<>();
        for (ForumTopic topic : topicRepository.findAll()) {
            existing.putIfAbsent(topic.getTopicName(), topic);
        }
        Map<String, String> wanted = new LinkedHashMap<>();
        wanted.put(TOPIC_PATH, "从入学到拿到 offer，记录每个人真实的成长节点");
        wanted.put(TOPIC_EXPERIENCE, "求职准备、笔试面试、学习方法上的实战经验");
        wanted.put(TOPIC_CASE, "岗位、行业、求职过程与结果复盘");
        wanted.put(TOPIC_QA, "把困惑写出来，等学长学姐来回答");
        wanted.put(TOPIC_REFERRAL, "校友与平台整理的实习、校招与内推机会");
        for (Map.Entry<String, String> entry : wanted.entrySet()) {
            if (existing.containsKey(entry.getKey())) continue;
            ForumTopic topic = new ForumTopic();
            topic.setTopicName(entry.getKey());
            topic.setDescription(entry.getValue());
            topic.setPostCount(0);
            topic.setIsHot(1);
            topic.setStatus("ACTIVE");
            topic.setCreateTime(LocalDateTime.now());
            existing.put(entry.getKey(), topicRepository.save(topic));
        }
        return existing;
    }

    private User ensureUser(String username, String realName, String major) {
        return userRepository.findByUsername(username).orElseGet(() -> {
            Role role = roleRepository.findByName("STUDENT").orElseGet(() -> {
                Role created = new Role();
                created.setName("STUDENT");
                return roleRepository.save(created);
            });
            User user = new User();
            user.setUsername(username);
            user.setPassword("Community@Demo2026");
            user.setRealName(realName);
            user.setMajor(major);
            user.setRole(role);
            user.setStatus(1);
            return userRepository.save(user);
        });
    }

    private User resolveAuthor(String key, User helper, User alumni) {
        return "alumni".equals(key) ? alumni : helper;
    }

    private ForumPost newPost(User author, ForumTopic topic, String title, String content,
                              int pinOrder, boolean highlighted) {
        ForumPost post = new ForumPost();
        post.setUserId(author.getId());
        post.setTitle(title);
        post.setContent(content);
        post.setTopicId(topic.getId());
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setStatus("PUBLISHED");
        post.setPinOrder(pinOrder);
        post.setHighlighted(highlighted);
        post.setCreateTime(LocalDateTime.now());
        post.setUpdateTime(LocalDateTime.now());
        return post;
    }

    /** 「内推招聘」直接用平台抓取到的公开岗位数据，保证是真实信息。 */
    private List<ForumPost> referralPosts(ForumTopic topic, User author) {
        List<ForumPost> posts = new ArrayList<>();
        if (topic == null) return posts;
        for (LocalJobPosting job : jobRepository.findAllByOrderByPublishedAtDescIdDesc(PageRequest.of(0, REFERRAL_LIMIT))) {
            String title = "【招聘】" + safe(job.getCompany(), "本地企业") + " · " + job.getJobTitle();
            StringBuilder body = new StringBuilder();
            body.append("【岗位】").append(job.getJobTitle()).append('\n');
            body.append("【公司】").append(safe(job.getCompany(), "未标注")).append('\n');
            String place = (safe(job.getCity(), "") + safe(job.getDistrict(), "")).trim();
            if (!place.isEmpty()) body.append("【地点】").append(place).append('\n');
            if (notBlank(job.getEducation())) body.append("【学历】").append(job.getEducation()).append('\n');
            if (notBlank(job.getSalaryText())) body.append("【薪资】").append(job.getSalaryText()).append('\n');
            if (notBlank(job.getSourceName())) body.append("【来源】").append(job.getSourceName()).append('\n');
            if (notBlank(job.getDetailUrl())) body.append("【详情】").append(job.getDetailUrl()).append('\n');
            body.append("（信息由平台从公开渠道整理，投递前请以原渠道为准。）");
            posts.add(newPost(author, topic, title, body.toString(), 0, false));
        }
        return posts;
    }

    private void refreshTopicCounts(Map<String, ForumTopic> topics) {
        // 帖子量级很小，直接内存统计即可
        java.util.List<ForumPost> all = postRepository.findAll();
        for (ForumTopic topic : topics.values()) {
            long matched = all.stream()
                    .filter(post -> topic.getId().equals(post.getTopicId()))
                    .count();
            topic.setPostCount((int) matched);
            topicRepository.save(topic);
        }
    }

    private List<Seed> curatedSeeds() {
        return List.of(
            new Seed(TOPIC_PATH, "alumni",
                "大一到毕业，我把四年的技能路线整理成了一张图",
                "大一：先把高数和英语稳住，同时入门 Python，能写简单脚本就够了。\n"
                + "大二：补数据结构与算法，开始刷题；把 MySQL 和一门后端框架学起来。\n"
                + "大三：做 1-2 个能讲清楚的项目，争取一段实习，并拿着目标岗位的技能要求一条条对差距。\n"
                + "大四：秋招集中投递，把项目、实习、刷题三条线整理成简历素材。\n\n"
                + "最大的体会是：不要等到大四才开始想岗位。大三就该对照岗位要求，缺什么补什么。"),
            new Seed(TOPIC_PATH, "alumni",
                "从零基础到能写接口，我用一年补完的 5 个技能",
                "刚入学时我连命令行都不熟。这一年我按这个顺序补了 5 块：\n"
                + "1. Python 基础语法（能写出小程序）\n"
                + "2. Linux 常用命令（会看日志、会启服务）\n"
                + "3. MySQL 增删改查（会建表、会写查询）\n"
                + "4. FastAPI 写接口（能把数据查出来返回给前端）\n"
                + "5. Git 协作（会分支、会提 PR）\n\n"
                + "顺序很重要：先能写出东西，再补工程规范。每个技能都配一个小练习，比只看视频记得牢得多。"),
            new Seed(TOPIC_EXPERIENCE, "alumni",
                "秋招笔试怎么准备？我踩过的 3 个坑",
                "坑一：只刷题不总结。同一类题反复错，后来我把每道错题按知识点归类，正确率才真正提上去。\n"
                + "坑二：忽略选择题。不少公司的笔试里计算机基础、数据库、操作系统占很大比例，别只练算法。\n"
                + "坑三：临考前才准备。笔试是体力活，至少提前一个月保持手感，每周固定做 1-2 套。"),
            new Seed(TOPIC_EXPERIENCE, "alumni",
                "面试被问项目经历，我是这样讲的",
                "我固定用四句话讲一个项目：\n"
                + "① 这个项目解决什么问题；② 我具体负责哪一块；③ 遇到什么困难、怎么解决的；④ 最后结果如何。\n\n"
                + "面试官其实不关心你用了多少个技术栈，关心你能不能把一件事讲清楚。\n"
                + "不要背稿，先把项目里最难的三个点想透，面试时自然能展开。"),
            new Seed(TOPIC_EXPERIENCE, "helper",
                "专业课和自学怎么平衡",
                "我的做法是把专业课当底线、把自学当加速器：\n"
                + "• 专业课保证不挂科并尽量拿高分，保研和评优都要看；\n"
                + "• 自学只挑和目标岗位直接相关的内容，不要什么都想学。\n\n"
                + "判断标准很简单：这个技能能不能写进简历、能不能在项目里用到。两个都答不上来的，先放一放。"),
            new Seed(TOPIC_CASE, "alumni",
                "双非本科拿下后端开发岗的完整复盘",
                "背景：普通本科，计算机相关专业，没有大厂实习。\n\n"
                + "准备分三步：① 把一门语言学扎实（我选的是 Java）；② 用两个课程项目撑起简历；③ 集中刷高频算法题和八股。\n"
                + "投递节奏：先投中小公司练手，再投目标公司，避免一上来就把最想去的公司面掉。\n\n"
                + "结果：拿到 3 个 offer，最后选了成都一家做企业软件的公司。\n"
                + "复盘下来，能不能把项目讲清楚，比公司大小和学校背景影响更大。"),
            new Seed(TOPIC_CASE, "alumni",
                "从测试实习到转正，我做对了什么",
                "实习期间我做了三件小事：\n"
                + "一是把每个缺陷都写清楚复现步骤，开发不用来回问；\n"
                + "二是主动把重复的手工用例整理成自动化脚本；\n"
                + "三是每周给导师发一份简短进展。\n\n"
                + "转正答辩时，这些就是能直接拿出来的证据。实习最怕的不是活多，而是只做被安排的事。"),
            new Seed(TOPIC_QA, "alumni",
                "大三了还没有实习经历，秋招还有机会吗？",
                "有，但要换策略：\n"
                + "1. 用课程项目替代实习经历，挑一个你能讲透的，把需求、设计、实现都补全；\n"
                + "2. 针对目标岗位补 2-3 个核心技能，别铺开；\n"
                + "3. 秋招优先投对实习经历要求不高的岗位，把中小公司作为第一批练手。\n\n"
                + "身边不少同学就是这样拿到 offer 的，关键是别因为没实习就放弃投递。"),
            new Seed(TOPIC_QA, "helper",
                "目标岗位要求数据库 70 分，我现在 0 分，该先学什么？",
                "按「能立刻用上」的顺序学：\n"
                + "先学单表增删改查和查询条件 → 再学表设计与主外键 → 然后学索引和事务 → 最后补 SQL 优化。\n\n"
                + "每学一块就动手建一张表、写几条语句，比背语法有效得多。"),
            new Seed(TOPIC_QA, "helper",
                "刷题应该按知识点刷，还是按难度刷？",
                "先按知识点，再按难度。\n"
                + "入门阶段锁定一个题型（比如数组与哈希表）连续刷 10-15 题，把套路固化下来；\n"
                + "有基础之后再按难度分层：简单题练手感，中等题练思路，难题按需挑战。\n\n"
                + "不要一上来就随机难度刷，很容易被劝退。")
        );
    }

    private String safe(String value, String fallback) {
        return notBlank(value) ? value : fallback;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private record Seed(String topicName, String author, String title, String content) { }
}