import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Table, Button, Space, message, Modal, Tag, Input, Select, Checkbox } from 'antd';
import { getCardList, deleteCard } from '../services/cardService';
import { getDocumentList } from '../services/documentService';
import type { CardDTO } from '../types/api';
import type { DocumentDTO } from '../types/api';

/**
 * 卡片管理页：列表展示、按文档/关键词/熟练度/今日待复习筛选、新建、编辑、删除、定位。
 */
export default function CardList() {
  const navigate = useNavigate();
  const [list, setList] = useState<CardDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [documents, setDocuments] = useState<DocumentDTO[]>([]);
  const [filterDocumentId, setFilterDocumentId] = useState<number | undefined>(undefined);
  const [filterKeyword, setFilterKeyword] = useState('');
  const [filterProficiencyMax, setFilterProficiencyMax] = useState<number | undefined>(undefined);
  const [filterDueToday, setFilterDueToday] = useState(false);

  const load = async (override?: { keyword?: string }) => {
    setLoading(true);
    try {
      const kw = override?.keyword !== undefined ? override.keyword : filterKeyword;
      const data = await getCardList({
        documentId: filterDocumentId,
        keyword: kw?.trim() || undefined,
        proficiencyMax: filterProficiencyMax,
        dueToday: filterDueToday || undefined,
      });
      setList(data ?? []);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '加载失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    getDocumentList().then(setDocuments).catch(() => setDocuments([]));
  }, []);

  useEffect(() => {
    load();
  }, [filterDocumentId, filterProficiencyMax, filterDueToday]);

  const handleDelete = (record: CardDTO) => {
    if (!record.id) return;
    Modal.confirm({
      title: '确认删除',
      content: '确定删除该卡片吗？',
      onOk: async () => {
        try {
          await deleteCard(record.id!);
          message.success('已删除');
          load();
        } catch (e) {
          message.error(e instanceof Error ? e.message : '删除失败');
        }
      },
    });
  };

  const columns = [
    { title: '正面', dataIndex: 'frontContent', key: 'frontContent', ellipsis: true, width: 200 },
    { title: '背面', dataIndex: 'backContent', key: 'backContent', ellipsis: true, width: 200 },
    {
      title: '熟练度',
      key: 'proficiency',
      width: 100,
      render: (_: unknown, record: CardDTO) =>
        record.progress?.proficiencyLevel != null ? (
          <Tag color="blue">{record.progress.proficiencyLevel} 级</Tag>
        ) : (
          <Tag>未复习</Tag>
        ),
    },
    {
      title: '下次复习',
      key: 'nextReview',
      width: 160,
      render: (_: unknown, record: CardDTO) =>
        record.progress?.nextReviewAt
          ? new Date(record.progress.nextReviewAt).toLocaleString()
          : '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      render: (_: unknown, record: CardDTO) => (
        <Space>
          <Button type="link" onClick={() => navigate(`/cards/${record.id}/edit`)}>
            编辑
          </Button>
          {record.documentId != null && (
            <Button type="link" onClick={() => navigate(`/documents/${record.documentId}#card-${record.id}`)}>
              定位
            </Button>
          )}
          <Button type="link" danger onClick={() => handleDelete(record)}>
            删除
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
        <h2 style={{ margin: 0 }}>卡片管理</h2>
        <Button type="primary" onClick={() => navigate('/cards/new')}>
          新建卡片
        </Button>
      </div>
      <div style={{ marginBottom: 16, display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
        <Select
          placeholder="按文档筛选"
          allowClear
          style={{ width: 200 }}
          value={filterDocumentId ?? null}
          onChange={(v) => setFilterDocumentId(v != null ? v : undefined)}
          options={[{ value: null, label: '全部文档' }, ...documents.filter((d) => d.id != null).map((d) => ({ value: d.id!, label: d.fileName }))]}
        />
        <Input.Search
          placeholder="搜索正面/背面"
          allowClear
          style={{ width: 200 }}
          value={filterKeyword}
          onChange={(e) => setFilterKeyword(e.target.value)}
          onSearch={(value) => load({ keyword: value })}
        />
        <Select
          placeholder="熟练度"
          style={{ width: 140 }}
          value={filterProficiencyMax ?? undefined}
          onChange={(v) => setFilterProficiencyMax(v ?? undefined)}
          options={[
            { value: undefined, label: '全部' },
            { value: 2, label: '仅不熟(1-2级)' },
          ]}
        />
        <Checkbox checked={filterDueToday} onChange={(e) => setFilterDueToday(e.target.checked)}>
          今日待复习
        </Checkbox>
      </div>
      <Table
        rowKey="id"
        loading={loading}
        dataSource={list}
        columns={columns}
        pagination={{ pageSize: 10 }}
      />
    </div>
  );
}
