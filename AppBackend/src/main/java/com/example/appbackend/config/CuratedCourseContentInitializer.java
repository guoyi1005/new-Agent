package com.example.appbackend.config;

import com.example.appbackend.entity.CampusCourse;
import com.example.appbackend.entity.CampusCourseChapter;
import com.example.appbackend.repository.CampusCourseChapterRepository;
import com.example.appbackend.repository.CampusCourseRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 精课内容：为三门核心课程补齐章节正文（Python 编程基础 / 前端开发基础 / Java 程序设计基础）。
 * 只填充原本为空的章节，管理员已经录入过的内容不会被覆盖。
 */
@Component
@Order(220)
public class CuratedCourseContentInitializer implements ApplicationRunner {

    private final CampusCourseRepository courseRepository;
    private final CampusCourseChapterRepository chapterRepository;

    public CuratedCourseContentInitializer(CampusCourseRepository courseRepository,
                                           CampusCourseChapterRepository chapterRepository) {
        this.courseRepository = courseRepository;
        this.chapterRepository = chapterRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        fill(pythonContent());
        fill(webContent());
        fill(javaContent());
        fill(sqlContent());
        fill(vueContent());
        fill(springBootContent());
        fill(fastApiContent());
        fill(typeScriptContent());
        fill(dataAnalysisContent());
        fill(llmContent());
        fill(vectorDeployContent());
        fill(pytorchContent());
        fill(productContent());
    }

    private void fill(List<ChapterContent> items) {
        Map<String, CampusCourse> coursesByName = new LinkedHashMap<>();
        for (CampusCourse course : courseRepository.findAll()) {
            coursesByName.putIfAbsent(course.getName(), course);
        }
        for (ChapterContent item : items) {
            CampusCourse course = coursesByName.get(item.courseName());
            if (course == null) continue;
            for (CampusCourseChapter chapter : chapterRepository.findByCourseIdOrderBySortOrderAscIdAsc(course.getId())) {
                if (!item.chapterTitle().equals(chapter.getTitle())) continue;
                if (chapter.getContent() != null && !chapter.getContent().isBlank()) continue;
                chapter.setContent(item.content());
                chapterRepository.save(chapter);
            }
        }
    }

    private List<ChapterContent> pythonContent() {
        return List.of(
                new ChapterContent("Python 编程基础", "开发环境与基础语法",
                        "本章目标：能独立装好 Python，并写出第一个可运行的程序。\n\n"
                        + "一、安装与环境\n"
                        + "1. 到 python.org 下载 3.11 以上版本，安装时勾选 Add Python to PATH。\n"
                        + "2. 终端执行 python --version，能显示版本号说明安装成功。\n"
                        + "3. 编辑器推荐 VS Code + Python 扩展，或 PyCharm 社区版。\n\n"
                        + "二、变量与基本类型\n"
                        + "1. Python 是动态类型语言，赋值即声明：name = '张三'、age = 18、score = 92.5。\n"
                        + "2. 常用类型：str 字符串、int 整数、float 小数、bool 布尔值。\n"
                        + "3. 用 type(x) 查看类型，用 int()、float()、str() 做类型转换。\n\n"
                        + "三、输入输出\n"
                        + "1. input() 读取用户输入，返回值一定是字符串，要数字时写成 int(input())。\n"
                        + "2. print() 支持多个参数，推荐用 f-string：print(f'你好，{name}')。\n"
                        + "3. 单行注释用 #，多行说明用三引号。\n\n"
                        + "练习：写一个程序，询问用户姓名和出生年份，输出「你好 XXX，你今年 XX 岁」。"),
                new ChapterContent("Python 编程基础", "数据类型与流程控制",
                        "本章目标：掌握列表、字典等常用容器，并能用分支和循环控制程序流程。\n\n"
                        + "一、列表与元组\n"
                        + "1. 列表可变：nums = [3, 1, 4]，支持 append、insert、remove、sort。\n"
                        + "2. 切片：nums[1:3] 取第 2 到第 3 个元素，nums[::-1] 得到反转结果。\n"
                        + "3. 元组不可变：point = (3, 5)，适合表示不会被修改的数据。\n\n"
                        + "二、字典与集合\n"
                        + "1. 字典按 key 取值：student = {'name': '李四', 'score': 88}。\n"
                        + "2. 取值建议用 student.get('score', 0)，键不存在时不会报错。\n"
                        + "3. 遍历写成 for key, value in student.items()。\n"
                        + "4. 集合用来去重：set([1, 2, 2, 3]) 得到 {1, 2, 3}。\n\n"
                        + "三、分支与循环\n"
                        + "1. if / elif / else，Python 用缩进表示代码块，不要混用空格和 Tab。\n"
                        + "2. for 用来遍历序列，range(1, 6) 生成 1 到 5。\n"
                        + "3. while 用于次数不确定的循环，注意检查退出条件避免死循环。\n"
                        + "4. break 跳出整个循环，continue 只跳过本次。\n\n"
                        + "练习：统计一段文本里每个单词出现的次数，输出出现最多的 3 个单词。"),
                new ChapterContent("Python 编程基础", "函数、模块与异常处理",
                        "本章目标：会把重复逻辑封装成函数，会拆分模块，并处理程序出错的情况。\n\n"
                        + "一、函数\n"
                        + "1. 定义：def greet(name, greeting='你好'): return f'{greeting}，{name}'。\n"
                        + "2. 参数形式：默认参数、关键字参数、可变参数 *args 与 **kwargs。\n"
                        + "3. 一个函数只做一件事，名字用动词，方便复用和测试。\n\n"
                        + "二、模块与包\n"
                        + "1. 一个 .py 文件就是一个模块，用 import math 或 from math import sqrt 引入。\n"
                        + "2. 用 if __name__ == '__main__': 区分「被导入」和「直接运行」。\n"
                        + "3. 多个模块放在同一目录并加上 __init__.py 就构成包。\n\n"
                        + "三、异常处理\n"
                        + "1. try / except / else / finally 的结构与执行顺序要能说清楚。\n"
                        + "2. 捕获具体异常，例如 except ValueError，不要用裸 except 吞掉所有错误。\n"
                        + "3. 用 raise 主动抛出异常，用 assert 做开发期断言。\n\n"
                        + "练习：写一个读取成绩文件并计算平均分的函数，遇到文件不存在或格式错误时给出友好提示。")
        );
    }

