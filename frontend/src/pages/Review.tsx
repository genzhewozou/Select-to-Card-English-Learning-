import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Card, Button, message, Slider, Space, Select, Dropdown, Switch } from 'antd';
import type { MenuProps } from 'antd';
import { SoundOutlined } from '@ant-design/icons';
import { getTodayReviewPage, getWeakCardsPage, postponeReview, submitReview } from '../services/reviewService';
import { getCardPage } from '../services/cardService';
import { getDocumentList } from '../services/documentService';
import { useTTS } from '../hooks/useTTS';
import type { CardDTO } from '../types/api';
import type { DocumentDTO } from '../types/api';

/**
 * 学习复习页：展示今日待复习或错题本卡片；
 * 支持 URL ?mode=weak 只复习错题（熟练度 1-2）。
 */
export default function Review() {
  const [searchParams, setSearchParams] = useSearchParams();
  const modeWeak = searchParams.get('mode') === 'weak';
  const modeRelearn = searchParams.get('relearn') === '1';
  const urlDocId = searchParams.get('documentId');
  const parsedDocId = urlDocId ? Number(urlDocId) : undefined;
  const [list, setList] = useState<CardDTO[]>([]);
  const [documents, setDocuments] = useState<DocumentDTO[]>([]);
  const [selectedDocumentId, setSelectedDocumentId] = useState<number | undefined>(
    Number.isFinite(parsedDocId) ? parsedDocId : undefined,
  );
  const [loading, setLoading] = useState(true);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [proficiency, setProficiency] = useState(3);
  const [submitting, setSubmitting] = useState(false);
  const [postponingDays, setPostponingDays] = useState<number | null>(null);
  const [showBack, setShowBack] = useState(false);
  const [showNativeTip, setShowNativeTip] = useState(() => localStorage.getItem('reviewShowNativeTip') === '1');
  const { speak, stop, isSpeaking, isSupported } = useTTS();

  const handleSpeak = (text: string) => {
    if (!text?.trim()) return;
    if (!isSupported) {
      message.info('当前浏览器不支持朗读，请使用 Chrome、Edge 等现代浏览器');
      return;
    }
    if (isSpeaking) {
      stop();
      return;
    }
    speak(text);
  };

  const load = async (docIdOverride?: number) => {
    const docId = docIdOverride !== undefined ? docIdOverride : selectedDocumentId;
    setLoading(true);
    try {
      // 默认最多拉 200 张，保证速度；需要更多可后续做「下一页」加载
      const data = modeWeak
        ? await getWeakCardsPage(1, 200, docId)
        : modeRelearn
          ? await getCardPage({ documentId: docId, page: 1, size: 200 })
          : await getTodayReviewPage(1, 200, docId);
      setList(data?.list ?? []);
      setCurrentIndex(0);
      setShowBack(false);
      setProficiency(3);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '加载失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    getDocumentList().then(setDocuments).catch(() => setDocuments([]));
  }, []);

  useEffect(() => {
    setSelectedDocumentId(Number.isFinite(parsedDocId) ? parsedDocId : undefined);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [urlDocId]);

  useEffect(() => {
    load();
  }, [modeWeak, modeRelearn, selectedDocumentId]);

  const current = list[currentIndex];
  const currentSpeakText = (() => {
    if (!current) return '';
    if (current.senses && current.senses.length > 0) {
      const lines: string[] = [];
      current.senses.forEach((s, i) => {
        lines.push(`Sense ${i + 1}`);
        if (s.translationZh) lines.push(s.translationZh);
        if (s.explanationEn) lines.push(s.explanationEn);
        (s.examples ?? []).forEach((ex) => {
          if (ex.sentenceEn) lines.push(ex.sentenceEn);
          if (ex.sentenceZh) lines.push(ex.sentenceZh);
        });
      });
      if (lines.length > 0) return lines.join('. ');
    }
    return current.backContent ?? '';
  })();

  const handleSubmitReview = async () => {
    if (!current?.id) return;
    setSubmitting(true);
    try {
      await submitReview({ cardId: current.id, proficiencyLevel: proficiency });
      message.success('已记录，下次将按艾宾浩斯时间推荐复习');
      const nextList = list.filter((_, i) => i !== currentIndex);
      setList(nextList);
      setCurrentIndex(currentIndex >= list.length - 1 ? 0 : currentIndex);
      setShowBack(false);
      setProficiency(3);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '提交失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handlePostpone = async (days: 1 | 2 | 7) => {
    if (!current?.id) return;
    setPostponingDays(days);
    try {
      await postponeReview({ cardId: current.id, days });
      message.success(`已延后 ${days} 天`);
      const nextList = list.filter((_, i) => i !== currentIndex);
      setList(nextList);
      setCurrentIndex(currentIndex >= list.length - 1 ? 0 : currentIndex);
      setShowBack(false);
      setProficiency(3);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '延后失败');
    } finally {
      setPostponingDays(null);
    }
  };

  const onDocumentChange = (value: number | null) => {
    const nextDocId = value == null ? undefined : value;
    setSelectedDocumentId(nextDocId);
    const next = new URLSearchParams(searchParams);
    if (nextDocId == null) {
      next.delete('documentId');
    } else {
      next.set('documentId', String(nextDocId));
    }
    setSearchParams(next);
    // 切换文档时立即重新查询，避免等待路由参数/状态同步
    load(nextDocId);
  };

  const postponeMenuItems: MenuProps['items'] = [
    { key: '1', label: '延后1天' },
    { key: '2', label: '延后2天' },
    { key: '7', label: '延后7天' },
  ];

  useEffect(() => {
    localStorage.setItem('reviewShowNativeTip', showNativeTip ? '1' : '0');
  }, [showNativeTip]);

  if (loading) {
    return <div style={{ padding: 24 }}>加载中...</div>;
  }

  if (list.length === 0) {
    return (
      <Card>
        <div style={{ marginBottom: 16, display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
          <span style={{ color: '#666' }}>复习范围：</span>
          <Select
            style={{ width: 280 }}
            value={selectedDocumentId ?? null}
            onChange={onDocumentChange}
            options={[
              { value: null, label: '全部文档' },
              ...documents
                .filter((d) => d.id != null)
                .map((d) => ({ value: d.id!, label: d.fileName })),
            ]}
          />
        </div>
        <h2>{modeWeak ? '错题复习' : modeRelearn ? '文档重温复习' : '今日复习'}</h2>
        <p>
          {modeWeak
            ? '暂无熟练度 1～2 级的卡片。在「今日复习」中标记不熟后，会出现在错题本。'
            : '今日没有需要复习的卡片，或你尚未添加任何学习进度。去文档中选词生成卡片并复习一次后，这里会显示待复习列表。'}
        </p>
      </Card>
    );
  }

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>
        {modeWeak ? '错题复习' : modeRelearn ? '文档重温复习' : '今日复习'}（{currentIndex + 1} / {list.length}）
      </h2>
      <Card>
        <div style={{ marginBottom: 16, display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
          <span style={{ color: '#666' }}>复习范围：</span>
          <Select
            style={{ width: 280 }}
            value={selectedDocumentId ?? null}
            onChange={onDocumentChange}
            options={[
              { value: null, label: '全部文档' },
              ...documents
                .filter((d) => d.id != null)
                .map((d) => ({ value: d.id!, label: d.fileName })),
            ]}
          />
        </div>
        <p style={{ marginBottom: 8, color: '#666' }}>正面：</p>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 16, flexWrap: 'wrap' }}>
          <p style={{ fontSize: 18, margin: 0 }}>{current?.frontContent}</p>
          {current?.frontContent?.trim() && (
            <Button
              type="default"
              icon={<SoundOutlined />}
              onClick={() => handleSpeak(current.frontContent ?? '')}
              size="middle"
            >
              {isSpeaking ? '停止' : '朗读'}
            </Button>
          )}
        </div>
        {!showBack ? (
          <Button type="primary" onClick={() => setShowBack(true)}>
            显示背面
          </Button>
        ) : (
          <>
            <p style={{ marginBottom: 8, color: '#666' }}>背面：</p>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12, flexWrap: 'wrap' }}>
              {currentSpeakText.trim() && (
                <Button
                  type="default"
                  icon={<SoundOutlined />}
                  onClick={() => handleSpeak(currentSpeakText)}
                  size="middle"
                >
                  {isSpeaking ? '停止' : '朗读'}
                </Button>
              )}
              <span style={{ color: '#666' }}>显示 Native Tip</span>
              <Switch checked={showNativeTip} onChange={setShowNativeTip} />
            </div>
            {current?.senses && current.senses.length > 0 ? (
              <div
                style={{
                  background: '#fafafa',
                  border: '1px solid #f0f0f0',
                  borderRadius: 8,
                  padding: 16,
                  lineHeight: 1.75,
                  fontSize: 14,
                  color: '#333',
                  maxHeight: 420,
                  overflow: 'auto',
                  marginBottom: 16,
                }}
              >
                {current.senses.map((sense, idx) => (
                  <div key={sense.id ?? idx} style={{ marginBottom: idx === current.senses!.length - 1 ? 0 : 16 }}>
                    <div style={{ fontWeight: 700, marginBottom: 6 }}>释义 {idx + 1}</div>
                    <div style={{ marginBottom: 4, color: '#555' }}>中文释义：</div>
                    <div style={{ marginBottom: 8, whiteSpace: 'pre-wrap' }}>{sense.translationZh || '（无）'}</div>
                    <div style={{ marginBottom: 4, color: '#555' }}>英文释义：</div>
                    <div style={{ marginBottom: 8, whiteSpace: 'pre-wrap' }}>{sense.explanationEn || '（无）'}</div>
                    <div style={{ marginBottom: 4, color: '#555' }}>例句：</div>
                    {(sense.examples ?? []).length > 0 ? (
                      <ul style={{ paddingLeft: 20, marginBottom: 8 }}>
                        {(sense.examples ?? []).map((ex, exIdx) => (
                          <li key={ex.id ?? exIdx} style={{ marginBottom: 6 }}>
                            <div>{ex.sentenceEn || '（无）'}</div>
                            {ex.sentenceZh ? <div style={{ color: '#666' }}>{ex.sentenceZh}</div> : null}
                          </li>
                        ))}
                      </ul>
                    ) : (
                      <div style={{ marginBottom: 8 }}>（无）</div>
                    )}
                    <div style={{ marginBottom: 4, color: '#555' }}>同义词：</div>
                    {(sense.synonyms ?? []).length > 0 ? (
                      <ul style={{ paddingLeft: 20, marginBottom: 8 }}>
                        {(sense.synonyms ?? []).map((sy, syIdx) => (
                          <li key={sy.id ?? syIdx}>
                            {sy.lemma}
                            {sy.noteZh ? ` - ${sy.noteZh}` : ''}
                          </li>
                        ))}
                      </ul>
                    ) : (
                      <div style={{ marginBottom: 8 }}>（无）</div>
                    )}
                  </div>
                ))}
                {current.globalExtra ? (
                  <div style={{ borderTop: '1px dashed #ddd', paddingTop: 12 }}>
                    {showNativeTip && current.globalExtra.nativeTip ? (
                      <div style={{ marginBottom: 8 }}>
                        <div style={{ marginBottom: 4, color: '#555' }}>Native Tip：</div>
                        <div>{current.globalExtra.nativeTip}</div>
                      </div>
                    ) : null}
                  </div>
                ) : null}
              </div>
            ) : (
              <div
                style={{
                  background: '#fafafa',
                  border: '1px solid #f0f0f0',
                  borderRadius: 8,
                  padding: 16,
                  whiteSpace: 'pre-wrap',
                  lineHeight: 1.75,
                  fontSize: 14,
                  color: '#333',
                  maxHeight: 320,
                  overflow: 'auto',
                  marginBottom: 16,
                }}
              >
                {current?.backContent?.trim() ? current.backContent : '（无）'}
              </div>
            )}
            <p style={{ marginBottom: 8 }}>熟练度（1-5）：</p>
            <Slider
              min={1}
              max={5}
              value={proficiency}
              onChange={setProficiency}
              marks={{ 1: '1', 2: '2', 3: '3', 4: '4', 5: '5' }}
              style={{ marginBottom: 24 }}
            />
            <Space wrap>
              <Button type="primary" loading={submitting} onClick={handleSubmitReview}>
                提交并下一张
              </Button>
              <Dropdown.Button
                loading={postponingDays != null}
                menu={{
                  items: postponeMenuItems,
                  onClick: ({ key }) => handlePostpone(Number(key) as 1 | 2 | 7),
                }}
                onClick={() => handlePostpone(1)}
              >
                延后1天
              </Dropdown.Button>
            </Space>
          </>
        )}
      </Card>
    </div>
  );
}
