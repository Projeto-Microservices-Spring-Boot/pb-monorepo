import '@testing-library/jest-dom';
import * as matchers from '@testing-library/jest-dom/matchers';

import { cleanup } from './test/custom-render';

expect.extend(matchers);

afterEach(() => {
  cleanup();

  jest.resetAllMocks();
});
