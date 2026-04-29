import { createEnv } from '@t3-oss/env-nextjs';
import * as z from 'zod';

export const env = createEnv({
  server: {
    NODE_ENV: z
      .enum(['test', 'e2e', 'development', 'production', 'CI'])
      .default('development'),
  },

  experimental__runtimeEnv: process.env,
});
