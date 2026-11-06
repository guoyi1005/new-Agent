package com.example.appbackend.service;

import com.example.appbackend.entity.AiModelConfig;
import com.example.appbackend.entity.SystemConfig;
import com.example.appbackend.exception.InterviewServiceException;
import com.example.appbackend.repository.AiModelConfigRepository;
import com.example.appbackend.repository.SystemConfigRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class InterviewDeepSeekService {

    private static final String DEFAULT_BASE_URL = "https://api.deepseek.com";
    private static final String DEFAULT_MODEL = "deepseek-chat";
    private static final String TEXT_CONFIG_PREFIX = "ai.service.text.";
    private static final Set<String> TEXT_MODEL_FIELDS = Set.of("provider", "base-url", "api-key", "model");
    private static final String TESTED_FINGERPRINT_FIELD = "tested-fingerprint";
    private static final int DEFAULT_MAX_TOKENS = 320;
    private static final double DEFAULT_TEMPERATURE = 0.55;

    private final Environment environment;
    private final SystemConfigService systemConfigService;
    private final AiModelConfigRepository aiModelConfigRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final ObjectMapper objectMapper;

    public InterviewDeepSeekService(Environment environment,
                                    SystemConfigService systemConfigService,
                                    AiModelConfigRepository aiModelConfigRepository,
                                    SystemConfigRepository systemConfigRepository,
                                    ObjectMapper objectMapper) {
        this.environment = environment;
        this.systemConfigService = systemConfigService;
        this.aiModelConfigRepository = aiModelConfigRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> status() {
        ResolvedAiConfig config = resolveAiConfig();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("configured", StringUtils.hasText(config.apiKey()));
        result.put("source", config.source());
        result.put("base_url", config.baseUrl());
        result.put("model", config.model());
        return result;
    }

    public String chat(List<Map<String, String>> messages) {
        return chat(messages, DEFAULT_MAX_TOKENS, DEFAULT_TEMPERATURE);
    }

    /**
     * 生成面试问题等短文本时使用默认预算；
     * 生成评估报告这类长结构化结果时必须显式放大 maxTokens，否则 JSON 会被截断。
     */
    public String chat(List<Map<String, String>> messages, int maxTokens, double temperature) {
        ResolvedAiConfig config = resolveAiConfig();
        if (!StringUtils.hasText(config.apiKey())) {
            throw new InterviewServiceException("interview_ai_not_configured", 503);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", config.model());
        payload.put("messages", messages);
        payload.put("temperature", temperature);
        payload.put("max_tokens", maxTokens);
        payload.put("stream", false);

        try {
            String raw = WebClient.builder()
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.apiKey())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build()
                    .post()
                    .uri(resolveChatCompletionsUrl(config.baseUrl()))
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(40))
                    .block();

            JsonNode root = objectMapper.readTree(raw);
            String content = root.path("choices").path(0).path("message").path("content").asText("").trim();
            if (!StringUtils.hasText(content)) {
                throw new InterviewServiceException("interview_ai_empty_response", 502);
            }
            return content;
        } catch (WebClientResponseException error) {
            int status = error.getStatusCode().value();
            if (status == 401 || status == 403) {
                throw new InterviewServiceException("interview_ai_unauthorized", 502);
            }
            if (status == 429) {
                throw new InterviewServiceException("interview_ai_rate_limited", 503);
            }
            throw new InterviewServiceException("interview_ai_upstream_error", 502,
                    Map.of("upstream_status", status));
        } catch (InterviewServiceException error) {
            throw error;
        } catch (Exception error) {
            throw new InterviewServiceException("interview_ai_request_failed", 502);
        }
    }

    /**
     * 面试链路优先使用显式环境变量，其次复用后台已保存的 DeepSeek 模型配置。
     * 多个完整配置同时存在时，测试成功的优先，其次按配置前缀排序，保证结果稳定。
     */
    private ResolvedAiConfig resolveAiConfig() {
        String envApiKey = firstNonBlank(
                environment.getProperty("DEEPSEEK_API_KEY"),
                environment.getProperty("interview.deepseek.api-key")
        );
        if (StringUtils.hasText(envApiKey)) {
            return new ResolvedAiConfig(
                    envApiKey,
                    firstNonBlank(
                            environment.getProperty("DEEPSEEK_BASE_URL"),
                            environment.getProperty("interview.deepseek.base-url"),
                            DEFAULT_BASE_URL
                    ),
                    firstNonBlank(
                            environment.getProperty("DEEPSEEK_MODEL"),
                            environment.getProperty("interview.deepseek.model"),
                            DEFAULT_MODEL
                    ),
                    "env"
            );
        }

        Optional<AiModelConfig> adminConfig = adminConfiguredDeepSeek();
        if (adminConfig.isPresent()) {
            AiModelConfig config = adminConfig.get();
            return new ResolvedAiConfig(
                    config.getApiKey(),
                    firstNonBlank(config.getBaseUrl(), DEFAULT_BASE_URL),
                    firstNonBlank(config.getModelName(), DEFAULT_MODEL),
                    "database"
            );
        }

        Optional<SystemTextModelConfig> textConfig = systemConfiguredDeepSeek();
        if (textConfig.isPresent()) {
            SystemTextModelConfig config = textConfig.get();
            return new ResolvedAiConfig(
                    config.apiKey(),
                    firstNonBlank(config.baseUrl(), DEFAULT_BASE_URL),
                    firstNonBlank(config.model(), DEFAULT_MODEL),
                    "system_config"
            );
        }

        // 兼容旧版本只维护 ai.service.text.deepseek-chat.api-key 的部署。
        String legacyPrefix = TEXT_CONFIG_PREFIX + "deepseek-chat";
        String legacyApiKey = systemConfigService.getValue(legacyPrefix + ".api-key", "");
        if (StringUtils.hasText(legacyApiKey)) {
            return new ResolvedAiConfig(
                    legacyApiKey,
                    firstNonBlank(
                            systemConfigService.getValue(legacyPrefix + ".base-url", ""),
                            DEFAULT_BASE_URL
                    ),
                    firstNonBlank(
                            systemConfigService.getValue(legacyPrefix + ".model", ""),
                            DEFAULT_MODEL
                    ),
                    "system_config"
            );
        }

        String legacyDefaultApiKey = isConfiguredDeepSeekDefault()
                ? systemConfigService.getValue("ai.service.text.api-key", "")
                : "";
        return new ResolvedAiConfig(
                legacyDefaultApiKey,
                DEFAULT_BASE_URL,
                DEFAULT_MODEL,
                StringUtils.hasText(legacyDefaultApiKey) ? "system_config" : "none"
        );
    }

    /**
     * 从启用的 ai.service.text.* 配置中查找字段完整的 DeepSeek 配置。
     * 管理端会按模型 ID 生成前缀，因此不能只识别固定的 deepseek-chat。
     */
    private Optional<SystemTextModelConfig> systemConfiguredDeepSeek() {
        Map<String, Map<String, String>> valuesByPrefix = new HashMap<>();
        for (SystemConfig item : systemConfigRepository.findByConfigKeyStartingWithAndStatus(TEXT_CONFIG_PREFIX, 1)) {
            String prefix = extractTextModelPrefix(item.getConfigKey());
            String field = extractTextModelField(item.getConfigKey());
            if (StringUtils.hasText(prefix)
                    && StringUtils.hasText(field)
                    && StringUtils.hasText(item.getConfigValue())) {
                valuesByPrefix
                        .computeIfAbsent(prefix, ignored -> new HashMap<>())
                        .put(field, item.getConfigValue().trim());
            }
        }

        return valuesByPrefix.entrySet().stream()
                .filter(entry -> entry.getValue().keySet().containsAll(TEXT_MODEL_FIELDS))
                .map(entry -> new SystemTextModelConfig(
                        entry.getKey(),
                        entry.getValue().get("provider"),
                        entry.getValue().get("api-key"),
                        entry.getValue().get("base-url"),
                        entry.getValue().get("model"),
                        entry.getValue().containsKey(TESTED_FINGERPRINT_FIELD)
                ))
                .filter(this::isDeepSeekTextModel)
                .sorted(Comparator
                        .comparing(SystemTextModelConfig::tested)
                        .reversed()
                        .thenComparing(SystemTextModelConfig::prefix))
                .findFirst();
    }

    private String extractTextModelPrefix(String configKey) {
        if (!StringUtils.hasText(configKey) || !configKey.startsWith(TEXT_CONFIG_PREFIX)) {
            return "";
        }
        int separator = configKey.lastIndexOf('.');
        if (separator <= TEXT_CONFIG_PREFIX.length()) {
            return "";
        }
        return configKey.substring(0, separator);
    }

    private String extractTextModelField(String configKey) {
        if (!StringUtils.hasText(configKey)) {
            return "";
        }
        int separator = configKey.lastIndexOf('.');
        return separator < 0 ? "" : configKey.substring(separator + 1);
    }

    private boolean isDeepSeekTextModel(SystemTextModelConfig config) {
        String haystack = String.join(" ", config.provider(), config.baseUrl(), config.model())
                .toLowerCase(Locale.ROOT);
        return haystack.contains("deepseek");
    }

    /**
     * 管理端配置的模型（ai_model_config.api_key 由 EncryptedStringConverter 加密落库）。
     * 只有形如 DeepSeek 的配置才会被面试官复用，避免把面试内容发给无关的服务商。
     */
    private Optional<AiModelConfig> adminConfiguredDeepSeek() {
        return aiModelConfigRepository.findAll().stream()
                .filter(config -> config.getStatus() != null && config.getStatus() == 1)
                .filter(this::isDeepSeekConfig)
                .filter(config -> StringUtils.hasText(config.getApiKey()))
                .findFirst();
    }

    private boolean isDeepSeekConfig(AiModelConfig config) {
        String haystack = String.join(" ",
                String.valueOf(config.getProvider()),
                String.valueOf(config.getBaseUrl()),
                String.valueOf(config.getModelName())).toLowerCase(Locale.ROOT);
        return haystack.contains("deepseek");
    }

    private String resolveChatCompletionsUrl(String baseUrl) {
        return firstNonBlank(baseUrl, DEFAULT_BASE_URL).replaceAll("/+$", "") + "/chat/completions";
    }

    private boolean isConfiguredDeepSeekDefault() {
        String provider = systemConfigService.getValue("ai.service.text.provider", "");
        String model = systemConfigService.getValue("ai.service.text.model", "");
        return (provider + " " + model).toLowerCase().contains("deepseek");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private record ResolvedAiConfig(String apiKey, String baseUrl, String model, String source) {
    }

    private record SystemTextModelConfig(
            String prefix,
            String provider,
            String apiKey,
            String baseUrl,
            String model,
            boolean tested
    ) {
    }
}
