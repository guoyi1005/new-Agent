package com.example.appbackend.service;

import com.example.appbackend.entity.SystemConfig;
import com.example.appbackend.repository.AiModelConfigRepository;
import com.example.appbackend.repository.SystemConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InterviewDeepSeekServiceTest {

    private Environment environment;
    private SystemConfigService systemConfigService;
    private AiModelConfigRepository aiModelConfigRepository;
    private SystemConfigRepository systemConfigRepository;
    private InterviewDeepSeekService service;

    @BeforeEach
    void setUp() {
        environment = mock(Environment.class);
        systemConfigService = mock(SystemConfigService.class);
        aiModelConfigRepository = mock(AiModelConfigRepository.class);
        systemConfigRepository = mock(SystemConfigRepository.class);
        when(aiModelConfigRepository.findAll()).thenReturn(List.of());
        service = new InterviewDeepSeekService(
                environment,
                systemConfigService,
                aiModelConfigRepository,
                systemConfigRepository,
                new ObjectMapper()
        );
    }

    @Test
    void statusUsesCompleteDeepSeekTextModelConfig() {
        when(systemConfigRepository.findByConfigKeyStartingWithAndStatus("ai.service.text.", 1))
                .thenReturn(modelConfig("ai.service.text.deepseek-v4-flash", "deepseek-v4-flash", true));

        Map<String, Object> status = service.status();

        assertTrue((Boolean) status.get("configured"));
        assertEquals("system_config", status.get("source"));
        assertEquals("https://api.deepseek.com", status.get("base_url"));
        assertEquals("deepseek-v4-flash", status.get("model"));
    }

    @Test
    void statusPrefersTestedConfigDeterministically() {
        List<SystemConfig> configs = new ArrayList<>();
        configs.addAll(modelConfig("ai.service.text.deepseek-a", "deepseek-a", false));
        configs.addAll(modelConfig("ai.service.text.deepseek-z", "deepseek-z", true));
        when(systemConfigRepository.findByConfigKeyStartingWithAndStatus("ai.service.text.", 1))
                .thenReturn(configs);

        Map<String, Object> status = service.status();

        assertTrue((Boolean) status.get("configured"));
        assertEquals("deepseek-z", status.get("model"));
    }

    @Test
    void statusIsUnconfiguredWhenOnlyIncompleteModelConfigExists() {
        when(systemConfigRepository.findByConfigKeyStartingWithAndStatus("ai.service.text.", 1))
                .thenReturn(List.of(config("ai.service.text.deepseek-v4-flash.api-key", "test-key")));

        Map<String, Object> status = service.status();

        assertFalse((Boolean) status.get("configured"));
        assertEquals("none", status.get("source"));
        assertEquals("deepseek-chat", status.get("model"));
    }

    private static List<SystemConfig> modelConfig(String prefix, String model, boolean tested) {
        List<SystemConfig> configs = new ArrayList<>(List.of(
                config(prefix + ".provider", "deepseek"),
                config(prefix + ".base-url", "https://api.deepseek.com"),
                config(prefix + ".api-key", "test-key"),
                config(prefix + ".model", model)
        ));
        if (tested) {
            configs.add(config(prefix + ".tested-fingerprint", "fingerprint"));
        }
        return configs;
    }

    private static SystemConfig config(String key, String value) {
        SystemConfig config = new SystemConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setStatus(1);
        return config;
    }
}
