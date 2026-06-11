'use client';

import type { InputHTMLAttributes } from 'react';

import { inputVariants, type InputVariantProps } from './variants';
import { useInputModel } from './useInput.model';

import { cn } from '@/utils/utils';
import { ShowPasswordButton } from './_components/show-password-button';
import { ErrorMessage } from './_components/error-message';

interface InputProps
  extends InputHTMLAttributes<HTMLInputElement>, InputVariantProps {
  label?: string;
  error?: string;
}

export function Input({
  className,
  error,
  label,
  onBlur,
  value,
  type,
  ...props
}: InputProps) {
  const {
    handleBlur,
    handleFocus,
    isFocused,
    showPassword,
    handleShowPassword,
  } = useInputModel({
    onBlur,
    isError: !!error,
  });

  const styles = inputVariants({ isError: !!error, isFocused });

  return (
    <div className="space-y-1">
      {label && (
        <label
          htmlFor={label}
          className="block text-sm font-medium text-gray-700"
        >
          {label}
        </label>
      )}
      <input
        onBlur={handleBlur}
        onFocus={handleFocus}
        value={value}
        type={type === 'password' && showPassword ? 'text' : type}
        className={cn(styles.input(), className)}
        {...props}
      />

      {type === 'password' && (
        <ShowPasswordButton
          onClick={handleShowPassword}
          showPassword={showPassword}
        />
      )}

      {error && <ErrorMessage className={styles.errorText()} error={error} />}
    </div>
  );
}
