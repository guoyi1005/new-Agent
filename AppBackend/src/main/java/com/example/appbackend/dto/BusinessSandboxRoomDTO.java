package com.example.appbackend.dto;

import java.util.Map;

public final class BusinessSandboxRoomDTO {
    private BusinessSandboxRoomDTO() {}

    public record CreateRoomRequest(String scenarioId, String name, Integer maxCompanies, Integer maxMembers) {}
    public record CreateCompanyRequest(String companyName) {}
    public record JoinCompanyRequest(Long companyId, String role) {}
    public record DraftRequest(String role, Map<String, Object> decision) {}
    public record ConfirmRequest(Boolean confirmed) {}
}