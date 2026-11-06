package com.example.appbackend.service;

import com.example.appbackend.entity.Result;
import com.example.appbackend.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

/**
 * Python 判题核心服务。
 *
 * <p>The API layer passes only server-owned function names and test cases here.
 * User code still deserves a dedicated sandbox in production; this service adds
 * authentication at the controller, bounded input/output, isolated working
 * directories, scrubbed environment variables, process-tree cleanup, and
 * platform resource limits as defense in depth.</p>
 */
@Service
public class CodeJudgeService {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int MAX_TESTCASES = 20;
    private static final int MAX_INPUT_CHARS = 20_000;
    private static final int MAX_EXPECTED_CHARS = 20_000;
    private static final int MAX_OUTPUT_BYTES = 64 * 1024;
    private static final long TIMEOUT_SECONDS = 5;
    private static final Semaphore CONCURRENT_JUDGES = new Semaphore(2);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    @Value("${code-judge.python-executable:python}")
    private String pythonExecutable;

    @Value("${code-judge.remote-url:}")
    private String remoteUrl;

    @Value("${code-judge.internal-token:}")
    private String internalToken;

    @Value("${code-judge.local-fallback-enabled:false}")
    private boolean localFallbackEnabled;

    /** A server-owned judge definition loaded from PythonProblem. */
    public record JudgeSpec(String funcName, List<JudgeCase> testcases) {
    }

    /** 单个判题用例 */
    public static class JudgeCase {
        public String input;
        public String expected;
        /** 额外可接受的答案（格式同 expected），用于多解题目，任一匹配即通过 */
        public List<String> accepts;
        /** 比较模式：exact 精确匹配（默认）/ set 外层数组无序 / deepset 递归无序 */
        public String mode;
    }

