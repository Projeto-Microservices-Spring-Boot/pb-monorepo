
import { useRegisterFormModel } from './register-form.model';
import { RegisterFormView } from './register-form.view';

export const RegisterForm = () => {
  const model = useRegisterFormModel();
  return <RegisterFormView {...model} />;
};
