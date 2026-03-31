import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Table, Button, Space, message, Modal, Tag, Input, Select, Checkbox, Dropdown, List } from 'antd';
import type { MenuProps } from 'antd';
import { getCardPage, deleteCard, getCardSources } from '../services/cardService';
import { postponeReview, postponeReviewByDocument } from '../services/reviewService';
import { getDocumentList } from '../services/documentService';
import type { CardDTO, CardSourceDTO } from '../types/api';
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
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [postponeLoadingKey, setPostponeLoadingKey] = useState<string>('');
  const [documentPostponeLoading, setDocumentPostponeLoading] = useState(false);
  const [sourceModalOpen, setSourceModalOpen] = useState(false);
  const [sourceLoading, setSourceLoading] = useState(false);
  const [currentSourceCard, setCurrentSourceCard] = useState<CardDTO | null>(null);
  const [sources, setSources] = useState<CardSourceDTO[]>([]);

  const load = async (opts?: { keyword?: string; page?: number; size?: number }) => {
    setLoading(true);
    try {
      const kw = opts?.keyword !== undefined ? opts.keyword : filterKeyword;
      const p = opts?.page ?? page;
      const s = opts?.size ?? pageSize;
      const data = await getCardPage({
        documentId: filterDocumentId,
        keyword: kw?.trim() || undefined,
        proficiencyMax: filterProficiencyMax,
        dueToday: filterDueToday || undefined,
        page: p,
        size: s,
      });
      setList(data?.list ?? []);
      setTotal(data?.total ?? 0);
      setPage(data?.page ?? p);
      setPageSize(data?.size ?? s);
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
          load({ page: 1 });
        } catch (e) {
          message.error(e instanceof Error ? e.message : '删除失败');
        }
      },
    });
  };

  const handlePostpone = async (record: CardDTO, days: 1 | 2 | 7) => {
    if (!record.id) return;
    const key = `${record.id}-${days}`;
    setPostponeLoadingKey(key);
    try {
      await postponeReview({ cardId: record.id, days });
      message.success(`已将该卡片延后 ${days} 天`);
      load();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '延后失败');
    } finally {
      setPostponeLoadingKey('');
    }
  };

  const handleDocumentPostpone = async (days: 1 | 2 | 7) => {
    if (filterDocumentId == null) {
      message.info('请先选择一个文档');
      return;
    }
    setDocumentPostponeLoading(true);
    try {
      const affected = await postponeReviewByDocument({ documentId: filterDocumentId, days });
      message.success(`已将当前文档 ${affected} 张卡片延后 ${days} 天`);
      load();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '文档级延后失败');
    } finally {
      setDocumentPostponeLoading(false);
    }
  };

  const handleLocate = async (record: CardDTO) => {
    if (!record.id) return;
    setCurrentSourceCard(record);
    setSourceModalOpen(true);
    setSourceLoading(true);
    try {
      const rows = await getCardSources(record.id);
      setSources(rows ?? []);
      if (!rows || rows.length === 0) {
        message.info('该卡片暂无文档来源');
      } else if (rows.length === 1) {
        const one = rows[0];
        if (one.documentId != null) {
          setSourceModalOpen(false);
          navigate(`/documents/${one.documentId}#card-${record.id}`);
        }
      }
    } catch (e) {
      message.error(e instanceof Error ? e.message : '加载来源失败');
    } finally {
      setSourceLoading(false);
    }
  };

  const summaryOf = (record: CardDTO) => {
    const senses = record.senses ?? [];
    if (senses.length > 0) {
      const first = senses[0];
      const zh = first?.translationZh?.trim();
      const en = first?.explanationEn?.trim();
      const text = [zh, en].filter(Boolean).join(' | ');
      return text || '（无摘要）';
    }
    return record.backContent?.trim() || '（无摘要）';
  };

  const columns = [
    { title: '正面', dataIndex: 'frontContent', key: 'frontContent', ellipsis: true, width: 200 },
    {
      title: '释义摘要',
      key: 'summary',
      ellipsis: true,
      width: 260,
      render: (_: unknown, record: CardDTO) => summaryOf(record),
    },
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
      width: 280,
      render: (_: unknown, record: CardDTO) => (
        <Space>
          <Button type="link" onClick={() => navigate(`/cards/${record.id}/edit`)}>
            编辑
          </Button>
          <Button type="link" onClick={() => handleLocate(record)}>
            定位
          </Button>
          <Button type="link" danger onClick={() => handleDelete(record)}>
            删除
          </Button>
          <Dropdown.Button
            type="link"
            loading={postponeLoadingKey.startsWith(`${record.id}-`)}
            menu={{
              items: [
                { key: '2', label: '延后2天' },
                { key: '7', label: '延后7天' },
              ] as MenuProps['items'],
              onClick: ({ key }) => handlePostpone(record, Number(key) as 2 | 7),
            }}
            onClick={() => handlePostpone(record, 1)}
          >
            延后1天
          </Dropdown.Button>
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
          placeholder="搜索正面/释义/例句/同义词"
          allowClear
          style={{ width: 200 }}
          value={filterKeyword}
          onChange={(e) => setFilterKeyword(e.target.value)}
          onSearch={(value) => load({ keyword: value, page: 1 })}
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
        <Button onClick={() => navigate(filterDocumentId != null ? `/review?documentId=${filterDocumentId}` : '/review')}>
          {filterDocumentId != null ? '复习当前文档' : '开始全部复习'}
        </Button>
        <Button
          disabled={filterDocumentId == null}
          onClick={() => navigate(filterDocumentId != null ? `/review?documentId=${filterDocumentId}&relearn=1` : '/review?relearn=1')}
        >
          重新复习当前文档
        </Button>
        <Dropdown.Button
          disabled={filterDocumentId == null}
          loading={documentPostponeLoading}
          menu={{
            items: [
              { key: '2', label: '当前文档延后2天' },
              { key: '7', label: '当前文档延后7天' },
            ] as MenuProps['items'],
            onClick: ({ key }) => handleDocumentPostpone(Number(key) as 2 | 7),
          }}
          onClick={() => handleDocumentPostpone(1)}
        >
          当前文档延后1天
        </Dropdown.Button>
      </div>
      <Table
        rowKey="id"
        loading={loading}
        dataSource={list}
        columns={columns}
        pagination={{
          current: page,
          pageSize,
          total,
          showSizeChanger: true,
          onChange: (p, s) => load({ page: p, size: s }),
        }}
      />
      <Modal
        title={`选择来源定位${currentSourceCard?.frontContent ? `：${currentSourceCard.frontContent}` : ''}`}
        open={sourceModalOpen}
        onCancel={() => setSourceModalOpen(false)}
        footer={null}
      >
        <List
          loading={sourceLoading}
          dataSource={sources}
          locale={{ emptyText: '该卡片暂无文档来源' }}
          renderItem={(s) => (
            <List.Item
              actions={[
                <Button
                  key="go"
                  type="link"
                  disabled={s.documentId == null}
                  onClick={() => {
                    if (s.documentId == null) return;
                    setSourceModalOpen(false);
                    navigate(`/documents/${s.documentId}#card-${currentSourceCard?.id}`);
                  }}
                >
                  跳转
                </Button>,
              ]}
            >
              <List.Item.Meta
                title={s.documentName || `文档 #${s.documentId}`}
                description={`偏移: ${s.startOffset ?? '-'} ~ ${s.endOffset ?? '-'}`}
              />
            </List.Item>
          )}
        />
      </Modal>
    </div>
  );
}
