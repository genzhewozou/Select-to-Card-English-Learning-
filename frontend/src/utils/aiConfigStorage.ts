/** localStorage key for AI config (API Key / Model / Base URL) */
const AI_CONFIG_KEY = 'english-learn-ai-config';
/** localStorage key for document-level "开启 AI 注释" toggle */
export const DOC_AI_NOTE_ENABLED_KEY = 'english-learn-doc-ai-note-enabled';

export interface AiConfigSaved {
  apiKey?: string;
  model?: string;
  baseUrl?: string;
}

const defaults: AiConfigSaved = {
  model: 'gpt-3.5-turbo',
  baseUrl: 'https://api.openai.com/v1',
};

export function getAiConfig(): AiConfigSaved {
  try {
    const raw = localStorage.getItem(AI_CONFIG_KEY);
    if (!raw) return { ...defaults };
    const parsed = JSON.parse(raw) as Partial<AiConfigSaved>;
    return { ...defaults, ...parsed };
  } catch {
    return { ...defaults };
  }
}

export function setAiConfig(config: AiConfigSaved): void {
  localStorage.setItem(AI_CONFIG_KEY, JSON.stringify(config));
}

export function getDocAiNoteEnabled(): boolean {
  const v = localStorage.getItem(DOC_AI_NOTE_ENABLED_KEY);
  if (v === null) return false;
  return v === 'true';
}

export function setDocAiNoteEnabled(enabled: boolean): void {
  localStorage.setItem(DOC_AI_NOTE_ENABLED_KEY, enabled ? 'true' : 'false');
}
