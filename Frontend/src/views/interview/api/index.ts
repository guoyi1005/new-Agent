// 轻量级 fetch 封装，避免依赖第三方库
import { resolveApiBase as resolveConfiguredApiBase } from '../../../config/androidApi'
export interface RequestOptions extends RequestInit {
  query?: Record<string, string | number | boolean | undefined | null>;
}

export class ApiError extends Error {
  status: number;
  code?: string;
  payload?: any;

  constructor(message: string, status: number, code?: string, payload?: any) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.payload = payload;
  }
}

const SESSION_ERROR_CODES = new Set(["invalid_or_expired_session"]);

export function isSessionExpiredResponse(status: number, payload?: any): boolean {
  const code = payload && typeof payload === "object" ? (payload as any).error : undefined;
  return status === 401 || SESSION_ERROR_CODES.has(String(code || ""));
}

export function redirectToLoginOnSessionExpired(status: number, payload?: any): boolean {
  if (!isSessionExpiredResponse(status, payload)) return false;
  const keysToClear = [
    "session_token",
    "token",
    "user_id",
    "nickname",
    "is_manager",
    "conversation_id",
    "conversation_status",
    "conversation_started_at",
    "conversation_ended_at",
  ];
  keysToClear.forEach((k) => localStorage.removeItem(k));
  window.location.href = "/login";
  return true;
}

export function resolveApiBase(): string {
  return resolveConfiguredApiBase()
}

const API_BASE = resolveApiBase()

function authHeaders(): Record<string, string> {
  const token = localStorage.getItem("token") || localStorage.getItem("session_token") || "";
  if (!token) return {};
  return {
    Authorization: `Bearer ${token}`,
    "X-Session-Token": token,
  };
}

function buildUrl(path: string, query?: RequestOptions["query"]): string {
  const url = new URL(path.startsWith("http") ? path : `${API_BASE}${path}`, window.location.origin);
  if (query) {
    Object.entries(query).forEach(([k, v]) => {
      if (v !== undefined && v !== null) url.searchParams.set(k, String(v));
    });
  }
  // 开发环境默认直连 8080；生产环境的空 API_BASE 仍会解析为当前域名，交给 nginx 代理。
  return url.toString();
}

async function request<T>(method: string, path: string, opts: RequestOptions = {}): Promise<T> {
  const { query, headers, body, ...rest } = opts;
  const url = buildUrl(path, query);
  let payload: BodyInit | null | undefined = null;
  if (body !== undefined) {
    if (
      typeof body === "string" ||
      body instanceof Blob ||
      body instanceof FormData ||
      body instanceof URLSearchParams
    ) {
      payload = body as BodyInit;
    } else if (body === null) {
      payload = null;
    } else {
      payload = JSON.stringify(body) as unknown as BodyInit;
    }
  }
  const shouldLog = import.meta.env.DEV || localStorage.getItem("debug_api") === "1";
  if (shouldLog) {
    const sanitized = body && typeof body === "object" ? JSON.parse(JSON.stringify(body)) : body;
    if (sanitized && typeof sanitized === "object") {
      if ("password" in (sanitized as any)) (sanitized as any).password = "***";
    }
    console.groupCollapsed(`[API] ${method} ${url}`);
    console.log("Request:", sanitized ?? null);
  }
  const res = await fetch(url, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...authHeaders(),
      ...headers,
    },
    body: payload,
    ...rest,
  });
  if (!res.ok) {
    const contentType = res.headers.get("content-type") || "";
    let payload: any = null;
    let text = "";
    if (contentType.includes("application/json")) {
      payload = await res.json().catch(() => null);
      text = payload ? JSON.stringify(payload) : "";
    } else {
      text = await res.text().catch(() => "");
    }

    redirectToLoginOnSessionExpired(res.status, payload);

    if (shouldLog) {
      console.log("Response Status:", res.status, res.statusText);
      console.log("Response Body:", text);
      console.groupEnd();
    }
    const code = payload && typeof payload === "object"
      ? ((payload as any).error || (payload as any).msg)
      : undefined;
    const field = payload && typeof payload === "object" ? (payload as any).field : undefined;
    const safeMessage = code === "missing_or_empty_field"
      ? `缺少必填字段${field ? `：${field}` : ""}`
      : `HTTP ${res.status}: ${res.statusText || "请求失败"}`;
    throw new ApiError(safeMessage, res.status, code, payload ?? text);
  }
  const contentType = res.headers.get("content-type") || "";
  const data = contentType.includes("application/json") ? ((await res.json()) as T) : ((await res.text()) as unknown as T);
  if (shouldLog) {
    console.log("Response:", data);
    console.groupEnd();
  }
  return data;
}

