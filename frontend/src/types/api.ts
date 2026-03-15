/**
 * 与后端统一的 REST 返回结构。
 */
export interface Result<T> {
  code: number;
  message: string;
  data: T;
}

export interface UserDTO {
  id?: number;
  username: string;
  password?: string;
  nickname?: string;
  email?: string;
  gmtCreate?: string;
  gmtModified?: string;
}

export interface DocumentDTO {
  id?: number;
  userId?: number;
  fileName: string;
  fileType?: string;
  content?: string;
  gmtCreate?: string;
  gmtModified?: string;
}

export interface CardNoteDTO {
  id?: number;
  cardId: number;
  content: string;
  gmtCreate?: string;
  gmtModified?: string;
}

export interface CardProgressDTO {
  id?: number;
  userId?: number;
  cardId: number;
  proficiencyLevel?: number;
  reviewCount?: number;
  nextReviewAt?: string;
  lastReviewAt?: string;
  gmtCreate?: string;
  gmtModified?: string;
}

export interface CardDTO {
  id?: number;
  userId?: number;
  documentId?: number;
  frontContent: string;
  backContent?: string;
  /** 选中内容所在句子/段落，用于 AI 生成注释（选填） */
  contextSentence?: string;
  /** 是否使用 AI 生成注释（勾选时需传 aiApiKey 等） */
  useAiNote?: boolean;
  /** AI API Key（仅当 useAiNote=true 时必填，不落库） */
  aiApiKey?: string;
  /** AI 模型名，如 gpt-3.5-turbo（选填） */
  aiModel?: string;
  /** AI 接口根地址（选填） */
  aiBaseUrl?: string;
  /** 前端已生成好的注释内容（有此字段时直接落库，不再调 AI） */
  aiNoteContent?: string;
  startOffset?: number;
  endOffset?: number;
  gmtCreate?: string;
  gmtModified?: string;
  notes?: CardNoteDTO[];
  progress?: CardProgressDTO;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface ReviewRequest {
  cardId: number;
  proficiencyLevel: number; // 1-5
}
