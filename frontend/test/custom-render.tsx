import type { FC, ReactElement, ReactNode } from 'react';
import { render, RenderOptions } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

type UI = ReactElement | string;

interface Options extends Omit<RenderOptions, 'wrapper'> {
  props?: Record<string, unknown>;
}

export const Wrapper: FC<{ children: ReactNode }> = ({ children }) => {
  const client = new QueryClient();

  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
};

export function CustomRender(ui: UI, options: Options) {
  return render(ui as ReactElement, options);
}

export * from '@testing-library/react';
export * from '@testing-library/user-event';
export { CustomRender as render };
