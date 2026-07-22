import { createTheme } from '@mui/material/styles';
import type { PaletteMode } from '@mui/material';
import type { CompanyProfileBranding } from '../platform/configuration/companyProfile';

const fallbackPrimaryColor = '#F6862B';

function normalizeColor(value: string | null | undefined, fallback: string): string {
  return value !== null && value !== undefined && /^#[0-9A-Fa-f]{6}$/.test(value) ? value.toUpperCase() : fallback;
}

function readableTextColor(hexColor: string): string {
  const red = Number.parseInt(hexColor.slice(1, 3), 16);
  const green = Number.parseInt(hexColor.slice(3, 5), 16);
  const blue = Number.parseInt(hexColor.slice(5, 7), 16);
  const luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255;

  return luminance > 0.58 ? '#111111' : '#FFFFFF';
}

function normalizePaletteMode(mode: string | null | undefined): PaletteMode {
  return mode === 'light' ? 'light' : 'dark';
}

export function createKodaTheme(branding?: CompanyProfileBranding | null, systemMode: PaletteMode = 'dark') {
  const paletteMode = branding?.themeMode === 'system' ? systemMode : normalizePaletteMode(branding?.themeMode);
  const primaryColor = normalizeColor(branding?.primaryColor, fallbackPrimaryColor);
  const secondaryColor = normalizeColor(branding?.secondaryColor, paletteMode === 'dark' ? '#FFFFFF' : '#111111');

  return createTheme({
  palette: {
    mode: paletteMode,
    primary: {
      main: primaryColor,
      contrastText: readableTextColor(primaryColor),
    },
    secondary: {
      main: secondaryColor,
      contrastText: readableTextColor(secondaryColor),
    },
    background: {
      default: paletteMode === 'dark' ? '#0B0D10' : '#F7F8FA',
      paper: paletteMode === 'dark' ? '#12161C' : '#FFFFFF',
    },
    divider: paletteMode === 'dark' ? 'rgba(255,255,255,0.10)' : 'rgba(17,24,39,0.12)',
  },
  shape: {
    borderRadius: 8,
  },
  typography: {
    fontFamily: ['Inter', 'Segoe UI', 'Roboto', 'Arial', 'sans-serif'].join(','),
    h1: {
      fontSize: '1.875rem',
      fontWeight: 700,
      letterSpacing: 0,
    },
    h2: {
      fontSize: '1.25rem',
      fontWeight: 700,
      letterSpacing: 0,
    },
    button: {
      textTransform: 'none',
      fontWeight: 700,
      letterSpacing: 0,
    },
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          borderRadius: 8,
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundImage: 'none',
        },
      },
    },
  },
});
}

export const kodaTheme = createKodaTheme();
