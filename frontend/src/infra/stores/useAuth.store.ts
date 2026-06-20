'use client';

import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';

type AuthStates = {
  accessToken: string | null;
  refreshToken: string | null;
};

type AuthActions = {
  setTokens: (accessToken: string, refreshToken: string) => void;
  logout: () => void;
};

type AuthStore = AuthStates & AuthActions;

export const useAuthStore = create<AuthStore>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,

      setTokens: (accessToken, refreshToken) => {
        set({ accessToken, refreshToken });
      },
      logout: () => {
        set({ accessToken: null, refreshToken: null });
      },
    }),
    { name: 'users-jwt', storage: createJSONStorage(() => localStorage) },
  ),
);
