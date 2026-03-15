import request from '../utils/request';
import type { Result } from '../types/api';
import type { CardDTO } from '../types/api';

/**
 * 卡片相关 API：创建、列表、详情、更新、删除。
 */
export function createCard(data: CardDTO) {
  return request.post<Result<CardDTO>>('/card', data).then((r) => r.data.data);
}

export interface CardListParams {
  documentId?: number;
  keyword?: string;
  proficiencyMax?: number;
  dueToday?: boolean;
}

export function getCardList(params?: CardListParams) {
  const p: Record<string, string | number | boolean | undefined> = {};
  if (params?.documentId != null) p.documentId = params.documentId;
  if (params?.keyword != null && params.keyword.trim()) p.keyword = params.keyword.trim();
  if (params?.proficiencyMax != null) p.proficiencyMax = params.proficiencyMax;
  if (params?.dueToday === true) p.dueToday = true;
  return request.get<Result<CardDTO[]>>('/card/list', { params: p }).then((r) => r.data.data);
}

export function getCard(id: number) {
  return request.get<Result<CardDTO>>(`/card/${id}`).then((r) => r.data.data);
}

export function updateCard(id: number, data: Partial<CardDTO>) {
  return request.put<Result<CardDTO>>(`/card/${id}`, data).then((r) => r.data.data);
}

export function deleteCard(id: number) {
  return request.delete<Result<null>>(`/card/${id}`).then((r) => r.data.data);
}
