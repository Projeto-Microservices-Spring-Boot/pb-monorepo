'use client';

import { useLoginFormModel } from './login-form.model';
import { LoginFormView } from './login-form.view';

export const LoginForm = () => {
  const model = useLoginFormModel();
  return <LoginFormView {...model} />;
};
