import { UserProfile } from '@/presentation/components/profile/user-profile/user-profile';

export default function ProfilePage() {
  return (
    <div className="flex min-h-screen items-start justify-center bg-gray-50 px-4 py-12">
      <UserProfile />
    </div>
  );
}
