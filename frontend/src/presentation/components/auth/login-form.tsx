import Link from 'next/link';

import { Button } from '@/presentation/components/ui/button';
import { Input } from '@/presentation/components/ui/input';

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
  },
  paragraphs: {
    noAccount: 'Ainda não tem conta?',
    signUp: 'Cadastre-se',
  },
};

export function LoginForm() {
  return (
    <form className="space-y-5">
      <div className="space-y-1">
        <h1 className="text-2xl font-bold text-gray-900">{texts.form.title}</h1>
        <p className="text-sm text-gray-500">{texts.form.subtitle}</p>
      </div>

      <Input
        label={texts.inputs.email.label}
        type="email"
        placeholder={texts.inputs.email.placeholder}
        autoComplete="email"
      />

      <Input
        label={texts.inputs.password.label}
        type="password"
        placeholder={texts.inputs.password.placeholder}
        autoComplete="current-password"
      />

      <Button type="submit" className="w-full" size="lg">
        {texts.button.title}
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
