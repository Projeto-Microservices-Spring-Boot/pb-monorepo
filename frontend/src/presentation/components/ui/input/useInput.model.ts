import { useState, type FocusEvent } from 'react';

interface useInputModelProps {
  label?: string;
  error?: string;
  isError?: boolean;
  isDisabled?: boolean;
  value?: string;
  onFocus?: (e: FocusEvent) => void;
  onBlur?: (e: FocusEvent<HTMLInputElement>) => void;
}

export const useInputModel = ({
  label,
  error,
  isError,
  isDisabled,
  onFocus,
  value,
  ...props
}: useInputModelProps) => {
  const [isFocused, setIsFocused] = useState<boolean>(false);
  const [showPassword, setShowPassword] = useState<boolean | undefined>(false);

  const handleFocus = (e: FocusEvent) => {
    setIsFocused(true);
    onFocus?.(e);
  };

  const handleBlur = (e: FocusEvent<HTMLInputElement>) => {
    setIsFocused(false);
    onFocus?.(e);
  };

  const handleShowPassword = () => setShowPassword((prev) => !prev);

  return {
    isFocused,
    showPassword,
    handleShowPassword,
    handleFocus,
    handleBlur,
    label,
    error,
    value,
    isError,
    isDisabled,
    ...props,
  };
};
