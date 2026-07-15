import { CssBaseline, ThemeProvider } from '@mui/material';
import type { PropsWithChildren } from 'react';
import { kodaTheme } from '../../theme/kodaTheme';

export function AppProviders({ children }: PropsWithChildren) {
  return (
    <ThemeProvider theme={kodaTheme}>
      <CssBaseline />
      {children}
    </ThemeProvider>
  );
}