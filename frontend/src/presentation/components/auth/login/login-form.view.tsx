import Link from 'next/link';

import { Button } from '@/presentation/components/ui/button/button';
import { Input } from '@/presentation/components/ui/input/input';
import type { useLoginFormModel } from './login-form.model';

const texts = {
  form: {
    title: 'Entrar',
    subtitle: 'Acesse sua conta para continuar',
  },
  inputs: {
    email: {
      label: 'Email',
      placeholder: 'seu@email.com',
    },
    password: {
      label: 'Senha',
      placeholder: '••••••••',
    },
  },
  button: {
    title: 'Entrar',
    isPending: 'Entrando...',
  },
  paragraphs: {
    noAccount: 'Ainda não tem conta?',
    signUp: 'Cadastre-se',
  },
};

type LoginFormViewProps = ReturnType<typeof useLoginFormModel>;

export function LoginFormView({
  register,
  errors,
  onSubmit,
  isPending,
}: LoginFormViewProps) {
  return (
    <form className="space-y-5" onSubmit={onSubmit}>
      <div className="space-y-1">
        <h1 className="text-2xl font-bold text-gray-900">{texts.form.title}</h1>
        <p className="text-sm text-gray-500">{texts.form.subtitle}</p>
      </div>

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
        autoComplete="current-password"
      />

      <Button type="submit" className="w-full" size="lg">
        {isPending ? texts.button.isPending : texts.button.title}
      </Button>

      <p className="text-center text-sm text-gray-500">
        {texts.paragraphs.noAccount}{' '}
        <Link
          href="/auth/register"
          className="font-medium text-indigo-600 hover:text-indigo-500"
        >
          {texts.paragraphs.signUp}
        </Link>
      </p>
    </form>
  );
}
