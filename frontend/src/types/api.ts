/**
 * 与后端统一的 REST 返回结构。
 */
export interface Result<T> {
  code: number;
  message: string;
  data: T;
}

export interface PageResult<T> {
  page: number;
  size: number;
  total: number;
  list: T[];
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
  /** 服务器是否保存了上传原件（可下载） */
  originalAvailable?: boolean;
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

export interface CardExampleDTO {
  id?: number;
  senseId?: number;
  sortOrder?: number;
  sentenceEn?: string;
  sentenceZh?: string;
  scenarioTag?: string;
}

export interface CardSynonymDTO {
  id?: number;
  senseId?: number;
  sortOrder?: number;
  lemma?: string;
  noteZh?: string;
}

export interface CardSenseDTO {
  id?: number;
  cardId?: number;
  sortOrder?: number;
  label?: string;
  translationZh?: string;
  explanationEn?: string;
  tone?: string;
  examples?: CardExampleDTO[];
  synonyms?: CardSynonymDTO[];
}

export interface CardGlobalExtraDTO {
  id?: number;
  cardId?: number;
  collocations?: string[];
  nativeTip?: string;
  highLevelEn?: string;
  highLevelZh?: string;
}

export interface CardStructuredExamplePayload {
  order?: number;
  en: string;
  zh?: string;
  tag?: string;
}

export interface CardStructuredSynonymPayload {
  order?: number;
  lemma: string;
  noteZh?: string;
}

export interface CardStructuredSensePayload {
  order: number;
  label?: string;
  translationZh?: string;
  explanationEn?: string;
  tone?: string;
  examples: CardStructuredExamplePayload[];
  synonyms: CardStructuredSynonymPayload[];
}

export interface CardStructuredGlobalPayload {
  collocations?: string[];
  nativeTip?: string;
  highLevelEn?: string;
  highLevelZh?: string;
}

export interface CardStructuredSaveRequest {
  senses: CardStructuredSensePayload[];
  globalExtra?: CardStructuredGlobalPayload;
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
  /** AI 注释提示词模板（选填） */
  aiNotePrompt?: string;
  /** 前端已生成好的注释内容（有此字段时直接落库，不再调 AI） */
  aiNoteContent?: string;
  startOffset?: number;
  endOffset?: number;
  gmtCreate?: string;
  gmtModified?: string;
  senses?: CardSenseDTO[];
  globalExtra?: CardGlobalExtraDTO;
  progress?: CardProgressDTO;
}

export interface QuizQuestionDTO {
  itemId: number;
  sequence: number;
  type?: string;
  prompt?: string;
  sentenceEn?: string;
  sentenceZh?: string;
  options?: string[];
}

export interface QuizSessionStartResponse {
  sessionId: number;
  questions: QuizQuestionDTO[];
}

export interface QuizAnswerResponse {
  correct: boolean;
  verdict?: 'CORRECT' | 'PARTIAL' | 'WRONG';
  score?: number;
  feedback?: string;
  expectedFront?: string;
  expectedSentence?: string;
  sentenceEn?: string;
  sentenceZh?: string;
  frontCorrect?: boolean;
  sentenceVerdict?: 'CORRECT' | 'PARTIAL' | 'WRONG';
  sentenceScore?: number;
  frontFeedback?: string;
  sentenceFeedback?: string;
  sessionCompleted: boolean;
}

export interface QuizResultItemDTO {
  itemId: number;
  sequence: number;
  sentenceEn?: string;
  isCorrect?: boolean;
  type?: string;
  prompt?: string;
  expected?: string;
  userAnswer?: string;
}

export interface QuizResultResponse {
  sessionId: number;
  total: number;
  correctCount: number;
  items: QuizResultItemDTO[];
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface ReviewRequest {
  cardId: number;
  proficiencyLevel: number; // 1-5
}

export interface ReviewPostponeRequest {
  cardId: number;
  days: 1 | 2 | 7;
}

export interface ReviewDocumentPostponeRequest {
  documentId: number;
  days: 1 | 2 | 7;
}
