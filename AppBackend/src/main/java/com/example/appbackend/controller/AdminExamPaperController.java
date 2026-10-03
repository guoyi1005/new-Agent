package com.example.appbackend.controller;

import com.example.appbackend.dto.ExamPaperDTO.DownloadContent;
import com.example.appbackend.dto.ExamPaperDTO.PaperVO;
import com.example.appbackend.dto.PageResponse;
import com.example.appbackend.entity.Result;
import com.example.appbackend.exception.BusinessException;
import com.example.appbackend.service.ExamPaperCourseResolver;
import com.example.appbackend.service.ExamPaperService;
import com.example.appbackend.service.ExamPaperService.DownloadFile;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 管理端试卷接口：与用户端 /api/exam/papers 区分开。
 * 用户端只能看自己创建的卷子，管理端可以看平台上的全部试卷（包括课程配套试卷）。
 */
@RestController
@RequestMapping("/api/admin/exam-papers")
public class AdminExamPaperController {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final MediaType DOCX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    private static final String ILLEGAL_FILENAME_CHARACTERS = "[\\\\/:*?\"<>|\\r\\n]";

    private final ExamPaperService examPaperService;
    private final ExamPaperCourseResolver courseResolver;

    public AdminExamPaperController(ExamPaperService examPaperService,
                                    ExamPaperCourseResolver courseResolver) {
        this.examPaperService = examPaperService;
        this.courseResolver = courseResolver;
    }

    @GetMapping
    public Result<PageResponse<PaperVO>> list(@RequestParam(defaultValue = "1") Integer current,
                                              @RequestParam(defaultValue = "10") Integer size,
                                              @RequestParam(required = false) String keyword,
                                              HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        PageResponse<PaperVO> page = examPaperService.listAll(current, size, keyword);
        attachSourceCourse(page.getRecords());
        return Result.success(page);
    }

    @GetMapping("/{id}")
    public Result<PaperVO> detail(@PathVariable Long id, HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        PaperVO paper = examPaperService.detailAsAdmin(id);
        attachSourceCourse(List.of(paper));
        return Result.success(paper);
    }

    @PostMapping("/{id}/publish")
    public Result<PaperVO> publish(@PathVariable Long id, HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        return Result.success("发布成功", examPaperService.publishAsAdmin(id));
    }

    @PostMapping("/{id}/unpublish")
    public Result<PaperVO> unpublish(@PathVariable Long id, HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        return Result.success("取消发布成功", examPaperService.unpublishAsAdmin(id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id,
                                           @RequestParam(required = false) String content,
                                           HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        DownloadContent downloadContent = parseContent(content);
        DownloadFile file = examPaperService.downloadAsAdmin(id, downloadContent);
        String filename = sanitizeFilename(file.title()) + "-" + id
                + (downloadContent == DownloadContent.ANSWER ? "-答案.docx" : "-试卷.docx");
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(DOCX_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.bytes());
    }

    private void attachSourceCourse(List<PaperVO> papers) {
        if (papers == null || papers.isEmpty()) {
            return;
        }
        Map<Long, String> namesByPaperId = courseResolver.courseNamesByPaperId(
                papers.stream().map(PaperVO::getId).toList());
        if (namesByPaperId.isEmpty()) {
            return;
        }
        for (PaperVO paper : papers) {
            paper.setSourceCourseName(namesByPaperId.get(paper.getId()));
        }
    }

    private DownloadContent parseContent(String content) {
        if (content == null) {
            throw new BusinessException(Result.BAD_REQUEST_CODE, "content 仅支持 paper 或 answer");
        }
        try {
            return DownloadContent.valueOf(content.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(Result.BAD_REQUEST_CODE, "content 仅支持 paper 或 answer");
        }
    }

    private String sanitizeFilename(String title) {
        String sanitized = title == null ? "" : title.replaceAll(ILLEGAL_FILENAME_CHARACTERS, "_").trim();
        return sanitized.isEmpty() ? "试卷" : sanitized;
    }

    private void requireAdmin(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(Result.UNAUTHORIZED_CODE, "请先登录");
        }
        if (!ROLE_ADMIN.equals(request.getAttribute("role"))) {
            throw new BusinessException(Result.FORBIDDEN_CODE, "仅管理员可管理试卷");
        }
    }
}
