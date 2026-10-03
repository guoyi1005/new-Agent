package com.example.appbackend.controller;

import com.example.appbackend.dto.PythonPaperDTO;
import com.example.appbackend.entity.PythonPaper;
import com.example.appbackend.entity.Result;
import com.example.appbackend.exception.BusinessException;
import com.example.appbackend.repository.PythonPaperRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 学生端 Python 练习卷：保存生成的试卷，便于之后回看与打印。
 *
 * 试卷只保存题目 ID 快照，题目正文仍从 python_problem 读取，
 * 因此题库更新后不会出现内容不一致的副本。
 */
@RestController
@RequestMapping("/api/app/learning/python/papers")
public class AppPythonPaperController {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final PythonPaperRepository paperRepository;
    private final ObjectMapper objectMapper;

    public AppPythonPaperController(PythonPaperRepository paperRepository, ObjectMapper objectMapper) {
        this.paperRepository = paperRepository;
        this.objectMapper = objectMapper;
    }

    /** 保存一份试卷。 */
    @PostMapping
    public Result<PythonPaperDTO.PaperView> save(@RequestBody PythonPaperDTO.CreateRequest request,
                                                 HttpServletRequest httpRequest) {
        Long userId = requireUserId(httpRequest);
        if (request == null || request.getQuestionIds() == null || request.getQuestionIds().isEmpty()) {
            throw new BusinessException(400, "请先生成试卷再保存");
        }
        PythonPaper paper = new PythonPaper();
        paper.setUserId(userId);
        paper.setTitle(StringUtils.hasText(request.getTitle())
                ? request.getTitle().trim() : "Python 练习卷");
        paper.setDifficulty(StringUtils.hasText(request.getDifficulty())
                ? request.getDifficulty().trim() : "all");
        paper.setTagsJson(writeJson(request.getTags()));
        paper.setQuestionIdsJson(writeJson(request.getQuestionIds()));
        paper.setQuestionCount(request.getQuestionIds().size());
        paper.setTotalScore(request.getQuestionIds().size() * PythonPaper.SCORE_PER_QUESTION);
        return Result.success(view(paperRepository.save(paper)));
    }

    /** 我保存过的试卷。 */
    @GetMapping
    public Result<List<PythonPaperDTO.PaperView>> list(HttpServletRequest request) {
        Long userId = requireUserId(request);
        List<PythonPaperDTO.PaperView> views = new ArrayList<>();
        for (PythonPaper paper : paperRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId)) {
            views.add(view(paper));
        }
        return Result.success(views);
    }

    /** 单份试卷详情。 */
    @GetMapping("/{id}")
    public Result<PythonPaperDTO.PaperView> detail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = requireUserId(request);
        PythonPaper paper = paperRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND_CODE, "试卷不存在"));
        return Result.success(view(paper));
    }

    /** 删除一份试卷（只能删除自己的）。 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = requireUserId(request);
        PythonPaper paper = paperRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND_CODE, "试卷不存在"));
        paperRepository.delete(paper);
        return Result.success();
    }

    private PythonPaperDTO.PaperView view(PythonPaper paper) {
        PythonPaperDTO.PaperView view = new PythonPaperDTO.PaperView();
        view.setId(paper.getId());
        view.setTitle(paper.getTitle());
        view.setDifficulty(paper.getDifficulty());
        view.setTags(readStringList(paper.getTagsJson()));
        view.setQuestionIds(readLongList(paper.getQuestionIdsJson()));
        view.setQuestionCount(paper.getQuestionCount());
        view.setTotalScore(paper.getTotalScore());
        view.setCreatedAt(paper.getCreatedAt() == null ? null : paper.getCreatedAt().format(DATE_TIME));
        return view;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (Exception error) {
            return "[]";
        }
    }

    private List<String> readStringList(String json) {
        if (!StringUtils.hasText(json)) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() { });
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private List<Long> readLongList(String json) {
        if (!StringUtils.hasText(json)) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() { });
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private Long requireUserId(HttpServletRequest request) {
        Object raw = request.getAttribute("userId");
        if (!(raw instanceof Number number)) {
            throw new BusinessException(Result.UNAUTHORIZED_CODE, "请先登录");
        }
        return number.longValue();
    }
}