    public Map<String, Object> judge(String code, String funcName, List<JudgeCase> testcases) {
        validateJudgeInput(code, funcName, testcases);
        if (!CONCURRENT_JUDGES.tryAcquire()) {
            throw new BusinessException(429, "当前判题任务较多，请稍后重试");
        }
        try {
            List<Map<String, Object>> results = new ArrayList<>();
            double totalMs = 0;
            double maxPeakMb = 0;

            for (JudgeCase tc : testcases) {
                Map<String, Object> caseResult = runOneTestcase(code, funcName, tc);
                if (caseResult.get("ms") instanceof Number number) {
                    totalMs += number.doubleValue();
                }
                if (caseResult.get("peakMb") instanceof Number number) {
                    maxPeakMb = Math.max(maxPeakMb, number.doubleValue());
                }
                caseResult.remove("ms");
                caseResult.remove("peakMb");
                results.add(caseResult);
            }

            long passedCount = results.stream()
                    .filter(result -> "pass".equals(result.get("status")))
                    .count();
            boolean allPass = passedCount == results.size();

            String status;
            String statusLabel;
            if (allPass) {
                status = "ac";
                statusLabel = "通过";
            } else if (results.stream().anyMatch(result -> "ce".equals(result.get("status")))) {
                status = "ce";
                statusLabel = "编译错误";
            } else if (results.stream().anyMatch(result -> "err".equals(result.get("status")))) {
                status = "err";
                statusLabel = "判题服务异常";
            } else if (results.stream().anyMatch(result -> "re".equals(result.get("status")))) {
                status = "re";
                statusLabel = "运行错误";
            } else if (results.stream().anyMatch(result -> "tle".equals(result.get("status")))) {
                status = "tle";
                statusLabel = "执行超时";
            } else {
                status = "wa";
                statusLabel = "解答错误";
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("status", status);
            data.put("statusLabel", statusLabel);
            data.put("runtime", Math.round(totalMs));
            data.put("memory", Math.round(maxPeakMb * 10) / 10.0);
            data.put("passed", passedCount);
            data.put("total", results.size());
            data.put("testcases", results);
            return data;
        } finally {
            CONCURRENT_JUDGES.release();
        }
    }

    private void validateJudgeInput(String code, String funcName, List<JudgeCase> testcases) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(Result.BAD_REQUEST_CODE, "代码不能为空");
        }
        if (code.length() > 50_000) {
            throw new BusinessException(Result.BAD_REQUEST_CODE, "提交代码过长");
        }
        if (funcName == null || !funcName.matches("[A-Za-z_][A-Za-z0-9_]{0,63}")) {
            throw new BusinessException(Result.BAD_REQUEST_CODE, "判题函数名配置无效");
        }
        if (testcases == null || testcases.isEmpty()) {
            throw new BusinessException(Result.BAD_REQUEST_CODE, "测试用例不能为空");
        }
        if (testcases.size() > MAX_TESTCASES) {
            throw new BusinessException(Result.BAD_REQUEST_CODE, "测试用例数量超出限制");
        }
        for (JudgeCase testcase : testcases) {
            if (testcase == null
                    || length(testcase.input) > MAX_INPUT_CHARS
                    || length(testcase.expected) > MAX_EXPECTED_CHARS) {
                throw new BusinessException(Result.BAD_REQUEST_CODE, "测试用例内容超出限制");
            }
            if (testcase.accepts != null) {
                if (testcase.accepts.size() > 20) {
                    throw new BusinessException(Result.BAD_REQUEST_CODE, "可接受答案数量超出限制");
                }
                for (String accepted : testcase.accepts) {
                    if (length(accepted) > MAX_EXPECTED_CHARS) {
                        throw new BusinessException(Result.BAD_REQUEST_CODE, "可接受答案内容超出限制");
                    }
                }
            }
        }
    }

    private int length(String value) {
        return value == null ? 0 : value.length();
    }

    private Map<String, Object> runOneTestcase(String userCode, String funcName, JudgeCase testcase) {
        Path sandboxDir = null;
        Process process = null;
        try {
            String[] parts = testcase.input == null ? new String[0]
                    : testcase.input.split(",\\s*(?=[a-zA-Z_]\\w*\\s*=)");
            StringBuilder setup = new StringBuilder();
            List<String> argNames = new ArrayList<>();
            for (String part : parts) {
                String trimmed = part.trim();
                int equalsIndex = trimmed.indexOf('=');
                if (equalsIndex > 0) {
                    String name = trimmed.substring(0, equalsIndex).trim();
                    String value = trimmed.substring(equalsIndex + 1).trim();
                    setup.append(name).append(" = ").append(value).append('\n');
                    argNames.add(name);
                }
            }

            String args = String.join(", ", argNames);
            String mode = testcase.mode == null || testcase.mode.isEmpty() ? "exact" : testcase.mode;
            String expectedLiteral = JSON.writeValueAsString(testcase.expected == null ? "" : testcase.expected);
            String acceptsLiteral = JSON.writeValueAsString(
                    testcase.accepts == null ? Collections.emptyList() : testcase.accepts);
            String modeLiteral = JSON.writeValueAsString(mode);

            String fullCode = sandboxPrelude() + """
import json, sys, io, time, tracemalloc
__real_stdout__ = sys.stdout
sys.stdout = io.StringIO()
""" + setup + userCode + """

tracemalloc.start()
__t0__ = time.perf_counter()
__result__ = """ + funcName + "(" + args + ")" + """

__ms__ = (time.perf_counter() - __t0__) * 1000.0
__peak__ = tracemalloc.get_traced_memory()[1]
tracemalloc.stop()
__actual__ = json.dumps(__result__, separators=(',', ':'), ensure_ascii=False)
__expected__ = """ + expectedLiteral + """
__accepts__ = list(""" + acceptsLiteral + """
)
__mode__ = """ + modeLiteral + """
""" + judgeHelpers() + """
__ok__ = __judge__(__actual__, [__expected__] + __accepts__, __mode__)
sys.stdout = __real_stdout__
print(json.dumps({'status': 'pass' if __ok__ else 'fail', 'actual': __actual__, 'ms': __ms__, 'peak': __peak__}, ensure_ascii=False))
""";

            if (remoteUrl != null && !remoteUrl.isBlank()) {
                try {
                    return executeInRemoteJudge(fullCode, testcase);
                } catch (Exception remoteError) {
                    if (!localFallbackEnabled) {
                        Map<String, Object> result = baseCaseResult(testcase, 0.0);
                        result.put("status", "err");
                        result.put("actual", "判题容器不可用，请确认 code-judge 服务已启动");
                        return result;
                    }
                }
            } else if (!localFallbackEnabled) {
                Map<String, Object> result = baseCaseResult(testcase, 0.0);
                result.put("status", "err");
                result.put("actual", "未配置隔离判题服务，已拒绝执行用户代码");
                return result;
            }

            sandboxDir = Files.createTempDirectory("pyjudge-");
            Path script = sandboxDir.resolve("main.py");
            Files.writeString(script, fullCode, StandardCharsets.UTF_8);

            ProcessBuilder builder = new ProcessBuilder(pythonExecutable, "-I", "-B", script.toAbsolutePath().toString());
            builder.directory(sandboxDir.toFile());
            scrubEnvironment(builder, sandboxDir);
            long startedAt = System.currentTimeMillis();
            process = builder.start();
            try {
                process.getOutputStream().close();
            } catch (Exception ignored) {
            }

            Process runningProcess = process;
            CompletableFuture<String> stdoutFuture = CompletableFuture.supplyAsync(
                    () -> readLimited(runningProcess.getInputStream(), MAX_OUTPUT_BYTES));
            CompletableFuture<String> stderrFuture = CompletableFuture.supplyAsync(
                    () -> readLimited(runningProcess.getErrorStream(), MAX_OUTPUT_BYTES));

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            long elapsed = System.currentTimeMillis() - startedAt;
            if (!finished) {
                killProcessTree(process);
                closeQuietly(process.getInputStream());
                closeQuietly(process.getErrorStream());
                awaitOutput(stdoutFuture);
                awaitOutput(stderrFuture);
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("status", "tle");
                result.put("input", truncate(testcase.input, 1000));
                result.put("expected", truncate(testcase.expected, 1000));
                result.put("actual", "执行超时（单用例限时 " + TIMEOUT_SECONDS + " 秒）");
                result.put("ms", (double) elapsed);
                return result;
            }

            killProcessTree(process);
            closeQuietly(process.getInputStream());
            closeQuietly(process.getErrorStream());
            String stdout = awaitOutput(stdoutFuture);
            String stderr = awaitOutput(stderrFuture);
            String output = stdout == null ? "" : stdout.trim();
            String error = stderr == null ? "" : stderr.trim();

            if (output.isEmpty() && !error.isEmpty()) {
                String errorMessage = lastLine(error);
                boolean compileError = errorMessage.contains("SyntaxError")
                        || errorMessage.contains("IndentationError");
                Map<String, Object> result = baseCaseResult(testcase, elapsed);
                result.put("status", compileError ? "ce" : "re");
                result.put("actual", errorMessage);
                return result;
            }

            Map<String, Object> judged = parseJudgeOutput(lastLine(output));
            if (judged == null) {
                Map<String, Object> result = baseCaseResult(testcase, elapsed);
                result.put("status", "re");
                result.put("actual", "判题结果解析失败");
                return result;
            }

            Map<String, Object> result = baseCaseResult(testcase, elapsed);
            result.put("status", "pass".equals(judged.get("status")) ? "pass" : "fail");
            result.put("actual", judged.get("actual") == null ? "" : truncate(String.valueOf(judged.get("actual")), 2000));
            if (judged.get("ms") instanceof Number number) {
                result.put("ms", number.doubleValue());
            }
            if (judged.get("peak") instanceof Number number) {
                result.put("peakMb", number.doubleValue() / 1048576.0);
            }
            return result;
        } catch (Exception error) {
            Map<String, Object> result = baseCaseResult(testcase, 0.0);
            result.put("status", "err");
            result.put("actual", "判题服务暂时不可用");
            return result;
        } finally {
            if (process != null) {
                killProcessTree(process);
            }
            deleteTree(sandboxDir);
        }
    }


    private Map<String, Object> executeInRemoteJudge(String script, JudgeCase testcase) throws Exception {
        String baseUrl = remoteUrl.endsWith("/")
                ? remoteUrl.substring(0, remoteUrl.length() - 1)
                : remoteUrl;
        String requestBody = JSON.writeValueAsString(Map.of("script", script));
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/execute"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8));
        if (internalToken != null && !internalToken.isBlank()) {
            builder.header("X-Code-Judge-Token", internalToken);
        }
        long startedAt = System.currentTimeMillis();
        HttpResponse<String> response = HTTP_CLIENT.send(
                builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        long elapsed = System.currentTimeMillis() - startedAt;
        if (response.statusCode() != 200) {
            throw new IOException("judge service returned HTTP " + response.statusCode());
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> output = JSON.readValue(response.body(), Map.class);
        return resultFromSandboxOutput(testcase, output, elapsed);
    }

    private Map<String, Object> resultFromSandboxOutput(JudgeCase testcase,
                                                        Map<String, Object> output,
                                                        long elapsed) {
        if (Boolean.TRUE.equals(output.get("timedOut"))) {
            Map<String, Object> result = baseCaseResult(testcase, elapsed);
            result.put("status", "tle");
            result.put("actual", "执行超时（单用例限时 " + TIMEOUT_SECONDS + " 秒）");
            return result;
        }
        String stdout = textValue(output.get("stdout"));
        String stderr = textValue(output.get("stderr"));
        if (stdout.isBlank() && !stderr.isBlank()) {
            String errorMessage = lastLine(stderr);
            boolean compileError = errorMessage.contains("SyntaxError")
                    || errorMessage.contains("IndentationError");
            Map<String, Object> result = baseCaseResult(testcase, elapsed);
            result.put("status", compileError ? "ce" : "re");
            result.put("actual", truncate(errorMessage, 2000));
            return result;
        }
        Map<String, Object> judged = parseJudgeOutput(lastLine(stdout));
        if (judged == null) {
            Map<String, Object> result = baseCaseResult(testcase, elapsed);
            result.put("status", "re");
            result.put("actual", "判题结果解析失败");
            return result;
        }
        Map<String, Object> result = baseCaseResult(testcase, elapsed);
        result.put("status", "pass".equals(judged.get("status")) ? "pass" : "fail");
        result.put("actual", truncate(textValue(judged.get("actual")), 2000));
        if (judged.get("ms") instanceof Number number) {
            result.put("ms", number.doubleValue());
        }
        if (judged.get("peak") instanceof Number number) {
            result.put("peakMb", number.doubleValue() / 1048576.0);
        }
        return result;
    }

    private String textValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Map<String, Object> baseCaseResult(JudgeCase testcase, double elapsedMs) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("input", truncate(testcase.input, 2000));
        result.put("expected", truncate(testcase.expected, 2000));
        result.put("ms", elapsedMs);
        return result;
    }

    private String sandboxPrelude() {
        return """
import os
try:
    import resource
    for _name, _soft, _hard in (
        ('RLIMIT_CPU', 5, 5),
        ('RLIMIT_AS', 268435456, 268435456),
        ('RLIMIT_FSIZE', 1048576, 1048576),
        ('RLIMIT_NOFILE', 32, 32),
        ('RLIMIT_NPROC', 32, 32),
    ):
        try:
            _limit = getattr(resource, _name, None)
            if _limit is not None:
                resource.setrlimit(_limit, (_soft, _hard))
        except Exception:
            pass
except ImportError:
    pass
try:
    os.environ.clear()
except Exception:
    pass
""";
    }

    private void scrubEnvironment(ProcessBuilder builder, Path sandboxDir) {
        Map<String, String> environment = builder.environment();
        String path = environment.get("PATH");
        String systemRoot = environment.get("SystemRoot");
        String windir = environment.get("WINDIR");
        environment.clear();
        if (path != null) environment.put("PATH", path);
        if (systemRoot != null) environment.put("SystemRoot", systemRoot);
        if (windir != null) environment.put("WINDIR", windir);
        environment.put("HOME", sandboxDir.toString());
        environment.put("TMPDIR", sandboxDir.toString());
        environment.put("TMP", sandboxDir.toString());
        environment.put("TEMP", sandboxDir.toString());
        environment.put("PYTHONIOENCODING", "utf-8");
        environment.put("PYTHONDONTWRITEBYTECODE", "1");
    }

    private String readLimited(InputStream input, int maxBytes) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(maxBytes, 8192));
        boolean truncated = false;
        try (InputStream stream = input) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                int remaining = maxBytes - output.size();
                if (remaining > 0) {
                    output.write(buffer, 0, Math.min(read, remaining));
                }
                if (read > remaining) {
                    truncated = true;
                }
            }
        } catch (Exception ignored) {
        }
        String text = output.toString(StandardCharsets.UTF_8);
        return truncated ? text + "\n[输出已截断]" : text;
    }

    private String awaitOutput(CompletableFuture<String> future) {
        try {
            return future.get(3, TimeUnit.SECONDS);
        } catch (TimeoutException | InterruptedException | java.util.concurrent.ExecutionException error) {
            if (error instanceof InterruptedException) Thread.currentThread().interrupt();
            future.cancel(true);
            return "";
        }
    }

    private void killProcessTree(Process process) {
        if (process == null) return;
        try {
            process.descendants().sorted(Comparator.reverseOrder()).forEach(handle -> {
                try {
                    handle.destroyForcibly();
                } catch (Exception ignored) {
                }
            });
        } catch (Exception ignored) {
        }
        try {
            process.destroyForcibly();
        } catch (Exception ignored) {
        }
    }

    private void closeQuietly(InputStream stream) {
        try {
            stream.close();
        } catch (Exception ignored) {
        }
    }

    private void deleteTree(Path directory) {
        if (directory == null || !Files.exists(directory)) return;
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ignored) {
                }
            });
        } catch (Exception ignored) {
        }
    }

    private String truncate(String value, int maxChars) {
        if (value == null) return "";
        return value.length() <= maxChars ? value : value.substring(0, maxChars) + "…";
    }

    /** Python 侧比较函数源码。判定规则见原实现。 */
    private static String judgeHelpers() {
        return """
def __key__(v):
    return json.dumps(v, separators=(',', ':'), ensure_ascii=False, sort_keys=True)
def __norm_deep__(v):
    if isinstance(v, list):
        return sorted([__norm_deep__(x) for x in v], key=__key__)
    return v
def __norm_shallow__(v):
    if isinstance(v, list):
        return sorted(v, key=__key__)
    return v
def __judge__(a, cands, mode):
    a = (a or '').strip()
    for c in cands:
        e = (c or '').strip()
        if a == e:
            return True
        if ''.join(a.split()) == ''.join(e.split()):
            return True
        try:
            av = json.loads(a)
            ev = json.loads(e)
        except Exception:
            continue
        if isinstance(av, bool) or isinstance(ev, bool):
            continue
        if isinstance(av, (int, float)) and isinstance(ev, (int, float)):
            if av == ev:
                return True
            continue
        if mode == 'deepset':
            if __norm_deep__(av) == __norm_deep__(ev):
                return True
        elif mode == 'set':
            if __norm_shallow__(av) == __norm_shallow__(ev):
                return True
    return False
""";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJudgeOutput(String line) {
        if (line == null || line.isEmpty()) {
            return null;
        }
        try {
            return JSON.readValue(line, Map.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String lastLine(String text) {
        if (text == null) {
            return "";
        }
        String[] lines = text.split("\r?\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            if (!lines[i].trim().isEmpty()) {
                return lines[i].trim();
            }
        }
        return text.trim();
    }


}
