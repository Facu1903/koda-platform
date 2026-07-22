import type { PropsWithChildren } from 'react';
import { CompanyProfileProvider } from '../../platform/configuration/CompanyProfileProvider';
import { CapabilitiesProvider } from '../../platform/licensing/CapabilitiesProvider';
import { CompanyThemeProvider } from '../../theme/CompanyThemeProvider';

export function AppProviders({ children }: PropsWithChildren) {
  return (
    <CompanyProfileProvider>
      <CompanyThemeProvider>
        <CapabilitiesProvider>{children}</CapabilitiesProvider>
      </CompanyThemeProvider>
    </CompanyProfileProvider>
  );
}