    private List<ChapterContent> webContent() {
        return List.of(
                new ChapterContent("前端开发基础", "HTML 页面结构",
                        "本章目标：能看懂并手写一个结构清晰的 HTML 页面。\n\n"
                        + "一、页面骨架\n"
                        + "1. 标准结构包含 <!DOCTYPE html>、<html>、<head> 与 <body>。\n"
                        + "2. <head> 里放 meta、title、样式和脚本引用，<body> 里放用户能看到的内容。\n\n"
                        + "二、常用标签\n"
                        + "1. 语义化标签：header、nav、main、section、article、footer。\n"
                        + "2. 文本标签：h1~h6 标题、p 段落、strong 强调、a 链接。\n"
                        + "3. 列表：ul / ol / li；表格：table / thead / tbody / tr / td。\n"
                        + "4. 图片用 img，务必写 alt 属性，方便读屏软件和图片加载失败时展示。\n\n"
                        + "三、表单\n"
                        + "1. form 配合 input、select、textarea 收集用户输入。\n"
                        + "2. input 的 type 决定控件形态：text、password、email、number、date。\n"
                        + "3. 用 label 的 for 属性关联输入框，点击文字也能聚焦。\n\n"
                        + "练习：用语义化标签写一个个人主页骨架，包含导航、简介、技能列表和联系表单。"),
                new ChapterContent("前端开发基础", "CSS 样式与页面布局",
                        "本章目标：能用 CSS 控制样式，并用 Flex / Grid 完成常见页面布局。\n\n"
                        + "一、选择器与样式引入\n"
                        + "1. 常用选择器：标签、类 .box、ID #app、后代 .nav a、伪类 :hover。\n"
                        + "2. 样式可以写在 style 标签、外部 css 文件或行内 style 属性，推荐外部文件。\n\n"
                        + "二、盒模型\n"
                        + "1. 每个元素由 content、padding、border、margin 组成。\n"
                        + "2. 统一设置 box-sizing: border-box，让宽度包含内边距和边框，布局更好算。\n"
                        + "3. margin 相邻元素会合并，padding 不会。\n\n"
                        + "三、布局\n"
                        + "1. Flex 适合一维排列：display: flex 配合 justify-content 和 align-items。\n"
                        + "2. Grid 适合二维栅格：grid-template-columns: repeat(3, 1fr) 做三列卡片。\n"
                        + "3. 移动端适配：媒体查询 @media (max-width: 768px) 覆盖小屏样式。\n\n"
                        + "四、常见问题\n"
                        + "1. 高度塌陷用 overflow: hidden 或 display: flow-root 解决。\n"
                        + "2. 元素居中：Flex 用 justify-content 与 align-items，绝对定位用 transform。\n\n"
                        + "练习：把上一章的主页骨架做成三列卡片布局，并保证手机宽度下自动变成单列。"),
                new ChapterContent("前端开发基础", "JavaScript 交互基础",
                        "本章目标：能用 JavaScript 操作页面元素并响应用户操作。\n\n"
                        + "一、基础语法\n"
                        + "1. 变量用 let 和 const，不要再用 var。\n"
                        + "2. 常用类型：number、string、boolean、null、undefined、object、array。\n"
                        + "3. 条件与循环和 Python 类似，但用 { } 表示代码块。\n\n"
                        + "二、函数与数组方法\n"
                        + "1. 函数声明与箭头函数：const add = (a, b) => a + b。\n"
                        + "2. 常用数组方法：map、filter、find、reduce，替代手写 for 循环。\n"
                        + "3. 模板字符串用反引号，可以内嵌变量。\n\n"
                        + "三、DOM 操作\n"
                        + "1. 查询元素：document.querySelector('.btn')，查询多个用 querySelectorAll。\n"
                        + "2. 修改内容和样式：el.textContent = '新文本'，el.classList.add('active')。\n"
                        + "3. 绑定事件：el.addEventListener('click', () => { })。\n\n"
                        + "四、常见坑\n"
                        + "1. 脚本写在 head 里要先加 defer，否则 DOM 还没生成就执行会报错。\n"
                        + "2. 用 === 而不是 == 比较，避免隐式类型转换。\n\n"
                        + "练习：给课程列表加一个搜索框，输入关键字时实时过滤卡片，并显示「没有匹配结果」。")
        );
    }

    private List<ChapterContent> javaContent() {
        return List.of(
                new ChapterContent("Java 程序设计基础", "Java 语法与面向对象",
                        "本章目标：掌握 Java 基本语法，能写出带类和对象的程序。\n\n"
                        + "一、基本语法\n"
                        + "1. 程序入口固定为 public static void main(String[] args)。\n"
                        + "2. Java 是静态类型语言，变量必须先声明类型：int age = 18; String name = '张三';\n"
                        + "3. 与 Python 不同，Java 语句以分号结尾，代码块用 { }。\n\n"
                        + "二、数据类型\n"
                        + "1. 基本类型：byte、short、int、long、float、double、char、boolean。\n"
                        + "2. 引用类型：String、数组、自定义类。\n"
                        + "3. 包装类 Integer、Double 提供与集合配合的能力，注意自动装箱拆箱。\n\n"
                        + "三、面向对象\n"
                        + "1. 用 class 定义类，用 new 创建对象。\n"
                        + "2. 封装：字段用 private，通过 getter / setter 访问。\n"
                        + "3. 继承用 extends，方法重写用 @Override；父类引用指向子类对象即多态。\n"
                        + "4. 接口用 interface，一个类可以实现多个接口，这是 Java 解耦的主要手段。\n\n"
                        + "练习：设计一个 Student 类（学号、姓名、成绩）和 Course 类，在 main 中创建若干个对象并打印。"),
                new ChapterContent("Java 程序设计基础", "集合与异常处理",
                        "本章目标：熟练使用常用集合，并能正确处理程序异常。\n\n"
                        + "一、集合框架\n"
                        + "1. List 有序可重复：ArrayList 查询快、LinkedList 增删快。\n"
                        + "2. Set 不重复：HashSet 无序、TreeSet 按大小排序。\n"
                        + "3. Map 键值对：HashMap 是最常用的实现，键不可重复。\n"
                        + "4. 遍历方式：for-each、迭代器 Iterator、Stream 流式处理。\n\n"
                        + "二、泛型\n"
                        + "1. 用 List<Student> 指定元素类型，避免运行时类型转换错误。\n"
                        + "2. 泛型只在编译期生效，运行时会被擦除（类型擦除）。\n\n"
                        + "三、异常处理\n"
                        + "1. 异常体系：Throwable 分为 Error 和 Exception，Exception 又分受检与非受检。\n"
                        + "2. try / catch / finally 的写法，finally 用来释放资源。\n"
                        + "3. 推荐用 try-with-resources 自动关闭文件、连接等资源。\n"
                        + "4. 不要用 catch (Exception e) {} 把异常吞掉，至少记录日志。\n\n"
                        + "练习：读取一个学生名单文件，把内容解析成 List<Student>，对文件不存在和格式错误分别给出提示。"),
                new ChapterContent("Java 程序设计基础", "多线程与常用类库",
                        "本章目标：理解线程基础，并熟悉日常开发常用的类库。\n\n"
                        + "一、多线程基础\n"
                        + "1. 创建线程的两种方式：继承 Thread、实现 Runnable（推荐）。\n"
                        + "2. 线程生命周期：新建、就绪、运行、阻塞、终止。\n"
                        + "3. 共享数据要用 synchronized 或 java.util.concurrent 下的原子类、锁。\n"
                        + "4. 线程池用 Executors 或 ThreadPoolExecutor 创建，避免无限创建线程。\n\n"
                        + "二、常用类库\n"
                        + "1. 字符串：String 不可变，频繁拼接用 StringBuilder。\n"
                        + "2. 时间日期：推荐用 java.time 包的 LocalDate、LocalDateTime、Duration。\n"
                        + "3. 工具类：Objects、Arrays、Collections 提供大量静态方法。\n"
                        + "4. IO 与 NIO：文件读写用 Files、Path，比传统 File 更方便。\n\n"
                        + "三、编码习惯\n"
                        + "1. 命名规范：类名大驼峰，方法和变量小驼峰，常量全大写下划线分隔。\n"
                        + "2. 学会看异常堆栈：从最下面一行的 Caused by 往上定位真正的出错位置。\n\n"
                        + "练习：用线程池并发下载 5 个文本文件的内容，最后汇总打印总字数。")
        );
    }

