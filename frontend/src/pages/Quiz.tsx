import { useEffect, useState } from 'react';
import { Button, Card, Input, List, message, Progress, Radio, Select, Space, Switch, Tag } from 'antd';
import { getDocumentList } from '../services/documentService';
import {
  getQuizResult,
  getQuizSessionDetail,
  listQuizSessions,
  retryQuizWrong,
  startQuizSession,
  submitQuizAnswer,
} from '../services/quizService';
import type { DocumentDTO, QuizAnswerResponse, QuizQuestionDTO, QuizResultResponse } from '../types/api';
import { getAiConfig } from '../utils/aiConfigStorage';

/**
 * 文档测验：每张卡固定两题（释义写词条 -> 中译英句子）。
 */
export default function Quiz() {
  const ACTIVE_KEY = 'activeQuizSessionId';
  const [documents, setDocuments] = useState<DocumentDTO[]>([]);
  const [documentId, setDocumentId] = useState<number | undefined>();
  const [questionCount, setQuestionCount] = useState(10);
  const [prioritizeWrong, setPrioritizeWrong] = useState(true);
  const [prioritizeLow, setPrioritizeLow] = useState(true);
  const [useAiTempExamples, setUseAiTempExamples] = useState(false);
  const [loading, setLoading] = useState(false);
  const [sessionId, setSessionId] = useState<number | null>(null);
  const [questions, setQuestions] = useState<QuizQuestionDTO[]>([]);
  const [currentIdx, setCurrentIdx] = useState(0);
  const [answer, setAnswer] = useState('');
  const [answerExtra, setAnswerExtra] = useState('');
  const [choice, setChoice] = useState<string | undefined>();
  const [completed, setCompleted] = useState(false);
  const [result, setResult] = useState<QuizResultResponse | null>(null);
  const [showResult, setShowResult] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [history, setHistory] = useState<QuizResultResponse[]>([]);
  const [lastEval, setLastEval] = useState<QuizAnswerResponse | null>(null);
  const [answeredCurrent, setAnsweredCurrent] = useState(false);
  const [readyToShowFinalResult, setReadyToShowFinalResult] = useState(false);

  useEffect(() => {
    getDocumentList().then(setDocuments).catch(() => setDocuments([]));
  }, []);

  useEffect(() => {
    listQuizSessions().then(setHistory).catch(() => setHistory([]));
  }, []);

  useEffect(() => {
    // 恢复上次未完成的 session（切走/刷新回来不丢）
    const raw = localStorage.getItem(ACTIVE_KEY);
    const sid = raw ? Number(raw) : NaN;
    if (!sid || Number.isNaN(sid)) return;
    (async () => {
      try {
        const data = await getQuizSessionDetail(sid);
        setSessionId(data.sessionId);
        setQuestions(data.questions ?? []);
        setCurrentIdx(0);
        setAnswer('');
        setAnswerExtra('');
        setChoice(undefined);
        setCompleted(false);
        setResult(null);
      } catch {
        localStorage.removeItem(ACTIVE_KEY);
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const current = questions[currentIdx];

  useEffect(() => {
    setLastEval(null);
    setAnsweredCurrent(false);
    setReadyToShowFinalResult(false);
  }, [currentIdx]);

  const quizTypeLabel = (t: string | undefined) => {
    switch (t) {
      case 'FRONT_INPUT':
        return '考词条';
      case 'DEFINITION_INPUT':
        return '英释义写词条';
      case 'COMBINED_INPUT':
        return '词条+中译英';
      case 'SENTENCE_TRANSLATION':
        return '中译英句子';
      case 'SYNONYM_CHOICE':
        return '同义词';
      default:
        return t ?? '';
    }
  };

  const splitCombinedPrompt = (prompt?: string) => {
    if (!prompt) return { upper: '', lower: '' };
    const markerRegex = /(下半题|下题)\s*[：:]/;
    const match = markerRegex.exec(prompt);
    if (!match || match.index < 0) {
      return { upper: prompt.trim(), lower: '' };
    }
    const upper = prompt.slice(0, match.index).trim();
    const lower = prompt.slice(match.index).trim();
    return { upper, lower };
  };

  const handleStart = async () => {
    if (documentId == null) {
      message.warning('请选择文档');
      return;
    }
    setLoading(true);
    setResult(null);
    setLastEval(null);
    setShowResult(true);
    setCompleted(false);
    try {
      const ai = getAiConfig();
      const data = await startQuizSession({
        documentId,
        questionCount,
        prioritizeWrong,
        prioritizeLowProficiency: prioritizeLow,
        useAiTempExamples,
        aiApiKey: useAiTempExamples ? ai.apiKey : undefined,
        aiModel: useAiTempExamples ? ai.model : undefined,
        aiBaseUrl: useAiTempExamples ? ai.baseUrl : undefined,
      });
      setSessionId(data.sessionId);
      setQuestions(data.questions ?? []);
      setCurrentIdx(0);
      setAnswer('');
      setAnswerExtra('');
      setChoice(undefined);
      setAnsweredCurrent(false);
      setReadyToShowFinalResult(false);
      localStorage.setItem(ACTIVE_KEY, String(data.sessionId));
      message.success(`已生成 ${data.questions?.length ?? 0} 道题`);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '开始失败');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmitAnswer = async () => {
    if (sessionId == null || !current?.itemId) return;
    const finalAnswer = (current.options && current.options.length > 0 ? choice : answer.trim()) ?? '';
    const finalExtra = answerExtra.trim();
    if (!finalAnswer) {
      message.warning('请先作答');
      return;
    }
    if (current.type === 'COMBINED_INPUT' && !finalExtra) {
      message.warning('请补充下半题英文句子');
      return;
    }
    setSubmitting(true);
    try {
      const ai = getAiConfig();
      const res = await submitQuizAnswer(sessionId, current.itemId, finalAnswer, finalExtra, {
        aiApiKey: ai.apiKey,
        aiModel: ai.model,
        aiBaseUrl: ai.baseUrl,
      });
      setLastEval(res);
      setAnsweredCurrent(true);
      if (res.sessionCompleted) {
        setReadyToShowFinalResult(true);
      } else {
        setReadyToShowFinalResult(false);
      }
    } catch (e) {
      message.error(e instanceof Error ? e.message : '提交失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handleRetryWrong = async () => {
    if (sessionId == null) return;
    setLoading(true);
    try {
      const data = await retryQuizWrong(sessionId);
      setSessionId(data.sessionId);
      setQuestions(data.questions ?? []);
      setCurrentIdx(0);
      setAnswer('');
      setAnswerExtra('');
      setChoice(undefined);
      setCompleted(false);
      setResult(null);
      setLastEval(null);
      setAnsweredCurrent(false);
      setReadyToShowFinalResult(false);
      setShowResult(true);
      localStorage.setItem(ACTIVE_KEY, String(data.sessionId));
      message.success('已根据错题生成新会话');
    } catch (e) {
      message.error(e instanceof Error ? e.message : '重试失败');
    } finally {
      setLoading(false);
    }
  };

  const handleNextQuestion = async () => {
    if (!answeredCurrent) return;
    if (sessionId == null) return;
    if (readyToShowFinalResult) {
      try {
        const r = await getQuizResult(sessionId);
        setResult(r);
        setShowResult(true);
        setCompleted(true);
        localStorage.removeItem(ACTIVE_KEY);
        listQuizSessions().then(setHistory).catch(() => setHistory([]));
      } catch (e) {
        message.error(e instanceof Error ? e.message : '加载结果失败');
      }
      return;
    }
    setAnswer('');
    setAnswerExtra('');
    setChoice(undefined);
    setCurrentIdx((idx) => idx + 1);
  };

  const progressPercent =
    questions.length > 0
      ? Math.round(((completed ? questions.length : currentIdx) / questions.length) * 100)
      : 0;
  const combinedPromptParts = current?.type === 'COMBINED_INPUT' ? splitCombinedPrompt(current.prompt) : null;

  return (
    <div>
      <Card title="文档测验">
        <Space wrap style={{ marginBottom: 16 }}>
          <Select
            placeholder="选择文档"
            style={{ minWidth: 220 }}
            value={documentId}
            onChange={(v) => setDocumentId(v)}
            options={documents
              .filter((d) => d.id != null)
              .map((d) => ({ label: d.fileName, value: d.id as number }))}
            allowClear
          />
          <span>题组数</span>
          <Input
            type="number"
            min={1}
            max={50}
            value={questionCount}
            onChange={(e) => setQuestionCount(Number(e.target.value) || 10)}
            style={{ width: 80 }}
          />
          <Button type="primary" onClick={handleStart} loading={loading}>
            开始测验
          </Button>
          <Space>
            <span style={{ color: '#666' }}>优先错题</span>
            <Switch checked={prioritizeWrong} onChange={setPrioritizeWrong} />
          </Space>
          <Space>
            <span style={{ color: '#666' }}>优先低熟练度</span>
            <Switch checked={prioritizeLow} onChange={setPrioritizeLow} />
          </Space>
          <Space>
            <span style={{ color: '#666' }}>AI 临时例句</span>
            <Switch checked={useAiTempExamples} onChange={setUseAiTempExamples} />
          </Space>
          {result && result.correctCount < result.total ? (
            <Button onClick={handleRetryWrong} loading={loading}>
              错题再练
            </Button>
          ) : null}
        </Space>

        {questions.length > 0 && !completed && current ? (
          <>
            <Progress percent={progressPercent} />
            <p style={{ color: '#666', marginTop: 8 }}>
              第 {currentIdx + 1} / {questions.length} 题
            </p>
            <div style={{ marginTop: 12 }}>
              {current.type ? <Tag>{quizTypeLabel(current.type)}</Tag> : null}
            </div>
            {current.type !== 'COMBINED_INPUT' && current.prompt ? (
              <p style={{ marginTop: 8, fontSize: 15, lineHeight: 1.7, whiteSpace: 'pre-wrap' }}>
                {current.prompt}
              </p>
            ) : null}
            {current.type === 'COMBINED_INPUT' ? (
              <Card
                size="small"
                title="题干区"
                style={{ marginTop: 10, borderColor: '#ffd591', background: '#fff7e6' }}
              >
                <div style={{ display: 'grid', gap: 10 }}>
                  <div style={{ padding: '8px 10px', borderRadius: 6, background: '#fff', border: '1px solid #ffe7ba' }}>
                    <div style={{ fontWeight: 600, marginBottom: 4 }}>上半题线索（释义 / 同义词）</div>
                    <div style={{ whiteSpace: 'pre-wrap', lineHeight: 1.7 }}>
                      {combinedPromptParts?.upper || current.prompt || '（无）'}
                    </div>
                  </div>
                  <div style={{ padding: '8px 10px', borderRadius: 6, background: '#fff', border: '1px solid #ffe7ba' }}>
                    <div style={{ fontWeight: 600, marginBottom: 4 }}>下半题原文（中文待翻译）</div>
                    <div style={{ whiteSpace: 'pre-wrap', lineHeight: 1.7 }}>
                      {current.sentenceZh || combinedPromptParts?.lower || '（无）'}
                    </div>
                  </div>
                </div>
              </Card>
            ) : null}
            {current.sentenceEn ? (
              <p style={{ marginTop: 12, fontSize: 15, lineHeight: 1.7 }}>{current.sentenceEn}</p>
            ) : null}
            {current.sentenceZh && current.type !== 'COMBINED_INPUT' ? (
              <p style={{ color: '#666' }}>
                {current.type === 'FRONT_INPUT' ? `中文提示：${current.sentenceZh}` : current.sentenceZh}
              </p>
            ) : null}
            {current.options && current.options.length > 0 ? (
              <>
                <p style={{ marginTop: 12 }}>请选择一个答案：</p>
                <Radio.Group
                  value={choice}
                  onChange={(e) => {
                    if (answeredCurrent) return;
                    setChoice(e.target.value);
                  }}
                  style={{ display: 'flex', flexDirection: 'column', gap: 8, marginBottom: 12 }}
                >
                  {current.options.map((op) => (
                    <Radio key={op} value={op}>
                      {op}
                    </Radio>
                  ))}
                </Radio.Group>
              </>
            ) : current.type === 'COMBINED_INPUT' ? (
              <>
                <Card
                  size="small"
                  title="上半题：根据释义/同义词写词条"
                  style={{ marginTop: 12, borderColor: '#91caff', background: '#f0f8ff' }}
                >
                  <p style={{ marginTop: 0 }}>请输入「卡片正面」的英文词条（不要输入整句例句）：</p>
                  <Input
                    value={answer}
                    onChange={(e) => setAnswer(e.target.value)}
                    onPressEnter={handleSubmitAnswer}
                    placeholder="填写卡片正面的英文词条"
                    disabled={answeredCurrent}
                    style={{ maxWidth: 480 }}
                  />
                </Card>
                <Card
                  size="small"
                  title="下半题：英文翻译"
                  style={{ marginTop: 12, borderColor: '#d9d9d9', background: '#fafafa' }}
                >
                  <p style={{ marginTop: 0 }}>请把题干中的中文翻译成英文句子：</p>
                  <Input
                    value={answerExtra}
                    onChange={(e) => setAnswerExtra(e.target.value)}
                    onPressEnter={handleSubmitAnswer}
                    placeholder="请输入英文句子"
                    disabled={answeredCurrent}
                    style={{ maxWidth: 680 }}
                  />
                </Card>
              </>
            ) : (
              <>
                <p style={{ marginTop: 12 }}>
                  {current.type === 'FRONT_INPUT' || current.type === 'DEFINITION_INPUT'
                    ? '请输入「卡片正面」的英文词条（不要输入整句例句）：'
                    : current.type === 'SENTENCE_TRANSLATION'
                    ? '请把中文翻译成英文句子（尽量包含词条）：'
                    : '请输入答案：'}
                </p>
                <Input
                  value={answer}
                  onChange={(e) => setAnswer(e.target.value)}
                  onPressEnter={handleSubmitAnswer}
                  placeholder={
                    current.type === 'FRONT_INPUT' || current.type === 'DEFINITION_INPUT'
                      ? '英文 단어 또는 짧은 구문（勿写整句）'
                      : current.type === 'SENTENCE_TRANSLATION'
                      ? '请输入英文句子'
                      : '英文答案'
                  }
                  disabled={answeredCurrent}
                  style={{ maxWidth: 480, marginBottom: 12 }}
                />
              </>
            )}
            <Button type="primary" onClick={handleSubmitAnswer} loading={submitting} disabled={answeredCurrent}>
              提交本题
            </Button>
            {lastEval ? (
              <Card size="small" style={{ marginTop: 12, borderColor: lastEval.correct ? '#b7eb8f' : '#ffccc7' }}>
                <div style={{ color: '#666' }}>
                  判分：{lastEval.verdict ?? '-'}{lastEval.score != null ? `（${lastEval.score}分）` : ''}
                  {lastEval.feedback ? `；反馈：${lastEval.feedback}` : ''}
                </div>
                {current.type === 'COMBINED_INPUT' ? (
                  <div style={{ marginTop: 8, display: 'grid', gap: 8 }}>
                    <div style={{ padding: '8px 10px', background: '#fafafa', border: '1px solid #e5e5e5', borderRadius: 6 }}>
                      <strong>上半题（词条）判定：</strong>
                      <span style={{ marginLeft: 6, color: lastEval.frontCorrect ? '#52c41a' : '#ff4d4f' }}>
                        {lastEval.frontCorrect ? '正确' : '错误'}
                      </span>
                      {lastEval.frontFeedback ? <div style={{ marginTop: 4, color: '#666' }}>{lastEval.frontFeedback}</div> : null}
                    </div>
                    <div style={{ padding: '8px 10px', background: '#fafafa', border: '1px solid #e5e5e5', borderRadius: 6 }}>
                      <strong>下半题（翻译）判定：</strong>
                      <span
                        style={{
                          marginLeft: 6,
                          color:
                            lastEval.sentenceVerdict === 'CORRECT'
                              ? '#52c41a'
                              : lastEval.sentenceVerdict === 'PARTIAL'
                              ? '#faad14'
                              : '#ff4d4f',
                        }}
                      >
                        {lastEval.sentenceVerdict ?? '-'}
                        {lastEval.sentenceScore != null ? `（${lastEval.sentenceScore}分）` : ''}
                      </span>
                      {lastEval.sentenceFeedback ? <div style={{ marginTop: 4, color: '#666' }}>{lastEval.sentenceFeedback}</div> : null}
                    </div>
                  </div>
                ) : (
                  <div style={{ marginTop: 6 }}>
                    结果：{lastEval.correct ? <span style={{ color: '#52c41a' }}>正确</span> : <span style={{ color: '#ff4d4f' }}>错误</span>}
                  </div>
                )}
                <div style={{ marginTop: 6, color: '#999' }}>本题已提交并锁定，请查看对照后点击下一题。</div>
                {(lastEval.expectedFront || lastEval.expectedSentence) && !current.options?.length ? (
                  <div style={{ marginTop: 10, display: 'grid', gap: 8 }}>
                    <div style={{ padding: '8px 10px', background: '#f6ffed', border: '1px solid #b7eb8f', borderRadius: 6 }}>
                      <strong>你的答案</strong>
                      <div style={{ marginTop: 4 }}>词条：{answer || '（空）'}</div>
                      {(current.type === 'COMBINED_INPUT' || lastEval.expectedSentence) ? (
                        <div style={{ marginTop: 2 }}>翻译：{answerExtra || '（空）'}</div>
                      ) : null}
                    </div>
                    <div style={{ padding: '8px 10px', background: '#fff2f0', border: '1px solid #ffccc7', borderRadius: 6 }}>
                      <strong>标准答案</strong>
                      <div style={{ marginTop: 4 }}>词条：{lastEval.expectedFront || '（空）'}</div>
                      {(current.type === 'COMBINED_INPUT' || lastEval.expectedSentence) ? (
                        <div style={{ marginTop: 2 }}>翻译：{lastEval.expectedSentence || '（空）'}</div>
                      ) : null}
                    </div>
                  </div>
                ) : null}
              </Card>
            ) : null}
            <div style={{ marginTop: 12 }}>
              <Button onClick={handleNextQuestion} disabled={!answeredCurrent}>
                {readyToShowFinalResult ? '查看本轮结果' : '下一题'}
              </Button>
            </div>
          </>
        ) : null}

        {result && showResult ? (
          <Card
            type="inner"
            title="本轮结果"
            style={{ marginTop: 24 }}
            extra={
              <Button size="small" onClick={() => setShowResult(false)}>
                关闭
              </Button>
            }
          >
            <p>
              正确 {result.correctCount} / {result.total}
            </p>
            <ul style={{ paddingLeft: 18 }}>
              {result.items.map((it) => (
                <li key={it.itemId} style={{ marginBottom: 10 }}>
                  <span style={{ color: it.isCorrect ? '#52c41a' : '#ff4d4f' }}>
                    {it.isCorrect ? '✓' : '✗'}
                  </span>{' '}
                  {it.sentenceEn?.slice(0, 80)}
                  {it.sentenceEn && it.sentenceEn.length > 80 ? '…' : ''}
                  <div style={{ marginTop: 4, color: '#666' }}>
                    你的答案：{it.userAnswer || '（空）'}；正确答案：{it.expected || '（空）'}
                  </div>
                </li>
              ))}
            </ul>
          </Card>
        ) : null}

        {result && !showResult ? (
          <Button style={{ marginTop: 16 }} onClick={() => setShowResult(true)}>
            展开本轮结果
          </Button>
        ) : null}

        {!(questions.length > 0 && !completed) ? (
          <Card type="inner" title="历史测验（可回看）" style={{ marginTop: 24 }}>
            <List
              dataSource={history}
              renderItem={(s) => (
                <List.Item
                  actions={[
                    <Button
                      key="view"
                      onClick={async () => {
                        try {
                          const r = await getQuizResult(s.sessionId);
                          setSessionId(s.sessionId);
                          setResult(r);
                          setShowResult(true);
                          setCompleted(true);
                        } catch {
                          message.error('加载失败');
                        }
                      }}
                    >
                      查看
                    </Button>,
                  ]}
                >
                  <List.Item.Meta
                    title={`Session #${s.sessionId}`}
                    description={`正确 ${s.correctCount} / ${s.total}`}
                  />
                </List.Item>
              )}
            />
          </Card>
        ) : null}
      </Card>
    </div>
  );
}
