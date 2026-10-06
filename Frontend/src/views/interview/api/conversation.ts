import { api } from "./index";
import { resolveApiBase as resolveConfiguredApiBase } from "../../../config/androidApi";

export interface ConversationItem {
  id: number;
  conversation_id: string;
  user_id: number;
  job_role: string;
  status: string;
  has_evaluation?: boolean;
  evaluation_id?: number | null;
  evaluation_score?: number | null;
  evaluation_created_at?: string | null;
  started_at?: string | null;
  ended_at?: string | null;
  created_at?: string;
  updated_at?: string;
}

export interface NewIdResponse {
  conversation_id: string;
  created_at?: string;
}

export interface InterviewConfigPayload {
  target_position: string;
  interview_mode: 'full' | 'focused';
  interviewer_persona: 'neutral' | 'friendly' | 'challenging' | 'pragmatic';
  focus_tags?: string | null;
  requirements?: string | null;
  difficulty: number;
  remark?: string | null;
  status?: number;
}

export interface InterviewConfigItem {
  id: number;
  conversation_id: string | null;
  target_position: string;
  interview_mode: string;
  interviewer_persona: string;
  focus_tags: string | null;
  requirements: string | null;
  difficulty: number;
  remark: string | null;
  status: number;
  created_at?: string | null;
  updated_at?: string | null;
}

export interface StartConversationResponse extends ConversationItem {
  config?: InterviewConfigItem;
}

export interface ChatResponse {
  role: string;
  content: string;
  conversation_id: string;
}

export interface MessageItem {
  id: number;
  role: string;
  content: string;
  created_at: string;
}

export interface EvaluationDimensionScore {
  score: number;
  comment: string;
}

export interface InterviewQuestionAnalysis {
  question: string;
  answer: string;
  answer_summary: string;
  score: number;
  comment: string;
}

export interface InterviewSummaryReport {
  id: number;
  conversation_id: string;
  user_id: number;
  job_role: string;
  score: number;
  core_conclusion: string;
  strengths: string;
  weaknesses: string;
  improvements: string;
  created_at: string;
  updated_at: string;
  /** 生成报告的模型名称 */
  model?: string;
  /** 五个维度的评分与点评 */
  dimensions?: Record<string, EvaluationDimensionScore>;
  /** 逐题分析（含学生真实回答） */
  question_analysis?: InterviewQuestionAnalysis[];
  started_at?: string | null;
  ended_at?: string | null;
  duration_seconds?: number | null;
}

export interface EvaluationReportResponse {
  success: boolean;
  code: string;
  message?: string;
  report?: InterviewSummaryReport;
  conversation_id?: string;
}

// 面部数据记录相关类型
export interface FaceRecordItem {
  capture_time: string;
  face_detected: boolean;
  blendshapes?: {
    [key: string]: number;
  };
}

export interface FaceRecordStartResponse {
  conversation_id: string;
  file_path: string;
}

export interface FaceRecordAppendResponse {
  total_records: number;
}

export interface FaceRecordEndResponse {
  file_path: string;
  total_records: number;
}

export interface FaceRecordListItem {
  filename: string;
  size: number;
  created_at: string;
  modified_at: string;
}

export interface FaceEmotionSummaryPerSecondItem {
  second: number;
  frames: number;
  face_frames: number;
  face_detect_ratio: number;
  main_label: string;
  dominant_expression: string;
  emotion: string;
  avg_tension_score: number;
  max_tension_score: number;
  avg_positivity_score: number;
  max_positivity_score: number;
  label_distribution: Record<string, number>;
}

export interface FaceEmotionSummaryResponse {
  overall: {
    total_seconds: number;
    overall_state: string;
    avg_tension_score: number;
    max_tension_score: number;
    avg_positivity_score: number;
    max_positivity_score: number;
    face_detect_ratio: number;
    frame_distribution: Record<string, number>;
  };
  per_second: FaceEmotionSummaryPerSecondItem[];
}

export interface AsrUploadResponse {
  saved: boolean;
  message: string;
  voice_format?: string;
  saved_file?: {
    filename: string;
    path: string;
    size: number;
  };
}

export interface AsrFilenameTranscribeResponse {
  text: string;
  request_id?: string;
  filename?: string;
}

export interface TtsSynthesizeResponse {
  text: string;
  filename?: string;
  path?: string;
  request_id?: string;
  session_id?: string;
  codec?: string;
  sample_rate?: number;
  audio_base64?: string;
}