    private List<ChapterContent> sqlContent() {
        return List.of(
                new ChapterContent("MySQL 数据库基础", "SQL 查询基础",
                        "本章目标：能独立写出常用的查询语句。\n\n"
                        + "一、数据库与表\n"
                        + "1. 数据库是表的集合，表由行（记录）和列（字段）组成。\n"
                        + "2. 常用命令：SHOW DATABASES; USE db; SHOW TABLES; DESC student;\n\n"
                        + "二、增删改\n"
                        + "1. 新增：INSERT INTO student(name, age) VALUES('张三', 18);\n"
                        + "2. 修改：UPDATE student SET age = 19 WHERE id = 1;\n"
                        + "3. 删除：DELETE FROM student WHERE id = 1;，先写 WHERE 再写条件，避免误删全表。\n\n"
                        + "三、查询\n"
                        + "1. 基本查询：SELECT name, age FROM student WHERE age >= 18 ORDER BY age DESC LIMIT 10;\n"
                        + "2. 模糊匹配 LIKE '张%'，范围 BETWEEN 18 AND 22，空值判断 IS NULL。\n"
                        + "3. 聚合函数 COUNT、SUM、AVG、MAX、MIN 配合 GROUP BY，分组后再用 HAVING 过滤。\n"
                        + "4. 多表连接：INNER JOIN 取交集，LEFT JOIN 保留左表全部记录。\n\n"
                        + "练习：建一张学生表并插入 5 条数据，查出年龄最大的 3 个人，以及每个班级的平均分。"),
                new ChapterContent("MySQL 数据库基础", "表设计与约束",
                        "本章目标：能设计出结构合理、不易出错的数据表。\n\n"
                        + "一、字段类型\n"
                        + "1. 整数用 INT / BIGINT，金额用 DECIMAL(10,2)，不要用 FLOAT 存钱。\n"
                        + "2. 字符串用 VARCHAR(n)，长文本用 TEXT，时间用 DATETIME 或 TIMESTAMP。\n\n"
                        + "二、约束\n"
                        + "1. 主键 PRIMARY KEY 唯一标识一行，通常用自增 ID。\n"
                        + "2. 非空 NOT NULL、唯一 UNIQUE、默认值 DEFAULT、外键 FOREIGN KEY。\n"
                        + "3. 外键能保证引用完整性，但高并发写入会有锁开销，不少项目改为在应用层校验。\n\n"
                        + "三、设计原则\n"
                        + "1. 一个字段只存一个含义，不要把多个值塞进一个字段。\n"
                        + "2. 尽量满足第三范式，但为了查询性能可以适度冗余。\n"
                        + "3. 每张表都加上 create_time 和 update_time，方便排查问题。\n\n"
                        + "四、修改表结构\n"
                        + "ALTER TABLE student ADD COLUMN class_id BIGINT;，生产环境改表要评估锁表时间。\n\n"
                        + "练习：为学生、课程、选课三个场景设计三张表，写出建表语句并说明主键与外键。"),
                new ChapterContent("MySQL 数据库基础", "索引与事务",
                        "本章目标：理解索引的作用，会正确使用事务保证数据一致性。\n\n"
                        + "一、索引\n"
                        + "1. 索引像书的目录，能大幅加快查询，但占空间并拖慢写入。\n"
                        + "2. 建索引：CREATE INDEX idx_student_name ON student(name);\n"
                        + "3. 最左前缀原则：联合索引 (a, b) 能命中 a 或 a+b 的查询，不能单独命中 b。\n"
                        + "4. 索引失效常见情况：对字段做函数运算、以 % 开头的 LIKE、隐式类型转换。\n"
                        + "5. 用 EXPLAIN 查看执行计划，重点关注 type、key、rows 三列。\n\n"
                        + "二、事务\n"
                        + "1. 事务把多条语句打包，要么全部成功，要么全部回滚。\n"
                        + "2. 手动控制：START TRANSACTION; ... COMMIT; 出错时 ROLLBACK;\n"
                        + "3. ACID 四个特性：原子性、一致性、隔离性、持久性。\n"
                        + "4. 常见并发问题：脏读、不可重复读、幻读，通过隔离级别控制。\n\n"
                        + "练习：给上一章的学生表加索引，用 EXPLAIN 对比加索引前后同一个查询的执行计划。")
        );
    }

