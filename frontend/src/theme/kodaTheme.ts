import { createTheme } from '@mui/material/styles';

export const kodaTheme = createTheme({
  palette: {
    mode: 'dark',
    primary: {
      main: '#F6862B',
      contrastText: '#111111',
    },
    background: {
      default: '#0B0D10',
      paper: '#12161C',
    },
    divider: 'rgba(255,255,255,0.10)',
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