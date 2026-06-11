import { type HTMLAttributes } from 'react';

import { cn } from '@/utils/utils';

interface CardProps extends HTMLAttributes<HTMLDivElement> {}

export function Card({ className, ...props }: CardProps) {
  return (
    <div
      className={cn(
        'w-full max-w-md rounded-xl border border-gray-200 bg-white p-8 shadow-sm',
        className,
      )}
      {...props}
    />
  );
}