    private List<ChapterContent> vueContent() {
        return List.of(
                new ChapterContent("Vue 3 组件开发", "Vue 3 基础语法",
                        "本章目标：掌握 Vue 3 的响应式与模板语法，能写出第一个组件。\n\n"
                        + "一、创建项目\n"
                        + "1. 用 npm create vue@latest 创建项目，模板选 Vue 3 + Vite。\n"
                        + "2. 目录：src/main.js 是入口，App.vue 是根组件，components 放公共组件。\n\n"
                        + "二、组合式 API\n"
                        + "1. 用 script setup 写组件，代码更简洁。\n"
                        + "2. 响应式数据用 ref（基本类型）和 reactive（对象）。\n"
                        + "3. 在脚本里取值要写 .value，模板中会自动解包。\n"
                        + "4. computed 用于派生数据，watch 用于监听变化执行副作用。\n\n"
                        + "三、模板语法\n"
                        + "1. 插值 {{ }}、属性绑定 v-bind（简写 :）、事件绑定 v-on（简写 @）。\n"
                        + "2. 条件渲染 v-if / v-else，频繁切换用 v-show。\n"
                        + "3. 列表渲染 v-for 必须写 key，且不要和 v-if 用在同一个元素上。\n\n"
                        + "练习：写一个计数器组件，包含加减和重置按钮，用 computed 显示当前是偶数还是奇数。"),
                new ChapterContent("Vue 3 组件开发", "组件通信与状态管理",
                        "本章目标：会在父子组件之间传值，并管理跨组件的共享状态。\n\n"
                        + "一、父子通信\n"
                        + "1. 父传子用 props，子组件用 defineProps 声明接收。\n"
                        + "2. 子传父用 emit，子组件用 defineEmits 声明并抛出事件。\n"
                        + "3. v-model 本质是 props + emit 的语法糖。\n\n"
                        + "二、插槽\n"
                        + "1. 默认插槽、具名插槽、作用域插槽的使用场景。\n"
                        + "2. 用插槽把弹窗、卡片这类通用组件的布局逻辑复用起来。\n\n"
                        + "三、状态管理\n"
                        + "1. 小组件用 props / emit 就够，跨层级或全局状态才引入 Pinia。\n"
                        + "2. Pinia 的 state、getters、actions 分别对应数据、派生和方法。\n"
                        + "3. 接口请求统一放到 store 的 action 里，不要在组件中到处复制。\n\n"
                        + "练习：做一个待办列表，父组件管理数据和筛选项，子组件负责展示并触发完成、删除事件。"),
                new ChapterContent("Vue 3 组件开发", "路由与接口联调",
                        "本章目标：会用路由组织页面，并完成前后端接口联调。\n\n"
                        + "一、路由\n"
                        + "1. 安装 vue-router，配置 createRouter 和 createWebHistory。\n"
                        + "2. 路由表用 path、name、component 描述，router-link 跳转，router-view 渲染。\n"
                        + "3. 动态路由如 /course/:id，组件里用 useRoute().params.id 取值。\n"
                        + "4. 路由守卫 beforeEach 用于登录校验和权限判断。\n\n"
                        + "二、接口联调\n"
                        + "1. 用 fetch 或 axios 发请求，统一封装 request 函数处理 baseURL、token 和错误。\n"
                        + "2. 请求要处理加载中、错误、空数据三种状态，不要只写成功分支。\n"
                        + "3. 开发阶段用 Vite 的 proxy 解决跨域，生产环境由 Nginx 反向代理。\n\n"
                        + "三、常见问题\n"
                        + "1. 组件卸载后请求才返回，可能更新已销毁的组件，要处理取消或忽略。\n"
                        + "2. 列表接口要做分页，避免一次性加载过多数据。\n\n"
                        + "练习：把待办列表拆成列表页和详情页，通过路由跳转并调用后端接口保存数据。")
        );
    }

    private List<ChapterContent> springBootContent() {
        return List.of(
                new ChapterContent("Spring Boot 后端开发", "Spring Boot 快速入门",
                        "本章目标：能跑起一个 Spring Boot 项目并写出第一个 REST 接口。\n\n"
                        + "一、项目结构\n"
                        + "1. 用 Spring Initializr 生成项目，勾选 Spring Web、Spring Data JPA、MySQL Driver。\n"
                        + "2. 目录约定：controller 放接口，service 放业务，repository 放数据访问，entity 放实体。\n"
                        + "3. 启动类上的 SpringBootApplication 负责自动配置和组件扫描。\n\n"
                        + "二、第一个接口\n"
                        + "1. 用 RestController 和 RequestMapping 声明控制器与路径。\n"
                        + "2. GetMapping、PostMapping、PutMapping、DeleteMapping 对应增删改查。\n"
                        + "3. RequestParam 取查询参数，PathVariable 取路径变量，RequestBody 接收 JSON 请求体。\n\n"
                        + "三、配置与依赖注入\n"
                        + "1. application.yml 里配置端口、数据库和日志级别。\n"
                        + "2. 构造器注入是官方推荐方式，便于测试也能提早发现循环依赖。\n"
                        + "3. 不同环境用 profile 区分，如 application-dev.yml 与 application-prod.yml。\n\n"
                        + "练习：写一个返回课程列表的接口，支持按关键字分页查询，并统一返回 code、msg、data 结构。"),
                new ChapterContent("Spring Boot 后端开发", "数据访问与 MySQL 集成",
                        "本章目标：会用 Spring Data JPA 完成对 MySQL 的增删改查。\n\n"
                        + "一、数据源配置\n"
                        + "1. 在 application.yml 配置 spring.datasource 的 url、username、password 和驱动。\n"
                        + "2. 连接池默认使用 HikariCP，按需调整 maximum-pool-size。\n\n"
                        + "二、实体与仓储\n"
                        + "1. Entity 和 Table 映射表，Id 与 GeneratedValue 配置主键。\n"
                        + "2. 继承 JpaRepository 就拥有 save、findById、findAll、delete 等常用方法。\n"
                        + "3. 按方法名派生查询，如 findByNameContaining、findByStatusOrderBySortOrderAsc。\n"
                        + "4. 复杂查询用 Query 注解写 JPQL 或原生 SQL。\n\n"
                        + "三、事务与常见坑\n"
                        + "1. 写操作加 Transactional，注意同类内部方法调用不会触发事务。\n"
                        + "2. 避免 N+1 查询，批量场景用 JOIN FETCH。\n"
                        + "3. ddl-auto=update 只适合开发环境，生产必须用 Flyway 这类迁移脚本。\n\n"
                        + "练习：建一张课程表，实现按名称模糊查询、按状态筛选和分页返回。"),
                new ChapterContent("Spring Boot 后端开发", "接口分层与 Redis 缓存",
                        "本章目标：掌握后端分层写法，并用 Redis 做缓存。\n\n"
                        + "一、分层职责\n"
                        + "1. Controller 只做参数校验和结果包装，不写业务逻辑。\n"
                        + "2. Service 承担业务编排和事务边界。\n"
                        + "3. Repository 只负责数据访问，不掺业务判断。\n\n"
                        + "二、统一异常与返回\n"
                        + "1. 自定义 BusinessException 携带错误码和提示信息。\n"
                        + "2. 用 RestControllerAdvice 统一捕获异常，避免每个接口都写 try-catch。\n"
                        + "3. 统一返回结构便于前端处理：成功 code=200，失败带 code 和 msg。\n\n"
                        + "三、Redis 缓存\n"
                        + "1. 常用命令：SET、GET、EXPIRE、DEL、HGETALL。\n"
                        + "2. 读取模式：先查缓存，未命中再查库并回写，同时设置过期时间。\n"
                        + "3. 一致性处理：更新数据后删除缓存，而不是更新缓存，可降低脏数据概率。\n"
                        + "4. 注意缓存穿透、击穿和雪崩三种典型问题。\n\n"
                        + "练习：给课程查询接口加 Redis 缓存，并实现更新课程后删除缓存的逻辑。")
        );
    }

