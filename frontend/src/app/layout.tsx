import { Geist } from 'next/font/google';

import { TanStackQueryProvider } from '@/infra/integrations/tanStack-query';
import { NuqsProvider } from '@/infra/integrations/nuqs';

import type { Metadata } from 'next';

import './globals.css';

const geistSans = Geist({
  variable: '--font-geist-sans',
  subsets: ['latin'],
});

export const metadata: Metadata = {
  title: 'Frontend',
  description: 'Frontend do PB ',
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className={`${geistSans.variable}`}>
      <body>
        <TanStackQueryProvider>
          <NuqsProvider>{children}</NuqsProvider>
        </TanStackQueryProvider>
      </body>
    </html>
  );
}
