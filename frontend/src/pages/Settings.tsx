import { useState, useEffect } from 'react';
import { Card, Form, Input, Button, message, Space, Divider } from 'antd';
import { chatWithAi } from '../services/aiService';
import { DEFAULT_AI_NOTE_PROMPT, getAiConfig, setAiConfig } from '../utils/aiConfigStorage';

/**
 * 设置页：AI 配置（API Key、模型、接口地址），保存到本地，供文档生成卡片等使用。
 */
export default function Settings() {
  const [form] = Form.useForm();
  const [saving, setSaving] = useState(false);
  const [chatInput, setChatInput] = useState('');
  const [chatOutput, setChatOutput] = useState('');
  const [chatLoading, setChatLoading] = useState(false);

  useEffect(() => {
    const config = getAiConfig();
    form.setFieldsValue({
      aiApiKey: config.apiKey ?? '',
      aiModel: config.model ?? 'gpt-3.5-turbo',
      aiBaseUrl: config.baseUrl ?? 'https://api.openai.com/v1',
      aiNotePrompt: config.notePrompt ?? DEFAULT_AI_NOTE_PROMPT,
    });
  }, [form]);

  const onFinish = (values: { aiApiKey?: string; aiModel?: string; aiBaseUrl?: string; aiNotePrompt?: string }) => {
    setSaving(true);
    try {
      setAiConfig({
        apiKey: values.aiApiKey?.trim() || undefined,
        model: values.aiModel?.trim() || undefined,
        baseUrl: values.aiBaseUrl?.trim() || undefined,
        notePrompt: values.aiNotePrompt?.trim() || DEFAULT_AI_NOTE_PROMPT,
      });
      message.success('AI 配置已保存（仅存于本机，不会上传）');
    } catch (e) {
      message.error('保存失败');
    } finally {
      setSaving(false);
    }
  };

  const resetPromptToDefault = () => {
    form.setFieldValue('aiNotePrompt', DEFAULT_AI_NOTE_PROMPT);
    message.success('已恢复默认提示词');
  };

  const handleChatTest = async () => {
    const values = form.getFieldsValue();
    const apiKey = values.aiApiKey?.trim();
    if (!apiKey) {
      message.warning('请先填写 API Key');
      return;
    }
    if (!chatInput.trim()) {
      message.warning('请输入聊天内容');
      return;
    }
    setChatLoading(true);
    try {
      const res = await chatWithAi({
        message: chatInput.trim(),
        aiApiKey: apiKey,
        aiModel: values.aiModel?.trim() || undefined,
        aiBaseUrl: values.aiBaseUrl?.trim() || undefined,
      });
      setChatOutput(res || '');
    } catch (e) {
      message.error(e instanceof Error ? e.message : '聊天失败');
    } finally {
      setChatLoading(false);
    }
  };

  return (
    <div>
      <Card title="AI 配置" style={{ maxWidth: 760 }}>
        <p style={{ color: '#666', marginBottom: 16 }}>
          填写后，在「文档管理」中选中文本生成卡片时可使用 AI 自动生成背面内容。配置仅保存在本机浏览器，不会上传到服务器。
        </p>
        <Form form={form} layout="vertical" onFinish={onFinish}>
          <Form.Item
            name="aiApiKey"
            label="API Key"
          >
            <Input.Password placeholder="OpenAI 或兼容接口的 API Key" allowClear />
          </Form.Item>
          <Form.Item name="aiModel" label="模型名称">
            <Input placeholder="如 gpt-3.5-turbo、gpt-4" allowClear />
          </Form.Item>
          <Form.Item name="aiBaseUrl" label="接口地址">
            <Input placeholder="如 https://api.openai.com/v1 或自建代理地址" allowClear />
          </Form.Item>
          <Form.Item
            name="aiNotePrompt"
            label="生成卡片背面提示词（可配置）"
            extra="支持占位符：{{target}}（选中文本）、{{context}}（上下文，可为空）"
          >
            <Input.TextArea rows={8} placeholder="用于生成卡片背面内容的提示词模板" />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" loading={saving}>
                保存配置
              </Button>
              <Button onClick={resetPromptToDefault}>恢复默认提示词</Button>
            </Space>
          </Form.Item>
        </Form>

        <Divider />

        <h3 style={{ marginTop: 0 }}>AI 聊天测试</h3>
        <p style={{ color: '#666', marginBottom: 12 }}>
          使用上方 API Key / 模型 / 接口地址直接聊天，便于验证配置是否可用。
        </p>
        <Input.TextArea
          rows={4}
          value={chatInput}
          onChange={(e) => setChatInput(e.target.value)}
          placeholder="输入你想让 AI 回答的问题"
          style={{ marginBottom: 12 }}
        />
        <Button type="default" onClick={handleChatTest} loading={chatLoading}>
          发送测试消息
        </Button>
        <Input.TextArea
          rows={8}
          value={chatOutput}
          readOnly
          placeholder="AI 回复将显示在这里"
          style={{ marginTop: 12 }}
        />
      </Card>
    </div>
  );
}
