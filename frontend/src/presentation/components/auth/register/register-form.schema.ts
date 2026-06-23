import * as z from 'zod';

const MIN_PASSWORD_LENGTH = 8;
const MIN_NAME_LENGTH = 2;
const MAX_NAME_LENGTH = 100;

// TODO => adicionar validations
export const registerFormSchema = z
  .object({
    name: z
      .string()
      .nonempty({ message: 'Nome obrigatório!' })
      .min(MIN_NAME_LENGTH, {
        message: `O nome deve ter ao menos ${MIN_NAME_LENGTH} caracteres!`,
      })
      .max(MAX_NAME_LENGTH, {
        message: `O nome deve ter no máximo ${MAX_NAME_LENGTH} caracteres!`,
      }),
    email: z.email({ message: 'Email inválido!' }).nonempty(),
    password: z
      .string()
      .nonempty({ message: 'Senha obrigatória!' })
      .min(MIN_PASSWORD_LENGTH, {
        message: `A senha deve ter ao menos ${MIN_PASSWORD_LENGTH} caracteres`,
      })
      .regex(/[A-Z]/, {
        message: 'A senha deve ter ao menos uma letra maiúscula!',
      })
      .regex(/[a-z]/, {
        message: 'A senha deve ter ao menos uma letra minúscula!',
      })
      .regex(/[0-9]/, { message: 'A senha deve ter ao menos um número!' })
      .regex(/[!@#$%^&*(),.?":{}|<>]/, {
        message: 'A senha deve ter ao menos um caractere especial!',
      }),
    confirmPassword: z
      .string()
      .nonempty({ message: 'Confirmar senha obrigatório!' })
      .min(MIN_PASSWORD_LENGTH, {
        message: `A senha deve ter ao menos ${MIN_PASSWORD_LENGTH} caracteres`,
      }),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'As senhas não coincidem',
    path: ['confirmPassword'],
  });

export type RegisterFormSchema = z.infer<typeof registerFormSchema>;
