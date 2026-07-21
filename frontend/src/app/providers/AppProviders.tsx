import { CssBaseline, ThemeProvider } from '@mui/material';
import type { PropsWithChildren } from 'react';
import { CapabilitiesProvider } from '../../platform/licensing/CapabilitiesProvider';
import { kodaTheme } from '../../theme/kodaTheme';

export function AppProviders({ children }: PropsWithChildren) {
  return (
    <ThemeProvider theme={kodaTheme}>
      <CssBaseline />
      <CapabilitiesProvider>{children}</CapabilitiesProvider>
    </ThemeProvider>
  );
}
