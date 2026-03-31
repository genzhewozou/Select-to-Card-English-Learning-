import { useEffect, useState } from 'react';
import { Outlet, useNavigate, Link, useLocation } from 'react-router-dom';
import { Layout as AntLayout, Menu, Button, Progress, Tag } from 'antd';
import {
  UnorderedListOutlined,
  FormOutlined,
  SettingOutlined,
  LogoutOutlined,
  ReadOutlined,
  ThunderboltOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons';

const { Sider, Content } = AntLayout;

/**
 * 主布局：学习状态导向的侧边导航 + 内容区。
 */
export default function Layout() {
  const navigate = useNavigate();
  const location = useLocation();
  const path = location.pathname;
  const [current, setCurrent] = useState(
    path.startsWith('/documents') ? 'documents' :
    path.startsWith('/cards') ? 'cards' :
    path.startsWith('/review/weak') ? 'weak' :
    path.startsWith('/review') ? 'review' :
    path.startsWith('/quiz') ? 'quiz' :
    path.startsWith('/settings') ? 'settings' : 'documents'
  );
  useEffect(() => {
    setCurrent(
      path.startsWith('/documents') ? 'documents' :
      path.startsWith('/cards') ? 'cards' :
      path.startsWith('/review/weak') ? 'weak' :
      path.startsWith('/review') ? 'review' :
      path.startsWith('/quiz') ? 'quiz' :
      path.startsWith('/settings') ? 'settings' : 'documents'
    );
  }, [path]);

  const handleLogout = () => {
    localStorage.removeItem('userId');
    localStorage.removeItem('username');
    navigate('/login');
  };

  const username = localStorage.getItem('username') || '学习者';
  const nowHour = new Date().getHours();
  const focusTitle = nowHour < 12 ? '晨间输入' : nowHour < 18 ? '午后巩固' : '晚间复盘';
  const focusTip =
    nowHour < 12
      ? '先读文档划词，再建卡，效率最高。'
      : nowHour < 18
      ? '优先完成复习，再做一轮短测验。'
      : '回看错题和薄弱词，保证睡前完成复盘。';

  const menuItems = [
    { key: 'documents', icon: <ReadOutlined />, label: <Link to="/documents">阅读与文档</Link> },
    { key: 'cards', icon: <UnorderedListOutlined />, label: <Link to="/cards">卡片工作台</Link> },
    { key: 'review', icon: <ThunderboltOutlined />, label: <Link to="/review">今日复习</Link> },
    { key: 'weak', icon: <CheckCircleOutlined />, label: <Link to="/review/weak">薄弱词再练</Link> },
    { key: 'quiz', icon: <FormOutlined />, label: <Link to="/quiz">文档测验</Link> },
    { key: 'settings', icon: <SettingOutlined />, label: <Link to="/settings">学习偏好设置</Link> },
  ];

  return (
    <AntLayout className="study-layout-shell">
      <Sider width={276} className="study-sider">
        <div className="study-brand">
          <div className="study-brand-title">Select-to-Card</div>
          <div className="study-brand-subtitle">English Learning Flow</div>
        </div>
        <div className="study-focus-block">
          <Tag color="blue" style={{ marginBottom: 10 }}>
            {focusTitle}
          </Tag>
          <p>{focusTip}</p>
          <Progress percent={72} size="small" strokeColor="#476cff" trailColor="#e8edff" showInfo={false} />
        </div>
        <Menu
          mode="inline"
          selectedKeys={[current]}
          onSelect={({ key }) => setCurrent(key)}
          items={menuItems}
          className="study-menu"
        />
        <div className="study-sider-footer">
          <div className="study-user">{username}</div>
          <Button icon={<LogoutOutlined />} onClick={handleLogout} className="study-logout-btn">
            退出登录
          </Button>
        </div>
      </Sider>
      <Content className="study-content-wrap">
        <div className="study-page-head">
          <h2>保持输入、输出、复盘的学习闭环</h2>
          <p>建议顺序：阅读划词 → 卡片整理 → 今日复习 → 测验巩固</p>
        </div>
        <div className="study-content-panel">
          <Outlet />
        </div>
      </Content>
    </AntLayout>
  );
}
