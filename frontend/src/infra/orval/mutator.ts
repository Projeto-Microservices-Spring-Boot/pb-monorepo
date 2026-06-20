import axios from 'axios';
import { env } from '@/infra/env/client';

import type {
  AxiosError,
  AxiosRequestConfig,
  InternalAxiosRequestConfig,
} from 'axios';
import { useAuthStore } from '../stores/useAuth.store';

const BASE_URL = env.NEXT_PUBLIC_API_URL;

const api = axios.create({
  baseURL: BASE_URL,
});

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const accessToken = useAuthStore.getState().accessToken;
  if (accessToken) {
    config.headers.set('Authorization', `Bearer ${accessToken}`);
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().logout();
    }
    return Promise.reject(error);
  },
);

export const OrvalMutator = async <TResponse, TBody = unknown>({
  url,
  method,
  data: body,
  params,
  headers,
}: AxiosRequestConfig<TBody>): Promise<TResponse> => {
  try {
    const { data } = await api.request<TResponse>({
      url,
      method,
      data: body,
      params,
      headers,
    });
    return data;
  } catch (err) {
    const error = err as AxiosError;
    console.error('HTTP ERROR', {
      message: error.message,
      code: error.code,
      status: error.response?.status,
      url: error.config?.url,
      method: error.config?.method,
    });
    const status = error.response?.status || 500;
    const message = error.response?.data || error.message;
    throw new Error(`Request failed with status ${status}: ${message}`);
  }
};
