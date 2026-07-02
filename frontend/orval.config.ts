import { defineConfig } from 'orval';

const USERS_SERVICE_BASE_URL = 'http://localhost:8000/docs/users/v3/api-docs';

export default defineConfig({
  users: {
    input: USERS_SERVICE_BASE_URL, 
    output: {
      namingConvention: 'kebab-case',
      mode: 'tags-split',
      target: 'src/api/generated/users/endpoints',
      schemas: {
        path: 'src/api/generated/users/models',
        type: 'typescript',
      },
      operationSchemas: 'src/api/generated/users/models/params',
      client: 'react-query',
      httpClient: 'axios',
      fileExtension: '.gen.ts',
      override: {
        mutator: {
          path: 'src/infra/orval/mutator.ts',
          name: 'OrvalMutator',
        },
      },
      formatter: 'prettier',
      clean: true,
      indexFiles: true,
    },
  },
});
