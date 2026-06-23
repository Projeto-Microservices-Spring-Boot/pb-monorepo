import { useForm } from 'react-hook-form';
import { useRegister } from '@/api/generated/users/endpoints/auth/auth.gen';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  registerFormSchema,
  type RegisterFormSchema,
} from './register-form.schema';
import { useRouter } from 'next/navigation';

export const useRegisterFormModel = () => {
  const router = useRouter();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<RegisterFormSchema>({
    mode: 'all',
    criteriaMode: 'firstError',
    defaultValues: {
      name: '',
      email: '',
      password: '',
      confirmPassword: '',
    },
    resolver: zodResolver(registerFormSchema),
  });

  const { mutateAsync: userRegisterMutation, isPending } = useRegister({
    mutation: {
      onSuccess: async () => {
        router.replace('/auth/login');
        reset();
      },
    },
  });

  const onSubmit = handleSubmit(async (data) => {
    await userRegisterMutation({ data });
  });

  return {
    register,
    errors,
    onSubmit,
    isPending,
  };
};
