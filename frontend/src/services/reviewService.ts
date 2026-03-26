import request from '../utils/request';
import type { PageResult, Result } from '../types/api';
import type {
  CardDTO,
  CardProgressDTO,
  ReviewDocumentPostponeRequest,
  ReviewPostponeRequest,
  ReviewRequest,
} from '../types/api';

/**
 * 复习相关 API：今日待复习列表、提交复习结果。
 */
export function getTodayReviewCards(documentId?: number) {
  return request.get<Result<CardDTO[]>>('/review/today', { params: { documentId } }).then((r) => r.data.data);
}

/** 错题本：熟练度 1-2 的卡片列表（可用于列表展示或「只复习错题」） */
export function getWeakCards(documentId?: number) {
  return request.get<Result<CardDTO[]>>('/review/weak', { params: { documentId } }).then((r) => r.data.data);
}

export function getTodayReviewPage(page = 1, size = 50, documentId?: number) {
  return request
    .get<Result<PageResult<CardDTO>>>('/review/today/page', { params: { page, size, documentId } })
    .then((r) => r.data.data);
}

export function getWeakCardsPage(page = 1, size = 10, documentId?: number) {
  return request
    .get<Result<PageResult<CardDTO>>>('/review/weak/page', { params: { page, size, documentId } })
    .then((r) => r.data.data);
}

export function submitReview(data: ReviewRequest) {
  return request.post<Result<CardProgressDTO>>('/review/submit', data).then((r) => r.data.data);
}

export function postponeReview(data: ReviewPostponeRequest) {
  return request.post<Result<CardProgressDTO>>('/review/postpone', data).then((r) => r.data.data);
}

export function postponeReviewByDocument(data: ReviewDocumentPostponeRequest) {
  return request.post<Result<number>>('/review/postpone/document', data).then((r) => r.data.data);
}
