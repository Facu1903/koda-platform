import { describe, expect, it } from 'vitest';
import type { CompanyProfileBranding } from '../platform/configuration/companyProfile';
import { createKodaTheme } from './kodaTheme';

describe('createKodaTheme', () => {
  it('creates a tenant theme from branding values', () => {
    const branding: CompanyProfileBranding = {
      logoUrl: null,
      faviconUrl: null,
      loginImageUrl: null,
      primaryColor: '#2ba84a',
      secondaryColor: '#111111',
      themeMode: 'light',
    };

    const theme = createKodaTheme(branding);

    expect(theme.palette.mode).toBe('light');
    expect(theme.palette.primary.main).toBe('#2BA84A');
    expect(theme.palette.secondary.main).toBe('#111111');
    expect(theme.palette.background.default).toBe('#F7F8FA');
  });

  it('falls back to KODA dark theme when branding is incomplete or unsafe', () => {
    const branding: CompanyProfileBranding = {
      logoUrl: null,
      faviconUrl: null,
      loginImageUrl: null,
      primaryColor: 'orange',
      secondaryColor: null,
      themeMode: 'unknown' as CompanyProfileBranding['themeMode'],
    };

    const theme = createKodaTheme(branding);

    expect(theme.palette.mode).toBe('dark');
    expect(theme.palette.primary.main).toBe('#F6862B');
    expect(theme.palette.background.default).toBe('#0B0D10');
  });
});
