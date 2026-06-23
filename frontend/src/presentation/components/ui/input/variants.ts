import { tv, type VariantProps } from 'tailwind-variants';

export const inputVariants = tv({
  slots: {
    input:
      'h-10 w-full flex justify-center  rounded-lg border px-3 py-2  text-gray-900 border-gray-300 bg-white text-base placeholder:text-gray-400',
    errorText: 'text-sm text-red-500 mt-1',
  },
  variants: {
    isFocused: {
      true: {
        input:
          'focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500 focus:outline-none disabled:cursor-not-allowed disabled:opacity-50',
      },
    },
    isError: {
      true: {
        input: ' border-red-500 focus:border-red-500 focus:ring-red-500',
      },
    },
  },
  defaultVariants: {
    isError: false,
    isFocused: false,
  },
});

export type InputVariantProps = VariantProps<typeof inputVariants>;
