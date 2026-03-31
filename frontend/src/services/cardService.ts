import request from '../utils/request';
import type { PageResult, Result } from '../types/api';
import type { CardDTO, CardStructuredSaveRequest } from '../types/api';

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

export interface CardPageParams extends CardListParams {
  page?: number;
  size?: number;
}

export function getCardPage(params?: CardPageParams) {
  const p: Record<string, string | number | boolean | undefined | null> = {};
  if (params?.documentId != null) p.documentId = params.documentId;
  if (params?.keyword != null && params.keyword.trim()) p.keyword = params.keyword.trim();
  if (params?.proficiencyMax != null) p.proficiencyMax = params.proficiencyMax;
  if (params?.dueToday === true) p.dueToday = true;
  if (params?.page != null) p.page = params.page;
  if (params?.size != null) p.size = params.size;
  return request.get<Result<PageResult<CardDTO>>>('/card/page', { params: p }).then((r) => r.data.data);
}

export interface CardRangeDTO {
  id: number;
  startOffset?: number;
  endOffset?: number;
  frontContent?: string;
}

export function getCardRanges(documentId: number) {
  return request
    .get<Result<CardRangeDTO[]>>('/card/ranges', { params: { documentId } })
    .then((r) => r.data.data);
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

export interface StructuredGenerateBody {
  contextSentence?: string;
  aiApiKey: string;
  aiModel?: string;
  aiBaseUrl?: string;
}

/** 内置 JSON schema，后端调 AI 并落库义项树 */
export function generateStructuredCard(id: number, body: StructuredGenerateBody) {
  return request
    .post<Result<CardDTO>>(`/card/${id}/structured/generate`, body)
    .then((r) => r.data.data);
}

/** 全量覆盖保存义项树 */
export function saveStructuredCard(id: number, body: CardStructuredSaveRequest) {
  return request.put<Result<CardDTO>>(`/card/${id}/structured`, body).then((r) => r.data.data);
}
