import { useEffect, useState } from 'react';
import { Outlet, useNavigate, Link, useLocation } from 'react-router-dom';
import { Layout as AntLayout, Menu, Button } from 'antd';
import { FileTextOutlined, UnorderedListOutlined, FormOutlined, SettingOutlined, LogoutOutlined } from '@ant-design/icons';

const { Header, Content } = AntLayout;

/**
 * 主布局：顶部导航（文档、卡片、复习）、退出登录、子路由出口。
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

  const menuItems = [
    { key: 'documents', icon: <FileTextOutlined />, label: <Link to="/documents">文档管理</Link> },
    { key: 'cards', icon: <UnorderedListOutlined />, label: <Link to="/cards">卡片管理</Link> },
    { key: 'review', icon: <FormOutlined />, label: <Link to="/review">学习复习</Link> },
    { key: 'weak', icon: <FormOutlined />, label: <Link to="/review/weak">错题本</Link> },
    { key: 'quiz', icon: <FormOutlined />, label: <Link to="/quiz">文档测验</Link> },
    { key: 'settings', icon: <SettingOutlined />, label: <Link to="/settings">设置</Link> },
  ];

  return (
    <AntLayout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <Menu
          theme="dark"
          mode="horizontal"
          selectedKeys={[current]}
          onSelect={({ key }) => setCurrent(key)}
          items={menuItems}
          style={{ flex: 1, minWidth: 0 }}
        />
        <span style={{ color: '#fff', marginRight: 12 }}>{localStorage.getItem('username') || '用户'}</span>
        <Button type="link" icon={<LogoutOutlined />} onClick={handleLogout} style={{ color: '#fff' }}>
          退出
        </Button>
      </Header>
      <Content style={{ padding: 24, margin: '16px auto', maxWidth: 1200, width: '100%' }}>
        <Outlet />
      </Content>
    </AntLayout>
  );
}
