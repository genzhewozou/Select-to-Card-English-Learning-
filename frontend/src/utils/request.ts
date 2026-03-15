import axios, { AxiosError } from 'axios';
import { Result } from '../types/api';

/**
 * 封装 axios 实例：baseURL、超时、统一请求头 X-User-Id、响应 data 解包。
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
});

request.interceptors.request.use((config) => {
  const userId = localStorage.getItem('userId');
  if (userId) {
    config.headers['X-User-Id'] = userId;
  }
  return config;
});

request.interceptors.response.use(
  (response) => {
    const res = response.data as Result<unknown>;
    if (res.code !== 0) {
      return Promise.reject(new Error(res.message || '请求失败'));
    }
    return response;
  },
  (err: AxiosError<Result<unknown>>) => {
    const msg = err.response?.data?.message ?? err.message ?? '网络错误';
    return Promise.reject(new Error(msg));
  }
);

export default request;
