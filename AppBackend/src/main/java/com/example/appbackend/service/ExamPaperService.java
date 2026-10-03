package com.example.appbackend.service;

import com.example.appbackend.dto.ExamPaperDTO.CreateRequest;
import com.example.appbackend.dto.ExamPaperDTO.DownloadContent;
import com.example.appbackend.dto.ExamPaperDTO.PaperVO;
import com.example.appbackend.dto.ExamPaperDTO.RandomPreviewRequest;
import com.example.appbackend.dto.PageResponse;

public interface ExamPaperService {

    PaperVO randomPreview(RandomPreviewRequest request, Long userId);

    PaperVO create(CreateRequest request, Long userId);

    PageResponse<PaperVO> list(Integer current, Integer size, String keyword, Long userId);

    /** 管理端：列表不限定创建人，返回平台上的全部试卷。 */
    PageResponse<PaperVO> listAll(Integer current, Integer size, String keyword);

    PaperVO detailAsAdmin(Long id);

    PaperVO publishAsAdmin(Long id);

    PaperVO unpublishAsAdmin(Long id);

    DownloadFile downloadAsAdmin(Long id, DownloadContent content);

    PaperVO detail(Long id, Long userId);

    PaperVO publish(Long id, Long adminUserId);

    PaperVO unpublish(Long id, Long adminUserId);

    DownloadFile download(Long id, Long userId, DownloadContent content);

    record DownloadFile(String title, byte[] bytes) {
    }
}
