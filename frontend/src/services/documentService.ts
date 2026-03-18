import request from '../utils/request';
import type { PageResult, Result } from '../types/api';
import type { DocumentDTO } from '../types/api';

/**
 * 文档相关 API：上传、列表、详情、删除。
 */
export function uploadDocument(file: File) {
  const formData = new FormData();
  formData.append('file', file);
  return request
    .post<Result<DocumentDTO>>('/document/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    .then((r) => r.data.data);
}

export function getDocumentList() {
  return request.get<Result<DocumentDTO[]>>('/document/list').then((r) => r.data.data);
}

export function getDocumentPage(page = 1, size = 10) {
  return request
    .get<Result<PageResult<DocumentDTO>>>('/document/page', { params: { page, size } })
    .then((r) => r.data.data);
}

export function getDocument(id: number) {
  return request.get<Result<DocumentDTO>>(`/document/${id}`).then((r) => r.data.data);
}

export function deleteDocument(id: number) {
  return request.delete<Result<null>>(`/document/${id}`).then((r) => r.data.data);
}