    private List<ChapterContent> fastApiContent() {
        return List.of(
                new ChapterContent("FastAPI 接口开发", "路由与请求参数",
                        "本章目标：能用 FastAPI 写出一组规范的 REST 接口。\n\n"
                        + "一、快速开始\n"
                        + "1. 安装 pip install fastapi uvicorn，启动 uvicorn main:app --reload。\n"
                        + "2. 用 app.get、app.post 声明路由，函数返回值自动序列化成 JSON。\n"
                        + "3. 自带交互文档，启动后访问 /docs 就能调试接口。\n\n"
                        + "二、参数类型\n"
                        + "1. 路径参数：app.get('/courses/{course_id}')，形参声明类型即自动校验。\n"
                        + "2. 查询参数：函数默认值就是查询参数，如 page: int = 1。\n"
                        + "3. 请求体：用 Pydantic 模型接收 JSON，POST 接口的参数类型写成模型类。\n\n"
                        + "三、响应\n"
                        + "1. 用 response_model 约束返回结构，避免泄露多余字段。\n"
                        + "2. 状态码约定：正常 200，创建成功 201，参数错误 422，未授权 401。\n\n"
                        + "练习：写一组课程接口（列表、详情、新增），并用 /docs 页面完成自测。"),
                new ChapterContent("FastAPI 接口开发", "数据校验与响应模型",
                        "本章目标：用 Pydantic 做严格的数据校验，保证接口健壮。\n\n"
                        + "一、Pydantic 模型\n"
                        + "1. 继承 BaseModel 定义字段和类型，如 name: str、score: float = 0。\n"
                        + "2. 用 Field 补充约束：min_length、max_length、ge、le。\n"
                        + "3. 用 Optional 表示可空字段，用 list 和 dict 表示复杂结构。\n\n"
                        + "二、模型复用\n"
                        + "1. 拆成 Base、Create、Update、Out 多个模型，避免一个模型承担所有场景。\n"
                        + "2. 配置 from_attributes=True 支持从 ORM 对象转换。\n"
                        + "3. 嵌套模型用来表达关联数据，例如课程里包含章节列表。\n\n"
                        + "三、错误处理\n"
                        + "1. 校验失败时 FastAPI 自动返回 422 和字段级错误信息。\n"
                        + "2. 业务错误用 HTTPException 主动抛出，并写清 detail。\n"
                        + "3. 用 exception_handler 注册全局异常处理，统一错误格式。\n\n"
                        + "练习：为课程新增接口定义 Create 模型，给名称加长度限制、给分数加范围限制，验证非法输入会被拒绝。"),
                new ChapterContent("FastAPI 接口开发", "依赖注入与项目结构",
                        "本章目标：用依赖注入组织代码，把项目拆成可维护的结构。\n\n"
                        + "一、依赖注入\n"
                        + "1. 用 Depends 声明依赖，例如 db=Depends(get_db)。\n"
                        + "2. 依赖可以层层嵌套，鉴权、分页、获取当前用户这类公共逻辑最适合抽成依赖。\n"
                        + "3. 依赖支持 yield，适合做进入时创建、退出时清理的资源管理。\n\n"
                        + "二、项目结构\n"
                        + "1. 推荐分层：main.py、routers、schemas、models、services、core。\n"
                        + "2. 路由按业务拆文件，用 APIRouter 组织后统一注册到主应用。\n"
                        + "3. 配置用 pydantic-settings 从环境变量读取，不要把密钥写进代码。\n\n"
                        + "三、异步\n"
                        + "1. IO 密集接口用 async def，数据库驱动也要用异步版本。\n"
                        + "2. 不要在 async 函数里写阻塞调用，否则会拖慢整个事件循环。\n\n"
                        + "练习：把课程接口按 routers、schemas、models 拆分，并加一个统一的鉴权依赖。")
        );
    }

