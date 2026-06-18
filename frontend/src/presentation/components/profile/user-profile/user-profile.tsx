'use client';

import { useUserProfileModel } from './user-profile.model';
import { UserProfileView } from './user-profile.view';

export const UserProfile = () => {
  const model = useUserProfileModel();
  return <UserProfileView {...model} />;
};
