import { useState, useCallback, useEffect, useRef } from 'react';
import { speak as ttsSpeak, stop as ttsStop, isTtsSupported } from '../utils/tts';

const STALE_TIMEOUT_MS = 5000; // 若 5 秒内未结束（无声音或卡住），强制恢复按钮状态

export function useTTS() {
  const [isSpeaking, setIsSpeaking] = useState(false);
  const [supported] = useState(isTtsSupported);
  const staleTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const clearStaleTimer = useCallback(() => {
    if (staleTimerRef.current != null) {
      clearTimeout(staleTimerRef.current);
      staleTimerRef.current = null;
    }
  }, []);

  // Chrome：进入页面时预加载语音列表，避免首次点击无声音
  useEffect(() => {
    if (typeof window === 'undefined' || !('speechSynthesis' in window)) return;
    window.speechSynthesis.getVoices();
    const onVoicesChanged = () => window.speechSynthesis.getVoices();
    window.speechSynthesis.addEventListener('voiceschanged', onVoicesChanged);
    return () => window.speechSynthesis.removeEventListener('voiceschanged', onVoicesChanged);
  }, []);

  const speak = useCallback(
    (text: string, lang?: string) => {
      if (!text?.trim() || !supported) return;
      clearStaleTimer();
      setIsSpeaking(true);
      const done = () => {
        clearStaleTimer();
        setIsSpeaking(false);
      };
      staleTimerRef.current = setTimeout(done, STALE_TIMEOUT_MS);
      ttsSpeak(text, {
        lang: lang ?? 'en-US',
        onStart: () => setIsSpeaking(true),
        onEnd: done,
        onError: done,
      });
    },
    [supported, clearStaleTimer]
  );

  const stop = useCallback(() => {
    clearStaleTimer();
    ttsStop();
    setIsSpeaking(false);
  }, [clearStaleTimer]);

  useEffect(() => () => {
    clearStaleTimer();
    ttsStop();
  }, [clearStaleTimer]);

  return { speak, stop, isSpeaking, isSupported: supported };
}