export const api = {
  get: <T>(path: string, opts?: RequestOptions) => request<T>("GET", path, opts),
  post: <T>(path: string, body?: any, opts?: Omit<RequestOptions, "body">) =>
    request<T>("POST", path, { ...(opts as any), body } as RequestOptions),
  put: <T>(path: string, body?: any, opts?: Omit<RequestOptions, "body">) =>
    request<T>("PUT", path, { ...(opts as any), body } as RequestOptions),
  patch: <T>(path: string, body?: any, opts?: Omit<RequestOptions, "body">) =>
    request<T>("PATCH", path, { ...(opts as any), body } as RequestOptions),
  delete: <T>(path: string, opts?: RequestOptions) => request<T>("DELETE", path, opts),
};

export interface InterviewerAiStatus {
  configured: boolean;
  base_url: string;
  model?: string;
}

export const interviewerApi = {
  status: (signal?: AbortSignal) =>
    api.get<InterviewerAiStatus>("/interview/ai/status", signal ? { signal } : undefined),
};

// 示例：题库 API（对接 Django 可快速替换路径）
export interface KnowledgeItem {
  id: number;
  job_position: string;
  question_type: string;
  question: string;
  difficulty: number;
  status: number;
  updated_at?: string;
  created_at?: string;
}

export const knowledgeApi = {
  list: (params?: { job_position?: string; question_type?: string }) =>
    api.get<KnowledgeItem[]>("/knowledge", { query: params }),
  get: (id: number) => api.get<KnowledgeItem>(`/knowledge/${id}`),
  create: (data: Partial<KnowledgeItem>) => api.post<KnowledgeItem>("/knowledge", data),
  update: (id: number, data: Partial<KnowledgeItem>) => api.put<KnowledgeItem>(`/knowledge/${id}`, data),
  remove: (id: number) => api.delete<void>(`/knowledge/${id}`),
};

// AI 职业导师 API
export interface AICoachResponse {
  message: string;
  user_info?: {
    nickname?: string;
    target_position?: string;
  };
}

export const aiCoachApi = {
  chat: (message: string) => api.post<AICoachResponse>("/ai-coach", { message }),
  
  /**
   * 流式调用 AI 职业规划导师
   * @param message 用户消息
   * @param onChunk 每次收到数据块的回调
   * @param onDone 流结束时的回调
   * @param onError 错误处理回调
   */
  chatStream: (
    message: string,
    onChunk: (chunk: string) => void,
    onDone?: () => void,
    onError?: (error: Error) => void
  ) => {
    const url = `${API_BASE}/ai-coach`;
    
    fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...authHeaders(),
      },
      body: JSON.stringify({ message }),
    })
      .then(async (res) => {
        if (!res.ok) {
          const text = await res.text().catch(() => "");
          throw new Error(`HTTP ${res.status}: ${text || res.statusText}`);
        }
        
        const reader = res.body?.getReader();
        if (!reader) {
          throw new Error("Response body is null");
        }
        
        const decoder = new TextDecoder();
        let buffer = "";
        
        while (true) {
          const { done, value } = await reader.read();
          if (done) break;
          
          buffer += decoder.decode(value, { stream: true });
          const lines = buffer.split("\n");
          buffer = lines.pop() || "";
          
          for (const line of lines) {
            const trimmed = line.trim();
            if (trimmed.startsWith("data: ")) {
              const data = trimmed.slice(6);
              if (data === "[DONE]") {
                onDone?.();
                return;
              }
              if (data.startsWith("ERROR:")) {
                throw new Error(data.slice(6));
              }
              onChunk(data);
            }
          }
        }
        
        // 处理缓冲区中剩余的数据
        if (buffer.trim()) {
          const trimmed = buffer.trim();
          if (trimmed.startsWith("data: ")) {
            const data = trimmed.slice(6);
            if (data !== "[DONE]") {
              onChunk(data);
            }
          }
        }
        
        onDone?.();
      })
      .catch((error) => {
        onError?.(error);
      });
  },
};

