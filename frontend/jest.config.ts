import createJestConfig from 'next/jest.js';

const config = createJestConfig({ dir: './' })({
  displayName: 'unit',
  rootDir: './',
  testMatch: ['**/*.{spec,test}.{ts,tsx}'],
  setupFilesAfterEnv: ['<rootDir>/jest.setup.ts'],
  testEnvironment: 'jest-environment-jsdom',
  testTimeout: 10000,

  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/src/$1',
    '^src/(.*)$': '<rootDir>/src/$1',
  },

  coverageDirectory: './coverage',
  coverageReporters: ['text', 'html', 'lcov', 'cobertura'],
  collectCoverageFrom: [
    'src/**/*.{ts,tsx}',
    '!src/**/*.test.{ts,tsx}',
    '!src/**/*.spec.{ts,tsx}',
    '!src/**/*.e2e-spec.ts',
    '!src/**/types/**',
    '!src/**/*.d.ts',
    '!**/*.stories.{ts,tsx}',
  ],
  testPathIgnorePatterns: [
    '<rootDir>/node_modules/',
    '<rootDir>/.next/',
    '<rootDir>/e2e/',
  ],
});

export default config;
