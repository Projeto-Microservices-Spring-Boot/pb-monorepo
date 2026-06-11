import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { loginFormSchema, type LoginFormSchema } from './login-form.schema';
import { useLogin } from '@/api/generated/users/endpoints/auth/auth.gen';
import { useAuthStore } from '@/infra/stores/useAuth.store';
import { useRouter } from 'next/navigation';

export const useLoginFormModel = () => {
  const router = useRouter();

  const setTokens = useAuthStore((s) => s.setTokens);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<LoginFormSchema>({
    mode: 'all',
    criteriaMode: 'firstError',
    defaultValues: { email: '', password: '' },
    resolver: zodResolver(loginFormSchema),
  });

  const { mutateAsync: userLoginMutation, isPending } = useLogin({
    mutation: {
      onSuccess: async (response) => {
        if (response?.accessToken && response?.refreshToken) {
          setTokens(response.accessToken, response.refreshToken);
        }
        reset();
        router.replace('/');
      },
    },
  });

  const onSubmit = handleSubmit(async (data) => {
    await userLoginMutation({ data });
  });

  return {
    register,
    errors,
    onSubmit,
    isPending,
  };
};
