package com.example.appbackend.controller;

import com.example.appbackend.dto.BusinessSandboxRoomDTO;
import com.example.appbackend.entity.Result;
import com.example.appbackend.exception.BusinessException;
import com.example.appbackend.service.BusinessSandboxRoomService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/app/business-sandbox/rooms")
public class BusinessSandboxRoomController {
    private final BusinessSandboxRoomService service;

    public BusinessSandboxRoomController(BusinessSandboxRoomService service) {
        this.service = service;
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody BusinessSandboxRoomDTO.CreateRoomRequest body, HttpServletRequest request) {
        return Result.success(service.createRoom(userId(request), body));
    }

    @GetMapping("/my")
    public Result<List<Map<String, Object>>> myRooms(HttpServletRequest request) {
        return Result.success(service.listMyRooms(userId(request)));
    }

    @GetMapping("/{roomId}")
    public Result<Map<String, Object>> detail(@PathVariable Long roomId, HttpServletRequest request) {
        return Result.success(service.getRoom(roomId, userId(request)));
    }

    @PostMapping("/{roomId}/companies")
    public Result<Map<String, Object>> createCompany(@PathVariable Long roomId, @RequestBody BusinessSandboxRoomDTO.CreateCompanyRequest body, HttpServletRequest request) {
        return Result.success(service.createCompany(roomId, userId(request), body));
    }

    @PostMapping("/{roomId}/join")
    public Result<Map<String, Object>> joinCompany(@PathVariable Long roomId, @RequestBody BusinessSandboxRoomDTO.JoinCompanyRequest body, HttpServletRequest request) {
        return Result.success(service.joinCompany(roomId, userId(request), body));
    }

    @PutMapping("/{roomId}/companies/{companyId}/draft")
    public Result<Map<String, Object>> saveDraft(@PathVariable Long roomId, @PathVariable String companyId,
                                                  @RequestBody BusinessSandboxRoomDTO.DraftRequest body,
                                                  HttpServletRequest request) {
        return Result.success(service.saveDraft(roomId, companyId, userId(request), body));
    }

    @PostMapping("/{roomId}/companies/{companyId}/confirm")
    public Result<Map<String, Object>> confirmCompany(@PathVariable Long roomId, @PathVariable String companyId,
                                                       @RequestBody(required = false) BusinessSandboxRoomDTO.ConfirmRequest body,
                                                       HttpServletRequest request) {
        return Result.success(service.confirmCompany(roomId, companyId, userId(request), body));
    }

    @PostMapping("/{roomId}/start")
    public Result<Map<String, Object>> start(@PathVariable Long roomId, HttpServletRequest request) {
        return Result.success(service.startRoom(roomId, userId(request)));
    }

    @PostMapping("/{roomId}/settle")
    public Result<Map<String, Object>> settle(@PathVariable Long roomId, HttpServletRequest request) {
        return Result.success(service.settleRoom(roomId, userId(request)));
    }

    private Long userId(HttpServletRequest request) {
        Object value = request.getAttribute("userId");
        if (value instanceof Number number) return number.longValue();
        throw new BusinessException(Result.UNAUTHORIZED_CODE, "请先登录");
    }
}