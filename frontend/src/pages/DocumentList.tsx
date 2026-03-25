import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Upload, Table, Button, Space, message, Modal } from 'antd';
import { getDocumentPage, uploadDocument, deleteDocument, downloadDocumentOriginal } from '../services/documentService';
import type { DocumentDTO } from '../types/api';

/**
 * 文档管理页：上传（Word/TXT）、列表、查看、删除。
 */
export default function DocumentList() {
  const navigate = useNavigate();
  const [list, setList] = useState<DocumentDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  const load = async (p = page, s = pageSize) => {
    setLoading(true);
    try {
      const data = await getDocumentPage(p, s);
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
    load(1, pageSize);
  }, []);

  const handleUpload = async (file: File) => {
    try {
      const created = await uploadDocument(file);
      if (created) {
        // 先做本地更新，避免等待分页请求返回时列表看起来“没刷新”
        setList((prev) => [created, ...prev].slice(0, pageSize));
        setTotal((t) => t + 1);
        setPage(1);
      }
      message.success('上传成功');
      load(1, pageSize);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '上传失败');
    }
    return false; // 阻止默认上传
  };

  const handleDelete = (record: DocumentDTO) => {
    if (!record.id) return;
    Modal.confirm({
      title: '确认删除',
      content: `确定删除文档「${record.fileName}」吗？`,
      onOk: async () => {
        try {
          await deleteDocument(record.id!);
          message.success('已删除');
          load(1, pageSize);
        } catch (e) {
          message.error(e instanceof Error ? e.message : '删除失败');
        }
      },
    });
  };

  const columns = [
    { title: '文件名', dataIndex: 'fileName', key: 'fileName', ellipsis: true },
    { title: '类型', dataIndex: 'fileType', key: 'fileType', width: 80 },
    {
      title: '操作',
      key: 'action',
      width: 240,
      render: (_: unknown, record: DocumentDTO) => (
        <Space>
          <Button type="link" onClick={() => navigate(`/documents/${record.id}`)}>
            查看
          </Button>
          {record.originalAvailable && (
            <Button
              type="link"
              onClick={async () => {
                try {
                  await downloadDocumentOriginal(record.id!, record.fileName);
                } catch (e) {
                  message.error(e instanceof Error ? e.message : '下载失败');
                }
              }}
            >
              原件
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
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2>文档管理</h2>
        <Upload beforeUpload={handleUpload} showUploadList={false} accept=".doc,.docx,.txt">
          <Button type="primary">上传文档（Word / TXT）</Button>
        </Upload>
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
          onChange: (p, s) => load(p, s),
        }}
      />
    </div>
  );
}
