import { useState, useEffect } from 'react';
import { Card, Form, Input, Button, message } from 'antd';
import { getAiConfig, setAiConfig } from '../utils/aiConfigStorage';

/**
 * 设置页：AI 配置（API Key、模型、接口地址），保存到本地，供文档生成卡片等使用。
 */
export default function Settings() {
  const [form] = Form.useForm();
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    const config = getAiConfig();
    form.setFieldsValue({
      aiApiKey: config.apiKey ?? '',
      aiModel: config.model ?? 'gpt-3.5-turbo',
      aiBaseUrl: config.baseUrl ?? 'https://api.openai.com/v1',
    });
  }, [form]);

  const onFinish = (values: { aiApiKey?: string; aiModel?: string; aiBaseUrl?: string }) => {
    setSaving(true);
    try {
      setAiConfig({
        apiKey: values.aiApiKey?.trim() || undefined,
        model: values.aiModel?.trim() || undefined,
        baseUrl: values.aiBaseUrl?.trim() || undefined,
      });
      message.success('AI 配置已保存（仅存于本机，不会上传）');
    } catch (e) {
      message.error('保存失败');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      <Card title="AI 配置" style={{ maxWidth: 560 }}>
        <p style={{ color: '#666', marginBottom: 16 }}>
          填写后，在「文档管理」中选中文本生成卡片时可选择「使用 AI 生成注释」。配置仅保存在本机浏览器，不会上传到服务器。
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
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={saving}>
              保存配置
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
}
