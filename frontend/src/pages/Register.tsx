import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Form, Input, Button, Card, message } from 'antd';
import { register } from '../services/userService';
import type { UserDTO } from '../types/api';

/**
 * 注册页：用户名、密码、昵称、邮箱，成功后跳转登录。
 */
export default function Register() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);

  const onFinish = async (values: UserDTO & { confirm?: string }) => {
    if (values.password !== values.confirm) {
      message.error('两次密码不一致');
      return;
    }
    setLoading(true);
    try {
      await register({
        username: values.username,
        password: values.password,
        nickname: values.nickname,
        email: values.email,
      });
      message.success('注册成功，请登录');
      navigate('/login');
    } catch (e) {
      message.error(e instanceof Error ? e.message : '注册失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-shell">
      <Card title="创建学习账号" className="auth-card" extra={<span className="auth-card-sub">从第一篇文档开始</span>}>
        <Form name="register" onFinish={onFinish} autoComplete="off" layout="vertical">
          <Form.Item
            name="username"
            rules={[{ required: true, message: '请输入用户名' }, { min: 2, message: '至少 2 个字符' }]}
          >
            <Input placeholder="用户名" size="large" />
          </Form.Item>
          <Form.Item
            name="password"
            rules={[{ required: true, message: '请输入密码' }, { min: 6, message: '至少 6 位' }]}
          >
            <Input.Password placeholder="密码" size="large" />
          </Form.Item>
          <Form.Item
            name="confirm"
            rules={[{ required: true, message: '请再次输入密码' }]}
          >
            <Input.Password placeholder="确认密码" size="large" />
          </Form.Item>
          <Form.Item name="nickname">
            <Input placeholder="昵称（选填）" size="large" />
          </Form.Item>
          <Form.Item name="email">
            <Input placeholder="邮箱（选填）" size="large" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block size="large">
              注册
            </Button>
          </Form.Item>
          <div style={{ textAlign: 'center' }}>
            已有账号？ <Link to="/login">登录</Link>
          </div>
        </Form>
      </Card>
    </div>
  );
}
