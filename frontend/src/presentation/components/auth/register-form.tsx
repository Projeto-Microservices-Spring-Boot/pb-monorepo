import Link from 'next/link';

import { Button } from '@/presentation/components/ui/button';
import { Input } from '@/presentation/components/ui/input';

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
  },
  button: {
    title: 'Cadastrar',
  },
  paragraphs: {
    alreadyHaveAccount: 'Já tem uma conta?',
    access: 'Entrar',
  },
};

export function RegisterForm() {
  return (
    <form className="space-y-5">
      <div className="space-y-1">
        <h1 className="text-2xl font-bold text-gray-900">{texts.form.title}</h1>
        <p className="text-sm text-gray-500">{texts.form.subtitle}</p>
      </div>

      <Input
        label={texts.inputs.name.label}
        type="text"
        placeholder={texts.inputs.name.placeholder}
        autoComplete="name"
      />

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
        autoComplete="new-password"
      />

      <Button type="submit" className="w-full" size="lg">
        {texts.button.title}
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
}
