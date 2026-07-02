export const ErrorMessage = ({
  className,
  error,
}: {
  className: string;
  error: string;
}) => {
  return (
    <p className={className} role="alert">
      {error}
    </p>
  );
};