export const conversationApi = {
  newId: (data?: { user_id?: number; job_role?: string; session_token?: string }) =>
    api.post<NewIdResponse>("/conversation/new-id", data),
  
  start: (data: { conversation_id: string; config?: InterviewConfigPayload }) =>
    api.post<StartConversationResponse>("/conversation/start", data),
  
  end: (data: { conversation_id: string }) => api.post<ConversationItem>("/conversation/end", data),

  commit: (data: { conversation_id: string; user_id: number; job_role: string; started_at?: string; ended_at?: string }) =>
    api.post<ConversationItem>("/conversation/commit", data),
    
  list: (params?: { user_id?: number; status?: string; job_role?: string; order_by?: string; page?: number; page_size?: number }) =>
    api.get<{ total: number; page: number; page_size: number; items: ConversationItem[] }>("/conversation/list", {
      query: params as any,
    }),
    
  messageList: (conversation_id: string) =>
    api.get<{ items: MessageItem[] }>("/message/list", {
      query: { conversation_id },
    }),

  langgraphChat: (data: { conversation_id: string; content: string; job_role?: string; user_info?: any }) =>
    api.post<ChatResponse>("/langgraph/chat", data),

  chatStreamUrl: (params: { text: string; job_role?: string }) => {
    const baseApi = resolveConfiguredApiBase()
    const base = `${baseApi}/conversation/chat/stream`
    const qs = new URLSearchParams()
    qs.set("text", params.text)
    if (params.job_role) qs.set("job_role", params.job_role)
    return `${base}?${qs.toString()}`
  },

  uploadAudio: async (audio: Blob, fileName = "recording.mp3", voice_format?: string): Promise<AsrUploadResponse> => {
    const baseApi = resolveConfiguredApiBase()
    const form = new FormData()
    form.append("audio", audio, fileName)
    if (voice_format) form.append("voice_format", voice_format)

    const resp = await fetch(`${baseApi}/asr/upload`, {
      method: "POST",
      headers: {
        ...(localStorage.getItem("token") || localStorage.getItem("session_token")
          ? {
              Authorization: `Bearer ${localStorage.getItem("token") || localStorage.getItem("session_token")}`,
              "X-Session-Token": localStorage.getItem("token") || localStorage.getItem("session_token") || "",
            }
          : {}),
      },
      body: form,
    });

    let payload: any = null;
    try {
      payload = await resp.json();
    } catch {
      payload = null;
    }

    if (!resp.ok) {
      const msg = payload?.error ? String(payload.error) : `HTTP ${resp.status}`;
      throw new Error(msg);
    }

    return payload as AsrUploadResponse;
  },

  transcribeByFilename: async (filename: string, voice_format?: string): Promise<AsrFilenameTranscribeResponse> => {
    return api.post<AsrFilenameTranscribeResponse>("/asr/transcribe", {
      filename,
      voice_format,
    });
  },

  ttsSynthesize: (
    text: string,
    options?: { codec?: "mp3" | "pcm" | "wav"; sample_rate?: 8000 | 16000; voice_type?: number },
  ) =>
    api.post<TtsSynthesizeResponse>("/tts/synthesize", {
      text,
      codec: options?.codec,
      sample_rate: options?.sample_rate,
      voice_type: options?.voice_type,
    }),

  /** 只读取数据库中已生成的报告，不触发 AI 生成。 */
  getEvaluationReport: (conversationId: string) =>
    api.get<EvaluationReportResponse>("/interview/evaluation/report", {
      query: { conversation_id: conversationId },
    }),

  /** 生成报告；已生成过则直接返回数据库结果。 */
  summarizeInterview: (data: { conversation_id: string }) =>
    api.post<EvaluationReportResponse>("/interview/evaluation/summarize", data),

  // 面部数据录制 API
  faceRecordStart: (data?: {
    conversation_id?: string;
    auto_capture?: boolean;
    detect_fps?: number;
    duration_seconds?: number;
    camera_index?: number;
    max_records?: number;
  }) =>
    api.post<FaceRecordStartResponse>("/face/record/start", data),

  faceRecordAppend: (data: {
    conversation_id: string;
    records?: FaceRecordItem[];
    frame_image?: string;
    capture_time?: string;
  }) =>
    api.post<FaceRecordAppendResponse>("/face/record/append", data),

  faceRecordEnd: (data: { conversation_id: string }) =>
    api.post<FaceRecordEndResponse>("/face/record/end", data),

  faceRecordList: () =>
    api.get<{ items: FaceRecordListItem[] }>("/face/record/list"),

  faceRecordDetail: (conversation_id: string) =>
    api.get<FaceRecordItem>("/face/record/detail/" + conversation_id),

  faceRecordEmotionSummary: (params: {
    conversation_id: string;
    smooth_window?: number;
    neutral_margin?: number;
    min_confidence?: number;
  }) =>
    api.get<FaceEmotionSummaryResponse>("/face/record/emotion-summary", {
      query: params as any,
    }),
};
