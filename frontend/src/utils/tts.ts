/**
 * 浏览器自带 TTS（Web Speech API）封装。
 * 用于卡片「点击朗读」增强；不支持时不展示朗读按钮即可。
 * 修复：Chrome 等浏览器需在 cancel 后延迟 speak，并确保语音列表已加载，否则无声音。
 */

export function isTtsSupported(): boolean {
  if (typeof window === 'undefined') return false;
  return 'speechSynthesis' in window;
}

export interface SpeakOptions {
  lang?: string;
  onStart?: () => void;
  onEnd?: () => void;
  onError?: () => void;
}

/** 触发浏览器加载语音列表（Chrome 首次需此步骤才有声音） */
function ensureVoicesLoaded(synth: SpeechSynthesis): void {
  if (synth.getVoices().length > 0) return;
  synth.getVoices(); // 触发异步加载
}

export function speak(text: string, options: SpeakOptions = {}): void {
  if (!text?.trim() || !isTtsSupported()) return;
  const { lang = 'en-US', onStart, onEnd, onError } = options;
  const synth = window.speechSynthesis;
  synth.cancel();
  ensureVoicesLoaded(synth);
  const toSpeak = text.trim();
  setTimeout(() => {
    const u = new SpeechSynthesisUtterance(toSpeak);
    u.lang = lang;
    u.rate = 1;
    u.volume = 1;
    const voices = synth.getVoices();
    const enVoice = voices.find((v) => v.lang.startsWith('en')) ?? voices[0];
    if (enVoice) u.voice = enVoice;
    u.onstart = () => onStart?.();
    u.onend = () => onEnd?.();
    u.onerror = (e: SpeechSynthesisErrorEvent) => {
      if (e.error === 'interrupted') {
        onEnd?.();
        return;
      }
      onError?.();
      console.warn('TTS error', e);
    };
    synth.speak(u);
  }, 100);
}

export function stop(): void {
  if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
    window.speechSynthesis.cancel();
  }
}