    private List<ChapterContent> typeScriptContent() {
        return List.of(
                new ChapterContent("TypeScript 与前端工程化", "TypeScript 类型系统",
                        "本章目标：掌握 TypeScript 的核心类型，能给现有 JS 代码加上类型。\n\n"
                        + "一、基础类型\n"
                        + "1. 原始类型：string、number、boolean、null、undefined。\n"
                        + "2. 数组用 string[] 或 Array<string>，对象用接口描述。\n"
                        + "3. 联合类型和字面量类型能有效约束取值范围，如 'easy' | 'medium' | 'hard'。\n\n"
                        + "二、接口与类型别名\n"
                        + "1. interface 描述对象结构，支持继承。\n"
                        + "2. type 更适合定义联合、交叉和工具类型。\n"
                        + "3. 可选属性用 ?，只读属性用 readonly。\n\n"
                        + "三、函数与泛型\n"
                        + "1. 给参数和返回值标注类型，可选参数要放在必选参数之后。\n"
                        + "2. 泛型让函数适配多种类型，如 function first<T>(list: T[]): T | undefined。\n"
                        + "3. 用 keyof、Partial、Pick、Omit 等工具类型减少重复定义。\n\n"
                        + "练习：给课程列表页定义数据结构 interface，并把筛选函数改成泛型版本。"),
                new ChapterContent("TypeScript 与前端工程化", "模块化与构建工具",
                        "本章目标：理解模块化与构建工具，能配置一个现代前端项目。\n\n"
                        + "一、模块化\n"
                        + "1. ES Module 用 export 和 import，一个文件只做一件事。\n"
                        + "2. 默认导出与具名导出的区别：具名导出更利于重构和自动补全。\n"
                        + "3. 路径别名（如 @/components）能让深层引用更清晰。\n\n"
                        + "二、Vite\n"
                        + "1. 开发时使用原生 ESM，启动快；打包时基于 Rollup。\n"
                        + "2. vite.config.ts 常用配置：alias、server.proxy、build.outDir。\n"
                        + "3. 环境变量以 VITE_ 开头，用 import.meta.env 读取。\n\n"
                        + "三、代码质量\n"
                        + "1. ESLint 管代码规范，Prettier 管格式，两者配合使用。\n"
                        + "2. 提交前用 lint-staged 加 husky 自动检查，避免把问题带进仓库。\n\n"
                        + "练习：给项目配置一个 @ 路径别名和开发代理，并用环境变量区分本地与线上接口地址。"),
                new ChapterContent("TypeScript 与前端工程化", "前端项目工程化实践",
                        "本章目标：把工程化手段用到真实项目里，提升协作与交付质量。\n\n"
                        + "一、目录组织\n"
                        + "1. 按职责分目录：api、components、views、stores、utils、types。\n"
                        + "2. 通用组件与业务组件分开，避免 components 目录无限膨胀。\n\n"
                        + "二、请求层封装\n"
                        + "1. 统一 axios 实例：baseURL、超时、token 注入、错误提示。\n"
                        + "2. 响应拦截器统一处理 401 跳登录、500 提示、业务错误码。\n"
                        + "3. 请求函数要类型化，定义好请求参数和返回数据的类型。\n\n"
                        + "三、构建与部署\n"
                        + "1. 用路由懒加载拆分首屏体积，避免单个 js 超过 1MB。\n"
                        + "2. 生产构建开启 sourcemap 便于定位问题，静态资源加 hash 便于缓存。\n"
                        + "3. 部署到 Nginx 时，history 模式需要 try_files 回退到 index.html。\n\n"
                        + "练习：给课程列表页做一次优化：请求层抽离、类型补全、路由懒加载，并对比优化前后的打包体积。")
        );
    }    private List<ChapterContent> dataAnalysisContent() {
        return List.of(
                new ChapterContent("数据分析与可视化", "数据分析方法入门",
                        "本章目标：建立数据分析的基本思路，能从一个问题走到一个结论。\n\n"
                        + "一、分析流程\n"
                        + "1. 明确问题：要回答什么业务问题，指标怎么定义。\n"
                        + "2. 获取数据：来源、口径、时间范围都要写清楚。\n"
                        + "3. 清洗数据：处理缺失值、异常值和重复记录。\n"
                        + "4. 分析验证：描述现状、寻找原因、做横向纵向对比。\n"
                        + "5. 输出结论：给出可执行建议，而不是只贴一堆图表。\n\n"
                        + "二、常用指标\n"
                        + "1. 总量、均值、中位数、分位数。\n"
                        + "2. 同比（与去年同期比）、环比（与上一周期比）。\n"
                        + "3. 转化率、留存率、活跃度等业务指标。\n\n"
                        + "三、常见误区\n"
                        + "1. 只看平均值忽略分布，容易被极端值带偏。\n"
                        + "2. 把相关当因果，缺少对照组就下结论。\n"
                        + "3. 样本量太小、时间窗口太短。\n\n"
                        + "练习：选一个熟悉的校园场景（如二手交易量），定义 3 个指标并写出可验证的分析问题。"),
                new ChapterContent("数据分析与可视化", "统计与图表可视化",
                        "本章目标：能选对图表，并正确表达数据之间的关系。\n\n"
                        + "一、图表选择\n"
                        + "1. 比较大小用柱状图，看趋势用折线图，看构成用饼图或堆叠柱状图。\n"
                        + "2. 看两个变量关系用散点图，看分布用直方图或箱线图。\n"
                        + "3. 一张图只表达一个结论，不要堆太多维度。\n\n"
                        + "二、图表规范\n"
                        + "1. 柱状图坐标轴从 0 开始；折线图可视情况不为 0，但要标注清楚。\n"
                        + "2. 标题写结论而不是写「某某图」，如「大三学生二手交易参与率最高」。\n"
                        + "3. 颜色不超过 5 种，注意色盲友好，避免红绿直接对比。\n\n"
                        + "三、用代码画图\n"
                        + "1. matplotlib 适合精细控制，seaborn 适合统计类图形。\n"
                        + "2. 常见坑：中文乱码要设置字体，图例和坐标轴标签要齐全。\n\n"
                        + "练习：用一组校园数据画出趋势图、构成图和分布图，并给每张图写一句结论作为标题。"),
                new ChapterContent("数据分析与可视化", "数据分析报告输出",
                        "本章目标：能把分析结果组织成别人看得懂的报告。\n\n"
                        + "一、报告结构\n"
                        + "1. 背景与问题、数据说明、分析过程、结论与建议。\n"
                        + "2. 先给结论再给证据，符合金字塔原理。\n"
                        + "3. 每个结论都要能追溯到具体的数据和口径。\n\n"
                        + "二、表达技巧\n"
                        + "1. 少用专业术语，用业务语言描述。\n"
                        + "2. 关键数字加粗或单独列出，避免淹没在段落里。\n"
                        + "3. 图表要配一段文字解读，不要让读者自己猜。\n\n"
                        + "三、数据可信度\n"
                        + "1. 说明数据来源、时间范围和样本量。\n"
                        + "2. 主动写出局限性和不确定性。\n"
                        + "3. 不夸大结论，不做超出数据的推断。\n\n"
                        + "练习：把前两章的分析整理成一页报告，包含背景、三张图、三条结论和两条建议。")
        );
    }

    private List<ChapterContent> llmContent() {
        return List.of(
                new ChapterContent("大模型应用开发", "大模型与提示词基础",
                        "本章目标：理解大模型的能力边界，会写出稳定的提示词。\n\n"
                        + "一、基本原理\n"
                        + "1. 大模型按概率逐字生成文本，本质是根据上下文预测下一个词。\n"
                        + "2. 上下文窗口有限，超长内容需要截断或摘要。\n"
                        + "3. 存在幻觉：会生成看似合理但不正确的内容，关键场景必须校验。\n\n"
                        + "二、提示词设计\n"
                        + "1. 结构：角色 + 任务 + 输入 + 约束 + 输出格式。\n"
                        + "2. 明确输出格式（如 JSON）能显著提升可用性。\n"
                        + "3. 给 1 到 3 个示例（few-shot）通常比反复强调规则更有效。\n"
                        + "4. 推理类任务可以要求模型先分析再回答。\n\n"
                        + "三、参数\n"
                        + "1. temperature 越低越稳定，越高越发散；结构化输出建议 0 到 0.3。\n"
                        + "2. max_tokens 控制长度，top_p 与 temperature 一般只调一个。\n\n"
                        + "练习：为「根据岗位要求推荐学习内容」写一个提示词，要求输出固定 JSON 字段并包含 2 个示例。"),
                new ChapterContent("大模型应用开发", "大模型接口调用",
                        "本章目标：能通过代码调用大模型接口并处理异常。\n\n"
                        + "一、调用方式\n"
                        + "1. 主流平台都提供兼容 OpenAI 的 HTTP 接口，可以用 requests 或 httpx 调用。\n"
                        + "2. 请求体一般包含 model、messages、temperature 等字段。\n"
                        + "3. 密钥必须放在环境变量或配置中心，绝不能写进前端代码或提交到仓库。\n\n"
                        + "二、消息结构\n"
                        + "1. messages 是对话数组，role 取 system、user 或 assistant。\n"
                        + "2. system 用来设定角色和全局约束，user 是用户输入，assistant 是历史回复。\n"
                        + "3. 多轮对话要把历史消息一起传回，否则模型没有记忆。\n\n"
                        + "三、流式与异常\n"
                        + "1. stream=True 可以逐字返回，显著改善等待体验。\n"
                        + "2. 必须处理超时、限流 429、余额不足和服务端错误，并做重试与降级。\n"
                        + "3. 统计 token 用量，控制调用成本。\n\n"
                        + "练习：写一个函数调用大模型接口，支持流式输出，并在超时和 429 时自动重试一次。"),
                new ChapterContent("大模型应用开发", "对话应用与工具调用",
                        "本章目标：把大模型接进真实应用，并让它能调用外部能力。\n\n"
                        + "一、对话应用\n"
                        + "1. 会话要持久化，每次请求带上最近若干轮历史。\n"
                        + "2. 历史过长时做摘要压缩，保留关键信息。\n"
                        + "3. 前端要做流式渲染、停止生成和错误重试。\n\n"
                        + "二、检索增强（RAG）\n"
                        + "1. 把文档切块后向量化，存入向量库。\n"
                        + "2. 提问时先检索最相关的片段，拼进提示词再让模型回答。\n"
                        + "3. 好处是能基于自己的资料回答，并给出引用来源，减少幻觉。\n\n"
                        + "三、工具调用\n"
                        + "1. 定义工具的名称、描述和参数结构，由模型决定调用哪个。\n"
                        + "2. 后端执行工具并把结果回传给模型，由模型生成最终回答。\n"
                        + "3. 敏感操作（付款、删除）必须加人工确认，不能全交给模型。\n\n"
                        + "练习：给学习平台做一个课程问答助手，要求先检索课程章节内容再回答，并输出引用的章节标题。")
        );
    }

