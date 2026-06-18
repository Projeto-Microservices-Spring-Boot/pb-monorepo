'use client';

import { RegisterForm } from '@/presentation/components/auth/register/register-form';
import { Card } from '@/presentation/components/ui/card/card';

export default function RegisterPage() {
  return (
    <Card>
      <RegisterForm />
    </Card>
  );
}
