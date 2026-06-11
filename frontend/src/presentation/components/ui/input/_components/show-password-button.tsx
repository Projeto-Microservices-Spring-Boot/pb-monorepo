import { Eye, EyeClosed } from 'lucide-react';

interface ShowPasswordButtonProps {
  onClick: () => void;
  showPassword: boolean | undefined;
}

export const ShowPasswordButton = ({
  onClick,
  showPassword,
}: ShowPasswordButtonProps) => {
  return (
    <span className="mr-2 flex items-center justify-end" onClick={onClick}>
      {showPassword ? (
        <Eye className="absolute mb-12 ml-10" size={20} color="black" />
      ) : (
        <EyeClosed className="absolute mb-12 ml-10" size={20} color="black" />
      )}
    </span>
  );
};
