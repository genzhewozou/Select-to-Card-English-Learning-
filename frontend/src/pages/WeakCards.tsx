import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Table, Button, Space, message, Tag, Card } from 'antd';
import { getWeakCards } from '../services/reviewService';
import type { CardDTO } from '../types/api';

/**
 * 错题本：熟练度 1-2 的卡片列表，支持「只复习错题」进入复习流程。
 */
export default function WeakCards() {
  const navigate = useNavigate();
  const [list, setList] = useState<CardDTO[]>([]);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    try {
      const data = await getWeakCards();
      setList(data ?? []);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '加载失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const columns = [
    { title: '正面', dataIndex: 'frontContent', key: 'frontContent', ellipsis: true, width: 200 },
    { title: '背面', dataIndex: 'backContent', key: 'backContent', ellipsis: true, width: 200 },
    {
      title: '熟练度',
      key: 'proficiency',
      width: 100,
      render: (_: unknown, record: CardDTO) =>
        record.progress?.proficiencyLevel != null ? (
          <Tag color="orange">{record.progress.proficiencyLevel} 级</Tag>
        ) : (
          <Tag>未复习</Tag>
        ),
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      render: (_: unknown, record: CardDTO) => (
        <Button type="link" onClick={() => navigate(`/cards/${record.id}/edit`)}>
          编辑
        </Button>
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
        <h2 style={{ margin: 0 }}>错题本</h2>
        <Button type="primary" onClick={() => navigate('/review?mode=weak')}>
          开始复习错题
        </Button>
      </div>
      <Card>
        <p style={{ color: '#666', marginBottom: 16 }}>
          以下为熟练度 1～2 级的卡片，可在此集中复习或点击「开始复习错题」进入复习流程。
        </p>
        <Table
          rowKey="id"
          loading={loading}
          dataSource={list}
          columns={columns}
          pagination={{ pageSize: 10 }}
        />
      </Card>
    </div>
  );
}