// AI 定制职业路径 API
export interface AICustomPathResponse {
  action: string;
  user_info: {
    nickname: string;
    target_position: string;
    tech_stack: string;
  };
  response: string;
}

export type CustomPathAction = 'career_path' | 'generate_resume' | 'optimize_resume';

export const aiCustomPathApi = {
  /**
   * AI 定制职业路径（普通调用）
   * @param action 操作类型：career_path(职业规划)、generate_resume(生成简历)、optimize_resume(优化简历)
   * @param message 用户额外要求或问题
   * @param resumeContent 当前简历内容（仅optimize_resume时需要）
   */
  chat: (action: CustomPathAction, message?: string, resumeContent?: string) =>
    api.post<AICustomPathResponse>("/ai-custom-path", {
      action,
      message: message || '',
      resume_content: resumeContent || undefined,
    }),
  
  /**
   * AI 定制职业路径（流式调用）
   * @param action 操作类型
   * @param message 用户消息
   * @param onChunk 每次收到数据块的回调
   * @param onDone 流结束时的回调
   * @param onError 错误处理回调
   * @param resumeContent 当前简历内容（仅optimize_resume时需要）
   */
  chatStream: (
    action: CustomPathAction,
    message: string,
    onChunk: (chunk: string) => void,
    onDone?: () => void,
    onError?: (error: Error) => void,
    resumeContent?: string
  ) => {
    const url = `${API_BASE}/ai-custom-path`;
    
    fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...authHeaders(),
      },
      body: JSON.stringify({
        action,
        message: message || '',
        resume_content: resumeContent || undefined,
      }),
    })
      .then(async (res) => {
        if (!res.ok) {
          const text = await res.text().catch(() => "");
          throw new Error(`HTTP ${res.status}: ${text || res.statusText}`);
        }
        
        const reader = res.body?.getReader();
        if (!reader) {
          throw new Error("Response body is null");
        }
        
        const decoder = new TextDecoder();
        let buffer = "";
        
        while (true) {
          const { done, value } = await reader.read();
          if (done) break;
          
          buffer += decoder.decode(value, { stream: true });
          const lines = buffer.split("\n");
          buffer = lines.pop() || "";
          
          for (const line of lines) {
            const trimmed = line.trim();
            if (trimmed.startsWith("data: ")) {
              const data = trimmed.slice(6);
              if (data === "[DONE]") {
                onDone?.();
                return;
              }
              if (data.startsWith("ERROR:")) {
                throw new Error(data.slice(6));
              }
              onChunk(data);
            }
          }
        }
        
        // 处理缓冲区中剩余的数据
        if (buffer.trim()) {
          const trimmed = buffer.trim();
          if (trimmed.startsWith("data: ")) {
            const data = trimmed.slice(6);
            if (data !== "[DONE]") {
              onChunk(data);
            }
          }
        }
        
        onDone?.();
      })
      .catch((error) => {
        onError?.(error);
      });
  },
};
