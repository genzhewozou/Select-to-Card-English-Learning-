import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import Login from './pages/Login';
import Register from './pages/Register';
import DocumentList from './pages/DocumentList';
import DocumentView from './pages/DocumentView';
import CardList from './pages/CardList';
import CardEdit from './pages/CardEdit';
import Review from './pages/Review';
import WeakCards from './pages/WeakCards';
import Settings from './pages/Settings';

/**
 * 根组件：路由配置。未登录时重定向到登录页（简单用 localStorage 存 userId）。
 */
function App() {
  const userId = localStorage.getItem('userId');

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/" element={userId ? <Layout /> : <Navigate to="/login" replace />}>
          <Route index element={<Navigate to="/documents" replace />} />
          <Route path="documents" element={<DocumentList />} />
          <Route path="documents/:id" element={<DocumentView />} />
          <Route path="cards" element={<CardList />} />
          <Route path="cards/new" element={<CardEdit />} />
          <Route path="cards/:id/edit" element={<CardEdit />} />
          <Route path="review" element={<Review />} />
          <Route path="review/weak" element={<WeakCards />} />
          <Route path="settings" element={<Settings />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
