import { useEffect, useState, useMemo } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { Button, Card, message, Modal, Input, Switch, Pagination } from 'antd';
import { getDocument, downloadDocumentOriginal } from '../services/documentService';
import { createCard, getCardRanges } from '../services/cardService';
import { generateNote } from '../services/aiService';
import { getAiConfig, getDocAiNoteEnabled, setDocAiNoteEnabled } from '../utils/aiConfigStorage';
import type { CardDTO, DocumentDTO } from '../types/api';
import type { CardRangeDTO } from '../services/cardService';

/**
 * 文档查看页：展示文档纯文本内容。
 * 顶部可控制是否开启 AI 注释；选中文本后打开“生成卡片”弹窗时，若已开启 AI 则立即调 AI 生成注释并填入弹窗，用户可编辑后再创建卡片。
 * 展示时按段落与换行渲染，不影响卡片 startOffset/endOffset（仍基于 doc.content 字符偏移）。
 */
type DocSegment = { type: 'text'; content: string } | { type: 'card'; content: string; cardId: number };

/** 每页展示行数（按 \n 拆行，前端直接分页） */
const LINES_PER_PAGE = 40;

export default function DocumentView() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const [doc, setDoc] = useState<DocumentDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [selectedText, setSelectedText] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [backContent, setBackContent] = useState('');
  const [noteLoading, setNoteLoading] = useState(false);
  const [aiNoteEnabled, setAiNoteEnabled] = useState(() => getDocAiNoteEnabled());
  const [selectionPopupEnabled, setSelectionPopupEnabled] = useState(() => {
    const v = localStorage.getItem('english-learn-selection-popup-enabled');
    return v == null ? true : v === 'true';
  });
  const [createLoading, setCreateLoading] = useState(false);
  const [selectedOffsets, setSelectedOffsets] = useState<{ start: number; end: number } | null>(null);
  const [docCards, setDocCards] = useState<CardRangeDTO[]>([]);
  const [currentPage, setCurrentPage] = useState(1);

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

  /** 按行分页：先按 \n 拆成行，每页 LINES_PER_PAGE 行，得到每页的字符区间 */
  const pageRanges = useMemo((): { start: number; end: number }[] => {
    if (!doc?.content) return [];
    const content = doc.content;
    const lines = content.split('\n');
    const lineOffsets: number[] = [0];
    let pos = 0;
    for (let i = 0; i < lines.length - 1; i++) {
      pos += lines[i].length + 1;
      lineOffsets.push(pos);
    }
    const ranges: { start: number; end: number }[] = [];
    for (let startLine = 0; startLine < lines.length; startLine += LINES_PER_PAGE) {
      const endLine = Math.min(startLine + LINES_PER_PAGE, lines.length);
      const pageStart = lineOffsets[startLine];
      const pageEnd = endLine < lines.length ? lineOffsets[endLine] : content.length;
      ranges.push({ start: pageStart, end: pageEnd });
    }
    if (ranges.length === 0) ranges.push({ start: 0, end: content.length });
    return ranges;
  }, [doc?.content]);

  const totalPages = Math.max(1, pageRanges.length);
  const currentRange = pageRanges[currentPage - 1] ?? pageRanges[0];

  /** 当前页内的 segments（文本与卡片高亮均内联展示，选中词不单独成行） */
  const pageSegments = useMemo((): DocSegment[] => {
    if (!doc?.content || !currentRange) return [];
    const { start: pageStart, end: pageEnd } = currentRange;
    const content = doc.content;
    const ranges = highlightRanges.filter((r) => r.end > pageStart && r.start < pageEnd);
    if (ranges.length === 0) return [{ type: 'text', content: content.slice(pageStart, pageEnd) }];
    const segs: DocSegment[] = [];
    let pos = pageStart;
    for (const r of ranges) {
      const rStart = Math.max(r.start, pageStart);
      const rEnd = Math.min(r.end, pageEnd);
      if (rStart > pos) segs.push({ type: 'text', content: content.slice(pos, rStart) });
      segs.push({ type: 'card', content: content.slice(rStart, rEnd), cardId: r.cardId });
      pos = rEnd;
    }
    if (pos < pageEnd) segs.push({ type: 'text', content: content.slice(pos, pageEnd) });
    return segs;
  }, [doc?.content, highlightRanges, currentRange]);

  /** 从卡片编辑页「在文档中定位」时切到卡片所在页 */
  useEffect(() => {
    if (!location.hash.startsWith('#card-') || pageRanges.length === 0) return;
    const cardId = location.hash.slice(6);
    const card = docCards.find((c) => String(c.id) === cardId);
    if (!card?.id) return;
    const content = doc?.content ?? '';
    let cardStart = content.length;
    if (card.startOffset != null && card.endOffset != null) cardStart = card.startOffset;
    else if (card.frontContent) { const i = content.indexOf(card.frontContent); if (i >= 0) cardStart = i; }
    const pageIndex = pageRanges.findIndex((r) => r.start <= cardStart && r.end > cardStart);
    if (pageIndex >= 0) setCurrentPage(pageIndex + 1);
    setTimeout(() => {
      const el = document.getElementById(`card-${cardId}`);
      if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }, 100);
  }, [location.hash, pageRanges, doc?.content, docCards]);

  useEffect(() => {
    if (!docId) return;
    setLoading(true);
    setCurrentPage(1);
    Promise.all([getDocument(docId), getCardRanges(docId)])
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

  const handleToggleSelectionPopup = (enabled: boolean) => {
    setSelectionPopupEnabled(enabled);
    localStorage.setItem('english-learn-selection-popup-enabled', enabled ? 'true' : 'false');
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
    if (!selectionPopupEnabled) return;
    const selection = window.getSelection();
    const text = selection?.toString().trim();
    if (text && doc?.content) {
      setSelectedText(text);
      setBackContent('');
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
      aiApiKey: config.apiKey.trim(),
      aiModel: config.model?.trim() || undefined,
      aiBaseUrl: config.baseUrl?.trim() || undefined,
      aiNotePrompt: config.notePrompt?.trim() || undefined,
    })
      .then((content) => setBackContent(content || ''))
      .catch(() => message.error('AI 释义生成失败'))
      .finally(() => setNoteLoading(false));
  }, [modalOpen, aiNoteEnabled, selectedText]);

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
        payload.aiNotePrompt = config.notePrompt?.trim() || undefined;
      }
      const created = await createCard(payload);
      setModalOpen(false);
      setSelectedText('');
      message.success('卡片已创建，可继续在文档中选词生成');
      if (created?.id != null) {
        const newCard: CardRangeDTO = {
          id: created.id,
          startOffset: created.startOffset,
          endOffset: created.endOffset,
          frontContent: created.frontContent ?? '',
        };
        setDocCards((prev) => [newCard, ...prev]);
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
        {doc.originalAvailable && (
          <Button
            onClick={async () => {
              try {
                await downloadDocumentOriginal(docId, doc.fileName);
              } catch (e) {
                message.error(e instanceof Error ? e.message : '下载失败');
              }
            }}
          >
            查看原件
          </Button>
        )}
        <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Switch checked={selectionPopupEnabled} onChange={handleToggleSelectionPopup} />
          <span>选词弹出创建卡片</span>
        </span>
        <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Switch checked={aiNoteEnabled} onChange={handleToggleAiNote} />
          <span>开启 AI 注释（生成卡片时将自动调 AI 填充注释，可在编辑页调整）</span>
        </span>
      </div>
      <Card title={doc.fileName}>
        <div
          role="article"
          className="doc-content"
          style={{
            minHeight: 360,
            userSelect: 'text',
            width: '100%',
            lineHeight: 1.8,
            fontSize: 15,
            color: '#333',
            padding: '8px 0',
          }}
          onMouseUp={handleMouseUp}
        >
          {doc.content ? (
            pageSegments.length > 0 ? (
              <div style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                {pageSegments.map((seg, i) =>
                  seg.type === 'text' ? (
                    <span key={i}>
                      {seg.content.split('\n').map((line, j, arr) => (
                        <span key={j}>
                          {line.replace(/\[图片]/g, '')}
                          {j < arr.length - 1 ? <br /> : null}
                        </span>
                      ))}
                    </span>
                  ) : (
                    <span
                      key={i}
                      id={`card-${seg.cardId}`}
                      role="button"
                      tabIndex={0}
                      onClick={(e) => { e.preventDefault(); navigate(`/cards/${seg.cardId}/edit`); }}
                      onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); navigate(`/cards/${seg.cardId}/edit`); } }}
                      style={{
                        backgroundColor: 'rgba(24, 144, 255, 0.22)',
                        cursor: 'pointer',
                        borderRadius: 3,
                        padding: '0 2px',
                      }}
                      title="点击编辑卡片"
                    >
                      {seg.content.replace(/\[图片]/g, '')}
                    </span>
                  )
                )}
              </div>
            ) : (
              <div style={{ color: '#999' }}>（无内容）</div>
            )
          ) : (
            '（无内容）'
          )}
        </div>
        {totalPages > 1 && (
          <div style={{ marginTop: 16, display: 'flex', justifyContent: 'center', flexWrap: 'wrap' }}>
            <Pagination
              current={currentPage}
              total={totalPages}
              pageSize={1}
              showSizeChanger={false}
              showTotal={() => `共 ${totalPages} 页`}
              onChange={setCurrentPage}
            />
          </div>
        )}
        <p style={{ marginTop: 12, color: '#666', fontSize: 13 }}>
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
      </Modal>
    </div>
  );
}
