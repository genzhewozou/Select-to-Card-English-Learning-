import request from '../utils/request';
import type { Result } from '../types/api';
import type { CardDTO, CardProgressDTO, ReviewRequest } from '../types/api';

/**
 * 复习相关 API：今日待复习列表、提交复习结果。
 */
export function getTodayReviewCards() {
  return request.get<Result<CardDTO[]>>('/review/today').then((r) => r.data.data);
}

/** 错题本：熟练度 1-2 的卡片列表（可用于列表展示或「只复习错题」） */
export function getWeakCards() {
  return request.get<Result<CardDTO[]>>('/review/weak').then((r) => r.data.data);
}

export function submitReview(data: ReviewRequest) {
  return request.post<Result<CardProgressDTO>>('/review/submit', data).then((r) => r.data.data);
}
