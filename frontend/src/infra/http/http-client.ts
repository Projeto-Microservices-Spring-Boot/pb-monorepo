import axios from 'axios';
import { env } from '@/infra/env/client';

import type { HttpRequest, IHttpClient } from './http-client.types';
import type { AxiosError, AxiosInstance } from 'axios';

const BASE_URL = env.NEXT_PUBLIC_API_URL;

// HTTP client customizado
export class HttpClient implements IHttpClient {
  constructor(private api: AxiosInstance = axios) {}

  static create(): IHttpClient {
    return new HttpClient();
  }

  async request<TResponse, TBody = unknown>({
    endpoint,
    method,
    headers,
    body,
    params,
  }: HttpRequest<TBody>) {
    try {
      const { data } = await this.api.request<TResponse>({
        url: `${BASE_URL}${endpoint}`,
        method,
        headers,
        data: body,
        params,
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
  }
}
