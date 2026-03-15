import { useEffect, useState, useMemo } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { Button, Card, message, Modal, Input, Switch } from 'antd';
import { getDocument } from '../services/documentService';
import { createCard, getCardList } from '../services/cardService';
import { generateNote } from '../services/aiService';
import { getAiConfig, getDocAiNoteEnabled, setDocAiNoteEnabled } from '../utils/aiConfigStorage';
import type { CardDTO, DocumentDTO } from '../types/api';

/**
 * 文档查看页：展示文档纯文本内容。
 * 顶部可控制是否开启 AI 注释；选中文本后打开“生成卡片”弹窗时，若已开启 AI 则立即调 AI 生成注释并填入弹窗，用户可编辑后再创建卡片。
 */
type DocSegment = { type: 'text'; content: string } | { type: 'card'; content: string; cardId: number };

export default function DocumentView() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const [doc, setDoc] = useState<DocumentDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [selectedText, setSelectedText] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [backContent, setBackContent] = useState('');
  const [contextSentence, setContextSentence] = useState('');
  const [noteLoading, setNoteLoading] = useState(false);
  const [aiNoteEnabled, setAiNoteEnabled] = useState(() => getDocAiNoteEnabled());
  const [createLoading, setCreateLoading] = useState(false);
  const [selectedOffsets, setSelectedOffsets] = useState<{ start: number; end: number } | null>(null);
  const [docCards, setDocCards] = useState<CardDTO[]>([]);

  const docId = id ? Number(id) : 0;

  /** 根据文档内容与已生成卡片构建高亮区间（有 offset 用 offset，否则按正面内容匹配） */
  const highlightRanges = useMemo((): { start: number; end: number; cardId: number }[] => {
    if (!doc?.content || docCards.length === 0) return [];
    const content = doc.content;
    const ranges: { start: number; end: number; cardId: number }[] = [];
    for (const card of docCards) {
      if (!card.id) continue;
      let start: number;
      let end: number;
      if (card.startOffset != null && card.endOffset != null && card.startOffset < content.length && card.endOffset <= content.length) {
        start = card.startOffset;
        end = card.endOffset;
      } else if (card.frontContent) {
        const idx = content.indexOf(card.frontContent);
        if (idx < 0) continue;
        start = idx;
        end = idx + card.frontContent.length;
      } else continue;
      ranges.push({ start, end, cardId: card.id });
    }
    ranges.sort((a, b) => a.start - b.start);
    const merged: { start: number; end: number; cardId: number }[] = [];
    for (const r of ranges) {
      if (merged.length > 0 && r.start < merged[merged.length - 1].end) continue;
      merged.push(r);
    }
    return merged;
  }, [doc?.content, docCards]);

  /** 将文档内容拆成段落（普通文本 + 可点击高亮） */
  const docSegments = useMemo((): DocSegment[] => {
    if (!doc?.content) return [];
    if (highlightRanges.length === 0) return [{ type: 'text', content: doc.content }];
    const segments: DocSegment[] = [];
    let pos = 0;
    for (const r of highlightRanges) {
      if (r.start > pos) segments.push({ type: 'text', content: doc.content.slice(pos, r.start) });
      segments.push({ type: 'card', content: doc.content.slice(r.start, r.end), cardId: r.cardId });
      pos = r.end;
    }
    if (pos < doc.content.length) segments.push({ type: 'text', content: doc.content.slice(pos) });
    return segments;
  }, [doc?.content, highlightRanges]);

  /** 从卡片编辑页「在文档中定位」跳转时滚动到对应高亮 */
  useEffect(() => {
    const hash = location.hash;
    if (hash.startsWith('#card-')) {
      const cardId = hash.slice(6);
      const el = document.getElementById(`card-${cardId}`);
      if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }, [location.hash, docSegments.length]);

  /** 从文档内容中取出包含选中文本的段落，作为 AI 上下文 */
  function getParagraphContainingSelection(fullText: string, selected: string): string {
    if (!fullText || !selected) return '';
    const idx = fullText.indexOf(selected);
    if (idx < 0) return '';
    const before = fullText.slice(0, idx);
    const after = fullText.slice(idx + selected.length);
    const start = Math.max(0, (Math.max(before.lastIndexOf('\n\n'), before.lastIndexOf('\n')) + 1) || 0);
    const endNN = after.indexOf('\n\n');
    const end = endNN >= 0 ? idx + selected.length + endNN + 2 : fullText.length;
    return fullText.slice(start, end).trim();
  }

  useEffect(() => {
    if (!docId) return;
    setLoading(true);
    Promise.all([getDocument(docId), getCardList({ documentId: docId })])
      .then(([documentData, cards]) => {
        setDoc(documentData);
        setDocCards(cards ?? []);
      })
      .catch((e) => message.error(e instanceof Error ? e.message : '加载失败'))
      .finally(() => setLoading(false));
  }, [docId]);

  const handleToggleAiNote = (enabled: boolean) => {
    setAiNoteEnabled(enabled);
    setDocAiNoteEnabled(enabled);
  };

  // 选中文本在文档内容中的起始/结束偏移（用于保存到卡片并高亮）
  const getSelectionOffsets = (text: string): { start: number; end: number } | null => {
    if (!doc?.content || !text) return null;
    const idx = doc.content.indexOf(text);
    if (idx < 0) return null;
    return { start: idx, end: idx + text.length };
  };

  // 选中文本后触发：保存选中内容并打开“生成卡片”弹窗
  const handleMouseUp = () => {
    const selection = window.getSelection();
    const text = selection?.toString().trim();
    if (text && doc?.content) {
      setSelectedText(text);
      setBackContent('');
      setContextSentence(getParagraphContainingSelection(doc.content, text));
      setSelectedOffsets(getSelectionOffsets(text));
      setModalOpen(true);
    }
  };

  // 弹窗打开且开启 AI 注释时，立即请求生成并直接填入「背面」框
  useEffect(() => {
    if (!modalOpen || !aiNoteEnabled || !selectedText) return;
    const config = getAiConfig();
    if (!config.apiKey?.trim()) {
      return;
    }
    setNoteLoading(true);
    generateNote({
      frontContent: selectedText,
      contextSentence: contextSentence || '',
      aiApiKey: config.apiKey.trim(),
      aiModel: config.model?.trim() || undefined,
      aiBaseUrl: config.baseUrl?.trim() || undefined,
    })
      .then((content) => setBackContent(content || ''))
      .catch(() => message.error('AI 释义生成失败'))
      .finally(() => setNoteLoading(false));
  }, [modalOpen, aiNoteEnabled, selectedText, contextSentence]);

  const handleCreateCard = async () => {
    const userId = localStorage.getItem('userId');
    if (!userId) return;
    const config = getAiConfig();
    if (aiNoteEnabled && !config.apiKey?.trim()) {
      message.warning('已开启 AI 注释，请先在「设置」中填写 API Key');
      return;
    }
    setCreateLoading(true);
    try {
      const payload: CardDTO = {
        userId: Number(userId),
        documentId: docId,
        frontContent: selectedText,
        backContent: backContent || undefined,
        contextSentence: contextSentence || undefined,
        startOffset: selectedOffsets?.start,
        endOffset: selectedOffsets?.end,
      };
      if (aiNoteEnabled && backContent.trim()) {
        payload.aiNoteContent = backContent.trim();
      } else if (aiNoteEnabled && !backContent.trim()) {
        payload.useAiNote = true;
        payload.aiApiKey = config.apiKey?.trim() ?? '';
        payload.aiModel = config.model?.trim() || undefined;
        payload.aiBaseUrl = config.baseUrl?.trim() || undefined;
      }
      const created = await createCard(payload);
      setModalOpen(false);
      setSelectedText('');
      if (aiNoteEnabled && created?.id) {
        message.success('卡片已创建');
        setDocCards((prev) => (created ? [created, ...prev] : prev));
        navigate(`/cards/${created.id}/edit`);
      } else {
        message.success('卡片已创建');
        if (created) setDocCards((prev) => [created, ...prev]);
      }
    } catch (e) {
      message.error(e instanceof Error ? e.message : '创建失败');
    } finally {
      setCreateLoading(false);
    }
  };

  if (loading || !doc) {
    return <div style={{ padding: 24 }}>加载中...</div>;
  }

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
        <Button onClick={() => navigate('/documents')}>返回列表</Button>
        <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Switch checked={aiNoteEnabled} onChange={handleToggleAiNote} />
          <span>开启 AI 注释（生成卡片时将自动调 AI 填充注释，可在编辑页调整）</span>
        </span>
      </div>
      <Card title={doc.fileName}>
        <div
          role="article"
          style={{ whiteSpace: 'pre-wrap', minHeight: 400, userSelect: 'text' }}
          onMouseUp={handleMouseUp}
        >
          {doc.content ? (
            docSegments.map((seg, i) =>
              seg.type === 'text' ? (
                <span key={i}>{seg.content}</span>
              ) : (
                <span
                  key={i}
                  id={`card-${seg.cardId}`}
                  role="button"
                  tabIndex={0}
                  onClick={(e) => { e.preventDefault(); navigate(`/cards/${seg.cardId}/edit`); }}
                  onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); navigate(`/cards/${seg.cardId}/edit`); } }}
                  style={{ backgroundColor: 'rgba(24, 144, 255, 0.2)', cursor: 'pointer', borderRadius: 2 }}
                  title="点击编辑卡片"
                >
                  {seg.content}
                </span>
              )
            )
          ) : (
            '（无内容）'
          )}
        </div>
        <p style={{ marginTop: 12, color: '#666' }}>
          提示：在文档中选中单词或句子，可生成学习卡片。{aiNoteEnabled && '当前已开启 AI 注释，创建卡片时会自动生成释义与例句。'}
        </p>
      </Card>

      <Modal
        title="生成学习卡片"
        open={modalOpen}
        onCancel={() => {
          setModalOpen(false);
          setNoteLoading(false);
        }}
        onOk={handleCreateCard}
        confirmLoading={createLoading}
        okText="创建卡片"
      >
        <p><strong>正面（选中的内容）：</strong></p>
        <p style={{ background: '#f5f5f5', padding: 8, borderRadius: 4 }}>{selectedText}</p>
        <p style={{ marginTop: 12 }}><strong>背面（释义/例句，选填）：</strong></p>
        {noteLoading ? (
          <p style={{ color: '#666', marginBottom: 8 }}>正在生成释义与例句…</p>
        ) : null}
        <Input.TextArea
          rows={6}
          value={backContent}
          onChange={(e) => setBackContent(e.target.value)}
          placeholder={aiNoteEnabled ? '打开弹窗时自动生成，可直接修改' : '可填写释义、例句等'}
          disabled={noteLoading}
        />
        <p style={{ marginTop: 16 }}><strong>上下文（选填，供 AI 消歧与生成例句）：</strong></p>
        <Input.TextArea
          rows={2}
          value={contextSentence}
          onChange={(e) => setContextSentence(e.target.value)}
          placeholder="默认已填入选中内容所在段落"
        />
      </Modal>
    </div>
  );
}
