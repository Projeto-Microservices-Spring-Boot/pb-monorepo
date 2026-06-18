import Link from 'next/link';

import { Button } from '@/presentation/components/ui/button/button';
import { Input } from '@/presentation/components/ui/input/input';

import { useRegisterFormModel } from './register-form.model';

const texts = {
  form: {
    title: 'Criar conta',
    subtitle: 'Preencha os dados para se cadastrar',
  },
  inputs: {
    name: {
      label: 'Nome',
      placeholder: 'Seu nome completo',
    },
    email: {
      label: 'Email',
      placeholder: 'seu@email.com',
    },
    password: {
      label: 'Senha',
      placeholder: 'Crie uma senha segura',
    },
    confirmPassword: {
      label: 'Confirmar senha',
      placeholder: 'Confirme sua senha',
    },
  },
  button: {
    title: 'Cadastrar',
    isPending: 'Cadastrando...',
  },
  paragraphs: {
    alreadyHaveAccount: 'Já tem uma conta?',
    access: 'Entrar',
  },
};

type RegisterFormViewProps = ReturnType<typeof useRegisterFormModel>;

export const RegisterFormView = ({
  register,
  errors,
  onSubmit,
  isPending,
}: RegisterFormViewProps) => {
  return (
    <form onSubmit={onSubmit} className="space-y-5">
      <div className="space-y-1">
        <h1 className="text-2xl font-bold text-gray-900">{texts.form.title}</h1>
        <p className="text-sm text-gray-500">{texts.form.subtitle}</p>
      </div>

      <Input
        {...register('name')}
        label={texts.inputs.name.label}
        placeholder={texts.inputs.name.placeholder}
        error={errors.name?.message}
        type="text"
        autoComplete="name"
      />

      <Input
        {...register('email')}
        label={texts.inputs.email.label}
        placeholder={texts.inputs.email.placeholder}
        error={errors.email?.message}
        type="email"
        autoComplete="email"
      />

      <Input
        {...register('password')}
        label={texts.inputs.password.label}
        placeholder={texts.inputs.password.placeholder}
        error={errors.password?.message}
        type="password"
        autoComplete="new-password"
      />

      <Input
        {...register('confirmPassword')}
        label={texts.inputs.confirmPassword.label}
        placeholder={texts.inputs.confirmPassword.placeholder}
        error={errors.confirmPassword?.message}
        type="password"
        autoComplete="new-password"
      />

      <Button type="submit" className="w-full" size="lg" disabled={isPending}>
        {isPending ? texts.button.isPending : texts.button.title}
      </Button>

      <p className="text-center text-sm text-gray-500">
        {texts.paragraphs.alreadyHaveAccount}{' '}
        <Link
          href="/auth/login"
          className="font-medium text-indigo-600 hover:text-indigo-500"
        >
          {texts.paragraphs.access}
        </Link>
      </p>
    </form>
  );
};
