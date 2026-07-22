export type ThemeMode = 'dark' | 'light' | 'system';

export interface CompanyProfileTenant {
  id: string;
  commercialName: string;
  countryCode: string;
}

export interface CompanyProfileBranding {
  logoUrl: string | null;
  faviconUrl: string | null;
  loginImageUrl: string | null;
  primaryColor: string;
  secondaryColor: string | null;
  themeMode: ThemeMode;
}

export interface CompanyProfileRegional {
  defaultLocale: string;
  defaultCurrency: string;
  timeZone: string;
  dateFormat: string;
  timeFormat: string;
  numberLocale: string;
  currencyFormat: string;
}

export interface CompanyRuntimeProfile {
  tenant: CompanyProfileTenant;
  branding: CompanyProfileBranding;
  regional: CompanyProfileRegional;
  updatedAt: string;
}

export const fallbackCompanyProfile: CompanyRuntimeProfile = {
  tenant: {
    id: 'local-fallback',
    commercialName: 'KODA',
    countryCode: 'AR',
  },
  branding: {
    logoUrl: null,
    faviconUrl: null,
    loginImageUrl: null,
    primaryColor: '#F6862B',
    secondaryColor: null,
    themeMode: 'dark',
  },
  regional: {
    defaultLocale: 'es-AR',
    defaultCurrency: 'ARS',
    timeZone: 'America/Argentina/Buenos_Aires',
    dateFormat: 'dd/MM/yyyy',
    timeFormat: 'HH:mm',
    numberLocale: 'es-AR',
    currencyFormat: 'symbol',
  },
  updatedAt: '2026-07-22T00:00:00Z',
};

export function effectiveCompanyProfile(profile: CompanyRuntimeProfile | null): CompanyRuntimeProfile {
  return profile ?? fallbackCompanyProfile;
}
