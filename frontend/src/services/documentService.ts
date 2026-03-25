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

/** 下载服务器保存的上传原件（需 originalAvailable） */
export function downloadDocumentOriginal(id: number, fallbackFileName: string) {
  return request
    .get(`/document/${id}/download`, { responseType: 'blob', timeout: 120000 })
    .then((response) => {
      const blob = response.data as Blob;
      const cd = response.headers['content-disposition'] as string | undefined;
      let name = fallbackFileName;
      if (cd) {
        const star = cd.match(/filename\*=UTF-8''([^;]+)/i);
        if (star?.[1]) {
          try {
            name = decodeURIComponent(star[1].trim());
          } catch {
            name = fallbackFileName;
          }
        } else {
          const m = cd.match(/filename="([^"]+)"/i) || cd.match(/filename=([^;]+)/i);
          if (m?.[1]) name = m[1].trim();
        }
      }
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = name;
      a.click();
      window.URL.revokeObjectURL(url);
    });
}
