import { type ButtonHTMLAttributes } from 'react';

import { cn } from '@/utils/utils';
import { buttonVariants, type ButtonVariantProps } from './variants';

interface ButtonProps
  extends ButtonHTMLAttributes<HTMLButtonElement>, ButtonVariantProps {}

export function Button({ className, variant, size, ...props }: ButtonProps) {
  return (
    <button
      className={cn(buttonVariants({ variant, size }), className)}
      {...props}
    />
  );
}

export { buttonVariants };
