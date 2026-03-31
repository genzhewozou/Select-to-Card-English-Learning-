import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Form,
  Input,
  Button,
  Card,
  message,
  Space,
  Collapse,
  Divider,
} from 'antd';
import { SoundOutlined, PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import { getCard, createCard, updateCard, saveStructuredCard } from '../services/cardService';
import { useTTS } from '../hooks/useTTS';
import type {
  CardDTO,
  CardStructuredSaveRequest,
  CardStructuredSensePayload,
  CardStructuredExamplePayload,
  CardStructuredSynonymPayload,
  CardStructuredGlobalPayload,
} from '../types/api';

function emptyStructured(): CardStructuredSaveRequest {
  return {
    senses: [],
    globalExtra: { collocations: [] },
  };
}

/** 从详情 DTO 转为保存请求 */
function cardToStructuredSave(card: CardDTO): CardStructuredSaveRequest {
  const senses: CardStructuredSensePayload[] = (card.senses ?? []).map((s, si) => ({
    order: s.sortOrder ?? si + 1,
    translationZh: s.translationZh ?? '',
    explanationEn: s.explanationEn ?? '',
    examples: (s.examples ?? []).map((e, ei) => ({
      order: e.sortOrder ?? ei + 1,
      en: e.sentenceEn ?? '',
      zh: e.sentenceZh ?? '',
    })),
    synonyms: (s.synonyms ?? []).map((y, yi) => ({
      order: y.sortOrder ?? yi + 1,
      lemma: y.lemma ?? '',
    })),
  }));
  const g = card.globalExtra;
  let globalExtra: CardStructuredGlobalPayload | undefined;
  if (g && ((g.collocations?.length ?? 0) > 0 || g.nativeTip || g.highLevelEn || g.highLevelZh)) {
    globalExtra = {
      collocations: [...(g.collocations ?? [])],
      nativeTip: g.nativeTip ?? '',
      highLevelEn: g.highLevelEn ?? '',
      highLevelZh: g.highLevelZh ?? '',
    };
  }
  return { senses, globalExtra };
}

function buildBackSummary(card?: CardDTO | null) {
  const senses = card?.senses ?? [];
  if (senses.length === 0) return '';
  return senses
    .map((s, idx) => {
      const zh = (s.translationZh ?? '').trim();
      const en = (s.explanationEn ?? '').trim();
      const lines = [`【释义 ${idx + 1}】`];
      if (zh) lines.push(`ZH: ${zh}`);
      if (en) lines.push(`EN: ${en}`);
      return lines.join('\n');
    })
    .join('\n\n');
}

/**
 * 卡片编辑：正面、背面；义项 / 例句 / 同义词结构化编辑。
 */
export default function CardEdit() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(!!id);
  const [submitting, setSubmitting] = useState(false);
  const [structuring, setStructuring] = useState(false);
  const [form] = Form.useForm();
  const [documentId, setDocumentId] = useState<number | null>(null);
  const [structured, setStructured] = useState<CardStructuredSaveRequest>(() => emptyStructured());
  const isEdit = id && id !== 'new';
  const { speak, stop, isSpeaking, isSupported } = useTTS();

  const handlePreview = (field: 'frontContent' | 'backContent') => {
    const text = form.getFieldValue(field);
    if (!text?.trim()) {
      message.info(field === 'frontContent' ? '请先输入正面内容' : '请先输入背面内容');
      return;
    }
    if (!isSupported) {
      message.info('当前浏览器不支持试听，请使用 Chrome、Edge 等现代浏览器');
      return;
    }
    if (isSpeaking) {
      stop();
      return;
    }
    speak(text);
  };

  useEffect(() => {
    if (!isEdit || !id) return;
    getCard(Number(id))
      .then((data) => {
        form.setFieldsValue({
          frontContent: data.frontContent,
          backContent: buildBackSummary(data),
        });
        setDocumentId(data.documentId ?? null);
        setStructured(
          data.senses?.length || data.globalExtra ? cardToStructuredSave(data) : emptyStructured(),
        );
      })
      .catch((e) => message.error(e instanceof Error ? e.message : '加载失败'))
      .finally(() => setLoading(false));
  }, [isEdit, id, form]);

  const onFinish = async (values: Record<string, string>) => {
    const userId = localStorage.getItem('userId');
    if (!userId) return;
    setSubmitting(true);
    try {
      if (isEdit && id) {
        await updateCard(Number(id), {
          frontContent: values.frontContent,
        });
        message.success('保存成功');
      } else {
        await createCard({
          userId: Number(userId),
          frontContent: values.frontContent,
        });
        message.success('创建成功');
      }
      navigate('/cards');
    } catch (e) {
      message.error(e instanceof Error ? e.message : '保存失败');
    } finally {
      setSubmitting(false);
    }
  };

  const updateSense = (index: number, patch: Partial<CardStructuredSensePayload>) => {
    setStructured((prev) => {
      const senses = [...(prev.senses ?? [])];
      senses[index] = { ...senses[index], ...patch };
      return { ...prev, senses };
    });
  };

  const addSense = () => {
    setStructured((prev) => {
      const senses = [...(prev.senses ?? [])];
      const n = senses.length + 1;
      senses.push({
        order: n,
        translationZh: '',
        explanationEn: '',
        examples: [],
        synonyms: [],
      });
      return { ...prev, senses };
    });
  };

  const removeSense = (index: number) => {
    setStructured((prev) => {
      const senses = [...(prev.senses ?? [])];
      senses.splice(index, 1);
      return { ...prev, senses };
    });
  };

  const addExample = (si: number) => {
    setStructured((prev) => {
      const senses = [...(prev.senses ?? [])];
      const ex = senses[si].examples ?? [];
      senses[si] = {
        ...senses[si],
        examples: [...ex, { order: ex.length + 1, en: '', zh: '', tag: '' }],
      };
      return { ...prev, senses };
    });
  };

  const updateExample = (si: number, ei: number, patch: Partial<CardStructuredExamplePayload>) => {
    setStructured((prev) => {
      const senses = [...(prev.senses ?? [])];
      const examples = [...(senses[si].examples ?? [])];
      examples[ei] = { ...examples[ei], ...patch };
      senses[si] = { ...senses[si], examples };
      return { ...prev, senses };
    });
  };

  const removeExample = (si: number, ei: number) => {
    setStructured((prev) => {
      const senses = [...(prev.senses ?? [])];
      const examples = [...(senses[si].examples ?? [])];
      examples.splice(ei, 1);
      senses[si] = { ...senses[si], examples };
      return { ...prev, senses };
    });
  };

  const addSynonym = (si: number) => {
    setStructured((prev) => {
      const senses = [...(prev.senses ?? [])];
      const sy = senses[si].synonyms ?? [];
      senses[si] = {
        ...senses[si],
        synonyms: [...sy, { order: sy.length + 1, lemma: '' }],
      };
      return { ...prev, senses };
    });
  };

  const updateSynonym = (si: number, yi: number, patch: Partial<CardStructuredSynonymPayload>) => {
    setStructured((prev) => {
      const senses = [...(prev.senses ?? [])];
      const syn = [...(senses[si].synonyms ?? [])];
      syn[yi] = { ...syn[yi], ...patch };
      senses[si] = { ...senses[si], synonyms: syn };
      return { ...prev, senses };
    });
  };

  const removeSynonym = (si: number, yi: number) => {
    setStructured((prev) => {
      const senses = [...(prev.senses ?? [])];
      const syn = [...(senses[si].synonyms ?? [])];
      syn.splice(yi, 1);
      senses[si] = { ...senses[si], synonyms: syn };
      return { ...prev, senses };
    });
  };

  const handleSaveStructured = async () => {
    if (!isEdit || !id) return;
    setStructuring(true);
    try {
      const card = await saveStructuredCard(Number(id), structured);
      const summary = buildBackSummary(card);
      form.setFieldsValue({ backContent: summary });
      message.success('结构化内容已保存，背面已同步汇总');
    } catch (e) {
      message.error(e instanceof Error ? e.message : '保存失败');
    } finally {
      setStructuring(false);
    }
  };

  if (loading) {
    return <div style={{ padding: 24 }}>加载中...</div>;
  }

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', alignItems: 'center', gap: 8 }}>
        <Button onClick={() => navigate('/cards')}>返回列表</Button>
        {isEdit && documentId != null && id && (
          <Button type="link" onClick={() => navigate(`/documents/${documentId}#card-${id}`)}>
            在文档中定位
          </Button>
        )}
      </div>
      <Card title={isEdit ? '编辑卡片' : '新建卡片'} bodyStyle={{ paddingBottom: 8 }} className="sense-editor-shell">
        <Form form={form} layout="vertical" onFinish={onFinish}>
          <Form.Item
            name="frontContent"
            label={
              <Space>
                正面内容
                <Button
                  type="link"
                  size="small"
                  icon={<SoundOutlined />}
                  onClick={() => handlePreview('frontContent')}
                  disabled={isSpeaking}
                  style={{ padding: 0 }}
                >
                  {isSpeaking ? '停止' : '试听'}
                </Button>
              </Space>
            }
            rules={[{ required: true, message: '请输入正面内容' }]}
          >
            <Input.TextArea rows={3} placeholder="单词或句子" />
          </Form.Item>
          <Collapse
            defaultActiveKey={['back']}
            items={[
              {
                key: 'back',
                label: (
                  <Space>
                    <span>背面内容（可折叠）</span>
                    <Button
                      type="link"
                      size="small"
                      icon={<SoundOutlined />}
                      onClick={(e) => {
                        e.preventDefault();
                        e.stopPropagation();
                        handlePreview('backContent');
                      }}
                      disabled={isSpeaking}
                      style={{ padding: 0 }}
                    >
                      {isSpeaking ? '停止' : '试听'}
                    </Button>
                  </Space>
                ),
                children: (
                  <Form.Item name="backContent" style={{ marginBottom: 0 }}>
                    <Input.TextArea
                      autoSize={{ minRows: 6, maxRows: 16 }}
                      readOnly
                      placeholder="背面摘要由结构化释义自动生成（仅中英释义）"
                    />
                  </Form.Item>
                ),
              },
            ]}
            style={{ marginBottom: 12 }}
          />
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={submitting}>
              {isEdit ? '保存卡片（正面）' : '创建'}
            </Button>
          </Form.Item>
        </Form>
      </Card>

      {isEdit && id ? (
        <Collapse
          defaultActiveKey={['structured']}
          style={{ marginTop: 16 }}
          className="sense-editor-collapse"
          items={[
            {
              key: 'structured',
              label: '释义内容（可折叠）',
              children: (
                <Card bordered={false} bodyStyle={{ padding: 0 }} className="sense-editor-panel">
                  <p style={{ color: '#6a7895', fontSize: 12, marginBottom: 12 }}>
                    「释义 1、释义 2…」表示同一单词/短语的不同义项；每个义项只放一种中文概括，以及本义项专用的英文释义、例句、同义词。文档测验里的考词条 / 选英文句 / 同义词会按这里的拆分来出题。
                  </p>
                  <Space wrap style={{ marginBottom: 12 }}>
                    <Button type="primary" onClick={handleSaveStructured} loading={structuring}>
                      保存释义内容
                    </Button>
                    <Button icon={<PlusOutlined />} onClick={addSense}>
                      添加释义
                    </Button>
                    <span style={{ color: '#6a7895', fontSize: 12 }}>
                      提示：创建卡片时开启 AI 注释会自动填充；这里用于手动调整与保存
                    </span>
                  </Space>
                  <Collapse
                    className="sense-list-collapse"
                    items={(structured.senses ?? []).map((s, si) => ({
                      key: String(si),
                      label: (
                        <div className="sense-header-row">
                          <span className="sense-header-title">{`释义 ${si + 1}${s.translationZh?.trim() ? `：${s.translationZh.trim()}` : ''}`}</span>
                          <Button
                            size="small"
                            danger
                            type="text"
                            icon={<DeleteOutlined />}
                            onClick={(e) => {
                              e.preventDefault();
                              e.stopPropagation();
                              removeSense(si);
                            }}
                          >
                            删除
                          </Button>
                        </div>
                      ),
                      children: (
                        <div className="sense-content-wrap">
                          <Space direction="vertical" style={{ width: '100%' }} size="small">
                            <Input.TextArea
                              rows={3}
                              value={s.explanationEn}
                              onChange={(e) => updateSense(si, { explanationEn: e.target.value })}
                              placeholder="英文释义（可包含 ✅ Meaning / ✅ Tone）"
                              className="sense-def-input"
                            />
                          </Space>
                          <Divider orientation="left" plain style={{ margin: '12px 0 8px' }} className="sense-divider">
                            例句 Examples
                          </Divider>
                          <Button size="small" onClick={() => addExample(si)} style={{ marginBottom: 8 }} className="sense-mini-btn">
                            添加例句
                          </Button>
                          {(s.examples ?? []).map((ex, ei) => (
                            <Space key={ei} direction="vertical" style={{ width: '100%', marginBottom: 10 }} className="sense-example-item">
                              <Space wrap>
                                <Input
                                  style={{ minWidth: 320 }}
                                  value={ex.en}
                                  onChange={(e) => updateExample(si, ei, { en: e.target.value })}
                                  placeholder="英文例句 *"
                                />
                                <Button
                                  size="small"
                                  danger
                                  icon={<DeleteOutlined />}
                                  onClick={() => removeExample(si, ei)}
                                />
                              </Space>
                              <Input
                                value={ex.zh ?? ''}
                                onChange={(e) => updateExample(si, ei, { zh: e.target.value })}
                                placeholder="中文（选填）"
                              />
                            </Space>
                          ))}
                          <Divider orientation="left" plain style={{ margin: '12px 0 8px' }} className="sense-divider">
                            同义词 Synonyms（无例句）
                          </Divider>
                          <Button size="small" onClick={() => addSynonym(si)} style={{ marginBottom: 8 }} className="sense-mini-btn">
                            添加同义词
                          </Button>
                          <div className="sense-synonym-grid">
                            {(s.synonyms ?? []).map((sy, yi) => (
                              <div key={yi} className="sense-synonym-item">
                                <Input
                                  value={sy.lemma}
                                  onChange={(e) => updateSynonym(si, yi, { lemma: e.target.value })}
                                  placeholder="英文同义表达 *"
                                />
                                <Button
                                  size="small"
                                  danger
                                  icon={<DeleteOutlined />}
                                  onClick={() => removeSynonym(si, yi)}
                                />
                              </div>
                            ))}
                          </div>
                        </div>
                      ),
                    }))}
                  />
                </Card>
              ),
            },
          ]}
        />
      ) : null}
    </div>
  );
}
