import * as z from 'zod';

const MIN_PASSWORD_LENGTH = 8;

export const loginFormSchema = z.object({
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
});

export type LoginFormSchema = z.infer<typeof loginFormSchema>;
