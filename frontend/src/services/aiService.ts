import request from '../utils/request';
import type { Result } from '../types/api';

export interface GenerateNoteParams {
  frontContent: string;
  contextSentence?: string;
  aiApiKey: string;
  aiModel?: string;
  aiBaseUrl?: string;
  aiNotePrompt?: string;
}

export interface ChatWithAiParams {
  message: string;
  aiApiKey: string;
  aiModel?: string;
  aiBaseUrl?: string;
}

/**
 * 仅生成 AI 注释文本（不创建卡片），供弹窗内预览/编辑。
 * 超时 30s，因 AI 接口可能较慢。
 */
export function generateNote(params: GenerateNoteParams) {
  return request
    .post<Result<string>>('/ai/generate-note', params, { timeout: 30000 })
    .then((r) => r.data.data);
}

/** 配置页聊天测试 */
export function chatWithAi(params: ChatWithAiParams) {
  return request.post<Result<string>>('/ai/chat', params, { timeout: 30000 }).then((r) => r.data.data);
}
