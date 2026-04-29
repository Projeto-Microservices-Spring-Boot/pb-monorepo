import axios from 'axios';
import { env } from '@/infra/env/client';

import type { AxiosError, AxiosRequestConfig } from 'axios';

const BASE_URL = env.NEXT_PUBLIC_API_URL;

const api = axios.create({
  baseURL: BASE_URL,
});

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
