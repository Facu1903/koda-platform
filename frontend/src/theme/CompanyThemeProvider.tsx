import { CssBaseline, ThemeProvider } from '@mui/material';
import type { PaletteMode } from '@mui/material';
import type { PropsWithChildren } from 'react';
import { useEffect, useMemo, useState } from 'react';
import { useCompanyProfile } from '../platform/configuration/CompanyProfileProvider';
import { createKodaTheme } from './kodaTheme';

function readSystemMode(): PaletteMode {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
    return 'dark';
  }

  return window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark';
}

export function CompanyThemeProvider({ children }: PropsWithChildren) {
  const { effectiveProfile } = useCompanyProfile();
  const [systemMode, setSystemMode] = useState<PaletteMode>(readSystemMode);

  useEffect(() => {
    if (typeof window.matchMedia !== 'function') {
      return undefined;
    }

    const query = window.matchMedia('(prefers-color-scheme: light)');
    const listener = () => setSystemMode(readSystemMode());

    query.addEventListener('change', listener);

    return () => query.removeEventListener('change', listener);
  }, []);

  const theme = useMemo(() => createKodaTheme(effectiveProfile.branding, systemMode), [effectiveProfile.branding, systemMode]);

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      {children}
    </ThemeProvider>
  );
}
