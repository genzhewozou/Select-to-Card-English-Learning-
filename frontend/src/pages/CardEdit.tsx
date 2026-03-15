import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Form, Input, Button, Card, message } from 'antd';
import { getCard, createCard, updateCard } from '../services/cardService';
import { updateCardNote } from '../services/cardNoteService';
import type { CardDTO } from '../types/api';

/**
 * 卡片编辑页：新建或编辑卡片，表单含正面、背面、AI 注释（编辑时可修改）。
 */
export default function CardEdit() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(!!id);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm();
  const [noteId, setNoteId] = useState<number | null>(null);
  const [documentId, setDocumentId] = useState<number | null>(null);
  const isEdit = id && id !== 'new';

  useEffect(() => {
    if (!isEdit || !id) return;
    getCard(Number(id))
      .then((data) => {
        form.setFieldsValue({
          frontContent: data.frontContent,
          backContent: data.backContent,
          noteContent: data.notes?.[0]?.content ?? '',
        });
        const first = data.notes?.[0];
        setNoteId(first?.id ?? null);
        setDocumentId(data.documentId ?? null);
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
          backContent: values.backContent,
        });
        if (noteId != null && values.noteContent !== undefined) {
          await updateCardNote(noteId, { content: values.noteContent });
        }
        message.success('保存成功');
      } else {
        await createCard({
          userId: Number(userId),
          frontContent: values.frontContent,
          backContent: values.backContent,
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
      <Card title={isEdit ? '编辑卡片' : '新建卡片'}>
        <Form form={form} layout="vertical" onFinish={onFinish}>
          <Form.Item
            name="frontContent"
            label="正面内容"
            rules={[{ required: true, message: '请输入正面内容' }]}
          >
            <Input.TextArea rows={3} placeholder="单词或句子" />
          </Form.Item>
          <Form.Item name="backContent" label="背面内容">
            <Input.TextArea rows={3} placeholder="释义、例句等" />
          </Form.Item>
          {isEdit && (
            <Form.Item name="noteContent" label="AI 注释（可修改）">
              <Input.TextArea rows={5} placeholder="AI 生成的释义与例句，可在此修改" />
            </Form.Item>
          )}
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={submitting}>
              {isEdit ? '保存' : '创建'}
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
}
