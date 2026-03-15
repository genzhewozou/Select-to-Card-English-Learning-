import request from '../utils/request';
import type { Result } from '../types/api';
import type { UserDTO, LoginRequest } from '../types/api';

/**
 * 用户相关 API：注册、登录、查询、更新、删除。
 */
export function register(data: UserDTO) {
  return request.post<Result<UserDTO>>('/user/register', data).then((r) => r.data.data);
}

export function login(data: LoginRequest) {
  return request.post<Result<UserDTO>>('/user/login', data).then((r) => r.data.data);
}

export function getUser(id: number) {
  return request.get<Result<UserDTO>>(`/user/${id}`).then((r) => r.data.data);
}

export function updateUser(id: number, data: Partial<UserDTO>) {
  return request.put<Result<UserDTO>>(`/user/${id}`, data).then((r) => r.data.data);
}