    private List<ChapterContent> vectorDeployContent() {
        return List.of(
                new ChapterContent("向量检索与服务部署", "向量与语义检索",
                        "本章目标：理解向量检索的原理，并知道它和关键词检索的区别。\n\n"
                        + "一、为什么需要向量\n"
                        + "1. 关键词检索只能匹配字面，问「怎么提升网速」和文档里的「网络延迟优化」匹配不上。\n"
                        + "2. 向量把文本映射成高维数字，语义相近的文本距离更近。\n"
                        + "3. 常用相似度：余弦相似度、点积、欧氏距离。\n\n"
                        + "二、文本向量化\n"
                        + "1. 用嵌入模型把文本转成向量，常见维度是 768 或 1536。\n"
                        + "2. 同一批数据必须用同一个模型生成向量，换模型要全量重建。\n"
                        + "3. 长文本要按语义切块，块太大检索不准，太小会丢上下文。\n\n"
                        + "三、检索质量\n"
                        + "1. 关注召回率（该找到的有没有找到）和准确率（找到的是不是相关）。\n"
                        + "2. 混合检索：向量检索与关键词检索配合，效果通常更好。\n"
                        + "3. 用重排序对初步结果再排序，提升前几条的准确度。\n\n"
                        + "练习：把一门课的章节切块并向量化，实现「输入问题返回最相关的 3 个章节」。"),
                new ChapterContent("向量检索与服务部署", "向量库使用",
                        "本章目标：会用向量数据库存储和检索向量。\n\n"
                        + "一、选型\n"
                        + "1. 轻量方案：FAISS（本地文件）、Chroma（易上手）。\n"
                        + "2. 生产方案：Milvus、Qdrant、pgvector（已有 PostgreSQL 时最省事）。\n"
                        + "3. 数据量小于 10 万条时多数方案都能满足，不必过度设计。\n\n"
                        + "二、核心操作\n"
                        + "1. 建集合：指定维度与距离度量（余弦或欧氏）。\n"
                        + "2. 写入：向量、原始文本、元数据（课程 id、章节 id）一起存。\n"
                        + "3. 检索：传入查询向量，返回 top-k 结果和相似度分数。\n\n"
                        + "三、工程要点\n"
                        + "1. 元数据过滤能大幅提升准确度，比如限定「只在某一门课里检索」。\n"
                        + "2. 记录向量模型版本，方便后续平滑升级。\n"
                        + "3. 定期评估检索效果，不要上线后再也不看。\n\n"
                        + "练习：用向量库存入课程章节，并实现带课程过滤的检索接口。"),
                new ChapterContent("向量检索与服务部署", "服务部署与运维",
                        "本章目标：能把服务部署到服务器上并做基本运维。\n\n"
                        + "一、打包与运行\n"
                        + "1. 后端打包成 jar 用 java -jar 启动；前端构建成静态文件交给 Nginx。\n"
                        + "2. 用 systemd 或 Docker 托管进程，保证开机自启和异常重启。\n"
                        + "3. 环境变量与密钥通过配置文件或密钥管理注入，不打进镜像。\n\n"
                        + "二、Nginx 常见配置\n"
                        + "1. 静态资源直接返回，/api 反向代理到后端。\n"
                        + "2. 前端 history 路由需要 try_files 回退到 index.html。\n"
                        + "3. 配置 gzip 和静态资源缓存头，减少带宽消耗。\n\n"
                        + "三、监控与排查\n"
                        + "1. 日志分级输出到文件并按天切割，避免磁盘被打满。\n"
                        + "2. 关注 CPU、内存、磁盘、接口响应时间和错误率。\n"
                        + "3. 出问题先看日志和监控再考虑重启，别用重启掩盖问题。\n"
                        + "4. 上线前准备回滚方案，新版本出问题能快速切回。\n\n"
                        + "练习：把课程服务用 Docker 部署到本机，配置 Nginx 反向代理，并验证刷新子路由页面不会 404。")
        );
    }

