import request from '../utils/request';
import type { Result } from '../types/api';
import type { CardNoteDTO } from '../types/api';

/**
 * 卡片注释 API：创建、按卡片查询、更新、删除。
 */
export function createCardNote(data: CardNoteDTO) {
  return request.post<Result<CardNoteDTO>>('/card/note', data).then((r) => r.data.data);
}

export function getNotesByCardId(cardId: number) {
  return request.get<Result<CardNoteDTO[]>>('/card/note/list', { params: { cardId } }).then((r) => r.data.data);
}

export function updateCardNote(id: number, data: Partial<CardNoteDTO>) {
  return request.put<Result<CardNoteDTO>>(`/card/note/${id}`, data).then((r) => r.data.data);
}

export function deleteCardNote(id: number) {
  return request.delete<Result<null>>(`/card/note/${id}`).then((r) => r.data.data);
}