    private List<ChapterContent> pytorchContent() {
        return List.of(
                new ChapterContent("PyTorch 深度学习入门", "数学与统计基础",
                        "本章目标：补齐深度学习需要的数学与统计基础。\n\n"
                        + "一、线性代数\n"
                        + "1. 标量、向量、矩阵、张量的概念与维度表示。\n"
                        + "2. 矩阵乘法、转置、逆矩阵，以及它们在神经网络中的含义。\n"
                        + "3. 特征值分解与奇异值分解的用途（降维、推荐）。\n\n"
                        + "二、概率与统计\n"
                        + "1. 随机变量、期望、方差、标准差。\n"
                        + "2. 常用分布：正态分布、均匀分布、伯努利分布。\n"
                        + "3. 条件概率与贝叶斯公式的直观理解。\n\n"
                        + "三、微积分\n"
                        + "1. 导数表示变化率，偏导数用于多变量函数。\n"
                        + "2. 梯度指向函数增长最快的方向，梯度下降就是沿负梯度更新参数。\n"
                        + "3. 链式法则是反向传播的基础。\n\n"
                        + "练习：用 numpy 手写一次简单函数的梯度下降，观察学习率过大和过小时的区别。"),
                new ChapterContent("PyTorch 深度学习入门", "PyTorch 张量与模型",
                        "本章目标：掌握张量操作，并能搭起一个简单模型。\n\n"
                        + "一、张量\n"
                        + "1. 创建：torch.tensor、torch.zeros、torch.randn。\n"
                        + "2. 形状操作：view 与 reshape、squeeze 与 unsqueeze、transpose。\n"
                        + "3. 设备管理：把张量和模型都放到同一设备上，如 .to('cuda')。\n\n"
                        + "二、自动求导\n"
                        + "1. 设置 requires_grad=True 后，PyTorch 会记录计算图。\n"
                        + "2. 调用 loss.backward() 自动求梯度，结果存在张量的 grad 里。\n"
                        + "3. 每轮训练要 zero_grad() 清空梯度，否则梯度会累加。\n\n"
                        + "三、模型定义\n"
                        + "1. 继承 nn.Module，在 __init__ 里定义层，在 forward 里定义前向计算。\n"
                        + "2. 常用层：nn.Linear、nn.Conv2d、nn.ReLU、nn.Dropout。\n"
                        + "3. 损失函数：回归用 MSELoss，分类用 CrossEntropyLoss。\n\n"
                        + "练习：用 nn.Linear 搭一个线性回归模型，拟合 y = 2x + 1 并打印训练后的参数。"),
                new ChapterContent("PyTorch 深度学习入门", "神经网络训练实践",
                        "本章目标：能完整跑通一次训练，并判断模型是否正常。\n\n"
                        + "一、训练流程\n"
                        + "1. 准备数据、定义模型、定义损失和优化器，然后循环前向、计算损失、反向、更新参数。\n"
                        + "2. 优化器常用 Adam 或 SGD，学习率是最重要的超参数。\n"
                        + "3. 每个 epoch 记录训练损失和验证损失。\n\n"
                        + "二、划分数据\n"
                        + "1. 训练集、验证集、测试集，比例通常 6:2:2 或 8:1:1。\n"
                        + "2. 验证集用来调参，测试集只在最后评估一次。\n"
                        + "3. 数据量小容易过拟合，用 Dropout、权重衰减和早停缓解。\n\n"
                        + "三、诊断\n"
                        + "1. 训练损失下降但验证损失上升，说明过拟合。\n"
                        + "2. 两者都不下降，可能是学习率不当、模型太小或数据有问题。\n"
                        + "3. 损失变成 NaN，通常是学习率过大或数据未归一化。\n\n"
                        + "练习：训练一个手写数字分类模型，画出损失曲线并判断是否过拟合。")
        );
    }

    private List<ChapterContent> productContent() {
        return List.of(
                new ChapterContent("产品需求与原型设计", "需求分析与产品规划",
                        "本章目标：能把一个模糊想法整理成清晰的需求。\n\n"
                        + "一、需求来源\n"
                        + "1. 用户反馈、数据分析、竞品调研、业务目标。\n"
                        + "2. 区分用户说的和用户真正需要的，多问几个为什么。\n"
                        + "3. 需求要能对应到具体场景：谁、在什么情况下、要完成什么。\n\n"
                        + "二、需求描述\n"
                        + "1. 用户故事：作为某角色，我希望某功能，以便获得某价值。\n"
                        + "2. 验收标准要可验证，避免「界面友好」这类无法验收的描述。\n"
                        + "3. 用流程图或状态图说清主流程和异常流程。\n\n"
                        + "三、优先级\n"
                        + "1. 用 KANO 模型区分基本型、期望型、兴奋型需求。\n"
                        + "2. 用 RICE（触达 × 影响 × 信心 ÷ 成本）排序，避免拍脑袋。\n"
                        + "3. 明确本期做什么、不做什么，写清「不做」往往比写「要做」更重要。\n\n"
                        + "练习：为校园二手交易功能写 5 条用户故事，并用 RICE 排出优先级。"),
                new ChapterContent("产品需求与原型设计", "用户研究与竞品分析",
                        "本章目标：用研究结论支撑产品决策，而不是靠猜测。\n\n"
                        + "一、用户研究\n"
                        + "1. 常见方法：用户访谈、问卷、可用性测试、数据分析。\n"
                        + "2. 访谈要问过去的行为，不要问「你会不会用」这类假设性问题。\n"
                        + "3. 问卷样本要有代表性，注意幸存者偏差。\n"
                        + "4. 定量看规模，定性看原因，两者结合。\n\n"
                        + "二、竞品分析\n"
                        + "1. 先明确目的：找差距、找机会还是验证方案。\n"
                        + "2. 对比维度：目标用户、核心功能、交互路径、商业模式。\n"
                        + "3. 输出结论，而不只是罗列功能清单。\n\n"
                        + "三、用户画像与旅程\n"
                        + "1. 画像要基于真实数据，不要编造空泛的人物设定。\n"
                        + "2. 用户旅程图标注每个环节的行为、痛点和情绪。\n"
                        + "3. 痛点密集的环节就是优先改进点。\n\n"
                        + "练习：找 2 个同类校园产品做竞品分析，输出一张对比表和一个改进建议。"),
                new ChapterContent("产品需求与原型设计", "原型与交互设计",
                        "本章目标：能把需求画成可讨论、可测试的原型。\n\n"
                        + "一、原型层级\n"
                        + "1. 低保真线框图：只关心结构和信息层级，不纠结样式。\n"
                        + "2. 中高保真原型：加上视觉和交互，用于评审和可用性测试。\n"
                        + "3. 工具：Figma、Axure、墨刀，团队协作优先选在线工具。\n\n"
                        + "二、交互设计原则\n"
                        + "1. 一致性：同类操作在系统各处保持相同位置和反馈。\n"
                        + "2. 可见性：用户随时知道自己在哪、能做什么、刚才做了什么。\n"
                        + "3. 反馈及时：操作后 100 毫秒内要有视觉响应，超过 1 秒要给加载态。\n"
                        + "4. 容错：危险操作要二次确认，并尽量支持撤销。\n\n"
                        + "三、交付\n"
                        + "1. 标注间距、字号、颜色，附上交互说明和边界状态。\n"
                        + "2. 补充空状态、加载态、错误态，这三类最容易被遗漏。\n"
                        + "3. 交付前自己走一遍主流程，确认没有断点。\n\n"
                        + "练习：为校园二手交易的商品详情页画一版线框图，并补充空状态与加载状态。")
        );
    }    private record ChapterContent(String courseName, String chapterTitle, String content) { }
}